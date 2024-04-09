package com.guanyanqi.annotation;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

/**
 * 用于标记命令行应用程序中命令类的注解，提供命令名称和描述。
 * 此注解允许命令行框架自动生成帮助文档，便于用户理解每个命令的用途和使用方式。
 *
 * 被此注解标记的类应该代表一个独立的命令，并包含与该命令相关的所有逻辑。
 *
 * 示例用法：
 * {@code @Cmd(names = {"login", "l"}, desc = "Logs the user into the system.")}
 *
 * {@code @Inherited}注解确保如果一个类被{@code @Cmd}注解标记，
 * 则它的子类也将继承这个注解，除非子类自己使用了{@code @Cmd}注解。
 *
 * @author guanyanqi
 */
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({TYPE})
@Inherited
public @interface Cmd {
    /**
     * 命令的名称数组。可以指定一个命令的多个别名，用户可以使用这些别名来触发命令。
     * 例如：{"login", "l"}
     *
     * @return 命令的名称或别名数组
     */
    String[] names();

    /**
     * 命令的描述文本，用于帮助生成详细的帮助信息。
     * 这个描述应该简洁明了地说明命令的功能和主要用途。
     *
     * @return 命令的描述文本
     */
    String desc() default "";
}
