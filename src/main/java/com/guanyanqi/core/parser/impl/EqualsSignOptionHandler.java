package com.guanyanqi.core.parser.impl;

import com.guanyanqi.constant.Constants;
import com.guanyanqi.core.parser.*;

/**
 * 处理 GNU 风格的等号分隔选项（{@code --key=value} 或 {@code -k=value}）。
 * <p>
 * 将等号前后拆分为选项名和选项值，无需消费后续 token。
 * </p>
 *
 * @author guanyanqi
 */
public class EqualsSignOptionHandler implements TokenHandler {

    /**
     * 创建等号分隔选项处理器实例。
     */
    public EqualsSignOptionHandler() {
    }

    @Override
    public TokenResult handle(TokenContext context, ParseState state) {
        if (state.isTerminatorSeen()) {
            return null;
        }
        String token = context.currentToken();
        if (!token.startsWith(Constants.SINGLE_DASH) || token.startsWith(Constants.TRIPLE_DASH)) {
            return null;
        }
        int eqIdx = token.indexOf(Constants.COMMON_KV_SPLIT_REG);
        if (eqIdx <= 1) {
            return null;
        }
        String optName = token.substring(0, eqIdx);
        String optValue = token.substring(eqIdx + 1);
        return TokenResult.option(optName, optValue, context.currentIndex() + 1);
    }
}
