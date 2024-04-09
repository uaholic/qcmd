package com.guanyanqi.converter;

import com.google.common.collect.Maps;
import com.guanyanqi.constant.Constants;
import com.guanyanqi.exception.QCmdException;

import java.util.Map;

/**
 * 默认的Map字符串转换器，实现了 {@link QMapStringConverter} 接口。
 * 该转换器用于将单个字符串按照预定义的项分隔符（{@link Constants#COMMON_SPLIT_REG}）和键值对分隔符（{@link Constants#COMMON_KV_SPLIT_REG}）
 * 分割为键值对，并存储在 {@link Map} 中。
 *
 * 使用单例模式来确保应用中仅存在一个实例。
 *
 * 示例用法：
 * {@code Map<String, String> result = DefaultMapStringConverter.getInstance().convert("key1=value1,key2=value2");}
 * 结果 {@code result} 将是一个包含 {"key1"="value1", "key2"="value2"} 的 {@link Map}。
 *
 * 注意：这个转换器默认使用逗号（,）作为项分隔符，等号（=）作为键值对分隔符，这些分隔符可以在 {@link Constants} 中修改。
 * 如果输入字符串不符合预期格式（"key=value"），会抛出 {@link QCmdException} 异常。
 *
 * @author guanyanqi
 */
public class DefaultMapStringConverter implements QMapStringConverter<String, String> {

    private static final DefaultMapStringConverter instance = new DefaultMapStringConverter();

    // 私有构造函数以防止外部直接实例化
    private DefaultMapStringConverter() {
    }

    /**
     * 将给定的字符串按照预定义的分隔符分割为键值对，并返回一个 {@link Map}。
     *
     * @param value 待转换的字符串，期望包含键值对，键值对之间用 {@link Constants#COMMON_SPLIT_REG} 分隔，
     *              键和值之间用 {@link Constants#COMMON_KV_SPLIT_REG} 分隔。
     * @return 转换后的键值对存储在 {@link Map} 中
     * @throws QCmdException 如果字符串格式不符合"key=value"的预期格式
     */
    @Override
    public Map<String, String> convert(String value) {
        Map<String, String> map = Maps.newHashMap();
        for (String s : value.split(Constants.COMMON_SPLIT_REG)) {
            String[] kv = s.split(Constants.COMMON_KV_SPLIT_REG, 2);
            if (kv.length < 2) {
                throw new QCmdException("Map类型参数格式错误，期望格式为key=value。");
            }
            map.put(kv[0], kv[1]);
        }
        return map;
    }

    /**
     * 获取 {@link DefaultMapStringConverter} 类的单例实例。
     *
     * @return {@link DefaultMapStringConverter} 的单例实例
     */
    public static DefaultMapStringConverter getInstance() {
        return instance;
    }
}
