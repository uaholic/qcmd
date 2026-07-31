package com.guanyanqi.core.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 解析过程中的可变累积状态（仅在 parse 方法内部使用，不对外暴露）。
 * <p>
 * 持有选项值映射、位置变量列表和终止符标志。
 * TokenHandler 通过修改 ParseState 来影响后续 handler 的行为（如终止符标志）。
 * </p>
 *
 * @author guanyanqi
 */
public class ParseState {

    final Map<String, String> optionValues = new HashMap<>();
    final List<String> positionalVars = new ArrayList<>();
    boolean terminatorSeen = false;

    public boolean isTerminatorSeen() {
        return terminatorSeen;
    }

    public void setTerminatorSeen(boolean terminatorSeen) {
        this.terminatorSeen = terminatorSeen;
    }

    /**
     * 将 TokenResult 应用到当前状态。
     */
    void apply(TokenResult result) {
        switch (result.kind()) {
            case OPTION:
            case BOOL_FLAG:
                optionValues.put(result.optionName(), result.optionValue());
                break;
            case POSITIONAL:
                positionalVars.add(result.optionValue());
                break;
            case SKIP:
                // "--" 终止符：不存储任何值，但设置终止标志
                break;
        }
    }
}
