package com.guanyanqi;

/**
 * 命令行解析完成后的不可变结果容器。
 * <p>
 * 封装解析得到的目标实例、帮助文本和本次请求应执行的动作。
 * 对于 {@code --help} 和已配置版本的 {@code --version} 请求，
 * {@link #value()} 为 {@code null}，调用方应输出 {@link #outputText()} 后正常退出。
 * </p>
 *
 * @param value    解析映射后的命令实例
 * @param helpText 自动生成的帮助说明文本
 * @param action   本次解析请求对应的动作
 * @param outputText 建议直接向用户输出的文本；正常执行时为空字符串
 * @param <T>      目标命令类类型
 * @author guanyanqi
 */
public record ParsedCommand<T>(T value, String helpText, ParseAction action, String outputText) {

    /**
     * 保留 1.0.x 的两参构造方法，便于既有代码平滑升级。
     *
     * @param value 解析绑定后的命令实例
     * @param helpText 帮助文本
     */
    public ParsedCommand(T value, String helpText) {
        this(value, helpText, ParseAction.EXECUTE, "");
    }

    /**
     * 构建帮助请求结果。
     */
    static <T> ParsedCommand<T> help(String helpText) {
        return new ParsedCommand<>(null, helpText, ParseAction.SHOW_HELP, helpText);
    }

    /**
     * 构建版本请求结果。
     */
    static <T> ParsedCommand<T> version(String helpText, String versionText) {
        return new ParsedCommand<>(null, helpText, ParseAction.SHOW_VERSION, versionText);
    }

    /**
     * 是否应输出文本后退出，而不是执行命令业务。
     *
     * @return 帮助或版本请求返回 true，普通执行返回 false
     */
    public boolean shouldExit() {
        return action != ParseAction.EXECUTE;
    }

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
