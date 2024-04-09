package com.guanyanqi.converter;

import java.util.Map;

/**
 * 专门用于将字符串转换为键值对映射（Map<K, V>）的转换器接口。
 * 继承自 {@link QStringConverter} 接口，强调了转换目标是一个映射（Map）类型。
 *
 * 这个接口允许实现类定义将单一字符串值（通常格式为"key1=value1,key2=value2"）转换为Map中的键值对的逻辑，
 * 非常适合处理需要从字符串中解析键值对数据的场景。
 *
 * @param <K> Map中键的类型
 * @param <V> Map中值的类型
 *
 * @author guanyanqi
 */
public interface QMapStringConverter<K, V> extends QStringConverter<Map<K, V>> {

    /**
     * 将给定的字符串值转换为键值对映射。
     *
     * @param value 待转换的字符串，通常包含多个键值对
     * @return 转换后的Map，包含类型为 {@code K} 的键和类型为 {@code V} 的值
     */
    @Override
    Map<K, V> convert(String value);
}
