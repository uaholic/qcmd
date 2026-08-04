# qcmd 架构设计

## 概述

qcmd 是一个零依赖、面向 Java 17+ 的注解驱动命令行参数解析库。设计上追求**最少概念 + 最大可扩展性**——对外有且仅有一个入口 `QCmd.of(args)`，对内通过 Strategy Pattern 与 Chain of Responsibility 模式在每个关键环节提供开放扩展点。

全文按解析管线的处理顺序组织，每一章节对应架构中的一个核心模块。

---

## 解析管线概览

```
QCmd.of(args)
  │
  ├─ 1. CommandDescriptor(Class)          ← 反射提取注解元数据
  ├─ 2. formatter.format(descriptor)      ← 生成帮助文本（可替换策略）
  ├─ 3. TokenHandlerChain.execute(...)    ← Token 分流，含内置动作识别
  ├─ 4. ACTION 结果短路                   ← 正常显示 help/version 后退出
  ├─ 5. CommandValidator.validate(...)    ← 参数规则校验
  └─ 6. InstanceBinder.bind(...)          ← 反射构造目标实例
       │
       └─ ParsedCommand<T>(value, helpText, action, outputText)
```

解析管线中的 `CommandDescriptor` 和 `ParseResult` 以只读快照传递。`ConverterRegistry` 是明确的进程级全局扩展点；其他解析状态限定在当前 `QCmd` 会话内。

---

## 一、元数据提取 — CommandDescriptor

### 职责

从用户定义的 `@Cmd` 命令类中提取结构化的命令元数据，供后续所有步骤只读使用。

### 设计

```
CommandDescriptor
  ├── commandNames: Set<String>           ← @Cmd.names
  ├── options: List<OptionDescriptor>      ← 每个 @Parameter → 一个 OptionDescriptor
  ├── nameToOptionMap: Map<String, OptionDescriptor> ← 选项名索引
  ├── boolOptionNames: Set<String>         ← 布尔类型选项专门集合
  ├── requiredOptionGroups: List<List<String>> ← 必填选项名分组
  ├── varsDescriptor: VarsDescriptor       ← @Vars 元数据
  └── convertValue / convertVars           ← 六步类型转换管线
```

### 元数据提取策略

通过策略模式从 POJO 的 `Field` 或 Record 的 `RecordComponent` 中提取注解。每个 Field/RecordComponent 上 `@Parameter` 与 `@Vars` 互斥，同时声明会立即报错。提取完成后 descriptor 冻结，所有集合 getter 返回只读视图或副本。

```java
for (Field field : fields) {
    Parameter param = field.getAnnotation(Parameter.class);
    Vars vars = field.getAnnotation(Vars.class);
    if (param != null && vars != null) {
        throw new QCmdException("@Parameter 与 @Vars 不能同时声明");
    }
    if (param != null) {
        descriptor.registerOption(...);
    } else if (vars != null) {
        descriptor.registerVars(...);
    }
}
```

`OptionDescriptor` 和 `VarsDescriptor` 本身通过 `AnnotatedElement`（Field / RecordComponent 两者的共同超类型）做到类型无关——所有下游代码不关心来源。

### 类型转换管线

`convertValue()` 实现了六优先级回退链：

1. 注解声明的 `converter` Class
2. `ConverterRegistry` 全局注册表
3. `Enum.valueOf` 枚举自动匹配
4. Collection（切分 → 递归转换元素）
5. Map（解析 k=v → 递归转换键值）
6. unique String 参数构造方法兜底

注解声明的转换器按解析请求实例化，不强制用户转换器承担全局线程安全责任。`ConverterRegistry` 中显式注册的实例则由调用方负责生命周期和线程安全。

---

## 二、Token 解析 — TokenHandler 链

### 职责

将 `String[] args` 拆解为 `ParseResult(commandName, optionValues, positionalVars)`。

### 设计

采用 Chain of Responsibility 模式——每个 token 依次尝试所有 handler，第一个返回非 null 结果者胜出：

```
@FunctionalInterface
interface TokenHandler {
    TokenResult handle(TokenContext context, ParseState state);
    // 返回 null → 不处理，下一个 handler 继续
    // 返回 TokenResult → 处理完成，应用结果并跳过后续 handler
}
```

### 默认处理器链

7 个内置 handler 按执行顺序：

