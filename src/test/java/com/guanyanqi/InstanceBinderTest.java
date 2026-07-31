package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.core.CommandDescriptor;
import com.guanyanqi.core.CommandLineParser;
import com.guanyanqi.core.InstanceBinder;
import com.guanyanqi.exception.QCmdException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InstanceBinder 异常包装测试。
 * <p>
 * 验证当目标 POJO 的无参构造方法内部抛出异常时，
 * InstanceBinder.bind() 能够捕获并包装为 QCmdException，
 * 而非让原始 RuntimeException 直接泄露给调用方。
 * </p>
 *
 * @author guanyanqi
 */
public class InstanceBinderTest {

    /** 无参构造方法内部主动抛出 RuntimeException 的 POJO */
    @Cmd(names = "err-ctor")
    public static class ThrowCtorCmd {
        public ThrowCtorCmd() {
            throw new RuntimeException("Ctor error");
        }
    }

    /** 原始 RuntimeException 应被包装为 QCmdException 并包含"解析绑定"关键字 */
    @Test
    public void testInstanceBinderExceptionWrapping() {
        CommandDescriptor desc = new CommandDescriptor(ThrowCtorCmd.class);
        CommandLineParser.ParseResult parseResult = CommandLineParser.parse(new String[]{"err-ctor"}, desc);
        QCmdException e = assertThrows(QCmdException.class, () -> {
            InstanceBinder.bind(parseResult, desc);
        });
        assertTrue(e.getMessage().contains("解析绑定"));
    }
}
