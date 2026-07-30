package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.core.CommandDescriptor;
import com.guanyanqi.core.CommandLineParser;
import com.guanyanqi.exception.QCmdException;
import org.junit.Assert;
import org.junit.Test;

public class CommandLineParserTest {

    @Cmd(names = "parser-sample")
    public static class ParserSampleCmd {
        @Parameter(names = "-p")
        public String param;
    }

    @Test
    public void testNullArgs() {
        try {
            CommandDescriptor desc = new CommandDescriptor(ParserSampleCmd.class);
            CommandLineParser.parse(null, desc);
            Assert.fail();
        } catch (QCmdException e) {
            Assert.assertTrue(e.getMessage().contains("命令行内容为空"));
        }
    }

    @Test
    public void testEmptyArgs() {
        try {
            CommandDescriptor desc = new CommandDescriptor(ParserSampleCmd.class);
            CommandLineParser.parse(new String[0], desc);
            Assert.fail();
        } catch (QCmdException e) {
            Assert.assertTrue(e.getMessage().contains("命令行内容为空"));
        }
    }

    @Test
    public void testCommandNameMismatch() {
        try {
            CommandDescriptor desc = new CommandDescriptor(ParserSampleCmd.class);
            CommandLineParser.parse(new String[]{"wrong-cmd"}, desc);
            Assert.fail();
        } catch (QCmdException e) {
            Assert.assertTrue(e.getMessage().contains("不匹配"));
        }
    }

    @Test
    public void testMissingOptionValueAtEndOfLine() {
        try {
            CommandDescriptor desc = new CommandDescriptor(ParserSampleCmd.class);
            CommandLineParser.parse(new String[]{"parser-sample", "-p"}, desc);
            Assert.fail();
        } catch (QCmdException e) {
            Assert.assertTrue(e.getMessage().contains("缺少对应的参数值"));
        }
    }
}
