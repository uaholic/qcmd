package com.guanyanqi;

/**
 * 命令行解析完成后的不可变结果容器。
 * <p>
 * 封装了解析得到的目标实例和自动生成的帮助文本，避免在 {@link QCmd} 上保存可变状态。
 * </p>
 *
 * @param value    解析映射后的命令实例
 * @param helpText 自动生成的帮助说明文本
 * @param <T>      目标命令类类型
 * @author guanyanqi
 */
public record ParsedCommand<T>(T value, String helpText) {

    /**
     * 获取解析映射后的命令实例。
     *
     * @return 强类型命令实例
     */
    public T getValue() {
        return value;
    }

    /**
     * 获取自动生成的帮助说明文本。
     *
     * @return 帮助说明文本字符串
     */
    public String getHelpText() {
        return helpText;
    }
}
