package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.converter.QStringConverter;
import org.junit.Assert;
import org.junit.Test;

public class CustomConverterTest {

    public record Money(double amount, String currency) {}

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

    @Test
    public void testCustomConverter() {
        PayCmd cmd = QCmd.of(new String[]{"pay", "-m", "199.9:USD"}).parse(PayCmd.class);

        Assert.assertEquals(199.9d, cmd.money().amount(), 0.0001);
        Assert.assertEquals("USD", cmd.money().currency());
    }
}
