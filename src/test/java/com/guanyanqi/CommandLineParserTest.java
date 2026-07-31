package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.annotation.Vars;
import com.guanyanqi.core.CommandDescriptor;
import com.guanyanqi.core.CommandLineParser;
import com.guanyanqi.exception.QCmdException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CommandLineParser 的 Token 分词与分流专项测试。
 * <p>
 * 覆盖了 8 种核心解析场景，包括标准用法和近期新增的 GNU 扩展语法：
 * <ul>
 *   <li>空参数 / null 参数的防御</li>
 *   <li>命令名不匹配的检测</li>
 *   <li>带值选项缺少参数值（末尾）的报错</li>
 *   <li>--key=value 等号语法（长选项和短选项）</li>
 *   <li>-- 终止符后的 token 全部归为位置变量</li>
 *   <li>负数 token（-3.14、-100）被正确识别为非选项的位置变量</li>
 * </ul>
 * </p>
 *
 * @author guanyanqi
 */
public class CommandLineParserTest {

    @Cmd(names = "parser-sample")
    public static class ParserSampleCmd {
        @Parameter(names = "-p")
        public String param;
    }

    @Cmd(names = "with-vars")
    public static class WithVarsCmd {
        @Parameter(names = {"-n", "--name"})
        public String name;
        @Parameter(names = {"-v", "--verbose"})
        public boolean verbose;
        @Vars
        public List<String> files;
    }

    /** null args 应抛出"命令行内容为空" */
    @Test
    public void testNullArgs() {
        CommandDescriptor desc = new CommandDescriptor(ParserSampleCmd.class);
        QCmdException e = assertThrows(QCmdException.class, () -> {
            CommandLineParser.parse(null, desc);
        });
        assertTrue(e.getMessage().contains("命令行内容为空"));
    }

    /** 空数组 args 应抛出"命令行内容为空" */
    @Test
    public void testEmptyArgs() {
        CommandDescriptor desc = new CommandDescriptor(ParserSampleCmd.class);
        QCmdException e = assertThrows(QCmdException.class, () -> {
            CommandLineParser.parse(new String[0], desc);
        });
        assertTrue(e.getMessage().contains("命令行内容为空"));
    }

    /** args[0] 与 @Cmd(names=...) 不匹配时应报错 */
    @Test
    public void testCommandNameMismatch() {
        CommandDescriptor desc = new CommandDescriptor(ParserSampleCmd.class);
        QCmdException e = assertThrows(QCmdException.class, () -> {
            CommandLineParser.parse(new String[]{"wrong-cmd"}, desc);
        });
        assertTrue(e.getMessage().contains("不匹配"));
    }

    /** 最后一个 token 是带值选项但没有提供值时抛异常 */
    @Test
    public void testMissingOptionValueAtEndOfLine() {
        CommandDescriptor desc = new CommandDescriptor(ParserSampleCmd.class);
        QCmdException e = assertThrows(QCmdException.class, () -> {
            CommandLineParser.parse(new String[]{"parser-sample", "-p"}, desc);
        });
        assertTrue(e.getMessage().contains("缺少对应的参数值"));
    }

    /** GNU 风格等号语法：--key=value 将等号前后拆分为选项名和值 */
    @Test
    public void testEqualsSignSyntax() {
        CommandDescriptor desc = new CommandDescriptor(WithVarsCmd.class);
        CommandLineParser.ParseResult result = CommandLineParser.parse(
                new String[]{"with-vars", "--name=hello"}, desc);
        assertEquals("hello", result.optionValues().get("--name"));
    }

    /** 短选项也支持等号语法：-n=world */
    @Test
    public void testEqualsSignSyntaxWithShortOption() {
        CommandDescriptor desc = new CommandDescriptor(WithVarsCmd.class);
        CommandLineParser.ParseResult result = CommandLineParser.parse(
                new String[]{"with-vars", "-n=world"}, desc);
        assertEquals("world", result.optionValues().get("-n"));
    }

    /** -- 终止符之后的 -v 和 --unknown 应全部归为位置变量，不再识别为选项 */
    @Test
    public void testDoubleHyphenTerminator() {
        CommandDescriptor desc = new CommandDescriptor(WithVarsCmd.class);
        CommandLineParser.ParseResult result = CommandLineParser.parse(
                new String[]{"with-vars", "--name", "test", "--", "-v", "--unknown"}, desc);
        assertEquals("test", result.optionValues().get("--name"));
        assertEquals(2, result.positionalVars().size());
        assertEquals("-v", result.positionalVars().get(0));
        assertEquals("--unknown", result.positionalVars().get(1));
    }

    /** -3.14 / -100 看起来像负数且不是已声明选项，应归为位置变量 */
    @Test
    public void testNegativeNumberAsPositionalVar() {
        CommandDescriptor desc = new CommandDescriptor(WithVarsCmd.class);
        CommandLineParser.ParseResult result = CommandLineParser.parse(
                new String[]{"with-vars", "-3.14", "-100"}, desc);
        assertEquals(2, result.positionalVars().size());
        assertEquals("-3.14", result.positionalVars().get(0));
        assertEquals("-100", result.positionalVars().get(1));
    }
}
