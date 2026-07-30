package com.guanyanqi;

import com.guanyanqi.core.*;

/**
 * QCmd 命令行处理工具的核心门面类（Facade）。
 * 委托给 core 层的 CommandDescriptor, CommandLineParser, CommandValidator, InstanceBinder, HelpFormatter 处理。
 *
 * @author guanyanqi
 */
public class QCmd {

    private String[] args;
    private Object value;
    private String desc;

    /**
     * 静态工厂方法，用于创建 QCmd 实例。
     *
     * @param args 命令行参数
     * @return QCmd 实例
     */
    public static QCmd of(String[] args) {
        return new QCmd().args(args);
    }

    /**
     * 设置命令行参数并返回当前实例。
     */
    private QCmd args(String[] args) {
        this.args = args;
        return this;
    }

    /**
     * 解析命令行参数，并将解析结果装配映射到指定类的实例上（支持 POJO 与 Java Record）。
     *
     * @param <T>   目标类的类型
     * @param clazz 目标类
     * @return 映射了命令行参数的类实例
     */
    public <T> T parse(Class<T> clazz) {
        CommandDescriptor descriptor = new CommandDescriptor(clazz);
        this.desc = HelpFormatter.formatHelp(descriptor);

        CommandLineParser.ParseResult parseResult = CommandLineParser.parse(args, descriptor);
        CommandValidator.validate(parseResult, descriptor);

        T result = InstanceBinder.bind(parseResult, descriptor);
        this.value = result;
        return result;
    }

    public Object getValue() {
        return value;
    }

    public String[] getArgs() {
        return args;
    }

    /**
     * 获取生成的命令行帮助说明描述。
     */
    public String getDesc() {
        return desc;
    }
}
