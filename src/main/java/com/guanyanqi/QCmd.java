package com.guanyanqi;

import com.guanyanqi.core.*;
import com.guanyanqi.core.parser.TokenHandlerChain;

import java.util.function.UnaryOperator;

/**
 * QCmd 命令行处理工具的核心门面类（Facade）。
 * 委托给 core 层的 CommandDescriptor, CommandLineParser, CommandValidator, InstanceBinder, HelpFormatter 处理。
 *
 * <p>推荐用法（无状态）：
 * <pre>
 *     ParsedCommand&lt;DeployCmd&gt; result = QCmd.of(args).parse(DeployCmd.class);
 *     DeployCmd cmd = result.value();
 *     String help = result.helpText();
 * </pre>
 *
 * <p>自定义：
 * <pre>
 *     // 自定义 Token 处理器链
 *     QCmd.of(args).withTokenHandlers(chain -&gt; chain.append(new MyHandler()))
 *         .parse(DeployCmd.class);
 *
 *     // 自定义帮助文档格式
 *     QCmd.of(args).withHelpFormatter(new MarkdownHelpFormatter())
 *         .parse(DeployCmd.class);
 * </pre>
 *
 * @author guanyanqi
 */
public class QCmd {

    private final String[] args;
    private TokenHandlerChain tokenHandlerChain;
    private HelpFormatter helpFormatter;

    private QCmd(String[] args) {
        this.args = args;
    }

    public static QCmd of(String[] args) {
        return new QCmd(args);
    }

    /**
     * 自定义 Token 处理器链。
     *
     * @param customizer 以默认链 Builder 为输入的自定义函数
     * @return 当前 QCmd 实例
     */
    public QCmd withTokenHandlers(UnaryOperator<TokenHandlerChain.Builder> customizer) {
        TokenHandlerChain.Builder builder = TokenHandlerChain.builder().defaults();
        this.tokenHandlerChain = customizer.apply(builder).build();
        return this;
    }

    /**
     * 自定义帮助文档格式化器。
     * <p>内置实现：</p>
     * <ul>
     *   <li>{@link TerminalHelpFormatter} —— 纯文本终端风格（默认）</li>
     *   <li>{@link MarkdownHelpFormatter} —— Markdown 表格风格</li>
     * </ul>
     *
     * @param formatter 自定义格式化器
     * @return 当前 QCmd 实例
     */
    public QCmd withHelpFormatter(HelpFormatter formatter) {
        this.helpFormatter = formatter;
        return this;
    }

    /**
     * 解析命令行参数，并将解析结果装配映射到指定类的实例上（支持 POJO 与 Java Record）。
     *
     * @param <T>   目标类的类型
     * @param clazz 目标类
     * @return 包含映射实例和帮助文本的解析结果
     */
    public <T> ParsedCommand<T> parse(Class<T> clazz) {
        CommandDescriptor descriptor = new CommandDescriptor(clazz);

        HelpFormatter formatter = helpFormatter != null ? helpFormatter : new TerminalHelpFormatter();
        String helpText = formatter.format(descriptor);

        TokenHandlerChain chain = tokenHandlerChain != null ? tokenHandlerChain : TokenHandlerChain.defaults();
        CommandLineParser.ParseResult parseResult = chain.execute(args, descriptor);
        CommandValidator.validate(parseResult, descriptor);

        T result = InstanceBinder.bind(parseResult, descriptor);
        return new ParsedCommand<>(result, helpText);
    }
}
