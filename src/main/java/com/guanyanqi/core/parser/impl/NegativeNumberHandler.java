package com.guanyanqi.core.parser.impl;

import com.guanyanqi.constant.Constants;
import com.guanyanqi.core.parser.*;

import java.math.BigDecimal;

/**
 * 负数位置参数防误判处理器。
 * <p>
 * 当 token 匹配负数格式（如 {@code -5}、{@code -3.14}）且未注册为已知选项时，
 * 将其排除在选项识别之外，避免将负数误判为未知选项。
 * </p>
 *
 * @author guanyanqi
 */
public class NegativeNumberHandler implements TokenHandler {

    /**
     * 创建负数处理器实例。
     */
    public NegativeNumberHandler() {
    }

    @Override
    public TokenResult handle(TokenContext context, ParseState state) {
        if (state.isTerminatorSeen()) {
            return null;
        }
        String token = context.currentToken();
        if (!token.startsWith(Constants.SINGLE_DASH) || token.length() <= 1) {
            return null;
        }
        if (isNegativeNumber(token)) {
            boolean isRegisteredOption = context.descriptor().getNameToOptionMap().containsKey(token);
            if (!isRegisteredOption) {
                return TokenResult.positional(token, context.currentIndex() + 1);
            }
        }
        return null;
    }

    /**
     * 判断 token 是否为十进制负数（包括小数与科学计数法）。
     *
     * @param token 待检查 token
     * @return 是负数时返回 true
     */
    public static boolean isNegativeNumber(String token) {
        if (token == null || token.length() < 2 || token.charAt(0) != '-') {
            return false;
        }
        try {
            new BigDecimal(token);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
