# qcmd Extension Guide

For developers extending qcmd. All open extension points with best practices.

## Table of Contents

- [Custom Token Handlers](#custom-token-handlers)
- [Custom Help Formatter](#custom-help-formatter)
- [Custom Type Converter](#custom-type-converter)
- [Global Converter Registration](#global-converter-registration)
- [Extension Point Overview](#extension-point-overview)

---

## Custom Token Handlers

### Interface

```java
@FunctionalInterface
public interface TokenHandler {
    TokenResult handle(TokenContext context, ParseState state);
    // null → pass to next handler
    // TokenResult → handled, short-circuit the chain
}
```

### TokenResult Factory Methods

```java
TokenResult.option("-p", "8080", nextIndex);      // named option
TokenResult.boolFlag("-v", nextIndex);             // boolean flag
TokenResult.boolFlag("-v", "false", nextIndex);    // boolean flag with an explicit value
TokenResult.positional("raw-value", nextIndex);    // positional var
TokenResult.skip(nextIndex);                       // skip (e.g. "--" itself)
```

### Example: Environment Variable Expansion

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

### Example: Windows-Style `/opt` to `--opt`

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

### Registration

```java
QCmd.of(args)
    .withTokenHandlers(chain -> chain
        .prepend(new WindowsStyleHandler())              // highest priority
        .before(StandardOptionHandler.class, handler)   // before standard option
        .after(BooleanFlagHandler.class, handler)        // after boolean flag
        .replace(PositionalHandler.class, handler)       // replace
        .remove(NegativeNumberHandler.class)             // remove
        .append(new EnvVarHandler())                     // low-priority catch-all
    )
    .parse(MyCmd.class);
```

### Handler Execution Order

```
TerminatorHandler
  → BuiltInActionHandler
  → EqualsSignOptionHandler
  → BooleanFlagHandler
  → NegativeNumberHandler
  → StandardOptionHandler
  → PositionalHandler
```

First non-null result wins. A `BuiltInActionHandler` match ends token scanning, so the first help/version action wins. `PositionalHandler` must be last.

### Best Practices

1. **Always check `state.isTerminatorSeen()`** — tokens after `--` shouldn't be treated as options
2. **Be careful with `nextIndex`** — usually `currentIndex + 1`, or `+ 2` when consuming a value token
3. **Use named classes for position-based insertion** — lambdas can't be looked up by type
4. **Use `withTokenHandlers`** — this is the only intended parsing extension entry point

---

## Custom Help Formatter

### Interface

```java
@FunctionalInterface
public interface HelpFormatter {
    String format(CommandDescriptor descriptor);
}
```

### Example: Slack/Discord Chat Format

```java
public class ChatHelpFormatter implements HelpFormatter {
    @Override
    public String format(CommandDescriptor descriptor) {
        StringBuilder sb = new StringBuilder();
        String cmd = descriptor.getCommandNames().iterator().next();
        sb.append("*Command*: `").append(cmd).append("`\n");

        Cmd cmdAnno = descriptor.getCmdAnnotation();
        if (isNotBlank(cmdAnno.desc())) {
            sb.append("*Description*: ").append(cmdAnno.desc()).append("\n");
        }

        for (OptionDescriptor opt : descriptor.getOptions()) {
            sb.append("• `").append(String.join(", ", opt.names()))
              .append("` — ").append(opt.desc())
              .append(opt.required() ? " *(required)*" : "").append("\n");
        }

        VarsDescriptor vars = descriptor.getVarsDescriptor();
        if (vars != null && isNotBlank(vars.desc())) {
            sb.append("• _Positional_: ").append(vars.desc()).append("\n");
        }
        return sb.toString();
    }
}
```

### Registration

```java
QCmd.of(args)
    .withHelpFormatter(new ChatHelpFormatter())
    .parse(MyCmd.class);

// Or lambda
QCmd.of(args)
    .withHelpFormatter(d -> "USAGE: " + d.getCommandNames())
    .parse(MyCmd.class);
```

### Built-in Implementations

| Class | Style |
|---|---|
| `TerminalHelpFormatter` (default) | Plain text |
| `MarkdownHelpFormatter` | Markdown table + headings |

---

## Custom Type Converter

### Interface

```java
@FunctionalInterface
public interface QStringConverter<T> {
    T convert(String value);
}
```

### Example: Parsing `host:port`

```java
public record ServerAddress(String host, int port) {}

public class ServerAddressConverter implements QStringConverter<ServerAddress> {
    @Override
    public ServerAddress convert(String value) {
        String[] parts = value.split(":");
        return new ServerAddress(parts[0], Integer.parseInt(parts[1]));
    }
}

// Annotation-level
@Parameter(names = "-s", converter = ServerAddressConverter.class)
ServerAddress server;

// @Vars element converter
@Vars(elementConverter = ServerAddressConverter.class)
List<ServerAddress> servers;
```

---

## Global Converter Registration

```java
ConverterRegistry.register(MyCustomType.class, value -> new MyCustomType(value));
```

### Priority Chain

1. Annotation `converter` class (highest)
2. Global `ConverterRegistry`
3. Enum auto-match
4. Collection / Map splitting
5. String constructor fallback (lowest)

---

## Extension Point Overview

| Layer | Interface | Registration | Scope |
|---|---|---|---|
| Token parsing | `TokenHandler` | `QCmd.withTokenHandlers()` | Per-parse |
| Help format | `HelpFormatter` | `QCmd.withHelpFormatter()` | Per-parse |
| Type conversion | `QStringConverter<T>` | `@Parameter(converter=...)` | Per-field |
| Global conversion | `QStringConverter<T>` | `ConverterRegistry.register()` | Global |
| Vars elements | `QStringConverter<T>` | `@Vars(elementConverter=...)` | Per-command |
