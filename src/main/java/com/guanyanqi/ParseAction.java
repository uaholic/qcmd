package com.guanyanqi;

import java.util.Set;

/**
 * 描述一次命令行解析请求的处理结果。
 *
 * @author guanyanqi
 */
public enum ParseAction {
    /** 参数已正常解析并绑定到命令对象。 */
    EXECUTE(),
    /** 用户请求显示帮助文本。 */
    SHOW_HELP("-h", "--help"),
    /** 用户请求显示命令版本。 */
    SHOW_VERSION("-V", "--version");

    private final Set<String> optionNames;

    ParseAction(String... optionNames) {
        this.optionNames = Set.of(optionNames);
    }

    /**
     * 获取触发当前动作的内置选项名。
     *
     * @return 不可变的选项名集合
     */
    public Set<String> optionNames() {
        return optionNames;
    }

    /**
     * 将内置选项名解析为动作，普通选项返回 {@link #EXECUTE}。
     *
     * @param optionName 选项名
     * @return 对应的解析动作
     */
    public static ParseAction fromOptionName(String optionName) {
        for (ParseAction action : values()) {
            if (action.optionNames.contains(optionName)) {
                return action;
            }
        }
        return EXECUTE;
    }
}
