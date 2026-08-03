# 变更日志 / Changelog

这里记录 qcmd 的重要变更。项目遵循语义化版本规范。

All notable changes to qcmd are documented here. The project follows semantic versioning.

## [Unreleased]

## [1.1.1] - 2026-08-03

### 修复 / Fixed

- 调用 `QCmd.of(args)` 后再修改原始参数数组，不再影响尚未执行的解析会话。 / Mutating the input array after `QCmd.of(args)` no longer changes a pending parse session.
- Token 处理器链定制器或帮助格式化器为 null 时，统一抛出 `QCmdException`。 / Null handler-chain customizers and help formatters now consistently produce `QCmdException`.

### 变更 / Changed

- 内置 help/version 动作改由默认 Token 处理器链识别；首个动作会结束 Token 扫描，`--` 后形似动作的参数仍按位置参数处理。 / Built-in help/version recognition now runs inside the default token-handler chain. The first action ends token scanning, while action-like tokens after `--` remain positional arguments.
- 解析结果保留集合的只读快照，不暴露解析器持有的可变状态。 / Parse results retain read-only collection snapshots instead of exposing parser-owned mutable state.
- 文档改为明确列出 qcmd 实际支持的命令行写法，不再作更宽泛的兼容性描述。 / Documentation now lists the concrete command-line forms qcmd supports without making a broader compatibility claim.

## [1.1.0] - 2026-08-03

### 新增 / Added

- 新增内置 `-h` / `--help` 动作，以及通过 `@Cmd.version` 启用的 `-V` / `--version` 动作。 / Added built-in `-h` / `--help` actions and optional `-V` / `--version` actions through `@Cmd.version`.
- 新增 `ParseAction`、`ParsedCommand.shouldExit()` 和 `ParsedCommand.outputText()`，用于正常处理 help/version 控制流。 / Added `ParseAction`, `ParsedCommand.shouldExit()`, and `ParsedCommand.outputText()` for normal help/version control flow.
- 新增独立的 `QCmd.help(...)` API。 / Added standalone `QCmd.help(...)` APIs.
- 支持嵌套泛型集合及 Map 值的递归转换。 / Added recursive conversion for nested generic collections and map values.
- CI 增加行覆盖率和分支覆盖率门禁。 / Added CI coverage gates for line and branch coverage.

### 修复 / Fixed

- 带值选项不再把紧随其后的其他选项 Token 吞作自己的值。 / Value-taking options no longer consume a following option token as their value.
- 命令行末尾的未知选项现在会抛出 `UnknownOptionException`。 / Unknown options at the end of the command line now produce `UnknownOptionException`.
- 负小数和科学计数法数值可以继续作为合法选项值。 / Negative decimals and scientific notation remain valid option values.
- 选项别名的优先级固定为命令行中最后一次出现者生效。 / Alias precedence is deterministic: the last occurrence on the command line wins.
- JUnit 4 测试及此前缺少注解的集成测试现已由 JUnit 5 正常执行。 / JUnit 4 tests and the previously unannotated integration test now run under JUnit 5.

### 变更 / Changed

- 描述信息和解析结果中的集合改为只读快照。 / Descriptor and parse-result collections are exposed as read-only snapshots.
- 注解声明的转换器改为每次解析独立实例化，不再全局缓存。 / Annotation-declared converters are instantiated per parse request instead of being globally cached.
- GPG 签名和 Maven Central 发布仅在 Maven `release` profile 中执行。 / GPG signing and Central publishing run only in the Maven `release` profile.
- Java 17 编译改用 `--release 17`。 / Java 17 compilation uses `--release 17`.
- 文档明确列出本版本实际支持的命令行写法。 / Documentation lists the concrete command-line forms supported by this release.

[Unreleased]: https://github.com/uaholic/qcmd/compare/v1.1.1...HEAD
[1.1.1]: https://github.com/uaholic/qcmd/compare/v1.1.0...v1.1.1
[1.1.0]: https://github.com/uaholic/qcmd/compare/v1.0.1...v1.1.0
