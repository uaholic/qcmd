package com.guanyanqi.annotation;

import com.guanyanqi.converter.NoConverter;
import com.guanyanqi.converter.QStringConverter;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;

/**
 * 用于标记命令行工具类中字段/组件/方法的注解，定义如何从命令行参数映射值到这些属性。
 * 支持 POJO 字段、Java Record 组件以及 Accessor 方法。
 *
 * @author guanyanqi
 */
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({FIELD, METHOD, RECORD_COMPONENT, PARAMETER})
public @interface Parameter {

    /**
     * 命令行参数的名称列表。每个名称应以"-"或"--"开头。
     *
     * @return 命令行参数的名称数组
     */
    String[] names();

    /**
     * 参数的描述文本，用于生成帮助信息。
     *
     * @return 参数描述
     */
    String desc() default "";

    /**
     * 标记该参数是否为必需的。
     *
     * @return 参数是否必需
     */
    boolean required() default false;

    /**
     * 参数值有效性的正则表达式。
     *
     * @return 参数值有效性的正则表达式
     */
    String valueValidRegex() default "";

    /**
     * 参数值有效性描述。
     *
     * @return 参数值有效性描述
     */
    String valueValidDesc() default "";

    /**
     * 参数值的自定义转换器。
     *
     * @return 参数值的自定义转换器类
     */
    Class<? extends QStringConverter<?>> converter() default NoConverter.class;

}
