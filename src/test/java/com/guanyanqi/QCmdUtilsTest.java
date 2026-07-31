package com.guanyanqi;

import com.guanyanqi.exception.QCmdException;
import com.guanyanqi.utils.QCmdUtils;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * QCmdUtils 工具方法的单元测试。
 * <p>
 * 覆盖 getAllFieldsList（含继承）和 createCollectionByType（含全部默认类型和异常路径）。
 * </p>
 *
 * @author guanyanqi
 */
public class QCmdUtilsTest {

    /** 继承层次测试类 */
    public static class Parent {
        public String parentField;
    }

    public static class Child extends Parent {
        public String childField;
    }

    /** 有公开无参构造方法的自定义集合子类 */
    public static class CustomCollection extends ArrayList<String> {
        public CustomCollection() {}
    }

    /** 只有私有构造方法的集合子类，createCollectionByType 应抛异常 */
    public static class PrivateCollection extends ArrayList<String> {
        private PrivateCollection(String dummy) {}
    }

    /** getAllFieldsList 应返回包含父类字段在内的全部字段 */
    @Test
    public void testGetAllFieldsList() {
        var fields = QCmdUtils.getAllFieldsList(Child.class);
        assertEquals(2, fields.size());
    }

    /** 覆盖 List / Collection / Set / Queue / Deque / 自定义子类 的实例创建 */
    @Test
    public void testCreateCollectionByType() throws Exception {
        assertNotNull(QCmdUtils.createCollectionByType(List.class));
        assertNotNull(QCmdUtils.createCollectionByType(Collection.class));
        assertNotNull(QCmdUtils.createCollectionByType(Set.class));
        assertNotNull(QCmdUtils.createCollectionByType(Queue.class));
        assertNotNull(QCmdUtils.createCollectionByType(Deque.class));
        assertNotNull(QCmdUtils.createCollectionByType(CustomCollection.class));
    }

    /** 只有私有构造方法的集合子类：createCollectionByType 应抛异常 */
    @Test
    public void testCreateCollectionByTypeFailure() {
        Exception e = assertThrows(Exception.class, () -> {
            QCmdUtils.createCollectionByType(PrivateCollection.class);
        });
        assertTrue(e.getMessage().contains("没有默认无参构造方法"));
    }
}
