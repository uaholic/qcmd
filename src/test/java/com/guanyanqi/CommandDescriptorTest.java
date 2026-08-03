package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.annotation.Vars;
import com.guanyanqi.converter.QStringConverter;
import com.guanyanqi.core.CommandDescriptor;
import com.guanyanqi.exception.QCmdException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CommandDescriptor 元数据提取与 convertValue 转换管线的专项测试。
 * <p>
 * 覆盖以下核心功能点：
 * <ul>
 *   <li>null targetClass 的防御性校验</li>
 *   <li>TreeMap 等具体 Map 子类的实例化</li>
 *   <li>@Vars 自定义 elementConverter 对单变量/集合变量的转换</li>
 *   <li>无泛型声明的 raw type Collection/Map 回退到 String 元素</li>
 *   <li>unique String 参数构造方法触发异常的捕获与包装</li>
 * </ul>
 * </p>
 *
 * @author guanyanqi
 */
public class CommandDescriptorTest {

    /** 自定义 Vars 元素转换器：在元素值前添加 "var:" 前缀 */
    public static class CustomVarConverter implements QStringConverter<String> {
        @Override
        public String convert(String value) {
            return "var:" + value;
        }
    }

    public static class CountingConverter implements QStringConverter<String> {
        static final AtomicInteger CONSTRUCTIONS = new AtomicInteger();

        public CountingConverter() {
            CONSTRUCTIONS.incrementAndGet();
        }

        @Override
        public String convert(String value) {
            return value;
        }
    }

    /** String 构造方法内部抛异常的类型，用于测试异常包装路径 */
    public static class BadStringCtorType {
        public BadStringCtorType(String val) {
            throw new IllegalArgumentException("Bad string ctor");
        }
    }

    @Cmd(names = "tree-map")
    public static class TreeMapCmd {
        @Parameter(names = "-m")
        public TreeMap<String, String> map;
    }

    @Cmd(names = "single-custom-var")
    public static class SingleCustomVarCmd {
        @Vars(elementConverter = CustomVarConverter.class)
        public String var;
    }

    @Cmd(names = "list-custom-var")
    public static class ListCustomVarCmd {
        @Vars(elementConverter = CustomVarConverter.class)
        public List<String> vars;
    }

    @SuppressWarnings("rawtypes")
    @Cmd(names = "raw-type")
    public static class RawTypeCmd {
        @Parameter(names = "-l")
        public List rawList;

        @Parameter(names = "-m")
        public Map rawMap;

        @Parameter(names = "-hm")
        public HashMap<String, String> explicitHashMap;

        @Vars
        public List rawVars;
    }

    @Cmd(names = "bad-ctor")
    public static class BadCtorCmd {
        @Parameter(names = "-b")
        public BadStringCtorType badType;
    }

    @Cmd(names = "nested")
    public static class NestedGenericCmd {
        @Parameter(names = "--lists")
        public List<List<Integer>> lists;

        @Parameter(names = "--map")
        public Map<String, List<Long>> map;
    }

    @Cmd(names = "counting")
    public static class CountingConverterCmd {
        @Parameter(names = "--value", converter = CountingConverter.class)
        public String value;
    }

    @Cmd(names = "both")
    public static class ConflictingAnnotationsCmd {
        @Parameter(names = "--value")
        @Vars
        public String value;
    }

    /**
     * null targetClass 应抛出 NullPointerException 并包含明确提示。
     */
    @Test
    public void testNullTargetClass() {
        NullPointerException e = assertThrows(NullPointerException.class, () -> {
            new CommandDescriptor(null);
        });
        assertTrue(e.getMessage().contains("Target class must not be null"));
    }

    /**
     * 验证 Map 的具体实现类（如 TreeMap）能被正确实例化并填充键值对。
     */
    @Test
    public void testTreeMapTypeConversion() {
        TreeMapCmd cmd = QCmd.of(new String[]{"tree-map", "-m", "k1=v1"}).parse(TreeMapCmd.class).value();
        assertNotNull(cmd.map);
        assertTrue(cmd.map instanceof TreeMap);
        assertEquals("v1", cmd.map.get("k1"));
    }

