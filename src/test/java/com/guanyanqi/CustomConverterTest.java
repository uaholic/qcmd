package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.converter.QStringConverter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 通过注解声明的自定义类型转换器（@Parameter.converter）测试。
 * <p>
 * 验证 convertValue 管线第 1 步优先级：注解上声明的 converter Class
 * 优先于全局注册和 String 构造方法兜底。
 * </p>
 *
 * @author guanyanqi
 */
public class CustomConverterTest {

    /** 不可变 Money 值对象，由自定义转换器从 "amount:currency" 格式解析 */
    public record Money(double amount, String currency) {}

    /** 解析 "199.9:USD" → Money(199.9, "USD") */
    public static class MoneyConverter implements QStringConverter<Money> {
        @Override
        public Money convert(String value) {
            String[] parts = value.split(":");
            return new Money(Double.parseDouble(parts[0]), parts[1]);
        }
    }

    @Cmd(names = {"pay"}, desc = "支付命令")
    public record PayCmd(
            @Parameter(names = "-m", converter = MoneyConverter.class)
            Money money
    ) {}

    /** 验证注解级别 converter 被正确调用，输出 Money record 实例 */
    @Test
    public void testCustomConverter() {
        PayCmd cmd = QCmd.of(new String[]{"pay", "-m", "199.9:USD"}).parse(PayCmd.class).value();

        assertEquals(199.9d, cmd.money().amount(), 0.0001);
        assertEquals("USD", cmd.money().currency());
    }
}
