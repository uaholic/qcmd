package com.guanyanqi.core.parser.impl;

import com.guanyanqi.constant.Constants;
import com.guanyanqi.core.parser.*;

/**
 * 处理位置参数（不带 {@code -} 前缀的普通非选项 token）。
 * <p>
 * 如果遇到了 {@code --} 终止符，所有后续 token（包括以 {@code -} 开头的）均按位置参数处理。
 * </p>
 *
 * @author guanyanqi
 */
public class PositionalHandler implements TokenHandler {

    /**
     * 创建位置参数处理器实例。
     */
    public PositionalHandler() {
    }

    @Override
    public TokenResult handle(TokenContext context, ParseState state) {
        String token = context.currentToken();
        if (state.isTerminatorSeen() || !token.startsWith(Constants.SINGLE_DASH)) {
            return TokenResult.positional(token, context.currentIndex() + 1);
        }
        return null;
    }
}
