package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.annotation.Vars;
import com.guanyanqi.core.CommandDescriptor;
import com.guanyanqi.core.HelpFormatter;
import org.junit.Assert;
import org.junit.Test;

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

    @Test
    public void testHelpText() {
        QCmd qcmd = QCmd.of(new String[]{"demo", "-n", "test", "file1"});
        qcmd.parse(HelpDemoCmd.class);

        String help = qcmd.getDesc();
        Assert.assertTrue(help.contains("命令：demo"));
        Assert.assertTrue(help.contains("功能描述：示例描述"));
        Assert.assertTrue(help.contains("参数：-n|--name（必填），参数说明：名称"));
        Assert.assertTrue(help.contains("变量描述：附加文件列表"));
    }

    @Test
    public void testEmptyHelpTextBranches() {
        CommandDescriptor desc1 = new CommandDescriptor(NoDescCmd.class);
        String help1 = HelpFormatter.formatHelp(desc1);
        Assert.assertFalse(help1.contains("功能描述："));

        CommandDescriptor desc2 = new CommandDescriptor(NoVarsCmd.class);
        String help2 = HelpFormatter.formatHelp(desc2);
        Assert.assertFalse(help2.contains("变量描述："));
    }
}
