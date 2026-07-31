# qcmd 扩展指南

本文档面向希望扩展 qcmd 能力的开发者，介绍库中所有开放的扩展点及推荐实践。

## 目录

- [自定义 Token 处理器](#自定义-token-处理器)
- [自定义帮助文档格式](#自定义帮助文档格式)
- [自定义类型转换器](#自定义类型转换器)
- [全局转换器注册](#全局转换器注册)
- [扩展点总览](#扩展点总览)

---

## 自定义 Token 处理器

### 接口定义

```java
@FunctionalInterface
public interface TokenHandler {
    /**
     * @param context 当前 token 不可变上下文（token 文本、位置、descriptor）
     * @param state   解析累积状态（optionValues、positionalVars、terminatorSeen）
     * @return 处理成功返回 TokenResult；不处理返回 null
     */
    TokenResult handle(TokenContext context, ParseState state);
}
```

### TokenResult 工厂方法

```java
TokenResult.option("-p", "8080", nextIndex);      // 命名选项
TokenResult.boolFlag("-v", nextIndex);             // 布尔开关
TokenResult.positional("raw-value", nextIndex);    // 位置变量
TokenResult.skip(nextIndex);                       // 跳过（"--" 自身）
```

### 示例：环境变量展开

```java
public class EnvVarHandler implements TokenHandler {
    @Override
    public TokenResult handle(TokenContext ctx, ParseState state) {
        if (state.isTerminatorSeen()) return null;

        String token = ctx.currentToken();
        if (token.startsWith("${") && token.endsWith("}")) {
            String varName = token.substring(2, token.length() - 1);
            String resolved = System.getenv().getOrDefault(varName, "");
            return TokenResult.positional(resolved, ctx.currentIndex() + 1);
        }
        return null;
    }
}
```

### 示例：Windows 风格 `/opt` 转 `--opt`

```java
public class WindowsStyleHandler implements TokenHandler {
    @Override
    public TokenResult handle(TokenContext ctx, ParseState state) {
        if (state.isTerminatorSeen()) return null;

        String token = ctx.currentToken();
        if (token.startsWith("/") && token.length() > 1) {
            String normalized = "-" + token.substring(1);
            if (ctx.hasNext()) {
                return TokenResult.option(normalized, ctx.peekNext(), ctx.currentIndex() + 2);
            }
            return TokenResult.boolFlag(normalized, ctx.currentIndex() + 1);
        }
        return null;
    }
}
```

### 注册

```java
QCmd.of(args)
    .withTokenHandlers(chain -> chain
        .prepend(new WindowsStyleHandler())              // 最优先处理
        .before(StandardOptionHandler.class, handler)    // 在标准选项前处理
        .after(BooleanFlagHandler.class, handler)        // 在布尔开关后处理
        .replace(PositionalHandler.class, handler)       // 替换
        .remove(NegativeNumberHandler.class)             // 移除
        .append(new EnvVarHandler())                     // 追加到末尾
    )
    .parse(MyCmd.class);
```

### 处理器顺序

```
TerminatorHandler
  → EqualsSignOptionHandler
  → BooleanFlagHandler
  → NegativeNumberHandler
  → StandardOptionHandler
  → PositionalHandler
```

每个 token 依次通过这些 handler，第一个返回非 null 结果的 handler 胜出。`PositionalHandler` 必须位于链末。

### 最佳实践

1. **始终检查 `state.isTerminatorSeen()`** — 终止符 `--` 之后的 token 不应被选项类 handler 处理
2. **小心消费 token 位置** — `nextIndex` 通常为 `currentIndex + 1`，消费后续 token 时为 `currentIndex + 2`
3. **具名类优于 Lambda** — 需要通过 `before(Class, handler)` 按类型定位时必须用具名类
4. **优先使用 `withTokenHandlers`** — 这是 QCmd 唯一的解析扩展入口

---

## 自定义帮助文档格式

### 接口定义

```java
@FunctionalInterface
public interface HelpFormatter {
    String format(CommandDescriptor descriptor);
}
```

### 示例：Slack/飞书聊天框格式

```java
public class ChatHelpFormatter implements HelpFormatter {
    @Override
    public String format(CommandDescriptor descriptor) {
        StringBuilder sb = new StringBuilder();
        String cmd = descriptor.getCommandNames().iterator().next();
        sb.append("*命令*: `").append(cmd).append("`\n");

        Cmd cmdAnno = descriptor.getCmdAnnotation();
        if (isNotBlank(cmdAnno.desc())) {
            sb.append("*描述*: ").append(cmdAnno.desc()).append("\n");
        }

        for (OptionDescriptor opt : descriptor.getOptions()) {
            sb.append("• `").append(String.join(", ", opt.names()))
              .append("` — ").append(opt.desc())
              .append(opt.required() ? " (必填)" : " (可选)").append("\n");
        }

        VarsDescriptor vars = descriptor.getVarsDescriptor();
        if (vars != null && isNotBlank(vars.desc())) {
            sb.append("• _位置变量_: ").append(vars.desc()).append("\n");
        }
        return sb.toString();
    }
}
```

### 注册

```java
QCmd.of(args)
    .withHelpFormatter(new ChatHelpFormatter())
    .parse(MyCmd.class);

// 或 Lambda
QCmd.of(args)
    .withHelpFormatter(d -> "USAGE: " + d.getCommandNames())
    .parse(MyCmd.class);
```

### 内置实现

| 实现 | 输出风格 |
|---|---|
| `TerminalHelpFormatter`（默认） | 纯文本列表 |
| `MarkdownHelpFormatter` | Markdown 表格 + 标题 |

---

## 自定义类型转换器

### 接口定义

```java
@FunctionalInterface
public interface QStringConverter<T> {
    T convert(String value);
}
```

### 示例：解析 `host:port`

```java
public record ServerAddress(String host, int port) {}

public class ServerAddressConverter implements QStringConverter<ServerAddress> {
    @Override
    public ServerAddress convert(String value) {
        String[] parts = value.split(":");
        return new ServerAddress(parts[0], Integer.parseInt(parts[1]));
    }
}

// 注解级别声明
@Parameter(names = "-s", converter = ServerAddressConverter.class)
ServerAddress server;

// @Vars 元素转换器
@Vars(elementConverter = ServerAddressConverter.class)
List<ServerAddress> servers;
```

---

## 全局转换器注册

```java
ConverterRegistry.register(MyCustomType.class, value -> new MyCustomType(value));
```

注册后所有命令类的 `MyCustomType` 字段自动使用该转换器。

### 优先级

1. 注解 `converter` Class（最高）
2. 全局 `ConverterRegistry`
3. Enum 自动匹配
4. Collection / Map 拆分
5. String 参数构造方法兜底（最低）

---

## 扩展点总览

| 扩展点 | 接口 | 注册方式 | 范围 |
|---|---|---|---|
| Token 解析策略 | `TokenHandler` | `QCmd.withTokenHandlers()` | 单次解析 |
| 帮助文档格式 | `HelpFormatter` | `QCmd.withHelpFormatter()` | 单次解析 |
| 类型转换 | `QStringConverter<T>` | 注解 `converter` 属性 | 单个字段 |
| 全局类型转换 | `QStringConverter<T>` | `ConverterRegistry.register()` | 全局 |
| Vars 元素转换 | `QStringConverter<T>` | `@Vars(elementConverter=...)` | 单个命令 |
