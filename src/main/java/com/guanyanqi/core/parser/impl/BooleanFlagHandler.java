package com.guanyanqi.core.parser.impl;

import com.guanyanqi.constant.Constants;
import com.guanyanqi.core.parser.*;

/**
 * 处理布尔类型的开关选项（如 {@code -v} → "true"、{@code --verbose false} → "false"）。
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
        if (!token.startsWith(Constants.SINGLE_DASH)) {
            return null;
        }
        if (context.descriptor().getBoolOptionNames().contains(token)) {
            if (context.hasNext() && isBooleanLiteral(context.peekNext())) {
                return TokenResult.boolFlag(token, context.peekNext(), context.currentIndex() + 2);
            }
            return TokenResult.boolFlag(token, context.currentIndex() + 1);
        }
        return null;
    }

    private static boolean isBooleanLiteral(String value) {
        return Constants.BOOL_TRUE_STR.equals(value) || Constants.BOOL_FALSE_STR.equals(value);
    }
}
