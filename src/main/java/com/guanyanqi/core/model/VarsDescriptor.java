package com.guanyanqi.core.model;

import com.guanyanqi.converter.NoConverter;
import com.guanyanqi.converter.QStringConverter;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * 位置变量统一领域模型描述符（类型无关 Type-Agnostic）。
 * <p>
 * 用于描述 {@link com.guanyanqi.annotation.Vars} 注解标记的位置变量元数据。
 * </p>
 *
 * @author guanyanqi
 */
public class VarsDescriptor {

    private final String desc;
    private final Class<? extends QStringConverter<?>> elementConverterClass;
    private final Class<?> type;
    private final Type genericType;
    private final String targetName;
    private final AnnotatedElement rawElement;

    /**
     * 构造函数。
     *
     * @param desc                  位置变量描述说明
     * @param elementConverterClass 元素自定义转换器 Class
     * @param type                  目标类型
     * @param genericType           泛型类型
     * @param targetName            字段名或组件名
     * @param rawElement            底层反射元素
     */
    public VarsDescriptor(String desc,
                          Class<? extends QStringConverter<?>> elementConverterClass,
                          Class<?> type,
                          Type genericType,
                          String targetName,
                          AnnotatedElement rawElement) {
        this.desc = desc != null ? desc : "";
        this.elementConverterClass = elementConverterClass != null ? elementConverterClass : NoConverter.class;
        this.type = Objects.requireNonNull(type, "Vars type must not be null");
        this.genericType = genericType != null ? genericType : type;
        this.targetName = Objects.requireNonNull(targetName, "Vars targetName must not be null");
        this.rawElement = Objects.requireNonNull(rawElement, "Vars rawElement must not be null");
    }

    /**
     * 获取位置变量描述说明。
     */
    public String desc() { return desc; }

    /**
     * 获取元素自定义类型转换器 Class。
     */
    public Class<? extends QStringConverter<?>> elementConverterClass() { return elementConverterClass; }

    /**
     * 获取变量目标 Class 类型。
     */
    public Class<?> type() { return type; }

    /**
     * 获取变量目标泛型 Type。
     */
    public Type genericType() { return genericType; }

    /**
     * 获取属性名或组件名。
     */
    public String targetName() { return targetName; }

    /**
     * 获取底层原始反射元素。
     */
    public AnnotatedElement rawElement() { return rawElement; }
}
