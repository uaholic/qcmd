package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.annotation.Vars;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PojoBindingStrategy 对 POJO 无注解字段和空变量的绑定测试。
 * <p>
 * 验证：POJO 中未标注 @Parameter/@Vars 的字段不会被解析影响，
 * 且当命令行没有对应参数时，已标注字段保持 Java 默认值（引用类型为 null）。
 * </p>
 *
 * @author guanyanqi
 */
public class PojoBindingStrategyTest {

    @Cmd(names = "plain-pojo")
    public static class PlainPojoCmd {
        /** 无注解字段，解析器应完全忽略 */
        public String plainField;

        @Parameter(names = "-n")
        public String name;

        @Vars
        public String var;
    }

    /** 空参数列表：所有字段应保持默认值 null */
    @Test
    public void testPojoUnannotatedAndEmptyVars() {
        PlainPojoCmd pojo = QCmd.of(new String[]{"plain-pojo"}).parse(PlainPojoCmd.class).value();

        assertNull(pojo.plainField);
        assertNull(pojo.name);
        assertNull(pojo.var);
    }
}
