package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.annotation.Vars;
import com.guanyanqi.core.parser.*;
import com.guanyanqi.core.parser.impl.*;
import com.guanyanqi.core.CommandDescriptor;
import com.guanyanqi.core.CommandLineParser;
import com.guanyanqi.exception.QCmdException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试 TokenHandler 链的扩展性：自定义 handler、插入、替换、移除。
 *
 * @author guanyanqi
 */
public class TokenHandlerChainTest {

    @Cmd(names = "test")
    public record TestCmd(
            @Parameter(names = {"-n", "--name"})
            String name,
            @Parameter(names = {"-v", "--verbose"})
            boolean verbose,
            @Vars
            List<String> files
    ) {}

    // ---- 自定义 TokenHandler 测试 ----

    /**
     * 自定义 handler：将 @@env:VAR@@ 替换为环境变量的值。
     */
    static class EnvVarExpansionHandler implements TokenHandler {
        @Override
        public TokenResult handle(TokenContext context, ParseState state) {
            if (state.isTerminatorSeen()) return null;
            String token = context.currentToken();
            if (token.startsWith("@@") && token.endsWith("@@")) {
                String varName = token.substring(2, token.length() - 2);
                String expanded = System.getenv().getOrDefault(varName, "");
                return TokenResult.positional(expanded, context.currentIndex() + 1);
            }
            return null;
        }
    }

    /**
     * 自定义 handler：将 Windows 风格前缀 /opt 转为 --opt。
     */
    static class WindowsStyleHandler implements TokenHandler {
        @Override
        public TokenResult handle(TokenContext context, ParseState state) {
            if (state.isTerminatorSeen()) return null;
            String token = context.currentToken();
            if (token.startsWith("/") && token.length() > 1 && !token.contains("=")) {
                String normalized = "-" + token.substring(1);
                if (context.hasNext()) {
                    String value = context.peekNext();
                    return TokenResult.option(normalized, value, context.currentIndex() + 2);
                }
                return TokenResult.boolFlag(normalized, context.currentIndex() + 1);
            }
            return null;
        }
    }

    @Test
    public void testAppendCustomHandler() {
        // 使用自定义 chain：追加 EnvVarExpansionHandler
        TokenHandlerChain chain = TokenHandlerChain.builder()
                .defaults()
                .append(new EnvVarExpansionHandler())
                .build();

        CommandDescriptor desc = new CommandDescriptor(TestCmd.class);
        CommandLineParser.ParseResult result = chain.execute(
                new String[]{"test", "@@PATH@@", "-v"}, desc);

        // @@PATH@@ 应被自定义 handler 展开为环境变量值（非空）
        assertFalse(result.positionalVars().isEmpty());
        assertEquals("true", result.optionValues().get("-v"));
    }

    @Test
    public void testPrependCustomHandler() {
        // 前插 Windows 风格 handler：在默认处理器之前匹配 /opt
        TokenHandlerChain chain = TokenHandlerChain.builder()
                .defaults()
                .prepend(new WindowsStyleHandler())
                .build();

        CommandDescriptor desc = new CommandDescriptor(TestCmd.class);
        CommandLineParser.ParseResult result = chain.execute(
                new String[]{"test", "/n", "hello"}, desc);

        // /n hello 应被转换为 -n=hello
        assertEquals("hello", result.optionValues().get("-n"));
    }

    @Test
    public void testReplaceHandler() {
        // 用自定义 handler 替换 BooleanFlagHandler：让 bool 开关设 "YES" 而非 "true"
        TokenHandlerChain chain = TokenHandlerChain.builder()
                .defaults()
                .replace(BooleanFlagHandler.class, (ctx, state) -> {
                    if (state.isTerminatorSeen()) return null;
                    String token = ctx.currentToken();
                    if (!token.startsWith("-")) return null;
                    if (ctx.descriptor().getBoolOptionNames().contains(token)) {
                        return TokenResult.boolFlag(token, ctx.currentIndex() + 1);
                    }
                    return null;
                })
                .build();

        CommandDescriptor desc = new CommandDescriptor(TestCmd.class);
        CommandLineParser.ParseResult result = chain.execute(
                new String[]{"test", "-v"}, desc);

        assertEquals("true", result.optionValues().get("-v"));
    }

    @Test
    public void testRemoveHandler() {
        // 移除 NegativeNumberHandler：负数重新被当作未知选项
        TokenHandlerChain chain = TokenHandlerChain.builder()
                .defaults()
                .remove(NegativeNumberHandler.class)
                .build();

        CommandDescriptor desc = new CommandDescriptor(TestCmd.class);
        // -3.14 不再被识别为负数，会被 StandardOptionHandler 消费下一个 token "hello"
        // 作为其参数值，导致 "hello" 不再成为 positional var
        CommandLineParser.ParseResult result = chain.execute(
                new String[]{"test", "-n", "world", "-3.14", "hello"}, desc);

        // -3.14 被当作未知选项处理，hello 被当作其值
        assertEquals("world", result.optionValues().get("-n"));
        assertEquals("hello", result.optionValues().get("-3.14"));
    }

