package com.guanyanqi.core;

/**
 * 帮助文档格式化策略接口。
 * <p>
 * 不同的输出场景（终端、网页、聊天框等）可以实现各自适配的排版风格。
 * 默认提供 {@link TerminalHelpFormatter}（纯文本终端格式）。
 * </p>
 *
 * @author guanyanqi
 * @see TerminalHelpFormatter
 */
@FunctionalInterface
public interface HelpFormatter {

    /**
     * 根据命令描述符生成格式化帮助文本。
     *
     * @param descriptor 命令元数据描述符
     * @return 格式化后的帮助文本
     */
    String format(CommandDescriptor descriptor);
}
