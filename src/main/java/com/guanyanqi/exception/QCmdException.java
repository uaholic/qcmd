package com.guanyanqi.exception;

import com.guanyanqi.QCmd;

/**
 * QCmd 异常基类，继承自 {@link RuntimeException}。
 * 所有命令行解析、校验、类型转换过程中发生的异常均由此类及其子类表示。
 *
 * @author guanyanqi
 */
public class QCmdException extends RuntimeException {

    /**
     * 关联的 QCmd 实例
     */
    private QCmd qCmd;

    /**
     * 根据异常信息构造 QCmdException。
     *
     * @param message 错误描述信息
     */
    public QCmdException(String message) {
        super(message);
    }

    /**
     * 根据底层原因构造 QCmdException。
     *
     * @param cause 根本原因 Throwable
     */
    public QCmdException(Throwable cause) {
        super(cause);
    }

    /**
     * 构造带有错误信息和 QCmd 上下文的异常。
     *
     * @param message 错误描述信息
     * @param qCmd    关联的 QCmd 门面实例
     */
    public QCmdException(String message, QCmd qCmd) {
        super(message);
        this.qCmd = qCmd;
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

    /**
     * 构造带有错误信息、根本原因及 QCmd 上下文的异常。
     *
     * @param message 错误描述信息
     * @param cause   底层 Throwable 原因
     * @param qCmd    关联的 QCmd 门面实例
     */
    public QCmdException(String message, Throwable cause, QCmd qCmd) {
        super(message, cause);
        this.qCmd = qCmd;
    }

    /**
     * 设置关联的 QCmd 实例。
     *
     * @param qCmd 关联的 QCmd 实例
     */
    public void setQCmd(QCmd qCmd) {
        this.qCmd = qCmd;
    }

    /**
     * 获取关联的 QCmd 实例。
     *
     * @return 关联的 QCmd 实例
     */
    public QCmd getQCmd() {
        return qCmd;
    }
}