    @Test
    public void testBeforeHandler() {
        // 在 StandardOptionHandler 之前插入 handler：拦截特定的选项值并修改
        AtomicReference<String> intercepted = new AtomicReference<>();
        TokenHandlerChain chain = TokenHandlerChain.builder()
                .defaults()
                .before(StandardOptionHandler.class, (ctx, state) -> {
                    String token = ctx.currentToken();
                    // 如果 -n 的值是 "secret" 则替换为 "blocked"
                    if ("-n".equals(token) && ctx.hasNext() && "secret".equals(ctx.peekNext())) {
                        intercepted.set("blocked");
                        return TokenResult.option(token, "blocked", ctx.currentIndex() + 2);
                    }
                    return null;
                })
                .build();

        CommandDescriptor desc = new CommandDescriptor(TestCmd.class);
        CommandLineParser.ParseResult result = chain.execute(
                new String[]{"test", "-n", "secret", "-v"}, desc);

        assertEquals("blocked", intercepted.get());
        // -n 的值被替换为 "blocked"
        assertEquals("blocked", result.optionValues().get("-n"));
        assertEquals("true", result.optionValues().get("-v"));
    }

    @Test
    public void testHandlerExecutionOrder() {
        // 验证 handler 按顺序执行：prepend 的 handler 优先于 append
        // 用一个不以 - 开头的 token "hello"，它可以经过多个 handler
        // prepend handler 被加到 EqualsSignOptionHandler 之前，
        // append handler 被加到 PositionalHandler 之前
        AtomicInteger order = new AtomicInteger(0);
        AtomicInteger earlySeen = new AtomicInteger(-1);
        AtomicInteger lateSeen = new AtomicInteger(-1);

        TokenHandler early = (ctx, state) -> {
            if (!ctx.currentToken().startsWith("-") && !state.isTerminatorSeen()) {
                earlySeen.set(order.incrementAndGet());
            }
            return null;
        };
        TokenHandler late = (ctx, state) -> {
            if (!ctx.currentToken().startsWith("-") && !state.isTerminatorSeen()) {
                lateSeen.set(order.incrementAndGet());
            }
            return null;
        };

        TokenHandlerChain chain = TokenHandlerChain.builder()
                .defaults()
                .prepend(early)       // 在 chain 最前面
                .before(PositionalHandler.class, late)  // 在 PositionalHandler 之前
                .build();

        CommandDescriptor desc = new CommandDescriptor(TestCmd.class);
        chain.execute(new String[]{"test", "hello"}, desc);

        assertTrue(earlySeen.get() > 0, "early handler should have been called");
        assertTrue(lateSeen.get() > 0, "late handler should have been called");
        assertTrue(earlySeen.get() < lateSeen.get(),
                "early handler should execute before late handler: got early="
                        + earlySeen.get() + " late=" + lateSeen.get());
    }

    @Test
    public void testEmptyChainThrowsException() {
        QCmdException e = assertThrows(QCmdException.class, () -> {
            TokenHandlerChain.builder().build();
        });
        assertTrue(e.getMessage().contains("不能为空"));
    }

    @Test
    public void testReplaceMissingHandlerThrowsException() {
        // NegativeNumberHandler 已经被移除后再次 replace 应抛异常
        QCmdException e = assertThrows(QCmdException.class, () -> {
            TokenHandlerChain.builder()
                    .defaults()
                    .remove(NegativeNumberHandler.class)
                    .replace(NegativeNumberHandler.class, new NegativeNumberHandler())
                    .build();
        });
        assertTrue(e.getMessage().contains("未找到"));
    }

    // ---- QCmd.withTokenHandlers() 集成测试 ----

    @Test
    public void testQCmdWithTokenHandlers() {
        // 通过 QCmd 的 withTokenHandlers API 使用自定义 Windows 风格 handler
        ParsedCommand<TestCmd> result = QCmd.of(new String[]{"test", "/n", "hello"})
                .withTokenHandlers(chain -> chain
                        .prepend(new WindowsStyleHandler()))
                .parse(TestCmd.class);

        assertEquals("hello", result.value().name());
    }

    @Test
    public void testQCmdDefaultChainUnchanged() {
        // 不调用 withTokenHandlers 时行为不变
        ParsedCommand<TestCmd> result = QCmd.of(new String[]{
                "test", "-n", "world", "-v", "file.txt"
        }).parse(TestCmd.class);

        assertEquals("world", result.value().name());
        assertTrue(result.value().verbose());
        assertEquals(1, result.value().files().size());
        assertEquals("file.txt", result.value().files().get(0));
    }
}
