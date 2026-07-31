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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 异常体系与内置常量/单例的全面覆盖测试。
 * <p>
 * 包含两个维度的验证：
 * <ol>
 *   <li><b>注解约束异常场景</b>：无 @Cmd 注解、names 为空、参数名重复</li>
 *   <li><b>异常类构造器与字段 getter 覆盖</b>：QCmdException 两大核心构造器 +
 *       MissingParameterException / InvalidParameterValueException / UnknownOptionException
 *       的构造器与访问器，以及 Constants、Converter 单例的实例化</li>
 * </ol>
 * </p>
 *
 * @author guanyanqi
 */
public class ExceptionHandlingTest {

    /** 未标注 @Cmd 注解的类 */
    public static class NoAnnotationCmd {}

    /** names 为空数组的类 */
    @Cmd(names = {})
    public static class EmptyNameCmd {}

    /** 两个字段声明了相同的 -p 选项名 */
    @Cmd(names = "dup")
    public static class DupParamCmd {
        @Parameter(names = "-p")
        public String p1;

        @Parameter(names = "-p")
        public String p2;
    }

    /**
     * 验证三类注解配置错误场景各自的异常信息：
     * <ul>
     *   <li>缺少 @Cmd 注解 → "未标注 @Cmd 注解"</li>
     *   <li>names 为空数组 → "names 不能为空"</li>
     *   <li>参数名重复 → "重复定义"</li>
     * </ul>
     */
    @Test
    public void testExceptionScenarios() {
        // 无注解
        QCmdException e1 = assertThrows(QCmdException.class, () -> {
            QCmd.of(new String[]{"test"}).parse(NoAnnotationCmd.class);
        });
        assertTrue(e1.getMessage().contains("没有添加@Cmd注解") || e1.getMessage().contains("未标注 @Cmd 注解"));

        // 空 names
        QCmdException e2 = assertThrows(QCmdException.class, () -> {
            QCmd.of(new String[]{"test"}).parse(EmptyNameCmd.class);
        });
        assertTrue(e2.getMessage().contains("names 不能为空") || e2.getMessage().contains("没有声明names"));

        // 重复参数名
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
    public void testCoverageForExceptionAndConstantConstructors() {
        // 覆盖 Constants、Converters 与 Exceptions 默认构造器与访问器
        assertNotNull(new Constants());
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

        // UnknownOptionException 字段访问
        UnknownOptionException uoe = new UnknownOptionException("cmd", "-unk");
        assertEquals("-unk", uoe.getOptionName());
    }
}
