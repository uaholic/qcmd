# qcmd Architecture

## Overview

qcmd is a zero-dependency, annotation-driven CLI argument parser for Java 17+. The design prioritizes **minimum concepts + maximum extensibility**: a single facade entry point (`QCmd.of(args)`) backed by Strategy and Chain of Responsibility patterns at every extension point.

---

## Pipeline Overview

```
QCmd.of(args)
  │
  ├─ 1. CommandDescriptor(Class)          ← reflection-based metadata extraction
  ├─ 2. formatter.format(descriptor)      ← help text (swappable strategy)
   ├─ 3. help/version action shortcut      ← display and exit normally
   ├─ 4. TokenHandlerChain.execute(...)    ← token parsing (extensible chain)
   ├─ 5. CommandValidator.validate(...)    ← rule validation
   └─ 6. InstanceBinder.bind(...)          ← reflection-based instance construction
       │
        └─ ParsedCommand<T>(value, helpText, action, outputText)
```

`CommandDescriptor` and `ParseResult` cross pipeline stages as read-only snapshots. `ConverterRegistry` is an explicit process-global extension point; other parsing state remains scoped to the current `QCmd` session.

---

## 1. Metadata Extraction — CommandDescriptor

### Responsibility

Extract structured command metadata from user-defined `@Cmd` annotated classes for downstream read-only consumption.

### Design

```
CommandDescriptor
  ├── commandNames: Set<String>           ← @Cmd.names
  ├── options: List<OptionDescriptor>      ← @Parameter → OptionDescriptor
  ├── nameToOptionMap: Map<String, OptionDescriptor>
  ├── boolOptionNames: Set<String>         ← Boolean-typed options
  ├── requiredOptionGroups: List<List<String>>
  ├── varsDescriptor: VarsDescriptor       ← @Vars metadata
  └── convertValue / convertVars           ← 6-stage type conversion pipeline
```

Strategy implementations extract metadata from POJO `Field`s or Record `RecordComponent`s. `@Parameter` and `@Vars` are mutually exclusive; declaring both fails immediately. After extraction the descriptor is frozen, and collection accessors expose read-only views or copies.

```java
for (Field field : fields) {
    Parameter param = field.getAnnotation(Parameter.class);
    Vars vars = field.getAnnotation(Vars.class);
    if (param != null && vars != null) {
        throw new QCmdException("@Parameter and @Vars are mutually exclusive");
    }
    if (param != null) {
        descriptor.registerOption(...);
    } else if (vars != null) {
        descriptor.registerVars(...);
    }
}
```

`OptionDescriptor` and `VarsDescriptor` abstract over `Field` vs `RecordComponent` via `AnnotatedElement`, keeping all downstream code type-agnostic.

### Type Conversion Pipeline (6-stage fallback)

1. Annotation-declared `converter` class
2. Global `ConverterRegistry` lookup
3. `Enum.valueOf` auto-match
4. Collection (split → recursive element conversion)
5. Map (parse k=v → recursive key/value conversion)
6. Single-String-constructor fallback

Annotation-declared converters are instantiated per parse request, so user converters are not forced to be globally thread-safe. Instances explicitly registered in the process-global `ConverterRegistry` remain the caller's lifecycle responsibility.

---

## 2. Token Parsing — TokenHandler Chain

### Responsibility

Decompose `String[] args` into `ParseResult(commandName, optionValues, positionalVars)`.

### Design

Chain of Responsibility — each token is passed through all handlers in order; first non-null result wins:

```
@FunctionalInterface
interface TokenHandler {
    TokenResult handle(TokenContext context, ParseState state);
    // null → skip, try next handler
    // TokenResult → handled, apply and short-circuit
}
```

### Default Chain (6 handlers)

| # | Handler | Match | Action |
|---|---|---|---|
| 1 | `TerminatorHandler` | `"--"` | Set termination flag, skip |
| 2 | `EqualsSignOptionHandler` | `-x` contains `=` | Split into key=value option |
| 3 | `BooleanFlagHandler` | Known bool option | Store `"true"` |
| 4 | `NegativeNumberHandler` | `-\d` not a known option | Treat as positional |
| 5 | `StandardOptionHandler` | Other `-` prefix | Safely consume known-option values; retain unknown options for validation |
| 6 | `PositionalHandler` | Non-`-` prefix or after `--` | Treat as positional |

