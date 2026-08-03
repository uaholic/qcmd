package com.guanyanqi.core.parser.impl;

import com.guanyanqi.constant.Constants;
import com.guanyanqi.core.parser.*;

/**
 * 处理常见的选项终止符 {@code --}。
 * <p>
 * 当遇到独立的 {@code --} token 时，设置终止符标志，
 * 告诉解析链后续的所有 token（即使以 {@code -} 开头）都强制识别为位置参数。
 * </p>
 *
 * @author guanyanqi
 */
public class TerminatorHandler implements TokenHandler {

    /**
     * 创建选项终止符处理器实例。
     */
    public TerminatorHandler() {
    }

    @Override
    public TokenResult handle(TokenContext context, ParseState state) {
        String token = context.currentToken();
        if (Constants.DOUBLE_DASH.equals(token)) {
            state.setTerminatorSeen(true);
            return TokenResult.skip(context.currentIndex() + 1);
        }
        return null;
    }
}
