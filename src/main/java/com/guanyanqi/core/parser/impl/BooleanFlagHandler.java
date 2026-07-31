package com.guanyanqi.core.parser.impl;

import com.guanyanqi.core.parser.*;

/**
 * 处理布尔类型的无值开关选项（如 {@code -v} → "true"、{@code --verbose} → "true"）。
 * <p>
 * 只处理已在 {@link com.guanyanqi.core.CommandDescriptor#getBoolOptionNames()} 中注册的选项。
 * </p>
 *
 * @author guanyanqi
 */
public class BooleanFlagHandler implements TokenHandler {

    /**
     * 创建布尔开关处理器实例。
     */
    public BooleanFlagHandler() {
    }

    @Override
    public TokenResult handle(TokenContext context, ParseState state) {
        if (state.isTerminatorSeen()) {
            return null;
        }
        String token = context.currentToken();
        if (!token.startsWith("-")) {
            return null;
        }
        if (context.descriptor().getBoolOptionNames().contains(token)) {
            return TokenResult.boolFlag(token, context.currentIndex() + 1);
        }
        return null;
    }
}
