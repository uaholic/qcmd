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
     * 提取目标类的字段/组件元数据并注册到 CommandDescriptor
     */
    void extractMetadata(Class<?> targetClass, CommandDescriptor descriptor);

    /**
     * 根据解析出的命令行 Token 绑定并构造目标类实例
     */
    <T> T bindInstance(CommandLineParser.ParseResult parseResult, CommandDescriptor descriptor, Class<T> targetClass) throws Exception;
}
