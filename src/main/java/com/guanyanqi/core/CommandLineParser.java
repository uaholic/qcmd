package com.guanyanqi.core;

import com.guanyanqi.exception.QCmdException;

import java.util.*;

/**
 * POSIX / GNU 风格命令行参数解析器（负责分词分流与 Token 匹配提取）。
 *
 * <p>解析逻辑流程：
 * 1. 校验首个 Token 是否匹配命令类的名称（{@code @Cmd(names = ...)}）。
 * 2. 顺序遍历后续 Token：
 *    - 若以 "-" 开头且属于 Boolean 布尔开关选项，则直接存入值为 "true"。
 *    - 若以 "-" 开头且为非布尔选项，则消费下一个 Token 作为该选项的参数值。
 *    - 若不以 "-" 开头，则作为未指定名称的位置变量（Positional Variables）归集。
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
     *
     * @param args       命令行输入的原始参数数组
     * @param descriptor 命令类的描述符元数据
     * @return 解析分流后的结果 ParseResult
     * @throws QCmdException 当命令名不匹配、命令行为空或选项缺少值时抛出
     */
    public static ParseResult parse(String[] args, CommandDescriptor descriptor) {
        if (args == null || args.length == 0) {
            throw new QCmdException("命令行内容为空");
        }

        List<String> tokens = new ArrayList<>(Arrays.asList(args));
        // 第 0 个 Token 必须为命令匹配名称
        String cmd = tokens.get(0);

        if (!descriptor.getCommandNames().contains(cmd)) {
            throw new QCmdException("输入的命令 [" + cmd + "] 与目标类声明的命令 " + descriptor.getCommandNames() + " 不匹配");
        }

        Map<String, String> optionValues = new HashMap<>();
        List<String> positionalVars = new ArrayList<>();
        Set<String> boolOptions = descriptor.getBoolOptionNames();

        // 从第 1 个 Token 开始进行分词状态机解析
        for (int i = 1; i < tokens.size(); i++) {
            String curr = tokens.get(i);

            // 判断是否为选项开关（以 - 或 -- 开头）
            if (curr.startsWith("-")) {
                if (boolOptions.contains(curr)) {
                    // 场景 A：Boolean 无值开关选项（例如 -v），直接标记为 "true"
                    optionValues.put(curr, "true");
                } else {
                    // 场景 B：带值选项（例如 -port 8080），向前指针递增消费下一个 Token 作为参数值
                    if (i + 1 < tokens.size()) {
                        String next = tokens.get(i + 1);
                        optionValues.put(curr, next);
                        i++; // 跳过已消费的参数值 Token
                    } else {
                        throw new QCmdException("参数选项 [" + curr + "] 缺少对应的参数值");
                    }
                }
            } else {
                // 场景 C：不以 - 开头的纯文本，归集为位置变量（Positional Vars）
                positionalVars.add(curr);
            }
        }

        return new ParseResult(cmd, optionValues, positionalVars);
    }
}
