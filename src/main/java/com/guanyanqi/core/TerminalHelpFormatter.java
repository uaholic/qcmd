package com.guanyanqi.core;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.core.model.OptionDescriptor;
import com.guanyanqi.core.model.VarsDescriptor;

import java.util.ArrayList;
import java.util.List;

/**
 * 纯文本终端风格帮助文档格式化器——默认实现。
 * <p>
 * 输出格式：
 * <pre>
 * 使用方法：命令 [参数 参数值] [变量...]
 * 命令：deploy|dep
 * 功能描述：应用部署指令
 * 参数说明：
 *     参数：-e|--env（必填），参数说明：目标环境，输入规则：只能是 dev, test 或 prod
 *     参数：-t|--timeout（可选），参数说明：超时时间(秒)
 * 变量描述：部署产物路径列表
 * </pre>
 * </p>
 *
 * @author guanyanqi
 */
public class TerminalHelpFormatter implements HelpFormatter {

    @Override
    public String format(CommandDescriptor descriptor) {
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

    /** 单个选项的格式化 */
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
