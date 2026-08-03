package com.guanyanqi.core.parser.impl;

import com.guanyanqi.ParseAction;
import com.guanyanqi.core.parser.*;

/**
 * 内置动作选项处理器（{@code --help} 和 {@code --version}）。
 * <p>
 * 检测未被用户自定义覆盖的内置动作选项，将其记录为 ACTION 结果。
 * 该 handler 放在 TerminatorHandler 之后、EqualsSignOptionHandler 之前，
 * 确保在等号语法和标准选项之前优先匹配。
 * </p>
 *
 * @author guanyanqi
 */
public class BuiltInActionHandler implements TokenHandler {

    @Override
    public TokenResult handle(TokenContext context, ParseState state) {
        if (state.isTerminatorSeen()) {
            return null;
        }
        String token = context.currentToken();
        ParseAction action = ParseAction.fromOptionName(token);
        if (action == ParseAction.EXECUTE) {
            return null;
        }

        if (declaresAnyOption(context, action)) {
            return null;
        }

        if (action == ParseAction.SHOW_VERSION) {
            boolean versionConfigured = !context.descriptor().getCmdAnnotation().version().isBlank();
            if (!versionConfigured) {
                return null;
            }
        }

        return TokenResult.action(token, action, context.allTokens().size());
    }

    private static boolean declaresAnyOption(TokenContext context, ParseAction action) {
        for (String name : action.optionNames()) {
            if (context.descriptor().getNameToOptionMap().containsKey(name)) {
                return true;
            }
        }
        return false;
    }
}
