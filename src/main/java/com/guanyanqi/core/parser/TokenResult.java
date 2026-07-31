package com.guanyanqi.core.parser;

/**
 * Token 处理器返回的结构化结果。
 * <p>
 * 包含处理后的选项名/值、下一个待处理位置、以及结果类型。
 * 调用方根据这些信息更新累积状态和迭代指针。
 * </p>
 *
 * @param optionName  选项名称（如 "-e"），{@link TokenKind#POSITIONAL} 时为空字符串
 * @param optionValue 选项的值，布尔开关时为 "true"，位置变量时为 token 原文
 * @param nextIndex   该 token 处理完毕后，下一个待处理 token 的下标
 * @param kind        结果分类
 * @author guanyanqi
 */
public record TokenResult(
        String optionName,
        String optionValue,
        int nextIndex,
        TokenKind kind) {

    /**
     * 创建命名选项结果（-p 8080 或 --port=8080）。
     *
     * @param name      选项名称
     * @param value     选项参数值
     * @param nextIndex 下一个待处理 token 的下标
     * @return 构造好的 TokenResult
     */
    public static TokenResult option(String name, String value, int nextIndex) {
        return new TokenResult(name, value, nextIndex, TokenKind.OPTION);
    }

    /**
     * 创建布尔开关结果（-v → "true"）。
     *
     * @param name      开关选项名称
     * @param nextIndex 下一个待处理 token 的下标
     * @return 构造好的 TokenResult
     */
    public static TokenResult boolFlag(String name, int nextIndex) {
        return new TokenResult(name, "true", nextIndex, TokenKind.BOOL_FLAG);
    }

    /**
     * 创建位置变量结果。
     *
     * @param value     位置变量文本
     * @param nextIndex 下一个待处理 token 的下标
     * @return 构造好的 TokenResult
     */
    public static TokenResult positional(String value, int nextIndex) {
        return new TokenResult("", value, nextIndex, TokenKind.POSITIONAL);
    }

    /**
     * 创建跳过结果（如 "--" 终止符本身）。
     *
     * @param nextIndex 下一个待处理 token 的下标
     * @return 构造好的 TokenResult
     */
    public static TokenResult skip(int nextIndex) {
        return new TokenResult("", "", nextIndex, TokenKind.SKIP);
    }
}
