package com.guanyanqi.core.strategy;

/**
 * 命令绑定策略工厂类。
 * 根据 Target Class 类型动态路由器选择相应的策略实现。
 *
 * @author guanyanqi
 */
public class CommandBindingStrategyFactory {

    private static final CommandBindingStrategy POJO_STRATEGY = new PojoBindingStrategy();
    private static final CommandBindingStrategy RECORD_STRATEGY = new RecordBindingStrategy();

    public static CommandBindingStrategy getStrategy(Class<?> targetClass) {
        if (targetClass.isRecord()) {
            return RECORD_STRATEGY;
        }
        return POJO_STRATEGY;
    }
}
