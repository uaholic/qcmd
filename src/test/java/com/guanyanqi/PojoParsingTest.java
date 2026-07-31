package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.annotation.Vars;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * POJO 继承场景的字段解析测试。
 * <p>
 * 验证 QCmd 的 getAllFieldsList 能够正确遍历包括父类在内的全部 Field，
 * 使得父类中声明的 @Parameter 注解在子类解析时也能正常工作。
 * </p>
 *
 * @author guanyanqi
 */
public class PojoParsingTest {

    /** 父类：声明了所有子命令共用的 --help 布尔开关 */
    public static class BaseCommand {
        @Parameter(names = {"-h", "--help"}, desc = "帮助选项")
        public boolean help;
    }

    /** 子类：继承 -h，追加 -n（required）、-p（默认值 8080）和 @Vars */
    @Cmd(names = {"app"}, desc = "POJO 测试指令")
    public static class AppCommand extends BaseCommand {
        @Parameter(names = {"-n", "--name"}, required = true, desc = "应用名称")
        public String name;

        @Parameter(names = {"-p", "--port"}, desc = "端口号")
        public int port = 8080;

        @Vars(desc = "主文件路径")
        public List<String> mainFiles;
    }

    /**
     * 验证继承链字段绑定：
     * <ul>
     *   <li>-h → 父类 help 字段为 true</li>
     *   <li>-n my-service → 子类 name 字段</li>
     *   <li>-p 9090 → 覆盖默认值 8080</li>
     *   <li>main.java utils.java → @Vars 集合</li>
     * </ul>
     */
    @Test
    public void testPojoInheritance() {
        String[] args = new String[]{"app", "-h", "-n", "my-service", "-p", "9090", "main.java", "utils.java"};
        AppCommand cmd = QCmd.of(args).parse(AppCommand.class).value();

        assertTrue(cmd.help);
        assertEquals("my-service", cmd.name);
        assertEquals(9090, cmd.port);
        assertEquals(List.of("main.java", "utils.java"), cmd.mainFiles);
    }
}
