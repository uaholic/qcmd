package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.exception.InvalidParameterValueException;
import com.guanyanqi.exception.MissingParameterException;
import com.guanyanqi.exception.UnknownOptionException;
import org.junit.Assert;
import org.junit.Test;

public class ValidationTest {

    @Cmd(names = {"valid"}, desc = "校验测试")
    public static class ValidCmd {
        @Parameter(names = "-req", required = true)
        public String requiredField;

        @Parameter(names = "-num", valueValidRegex = "^[0-9]+$", valueValidDesc = "只能包含纯数字")
        public String numberField;
    }

    @Test
    public void testMissingParameterException() {
        try {
            QCmd.of(new String[]{"valid"}).parse(ValidCmd.class);
            Assert.fail("Should fail on missing required parameter");
        } catch (MissingParameterException e) {
            Assert.assertTrue(e.getMissingParameters().contains("-req"));
        }
    }

    @Test
    public void testInvalidParameterValueException() {
        try {
            QCmd.of(new String[]{"valid", "-req", "ok", "-num", "abc"}).parse(ValidCmd.class);
            Assert.fail("Should fail on regex validation");
        } catch (InvalidParameterValueException e) {
            Assert.assertEquals("abc", e.getValue());
        }
    }

    @Test
    public void testUnknownOptionException() {
        try {
            QCmd.of(new String[]{"valid", "-req", "ok", "-unknown", "123"}).parse(ValidCmd.class);
            Assert.fail("Should fail on unknown option");
        } catch (UnknownOptionException e) {
            Assert.assertEquals("-unknown", e.getOptionName());
        }
    }
}
