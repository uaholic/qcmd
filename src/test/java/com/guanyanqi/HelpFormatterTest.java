package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.annotation.Vars;
import com.guanyanqi.core.CommandDescriptor;
import com.guanyanqi.core.HelpFormatter;
import com.guanyanqi.core.MarkdownHelpFormatter;
import com.guanyanqi.core.TerminalHelpFormatter;
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
}
