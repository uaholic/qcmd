package com.guanyanqi.annotation;

import com.guanyanqi.converter.NoConverter;
import com.guanyanqi.converter.QStringConverter;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

/**
 * 用于标记接收命令行变量参数的字段。这些变量参数通常不是由键值对指定的，
 * 而是直接在命令行中以列表形式提供。此注解允许将这些变量参数自动转换并注入到指定的字段中。
 *
 * 注解还支持为变量参数指定一个自定义的转换器，用于将输入的字符串转换为字段期望的类型。
 *
 * 示例用法：
 * 如果命令行程序接受一个或多个未命名的参数，如文件路径列表，可以这样使用：
 * {@code @Vars(desc = "List of file paths to process", elementConverter = FilePathConverter.class)}
 *
 * 注意：一个命令接收类最多仅能有一个字段被此注解标记
 *
 * @author guanyanqi
 */
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({FIELD})
public @interface Vars {
    /**
     * 变量参数的描述，用于生成帮助信息。
     * 该描述应简洁明了地说明这些变量参数的用途和预期格式。
     *
     * @return 变量参数的描述文本
     */
    String desc() default "";

    /**
     * 用于变量参数的元素转换器。该转换器负责将接收到的字符串变量参数转换为字段所期望的类型。
     * 如果未指定转换器，则使用默认的转换逻辑。
     *
     * @return 变量参数的自定义转换器类
     */
    Class<? extends QStringConverter<?>> elementConverter() default NoConverter.class;

}
