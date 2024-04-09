package com.guanyanqi.constant;

/**
 * 定义了整个应用程序中通用的常量值。
 * 通过集中管理这些常量，可以提高代码的可读性和维护性，同时避免硬编码字符串的重复出现。
 *
 * 使用这些常量而不是直接使用字符串字面量，可以减少因拼写错误导致的bug，
 * 并使得将来对这些值的任何更改都集中在一个位置，易于管理和更新。
 *
 * 目前包括：
 * - 常用的列表项分隔符
 * - 用于键值对的分隔符
 *
 * 可以根据应用程序的发展，进一步添加更多的常用常量。
 *
 * @author guanyanqi
 */
public class Constants {
    /**
     * 用于分隔列表项的常用正则表达式字符串。
     * 例如，在处理以逗号分隔的字符串列表时使用。
     */
    public static final String COMMON_SPLIT_REG = ",";

    /**
     * 用于分隔键值对的常用正则表达式字符串。
     * 例如，在解析形如“key=value”的字符串时使用。
     */
    public static final String COMMON_KV_SPLIT_REG = "=";
}
