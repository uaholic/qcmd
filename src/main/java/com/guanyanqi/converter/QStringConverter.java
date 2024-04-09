package com.guanyanqi.converter;

/**
 * 一个功能性接口，定义了一个通用的字符串转换逻辑。
 * 旨在将字符串转换为任意类型 {@code T}，支持从简单的数据类型转换到复杂对象的构建。
 *
 * 此接口可以被实现用于处理各种数据转换需求，特别是在解析命令行参数或配置文件时，
 * 将字符串表示的值转换为更加具体和有用的数据类型。
 *
 * @param <T> 转换后的目标类型
 *
 * @author guanyanqi
 */
@FunctionalInterface
public interface QStringConverter<T> {

    /**
     * 将给定的字符串值转换为指定的类型 {@code T}。
     *
     * @param value 待转换的字符串
     * @return 转换后的目标类型实例
     */
    T convert(String value);
}