    /**
     * @Vars 注解的 elementConverter 属性：对单变量和集合变量分别验证自定义转换器是否生效。
     */
    @Test
    public void testCustomVarConverters() {
        // 单变量场景：位置变量 rawVal 经过 CustomVarConverter → "var:val1"
        SingleCustomVarCmd cmd1 = QCmd.of(new String[]{"single-custom-var", "val1"}).parse(SingleCustomVarCmd.class).value();
        assertEquals("var:val1", cmd1.var);

        // 集合变量场景：每个元素依次经过转换器
        ListCustomVarCmd cmd2 = QCmd.of(new String[]{"list-custom-var", "val1", "val2"}).parse(ListCustomVarCmd.class).value();
        assertEquals(List.of("var:val1", "var:val2"), cmd2.vars);
    }

    /**
     * 无泛型声明的 raw type Collection/Map 应回退到 String 元素类型，
     * 同时显式泛型声明（HashMap&lt;String,String&gt;）应正确提取类型参数。
     */
    @Test
    public void testRawTypesConversion() {
        RawTypeCmd rawResult = QCmd.of(new String[]{
                "raw-type",
                "-l", "item1,item2",
                "-m", "k1=v1,k2=v2",
                "-hm", "hk1=hv1",
                "pos1", "pos2"
        }).parse(RawTypeCmd.class).value();

        assertNotNull(rawResult.rawList);
        assertEquals(List.of("item1", "item2"), rawResult.rawList);
        assertNotNull(rawResult.rawMap);
        assertEquals("v1", rawResult.rawMap.get("k1"));
        assertNotNull(rawResult.explicitHashMap);
        assertEquals("hv1", rawResult.explicitHashMap.get("hk1"));
        assertNotNull(rawResult.rawVars);
        assertEquals(List.of("pos1", "pos2"), rawResult.rawVars);
    }

    /**
     * convertValue 兜底路径 —— unique String 参数构造方法内部抛异常，
     * 应被 InstanceBinder 统一包装为 QCmdException。
     */
    @Test
    public void testBadStringConstructorException() {
        QCmdException e = assertThrows(QCmdException.class, () -> {
            QCmd.of(new String[]{"bad-ctor", "-b", "val"}).parse(BadCtorCmd.class);
        });
        assertTrue(e.getMessage().contains("解析绑定"));
    }

    /** 嵌套集合和 Map value 的泛型应递归转换，不再将 ParameterizedType 强转为 Class。 */
    @Test
    public void testNestedGenericConversion() {
        NestedGenericCmd cmd = QCmd.of(new String[]{
                "nested", "--lists", "1,2", "--map", "first=3"
        }).parse(NestedGenericCmd.class).value();

        assertEquals(List.of(List.of(1), List.of(2)), cmd.lists);
        assertEquals(List.of(3L), cmd.map.get("first"));
    }

    /** 用户转换器默认按解析请求实例化，不强制其具备全局线程安全性。 */
    @Test
    public void testCustomConverterIsNotGloballyCached() {
        CountingConverter.CONSTRUCTIONS.set(0);

        QCmd.of(new String[]{"counting", "--value", "one"})
                .parse(CountingConverterCmd.class);
        QCmd.of(new String[]{"counting", "--value", "two"})
                .parse(CountingConverterCmd.class);

        assertEquals(2, CountingConverter.CONSTRUCTIONS.get());
    }

    /** 描述元数据只读，且选项名数组使用防御性副本。 */
    @Test
    public void testDescriptorMetadataIsImmutable() {
        CommandDescriptor descriptor = new CommandDescriptor(TreeMapCmd.class);
        assertThrows(UnsupportedOperationException.class,
                () -> descriptor.getOptions().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> descriptor.getNameToOptionMap().clear());

        String[] names = descriptor.getOptions().get(0).names();
        names[0] = "--changed";
        assertEquals("-m", descriptor.getOptions().get(0).names()[0]);
        assertThrows(QCmdException.class,
                () -> descriptor.registerOption(descriptor.getOptions().get(0)));
    }

    /** 同一元素上的 @Parameter 与 @Vars 语义冲突，应在建模时立即报错。 */
    @Test
    public void testParameterAndVarsAreMutuallyExclusive() {
        QCmdException e = assertThrows(QCmdException.class,
                () -> new CommandDescriptor(ConflictingAnnotationsCmd.class));
        assertTrue(e.getMessage().contains("不能同时声明"));
    }
}
