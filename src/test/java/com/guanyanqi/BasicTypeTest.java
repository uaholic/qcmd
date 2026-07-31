package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 基础类型（基本类型 / Enum / BigDecimal / LocalDate / Collection / Map）的综合解析测试。
 * <p>
 * 目的：验证 QCmd 的内置类型转换管线（CommandDescriptor.convertValue）能正确覆盖
 * int、long、double、boolean、BigDecimal、LocalDate、Enum 枚举、Set 集合、Map 映射
 * 等日常开发中最常用的类型。
 * </p>
 *
 * @author guanyanqi
 */
public class BasicTypeTest {

    /** 测试用枚举，验证 Enum.valueOf 自动解析路径 */
    public enum Level { LOW, MEDIUM, HIGH }

    /** 涵盖了 8 种基础类型 + Set + Map 的 POJO 命令类 */
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

    /**
     * 一次性覆盖全部基础类型的端到端解析与断言。
     * <p>包含布尔开关、枚举、日期、BigDecimal、逗号分隔 Set、k=v 格式 Map。</p>
     */
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

        BasicTypeCmd cmd = QCmd.of(args).parse(BasicTypeCmd.class).value();

        // 基本类型断言
        assertEquals(25, cmd.age);
        assertEquals(10000000000L, cmd.count);
        assertEquals(3.14159d, cmd.ratio, 0.00001);
        assertTrue(cmd.flag);

        // 特殊类型断言
        assertEquals(new BigDecimal("999.99"), cmd.amount);
        assertEquals(LocalDate.of(2026, 7, 30), cmd.date);
        assertEquals(Level.HIGH, cmd.level);

        // 集合与映射断言
        assertEquals(Set.of("java", "cli", "qcmd"), cmd.tags);
        assertEquals(Map.of("k1", 100, "k2", 200), cmd.meta);
    }
}
