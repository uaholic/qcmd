package com.guanyanqi.exception;

/**
 * 不支持的未知命令行参数选项异常。
 * 当命令行输入的参数选项未在目标接收类中声明时抛出。
 *
 * @author guanyanqi
 */
public class UnknownOptionException extends QCmdException {

    /**
     * 未知的参数选项名称
     */
    private final String optionName;

    /**
     * 构造 UnknownOptionException。
     *
     * @param commandName 命令名称
     * @param optionName  未知参数选项名称
     */
    public UnknownOptionException(String commandName, String optionName) {
        super("命令 [" + commandName + "] 不支持参数选项 [" + optionName + "]");
        this.optionName = optionName;
    }

    /**
     * 获取未知的参数选项名称。
     *
     * @return 参数选项名称
     */
    public String getOptionName() { return optionName; }
}
