package com.guanyanqi.core;

import com.guanyanqi.core.strategy.CommandBindingStrategy;
import com.guanyanqi.core.strategy.CommandBindingStrategyFactory;
import com.guanyanqi.exception.QCmdException;

/**
 * 属性转换与实体/Record 构造绑定器（使用策略模式多态绑定）。
 *
 * @author guanyanqi
 */
public class InstanceBinder {

    /**
     * 工具类私有构造函数。
     */
    private InstanceBinder() {
    }

    /**
     * 将解析结果多态绑定构建为目标 Class 实例。
     *
     * @param <T>         目标类型泛型
     * @param parseResult 解析中间结果
     * @param descriptor  命令描述符
     * @return 构建好的强类型命令实例
     */
    @SuppressWarnings("unchecked")
    public static <T> T bind(CommandLineParser.ParseResult parseResult, CommandDescriptor descriptor) {
        Class<T> clazz = (Class<T>) descriptor.getTargetClass();
        try {
            CommandBindingStrategy strategy = CommandBindingStrategyFactory.getStrategy(clazz);
            return strategy.bindInstance(parseResult, descriptor, clazz);
        } catch (QCmdException e) {
            throw e;
        } catch (Exception e) {
            throw new QCmdException("解析绑定 [" + clazz.getName() + "] 错误: " + e.getMessage(), e);
        }
    }
}
