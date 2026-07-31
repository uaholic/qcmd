package com.guanyanqi.core.strategy;

import com.guanyanqi.core.CommandDescriptor;
import com.guanyanqi.core.CommandLineParser;

/**
 * 命令绑定策略接口（抽象 POJO 与 Record 的元数据提取和实例绑定逻辑）。
 *
 * @author guanyanqi
 */
public interface CommandBindingStrategy {

    /**
     * 提取目标类的字段/组件元数据并注册到 CommandDescriptor。
     *
     * @param targetClass 目标命令类 Class
     * @param descriptor  描述符容器
     */
    void extractMetadata(Class<?> targetClass, CommandDescriptor descriptor);

    /**
     * 根据解析出的命令行 Token 绑定并构造目标类实例。
     *
     * @param <T>         目标命令类泛型
     * @param parseResult 命令行解析中间结果
     * @param descriptor  命令描述符
     * @param targetClass 目标命令类 Class
     * @return 构造并绑定属性后的目标类实例
     * @throws Exception 当反射或类型转换失败时抛出
     */
    <T> T bindInstance(CommandLineParser.ParseResult parseResult, CommandDescriptor descriptor, Class<T> targetClass) throws Exception;
}
