package com.guanyanqi;

import com.guanyanqi.core.*;
import com.guanyanqi.core.parser.TokenHandlerChain;

import java.util.Objects;
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
        Objects.requireNonNull(customizer, "Token handler customizer must not be null");
        TokenHandlerChain.Builder builder = TokenHandlerChain.builder().defaults();
        TokenHandlerChain.Builder customized = Objects.requireNonNull(
                customizer.apply(builder), "Token handler customizer must not return null");
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
        this.helpFormatter = Objects.requireNonNull(formatter, "Help formatter must not be null");
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
        return Objects.requireNonNull(formatter, "Help formatter must not be null")
                .format(new CommandDescriptor(clazz));
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

        if (!declaresAnyOption(descriptor, "-h", "--help")
                && containsActionOption("-h", "--help")) {
            validateCommandName(descriptor);
            return ParsedCommand.help(helpText);
        }
        if (!declaresAnyOption(descriptor, "-V", "--version")
                && containsActionOption("-V", "--version")
                && !descriptor.getCmdAnnotation().version().isBlank()) {
            validateCommandName(descriptor);
            String primaryName = descriptor.getCmdAnnotation().names()[0];
            return ParsedCommand.version(helpText,
                    primaryName + " " + descriptor.getCmdAnnotation().version());
        }

        TokenHandlerChain chain = tokenHandlerChain != null ? tokenHandlerChain : TokenHandlerChain.defaults();
        CommandLineParser.ParseResult parseResult = chain.execute(args, descriptor);
        CommandValidator.validate(parseResult, descriptor);

        T result = InstanceBinder.bind(parseResult, descriptor);
        return new ParsedCommand<>(result, helpText);
    }

    private boolean containsActionOption(String... names) {
        if (args == null || args.length < 2) {
            return false;
        }
        for (int i = 1; i < args.length; i++) {
            if ("--".equals(args[i])) {
                return false;
            }
            for (String name : names) {
                if (name.equals(args[i])) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean declaresAnyOption(CommandDescriptor descriptor, String... names) {
        for (String name : names) {
            if (descriptor.getNameToOptionMap().containsKey(name)) {
                return true;
            }
        }
        return false;
    }

    private void validateCommandName(CommandDescriptor descriptor) {
        if (args == null || args.length == 0) {
            throw new com.guanyanqi.exception.QCmdException("命令行内容为空");
        }
        if (!descriptor.getCommandNames().contains(args[0])) {
            throw new com.guanyanqi.exception.QCmdException(
                    "输入的命令 [" + args[0] + "] 与目标类声明的命令 "
                            + descriptor.getCommandNames() + " 不匹配");
        }
    }
}
