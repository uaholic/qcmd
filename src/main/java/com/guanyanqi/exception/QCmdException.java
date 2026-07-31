package com.guanyanqi.exception;

/**
 * QCmd 异常基类，继承自 {@link RuntimeException}。
 * 所有命令行解析、校验、类型转换过程中发生的异常均由此类及其子类表示。
 *
 * @author guanyanqi
 */
public class QCmdException extends RuntimeException {

    /**
     * 根据异常信息构造 QCmdException。
     *
     * @param message 错误描述信息
     */
    public QCmdException(String message) {
        super(message);
    }

    /**
     * 构造带有错误信息和根本原因的异常。
     *
     * @param message 错误描述信息
     * @param cause   底层 Throwable 原因
     */
    public QCmdException(String message, Throwable cause) {
        super(message, cause);
    }
}
