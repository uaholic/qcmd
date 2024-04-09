package com.guanyanqi.converter;

import com.google.common.collect.Lists;
import com.guanyanqi.constant.Constants;

import java.util.Collection;

/**
 * 默认的集合字符串转换器，实现了 {@link QCollectionStringConverter} 接口。
 * 该转换器用于将单个字符串按照预定义的分隔符（{@link Constants#COMMON_SPLIT_REG}）分割为字符串集合。
 *
 * 使用单例模式来确保应用中仅存在一个实例，减少资源占用。
 *
 * 示例用法：
 * {@code Collection<String> result = DefaultCollectionStringConverter.getInstance().convert("one,two,three");}
 * 结果 {@code result} 将是包含 "one", "two", "three" 的 {@link Collection}。
 *
 * 注意：这个转换器默认使用逗号（,）作为分隔符，分隔符可以在 {@link Constants#COMMON_SPLIT_REG} 中修改。
 *
 * @author guanyanqi
 */
public class DefaultCollectionStringConverter implements QCollectionStringConverter<String> {

    private static final DefaultCollectionStringConverter instance = new DefaultCollectionStringConverter();

    // 私有构造函数以防止外部直接实例化
    private DefaultCollectionStringConverter() {
    }

    /**
     * 将给定的字符串按照预定义的分隔符分割为一个字符串集合。
     *
     * @param value 待分割的字符串
     * @return 分割后的字符串集合，使用 {@link Lists#newArrayList} 创建
     */
    @Override
    public Collection<String> convert(String value) {
        return Lists.newArrayList(value.split(Constants.COMMON_SPLIT_REG));
    }

    /**
     * 获取 {@link DefaultCollectionStringConverter} 类的单例实例。
     *
     * @return {@link DefaultCollectionStringConverter} 的单例实例
     */
    public static DefaultCollectionStringConverter getInstance() {
        return instance;
    }
}
