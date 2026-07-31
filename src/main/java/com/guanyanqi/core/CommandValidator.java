package com.guanyanqi.core;

import com.guanyanqi.core.model.OptionDescriptor;
import com.guanyanqi.exception.InvalidParameterValueException;
import com.guanyanqi.exception.MissingParameterException;
import com.guanyanqi.exception.QCmdException;
import com.guanyanqi.exception.UnknownOptionException;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 纯粹的参数规则校验器（彻底类型无关 Type-Agnostic）。
 * 零 isRecord 与 零 instanceof 条件判断。
 *
 * @author guanyanqi
 */
public class CommandValidator {

    /**
     * 工具类私有构造函数。
     */
    private CommandValidator() {
    }

    /**
     * 校验解析出的命令行选项和位置变量是否合法。
     *
     * @param parseResult 解析结果 ParseResult
     * @param descriptor  命令描述符
     */
    public static void validate(CommandLineParser.ParseResult parseResult, CommandDescriptor descriptor) {
        Map<String, String> optionValues = parseResult.optionValues();
        String primaryCmd = descriptor.getCommandNames().iterator().next();

        // 1. 校验未知参数与正则匹配规则
        for (Map.Entry<String, String> entry : optionValues.entrySet()) {
            String optionName = entry.getKey();
            String value = entry.getValue();

            OptionDescriptor option = descriptor.getNameToOptionMap().get(optionName);
            if (option == null) {
                throw new UnknownOptionException(primaryCmd, optionName);
            }

            if (option.valueValidRegex() != null && !option.valueValidRegex().isEmpty()) {
                if (!Pattern.matches(option.valueValidRegex(), value)) {
                    throw new InvalidParameterValueException(primaryCmd, optionName, value, option.valueValidDesc());
                }
            }
        }

        // 2. 校验必填参数组
        for (List<String> group : descriptor.getRequiredOptionGroups()) {
            boolean present = group.stream().anyMatch(optionValues::containsKey);
            if (!present) {
                throw new MissingParameterException(primaryCmd, group);
            }
        }

        // 3. 校验位置变量
        if (!parseResult.positionalVars().isEmpty() && descriptor.getVarsDescriptor() == null) {
            throw new QCmdException("命令 [" + primaryCmd + "] 不支持接收位置变量");
        }
    }
}
