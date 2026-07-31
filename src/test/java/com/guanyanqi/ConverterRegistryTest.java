package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.converter.ConverterRegistry;
import com.guanyanqi.converter.QStringConverter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConverterRegistry 全局类型转换器注册与 String 构造方法兜底的组合测试。
 * <p>
 * 验证两种转换路径：
 * <ol>
 *   <li>未注册的 CustomType：走 convertValue 管线第 6 步——
 *       通过唯一 String 参数构造方法自动实例化</li>
 *   <li>已注册的 GlobalCustomType：走第 2 步——
 *       从全局 ConverterRegistry 中获取已注册的转换器</li>
 * </ol>
 * </p>
 *
 * @author guanyanqi
 */
public class ConverterRegistryTest {

    /** 通过 String 构造方法兜底实例化的类型 */
    public static class CustomType {
        private final String code;
        public CustomType(String code) {
            this.code = code;
        }
        public String getCode() { return code; }
    }

    /** 通过全局注册的 Lambda 转换器实例化的类型 */
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

    /**
     * 注册全局 Converter 并验证其优先级高于 String 构造方法兜底。
     * <p>-c CODE-123 → CustomType("CODE-123")（String 构造方法兜底）</p>
     * <p>-g 5 → GlobalCustomType(50)（Lambda: value * 10）</p>
     */
    @Test
    public void testConverterRegistrationAndFallback() {
        ConverterRegistry.register(GlobalCustomType.class, (QStringConverter<GlobalCustomType>) value -> new GlobalCustomType(Integer.parseInt(value) * 10));

        ConvCmd cmd = QCmd.of(new String[]{"conv", "-c", "CODE-123", "-g", "5"}).parse(ConvCmd.class).value();

        assertEquals("CODE-123", cmd.customType.getCode());
        assertEquals(50, cmd.globalCustomType.getVal());
    }
}
