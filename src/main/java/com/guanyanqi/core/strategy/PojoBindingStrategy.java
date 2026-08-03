package com.guanyanqi.core.strategy;

import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.annotation.Vars;
import com.guanyanqi.core.CommandDescriptor;
import com.guanyanqi.core.CommandLineParser;
import com.guanyanqi.core.model.OptionDescriptor;
import com.guanyanqi.core.model.VarsDescriptor;
import com.guanyanqi.exception.QCmdException;
import com.guanyanqi.utils.QCmdUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

/**
 * 普通 POJO 类的解析与注入绑定策略。
 * 遍历 Field 构造 OptionDescriptor/VarsDescriptor 统一领域模型。
 *
 * @author guanyanqi
 */
public class PojoBindingStrategy implements CommandBindingStrategy {

    /**
     * 创建 POJO 绑定策略实例。
     */
    public PojoBindingStrategy() {
    }

    @Override
    public void extractMetadata(Class<?> targetClass, CommandDescriptor descriptor) {
        List<Field> fields = QCmdUtils.getAllFieldsList(targetClass);
        for (Field field : fields) {
            Parameter param = field.getAnnotation(Parameter.class);
            Vars varsAnnotation = field.getAnnotation(Vars.class);
            if (param != null && varsAnnotation != null) {
                throw new QCmdException("属性 [" + field.getName() + "] 不能同时声明 @Parameter 和 @Vars");
            }
            if (param != null) {
                OptionDescriptor option = new OptionDescriptor(
                        param.names(),
                        param.desc(),
                        param.required(),
                        param.valueValidRegex(),
                        param.valueValidDesc(),
                        param.converter(),
                        field.getType(),
                        field.getGenericType(),
                        field.getName(),
                        field
                );
                descriptor.registerOption(option);
            } else if (varsAnnotation != null) {
                VarsDescriptor vars = new VarsDescriptor(
                        varsAnnotation.desc(),
                        varsAnnotation.elementConverter(),
                        field.getType(),
                        field.getGenericType(),
                        field.getName(),
                        field
                );
                descriptor.registerVars(vars);
            }
        }
    }

    @Override
    public <T> T bindInstance(CommandLineParser.ParseResult parseResult, CommandDescriptor descriptor, Class<T> targetClass) throws Exception {
        Constructor<T> constructor = targetClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        T instance = constructor.newInstance();

        // 1. 绑定 Option 参数
        for (Map.Entry<String, String> entry : parseResult.optionValues().entrySet()) {
            String optionName = entry.getKey();
            String rawVal = entry.getValue();

            OptionDescriptor option = descriptor.getNameToOptionMap().get(optionName);
            if (option != null && option.rawElement() instanceof Field field) {
                field.setAccessible(true);
                Object val = descriptor.convertValue(option.type(), option.genericType(), option.converterClass(), rawVal);
                field.set(instance, val);
            }
        }

        // 2. 绑定 Vars 位置变量
        VarsDescriptor varsDesc = descriptor.getVarsDescriptor();
        if (varsDesc != null && varsDesc.rawElement() instanceof Field field && !parseResult.positionalVars().isEmpty()) {
            field.setAccessible(true);
            Object varsVal = descriptor.convertVars(varsDesc.type(), varsDesc.genericType(), varsDesc, parseResult.positionalVars());
            field.set(instance, varsVal);
        }

        return instance;
    }
}
