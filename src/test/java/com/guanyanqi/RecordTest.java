package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.annotation.Vars;
import com.guanyanqi.converter.QStringConverter;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Java Record 完整端到端解析集成测试。
 * <p>
 * 核心场景：命令行中混含负数（-123.45）、布尔开关（-v）、自定义转换器（-u alice:admin）、
 * 位置变量（file1.txt file2.txt），验证解析器正确处理负数与布尔选项的区分，
 * 以及 Record 的不可变绑定。
 * </p>
 *
 * @author guanyanqi
 */
public class RecordTest {

    @Cmd(names = {"calc"}, desc = "计算器指令")
    public record CalculatorCmd(
            @Parameter(names = {"-a", "--amount"}, required = true, desc = "金额")
            double amount,

            @Parameter(names = {"-v", "--verbose"}, desc = "是否详细")
            boolean verbose,

            @Parameter(names = {"-u", "--user"}, converter = UserConverter.class, desc = "用户信息")
            UserInfo user,

            @Vars(desc = "操作文件列表")
            List<String> files
    ) {}

    public record UserInfo(String username, String role) {}

    /** 自定义转换器：将 "alice:admin" 格式解析为 UserInfo */
    public static class UserConverter implements QStringConverter<UserInfo> {
        @Override
        public UserInfo convert(String value) {
            String[] parts = value.split(":");
            return new UserInfo(parts[0], parts[1]);
        }
    }

    /**
     * 负数金额 -123.45 不应被误认为 -a 的附加短选项 -1 且值为 23.45，
     * 它应被 NegativeNumberHandler 正确识别为一个完整的负数位置变量，
     * 但这里 -a -123.45 是选项 -a 的负数值参数 —— 验证负数值正确赋值给 amount 字段。
     */
    @Test
    public void testRecordParsingWithNegativeValue() {
        String cmd = "calc -a -123.45 -v -u alice:admin file1.txt file2.txt";
        String[] args = cmd.split(" ");

        CalculatorCmd result = QCmd.of(args).parse(CalculatorCmd.class).value();

        assertNotNull(result);
        assertEquals(-123.45d, result.amount(), 0.0001);
        assertTrue(result.verbose());
        assertNotNull(result.user());
        assertEquals("alice", result.user().username());
        assertEquals("admin", result.user().role());
        assertEquals(Arrays.asList("file1.txt", "file2.txt"), result.files());
    }
}
