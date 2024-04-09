package com.guanyanqi.converter;

import java.util.Collection;

/**
 * 专门用于将字符串转换为特定类型的集合（Collection<T>）的转换器接口。
 * 继承自 {@link QStringConverter} 接口，强调了转换目标是一个集合类型。
 *
 * 这个接口允许实现类定义将单一字符串值转换为集合中元素的逻辑，
 * 通常用于处理命令行参数或配置值，将其拆分为多个独立的元素。
 *
 * @param <T> 集合中元素的目标类型
 *
 * @author guanyanqi
 */
public interface QCollectionStringConverter<T> extends QStringConverter<Collection<T>> {

    /**
     * 将给定的字符串值转换为目标类型 {@code T} 的集合。
     *
     * @param value 待转换的字符串
     * @return 转换后的集合，包含类型为 {@code T} 的元素
     */
    @Override
    Collection<T> convert(String value);
}
