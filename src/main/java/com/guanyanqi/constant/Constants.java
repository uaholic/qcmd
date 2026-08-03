package com.guanyanqi.constant;

/**
 * 定义了整个应用程序中通用的常量值。
 * 通过集中管理这些常量，可以提高代码的可读性和维护性，同时避免硬编码字符串的重复出现。
 *
 * @author guanyanqi
 */
public class Constants {

    /**
     * 工具类私有构造函数。
     */
    private Constants() {
    }

    /**
     * 空字符串常量。
     */
    public static final String EMPTY_STRING = "";

    /**
     * 短选项前缀字符串 "-"。
     */
    public static final String SINGLE_DASH = "-";

    /**
     * 长选项前缀 / 选项终止符字符串 "--"。
     */
    public static final String DOUBLE_DASH = "--";

    /**
     * 三连划线前缀字符串 "---"。
     */
    public static final String TRIPLE_DASH = "---";

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

    /**
     * 布尔开关 True 标记字符串。
     */
    public static final String BOOL_TRUE_STR = "true";

    /**
     * 布尔开关 False 标记字符串。
     */
    public static final String BOOL_FALSE_STR = "false";
}