| 顺序 | Handler | 匹配条件 | 动作 |
|---|---|---|---|
| 1 | `TerminatorHandler` | `"--"` | 设终止标志，跳过 |
| 2 | `BuiltInActionHandler` | help/version 且未被用户覆盖 | 记录 ACTION 并终止解析 |
| 3 | `EqualsSignOptionHandler` | `-x` 且含 `=` | 拆分为 key=value 选项 |
| 4 | `BooleanFlagHandler` | 已知 bool 选项 | 存 `"true"` |
| 5 | `NegativeNumberHandler` | `-\d` 且非已知选项 | 归为位置变量 |
| 6 | `StandardOptionHandler` | 其他 `-` 前缀 | 已知选项安全消费值；未知选项留给校验器 |
| 7 | `PositionalHandler` | 非 `-` 前缀 或终止后 | 归为位置变量 |

### 可扩展性

`TokenHandlerChain.Builder` 提供：

| 操作 | 说明 |
|---|---|
| `defaults()` | 以默认链为起点 |
| `prepend(handler)` | 前插到链头 |
| `append(handler)` | 追加到链尾 |
| `before(Class, handler)` | 在指定 handler 前插入 |
| `after(Class, handler)` | 在指定 handler 后插入 |
| `replace(Class, handler)` | 替换指定 handler |
| `remove(Class)` | 移除指定 handler |

Builder 在构建阶段是可变的；`build()` 会防御性复制 handler 列表，生成不可变的 `TokenHandlerChain`。

用户通过 `QCmd.withTokenHandlers()` 单入口使用：

```java
QCmd.of(args)
    .withTokenHandlers(chain -> chain.prepend(new MyHandler()))
    .parse(MyCmd.class);
```

---

## 三、参数校验 — CommandValidator

### 职责

对解析结果执行规则检查，四种校验按固定顺序执行：

1. **未知选项检测** — `optionValues` 的 key 是否都在 `nameToOptionMap` 中
2. **正则校验** — 已匹配选项的值是否满足 `valueValidRegex`
3. **必填校验** — `requiredOptionGroups` 中每组是否至少有一个存在
4. **位置变量校验** — 有 positional vars 但未声明 `@Vars` 时报错

每种校验失败抛出不同的异常子类型，携带结构化上下文字段：

| 场景 | 异常 | 字段 |
|---|---|---|
| 未知选项 | `UnknownOptionException` | 命令名、选项名 |
| 正则不匹配 | `InvalidParameterValueException` | 命令名、选项名、输入值、规则描述 |
| 缺少必填 | `MissingParameterException` | 命令名、缺失参数列表 |

校验器完全类型无关——仅操作 `ParseResult` + `CommandDescriptor`。

---

## 四、实例绑定 — InstanceBinder

### 职责

将 `ParseResult`（原始字符串映射）转换为目标命令类的强类型实例。

### 设计

复用策略模式：

```
CommandBindingStrategyFactory.getStrategy(Class)
  ├── targetClass.isRecord() → RecordBindingStrategy
  └── else → PojoBindingStrategy
```

**RecordBindingStrategy**：通过 `Class.getRecordComponents()` 和 `CanonicalConstructor`，将所有组件值排列为 Object[] 传给构造器。基本类型默认值（0 / false / \0）来自组件类型的零值。

**PojoBindingStrategy**：通过 `Class.getDeclaredFields()` + `getSuperclass()` 递归获取所有字段，反射注入。无默认值——未提供的字段保留 Java 默认值（null / 0 / false）。

两套策略共享相同的元数据提取逻辑（`extractMetadata()`），差异只在 `bindInstance()`。

---

## 五、帮助文本 — HelpFormatter

### 职责

根据 `CommandDescriptor` 生成适配不同输出场景的格式化帮助文本。

### 设计

`HelpFormatter` 是一个 `@FunctionalInterface`，允许通过简单替换策略适配终端、网页、聊天框等不同输出场景：

```java
@FunctionalInterface
public interface HelpFormatter {
    String format(CommandDescriptor descriptor);
}
```

内置实现：

| 实现 | 适用场景 | 输出风格 |
|---|---|---|
| `TerminalHelpFormatter`（默认） | 终端 CLI | 纯文本列表 |
| `MarkdownHelpFormatter` | GitHub/文档站点 | Markdown 表格 + 标题 |

用户入口：

```java
// 使用内置实现
QCmd.of(args).withHelpFormatter(new MarkdownHelpFormatter()).parse(MyCmd.class);

// Lambda 自定义
QCmd.of(args).withHelpFormatter(d -> "USAGE: " + d.getCommandNames()).parse(MyCmd.class);
```

---

## 六、公共 API 层

### QCmd

