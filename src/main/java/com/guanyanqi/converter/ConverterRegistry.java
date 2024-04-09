package com.guanyanqi.converter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局转换器注册器类，负责维护类型到其相应QStringConverter转换器的映射。
 * 支持对基本类型、常用的Java类型及Java 8日期时间类型的转换。
 *
 * 提供了注册和获取转换器的静态方法，可以方便地在应用启动时注册所需的转换器，
 * 并在运行时获取特定类型的转换器进行数据转换。
 *
 * 该类使用ConcurrentHashMap来存储类型与转换器的映射，确保了线程安全。
 *
 * 示例用法：
 * 注册转换器 - {@code ConverterRegistry.register(MyClass.class, myClassConverter);}
 * 获取转换器 - {@code QStringConverter<MyClass> converter = ConverterRegistry.getConverter(MyClass.class);}
 *
 * @author guanyanqi
 */
public class ConverterRegistry {
    // 存储类型与转换器映射的线程安全HashMap
    private static final Map<Class<?>, QStringConverter<?>> converters = new ConcurrentHashMap<>();

    // 定义全局默认的日期时间格式化器
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static {
        // 在静态初始化块中注册默认的转换器
        register(String.class, a -> a); // 对String类型，转换器直接返回输入值
        register(Integer.class, Integer::valueOf); // Integer类型及其基本类型
        register(int.class, Integer::valueOf);
        register(Double.class, Double::valueOf); // Double类型及其基本类型
        register(double.class, Double::valueOf);
        register(Float.class, Float::valueOf); // Float类型及其基本类型
        register(float.class, Float::valueOf);
        register(Long.class, Long::valueOf); // Long类型及其基本类型
        register(long.class, Long::valueOf);
        register(Boolean.class, Boolean::valueOf); // Boolean类型及其基本类型
        register(boolean.class, Boolean::valueOf);
        register(Byte.class, Byte::valueOf); // Byte类型及其基本类型
        register(byte.class, Byte::valueOf);
        register(Short.class, Short::valueOf); // Short类型及其基本类型
        register(short.class, Short::valueOf);
        register(BigDecimal.class, BigDecimal::new); // BigDecimal类型
        register(LocalDate.class, LocalDate::parse); // LocalDate类型
        register(LocalDateTime.class, s -> LocalDateTime.parse(s, formatter)); // LocalDateTime类型，使用自定义格式化器
        register(LocalTime.class, LocalTime::parse); // LocalTime类型
        register(Date.class, s -> Date.from(LocalDateTime.parse(s, formatter)
                .atZone(ZoneId.systemDefault()).toInstant())); // Date类型，转换为使用系统默认时区的日期时间
    }

    /**
     * 获取指定类型的转换器。
     *
     * @param <T> 期望转换的目标类型
     * @param type 要转换的类型的Class对象
     * @return 给定类型的QStringConverter，如果未找到返回null
     */
    public static <T> QStringConverter<T> getConverter(Class<T> type) {
        return (QStringConverter<T>) converters.get(type);
    }

    /**
     * 注册新的类型转换器。
     *
     * @param <T> 要注册的转换器的目标类型
     * @param type 要注册的类型的Class对象
     * @param converter 类型对应的转换器实例
     */
    public static <T> void register(Class<T> type, QStringConverter<T> converter) {
        converters.put(type, converter);
    }
}
