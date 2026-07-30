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
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class ExceptionHandlingTest {

    public static class NoAnnotationCmd {}

    @Cmd(names = {})
    public static class EmptyNameCmd {}

    @Cmd(names = "dup")
    public static class DupParamCmd {
        @Parameter(names = "-p")
        public String p1;

        @Parameter(names = "-p")
        public String p2;
    }

    @Test
    public void testExceptionScenarios() {
        try {
            QCmd.of(new String[]{"test"}).parse(NoAnnotationCmd.class);
            Assert.fail("Should fail on missing @Cmd");
        } catch (QCmdException e) {
            Assert.assertTrue(e.getMessage().contains("没有添加@Cmd注解") || e.getMessage().contains("未标注 @Cmd 注解"));
        }

        try {
            QCmd.of(new String[]{"test"}).parse(EmptyNameCmd.class);
            Assert.fail("Should fail on empty @Cmd names");
        } catch (QCmdException e) {
            Assert.assertTrue(e.getMessage().contains("names 不能为空") || e.getMessage().contains("没有声明names"));
        }

        try {
            QCmd.of(new String[]{"dup"}).parse(DupParamCmd.class);
            Assert.fail("Should fail on duplicate parameter name");
        } catch (QCmdException e) {
            Assert.assertTrue(e.getMessage().contains("重复定义") || e.getMessage().contains("重复声明"));
        }
    }

    @Test
    public void testCoverageForExceptionAndConstantConstructors() {
        // 覆盖 Constants、Converters 与 Exceptions 默认构造器与上下文访问器
        Assert.assertNotNull(new Constants());
        Assert.assertNotNull(DefaultCollectionStringConverter.getInstance());
        Assert.assertNotNull(DefaultMapStringConverter.getInstance());

        QCmd qcmd = QCmd.of(new String[]{"dup"});
        QCmdException ex1 = new QCmdException("msg");
        QCmdException ex2 = new QCmdException(new RuntimeException("cause"));
        QCmdException ex3 = new QCmdException("msg", qcmd);
        QCmdException ex4 = new QCmdException("msg", new RuntimeException("cause"));
        QCmdException ex5 = new QCmdException("msg", new RuntimeException("cause"), qcmd);

        ex1.setQCmd(qcmd);
        Assert.assertEquals(qcmd, ex1.getQCmd());
        Assert.assertEquals(qcmd, ex3.getQCmd());
        Assert.assertEquals(qcmd, ex5.getQCmd());
        Assert.assertNotNull(ex2.getCause());
        Assert.assertNotNull(ex4.getCause());

        MissingParameterException mpe = new MissingParameterException("cmd", List.of("-p"));
        Assert.assertEquals(List.of("-p"), mpe.getMissingParameters());

        InvalidParameterValueException ipve = new InvalidParameterValueException("cmd", "-p", "val", "rule");
        Assert.assertEquals("-p", ipve.getOptionName());
        Assert.assertEquals("val", ipve.getValue());
        Assert.assertEquals("rule", ipve.getRuleDesc());

        UnknownOptionException uoe = new UnknownOptionException("cmd", "-unk");
        Assert.assertEquals("-unk", uoe.getOptionName());
    }
}
