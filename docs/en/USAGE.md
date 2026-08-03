# qcmd Usage Guide

## Table of Contents

- [Quick Start](#quick-start)
- [Annotations](#annotations)
- [POJO Mode](#pojo-mode)
- [Type Conversion](#type-conversion)
- [Validation](#validation)
- [Help Text](#help-text)
- [Advanced Usage](#advanced-usage)

---

## Quick Start

### Define a Command

```java
@Cmd(names = {"deploy", "dep"}, desc = "Application deployment command", version = "1.0.0")
public record DeployCmd(
    @Parameter(names = {"-e", "--env"}, required = true,
               valueValidRegex = "^(dev|test|prod)$",
               valueValidDesc = "Must be dev, test, or prod",
               desc = "Target environment")
    String env,

    @Parameter(names = {"-t", "--timeout"}, desc = "Timeout in seconds")
    int timeout,

    @Parameter(names = {"-d", "--dry-run"}, desc = "Dry run mode")
    boolean dryRun,

    @Vars(desc = "Artifact paths")
    List<String> artifacts
) {}
```

### Parse

```java
String[] args = {"deploy", "-e", "prod", "-t", "30", "-d", "app.jar", "config.yaml"};

ParsedCommand<DeployCmd> parsed = QCmd.of(args).parse(DeployCmd.class);
DeployCmd cmd = parsed.value();

System.out.println(cmd.env());      // prod
System.out.println(cmd.timeout());  // 30
System.out.println(cmd.dryRun());   // true
System.out.println(cmd.artifacts());// [app.jar, config.yaml]
```

`parse()` returns a `ParsedCommand<T>`. For normal parsing, `action()` is `EXECUTE` and `value()` contains the command instance. For help or version requests, `shouldExit()` is `true` and `outputText()` is ready to print.

---

## Annotations

### @Cmd — Command Declaration

| Attribute | Type | Description |
|---|---|---|
| `names` | `String[]` | **Required**. Command name aliases, e.g. `{"deploy", "dep"}` |
| `desc` | `String` | Description for help text |
| `version` | `String` | Optional version; enables `-V` / `--version` when non-empty |

### @Parameter — Option Declaration

| Attribute | Type | Description |
|---|---|---|
| `names` | `String[]` | Option names, e.g. `{"-e", "--env"}` |
| `required` | `boolean` | Required flag, default `false` |
| `desc` | `String` | Parameter description |
| `converter` | `Class<? extends QStringConverter>` | Custom type converter |
| `valueValidRegex` | `String` | Regex validation |
| `valueValidDesc` | `String` | Validation failure hint |

`@Parameter` and `@Vars` are mutually exclusive per field/component. Declaring both fails during descriptor construction.

### @Vars — Positional Variable

| Attribute | Type | Description |
|---|---|---|
| `desc` | `String` | Variable description |
| `elementConverter` | `Class<? extends QStringConverter>` | Custom element converter |

---

## POJO Mode

Classic POJOs with full inheritance support:

```java
@Cmd(names = "trans")
public class TransactionCmd {
    @Parameter(names = {"-a", "--amount"}, required = true)
    private double amount;

    @Parameter(names = {"-t", "--type"})
    private OperationType type;  // automatic enum matching

    @Vars
    private List<String> files;
}
```

---

## Type Conversion

qcmd supports 20+ Java types by default, resolved through a 6-stage priority chain:

| Priority | Method | Description |
|---|---|---|
| 1 | Annotation `converter` | `@Parameter(converter = MyConverter.class)` |
| 2 | Global registry | `ConverterRegistry.register(MyType.class, converter)` |
| 3 | Enum matching | `Enum.valueOf(type, rawValue)` |
| 4 | Collection split | `,` delimiter, recursive element conversion |
| 5 | Map parsing | `key=value` format, recursive conversion |
| 6 | String constructor fallback | `new MyType(rawValue)` |

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

Annotation-declared custom converters are instantiated once per parse request, so they do not need to be globally thread-safe. `ConverterRegistry` is process-global and should normally be configured during application startup.

### Global Registration

```java
ConverterRegistry.register(MyCustomType.class, value -> new MyCustomType(value));
```

---

## Validation

| Scenario | Exception |
|---|---|
| Missing required param | `MissingParameterException` |
| Regex mismatch | `InvalidParameterValueException` |
| Unknown option | `UnknownOptionException` |

---

## Help Text

### Built-in help and version actions

```java
ParsedCommand<DeployCmd> parsed = QCmd.of(args).parse(DeployCmd.class);
if (parsed.shouldExit()) {
    System.out.println(parsed.outputText());
    return;
}
DeployCmd command = parsed.value();
```

`-h` / `--help` bypass required-option validation and instance binding. When `@Cmd.version` is non-empty, `-V` / `--version` behaves the same way. A literal `--help` after the `--` terminator remains positional. User-declared options with those names take precedence.

Help can also be generated without parsing arguments:

```java
String help = QCmd.help(DeployCmd.class);
String markdown = QCmd.help(DeployCmd.class, new MarkdownHelpFormatter());
```

### Formatting help

`ParsedCommand.helpText()` generates formatted help using the configured `HelpFormatter`.

```java
ParsedCommand<DeployCmd> parsed = QCmd.of(args).parse(DeployCmd.class);
System.out.println(parsed.helpText());
```

Output (terminal format, default):

```
使用方法：命令 [参数 参数值] [变量...]
命令：deploy
功能描述：Application deployment command
参数说明：
	参数：-e|--env（必填），参数说明：Target environment，输入规则：Must be dev, test, or prod
	参数：-t|--timeout（可选），参数说明：Timeout in seconds
	参数：-d|--dry-run（可选），参数说明：Dry run mode
内置选项：
	-h|--help：显示帮助信息
	-V|--version：显示版本信息
变量描述：Artifact paths
```

### Switch Formatter

```java
// Markdown table format
QCmd.of(args)
    .withHelpFormatter(new MarkdownHelpFormatter())
    .parse(DeployCmd.class);

// Custom lambda
QCmd.of(args)
    .withHelpFormatter(d -> "USAGE: " + d.getCommandNames())
    .parse(MyCmd.class);
```

Markdown output:

```
### `deploy`

> Application deployment command

| 选项 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `-e, --env` | String | *是* | Target environment (Must be dev, test, or prod) |
| `-t, --timeout` | int | 否 | Timeout in seconds |
| `-d, --dry-run` | boolean | 否 | Dry run mode |
| `-h, --help` | flag | 否 | 显示帮助信息 |
| `-V, --version` | flag | 否 | 显示版本信息 |
```

---

## Advanced Usage

### Custom Token Handler Chain

```java
QCmd.of(args)
    .withTokenHandlers(chain -> chain
        .prepend(new WindowsStyleHandler())   // prepend
        .append(new EnvVarHandler())          // append
        .remove(NegativeNumberHandler.class)  // remove
    )
    .parse(MyCmd.class);
```

### Built-in Handlers (execution order)

| Handler | Purpose |
|---|---|
| `TerminatorHandler` | `--` terminator |
| `EqualsSignOptionHandler` | `--key=value` syntax |
| `BooleanFlagHandler` | Boolean flags |
| `NegativeNumberHandler` | Negative number detection |
| `StandardOptionHandler` | Standard options |
| `PositionalHandler` | Positional variable catch-all |

### Builder Operations

```java
TokenHandlerChain.Builder builder = TokenHandlerChain.builder()
    .defaults()                                     // start from defaults
    .prepend(new MyEarlyHandler())                  // push to front
    .before(StandardOptionHandler.class, handler)   // insert before
    .after(BooleanFlagHandler.class, handler)       // insert after
    .replace(NegativeNumberHandler.class, handler)  // replace
    .remove(TerminatorHandler.class)                // remove
    .append(new MyCustomHandler());                 // push to back
```

### Supported POSIX/GNU-style syntax

| Feature | Example | Notes |
|---|---|---|
| Standard option | `deploy -e prod` | Space-delimited |
| Equals syntax | `deploy --env=prod` | GNU style |
| Boolean flag | `deploy -d` | No value consumed |
| Terminator | `deploy -- -v` | Everything after `--` is positional |
| Negative number | `deploy -t -30` | Not confused with option `-3` |
| Short option equals | `deploy -e=prod` | Short form also supports = |

qcmd does not claim complete POSIX/GNU compatibility and does not expand combined short options such as `-abc`. If a value-taking option is followed by another dash-prefixed token, qcmd reports the first option as missing its value; negative decimal numbers are the exception. Use equals syntax such as `--name=-literal` for ordinary dash-prefixed string values.

---

## Related Docs

- [Architecture](ARCHITECTURE.md)
- [Extending](EXTENDING.md)
