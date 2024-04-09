package com.guanyanqi.utils;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.annotation.Vars;
import com.guanyanqi.converter.ConverterRegistry;
import com.guanyanqi.converter.DefaultCollectionStringConverter;
import com.guanyanqi.converter.DefaultMapStringConverter;
import com.guanyanqi.converter.QStringConverter;
import com.guanyanqi.exception.QCmdException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.*;
import java.util.*;

/**
 * 工具类提供静态方法用于实例化命令类、进行类型转换以及生成命令行工具的帮助信息。
 * 核心功能包括利用反射创建实例、处理类型转换、生成帮助说明等。
 *
 * @author guanyanqi
 */
public class QCmdUtils {

    /**
     * 创建给定类的实例，要求类必须有一个无参构造函数。
     *
     * @param <T>   类型参数，指明要创建实例的类
     * @param clazz 要创建实例的类的Class对象
     * @return 创建的实例
     * @throws QCmdException 如果创建实例过程中出现反射相关错误
     */
    public static <T> Object buildInstance(Class<T> clazz) throws QCmdException {
        // 创建实例
        Object instance = null;
        try {
            // 获取无参构造器，如果有参数，需要传递参数的类型
            Constructor<?> constructor = clazz.getDeclaredConstructor();
            // 开放访问权限
            constructor.setAccessible(true);
            instance = constructor.newInstance();
        } catch (Exception e) {
            throw new QCmdException(e);
        }
        return instance;
    }

