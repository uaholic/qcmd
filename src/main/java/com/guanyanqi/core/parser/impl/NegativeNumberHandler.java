package com.guanyanqi.core.parser.impl;

import com.guanyanqi.core.parser.*;

/**
 * 处理看起来像负数的 token（如 {@code -3.14}、{@code -100}）。
 * <p>
 * 当一个以 {@code -} 开头且第二个字符为数字的 token 不是已声明的选项名时，
 * 将其归为位置变量而非未知选项。
 * </p>
 *
 * @author guanyanqi
 */
public class NegativeNumberHandler implements TokenHandler {

    @Override
    public TokenResult handle(TokenContext context, ParseState state) {
        if (state.isTerminatorSeen()) {
            return null;
        }
        String token = context.currentToken();
        if (!token.startsWith("-")) {
            return null;
        }
        if (!looksLikeNegativeNumber(token)) {
            return null;
        }
        // 让这一 token 已被 EqualsSignOptionHandler 或 BooleanFlagHandler 匹配
        // 只有不在已知选项中的负数才归为位置变量
        if (context.descriptor().getNameToOptionMap().containsKey(token)) {
            return null;
        }
        return TokenResult.positional(token, context.currentIndex() + 1);
    }

    private static boolean looksLikeNegativeNumber(String token) {
        if (token == null || token.length() < 2 || token.charAt(0) != '-') {
            return false;
        }
        char second = token.charAt(1);
        return second >= '0' && second <= '9';
    }
}
