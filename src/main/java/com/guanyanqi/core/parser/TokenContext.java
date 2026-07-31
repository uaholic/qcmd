package com.guanyanqi.core.parser;

import com.guanyanqi.core.CommandDescriptor;

import java.util.List;

/**
 * 当前正在处理的 token 的不可变上下文。
 * <p>
 * 封装了 token 文本本身、在原始参数列表中的位置、以及整个命令的元数据。
 * 提供 peekNext 方法让 handler 向前"窥视"后续 token。
* </p>
 *
 * @param currentToken 当前待处理的 token 文本
 * @param allTokens    完整的命令行 token 列表（含命令名）
 * @param currentIndex 当前 token 在 allTokens 中的下标
 * @param descriptor   命令描述符（共享不可变元数据）
 * @author guanyanqi
 */
public record TokenContext(
        String currentToken,
        List<String> allTokens,
        int currentIndex,
        CommandDescriptor descriptor) {

    /**
     * 是否还有下一个 token（不包含当前）。
     */
    public boolean hasNext() {
        return currentIndex + 1 < allTokens.size();
    }

    /**
     * 获取下一个 token（不消费，仅窥视）。
     *
     * @return 下一个 token，若不存在返回 null
     */
    public String peekNext() {
        return hasNext() ? allTokens.get(currentIndex + 1) : null;
    }
}
