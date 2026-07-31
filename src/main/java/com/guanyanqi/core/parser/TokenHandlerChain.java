package com.guanyanqi.core.parser;

import com.guanyanqi.core.CommandDescriptor;
import com.guanyanqi.core.CommandLineParser;
import com.guanyanqi.core.parser.impl.*;
import com.guanyanqi.exception.QCmdException;

import java.util.*;

/**
 * Token 处理器链——Chain of Responsibility 模式的编排器。
 * <p>
 * 按顺序调用注册的 {@link TokenHandler}，每个 handler 自行决定能否处理当前 token，
 * 若返回 null 则轮到下一个 handler；若所有 handler 都返回 null 则抛出异常。
 * </p>
 *
 * <p>推荐使用方式：
 * <pre>
 *     // 使用默认链（覆盖所有标准场景）
 *     ParseResult result = TokenHandlerChain.defaults().execute(args, descriptor);
 *
 *     // 自定义链
 *     TokenHandlerChain chain = TokenHandlerChain.builder()
 *         .append(new MyCustomHandler())
 *         .build();
 * </pre>
 * </p>
 *
 * @author guanyanqi
 */
public final class TokenHandlerChain {

    private final List<TokenHandler> handlers;

    private TokenHandlerChain(List<TokenHandler> handlers) {
        this.handlers = List.copyOf(handlers);
    }

    /**
     * 返回预构建的默认处理器链，覆盖所有标准 POSIX/GNU 解析场景。
     * <p>顺序（每个 token 按此顺序匹配）：</p>
     * <ol>
     *   <li>{@link TerminatorHandler} — "--" 终止符</li>
     *   <li>{@link EqualsSignOptionHandler} — "--key=value" 等号语法</li>
     *   <li>{@link BooleanFlagHandler} — bool 开关</li>
     *   <li>{@link NegativeNumberHandler} — 负数识别</li>
     *   <li>{@link StandardOptionHandler} — "-p 8080" 带值选项</li>
     *   <li>{@link PositionalHandler} — 位置变量兜底</li>
     * </ol>
     */
    public static TokenHandlerChain defaults() {
        return new TokenHandlerChain(List.of(
                new TerminatorHandler(),
                new EqualsSignOptionHandler(),
                new BooleanFlagHandler(),
                new NegativeNumberHandler(),
                new StandardOptionHandler(),
                new PositionalHandler()
        ));
    }

    /**
     * 创建构建器，初始为空链。
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 对给定命令行执行整个处理器链，返回 ParseResult。
     *
     * @param args       原始命令行参数
     * @param descriptor 命令描述符
     * @return 解析结果
     * @throws QCmdException 当命令名为空、不匹配、或某个 token 无法被任何 handler 处理时抛出
     */
    public CommandLineParser.ParseResult execute(String[] args, CommandDescriptor descriptor) {
        if (args == null || args.length == 0) {
            throw new QCmdException("命令行内容为空");
        }

        List<String> tokens = List.of(args);
        String cmd = tokens.get(0);

        if (!descriptor.getCommandNames().contains(cmd)) {
            throw new QCmdException("输入的命令 [" + cmd + "] 与目标类声明的命令 " + descriptor.getCommandNames() + " 不匹配");
        }

        ParseState state = new ParseState();

        // 从第 1 个 Token 开始（第 0 个是命令名）
        int i = 1;
        while (i < tokens.size()) {
            String currentToken = tokens.get(i);
            TokenContext context = new TokenContext(currentToken, tokens, i, descriptor);

            boolean handled = false;
            for (TokenHandler handler : handlers) {
                TokenResult result = handler.handle(context, state);
                if (result != null) {
                    state.apply(result);
                    i = result.nextIndex();
                    handled = true;
                    break;
                }
            }

            if (!handled) {
                // 跳过重 throw：StandardOptionHandler 已经 throw 了，但以防万一
                throw new QCmdException("无法识别的参数: " + currentToken);
            }
        }

        return new CommandLineParser.ParseResult(cmd, state.optionValues, state.positionalVars);
    }

    /**
     * {@link TokenHandlerChain} 的构建器。
     * <p>
     * 支持追加、前插、后插、替换四种操作。
     * 按类型定位的插入/替换操作始终匹配找到的第一个同类型 handler。
     * </p>
     */
    public static final class Builder {
        private final List<TokenHandler> handlers = new ArrayList<>();

        private Builder() {
        }

        /**
         * 从默认链初始化构建器（包含全部 6 个内置 handler）。
         */
        public Builder defaults() {
            handlers.clear();
            handlers.addAll(TokenHandlerChain.defaults().handlers);
            return this;
        }

        /**
         * 追加一个 handler 到链末尾。
         */
        public Builder append(TokenHandler handler) {
            handlers.add(Objects.requireNonNull(handler));
            return this;
        }

        /**
         * 前插一个 handler 到链开头。
         */
        public Builder prepend(TokenHandler handler) {
            handlers.add(0, Objects.requireNonNull(handler));
            return this;
        }

        /**
         * 在指定类型的 handler <strong>之前</strong>插入。
         * 若未找到 anchor，抛异常。
         */
        public Builder before(Class<? extends TokenHandler> anchor, TokenHandler handler) {
            int idx = indexOf(anchor);
            handlers.add(idx, Objects.requireNonNull(handler));
            return this;
        }

        /**
         * 在指定类型的 handler <strong>之后</strong>插入。
         * 若未找到 anchor，抛异常。
         */
        public Builder after(Class<? extends TokenHandler> anchor, TokenHandler handler) {
            int idx = indexOf(anchor);
            handlers.add(idx + 1, Objects.requireNonNull(handler));
            return this;
        }

        /**
         * 替换链中第一个匹配类型的 handler。
         * 若未找到 target，抛异常。
         */
        public Builder replace(Class<? extends TokenHandler> target, TokenHandler handler) {
            int idx = indexOf(target);
            handlers.set(idx, Objects.requireNonNull(handler));
            return this;
        }

        /**
         * 移除指定类型的 handler。
         * 若未找到 target，抛异常。
         */
        public Builder remove(Class<? extends TokenHandler> target) {
            handlers.remove(indexOf(target));
            return this;
        }

        /**
         * 构建不可变的 TokenHandlerChain。
         */
        public TokenHandlerChain build() {
            if (handlers.isEmpty()) {
                throw new QCmdException("处理器链不能为空");
            }
            return new TokenHandlerChain(new ArrayList<>(handlers));
        }

        private int indexOf(Class<? extends TokenHandler> target) {
            for (int i = 0; i < handlers.size(); i++) {
                if (target.isInstance(handlers.get(i))) {
                    return i;
                }
            }
            throw new QCmdException("链中未找到 " + target.getName() + " 类型的 handler");
        }
    }
}
