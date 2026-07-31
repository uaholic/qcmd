package com.guanyanqi.core;

import com.guanyanqi.core.parser.TokenHandlerChain;
import com.guanyanqi.exception.QCmdException;

import java.util.*;

/**
 * POSIX / GNU 风格命令行参数解析器（委托给可插拔的 TokenHandler 链）。
 *
 * <p>内部使用 {@link TokenHandlerChain#defaults()} 执行解析，
 * 等价于按序调用 6 个内置 handler：
 * TerminatorHandler → EqualsSignOptionHandler → BooleanFlagHandler →
 * NegativeNumberHandler → StandardOptionHandler → PositionalHandler。
 * </p>
 *
 * <p>若需自定义解析策略，推荐使用 {@code QCmd.of(args).withTokenHandlers(...)}，
 * 或直接调用 {@code TokenHandlerChain.builder()...build().execute(args, descriptor)}。
 * </p>
 *
 * @author guanyanqi
 */
public class CommandLineParser {

    /**
     * 命令行 Token 解析后的封装领域模型。
     *
     * @param commandName    命令名称
     * @param optionValues   选项名 -> 原始字符串值的映射表
     * @param positionalVars 剩余未具名位置变量列表
     */
    public record ParseResult(
            String commandName,
            Map<String, String> optionValues,
            List<String> positionalVars
    ) {}

    /**
     * 将原始命令行数组解析拆解为 ParseResult。
     * <p>使用默认处理器链 {@link TokenHandlerChain#defaults()}。</p>
     *
     * @param args       命令行输入的原始参数数组
     * @param descriptor 命令类的描述符元数据
     * @return 解析分流后的结果 ParseResult
     * @throws QCmdException 当命令名不匹配、命令行为空或选项缺少值时抛出
     */
    public static ParseResult parse(String[] args, CommandDescriptor descriptor) {
        return TokenHandlerChain.defaults().execute(args, descriptor);
    }
}
