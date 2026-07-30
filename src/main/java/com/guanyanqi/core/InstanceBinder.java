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
