package com.guanyanqi.converter;

/**
 * 一个实现 {@link QStringConverter} 接口的默认转换器类，不对字符串值进行任何转换。
 *
 * 该转换器主要用于参数解析过程中，当不需要对参数值进行特定的类型转换时可以使用此转换器。
 * 它简单地返回输入的字符串值本身，确保了字符串在转换过程中保持不变。
 *
 * 示例用法：
 * 在注册转换器到 {@link ConverterRegistry} 时，如果某个字段不需要特殊的转换逻辑，
 * 可以使用此转换器作为其默认转换器：
 * {@code ConverterRegistry.register(MyClass.class, NoConverter.getInstance());}
 *
 * 由于其简单的行为，`NoConverter` 也可作为转换器注册过程的占位符，
 * 明确指出某些字段不应用任何转换逻辑。
 *
 * 注意：尽管此类的行为简单，但显式使用 `NoConverter` 可以增加代码的可读性和意图的明确性，
 * 相对于直接使用 null 或省略转换器注册更为推荐。
 *
 * @author guanyanqi
 */
public class NoConverter implements QStringConverter<String> {

    /**
     * 将给定的字符串值直接返回，不进行任何转换。
     *
     * @param value 待转换的字符串值
     * @return 与输入相同的字符串值
     */
    @Override
    public String convert(String value) {
        return value;
    }
}
