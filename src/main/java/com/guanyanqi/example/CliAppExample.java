package com.guanyanqi.example;

import com.guanyanqi.QCmd;
import com.guanyanqi.ParsedCommand;
import com.guanyanqi.annotation.Cmd;
import com.guanyanqi.annotation.Parameter;
import com.guanyanqi.annotation.Vars;
import com.guanyanqi.converter.QStringConverter;

import java.util.List;

/**
 * 生产级 CLI 示范应用示例。
 * 演示如何一行代码解析包含 POJO / Record / 正则校验 / 自定义 Converter / 负数 / 部署文件列表的真实场景命令。
 *
 * @author guanyanqi
 */
public class CliAppExample {

    /**
     * 示例创建 CLI 示范类构造器。
     */
    public CliAppExample() {
    }

    /**
     * 服务器配置 Record 数据载体。
     *
     * @param host 主机名
     * @param port 端口号
     */
    public record ServerConfig(String host, int port) {}

    /**
     * 自定义服务器配置转换器。
     */
    public static class ServerConfigConverter implements QStringConverter<ServerConfig> {

        /**
         * 创建转换器实例。
         */
        public ServerConfigConverter() {
        }

        @Override
        public ServerConfig convert(String value) {
            String[] parts = value.split(":");
            return new ServerConfig(parts[0], Integer.parseInt(parts[1]));
        }
    }

    /**
     * 示例 Deploy 命令行配置描述 Record。
     *
     * @param server    目标服务器配置
     * @param env       目标部署环境
     * @param timeout   超时时间
     * @param dryRun    是否演练模式
     * @param artifacts 产物路径列表
     */
    @Cmd(names = {"deploy"}, desc = "部署云端应用服务的核心指令")
    public record DeployCommand(
            @Parameter(names = {"-s", "--server"}, required = true, converter = ServerConfigConverter.class, desc = "目标服务器信息 (host:port)")
            ServerConfig server,

            @Parameter(names = {"-e", "--env"}, valueValidRegex = "^(dev|test|prod)$", valueValidDesc = "只能是 dev, test 或 prod", desc = "目标环境")
            String env,

            @Parameter(names = {"-t", "--timeout"}, desc = "超时时间(秒)")
            int timeout,

            @Parameter(names = {"-d", "--dry-run"}, desc = "是否开启模拟运行演练")
            boolean dryRun,

            @Vars(desc = "需部署的产物包路径列表")
            List<String> artifacts
    ) {}

    /**
     * 主函数运行示例入口。
     *
     * @param args 命令行入参
     */
    public static void main(String[] args) {
        String[] mockCliArgs = new String[]{
                "deploy",
                "-s", "192.168.1.100:8080",
                "-e", "prod",
                "-t", "-30", // 负数参数测试
                "-d",
                "app-v1.0.jar", "config.yaml"
        };

        System.out.println("==================================================");
        System.out.println("🌟 欢迎使用 qcmd 命令行解析工具");
        System.out.println("==================================================");

        QCmd qcmd = QCmd.of(mockCliArgs);
        ParsedCommand<DeployCommand> parsed = qcmd.parse(DeployCommand.class);
        DeployCommand command = parsed.value();

        System.out.println("✅ 解析成功！装配结果如下：");
        System.out.println("   目标服务器 : " + command.server().host() + " (Port: " + command.server().port() + ")");
        System.out.println("   部署环境   : " + command.env());
        System.out.println("   超时设置   : " + command.timeout() + "s");
        System.out.println("   Dry-Run    : " + command.dryRun());
        System.out.println("   部署产物   : " + command.artifacts());
        System.out.println("\n📖 自动生成的帮助文档：");
        System.out.println(parsed.helpText());
    }
}
