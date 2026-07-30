package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.annotation.Vars;
import com.guanyanqi.core.strategy.RecordBindingStrategy;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.RecordComponent;
import java.util.List;

public class RecordBindingStrategyTest {

    @Cmd(names = "primitives")
    public record PrimitiveRecord(
            @Parameter(names = "-b") boolean b,
            @Parameter(names = "-i") int i,
            @Parameter(names = "-l") long l,
            @Parameter(names = "-d") double d,
            @Parameter(names = "-f") float f,
            @Parameter(names = "-s") short s,
            @Parameter(names = "-by") byte by,
            @Parameter(names = "-c") char c,
            @Vars List<String> vars
    ) {}

    public record AccessorAnnoRecord(String code) {
        @Parameter(names = "-c")
        @Override
        public String code() {
            return code;
        }
    }

    public record NoAnnoRecord(String prop) {}

    @Test
    public void testRecordPrimitiveDefaults() {
        PrimitiveRecord result = QCmd.of(new String[]{"primitives"}).parse(PrimitiveRecord.class);

        Assert.assertFalse(result.b());
        Assert.assertEquals(0, result.i());
        Assert.assertEquals(0L, result.l());
        Assert.assertEquals(0.0d, result.d(), 0.0001);
        Assert.assertEquals(0.0f, result.f(), 0.0001);
        Assert.assertEquals((short) 0, result.s());
        Assert.assertEquals((byte) 0, result.by());
        Assert.assertEquals('\0', result.c());
        Assert.assertNull(result.vars());
    }

    @Test
    public void testAccessorAnnotationFallback() {
        RecordComponent[] comps = AccessorAnnoRecord.class.getRecordComponents();
        Assert.assertNotNull(RecordBindingStrategy.getParameterAnnotation(comps[0], AccessorAnnoRecord.class));
    }

    @Test
    public void testUnannotatedRecordComponentFallback() {
        RecordComponent[] comps = NoAnnoRecord.class.getRecordComponents();
        Assert.assertNull(RecordBindingStrategy.getParameterAnnotation(comps[0], NoAnnoRecord.class));
        Assert.assertNull(RecordBindingStrategy.getVarsAnnotation(comps[0], NoAnnoRecord.class));
    }
}
