package com.guanyanqi.core;

import com.guanyanqi.ParseAction;
import com.guanyanqi.core.parser.TokenHandlerChain;
import com.guanyanqi.exception.QCmdException;

import java.util.*;

/**
 * 常见命令行参数写法的解析器（委托给可插拔的 TokenHandler 链）。
 *
 * <p>内部使用 {@link TokenHandlerChain#defaults()} 执行解析，
 * 等价于按序调用 7 个内置 handler：
 * TerminatorHandler → BuiltInActionHandler → EqualsSignOptionHandler → BooleanFlagHandler →
 * NegativeNumberHandler → StandardOptionHandler → PositionalHandler。
 * </p>
 *
 * <p>若需自定义解析策略，推荐使用 {@code QCmd.of(args).withTokenHandlers(...)}，
 * 或直接调用 {@code TokenHandlerChain.builder()...build().execute(args, descriptor)}。
 * </p>

 * @author guanyanqi
 */
public class CommandLineParser {

    /**
     * 工具类私有构造函数。
     */
    private CommandLineParser() {
    }

    /**
     * 命令行 Token 解析后的封装领域模型。
     *
     * @param commandName    命令名称
     * @param optionValues   选项名 -&gt; 原始字符串值的映射表
     * @param positionalVars 剩余未具名位置变量列表
     * @param actionOption   内置动作选项，未触发时为 null
     * @param action         强类型内置动作，未触发时为 EXECUTE
     */
    public record ParseResult(
            String commandName,
            Map<String, String> optionValues,
            List<String> positionalVars,
            String actionOption,
            ParseAction action
    ) {
        /** 四参构造，兼容原有 ParseResult 创建方式。 */
        public ParseResult(String commandName,
                           Map<String, String> optionValues,
                           List<String> positionalVars,
                           String actionOption) {
            this(commandName, optionValues, positionalVars, actionOption,
                    ParseAction.fromOptionName(actionOption));
        }

        /** 三参构造，兼容无 action 的场景。 */
        public ParseResult(String commandName,
                           Map<String, String> optionValues,
                           List<String> positionalVars) {
            this(commandName, optionValues, positionalVars, null);
        }

        /** 保存解析结果快照，不暴露解析器内部的可变集合。 */
        public ParseResult {
            optionValues = Collections.unmodifiableMap(new LinkedHashMap<>(optionValues));
            positionalVars = List.copyOf(positionalVars);
            action = action == null ? ParseAction.EXECUTE : action;
        }
    }

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
