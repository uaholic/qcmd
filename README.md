# 🚀 qcmd: Zero-Dependency, Record-First CLI Argument Parser for Java 17+

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)]()
[![Build](https://img.shields.io/badge/build-passing-brightgreen.svg)]()

> 🌐 [中文](#中文) | [English](#english)

---

## 中文

**qcmd**（Quick Command）是一个为现代 Java 17+ 打造的极简、轻量、**零外部依赖**、**原生支持 Java Record** 的注解驱动命令行参数解析框架。

能用一行代码，将复杂 Linux 命令行参数、标志位、正则校验、位置变量，直接装配到 POJO 或不可变 Record 中。

### 核心特性

| 特性 | 说明 |
|---|---|
| 🛡️ **零依赖** | 不引入任何第三方库，无依赖冲突 |
| 💎 **Record 原生支持** | 通过 RecordComponent + Canonical Constructor 直接绑定不可变 Record |
| ⚡ **POSIX/GNU 兼容** | `--key=value`、`--` 终止符、负数识别（`-a -123.45`） |
| 🎨 **类型转换管线** | 基本类型 / Enum / Collection / Map / 自定义 Converter / String 构造器兜底 |
| 🔍 **校验与帮助** | `required` 必填、`valueValidRegex` 正则、MissingParameterException / InvalidParameterValueException / UnknownOptionException |
| 🔌 **全链路可扩展** | Token 处理器链、HelpFormatter 帮助格式、Converter 类型转换均支持自定义 |

### 快速引入

```xml
<dependency>
    <groupId>com.guanyanqi</groupId>
    <artifactId>qcmd</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 30 秒快速上手

```java
@Cmd(names = {"deploy"}, desc = "应用部署指令")
public record DeployCmd(
    @Parameter(names = {"-e", "--env"}, required = true,
               valueValidRegex = "^(dev|test|prod)$", desc = "目标环境")
    String env,

    @Parameter(names = {"-t", "--timeout"}, desc = "超时时间")
    int timeout,

    @Parameter(names = {"-d", "--dry-run"}, desc = "模拟试运行")
    boolean dryRun,

    @Vars(desc = "部署包路径列表")
    List<String> files
) {}

// 一行解析
ParsedCommand<DeployCmd> parsed = QCmd.of(args).parse(DeployCmd.class);
DeployCmd cmd = parsed.value();

System.out.println(cmd.env());       // prod
System.out.println(cmd.timeout());   // 30
System.out.println(cmd.dryRun());    // true
System.out.println(cmd.files());     // [app.jar, config.yaml]
```

### 自定义类型转换器

```java
public record ServerAddress(String host, int port) {}

public class ServerAddressConverter implements QStringConverter<ServerAddress> {
    @Override
    public ServerAddress convert(String value) {
        String[] parts = value.split(":");
        return new ServerAddress(parts[0], Integer.parseInt(parts[1]));
    }
}

@Cmd(names = "connect")
public record ConnectCmd(
    @Parameter(names = "-s", converter = ServerAddressConverter.class)
    ServerAddress server
) {}
```

### 自定义帮助文档格式

内置 `TerminalHelpFormatter`（默认纯文本）和 `MarkdownHelpFormatter`（Markdown 表格），也可用 lambda 自定义：

```java
// 终端风格（默认）
ParsedCommand<DeployCmd> p = QCmd.of(args).parse(DeployCmd.class);
System.out.println(p.helpText());
// 命令：deploy
// 功能描述：应用部署指令
// 参数说明：
//     参数：-e|--env（必填），参数说明：目标环境

// Markdown 表格风格
ParsedCommand<DeployCmd> p = QCmd.of(args)
    .withHelpFormatter(new MarkdownHelpFormatter())
    .parse(DeployCmd.class);
// ### `deploy`
// > 应用部署指令
// | 选项 | 类型 | 必填 | 说明 |
// |------|------|------|------|
// | `-e, --env` | String | *是* | 目标环境 |

// Lambda 自定义
QCmd.of(args)
    .withHelpFormatter(d -> "USAGE: " + d.getCommandNames())
    .parse(MyCmd.class);
```

### 自定义 Token 解析器

```java
QCmd.of(args)
    .withTokenHandlers(chain -> chain
        .prepend(new WindowsStyleHandler())   // 前插 /opt → --opt 风格的 handler
        .append(new EnvVarExpander())         // 追加环境变量展开
    )
    .parse(MyCmd.class);
```

### 架构概览

| 模块 | 职责 |
|---|---|
| `QCmd` | 无状态门面入口 |
| `ParsedCommand<T>` | 不可变结果容器（value + helpText） |
| `CommandDescriptor` | 元数据提取 + 6 步类型转换管线 |
| `TokenHandlerChain` | 可插拔的 Chain of Responsibility 分词器 |
| `CommandValidator` | 未知选项 / 必填 / 正则校验 |
| `InstanceBinder` | Record / POJO 反射绑定（Strategy 模式） |
| `HelpFormatter` | 可替换的帮助文档格式化策略 |

### 扩展点

| 扩展点 | 接口 | 方式 |
|---|---|---|
| Token 解析 | `TokenHandler` | `QCmd.withTokenHandlers()` |
| 帮助格式 | `HelpFormatter` | `QCmd.withHelpFormatter()` |
| 类型转换 | `QStringConverter<T>` | `@Parameter(converter=...)` 或 `ConverterRegistry.register()` |

### 文档

- [使用指南](docs/zh/USAGE.md) · [English](docs/en/USAGE.md)
- [架构设计](docs/zh/ARCHITECTURE.md) · [English](docs/en/ARCHITECTURE.md)
- [扩展指南](docs/zh/EXTENDING.md) · [English](docs/en/EXTENDING.md)

### License

[MIT](LICENSE)

---

## English

**qcmd** (Quick Command) is a minimalist, zero-dependency, annotation-driven CLI argument parser for modern Java 17+. It maps command-line options, flags, regex rules, and positional variables directly onto POJOs or immutable Records — in a single line of code.

### Features

| Feature | Description |
|---|---|
| 🛡️ **Zero Dependencies** | No Guava, no Commons — zero runtime dependencies |
| 💎 **Native Record Support** | Direct binding via RecordComponent + Canonical Constructor |
| ⚡ **POSIX/GNU Compatible** | `--key=value` syntax, `--` terminator, negative number detection |
| 🎨 **Type Conversion Pipeline** | Primitives, enums, collections, maps, custom converters, String-ctor fallback |
| 🔍 **Validation & Help** | Required params, regex validation, typed exceptions, auto-generated help |
| 🔌 **Fully Extensible** | Custom token handlers, help formatters, and type converters |

### Quick Start

```xml
<dependency>
    <groupId>com.guanyanqi</groupId>
    <artifactId>qcmd</artifactId>
    <version>1.0.0</version>
</dependency>
```

```java
@Cmd(names = {"deploy"}, desc = "Application deployment command")
public record DeployCmd(
    @Parameter(names = {"-e", "--env"}, required = true,
               valueValidRegex = "^(dev|test|prod)$", desc = "Target environment")
    String env,

    @Parameter(names = {"-t", "--timeout"}, desc = "Timeout in seconds")
    int timeout,

    @Parameter(names = {"-d", "--dry-run"}, desc = "Dry run mode")
    boolean dryRun,

    @Vars(desc = "Artifact paths")
    List<String> files
) {}

// One-liner: parse + bind to immutable Record
ParsedCommand<DeployCmd> parsed = QCmd.of(args).parse(DeployCmd.class);
DeployCmd cmd = parsed.value();
```

### Custom Converter

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

### Custom Help Format

```java
// Terminal (default)
ParsedCommand<DeployCmd> p = QCmd.of(args).parse(DeployCmd.class);
System.out.println(p.helpText());

// Markdown table
QCmd.of(args).withHelpFormatter(new MarkdownHelpFormatter()).parse(DeployCmd.class);

// Lambda
QCmd.of(args).withHelpFormatter(d -> "USAGE: " + d.getCommandNames()).parse(MyCmd.class);
```

### Custom Token Handlers

```java
QCmd.of(args)
    .withTokenHandlers(chain -> chain
        .prepend(new WindowsStyleHandler())
        .append(new EnvVarExpander())
    )
    .parse(MyCmd.class);
```

### Architecture

| Module | Responsibility |
|---|---|
| `QCmd` | Stateless facade entry point |
| `ParsedCommand<T>` | Immutable result container |
| `CommandDescriptor` | Metadata extraction + 6-stage type conversion |
| `TokenHandlerChain` | Pluggable Chain of Responsibility tokenizer |
| `CommandValidator` | Unknown option / required / regex validation |
| `InstanceBinder` | Record / POJO reflection binding (Strategy pattern) |
| `HelpFormatter` | Swappable help format strategy |

### Extension Points

| Layer | Interface | Registration |
|---|---|---|
| Token parsing | `TokenHandler` | `QCmd.withTokenHandlers()` |
| Help format | `HelpFormatter` | `QCmd.withHelpFormatter()` |
| Type conversion | `QStringConverter<T>` | `@Parameter(converter=...)` or `ConverterRegistry.register()` |

### Documentation

- [Usage Guide](docs/en/USAGE.md) · [中文](docs/zh/USAGE.md)
- [Architecture](docs/en/ARCHITECTURE.md) · [中文](docs/zh/ARCHITECTURE.md)
- [Extending](docs/en/EXTENDING.md) · [中文](docs/zh/EXTENDING.md)

### License

[MIT](LICENSE)
