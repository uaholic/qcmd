package com.guanyanqi.core.model;

import com.guanyanqi.converter.NoConverter;
import com.guanyanqi.converter.QStringConverter;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Objects;

/**
 * 命令行参数选项统一领域模型描述符（类型无关 Type-Agnostic）。
 * <p>
 * 该类将来自 POJO 的 {@link java.lang.reflect.Field} 或 Java Record 的 {@link java.lang.reflect.RecordComponent}
 * 的参数配置统一抽象为高阶领域模型，屏蔽底层反射差异。
 * </p>
 *
 * @author guanyanqi
 */
public class OptionDescriptor {

    private final String[] names;
    private final String desc;
    private final boolean required;
    private final String valueValidRegex;
    private final String valueValidDesc;
    private final Class<? extends QStringConverter<?>> converterClass;
    private final Class<?> type;
    private final Type genericType;
    private final String targetName;
    private final AnnotatedElement rawElement;

    /**
     * 构造函数。
     *
     * @param names             参数选项名称数组，如 {"-e", "--env"}
     * @param desc              参数功能说明描述
     * @param required          是否为必填参数
     * @param valueValidRegex   参数值正则表达式校验规则
     * @param valueValidDesc    参数值规则提示说明
     * @param converterClass    自定义类型转换器 Class
     * @param type              参数目标 Java 类型
     * @param genericType       参数泛型类型
     * @param targetName        字段名或组件名
     * @param rawElement        底层反射元素（Field 或 RecordComponent）
     */
    public OptionDescriptor(String[] names,
                            String desc,
                            boolean required,
                            String valueValidRegex,
                            String valueValidDesc,
                            Class<? extends QStringConverter<?>> converterClass,
                            Class<?> type,
                            Type genericType,
                            String targetName,
                            AnnotatedElement rawElement) {
        this.names = Objects.requireNonNull(names, "Option names must not be null");
        this.desc = desc != null ? desc : "";
        this.required = required;
        this.valueValidRegex = valueValidRegex != null ? valueValidRegex : "";
        this.valueValidDesc = valueValidDesc != null ? valueValidDesc : "";
        this.converterClass = converterClass != null ? converterClass : NoConverter.class;
        this.type = Objects.requireNonNull(type, "Option type must not be null");
        this.genericType = genericType != null ? genericType : type;
        this.targetName = Objects.requireNonNull(targetName, "Option targetName must not be null");
        this.rawElement = Objects.requireNonNull(rawElement, "Option rawElement must not be null");
    }

    /**
     * 获取参数选项名称列表（例如 `-e`, `--env`）。
     *
     * @return 参数选项名称数组
     */
    public String[] names() { return names; }

    /**
     * 获取参数描述说明。
     *
     * @return 参数描述字符串
     */
    public String desc() { return desc; }

    /**
     * 是否为必填参数。
     *
     * @return 必填返回 true，否则返回 false
     */
    public boolean required() { return required; }

    /**
     * 获取参数值正则表达式校验规则。
     *
     * @return 正则校验规则表达式
     */
    public String valueValidRegex() { return valueValidRegex; }

    /**
     * 获取参数值输入规则校验失败时的提示说明。
     *
     * @return 校验规则提示字符串
     */
    public String valueValidDesc() { return valueValidDesc; }

    /**
     * 获取自定义转换器 Class 类型。
     *
     * @return 转换器 Class
     */
    public Class<? extends QStringConverter<?>> converterClass() { return converterClass; }

    /**
     * 获取参数目标 Class 类型。
     *
     * @return 目标 Class
     */
    public Class<?> type() { return type; }

    /**
     * 获取参数泛型 Type 类型。
     *
     * @return 泛型 Type
     */
    public Type genericType() { return genericType; }

    /**
     * 获取字段名或组件名。
     *
     * @return 目标属性名称
     */
    public String targetName() { return targetName; }

    /**
     * 获取底层原始反射元素（Field 或 RecordComponent）。
     *
     * @return 反射 AnnotatedElement
     */
    public AnnotatedElement rawElement() { return rawElement; }
}
