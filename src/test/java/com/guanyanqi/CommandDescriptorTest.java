package com.guanyanqi;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.annotation.Vars;
import com.guanyanqi.converter.QStringConverter;
import com.guanyanqi.core.CommandDescriptor;
import com.guanyanqi.exception.QCmdException;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CommandDescriptorTest {

    public static class CustomVarConverter implements QStringConverter<String> {
        @Override
        public String convert(String value) {
            return "var:" + value;
        }
    }

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

    @Test
    public void testNullTargetClass() {
        try {
            new CommandDescriptor(null);
            Assert.fail();
        } catch (NullPointerException e) {
            Assert.assertTrue(e.getMessage().contains("Target class must not be null"));
        }
    }

    @Test
    public void testTreeMapTypeConversion() {
        TreeMapCmd cmd = QCmd.of(new String[]{"tree-map", "-m", "k1=v1"}).parse(TreeMapCmd.class);
        Assert.assertNotNull(cmd.map);
        Assert.assertTrue(cmd.map instanceof TreeMap);
        Assert.assertEquals("v1", cmd.map.get("k1"));
    }

    @Test
    public void testCustomVarConverters() {
        SingleCustomVarCmd cmd1 = QCmd.of(new String[]{"single-custom-var", "val1"}).parse(SingleCustomVarCmd.class);
        Assert.assertEquals("var:val1", cmd1.var);

        ListCustomVarCmd cmd2 = QCmd.of(new String[]{"list-custom-var", "val1", "val2"}).parse(ListCustomVarCmd.class);
        Assert.assertEquals(List.of("var:val1", "var:val2"), cmd2.vars);
    }

    @Test
    public void testRawTypesConversion() {
        RawTypeCmd rawResult = QCmd.of(new String[]{
                "raw-type",
                "-l", "item1,item2",
                "-m", "k1=v1,k2=v2",
                "-hm", "hk1=hv1",
                "pos1", "pos2"
        }).parse(RawTypeCmd.class);

        Assert.assertNotNull(rawResult.rawList);
        Assert.assertEquals(List.of("item1", "item2"), rawResult.rawList);
        Assert.assertNotNull(rawResult.rawMap);
        Assert.assertEquals("v1", rawResult.rawMap.get("k1"));
        Assert.assertNotNull(rawResult.explicitHashMap);
        Assert.assertEquals("hv1", rawResult.explicitHashMap.get("hk1"));
        Assert.assertNotNull(rawResult.rawVars);
        Assert.assertEquals(List.of("pos1", "pos2"), rawResult.rawVars);
    }

    @Test
    public void testBadStringConstructorException() {
        try {
            QCmd.of(new String[]{"bad-ctor", "-b", "val"}).parse(BadCtorCmd.class);
            Assert.fail();
        } catch (QCmdException e) {
            Assert.assertTrue(e.getMessage().contains("解析绑定"));
        }
    }
}
