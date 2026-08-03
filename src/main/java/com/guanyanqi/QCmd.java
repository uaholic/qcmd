package com.guanyanqi;

import com.guanyanqi.core.*;
import com.guanyanqi.core.parser.TokenHandlerChain;
import com.guanyanqi.exception.QCmdException;

import java.util.function.UnaryOperator;

/**
 * QCmd 命令行处理工具的核心门面类（Facade）。
 * 委托给 core 层的 CommandDescriptor, CommandLineParser, CommandValidator, InstanceBinder, HelpFormatter 处理。
 *
 * <p>推荐用法（一次性解析会话）：</p>
 * <pre>
 *     ParsedCommand&lt;DeployCmd&gt; result = QCmd.of(args).parse(DeployCmd.class);
 *     DeployCmd cmd = result.value();
 *     String help = result.helpText();
 * </pre>
 *
 * <p>自定义：</p>
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
        this.args = args == null ? null : args.clone();
    }

    /**
     * 创建 QCmd 门面实例。
     *
     * @param args 命令行入参数组
     * @return 构造好的 QCmd 实例
     */
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
        if (customizer == null) {
            throw new QCmdException("Token handler customizer must not be null");
        }
        TokenHandlerChain.Builder builder = TokenHandlerChain.builder().defaults();
        TokenHandlerChain.Builder customized = customizer.apply(builder);
        if (customized == null) {
            throw new QCmdException("Token handler customizer must not return null");
        }
        this.tokenHandlerChain = customized.build();
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
        if (formatter == null) {
            throw new QCmdException("Help formatter must not be null");
        }
        this.helpFormatter = formatter;
        return this;
    }

    /**
     * 不解析任何参数，直接为指定命令类生成默认终端帮助文本。
     *
     * @param clazz 目标命令类
     * @return 帮助文本
     */
    public static String help(Class<?> clazz) {
        return help(clazz, new TerminalHelpFormatter());
    }

    /**
     * 使用指定格式化器为命令类生成帮助文本。
     *
     * @param clazz 目标命令类
     * @param formatter 帮助文本格式化器
     * @return 帮助文本
     */
    public static String help(Class<?> clazz, HelpFormatter formatter) {
        if (formatter == null) {
            throw new QCmdException("Help formatter must not be null");
        }
        return formatter.format(new CommandDescriptor(clazz));
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
        // 内置动作跳过 required 校验，handler 已直接产出强类型 ParseAction。
        if (parseResult.action() == ParseAction.SHOW_HELP) {
            return ParsedCommand.help(helpText);
        }
        if (parseResult.action() == ParseAction.SHOW_VERSION) {
            String version = descriptor.getCmdAnnotation().version();
            String primaryName = descriptor.getCmdAnnotation().names()[0];
            return ParsedCommand.version(helpText, primaryName + " " + version);
        }

        CommandValidator.validate(parseResult, descriptor);

        T result = InstanceBinder.bind(parseResult, descriptor);
        return new ParsedCommand<>(result, helpText);
    }

}
