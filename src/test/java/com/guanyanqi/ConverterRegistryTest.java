package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.converter.ConverterRegistry;
import com.guanyanqi.converter.QStringConverter;
import org.junit.Assert;
import org.junit.Test;

public class ConverterRegistryTest {

    public static class CustomType {
        private final String code;
        public CustomType(String code) {
            this.code = code;
        }
        public String getCode() { return code; }
    }

    public static class GlobalCustomType {
        private final int val;
        public GlobalCustomType(int val) { this.val = val; }
        public int getVal() { return val; }
    }

    @Cmd(names = "conv", desc = "转换器注册测试")
    public static class ConvCmd {
        @Parameter(names = "-c")
        public CustomType customType;

        @Parameter(names = "-g")
        public GlobalCustomType globalCustomType;
    }

    @Test
    public void testConverterRegistrationAndFallback() {
        ConverterRegistry.register(GlobalCustomType.class, (QStringConverter<GlobalCustomType>) value -> new GlobalCustomType(Integer.parseInt(value) * 10));

        ConvCmd cmd = QCmd.of(new String[]{"conv", "-c", "CODE-123", "-g", "5"}).parse(ConvCmd.class);

        Assert.assertEquals("CODE-123", cmd.customType.getCode());
        Assert.assertEquals(50, cmd.globalCustomType.getVal());
    }
}
