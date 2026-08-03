package com.guanyanqi;

/**
 * 描述一次命令行解析请求的处理结果。
 *
 * @author guanyanqi
 */
public enum ParseAction {
    /** 参数已正常解析并绑定到命令对象。 */
    EXECUTE,
    /** 用户请求显示帮助文本。 */
    SHOW_HELP,
    /** 用户请求显示命令版本。 */
    SHOW_VERSION
}
