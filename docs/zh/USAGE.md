# qcmd 使用指南

## 目录

- [快速开始](#快速开始)
- [注解详解](#注解详解)
- [POJO 模式](#pojo-模式)
- [类型转换](#类型转换)
- [校验规则](#校验规则)
- [帮助文本](#帮助文本)
- [进阶用法](#进阶用法)

---

## 快速开始

### 定义命令

使用 Java Record 定义命令行参数结构：

```java
@Cmd(names = {"deploy", "dep"}, desc = "应用部署指令")
public record DeployCmd(
    @Parameter(names = {"-e", "--env"}, required = true,
               valueValidRegex = "^(dev|test|prod)$",
               valueValidDesc = "只能是 dev, test 或 prod",
               desc = "目标环境")
    String env,

    @Parameter(names = {"-t", "--timeout"}, desc = "超时时间(秒)")
    int timeout,

    @Parameter(names = {"-d", "--dry-run"}, desc = "模拟试运行")
    boolean dryRun,

    @Vars(desc = "部署产物路径列表")
    List<String> artifacts
) {}
```

### 解析命令行

```java
String[] args = {"deploy", "-e", "prod", "-t", "30", "-d", "app.jar", "config.yaml"};

ParsedCommand<DeployCmd> parsed = QCmd.of(args).parse(DeployCmd.class);
DeployCmd cmd = parsed.value();

System.out.println(cmd.env());      // prod
System.out.println(cmd.timeout());  // 30
System.out.println(cmd.dryRun());   // true
System.out.println(cmd.artifacts());// [app.jar, config.yaml]
```

`parse()` 返回 `ParsedCommand<T>` 记录，包含 `value()`（命令实例）和 `helpText()`（帮助文档）。

---

## 注解详解

### @Cmd — 命令声明

| 属性 | 类型 | 说明 |
|---|---|---|
| `names` | `String[]` | **必填**，命令名称数组，如 `{"deploy", "dep"}` |
| `desc` | `String` | 命令功能描述，用于帮助文本 |

### @Parameter — 选项声明

| 属性 | 类型 | 说明 |
|---|---|---|
| `names` | `String[]` | 选项名称，如 `{"-e", "--env"}` |
| `required` | `boolean` | 是否必填，默认 `false` |
| `desc` | `String` | 参数说明 |
| `converter` | `Class<? extends QStringConverter>` | 自定义类型转换器 |
| `valueValidRegex` | `String` | 参数值正则校验 |
| `valueValidDesc` | `String` | 校验失败时的提示信息 |

每个 Field/RecordComponent 上 `@Parameter` 和 `@Vars` 互斥——只会匹配其一。

### @Vars — 位置变量声明

| 属性 | 类型 | 说明 |
|---|---|---|
| `desc` | `String` | 变量描述 |
| `elementConverter` | `Class<? extends QStringConverter>` | 元素自定义转换器 |

---

## POJO 模式

除 Record 外，qcmd 同样支持传统 POJO，且支持继承：

```java
@Cmd(names = "trans")
public class TransactionCmd {
    @Parameter(names = {"-a", "--amount"}, required = true)
    private double amount;

    @Parameter(names = {"-t", "--type"})
    private OperationType type;  // 自动 Enum 解析

    @Vars
    private List<String> files;
}
```

父类中声明的 `@Parameter` 字段在子类解析时依然生效。

---

## 类型转换

qcmd 内置支持 20+ 种常见 Java 类型，转换优先级如下：

| 优先级 | 转换方式 | 说明 |
|---|---|---|
| 1 | 注解声明的 `converter` | `@Parameter(converter = MyConverter.class)` |
| 2 | 全局注册转换器 | `ConverterRegistry.register(MyType.class, converter)` |
| 3 | Enum 自动匹配 | `Enum.valueOf(type, rawValue)` |
| 4 | Collection 拆分 | 默认 `,` 分割，递归转换每个元素 |
| 5 | Map 解析 | 默认 `key=value` 格式，递归转换 |
| 6 | String 构造器兜底 | `new MyType(rawValue)` |

### 自定义转换器

```java
public class ServerAddressConverter implements QStringConverter<ServerAddress> {
    @Override
    public ServerAddress convert(String value) {
        String[] parts = value.split(":");
        return new ServerAddress(parts[0], Integer.parseInt(parts[1]));
    }
}

@Parameter(names = "-s", converter = ServerAddressConverter.class)
ServerAddress server;
```

### 全局注册

```java
ConverterRegistry.register(MyCustomType.class, value -> new MyCustomType(value));
```

---

## 校验规则

### required — 必填校验

缺少必填参数时抛出 `MissingParameterException`。

### valueValidRegex — 正则校验

值不匹配正则时抛出 `InvalidParameterValueException`。

### 未知选项

传入未声明的选项名时抛出 `UnknownOptionException`。

---

## 帮助文本

`ParsedCommand.helpText()` 使用当前 HelpFormatter 生成帮助。默认使用 `TerminalHelpFormatter`（纯文本格式）。

```java
ParsedCommand<DeployCmd> parsed = QCmd.of(args).parse(DeployCmd.class);
System.out.println(parsed.helpText());
```

输出：

```
使用方法：命令 [参数 参数值] [变量...]
命令：deploy
功能描述：应用部署指令
参数说明：
	参数：-e|--env（必填），参数说明：目标环境，输入规则：只能是 dev, test 或 prod
	参数：-t|--timeout（可选），参数说明：超时时间(秒)
	参数：-d|--dry-run（可选），参数说明：模拟试运行
变量描述：部署产物路径列表
```

### 切换帮助格式

```java
// Markdown 表格风格
QCmd.of(args)
    .withHelpFormatter(new MarkdownHelpFormatter())
    .parse(DeployCmd.class);

// Lambda 自定义
QCmd.of(args)
    .withHelpFormatter(d -> "USAGE: " + d.getCommandNames())
    .parse(MyCmd.class);
```

Markdown 格式输出：

```
### `deploy`

> 应用部署指令

| 选项 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `-e, --env` | String | *是* | 目标环境（只能是 dev, test 或 prod） |
| `-t, --timeout` | int | 否 | 超时时间(秒) |
| `-d, --dry-run` | boolean | 否 | 模拟试运行 |
```

---

## 进阶用法

### 自定义 Token 处理器链

qcmd 的解析器由可插拔的 `TokenHandler` 处理器链构成：

```java
QCmd.of(args)
    .withTokenHandlers(chain -> chain
        .prepend(new WindowsStyleHandler())   // 前插
        .append(new EnvVarHandler())          // 追加
        .remove(NegativeNumberHandler.class)  // 移除
    )
    .parse(MyCmd.class);
```

内置 6 个处理器按执行顺序：

| 处理器 | 职责 |
|---|---|
| `TerminatorHandler` | `--` 终止符 |
| `EqualsSignOptionHandler` | `--key=value` 等号语法 |
| `BooleanFlagHandler` | 布尔开关 |
| `NegativeNumberHandler` | 负数识别 |
| `StandardOptionHandler` | 标准选项 |
| `PositionalHandler` | 位置变量兜底 |

### Builder 操作

```java
TokenHandlerChain.Builder builder = TokenHandlerChain.builder()
    .defaults()                                     // 以默认链为基础
    .prepend(new MyEarlyHandler())                  // 加到最前
    .before(StandardOptionHandler.class, handler)    // 在指定 handler 前插入
    .after(BooleanFlagHandler.class, handler)        // 在指定 handler 后插入
    .replace(NegativeNumberHandler.class, handler)   // 替换
    .remove(TerminatorHandler.class)                 // 移除
    .append(new MyCustomHandler());                  // 追加到末尾
```

### POSIX/GNU 兼容性

| 特性 | 示例 | 说明 |
|---|---|---|
| 标准选项 | `deploy -e prod` | 空格分隔 |
| 等号语法 | `deploy --env=prod` | GNU 风格 |
| 布尔开关 | `deploy -d` | 不消费值 |
| 终止符 | `deploy -- -v` | `--` 后全作位置变量 |
| 负数参数 | `deploy -t -30` | 不被误认为选项 |
| 短选项等号 | `deploy -e=prod` | 短选项也支持 |

---

## 相关文档

- [架构设计](ARCHITECTURE.md)
- [扩展指南](EXTENDING.md)
