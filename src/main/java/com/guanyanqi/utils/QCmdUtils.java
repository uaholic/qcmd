package com.guanyanqi.utils;

import com.guanyanqi.exception.QCmdException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;

/**
 * 基础反射与集合构造通用工具类。
 *
 * @author guanyanqi
 */
public class QCmdUtils {

    /**
     * 私有构造函数以防止静态工具类被误实例化。
     */
    private QCmdUtils() {
    }

    /**
     * 获取类及其所有父类的 DeclaredField 字段列表。
     *
     * @param clazz 目标 Class 对象
     * @return 字段列表
     */
    public static List<Field> getAllFieldsList(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    /**
     * 根据集合接口类型动态实例化具体集合实现对象。
     *
     * @param type 集合接口或实现类 Class
     * @return 实例化后的 Collection 对象
     * @throws Exception 当无法实例化集合对象时抛出
     */
    @SuppressWarnings("rawtypes")
    public static Collection createCollectionByType(Class<?> type) throws Exception {
        if (List.class == type || Collection.class == type) {
            return new ArrayList<>();
        } else if (Set.class == type) {
            return new HashSet<>();
        } else if (Queue.class == type || Deque.class == type) {
            return new LinkedList<>();
        } else {
            try {
                Constructor<?> ctor = type.getDeclaredConstructor();
                ctor.setAccessible(true);
                return (Collection) ctor.newInstance();
            } catch (NoSuchMethodException e) {
                throw new QCmdException("集合类型 [" + type.getName() + "] 没有默认无参构造方法无法创建实例");
            }
        }
    }
}
