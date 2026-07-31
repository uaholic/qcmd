package com.guanyanqi.core.parser;

/**
 * 单个 token 的处理器接口（Chain of Responsibility 模式）。
 * <p>
 * 每个实现负责判断能否处理当前 token，能则返回处理结果，不能则返回 null
 * 交由链中的下一个处理器继续尝试。
 * </p>
 *
 * <p>标有 {@link FunctionalInterface}，简单场景可直接用 lambda。
 * 但需要配合 {@link TokenHandlerChain.Builder#before} / {@link Builder#replace}
 * 按类型定位的高级用法时，请用具名类。</p>
 *
 * @author guanyanqi
 */
@FunctionalInterface
public interface TokenHandler {

    /**
     * 尝试处理当前 token。
     *
     * @param context   当前 token 的不可变上下文
     * @param parseState 解析过程中的可变累积状态
     * @return 处理成功返回 TokenResult；不处理返回 null
     */
    TokenResult handle(TokenContext context, ParseState parseState);
}
