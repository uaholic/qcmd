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

    /**
     * 工具类私有构造函数。
     */
    private CommandBindingStrategyFactory() {
    }

    /**
     * 根据目标 Class 是否为 Record 动态路由选择对应的绑定策略实现。
     *
     * @param targetClass 目标 Class
     * @return 对应的 CommandBindingStrategy 实例
     */
    public static CommandBindingStrategy getStrategy(Class<?> targetClass) {
        if (targetClass.isRecord()) {
            return RECORD_STRATEGY;
        }
        return POJO_STRATEGY;
    }
}
