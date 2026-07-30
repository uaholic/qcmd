package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.annotation.Vars;
import com.guanyanqi.converter.QStringConverter;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

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

    public static class UserConverter implements QStringConverter<UserInfo> {
        @Override
        public UserInfo convert(String value) {
            String[] parts = value.split(":");
            return new UserInfo(parts[0], parts[1]);
        }
    }

    @Test
    public void testRecordParsingWithNegativeValue() {
        String cmd = "calc -a -123.45 -v -u alice:admin file1.txt file2.txt";
        String[] args = cmd.split(" ");

        CalculatorCmd result = QCmd.of(args).parse(CalculatorCmd.class);

        Assert.assertNotNull(result);
        Assert.assertEquals(-123.45d, result.amount(), 0.0001);
        Assert.assertTrue(result.verbose());
        Assert.assertNotNull(result.user());
        Assert.assertEquals("alice", result.user().username());
        Assert.assertEquals("admin", result.user().role());
        Assert.assertEquals(Arrays.asList("file1.txt", "file2.txt"), result.files());
    }
}
