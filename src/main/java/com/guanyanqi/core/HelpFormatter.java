package com.guanyanqi.core;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.core.model.OptionDescriptor;
import com.guanyanqi.core.model.VarsDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * 纯粹的帮助文档格式化生成器（彻底类型无关 Type-Agnostic）。
 * 零 isRecord 与 零 instanceof 条件判断。
 *
 * @author guanyanqi
 */
public class HelpFormatter {

    public static String formatHelp(CommandDescriptor descriptor) {
        StringBuilder usage = new StringBuilder("使用方法：命令 [参数 参数值] [变量...]\n");
        Cmd cmdAnno = descriptor.getCmdAnnotation();
        String cmds = String.join("|", cmdAnno.names());
        usage.append("命令：").append(cmds).append("\n");

        if (cmdAnno.desc() != null && !cmdAnno.desc().trim().isEmpty()) {
            usage.append("功能描述：").append(cmdAnno.desc()).append("\n");
        }

        List<String> paramsList = new ArrayList<>();
        for (OptionDescriptor option : descriptor.getOptions()) {
            paramsList.add(formatParamHelp(option));
        }

        if (!paramsList.isEmpty()) {
            usage.append("参数说明：\n");
            for (String p : paramsList) {
                usage.append("\t").append(p).append("\n");
            }
        }

        VarsDescriptor varsDesc = descriptor.getVarsDescriptor();
        if (varsDesc != null && varsDesc.desc() != null && !varsDesc.desc().trim().isEmpty()) {
            usage.append("变量描述：").append(varsDesc.desc()).append("\n");
        }

        return usage.toString();
    }

    private static String formatParamHelp(OptionDescriptor option) {
        String paramName = String.join("|", option.names());
        StringBuilder paramUsage = new StringBuilder("参数：");
        paramUsage.append(paramName);
        if (option.required()) {
            paramUsage.append("（必填）");
        } else {
            paramUsage.append("（可选）");
        }
        if (option.desc() != null && !option.desc().trim().isEmpty()) {
            paramUsage.append("，参数说明：").append(option.desc());
        }
        if (option.valueValidDesc() != null && !option.valueValidDesc().trim().isEmpty()) {
            paramUsage.append("，输入规则：").append(option.valueValidDesc());
        }
        return paramUsage.toString();
    }
}
