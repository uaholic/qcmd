package com.guanyanqi.annotation;

import com.guanyanqi.converter.NoConverter;
import com.guanyanqi.converter.QStringConverter;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;

/**
 * 用于标记命令行工具类中字段的注解，定义如何从命令行参数映射值到这些字段。同时也用于针对命令的不同参数自动生成帮助文档。
 * 除了基本的参数映射，该注解还支持参数的自定义转换、参数描述和参数值的有效性校验。
 * 示例用法：
 * {@code @Parameter(names = {"-u", "--user"}, desc = "Specifies the username.", required = true)}
 *
 * @author guanyanqi
 */
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({FIELD})
public @interface Parameter {

    /**
     * 命令行参数的名称列表。每个名称应以"-"或"--"开头，表示短名称或长名称。
     * 例如: {"-u", "--user"}
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
     * 标记该参数是否为必需的。如果为true，解析命令行参数时未提供该参数将会抛出异常。
     *
     * @return 参数是否必需
     */
    boolean required() default false;

    /**
     * 参数值有效性的正则表达式。如果提供了该表达式，则参数值必须匹配该正则表达式。
     *
     * @return 参数值有效性的正则表达式
     */
    String valueValidRegex() default "";

    /**
     * 参数值有效性描述。当参数值不满足{@code valueValidRegex}定义的正则表达式时，将显示此描述。
     *
     * @return 参数值有效性描述
     */
    String valueValidDesc() default "";

    /**
     * 参数值的自定义转换器。该转换器用于将字符串参数值转换为字段的实际类型。
     * 如果未指定转换器，将使用默认转换逻辑。
     *
     * @return 参数值的自定义转换器类
     */
    Class<? extends QStringConverter<?>> converter() default NoConverter.class;

}
