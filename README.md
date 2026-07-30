# 🚀 qcmd: Zero-Dependency, Record-First CLI Argument Parser for Java 17+

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-17%2B-orange.svg)]()
[![Build](https://img.shields.io/badge/build-passing-brightgreen.svg)]()

**qcmd** (Quick Command) 是一个为现代 Java (17+) 打造的极简、轻量、**零依赖（Zero-Dependency）**、**原生支持 Java Record** 的注解驱动命令行参数解析框架。

能用一行代码，将复杂 Linux 命令行参数、标志位、正则表达式规则以及位置变量，直接自动装配到 POJO 或不可变 Java Record 实体类中！

---

## 🔥 核心特性 (Features)

* 🛡️ **零外部依赖 (Zero Dependency)**：不引入 Guava、Commons 等任何第三方库，打包极小，零依赖冲突风险。
* 💎 **原生支持 Java Record (Native Record Support)**：通过 JDK 16+ `RecordComponent` 与 Canonical Constructor，支持不可变 Record 实体类的直接绑定。
* ⚡ **符合 POSIX/GNU 解析规范**：完美解决带 `-` 开头的负数参数（如 `-a -123.45`）与无值 Boolean Flag 混淆的经典解析 Bug。
* 🎨 **自动类型转换与扩展**：内置支持基本类型、Date/Time、Enum、Collection、Map 以及自定义 `QStringConverter`，带有 `ConcurrentHashMap` 性能缓存。
* 🔍 **规则校验与帮助文档**：支持必填校验（`required`）、正则表达式校验（`valueValidRegex`），自动生成格式美观的命令行帮助手册 (`getDesc()`)。

---

## 📦 快速引入 (Installation)

在 `pom.xml` 中引入依赖：

```xml
<dependency>
    <groupId>com.guanyanqi</groupId>
    <artifactId>qcmd</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## ⚡ 30 秒快速上手 (Quick Start)

### 方式一：使用现代 Java Record（推荐 ✨）

```java
import com.guanyanqi.QCmd;
import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.annotation.Vars;
import java.util.List;

@Cmd(names = {"deploy"}, desc = "应用部署指令")
public record DeployCmd(
    @Parameter(names = {"-e", "--env"}, required = true, valueValidRegex = "^(dev|test|prod)$", desc = "目标环境")
    String env,

    @Parameter(names = {"-t", "--timeout"}, desc = "超时时间")
    int timeout,

    @Parameter(names = {"-d", "--dry-run"}, desc = "模拟试运行")
    boolean dryRun,

    @Vars(desc = "部署包路径列表")
    List<String> files
) {}

public class Main {
    public static void main(String[] args) {
        String[] inputArgs = new String[]{"deploy", "-e", "prod", "-t", "-30", "-d", "app.jar", "config.xml"};

        // 一行代码完成解析与不可变 Record 绑定！
        DeployCmd cmd = QCmd.of(inputArgs).parse(DeployCmd.class);

        System.out.println("环境: " + cmd.env());         // prod
        System.out.println("超时: " + cmd.timeout());     // -30
        System.out.println("DryRun: " + cmd.dryRun());    // true
        System.out.println("产物: " + cmd.files());       // [app.jar, config.xml]
    }
}
```

---

### 方式二：使用标准 POJO 类（兼容模式）

```java
import com.guanyanqi.QCmd;
import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;

@Cmd(names = {"trans"}, desc = "转账指令")
public class TransactionCmd {
    @Parameter(names = {"-a", "--amount"}, required = true, desc = "金额")
    private double amount;

    @Parameter(names = {"-t", "--tags"}, desc = "标签列表(逗号分隔)")
    private Set<String> tags;

    @Parameter(names = {"-m", "--meta"}, desc = "扩展元数据(k1=v1,k2=v2)")
    private Map<String, String> meta;
}
```

---

## 🛠️ 高级功能 (Advanced Features)

### 1. 自定义类型转换器 (`QStringConverter`)

如果命令行参数需要转换为复杂的自定义对象（如 `host:port` 转 `ServerAddress`）：

```java
import com.guanyanqi.converter.QStringConverter;

public record ServerAddress(String host, int port) {}

public class ServerAddressConverter implements QStringConverter<ServerAddress> {
    @Override
    public ServerAddress convert(String value) {
        String[] parts = value.split(":");
        return new ServerAddress(parts[0], Integer.parseInt(parts[1]));
    }
}

// 在 @Parameter 中指定 converter
@Cmd(names = "connect")
public record ConnectCmd(
    @Parameter(names = "-s", converter = ServerAddressConverter.class)
    ServerAddress server
) {}
```

### 2. 自动生成帮助说明 (Help Manual)

```java
QCmd qcmd = QCmd.of(args);
DeployCmd cmd = qcmd.parse(DeployCmd.class);

// 获取格式化输出的使用手册
System.out.println(qcmd.getDesc());
```

控制台输出：
```text
使用方法：命令 [参数 参数值] [变量...]
命令：deploy
功能描述：应用部署指令
参数说明：
	参数：-e|--env（必填），参数说明：目标环境，输入规则：只能是 dev, test 或 prod
	参数：-t|--timeout（可选），参数说明：超时时间
	参数：-d|--dry-run（可选），参数说明：模拟试运行
变量描述：部署包路径列表
```

---

## 🏛️ 架构设计 (Architecture)

`qcmd` 遵循单一职责与门面模式（Facade Pattern）设计：

* **`QCmd`**：门面统一入口。
* **`CommandDescriptor`**：元数据提取器（支持 RecordComponent & Field）。
* **`CommandLineParser`**：符合 POSIX/GNU 规范的分词状态机。
* **`CommandValidator`**：规则与细粒度异常校验器。
* **`InstanceBinder`**：属性转换与 POJO / Record 实例化绑定器。
* **`HelpFormatter`**：帮助文档渲染生成器。

---

## 📄 开源协议 (License)

本项目基于 [MIT License](LICENSE) 协议开源。
