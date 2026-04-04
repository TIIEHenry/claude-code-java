# Claude Code Java Port

Java 17+ 移植版本的 Claude Code CLI。

## 项目状态

| 指标 | 数量 |
|------|------|
| 源文件 | 846 |
| 测试文件 | 386 |
| JAR 大小 | 15.6 MB |
| 工具数量 | 51 |
| 占位符注释 | 33 (持续减少中) |

## 已实现功能

- ✅ 51个工具完整实现
- ✅ HTTP API 客户端 (Java 11+ HttpClient)
- ✅ 真实网络搜索 (DuckDuckGo API)
- ✅ LSP 代码智能支持 (内置 Java/TS/Python/Go/Rust)
- ✅ Git 仓库检测和操作 (git diff, git log, status)
- ✅ 设置管理 (JSON 解析/保存)
- ✅ 后台进程管理
- ✅ 计算机自动化 (Java AWT Robot)
- ✅ 自动更新检查 (GitHub Releases API)
- ✅ 分析遥测 (Datadog, FirstParty)
- ✅ 环境变量管理
- ✅ 语音录制 (Java Sound API)
- ✅ PDF 文本提取
- ✅ Sentry 错误报告
- ✅ MCP OAuth 认证
- ✅ 会话持久化
- ✅ API 使用量查询
- ✅ Policy 限制服务
- ✅ 远程设置同步

## 项目结构

```
claude-code-java/
├── claude-code-core/     # 核心库 (777 个源文件)
├── claude-code-tools/    # 工具模块 (50 个源文件)
└── claude-code-cli/      # 命令行界面 (18 个源文件)
```

## 技术栈

- Java 17+
- Maven 多模块项目
- JUnit 5 测试框架
- Project Reactor (响应式编程)
- Jackson (JSON处理)
- Picocli (命令行解析)

## 构建

```bash
# 编译
mvn compile

# 运行测试
mvn test

# 打包 (跳过测试)
mvn package -DskipTests

# 运行
java -jar claude-code-cli/target/claude-code-cli-1.0.0-SNAPSHOT.jar --help
```

## 命令行选项

```
Usage: claude-code [-hV] [--no-tools] [--read-only] [--verbose]
                   [--api-key=<apiKey>] [--cwd=<cwd>] [-m=<model>]
                   [--max-turns=<maxTurns>]
                   [--permission-mode=<permissionMode>] [PROMPT]

      [PROMPT]             Initial prompt to send
      --api-key=<apiKey>   Anthropic API key
      --cwd=<cwd>          Working directory
  -m, --model=<model>      Model to use (sonnet, opus, haiku)
      --max-turns=<maxTurns>
                           Maximum turns per conversation
      --permission-mode=<permissionMode>
                           Permission mode (default, accept-edits, bypass)
```

## 模块说明

### claude-code-core

核心功能模块，包含:
- 常量定义 (`constants/`)
- 类型定义 (`types/`)
- 服务层 (`services/`)
  - API客户端 (`api/`)
  - MCP协议 (`mcp/`)
  - 分析统计 (`analytics/`)
  - 远程设置 (`remotemanagedsettings/`)
- 工具类 (`utils/`)
  - 计算机自动化 (`computerUse/`)
  - Bash解析 (`bash/`)
  - 权限管理 (`permissions/`)
- 状态管理 (`state/`)
- Hook系统 (`hooks/`)
- Bridge通信 (`bridge/`)

### claude-code-tools

50个工具实现，包括:
- 文件操作: FileReadTool, FileWriteTool, FileEditTool, GlobTool, GrepTool
- Shell执行: BashTool, PowerShellTool
- 网络工具: WebFetchTool, WebSearchTool
- 任务管理: TaskTool, TaskCreateTool, TaskOutputTool
- MCP工具: MCPTool, McpAuthTool
- 其他: AskUserQuestionTool, AgentTool, SkillTool

### claude-code-cli

命令行界面入口:
- Main.java - 主入口
- InteractiveSession.java - 交互式会话
- CommandRegistry.java - 命令注册
- InputHandler.java - 输入处理
- OutputFormatter.java - 输出格式化

## 许可证

Copyright 2024-2026 Anthropic. All Rights Reserved.