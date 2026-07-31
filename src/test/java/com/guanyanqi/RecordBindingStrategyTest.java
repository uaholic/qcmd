package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.annotation.Vars;
import com.guanyanqi.core.strategy.RecordBindingStrategy;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * RecordBindingStrategy 的反射级别单元测试。
 * <p>
 * 覆盖：
 * <ul>
 *   <li>全部 8 种基本类型的默认值（零值）</li>
 *   <li>getParameterAnnotation 的 accessor method 回退逻辑——当 RecordComponent
 *       未标注 @Parameter 但其对应的 accessor 方法标注时也应能找到</li>
 *   <li>getParameterAnnotation / getVarsAnnotation 在无注解 Record 上的 null 返回</li>
 * </ul>
 * </p>
 *
 * @author guanyanqi
 */
public class RecordBindingStrategyTest {

    /** 8 种基本类型全覆盖的 Record */
    @Cmd(names = "primitives")
    public record PrimitiveRecord(
            @Parameter(names = "-b") boolean b,
            @Parameter(names = "-i") int i,
            @Parameter(names = "-l") long l,
            @Parameter(names = "-d") double d,
            @Parameter(names = "-f") float f,
            @Parameter(names = "-s") short s,
            @Parameter(names = "-by") byte by,
            @Parameter(names = "-c") char c,
            @Vars List<String> vars
    ) {}

    /** accessor method 上标注 @Parameter 而 RecordComponent 本身未标注 */
    public record AccessorAnnoRecord(String code) {
        @Parameter(names = "-c")
        @Override
        public String code() {
            return code;
        }
    }

    /** 完全无注解的 Record */
    public record NoAnnoRecord(String prop) {}

    /** 验证所有 8 种基本类型的零默认值 */
    @Test
    public void testRecordPrimitiveDefaults() {
        PrimitiveRecord result = QCmd.of(new String[]{"primitives"}).parse(PrimitiveRecord.class).value();

        assertFalse(result.b());
        assertEquals(0, result.i());
        assertEquals(0L, result.l());
        assertEquals(0.0d, result.d(), 0.0001);
        assertEquals(0.0f, result.f(), 0.0001);
        assertEquals((short) 0, result.s());
        assertEquals((byte) 0, result.by());
        assertEquals('\0', result.c());
        assertNull(result.vars());
    }

    /** RecordComponent 无注解时，应从其 accessor 方法上寻找 @Parameter */
    @Test
    public void testAccessorAnnotationFallback() {
        RecordComponent[] comps = AccessorAnnoRecord.class.getRecordComponents();
        assertNotNull(RecordBindingStrategy.getParameterAnnotation(comps[0], AccessorAnnoRecord.class));
    }

    /** 完全无注解 Record 的 getParameterAnnotation / getVarsAnnotation 应返回 null */
    @Test
    public void testUnannotatedRecordComponentFallback() {
        RecordComponent[] comps = NoAnnoRecord.class.getRecordComponents();
        assertNull(RecordBindingStrategy.getParameterAnnotation(comps[0], NoAnnoRecord.class));
        assertNull(RecordBindingStrategy.getVarsAnnotation(comps[0], NoAnnoRecord.class));
    }
}
