package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.annotation.Vars;
import com.guanyanqi.core.CommandDescriptor;
import com.guanyanqi.core.HelpFormatter;
import com.guanyanqi.core.MarkdownHelpFormatter;
import com.guanyanqi.core.TerminalHelpFormatter;
import com.guanyanqi.exception.QCmdException;
import com.guanyanqi.exception.UnknownOptionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HelpFormatter 帮助文本生成测试。
 * <p>
 * 覆盖了帮助文本包含/省略场景：
 * <ul>
 *   <li>正常命令的帮助文本应包含命令名、功能描述、参数说明、变量描述</li>
 *   <li>desc 为空的命令应省略"功能描述"行</li>
 *   <li>无 @Vars 的命令应省略"变量描述"行</li>
 * </ul>
 * </p>
 *
 * @author guanyanqi
 */
public class HelpFormatterTest {

    @Cmd(names = {"demo"}, desc = "示例描述")
    public record HelpDemoCmd(
            @Parameter(names = {"-n", "--name"}, required = true, desc = "名称")
            String name,

            @Vars(desc = "附加文件列表")
            String file
    ) {}

    @Cmd(names = "no-desc")
    public static class NoDescCmd {
        @Parameter(names = "-p", desc = "", valueValidDesc = "")
        public String param;

        @Vars(desc = "")
        public String var;
    }

    @Cmd(names = "no-vars")
    public static class NoVarsCmd {
        @Parameter(names = "-p")
        public String param;
    }

    @Cmd(names = "versioned", desc = "带版本命令", version = "2.4.0")
    public record VersionedCmd(
            @Parameter(names = "--required", required = true)
            String required
    ) {}

    @Cmd(names = "literal-help")
    public record LiteralHelpCmd(@Vars String value) {}

    /** 验证帮助文本包含命令名、描述、参数说明和变量描述 */
    @Test
    public void testHelpText() {
        ParsedCommand<HelpDemoCmd> parsed = QCmd.of(new String[]{"demo", "-n", "test", "file1"}).parse(HelpDemoCmd.class);

        String help = parsed.helpText();
        assertTrue(help.contains("命令：demo"));
        assertTrue(help.contains("功能描述：示例描述"));
        assertTrue(help.contains("参数：-n|--name（必填），参数说明：名称"));
        assertTrue(help.contains("变量描述：附加文件列表"));
    }

    /** desc 为空时省略"功能描述"，@Vars desc 为空时省略"变量描述" */
    @Test
    public void testEmptyHelpTextBranches() {
        TerminalHelpFormatter formatter = new TerminalHelpFormatter();

        CommandDescriptor desc1 = new CommandDescriptor(NoDescCmd.class);
        String help1 = formatter.format(desc1);
        assertFalse(help1.contains("功能描述："));

        CommandDescriptor desc2 = new CommandDescriptor(NoVarsCmd.class);
        String help2 = formatter.format(desc2);
        assertFalse(help2.contains("变量描述："));
    }

    /** Markdown 格式应包含表格、代码块等 Markdown 语法 */
    @Test
    public void testMarkdownFormatter() {
        MarkdownHelpFormatter formatter = new MarkdownHelpFormatter();
        CommandDescriptor desc = new CommandDescriptor(HelpDemoCmd.class);
        String md = formatter.format(desc);

        assertTrue(md.contains("### `demo`"));
        assertTrue(md.contains("> 示例描述"));
        assertTrue(md.contains("| 选项 |"));
        assertTrue(md.contains("| `-n, --name` |"));
    }

    /** 自定义 lambda 格式化器 */
    @Test
    public void testCustomFormatter() {
        HelpFormatter jsonFormatter = descriptor ->
                "{\"command\":\"" + descriptor.getCommandNames().iterator().next() + "\"}";

        String json = jsonFormatter.format(new CommandDescriptor(HelpDemoCmd.class));
        assertTrue(json.contains("\"command\":\"demo\""));
    }

    /** QCmd.withHelpFormatter 集成测试 */
    @Test
    public void testQCmdWithHelpFormatter() {
        ParsedCommand<HelpDemoCmd> parsed = QCmd.of(new String[]{"demo", "-n", "test", "file1"})
                .withHelpFormatter(new MarkdownHelpFormatter())
                .parse(HelpDemoCmd.class);

        String help = parsed.helpText();
        assertTrue(help.contains("### `demo`"));
        assertTrue(help.contains("| `-n, --name` |"));
    }

    /** --help 应跳过 required 校验与实例绑定，作为正常的显示动作返回。 */
    @Test
    public void testBuiltInHelpAction() {
        ParsedCommand<VersionedCmd> parsed = QCmd.of(
                new String[]{"versioned", "--help"}).parse(VersionedCmd.class);

        assertEquals(ParseAction.SHOW_HELP, parsed.action());
        assertTrue(parsed.shouldExit());
        assertNull(parsed.value());
        assertEquals(parsed.helpText(), parsed.outputText());
        assertTrue(parsed.outputText().contains("命令：versioned"));
    }

    /** 配置 @Cmd.version 后，--version 返回可直接输出的版本文本。 */
    @Test
    public void testBuiltInVersionAction() {
        ParsedCommand<VersionedCmd> parsed = QCmd.of(
                new String[]{"versioned", "--version"}).parse(VersionedCmd.class);

        assertEquals(ParseAction.SHOW_VERSION, parsed.action());
        assertTrue(parsed.shouldExit());
        assertNull(parsed.value());
        assertEquals("versioned 2.4.0", parsed.outputText());
    }

    /** 静态 help API 不需要伪造一组合法命令行参数。 */
    @Test
    public void testStandaloneHelpApi() {
        String help = QCmd.help(VersionedCmd.class);
        assertTrue(help.contains("命令：versioned"));
        assertTrue(help.contains("-V|--version"));
    }

    /** -- 终止符之后的 --help 是字面位置参数，不触发内置帮助。 */
    @Test
    public void testHelpAfterTerminatorIsPositional() {
        ParsedCommand<LiteralHelpCmd> parsed = QCmd.of(
                new String[]{"literal-help", "--", "--help"}).parse(LiteralHelpCmd.class);

        assertEquals(ParseAction.EXECUTE, parsed.action());
        assertFalse(parsed.shouldExit());
        assertEquals("--help", parsed.value().value());
    }

    /** 帮助请求也必须先匹配正确的命令名。 */
    @Test
    public void testHelpStillValidatesCommandName() {
        QCmdException e = assertThrows(QCmdException.class, () ->
                QCmd.of(new String[]{"wrong", "--help"}).parse(VersionedCmd.class));
        assertTrue(e.getMessage().contains("不匹配"));
    }

    /** 未在 @Cmd 配置 version 时，--version 仍是未知选项。 */
    @Test
    public void testVersionOptionRequiresConfiguration() {
        assertThrows(UnknownOptionException.class, () ->
                QCmd.of(new String[]{"demo", "--version"}).parse(HelpDemoCmd.class));
    }

    /** QCmd 的扩展点对 null 提供即时、明确的参数校验。 */
    @Test
    public void testNullCustomizersAreRejected() {
        assertThrows(NullPointerException.class, () ->
                QCmd.of(new String[]{"demo"}).withHelpFormatter(null));
        assertThrows(NullPointerException.class, () ->
                QCmd.of(new String[]{"demo"}).withTokenHandlers(null));
        assertThrows(NullPointerException.class, () ->
                QCmd.of(new String[]{"demo"}).withTokenHandlers(builder -> null));
    }
}
