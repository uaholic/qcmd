package com.guanyanqi;

import com.guanyanqi.converter.NoConverter;
import com.guanyanqi.core.model.OptionDescriptor;
import com.guanyanqi.core.model.VarsDescriptor;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;

public class OptionDescriptorTest {

    public static class Sample {
        public String field;
    }

    @Test
    public void testOptionDescriptorNullDefaults() throws Exception {
        Field field = Sample.class.getField("field");

        // 测试传入 null 参数触发的默认值分支
        OptionDescriptor option = new OptionDescriptor(
                new String[]{"-f"},
                null,
                false,
                null,
                null,
                null,
                String.class,
                null,
                "field",
                field
        );

        Assert.assertEquals("", option.desc());
        Assert.assertEquals("", option.valueValidRegex());
        Assert.assertEquals("", option.valueValidDesc());
        Assert.assertEquals(NoConverter.class, option.converterClass());
        Assert.assertEquals(String.class, option.genericType());
        Assert.assertEquals("field", option.targetName());
        Assert.assertEquals(field, option.rawElement());
    }

    @Test
    public void testVarsDescriptorNullDefaults() throws Exception {
        Field field = Sample.class.getField("field");

        // 测试 VarsDescriptor 传入 null 参数触发的默认值分支
        VarsDescriptor vars = new VarsDescriptor(
                null,
                null,
                String.class,
                null,
                "field",
                field
        );

        Assert.assertEquals("", vars.desc());
        Assert.assertEquals(NoConverter.class, vars.elementConverterClass());
        Assert.assertEquals(String.class, vars.genericType());
        Assert.assertEquals("field", vars.targetName());
        Assert.assertEquals(field, vars.rawElement());
    }
}
