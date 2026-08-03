package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.constant.Constants;
import com.guanyanqi.converter.DefaultCollectionStringConverter;
import com.guanyanqi.converter.DefaultMapStringConverter;
import com.guanyanqi.exception.InvalidParameterValueException;
import com.guanyanqi.exception.MissingParameterException;
import com.guanyanqi.exception.QCmdException;
import com.guanyanqi.exception.UnknownOptionException;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 专门针对各种异常边界场景的单元测试集合。
 * <p>涵盖未知选项、缺失必选项、校验失败、重复参数定义、参数非空断言等分支。</p>
 */
public class ExceptionHandlingTest {

    @Cmd(names = {"dummy"})
    public static class DummyCmd {
        @Parameter(names = {"-p"}, required = true)
        private String param;
    }

    @Cmd(names = {"validated"})
    public static class ValidatedCmd {
        @Parameter(names = {"-e"}, valueValidRegex = "^(dev|prod)$", valueValidDesc = "只能是 dev 或 prod")
        private String env;
    }

    @Cmd(names = {"dup"})
    public static class DupParamCmd {
        @Parameter(names = {"-p"})
        private String p1;
        @Parameter(names = {"-p"})
        private String p2;
    }

    @Test
    public void testUnknownOption() {
        UnknownOptionException e = assertThrows(UnknownOptionException.class, () -> {
            QCmd.of(new String[]{"dummy", "--unknown", "val"}).parse(DummyCmd.class);
        });
        assertEquals("--unknown", e.getOptionName());
        assertTrue(e.getMessage().contains("未知选项") || e.getMessage().contains("不支持参数选项"));
    }

    @Test
    public void testMissingRequiredParameter() {
        MissingParameterException e = assertThrows(MissingParameterException.class, () -> {
            QCmd.of(new String[]{"dummy"}).parse(DummyCmd.class);
        });
        assertNotNull(e.getMissingParameters());
        assertTrue(e.getMessage().contains("缺失必填参数") || e.getMessage().contains("必填参数"));
    }

    @Test
    public void testInvalidParameterValue() {
        InvalidParameterValueException e = assertThrows(InvalidParameterValueException.class, () -> {
            QCmd.of(new String[]{"validated", "-e", "test"}).parse(ValidatedCmd.class);
        });
        assertEquals("-e", e.getOptionName());
        assertEquals("test", e.getValue());
        assertEquals("只能是 dev 或 prod", e.getRuleDesc());
    }

    @Test
    public void testDuplicateOptionRegistration() {
        QCmdException e3 = assertThrows(QCmdException.class, () -> {
            QCmd.of(new String[]{"dup"}).parse(DupParamCmd.class);
        });
        assertTrue(e3.getMessage().contains("重复定义") || e3.getMessage().contains("重复声明"));
    }

    /**
     * 覆盖常量/转换器单例及全部异常类的构造器与 getter。
     * <p>目的：确保所有公共类可被实例化、所有异常字段的访问器不抛 NullPointerException，
     * 提高 JaCoCo 行覆盖率。</p>
     */
    @Test
    public void testCoverageForExceptionAndConstantConstructors() throws Exception {
        // 覆盖 Constants、Converters 与 Exceptions 默认构造器与访问器
        Constructor<Constants> constCtor = Constants.class.getDeclaredConstructor();
        constCtor.setAccessible(true);
        assertNotNull(constCtor.newInstance());

        assertNotNull(DefaultCollectionStringConverter.getInstance());
        assertNotNull(DefaultMapStringConverter.getInstance());

        // QCmdException 核心构造器
        QCmdException ex1 = new QCmdException("msg");
        QCmdException ex2 = new QCmdException("msg", new RuntimeException("cause"));
        assertEquals("msg", ex1.getMessage());
        assertEquals("msg", ex2.getMessage());
        assertNotNull(ex2.getCause());

        // MissingParameterException 字段访问
        MissingParameterException mpe = new MissingParameterException("cmd", List.of("-p"));
        assertEquals(List.of("-p"), mpe.getMissingParameters());

        // InvalidParameterValueException 字段访问
        InvalidParameterValueException ipve = new InvalidParameterValueException("cmd", "-p", "val", "rule");
        assertEquals("-p", ipve.getOptionName());
        assertEquals("val", ipve.getValue());
        assertEquals("rule", ipve.getRuleDesc());
    }
}
