package com.guanyanqi.core.parser.impl;

import com.guanyanqi.core.parser.*;

/**
 * 兜底处理器：将不以 {@code -} 开头的 token（或终止符之后的所有 token）
 * 归集为位置变量（Positional Variables）。
 *
 * @author guanyanqi
 */
public class PositionalHandler implements TokenHandler {

    @Override
    public TokenResult handle(TokenContext context, ParseState state) {
        // 终止符之后：无论是否以 - 开头，全部作为位置变量
        if (state.isTerminatorSeen()) {
            return TokenResult.positional(context.currentToken(), context.currentIndex() + 1);
        }
        // 非终止模式下：只处理不以 - 开头的普通 token
        String token = context.currentToken();
        if (!token.startsWith("-")) {
            return TokenResult.positional(token, context.currentIndex() + 1);
        }
        return null;
    }
}
