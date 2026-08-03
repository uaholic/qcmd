package com.guanyanqi.core.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
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

    final Map<String, String> optionValues = new LinkedHashMap<>();
    final List<String> positionalVars = new ArrayList<>();
    boolean terminatorSeen = false;
    /** 检测到的内置动作选项名（如 "--help"、"--version"），未触发为 null */
    String actionOption;

    public boolean isTerminatorSeen() {
        return terminatorSeen;
    }

    public void setTerminatorSeen(boolean terminatorSeen) {
        this.terminatorSeen = terminatorSeen;
    }

    public String getActionOption() {
        return actionOption;
    }

    /**
     * 将 TokenResult 应用到当前状态。
     *
     * @param result Token 处理结果
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
            case ACTION:
                actionOption = result.optionName();
                break;
            case SKIP:
                break;
        }
    }
}
