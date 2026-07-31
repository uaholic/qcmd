package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.exception.InvalidParameterValueException;
import com.guanyanqi.exception.MissingParameterException;
import com.guanyanqi.exception.UnknownOptionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CommandValidator 三种专项异常的端到端验证测试。
 * <p>
 * 从 QCmd 入口层驱动，验证 MissingParameterException / InvalidParameterValueException /
 * UnknownOptionException 在用户给出不合法命令行时被正确抛出，并携带准确的上下文字段。
 * </p>
 *
 * @author guanyanqi
 */
public class ValidationTest {

    @Cmd(names = {"valid"}, desc = "校验测试")
    public static class ValidCmd {
        @Parameter(names = "-req", required = true)
        public String requiredField;

        @Parameter(names = "-num", valueValidRegex = "^[0-9]+$", valueValidDesc = "只能包含纯数字")
        public String numberField;
    }

    /** 缺少 required=true 的 -req 选项时抛出 MissingParameterException */
    @Test
    public void testMissingParameterException() {
        MissingParameterException e = assertThrows(MissingParameterException.class, () -> {
            QCmd.of(new String[]{"valid"}).parse(ValidCmd.class);
        });
        assertTrue(e.getMissingParameters().contains("-req"));
    }

    /** -num 传入 "abc" 不匹配数字正则时抛出 InvalidParameterValueException */
    @Test
    public void testInvalidParameterValueException() {
        InvalidParameterValueException e = assertThrows(InvalidParameterValueException.class, () -> {
            QCmd.of(new String[]{"valid", "-req", "ok", "-num", "abc"}).parse(ValidCmd.class);
        });
        assertEquals("abc", e.getValue());
    }

    /** 传入未声明的 -unknown 选项时抛出 UnknownOptionException */
    @Test
    public void testUnknownOptionException() {
        UnknownOptionException e = assertThrows(UnknownOptionException.class, () -> {
            QCmd.of(new String[]{"valid", "-req", "ok", "-unknown", "123"}).parse(ValidCmd.class);
        });
        assertEquals("-unknown", e.getOptionName());
    }
}
