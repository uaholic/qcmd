package com.guanyanqi;

import com.guanyanqi.converter.NoConverter;
import com.guanyanqi.core.model.OptionDescriptor;
import com.guanyanqi.core.model.VarsDescriptor;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OptionDescriptor 和 VarsDescriptor 构造时 null 参数默认值行为测试。
 * <p>
 * 验证两类描述符在传入 null 时能正确应用内置默认值：
 * <ul>
 *   <li>desc / valueValidRegex / valueValidDesc 默认 ""</li>
 *   <li>converterClass / elementConverterClass 默认 NoConverter.class</li>
 *   <li>genericType 默认取 type 的值</li>
 *   <li>targetName / rawElement 正确保留传入值</li>
 * </ul>
 * </p>
 *
 * @author guanyanqi
 */
public class OptionDescriptorTest {

    public static class Sample {
        public String field;
    }

    /** 传入 null 参数触发所有默认值分支，验证每个 accessor 的返回值 */
    @Test
    public void testOptionDescriptorNullDefaults() throws Exception {
        Field field = Sample.class.getField("field");

        // 测试传入 null 参数触发的默认值分支
        OptionDescriptor option = new OptionDescriptor(
                new String[]{"-f"},
                null,
                false,
                null,
                null,
                null,
                String.class,
                null,
                "field",
                field
        );

        assertEquals("", option.desc());
        assertEquals("", option.valueValidRegex());
        assertEquals("", option.valueValidDesc());
        assertEquals(NoConverter.class, option.converterClass());
        assertEquals(String.class, option.genericType());
        assertEquals("field", option.targetName());
        assertEquals(field, option.rawElement());
    }

    /** VarsDescriptor 传入 null 参数触发默认值分支 */
    @Test
    public void testVarsDescriptorNullDefaults() throws Exception {
        Field field = Sample.class.getField("field");

        // 测试 VarsDescriptor 传入 null 参数触发的默认值分支
        VarsDescriptor vars = new VarsDescriptor(
                null,
                null,
                String.class,
                null,
                "field",
                field
        );

        assertEquals("", vars.desc());
        assertEquals(NoConverter.class, vars.elementConverterClass());
        assertEquals(String.class, vars.genericType());
        assertEquals("field", vars.targetName());
        assertEquals(field, vars.rawElement());
    }
}