    /**
     * 将字符串值按字段的类型进行转换。支持直接类型转换、集合、映射等复杂类型。
     *
     * @param field 字段对象，用于获取字段的泛型类型信息
     * @param type  目标转换类型
     * @param value 待转换的字符串值
     * @return 转换后的对象
     * @throws InvocationTargetException, InstantiationException, IllegalAccessException 当反射调用失败时
     */
    public static Object convert2Type(Field field, Class<?> type, String value) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        field.setAccessible(true);
        Object paramValue = convert2Type(type, value);
        if (Collection.class.isAssignableFrom(type)) {
            ParameterizedType pType = (ParameterizedType) field.getGenericType();
            Type[] actualTypeArguments = pType.getActualTypeArguments();
            Class<?> elementType = (Class<?>) actualTypeArguments[0];
            if (Collection.class.isAssignableFrom(elementType)) {
                throw new QCmdException("命令参数为集合类型时只能嵌套一层");
            }
            Collection collection;
            try {
                collection = createCollectionByType(type);
            } catch (NoSuchMethodException e) {
                throw new QCmdException(field.getName() + "字段，集合类型为" + type.getName() + "。没有默认构造方法无法创建实例。");
            }
            for (String elementValue : DefaultCollectionStringConverter.getInstance().convert(value)) {
                collection.add(elementType.cast(convert2Type(field, elementType, elementValue)));
            }
            paramValue = collection;
        }
        if (Map.class.isAssignableFrom(type)) {
            ParameterizedType pType = (ParameterizedType) field.getGenericType();
            Type[] actualTypeArguments = pType.getActualTypeArguments();
            Class<?> keyType = (Class<?>) actualTypeArguments[0];
            Class<?> valueType = (Class<?>) actualTypeArguments[1];
            if (Collection.class.isAssignableFrom(keyType) || Collection.class.isAssignableFrom(valueType) || Map.class.isAssignableFrom(keyType) || Map.class.isAssignableFrom(valueType)) {
                throw new QCmdException("命令参数为集合类型时只能嵌套一层");
            }
            Map map;
            if (Map.class == type) {
                map = new HashMap<>();
            } else {
                try {
                    map = (Map) type.getConstructor().newInstance();
                } catch (NoSuchMethodException e) {
                    throw new QCmdException(field.getName() + " 字段，集合类型为 " + type.getName() + "。没有默认构造方法无法创建实例。");
                }
            }
            for (Map.Entry<String, String> e : DefaultMapStringConverter.getInstance().convert(value).entrySet()) {
                String k = e.getKey();
                String v = e.getValue();
                Object key = convert2Type(field, keyType, k);
                Object val = convert2Type(field, valueType, v);
                map.put(key, val);
            }
            paramValue = map;
        }
        if (Objects.isNull(paramValue)) {
            try {
                Constructor<?> constructor = type.getConstructor(String.class);
                paramValue = constructor.newInstance(value);
            } catch (NoSuchMethodException e) {
                throw new QCmdException(field.getName() + "字段，类型为" + type.getName() + "。没有可用的类型转换器，也没有唯一String类型参数的构造方法，无法赋值。");
            }
        }
        return paramValue;
    }

    /**
     * 将字符串值按目标类型进行转换。不依赖于字段信息的简单类型转换。
     *
     * @param type  目标转换类型
     * @param value 待转换的字符串值
     * @return 转换后的对象
     */
    public static Object convert2Type(Class<?> type, String value) {
        Object paramValue = null;
        QStringConverter<?> converter = ConverterRegistry.getConverter(type);
        if (Objects.nonNull(converter)) {
            paramValue = converter.convert(value);
        }
        if (Enum.class.isAssignableFrom(type)) {
            paramValue = Enum.valueOf((Class<Enum>) type, value);
        }
        return paramValue;
    }

    /**
     * 根据类型创建相应的集合实例。支持List、Set、Queue等类型。
     *
     * @param type 集合的类型
     * @return 创建的集合实例
     * @throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException 当反射调用失败时
     */
    public static Collection createCollectionByType(Class<?> type) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Collection collection;
        if (List.class == type) {
            collection = Lists.newArrayList();
        } else if (Set.class == type) {
            collection = Sets.newHashSet();
        } else if (Collection.class == type) {
            collection = Lists.newArrayList();
        } else if (Queue.class == type || Deque.class == type) {
            collection = Lists.newLinkedList();
        } else {
            collection = (Collection) type.getConstructor().newInstance();
        }
        return collection;
    }

    /**
     * 根据Cmd注解和字段信息生成命令行工具的帮助说明。
     *
     * @param cmdAnnotation Cmd注解实例，包含命令的基本信息
     * @param fields        命令类中所有字段的列表，用于生成参数的帮助信息
     * @param vars          Vars注解实例，如果存在，用于生成变量的帮助信息
     * @return 生成的帮助说明文本
     */
    public static String createDesc(Cmd cmdAnnotation, List<Field> fields, Vars vars) {
        StringBuilder usage = new StringBuilder("使用方法：命令 [参数 参数值] [变量...]\n");
        String cmds = Joiner.on("|").join(cmdAnnotation.names());
        String cmdDesc = cmdAnnotation.desc();
        usage.append("命令：").append(cmds).append("\n");
        if (StringUtils.isNotBlank(cmdDesc)) {
            usage.append("功能描述：").append(cmdAnnotation.desc()).append("\n");
        }
        List<String> params = Lists.newArrayList();
        for (Field field : fields) {
            com.guanyanqi.annotation.Parameter annotation = field.getAnnotation(Parameter.class);
            if (Objects.nonNull(annotation)) {
                String paramName = Joiner.on("|").join(annotation.names());
                StringBuilder paramUsage = new StringBuilder("参数：");
                paramUsage.append(paramName);
                if (annotation.required()) {
                    paramUsage.append("（必填）");
                } else {
                    paramUsage.append("（可选）");
                }
                String paramDesc = annotation.desc();
                if (StringUtils.isNotBlank(paramDesc)) {
                    paramUsage.append("，参数说明：").append(paramDesc);
                }
                if (StringUtils.isNotBlank(annotation.valueValidDesc())) {
                    paramUsage.append("，输入规则：").append(annotation.valueValidDesc());
                }
                params.add(paramUsage.toString());
            }
        }
        if (CollectionUtils.isNotEmpty(params)) {
            usage.append("参数说明：").append("\n");
            for (String param : params) {
                usage.append("\t").append(param).append("\n");
            }
        }
        if (Objects.nonNull(vars)) {
            usage.append("变量描述：").append(vars.desc()).append("\n");
        }
        return usage.toString();
    }
}
