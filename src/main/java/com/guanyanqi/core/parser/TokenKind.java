package com.guanyanqi.core.parser;

/**
 * Token 解析结果的分类枚举。
 *
 * @author guanyanqi
 */
public enum TokenKind {

    /** 带值的命名选项（如 -p 8080 或 --port=8080） */
    OPTION,

    /** 布尔开关选项（无值，直接设为 "true"） */
    BOOL_FLAG,

    /** 位置变量（非选项的纯文本参数） */
    POSITIONAL,

    /** 跳过当前 token（如 "--" 终止符本身不产生值） */
    SKIP,

    /** 内置动作选项（如 --help、--version），不产生选项值 */
    ACTION
}
