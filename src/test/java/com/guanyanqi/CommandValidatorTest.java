package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.annotation.Vars;
import com.guanyanqi.exception.InvalidParameterValueException;
import com.guanyanqi.exception.MissingParameterException;
import com.guanyanqi.exception.QCmdException;
import com.guanyanqi.exception.UnknownOptionException;
import org.junit.Assert;
import org.junit.Test;

public class CommandValidatorTest {

    @Cmd(names = "valid-sample")
    public static class ValidSampleCmd {
        @Parameter(names = "-r", required = true)
        public String req;

        @Parameter(names = "-num", valueValidRegex = "^[0-9]+$", valueValidDesc = "数字")
        public String num;

        @Parameter(names = "-opt", valueValidRegex = "")
        public String opt;

        @Vars
        public String var;
    }

    @Cmd(names = "no-vars")
    public static class NoVarsCmd {
        @Parameter(names = "-p")
        public String param;
    }

    @Test
    public void testMissingRequiredParam() {
        try {
            QCmd.of(new String[]{"valid-sample"}).parse(ValidSampleCmd.class);
            Assert.fail();
        } catch (MissingParameterException e) {
            Assert.assertTrue(e.getMissingParameters().contains("-r"));
        }
    }

    @Test
    public void testInvalidRegexValue() {
        try {
            QCmd.of(new String[]{"valid-sample", "-r", "ok", "-num", "abc"}).parse(ValidSampleCmd.class);
            Assert.fail();
        } catch (InvalidParameterValueException e) {
            Assert.assertEquals("-num", e.getOptionName());
            Assert.assertEquals("abc", e.getValue());
            Assert.assertEquals("数字", e.getRuleDesc());
        }
    }

    @Test
    public void testEmptyRegexValuePass() {
        ValidSampleCmd cmd = QCmd.of(new String[]{"valid-sample", "-r", "ok", "-opt", "any"}).parse(ValidSampleCmd.class);
        Assert.assertEquals("any", cmd.opt);
    }

    @Test
    public void testUnknownOption() {
        try {
            QCmd.of(new String[]{"valid-sample", "-r", "ok", "-unknown", "val"}).parse(ValidSampleCmd.class);
            Assert.fail();
        } catch (UnknownOptionException e) {
            Assert.assertEquals("-unknown", e.getOptionName());
        }
    }

    @Test
    public void testUnexpectedPositionalVar() {
        try {
            QCmd.of(new String[]{"no-vars", "pos1"}).parse(NoVarsCmd.class);
            Assert.fail();
        } catch (QCmdException e) {
            Assert.assertTrue(e.getMessage().contains("不支持接收位置变量"));
        }
    }
}
