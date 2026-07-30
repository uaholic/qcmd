package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

public class BasicTypeTest {

    public enum Level { LOW, MEDIUM, HIGH }

    @Cmd(names = {"type-test"}, desc = "基础类型解析测试")
    public static class BasicTypeCmd {
        @Parameter(names = "-i") public int age;
        @Parameter(names = "-l") public long count;
        @Parameter(names = "-d") public double ratio;
        @Parameter(names = "-b") public boolean flag;
        @Parameter(names = "-bd") public BigDecimal amount;
        @Parameter(names = "-date") public LocalDate date;
        @Parameter(names = "-level") public Level level;
        @Parameter(names = "-tags") public Set<String> tags;
        @Parameter(names = "-meta") public Map<String, Integer> meta;
    }

    @Test
    public void testBasicTypes() {
        String[] args = new String[]{
                "type-test",
                "-i", "25",
                "-l", "10000000000",
                "-d", "3.14159",
                "-b",
                "-bd", "999.99",
                "-date", "2026-07-30",
                "-level", "HIGH",
                "-tags", "java,cli,qcmd",
                "-meta", "k1=100,k2=200"
        };

        BasicTypeCmd cmd = QCmd.of(args).parse(BasicTypeCmd.class);

        Assert.assertEquals(25, cmd.age);
        Assert.assertEquals(10000000000L, cmd.count);
        Assert.assertEquals(3.14159d, cmd.ratio, 0.00001);
        Assert.assertTrue(cmd.flag);
        Assert.assertEquals(new BigDecimal("999.99"), cmd.amount);
        Assert.assertEquals(LocalDate.of(2026, 7, 30), cmd.date);
        Assert.assertEquals(Level.HIGH, cmd.level);
        Assert.assertEquals(Set.of("java", "cli", "qcmd"), cmd.tags);
        Assert.assertEquals(Map.of("k1", 100, "k2", 200), cmd.meta);
    }
}
