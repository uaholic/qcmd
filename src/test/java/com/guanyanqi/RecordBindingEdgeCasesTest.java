package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.annotation.Vars;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Record 绑定边界条件测试。
 * <p>
 * 验证当命令行没有提供任何参数时，Record 组件的默认值行为：
 * <ul>
 *   <li>int 默认为 0，boolean 默认为 false</li>
 *   <li>String 等引用类型默认为 null</li>
 *   <li>@Vars 标记的 String 在没有位置变量时也为 null</li>
 * </ul>
 * </p>
 *
 * @author guanyanqi
 */
public class RecordBindingEdgeCasesTest {

    @Cmd(names = "edge", desc = "Record 边界条件测试")
    public record EdgeRecord(
            @Parameter(names = "-i") int intVal,
            @Parameter(names = "-b") boolean boolVal,
            @Parameter(names = "-str") String strVal,
            @Vars String varVal
    ) {}

    /** 空 args：基本类型为默认值，引用类型为 null */
    @Test
    public void testDefaultPrimitiveValues() {
        String[] args = new String[]{"edge"};
        EdgeRecord result = QCmd.of(args).parse(EdgeRecord.class).value();

        assertEquals(0, result.intVal());
        assertFalse(result.boolVal());
        assertNull(result.strVal());
        assertNull(result.varVal());
    }
}
