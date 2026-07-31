package com.guanyanqi.core.parser.impl;

import com.guanyanqi.core.parser.*;

/**
 * 处理 POSIX {@code --} 终止符。
 * <p>
 * 遇到 {@code --} 时设置终止标志，后续所有 token 均由 {@link PositionalHandler} 处理，
 * 不再作为选项解析。
 * </p>
 *
 * @author guanyanqi
 */
public class TerminatorHandler implements TokenHandler {

    @Override
    public TokenResult handle(TokenContext context, ParseState state) {
        if (!"--".equals(context.currentToken()) || state.isTerminatorSeen()) {
            return null;
        }
        state.setTerminatorSeen(true);
        return TokenResult.skip(context.currentIndex() + 1);
    }
}
