package com.guanyanqi.core.parser.impl;

import com.guanyanqi.constant.Constants;
import com.guanyanqi.core.parser.*;
import com.guanyanqi.exception.QCmdException;

/**
 * 处理带值的标准选项（如 {@code -p 8080} 或 {@code --port 8080}）。
 * <p>
 * 消费当前选项名作为选项名，下一个 token 作为选项值。
 * 若已是最后一个 token 且缺少参数值，则抛出异常。
 * </p>
 *
 * @author guanyanqi
 */
public class StandardOptionHandler implements TokenHandler {

    /**
     * 创建标准带值选项处理器实例。
     */
    public StandardOptionHandler() {
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
        if (!context.hasNext()) {
            throw new QCmdException("参数选项 [" + token + "] 缺少对应的参数值");
        }
        String next = context.peekNext();
        return TokenResult.option(token, next, context.currentIndex() + 2);
    }
}