### Extensibility

`TokenHandlerChain.Builder` supports:

| Operation | Description |
|---|---|
| `defaults()` | Start from default chain |
| `prepend(h)` | Insert at front |
| `append(h)` | Append to end |
| `before(Class, h)` | Insert before first matching type |
| `after(Class, h)` | Insert after first matching type |
| `replace(Class, h)` | Replace first matching type |
| `remove(Class)` | Remove first matching type |

The Builder is mutable while configuring the chain. `build()` defensively copies the handler list and returns an immutable `TokenHandlerChain`.

User entry point: `QCmd.withTokenHandlers(chain -> chain.prepend(new MyHandler()))`

---

## 3. Validation — CommandValidator

### Responsibility

Enforce parsing rules, four checks in fixed order:

1. Unknown option detection
2. Regex validation
3. Required-parameter check
4. Positional-variable presence check

Each failure throws a typed exception subclass carrying structured context:

| Scenario | Exception | Fields |
|---|---|---|
| Unknown option | `UnknownOptionException` | cmd name, option name |
| Regex mismatch | `InvalidParameterValueException` | cmd name, option name, value, rule |
| Missing required | `MissingParameterException` | cmd name, missing params |

Validator is fully type-agnostic — operates only on `ParseResult` + `CommandDescriptor`.

---

## 4. Instance Binding — InstanceBinder

### Responsibility

Convert `ParseResult` (string maps) into a strongly-typed command instance.

### Design

Strategy pattern:

```
CommandBindingStrategyFactory.getStrategy(Class)
  ├── targetClass.isRecord() → RecordBindingStrategy
  └── else → PojoBindingStrategy
```

**RecordBindingStrategy**: Uses `Class.getRecordComponents()` + Canonical Constructor. Assembles an `Object[]` in component order. Primitive defaults (0/false/`\0`) prevent `IllegalArgumentException`.

**PojoBindingStrategy**: Recursive `Field` traversal via `getDeclaredFields()` + `getSuperclass()`. Defaults to JVM defaults (null/0/false).

Both strategies share the same metadata extraction logic; only `bindInstance()` differs.

---

## 5. Help Text — HelpFormatter

### Responsibility

Generate formatted help output, adapt to different display environments.

### Design

`HelpFormatter` is a `@FunctionalInterface` strategy:

```java
@FunctionalInterface
public interface HelpFormatter {
    String format(CommandDescriptor descriptor);
}
```

Built-in implementations:

| Class | Target | Style |
|---|---|---|
| `TerminalHelpFormatter` (default) | CLI terminal | Plain text |
| `MarkdownHelpFormatter` | GitHub/docs sites | Markdown table + headings |

User entry:

```java
QCmd.of(args).withHelpFormatter(new MarkdownHelpFormatter()).parse(MyCmd.class);
QCmd.of(args).withHelpFormatter(d -> "USAGE: " + d.getCommandNames()).parse(MyCmd.class);
```

---

## 6. Public API

### QCmd

One-shot configurable parsing session:

```java
QCmd.of(String[] args)
ParsedCommand<T> parse(Class<T> clazz)
String QCmd.help(Class<?> clazz)
QCmd withTokenHandlers(UnaryOperator<Builder>)
QCmd withHelpFormatter(HelpFormatter)
```

### ParsedCommand

```java
record ParsedCommand<T>(T value, String helpText,
                        ParseAction action, String outputText) {}
```

---

## Design Principles

| Principle | Implementation |
|---|---|
| Zero dependencies | No external runtime dependencies |
| Domain model abstraction | `AnnotatedElement` shields Field vs RecordComponent |
| Strategy pattern | `CommandBindingStrategy` + Factory |
| Chain of Responsibility | `TokenHandler` chain |
| Adapter pattern | `HelpFormatter` with multiple output formats |
| Immutability | Result records contain read-only snapshots; metadata freezes after construction |
| Open extension | `ConverterRegistry`, `TokenHandlerChain.Builder`, `withTokenHandlers()`, `withHelpFormatter()` |
| Static factory | `QCmd.of()` single entry |
