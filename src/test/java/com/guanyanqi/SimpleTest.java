package com.guanyanqi;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.annotation.Vars;
import com.guanyanqi.converter.QStringConverter;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SimpleTest {

    @Test
    public void test() {
        String cmd = "trans -A 110@测试账户 -t LOAN -r 123=order1,456=order2,789=order3 --amount 0.01 -o 123,456,789 555 666 777";
        String[] argv = cmd.trim().split(" ");
        QCmd qCmd = QCmd.of(argv);
        Transaction result = qCmd.parse(Transaction.class);
        Assert.assertEquals(0.01d, result.amount, 0.0);
        Assert.assertEquals("", result.name);
        Assert.assertSame(result.operationType, OperationType.LOAN);
        Assert.assertEquals("测试账户", result.account.accountName);
        Assert.assertEquals("110", result.account.accountNo);
        Set<Long> ordersExpected = Sets.newHashSet(123L, 456L, 789L);
        Map<Long, String> remarkExpected = Maps.newHashMap();
        remarkExpected.put(123L, "order1");
        remarkExpected.put(456L, "order2");
        remarkExpected.put(789L, "order3");
        List<Long> idsExpected = Lists.newArrayList(555L, 666L, 777L);
        Assert.assertEquals(result.orders, ordersExpected);
        Assert.assertEquals(result.remark, remarkExpected);
        Assert.assertEquals(result.ids, idsExpected);
        String descExpected = "使用方法：命令 [参数 参数值] [变量...]\n" +
                "命令：trans\n" +
                "功能描述：账户操作命令\n" +
                "参数说明：\n" +
                "\t参数：-n|--name（可选），参数说明：姓名\n" +
                "\t参数：-A|--account（必填），参数说明：指定账户信息。格式：账户号@账户名称\n" +
                "\t参数：-t|--type（必填），参数说明：操作类型。REPAY-还款;LOAN-借款\n" +
                "\t参数：-a|--amount（必填），参数说明：操作金额，输入规则：请输入小数点后不超过两位的数字金额\n" +
                "\t参数：-o|--orders（可选），参数说明：订单号列表（以英文逗号分割）\n" +
                "\t参数：-r|--remark（可选），参数说明：备注列表（单号1=备注1,单号2=备注2）\n" +
                "变量描述：id列表\n";
        Assert.assertEquals(descExpected, qCmd.getDesc());
    }

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

    static class Account {
        private final String accountNo;
        private final String accountName;

        public Account(String accountNo, String accountName) {
            this.accountNo = accountNo;
            this.accountName = accountName;
        }

        public String getAccountNo() {
            return accountNo;
        }

        public String getAccountName() {
            return accountName;
        }
    }

    public enum OperationType {
        REPAY,
        LOAN
    }

    static class AccountConverter implements QStringConverter<Account> {
        @Override
        public Account convert(String value) {
            String[] split = value.split("@");
            return new Account(split[0], split[1]);
        }
    }

}
