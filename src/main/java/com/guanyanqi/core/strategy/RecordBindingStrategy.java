package com.guanyanqi.core.strategy;

import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.annotation.Vars;
import com.guanyanqi.core.CommandDescriptor;
import com.guanyanqi.core.CommandLineParser;
import com.guanyanqi.core.model.OptionDescriptor;
import com.guanyanqi.core.model.VarsDescriptor;
import com.guanyanqi.exception.QCmdException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.RecordComponent;
import java.util.HashMap;
import java.util.Map;

/**
 * Java 16+ Record 不可变类的元数据提取与规范构造器（Canonical Constructor）绑定策略。
 *
 * <p>核心机制说明：</p>
 * 1. <b>不可变性与规范构造器</b>：Record 类的字段全部为 private final，没有无参构造函数与 setter 方法。
 *    因此必须通过反射获取其规范构造函数（Canonical Constructor），并按编译期定义的组件顺序传入参数数组。
 * 2. <b>注解级联提取（Annotation Cascading）</b>：
 *    Java 中在 Record 头部声明的注解，可能被编译器自动挂载在 RecordComponent、自动生成的 private final Field，
 *    或自动生成的 Accessor 方法上。{@link #getParameterAnnotation} 实现了多级兜底提取，确保 100% 识别注解。
 * 3. <b>基本类型默认值兜底</b>：
 *    当规范构造函数需要基本类型（如 int, boolean）而命令行未传值时，Java 反射反射调用会抛出 {@link IllegalArgumentException}。
 *    因此必须自动补充 0, false 等默认基础值。
 *
 * @author guanyanqi
 */
public class RecordBindingStrategy implements CommandBindingStrategy {

    /**
     * 创建 Record 绑定策略实例。
     */
    public RecordBindingStrategy() {
    }

    @Override
    public void extractMetadata(Class<?> targetClass, CommandDescriptor descriptor) {
        RecordComponent[] components = targetClass.getRecordComponents();
        for (RecordComponent comp : components) {
            // 级联提取 @Parameter 注解，未找到则尝试 @Vars（互斥）
            Parameter param = getParameterAnnotation(comp, targetClass);
            Vars varsAnnotation = getVarsAnnotation(comp, targetClass);
            if (param != null && varsAnnotation != null) {
                throw new QCmdException("Record 组件 [" + comp.getName()
                        + "] 不能同时声明 @Parameter 和 @Vars");
            }
            if (param != null) {
                OptionDescriptor option = new OptionDescriptor(
                        param.names(),
                        param.desc(),
                        param.required(),
                        param.valueValidRegex(),
                        param.valueValidDesc(),
                        param.converter(),
                        comp.getType(),
                        comp.getGenericType(),
                        comp.getName(),
                        comp
                );
                descriptor.registerOption(option);
            } else if (varsAnnotation != null) {
                VarsDescriptor vars = new VarsDescriptor(
                        varsAnnotation.desc(),
                        varsAnnotation.elementConverter(),
                        comp.getType(),
                        comp.getGenericType(),
                        comp.getName(),
                        comp
                );
                descriptor.registerVars(vars);
            }
        }
    }

