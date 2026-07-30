package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.annotation.Vars;
import org.junit.Assert;
import org.junit.Test;

public class RecordBindingEdgeCasesTest {

    @Cmd(names = "edge", desc = "Record 边界条件测试")
    public record EdgeRecord(
            @Parameter(names = "-i") int intVal,
            @Parameter(names = "-b") boolean boolVal,
            @Parameter(names = "-str") String strVal,
            @Vars String varVal
    ) {}

    @Test
    public void testDefaultPrimitiveValues() {
        String[] args = new String[]{"edge"};
        EdgeRecord result = QCmd.of(args).parse(EdgeRecord.class);

        Assert.assertEquals(0, result.intVal());
        Assert.assertFalse(result.boolVal());
        Assert.assertNull(result.strVal());
        Assert.assertNull(result.varVal());
    }
}
