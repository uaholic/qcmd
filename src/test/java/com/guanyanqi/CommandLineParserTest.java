package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.annotation.Vars;
import com.guanyanqi.core.CommandDescriptor;
import com.guanyanqi.core.CommandLineParser;
import com.guanyanqi.core.parser.impl.NegativeNumberHandler;
import com.guanyanqi.exception.MissingOptionValueException;
import com.guanyanqi.exception.QCmdException;
import com.guanyanqi.exception.UnknownOptionException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CommandLineParser 的 Token 分词与分流专项测试。
 * <p>
 * 覆盖核心解析场景，包括空格分隔选项与等号分隔选项：
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

    @Cmd(names = "without-vars")
    public static class WithoutVarsCmd {
        @Parameter(names = {"-v", "--verbose"})
        public boolean verbose;
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
        MissingOptionValueException e = assertThrows(MissingOptionValueException.class, () -> {
            CommandLineParser.parse(new String[]{"parser-sample", "-p"}, desc);
        });
        assertEquals("-p", e.getOptionName());
        assertTrue(e.getMessage().contains("缺少对应的参数值"));
    }

    /** 带值选项后紧跟另一个已声明选项时，应报告前一个选项缺值，不能吞掉后一个选项 */
    @Test
    public void testMissingOptionValueBeforeAnotherOption() {
        CommandDescriptor desc = new CommandDescriptor(WithVarsCmd.class);

        MissingOptionValueException e = assertThrows(MissingOptionValueException.class, () -> {
            CommandLineParser.parse(
                    new String[]{"with-vars", "--name", "--verbose"}, desc);
        });

        assertEquals("--name", e.getOptionName());
        assertTrue(e.getMessage().contains("--name"));
        assertTrue(e.getMessage().contains("缺少对应的参数值"));
    }

    /** 负数即使以 '-' 开头，仍可作为带值选项的参数值。 */
    @Test
    public void testNegativeNumberAsOptionValue() {
        CommandDescriptor desc = new CommandDescriptor(WithVarsCmd.class);
        CommandLineParser.ParseResult result = CommandLineParser.parse(
                new String[]{"with-vars", "--name", "-1e3"}, desc);

        assertEquals("-1e3", result.optionValues().get("--name"));
    }

    /** 未知选项在行尾应被 CommandValidator 捕获为 UnknownOptionException。 */
    @Test
    public void testUnknownOptionAtEndOfLine() {
        UnknownOptionException e = assertThrows(UnknownOptionException.class, () ->
                QCmd.of(new String[]{"with-vars", "--unknown"}).parse(WithVarsCmd.class));
        assertEquals("--unknown", e.getOptionName());
    }

    /** 向调用方暴露的解析结果必须是只读快照。 */
    @Test
    public void testParseResultIsImmutable() {
        CommandDescriptor desc = new CommandDescriptor(WithVarsCmd.class);
        CommandLineParser.ParseResult result = CommandLineParser.parse(
                new String[]{"with-vars", "--name", "hello", "file.txt"}, desc);

        assertThrows(UnsupportedOperationException.class,
                () -> result.optionValues().put("--name", "changed"));
        assertThrows(UnsupportedOperationException.class,
                () -> result.positionalVars().add("another.txt"));
    }

    /** 负数检测覆盖小数、科学计数法与非数字边界。 */
    @Test
    public void testNegativeNumberDetectionBoundaries() {
        assertTrue(NegativeNumberHandler.isNegativeNumber("-.5"));
        assertTrue(NegativeNumberHandler.isNegativeNumber("-1e3"));
        assertFalse(NegativeNumberHandler.isNegativeNumber(null));
        assertFalse(NegativeNumberHandler.isNegativeNumber("-"));
        assertFalse(NegativeNumberHandler.isNegativeNumber("--name"));
    }

    /** 同一属性的多个别名同时出现时，按命令行顺序使用最后一个值。 */
    @Test
    public void testLastAliasValueWinsDeterministically() {
        WithVarsCmd cmd = QCmd.of(new String[]{
                "with-vars", "--name", "first", "-n", "second"
        }).parse(WithVarsCmd.class).value();

        assertEquals("second", cmd.name);
    }

    /** QCmd.of 捕获输入快照，不受调用方后续修改原数组的影响。 */
    @Test
    public void testInputArgumentsAreSnapshotted() {
        String[] args = {"with-vars", "--name", "before"};
        QCmd session = QCmd.of(args);
        args[2] = "after";

        assertEquals("before", session.parse(WithVarsCmd.class).value().name);
    }

    /** 等号分隔写法：--key=value 将等号前后拆分为选项名和值。 */
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

    /** 有 @Vars 时，显式布尔值应被开关消费，不能混入位置变量。 */
    @Test
    public void testExplicitBooleanValuesWithVars() {
        WithVarsCmd falseCmd = QCmd.of(new String[]{
                "with-vars", "--verbose", "false", "false.txt"
        }).parse(WithVarsCmd.class).value();
        WithVarsCmd trueCmd = QCmd.of(new String[]{
                "with-vars", "--verbose", "true", "true.txt"
        }).parse(WithVarsCmd.class).value();

        assertFalse(falseCmd.verbose);
        assertEquals(List.of("false.txt"), falseCmd.files);
        assertTrue(trueCmd.verbose);
        assertEquals(List.of("true.txt"), trueCmd.files);
    }

    /** 没有 @Vars 时，显式 true / false 不应触发“命令不支持位置变量”。 */
    @Test
    public void testExplicitBooleanValuesWithoutVars() {
        WithoutVarsCmd falseCmd = QCmd.of(new String[]{
                "without-vars", "--verbose", "false"
        }).parse(WithoutVarsCmd.class).value();
        WithoutVarsCmd trueCmd = QCmd.of(new String[]{
                "without-vars", "--verbose", "true"
        }).parse(WithoutVarsCmd.class).value();

        assertFalse(falseCmd.verbose);
        assertTrue(trueCmd.verbose);
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
