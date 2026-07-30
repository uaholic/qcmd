package com.guanyanqi.core;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.converter.ConverterRegistry;
import com.guanyanqi.converter.DefaultCollectionStringConverter;
import com.guanyanqi.converter.DefaultMapStringConverter;
import com.guanyanqi.converter.NoConverter;
import com.guanyanqi.converter.QStringConverter;
import com.guanyanqi.core.model.OptionDescriptor;
import com.guanyanqi.core.model.VarsDescriptor;
import com.guanyanqi.core.strategy.CommandBindingStrategy;
import com.guanyanqi.core.strategy.CommandBindingStrategyFactory;
import com.guanyanqi.exception.QCmdException;
import com.guanyanqi.utils.QCmdUtils;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 提取并持有命令类的统一领域模型描述符（包含 OptionDescriptor 列表与 VarsDescriptor）。
 *
 * <p>核心类型转换机制说明（{@link #convertValue}）：
 * 当将命令行中的原始 String 转换到目标字段/组件类型时，优先级如下：
 * 1. <b>自定义转换器 (Custom Converter)</b>：若注解中配置了 {@code converter = MyConverter.class}，优先使用。
 * 2. <b>全局注册转换器 (Global Registry)</b>：查找内置的 20+ 种数据类型转换器（如 Integer, LocalDate 等）。
 * 3. <b>枚举类型 (Enum)</b>：基于 {@code Enum.valueOf} 自动解析枚举名称。
 * 4. <b>集合类型 (Collection)</b>：提取元素泛型，使用 {@code DefaultCollectionStringConverter} 切分字符串并递归转换每个元素。
 * 5. <b>键值映射 (Map)</b>：提取 Key/Value 泛型，使用 {@code DefaultMapStringConverter} 解析键值对并递归转换。
 * 6. <b>String 参数构造方法兜底</b>：若目标类提供了接收单个 String 的构造方法，自动调用实例化。
 *
 * @author guanyanqi
 */
public class CommandDescriptor {

    private final Class<?> targetClass;
    private final Cmd cmdAnnotation;
    private final Set<String> commandNames;
    private final List<OptionDescriptor> options = new ArrayList<>();
    private final Map<String, OptionDescriptor> nameToOptionMap = new HashMap<>();
    private final Map<String, OptionDescriptor> targetNameToOptionMap = new HashMap<>();
    private final Set<String> boolOptionNames = new HashSet<>();
    private final List<List<String>> requiredOptionGroups = new ArrayList<>();
    private VarsDescriptor varsDescriptor;

    /**
     * 转换器单例缓存，避免频繁反射实例化。
     */
    private static final Map<Class<? extends QStringConverter<?>>, QStringConverter<?>> CONVERTER_CACHE = new ConcurrentHashMap<>();

    public CommandDescriptor(Class<?> targetClass) {
        this.targetClass = Objects.requireNonNull(targetClass, "Target class must not be null");
        this.cmdAnnotation = targetClass.getAnnotation(Cmd.class);
        if (this.cmdAnnotation == null) {
            throw new QCmdException("命令类 " + targetClass.getName() + " 未标注 @Cmd 注解");
        }
        if (this.cmdAnnotation.names().length == 0) {
            throw new QCmdException("命令类 " + targetClass.getName() + " @Cmd 注解 names 不能为空");
        }
        this.commandNames = new HashSet<>(Arrays.asList(this.cmdAnnotation.names()));

        // 使用策略模式自动判定目标类类型（POJO 还是 Java Record），提取描述符元数据
        CommandBindingStrategy strategy = CommandBindingStrategyFactory.getStrategy(targetClass);
        strategy.extractMetadata(targetClass, this);
    }

    /**
     * 注册选项描述符，建立选项名称与目标属性名的多重索引映射。
     */
    public void registerOption(OptionDescriptor option) {
        options.add(option);
        targetNameToOptionMap.put(option.targetName(), option);
        for (String name : option.names()) {
            OptionDescriptor old = nameToOptionMap.put(name, option);
            if (old != null) {
                throw new QCmdException("参数名称 [" + name + "] 重复定义在多个元素上");
            }
            if (option.type() == boolean.class || option.type() == Boolean.class) {
                boolOptionNames.add(name);
            }
        }
        if (option.required()) {
            requiredOptionGroups.add(Arrays.asList(option.names()));
        }
    }

    /**
     * 注册位置变量描述符，确保整个命令类最多声明一个 @Vars 位置变量。
     */
    public void registerVars(VarsDescriptor vars) {
        if (this.varsDescriptor != null) {
            throw new QCmdException("命令类 " + targetClass.getName() + " 最多只能声明一个 @Vars 位置变量");
        }
        this.varsDescriptor = vars;
    }

    public OptionDescriptor getOptionByTargetName(String targetName) {
        return targetNameToOptionMap.get(targetName);
    }

    /**
     * 通用类型转换核心管线方法。
     *
     * @param type                 目标字段/组件Class
     * @param genericType          目标字段/组件完整 Generic Type（用于提取集合/Map 泛型）
     * @param customConverterClass 自定义转换器 Class（无自定义时传入 NoConverter.class）
     * @param rawValue             命令行输入的原始字符串
     * @return 转换后的强类型对象
     * @throws Exception 当转换失败或没有适用的转换策略时抛出
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Object convertValue(Class<?> type, Type genericType, Class<? extends QStringConverter<?>> customConverterClass, String rawValue) throws Exception {
        // 1. 优先使用自定义转换器
        QStringConverter<?> customConverter = getConverterInstance(customConverterClass);
        if (customConverter != null) {
            return customConverter.convert(rawValue);
        }

        // 2. 尝试全局转换器注册表
        QStringConverter<?> registeredConverter = ConverterRegistry.getConverter(type);
        if (registeredConverter != null) {
            return registeredConverter.convert(rawValue);
        }

        // 3. 处理 Enum 枚举
        if (Enum.class.isAssignableFrom(type)) {
            return Enum.valueOf((Class<Enum>) type, rawValue);
        }

        // 4. 处理 Collection 集合（递归解析元素泛型）
        if (Collection.class.isAssignableFrom(type)) {
            Class<?> elementType = String.class;
            if (genericType instanceof ParameterizedType pType) {
                elementType = (Class<?>) pType.getActualTypeArguments()[0];
            }
            Collection collection = QCmdUtils.createCollectionByType(type);
            for (String elemStr : DefaultCollectionStringConverter.getInstance().convert(rawValue)) {
                collection.add(convertValue(elementType, elementType, NoConverter.class, elemStr));
            }
            return collection;
        }

        // 5. 处理 Map 映射（递归解析 Key / Value 泛型）
        if (Map.class.isAssignableFrom(type)) {
            Class<?> keyType = String.class;
            Class<?> valueType = String.class;
            if (genericType instanceof ParameterizedType pType) {
                keyType = (Class<?>) pType.getActualTypeArguments()[0];
                valueType = (Class<?>) pType.getActualTypeArguments()[1];
            }
            Map map = Map.class == type ? new HashMap<>() : (Map) type.getConstructor().newInstance();
            for (Map.Entry<String, String> e : DefaultMapStringConverter.getInstance().convert(rawValue).entrySet()) {
                Object k = convertValue(keyType, keyType, NoConverter.class, e.getKey());
                Object v = convertValue(valueType, valueType, NoConverter.class, e.getValue());
                map.put(k, v);
            }
            return map;
        }

        // 6. 兜底方案：寻找目标类自带的唯一 String 参数构造方法（如 new CustomType("rawValue")）
        try {
            Constructor<?> stringConstructor = type.getConstructor(String.class);
            return stringConstructor.newInstance(rawValue);
        } catch (NoSuchMethodException e) {
            throw new QCmdException("类型 [" + type.getName() + "] 没有注册转换器，也没有唯一的 String 类型参数构造方法");
        }
    }

    /**
     * 将解析出的位置变量（Positional Vars）转换为目标变量属性要求的类型（单个对象或集合）。
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Object convertVars(Class<?> type, Type genericType, VarsDescriptor varsDesc, List<String> positionalVars) throws Exception {
        QStringConverter<?> customConverter = getConverterInstance(varsDesc.elementConverterClass());
        String primaryCmd = getCommandNames().iterator().next();

        if (!Collection.class.isAssignableFrom(type)) {
            // 单变量场景：要求位置变量列表长度必须为 1
            if (positionalVars.size() != 1) {
                throw new QCmdException("命令 [" + primaryCmd + "] 的变量只接收1个参数，实际收到 " + positionalVars.size() + " 个");
            }
            String rawVal = positionalVars.get(0);
            if (customConverter != null) {
                return customConverter.convert(rawVal);
            }
            return convertValue(type, genericType, NoConverter.class, rawVal);
        } else {
            // 集合变量场景：将所有位置变量依次转换并添加入目标集合中
            Collection collection = QCmdUtils.createCollectionByType(type);
            Class<?> elementType = String.class;
            if (genericType instanceof ParameterizedType pType) {
                elementType = (Class<?>) pType.getActualTypeArguments()[0];
            }
            for (String varStr : positionalVars) {
                if (customConverter != null) {
                    collection.add(customConverter.convert(varStr));
                } else {
                    collection.add(convertValue(elementType, elementType, NoConverter.class, varStr));
                }
            }
            return collection;
        }
    }

    /**
     * 从缓存获取或反射实例化转换器单例。
     */
    private static QStringConverter<?> getConverterInstance(Class<? extends QStringConverter<?>> clazz) {
        if (clazz == NoConverter.class || clazz == null) return null;
        return CONVERTER_CACHE.computeIfAbsent(clazz, key -> {
            try {
                Constructor<? extends QStringConverter<?>> ctor = key.getDeclaredConstructor();
                ctor.setAccessible(true);
                return ctor.newInstance();
            } catch (Exception e) {
                throw new QCmdException("实例化转换器 [" + key.getName() + "] 失败", e);
            }
        });
    }

    public Class<?> getTargetClass() { return targetClass; }
    public Cmd getCmdAnnotation() { return cmdAnnotation; }
    public Set<String> getCommandNames() { return commandNames; }
    public List<OptionDescriptor> getOptions() { return options; }
    public Map<String, OptionDescriptor> getNameToOptionMap() { return nameToOptionMap; }
    public Set<String> getBoolOptionNames() { return boolOptionNames; }
    public List<List<String>> getRequiredOptionGroups() { return requiredOptionGroups; }
    public VarsDescriptor getVarsDescriptor() { return varsDescriptor; }
}
