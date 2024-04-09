package com.guanyanqi.exception;

import com.guanyanqi.QCmd;

/**
 * QCmdException类是QCmd命令行处理过程中的自定义异常。
 * 它继承自RuntimeException，意味着它是一个非受检异常，可以在运行时被抛出而不强制调用者处理。
 *
 * 除了标准的异常消息和可选的原因（Throwable），这个异常类还可以持有一个QCmd实例，
 * 允许进一步分析或记录出现异常时的命令行状态。
 *
 * 示例用法：
 * throw new QCmdException("命令解析失败", qCmdInstance);
 * throw new QCmdException("命令执行错误", causeThrowable, qCmdInstance);
 *
 * @author guanyanqi
 */
public class QCmdException extends RuntimeException {
    // 引发异常的QCmd实例，可用于记录或分析异常时的命令行状态
    private QCmd qCmd;

    // 构造函数仅接受异常消息
    public QCmdException(String message) {
        super(message);
    }

    // 构造函数接受Throwable作为异常原因
    public QCmdException(Throwable cause) {
        super(cause);
    }

    // 构造函数接受异常消息和QCmd实例
    public QCmdException(String message, QCmd qCmd) {
        super(message);
        this.qCmd = qCmd;
    }

    // 构造函数接受异常消息和Throwable作为异常原因
    public QCmdException(String message, Throwable cause) {
        super(message, cause);
    }

    // 构造函数接受异常消息、Throwable作为异常原因和QCmd实例
    public QCmdException(String message, Throwable cause, QCmd qCmd) {
        super(message, cause);
        this.qCmd = qCmd;
    }

    // 设置引发异常的QCmd实例
    public void setQCmd(QCmd qCmd) {
        this.qCmd = qCmd;
    }

    // 获取引发异常的QCmd实例
    public QCmd getQCmd() {
        return qCmd;
    }
}
