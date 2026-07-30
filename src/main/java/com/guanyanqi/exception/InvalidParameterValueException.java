package com.guanyanqi.exception;

/**
 * 参数值正则表达式校验失败异常。
 * 当参数值不符合注解声明的 {@code valueValidRegex} 正则表达式匹配规则时抛出。
 *
 * @author guanyanqi
 */
public class InvalidParameterValueException extends QCmdException {

    /**
     * 校验失败的参数选项名称
     */
    private final String optionName;

    /**
     * 输入的错误参数值
     */
    private final String value;

    /**
     * 校验规则说明提示
     */
    private final String ruleDesc;

    /**
     * 构造 InvalidParameterValueException。
     *
     * @param commandName 命令名称
     * @param optionName  参数选项名称
     * @param value       输入值
     * @param ruleDesc    规则提示
     */
    public InvalidParameterValueException(String commandName, String optionName, String value, String ruleDesc) {
        super("命令 [" + commandName + "] 参数 [" + optionName + "] 的值 [" + value + "] 校验失败。" +
                (ruleDesc != null && !ruleDesc.isEmpty() ? "规则说明：" + ruleDesc : ""));
        this.optionName = optionName;
        this.value = value;
        this.ruleDesc = ruleDesc;
    }

    /**
     * 获取校验失败的参数选项名称。
     *
     * @return 参数选项名称
     */
    public String getOptionName() { return optionName; }

    /**
     * 获取输入的错误参数值。
     *
     * @return 输入值
     */
    public String getValue() { return value; }

    /**
     * 获取校验规则提示说明。
     *
     * @return 规则说明
     */
    public String getRuleDesc() { return ruleDesc; }
}
