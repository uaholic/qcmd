package com.guanyanqi.exception;

import java.util.List;

/**
 * 必填参数缺失异常。
 * 当命令行输入的参数中缺少注解声明为 {@code required = true} 的必填选项时抛出。
 *
 * @author guanyanqi
 */
public class MissingParameterException extends QCmdException {

    /**
     * 缺失的必填参数组选项列表
     */
    private final List<String> missingParameters;

    /**
     * 构造 MissingParameterException。
     *
     * @param commandName       主命令名称
     * @param missingParameters 缺失的必填参数选项列表
     */
    public MissingParameterException(String commandName, List<String> missingParameters) {
        super("命令 [" + commandName + "] 必填参数缺失: " + String.join(" | ", missingParameters));
        this.missingParameters = missingParameters;
    }

    /**
     * 获取缺失的必填参数选项列表。
     *
     * @return 参数名称列表
     */
    public List<String> getMissingParameters() {
        return missingParameters;
    }
}