一次性、可配置的解析会话，核心方法：

```java
QCmd.of(String[] args)                                  // 工厂
ParsedCommand<T> parse(Class<T> clazz)                  // 解析
String QCmd.help(Class<?> clazz)                        // 独立生成帮助
QCmd withTokenHandlers(UnaryOperator<Builder>)           // 扩展 Token 链
QCmd withHelpFormatter(HelpFormatter)                    // 扩展帮助格式
```

### ParsedCommand

```java
record ParsedCommand<T>(T value, String helpText,
                        ParseAction action, String outputText) {}
```

不可变结果容器，调用方无需持有 QCmd 实例即可获取帮助文本。

---

## 七、包结构

```
com.guanyanqi
├── QCmd.java                    ← 门面入口
├── ParseAction.java             ← 执行 / 帮助 / 版本动作
├── ParsedCommand.java           ← 不可变结果容器
├── annotation/
│   ├── Cmd.java                 ← @Cmd 注解
│   ├── Parameter.java           ← @Parameter 注解
│   └── Vars.java                ← @Vars 注解
├── constant/
│   └── Constants.java           ← 分隔符常量
├── converter/
│   ├── QStringConverter.java         ← 转换器接口
│   ├── QCollectionStringConverter.java  ← 集合转换器 SPI
│   ├── QMapStringConverter.java      ← 映射转换器 SPI
│   ├── ConverterRegistry.java        ← 全局注册表
│   ├── DefaultCollectionStringConverter.java
│   ├── DefaultMapStringConverter.java
│   └── NoConverter.java              ← 空哨兵
├── core/
│   ├── CommandDescriptor.java    ← 元数据提取 + 类型转换
│   ├── CommandLineParser.java    ← 解析器封装（ParseResult 定义）
│   ├── CommandValidator.java     ← 参数校验
│   ├── HelpFormatter.java        ← 帮助文本策略接口
│   ├── TerminalHelpFormatter.java ← 终端风格（默认）
│   ├── MarkdownHelpFormatter.java ← Markdown 表格风格
│   ├── InstanceBinder.java       ← 实例构造
│   ├── model/
│   │   ├── OptionDescriptor.java ← 选项域模型
│   │   └── VarsDescriptor.java   ← 位置变量域模型
│   ├── parser/
│   │   ├── TokenHandler.java          ← 处理器接口
│   │   ├── TokenContext.java          ← 上下文 record
│   │   ├── TokenResult.java           ← 处理结果 record
│   │   ├── TokenKind.java             ← 结果枚举
│   │   ├── ParseState.java            ← 累积状态
│   │   ├── TokenHandlerChain.java     ← 处理器链 + Builder
│   │   └── impl/
│   │       ├── TerminatorHandler.java
│   │       ├── BuiltInActionHandler.java
│   │       ├── EqualsSignOptionHandler.java
│   │       ├── BooleanFlagHandler.java
│   │       ├── NegativeNumberHandler.java
│   │       ├── StandardOptionHandler.java
│   │       └── PositionalHandler.java
│   └── strategy/
│       ├── CommandBindingStrategy.java
│       ├── CommandBindingStrategyFactory.java
│       ├── PojoBindingStrategy.java
│       └── RecordBindingStrategy.java
├── exception/
│   ├── QCmdException.java
│   ├── MissingParameterException.java
│   ├── InvalidParameterValueException.java
│   └── UnknownOptionException.java
└── utils/
    └── QCmdUtils.java
```

可运行示例位于 `src/test/java/com/guanyanqi/example/`：随测试源码编译，但不进入发布 jar。

---

## 设计原则总结

| 原则 | 体现 |
|---|---|
| 零依赖 | 无任何外部运行时依赖 |
| 领域模型抽象 | `OptionDescriptor` / `VarsDescriptor` 通过 `AnnotatedElement` 屏蔽 Field vs RecordComponent |
| 策略模式 | `CommandBindingStrategy` 接口 + Factory + 两个具体实现 |
| 责任链模式 | `TokenHandler` 接口 + `TokenHandlerChain` + 7 个内置 handler |
| 适配器模式 | `HelpFormatter` 接口 + 多输出格式实现（终端 / Markdown / 自定义） |
| 不可变数据 | 解析结果使用 record + 只读集合，描述元数据构建后冻结 |
| 开放扩展 | `ConverterRegistry`、`TokenHandlerChain.Builder`、`withTokenHandlers()`、`withHelpFormatter()` |
| 静态工厂 | `QCmd.of()` 统一入口 |
