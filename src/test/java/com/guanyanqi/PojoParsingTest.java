package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.annotation.Vars;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class PojoParsingTest {

    public static class BaseCommand {
        @Parameter(names = {"-h", "--help"}, desc = "帮助选项")
        public boolean help;
    }

    @Cmd(names = {"app"}, desc = "POJO 测试指令")
    public static class AppCommand extends BaseCommand {
        @Parameter(names = {"-n", "--name"}, required = true, desc = "应用名称")
        public String name;

        @Parameter(names = {"-p", "--port"}, desc = "端口号")
        public int port = 8080;

        @Vars(desc = "主文件路径")
        public List<String> mainFiles;
    }

    @Test
    public void testPojoInheritance() {
        String[] args = new String[]{"app", "-h", "-n", "my-service", "-p", "9090", "main.java", "utils.java"};
        AppCommand cmd = QCmd.of(args).parse(AppCommand.class);

        Assert.assertTrue(cmd.help);
        Assert.assertEquals("my-service", cmd.name);
        Assert.assertEquals(9090, cmd.port);
        Assert.assertEquals(List.of("main.java", "utils.java"), cmd.mainFiles);
    }
}
