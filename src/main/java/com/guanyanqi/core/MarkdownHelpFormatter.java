package com.guanyanqi.core;

import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.core.model.OptionDescriptor;
import com.guanyanqi.core.model.VarsDescriptor;

/**
 * Markdown 风格帮助文档格式化器。
 * <p>
 * 输出适合嵌入 GitHub、文档站点等 Markdown 渲染环境的排版。
 * 使用三级标题、表格、粗体、代码块等 Markdown 语法。
 * </p>
 *
 * @author guanyanqi
 */
public class MarkdownHelpFormatter implements HelpFormatter {

    @Override
    public String format(CommandDescriptor descriptor) {
        Cmd cmdAnno = descriptor.getCmdAnnotation();
        String cmds = String.join(", ", cmdAnno.names());
        StringBuilder md = new StringBuilder();

        // 命令标题
        md.append("### `").append(cmds).append("`\n\n");

        // 功能描述
        if (cmdAnno.desc() != null && !cmdAnno.desc().trim().isEmpty()) {
            md.append("> ").append(cmdAnno.desc()).append("\n\n");
        }

        // 使用方式
        md.append("**使用方法**\n\n```\n");
        md.append(cmds.split(",")[0].trim());
        md.append(" [参数 参数值] [变量...]\n```\n\n");

        // 参数表格
        boolean hasOption = false;
        for (OptionDescriptor option : descriptor.getOptions()) {
            if (!hasOption) {
                md.append("**参数说明**\n\n");
                md.append("| 选项 | 类型 | 必填 | 说明 |\n");
                md.append("|------|------|------|------|\n");
                hasOption = true;
            }
            String names = String.join(", ", option.names());
            String required = option.required() ? "*是*" : "否";
            String desc = option.desc() != null && !option.desc().trim().isEmpty()
                    ? option.desc() : "—";
            if (option.valueValidDesc() != null && !option.valueValidDesc().trim().isEmpty()) {
                desc += "（" + option.valueValidDesc() + "）";
            }
            md.append("| `").append(names).append("` | ")
              .append(option.type().getSimpleName()).append(" | ")
              .append(required).append(" | ")
              .append(desc).append(" |\n");
        }

        // 位置变量描述
        VarsDescriptor varsDesc = descriptor.getVarsDescriptor();
        if (varsDesc != null && varsDesc.desc() != null && !varsDesc.desc().trim().isEmpty()) {
            md.append("\n**位置变量**\n\n");
            md.append("> ").append(varsDesc.desc()).append("\n");
        }

        return md.toString();
    }
}
