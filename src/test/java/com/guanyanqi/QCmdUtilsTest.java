package com.guanyanqi;

import com.guanyanqi.exception.QCmdException;
import com.guanyanqi.utils.QCmdUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class QCmdUtilsTest {

    public static class Parent {
        public String parentField;
    }

    public static class Child extends Parent {
        public String childField;
    }

    public static class CustomCollection extends ArrayList<String> {
        public CustomCollection() {}
    }

    public static class PrivateCollection extends ArrayList<String> {
        private PrivateCollection(String dummy) {}
    }

    @Test
    public void testIsNotBlank() {
        Assert.assertFalse(QCmdUtils.isNotBlank(null));
        Assert.assertFalse(QCmdUtils.isNotBlank("   "));
        Assert.assertTrue(QCmdUtils.isNotBlank("ok"));
    }

    @Test
    public void testIsNotEmpty() {
        Assert.assertFalse(QCmdUtils.isNotEmpty(null));
        Assert.assertFalse(QCmdUtils.isNotEmpty(Collections.emptyList()));
        Assert.assertTrue(QCmdUtils.isNotEmpty(List.of("1")));
    }

    @Test
    public void testGetAllFieldsList() {
        var fields = QCmdUtils.getAllFieldsList(Child.class);
        Assert.assertEquals(2, fields.size());
    }

    @Test
    public void testCreateCollectionByType() throws Exception {
        Assert.assertNotNull(QCmdUtils.createCollectionByType(List.class));
        Assert.assertNotNull(QCmdUtils.createCollectionByType(Collection.class)); // 覆盖 Collection.class == type 分支
        Assert.assertNotNull(QCmdUtils.createCollectionByType(Set.class));
        Assert.assertNotNull(QCmdUtils.createCollectionByType(Queue.class));
        Assert.assertNotNull(QCmdUtils.createCollectionByType(Deque.class));
        Assert.assertNotNull(QCmdUtils.createCollectionByType(CustomCollection.class));
    }

    @Test
    public void testCreateCollectionByTypeFailure() {
        try {
            QCmdUtils.createCollectionByType(PrivateCollection.class);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getMessage().contains("没有默认无参构造方法"));
        }
    }
}
