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
  ├─ 3. TokenHandlerChain.execute(...)    ← token parsing (extensible chain)
  ├─ 4. CommandValidator.validate(...)    ← rule validation
  └─ 5. InstanceBinder.bind(...)          ← reflection-based instance construction
       │
       └─ ParsedCommand<T>(value, helpText)
```

Shared state between steps is limited to `CommandDescriptor` (immutable metadata) and `ParseResult` (immutable parse result). Zero side effects, zero global state.

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

Strategy pattern extracts metadata from POJO `Field`s or Record `RecordComponent`s. `@Parameter` and `@Vars` are mutually exclusive per element, handled via if-else:

```java
for (Field field : fields) {
    Parameter param = field.getAnnotation(Parameter.class);
    if (param != null) {
        descriptor.registerOption(...);   // @Parameter branch
    } else {
        Vars vAnno = field.getAnnotation(Vars.class);
        if (vAnno != null) {
            descriptor.registerVars(...); // @Vars branch
        }
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

Converter instances cached via `ConcurrentHashMap`.

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
| 5 | `StandardOptionHandler` | Other `-` prefix | Consume next token as value |
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

The chain is immutable — each Builder operation returns a new `TokenHandlerChain`.

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

Stateless facade:

```java
QCmd.of(String[] args)
ParsedCommand<T> parse(Class<T> clazz)
QCmd withTokenHandlers(UnaryOperator<Builder>)
QCmd withHelpFormatter(HelpFormatter)
```

### ParsedCommand

```java
record ParsedCommand<T>(T value, String helpText) {}
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
| Immutability | All data objects are records |
| Open extension | `ConverterRegistry`, `TokenHandlerChain.Builder`, `withTokenHandlers()`, `withHelpFormatter()` |
| Static factory | `QCmd.of()` single entry |
