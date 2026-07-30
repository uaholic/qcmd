package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.annotation.Vars;
import org.junit.Assert;
import org.junit.Test;

public class PojoBindingStrategyTest {

    @Cmd(names = "plain-pojo")
    public static class PlainPojoCmd {
        public String plainField;

        @Parameter(names = "-n")
        public String name;

        @Vars
        public String var;
    }

    @Test
    public void testPojoUnannotatedAndEmptyVars() {
        PlainPojoCmd pojo = QCmd.of(new String[]{"plain-pojo"}).parse(PlainPojoCmd.class);

        Assert.assertNull(pojo.plainField);
        Assert.assertNull(pojo.name);
        Assert.assertNull(pojo.var);
    }
}