    @Override
    public <T> T bindInstance(CommandLineParser.ParseResult parseResult, CommandDescriptor descriptor, Class<T> targetClass) throws Exception {
        RecordComponent[] components = targetClass.getRecordComponents();
        Class<?>[] paramTypes = new Class<?>[components.length];
        Object[] paramValues = new Object[components.length];

        // 1. 将命令行解析出的 选项名 -> 原始值，转换为 目标属性名(targetName) -> 原始值 的映射
        Map<String, String> optionValueByTargetName = new HashMap<>();
        for (Map.Entry<String, String> entry : parseResult.optionValues().entrySet()) {
            String optionName = entry.getKey();
            OptionDescriptor option = descriptor.getNameToOptionMap().get(optionName);
            if (option != null) {
                optionValueByTargetName.put(option.targetName(), entry.getValue());
            }
        }

        VarsDescriptor varsDesc = descriptor.getVarsDescriptor();

        // 2. 严格按照 Record 规范构造函数的参数顺序（0 ~ N-1）依次装配参数数组
        for (int i = 0; i < components.length; i++) {
            RecordComponent comp = components[i];
            paramTypes[i] = comp.getType();
            String compName = comp.getName();

            OptionDescriptor option = descriptor.getOptionByTargetName(compName);

            if (option != null) {
                // 场景 A：该组件映射为一个 CLI 选项参数
                String rawVal = optionValueByTargetName.get(compName);
                if (rawVal != null) {
                    paramValues[i] = descriptor.convertValue(option.type(), option.genericType(), option.converterClass(), rawVal);
                } else {
                    // 选项未提供时，填充基本类型默认值或 null
                    paramValues[i] = getDefaultPrimitiveValue(comp.getType());
                }
            } else if (varsDesc != null && compName.equals(varsDesc.targetName())) {
                // 场景 B：该组件通过 targetName 显式匹配为位置变量组件
                if (!parseResult.positionalVars().isEmpty()) {
                    paramValues[i] = descriptor.convertVars(varsDesc.type(), varsDesc.genericType(), varsDesc, parseResult.positionalVars());
                } else {
                    paramValues[i] = getDefaultPrimitiveValue(comp.getType());
                }
            } else {
                // 场景 C：无注解组件，填充类型默认值
                paramValues[i] = getDefaultPrimitiveValue(comp.getType());
            }
        }

        // 3. 通过反射获取规范构造函数（Canonical Constructor）并实例化 Record
        Constructor<T> canonicalConstructor = targetClass.getDeclaredConstructor(paramTypes);
        canonicalConstructor.setAccessible(true);
        return canonicalConstructor.newInstance(paramValues);
    }

    /**
     * 级联提取 @Parameter 注解。
     * 优先尝试 RecordComponent 本身，若为空则顺查底层 Field 与 Accessor 方法。
     *
     * @param comp        Record 组件
     * @param recordClass Record 目标类 Class
     * @return 提取到的 Parameter 注解实例，未找到返回 null
     */
    public static Parameter getParameterAnnotation(RecordComponent comp, Class<?> recordClass) {
        Parameter param = comp.getAnnotation(Parameter.class);
        if (param != null) return param;
        try {
            Field field = recordClass.getDeclaredField(comp.getName());
            param = field.getAnnotation(Parameter.class);
            if (param != null) return param;
        } catch (Exception ignored) {}
        try {
            param = comp.getAccessor().getAnnotation(Parameter.class);
            if (param != null) return param;
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * 级联提取 @Vars 注解。
     * 优先尝试 RecordComponent 本身，若为空则顺查底层 Field 与 Accessor 方法。
     *
     * @param comp        Record 组件
     * @param recordClass Record 目标类 Class
     * @return 提取到的 Vars 注解实例，未找到返回 null
     */
    public static Vars getVarsAnnotation(RecordComponent comp, Class<?> recordClass) {
        Vars varsAnno = comp.getAnnotation(Vars.class);
        if (varsAnno != null) return varsAnno;
        try {
            Field field = recordClass.getDeclaredField(comp.getName());
            varsAnno = field.getAnnotation(Vars.class);
            if (varsAnno != null) return varsAnno;
        } catch (Exception ignored) {}
        try {
            varsAnno = comp.getAccessor().getAnnotation(Vars.class);
            if (varsAnno != null) return varsAnno;
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * 为基本类型生成默认零值，防止 Record 规范构造函数反射调用报 IllegalArgumentException。
     *
     * @param type 目标类型 Class
     * @return 基本类型默认零值对象
     */
    private static Object getDefaultPrimitiveValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0d;
        if (type == float.class) return 0.0f;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return '\0';
        return null;
    }
}
