package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.annotation.Vars;
import com.guanyanqi.converter.QStringConverter;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 复杂场景全功能集成测试——覆盖自定义 Converter、Enum、正则校验、Set、Map、负数、帮助文本等。
 * <p>
 * 验证一条复杂命令行对 Transaction POJO 的全部字段绑定结果和生成的帮助文本。
 * </p>
 *
 * <p>覆盖的功能点：</p>
 * <ul>
 *   <li>@Parameter.converter 自定义类型转换（Account "账号@名称"）</li>
 *   <li>@Parameter.valueValidRegex 正则校验（金额两位小数）</li>
 *   <li>Enum 枚举自动解析（OperationType）</li>
 *   <li>Collection&lt;Long&gt; 集合类型转换（逗号分隔订单号）</li>
 *   <li>Map&lt;Long,String&gt; 映射类型转换（k=v 备注）</li>
 *   <li>@Vars 位置变量集合（id 列表）</li>
 *   <li>POJO 中 final 字段的初始化值保持</li>
 *   <li>帮助文本自动生成</li>
 * </ul>
 *
 * @author guanyanqi
 */
public class SimpleTest {

    /**
     * 全功能集成测试入口。
     * <p>命令行：trans -A 110@测试账户 -t LOAN -r 123=order1,456=order2,789=order3
     * --amount 0.01 -o 123,456,789 555 666 777</p>
     * <p>同时验证帮助文本的结构完整性。</p>
     */
    @Test
    public void testFullPojoIntegration() {
        String cmd = "trans -A 110@测试账户 -t LOAN -r 123=order1,456=order2,789=order3 --amount 0.01 -o 123,456,789 555 666 777";
        String[] argv = cmd.trim().split(" ");
        QCmd qCmd = QCmd.of(argv);
        ParsedCommand<Transaction> parsed = qCmd.parse(Transaction.class);
        Transaction result = parsed.value();

        // 基础字段断言
        assertEquals(0.01d, result.amount);
        assertEquals("", result.name);
        assertSame(OperationType.LOAN, result.operationType);
        assertEquals("测试账户", result.account.accountName);
        assertEquals("110", result.account.accountNo);

        // 集合字段断言
        Set<Long> ordersExpected = new HashSet<>(Arrays.asList(123L, 456L, 789L));
        assertEquals(ordersExpected, result.orders);

        // 映射字段断言
        Map<Long, String> remarkExpected = new HashMap<>();
        remarkExpected.put(123L, "order1");
        remarkExpected.put(456L, "order2");
        remarkExpected.put(789L, "order3");
        assertEquals(remarkExpected, result.remark);

        // 位置变量断言
        List<Long> idsExpected = Arrays.asList(555L, 666L, 777L);
        assertEquals(idsExpected, result.ids);

        // 帮助文本断言
        assertTrue(parsed.helpText().contains("命令：trans"));
        assertTrue(parsed.helpText().contains("参数：-A|--account（必填）"));
        assertTrue(parsed.helpText().contains("变量描述：id列表"));
        assertTrue(parsed.helpText().contains("-h|--help"));
    }

    /** 转账命令 POJO：测试用命令类，涵盖所有常见参数类型 */
    @Cmd(names = {"trans"}, desc = "账户操作命令")
    static class Transaction {
        @Parameter(names = {"-n", "--name"}, desc = "姓名")
        private final String name = "";

        @Parameter(names = {"-A", "--account"}, desc = "指定账户信息。格式：账户号@账户名称", required = true, converter = AccountConverter.class)
        private Account account;

        @Parameter(names = {"-t", "--type"}, desc = "操作类型。REPAY-还款;LOAN-借款", required = true)
        private OperationType operationType;

        @Parameter(names = {"-a", "--amount"}, desc = "操作金额", required = true, valueValidRegex = "^[0-9]+(\\.[0-9]{1,2})?$", valueValidDesc = "请输入小数点后不超过两位的数字金额")
        private double amount;

        @Parameter(names = {"-o", "--orders"}, desc = "订单号列表（以英文逗号分割）")
        private Set<Long> orders;

        @Parameter(names = {"-r", "--remark"}, desc = "备注列表（单号1=备注1,单号2=备注2）")
        private Map<Long, String> remark;

        @Vars(desc = "id列表")
        private List<Long> ids;
    }

    /** 账户 POJO：由 AccountConverter 从 "110@测试账户" 格式反序列化 */
    static class Account {
        private final String accountNo;
        private final String accountName;

        public Account(String accountNo, String accountName) {
            this.accountNo = accountNo;
            this.accountName = accountName;
        }

        public String getAccountNo() { return accountNo; }
        public String getAccountName() { return accountName; }
    }

    /** 操作类型枚举 */
    public enum OperationType { REPAY, LOAN }

    /** 账户自定义转换器：按 "@" 分割为 账号 和 账户名称 */
    static class AccountConverter implements QStringConverter<Account> {
        @Override
        public Account convert(String value) {
            String[] split = value.split("@");
            return new Account(split[0], split[1]);
        }
    }
}
