package com.guanyanqi.exception;

/**
 * 带值选项缺少参数值异常。
 * 当选项位于命令行末尾，或其后紧跟另一个选项时抛出。
 *
 * @author guanyanqi
 */
public class MissingOptionValueException extends QCmdException {

    private final String optionName;

    /**
     * 构造 MissingOptionValueException。
     *
     * @param commandName 命令名称
     * @param optionName  缺少值的选项名称
     */
    public MissingOptionValueException(String commandName, String optionName) {
        super("命令 [" + commandName + "] 参数选项 [" + optionName + "] 缺少对应的参数值");
        this.optionName = optionName;
    }

    /**
     * 获取缺少值的选项名称。
     *
     * @return 选项名称
     */
    public String getOptionName() {
        return optionName;
    }
}
