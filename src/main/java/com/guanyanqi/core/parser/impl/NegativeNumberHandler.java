package com.guanyanqi.core.parser.impl;

import com.guanyanqi.constant.Constants;
import com.guanyanqi.core.parser.*;

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
        char c = token.charAt(1);
        if (Character.isDigit(c)) {
            boolean isRegisteredOption = context.descriptor().getNameToOptionMap().containsKey(token);
            if (!isRegisteredOption) {
                return TokenResult.positional(token, context.currentIndex() + 1);
            }
        }
        return null;
    }
}
