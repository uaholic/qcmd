package com.guanyanqi.core.parser.impl;

import com.guanyanqi.constant.Constants;
import com.guanyanqi.core.parser.*;
import com.guanyanqi.exception.MissingOptionValueException;

/**
 * 处理带值的标准选项（如 {@code -p 8080} 或 {@code --port 8080}）。
 * <p>
 * 消费当前选项名作为选项名，下一个 token 作为选项值。
 * 若已是最后一个 token 且缺少参数值，则抛出异常。
 * 若下一 token 是已声明的选项或终止符，也应报缺少值。
 * </p>
 *
 * @author guanyanqi
 */
public class StandardOptionHandler implements TokenHandler {

    @Override
    public TokenResult handle(TokenContext context, ParseState state) {
        if (state.isTerminatorSeen()) {
            return null;
        }
        String token = context.currentToken();
        if (!token.startsWith(Constants.SINGLE_DASH)) {
            return null;
        }
        if (!context.descriptor().getNameToOptionMap().containsKey(token)) {
            // 未知选项不消费后续 token，交由 CommandValidator 生成类型化异常。
            return TokenResult.option(token, Constants.EMPTY_STRING, context.currentIndex() + 1);
        }
        if (!context.hasNext()) {
            throw missingValue(context, token);
        }
        String next = context.peekNext();
        boolean nextIsRegisteredOption = context.descriptor().getNameToOptionMap().containsKey(next);
        boolean nextLooksLikeOption = next.startsWith(Constants.SINGLE_DASH)
                && !NegativeNumberHandler.isNegativeNumber(next);
        if (Constants.DOUBLE_DASH.equals(next) || nextIsRegisteredOption || nextLooksLikeOption) {
            throw missingValue(context, token);
        }
        return TokenResult.option(token, next, context.currentIndex() + 2);
    }

    private static MissingOptionValueException missingValue(TokenContext context, String optionName) {
        return new MissingOptionValueException(context.allTokens().get(0), optionName);
    }
}
