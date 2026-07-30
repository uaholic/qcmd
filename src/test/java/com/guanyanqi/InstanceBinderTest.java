package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.core.CommandDescriptor;
import com.guanyanqi.core.CommandLineParser;
import com.guanyanqi.core.InstanceBinder;
import com.guanyanqi.exception.QCmdException;
import org.junit.Assert;
import org.junit.Test;

public class InstanceBinderTest {

    @Cmd(names = "err-ctor")
    public static class ThrowCtorCmd {
        public ThrowCtorCmd() {
            throw new RuntimeException("Ctor error");
        }
    }

    @Test
    public void testInstanceBinderExceptionWrapping() {
        try {
            CommandDescriptor desc = new CommandDescriptor(ThrowCtorCmd.class);
            CommandLineParser.ParseResult parseResult = CommandLineParser.parse(new String[]{"err-ctor"}, desc);
            InstanceBinder.bind(parseResult, desc);
            Assert.fail();
        } catch (QCmdException e) {
            Assert.assertTrue(e.getMessage().contains("解析绑定"));
        }
    }
}
