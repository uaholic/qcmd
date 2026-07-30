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
 * 用于标记命令行工具类中变量属性的注解，用于捕获未被明确指定名称的变量参数。
 *
 * @author guanyanqi
 */
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({FIELD, METHOD, RECORD_COMPONENT, PARAMETER})
public @interface Vars {

    /**
     * 变量属性的描述信息，用于自动生成命令行工具的帮助手册。
     *
     * @return 变量描述
     */
    String desc() default "";

    /**
     * 变量集合元素的自定义转换器。
     *
     * @return 转换器类型
     */
    Class<? extends QStringConverter<?>> elementConverter() default NoConverter.class;

}
