package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.annotation.Vars;
import com.guanyanqi.exception.InvalidParameterValueException;
import com.guanyanqi.exception.MissingParameterException;
import com.guanyanqi.exception.QCmdException;
import com.guanyanqi.exception.UnknownOptionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CommandValidator 参数校验规则专项测试。
 * <p>
 * 覆盖 5 种校验失败/通过场景：
 * <ul>
 *   <li>required=true 时缺少必填参数 → MissingParameterException</li>
 *   <li>valueValidRegex 正则不匹配 → InvalidParameterValueException</li>
 *   <li>空正则/无正则的选项应正常通过</li>
 *   <li>提供了未在 @Parameter 中声明的选项 → UnknownOptionException</li>
 *   <li>命令类未声明 @Vars 但传入了位置变量 → QCmdException</li>
 * </ul>
 * </p>
 *
 * @author guanyanqi
 */
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

    /** 缺少 required=true 的选项 -r 时抛 MissingParameterException */
    @Test
    public void testMissingRequiredParam() {
        MissingParameterException e = assertThrows(MissingParameterException.class, () -> {
            QCmd.of(new String[]{"valid-sample"}).parse(ValidSampleCmd.class);
        });
        assertTrue(e.getMissingParameters().contains("-r"));
    }

    /** -num 的值 "abc" 不匹配 ^[0-9]+$ 正则 → InvalidParameterValueException */
    @Test
    public void testInvalidRegexValue() {
        InvalidParameterValueException e = assertThrows(InvalidParameterValueException.class, () -> {
            QCmd.of(new String[]{"valid-sample", "-r", "ok", "-num", "abc"}).parse(ValidSampleCmd.class);
        });
        assertEquals("-num", e.getOptionName());
        assertEquals("abc", e.getValue());
        assertEquals("数字", e.getRuleDesc());
    }

    /** 空字符串的正则约束应视为无约束，任意值均通过 */
    @Test
    public void testEmptyRegexValuePass() {
        ValidSampleCmd cmd = QCmd.of(new String[]{"valid-sample", "-r", "ok", "-opt", "any"}).parse(ValidSampleCmd.class).value();
        assertEquals("any", cmd.opt);
    }

    /** 未声明的选项 -unknown → UnknownOptionException */
    @Test
    public void testUnknownOption() {
        UnknownOptionException e = assertThrows(UnknownOptionException.class, () -> {
            QCmd.of(new String[]{"valid-sample", "-r", "ok", "-unknown", "val"}).parse(ValidSampleCmd.class);
        });
        assertEquals("-unknown", e.getOptionName());
    }

    /** 命令未声明 @Vars 但传入了位置变量 "pos1" → QCmdException */
    @Test
    public void testUnexpectedPositionalVar() {
        QCmdException e = assertThrows(QCmdException.class, () -> {
            QCmd.of(new String[]{"no-vars", "pos1"}).parse(NoVarsCmd.class);
        });
        assertTrue(e.getMessage().contains("不支持接收位置变量"));
    }
}
