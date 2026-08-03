package com.guanyanqi.core.parser.impl;

import com.guanyanqi.core.parser.*;

import java.util.Set;

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

    private static final Set<String> HELP_NAMES = Set.of("-h", "--help");
    private static final Set<String> VERSION_NAMES = Set.of("-V", "--version");

    @Override
    public TokenResult handle(TokenContext context, ParseState state) {
        if (state.isTerminatorSeen()) {
            return null;
        }
        String token = context.currentToken();

        if (HELP_NAMES.contains(token)) {
            if (!declaresAnyOption(context, HELP_NAMES)) {
                return TokenResult.action(token, context.allTokens().size());
            }
        }

        if (VERSION_NAMES.contains(token)) {
            boolean versionConfigured = !context.descriptor().getCmdAnnotation().version().isBlank();
            if (!declaresAnyOption(context, VERSION_NAMES) && versionConfigured) {
                return TokenResult.action(token, context.allTokens().size());
            }
        }

        return null;
    }

    private static boolean declaresAnyOption(TokenContext context, Set<String> names) {
        for (String name : names) {
            if (context.descriptor().getNameToOptionMap().containsKey(name)) {
                return true;
            }
        }
        return false;
    }
}
