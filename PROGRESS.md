# Claude Code Java 移植进度

## 当前状态

| 项目 | 原版 (TypeScript) | Java 版 | 进度 |
|------|------------------|---------|------|
| 文件数 | 1,884 | 377 | 20.0% |
| 代码行数 | 512,664 | 59,465 | 11.6% |

## 已移植模块

### 核心接口 (claude-code-core)
- [x] Tool.java - 工具接口
- [x] Task.java - 任务状态
- [x] ToolResult.java - 工具结果
- [x] ToolUseContext.java - 工具上下文
- [x] ToolProgress.java - 工具进度
- [x] ValidationResult.java - 验证结果
- [x] CanUseToolFn.java - 权限检查函数
- [x] AssistantMessage.java - 助手消息
- [x] ToolProgressCallback.java - 进度回调
- [x] ToolProgressData.java - 进度数据
- [x] ToolDescribeOptions.java - 工具描述选项
- [x] ToolPromptOptions.java - 工具提示选项

### 权限模块 (permission)
- [x] PermissionMode.java - 权限模式
- [x] PermissionBehavior.java - 权限行为
- [x] PermissionResult.java - 权限结果 (sealed interface)
- [x] PermissionRule.java - 权限规则
- [x] PermissionRuleSource.java - 规则来源
- [x] PermissionRuleValue.java - 规则值

### 引擎模块 (engine)
- [x] QueryEngine.java - 查询引擎
- [x] QueryEngineConfig.java - 引擎配置

### API 服务 (services/api) - 16/16 完成
- [x] ApiClient.java - API 客户端
- [x] ApiClientConfig.java - 客户端配置
- [x] ApiException.java - API 异常
- [x] ApiRequest.java - API 请求
- [x] ApiResponse.java - API 响应
- [x] ApiStreamingResponse.java - 流式响应
- [x] HttpClient.java - HTTP 客户端
- [x] ApiErrorUtils.java - 错误工具 (SSL, 连接错误)
- [x] ApiErrors.java - 错误消息常量和分类
- [x] ApiUsage.java - 使用量/配额类型
- [x] ApiBootstrap.java - Bootstrap 数据获取
- [x] ApiClientFactory.java - 客户端工厂 (Bedrock/Vertex/Foundry)
- [x] FilesApiService.java - 文件上传下载服务
- [x] ApiRetryService.java - 重试服务 (指数退避)
- [x] ApiLoggingService.java - API 日志服务
- [x] FirstTokenDate.java - 首次 Token 日期追踪
- [x] ReferralService.java - 推荐码服务
- [x] OverageCreditGrant.java - 额外配额管理

### LSP 服务 (services/lsp) - 6/6 完成
- [x] LspConfig.java - LSP 配置
- [x] LspClient.java - LSP 客户端接口和实现
- [x] MessageConnection.java - JSON-RPC 消息连接
- [x] LspDiagnosticRegistry.java - 诊断注册表
- [x] LspServerManager.java - 服务器管理器
- [x] LspServerInstance.java - 服务器实例

### 钩子模块 (hooks)
- [x] Hook.java - 钩子接口
- [x] HookContext.java - 钩子上下文
- [x] HookManager.java - 钩子管理器
- [x] CanUseToolFn.java - 工具使用检查

### 上下文模块 (context)
- [x] AppState.java - 应用状态
- [x] NotificationSystem.java - 通知系统
- [x] SessionContext.java - 会话上下文
- [x] StatsContext.java - 统计上下文

### 状态模块 (state)
- [x] Store.java - 状态存储
- [x] AppState.java - 应用状态
- [x] AppStateStore.java - 状态存储单例
- [x] Selectors.java - 状态选择器

### 引导模块 (bootstrap)
- [x] AppState.java - 全局状态
- [x] BootstrapUtils.java - 引导工具

### 组件模块 (components) - 6/6 完成 ✓
- [x] agents/AgentTypes.java - Agent 类型定义
- [x] agents/AgentUtils.java - Agent 工具函数
- [x] agents/AgentValidator.java - Agent 验证器
- [x] agents/AgentGenerator.java - Agent 生成器
- [x] permissions/PermissionComponentUtils.java - 权限组件工具
- [x] permissions/PermissionHooks.java - 权限钩子

### 工具模块 (utils)
- [x] ArrayUtils.java - 数组工具
- [x] AsyncUtils.java - 异步工具
- [x] CircularBuffer.java - 循环缓冲区
- [x] Config.java - 配置工具
- [x] ContextUtils.java - 上下文工具
- [x] Cwd.java - 工作目录
- [x] EnvUtils.java - 环境工具
- [x] ErrorUtils.java - 错误处理
- [x] FileStateCache.java - 文件状态缓存
- [x] FormatUtils.java - 格式化工具
- [x] GitUtils.java - Git 工具
- [x] MessageUtils.java - 消息工具
- [x] PathUtils.java - 路径工具
- [x] Signal.java - 信号
- [x] StringUtils.java - 字符串工具
- [x] Subscription.java - 订阅
- [x] Tokens.java - Token 计数
- [x] BrowserUtils.java - 浏览器工具
- [x] SleepUtils.java - 睡眠工具
- [x] LockfileUtils.java - 锁文件工具
- [x] MemoizeUtils.java - 记忆化工具
- [x] ActivityManager.java - 活动管理器
- [x] CleanupUtils.java - 清理工具
- [x] CachePaths.java - 缓存路径
- [x] BufferedWriterUtils.java - 缓冲写入器
- [x] AbortController.java - 中止控制器
- [x] QueryGuard.java - 查询守卫
- [x] Cursor.java - 游标
- [x] ShellUtils.java - Shell 工具
- [x] BillingUtils.java - 计费工具
- [x] BinaryCheckUtils.java - 二进制检测
- [x] CliArgsUtils.java - CLI 参数解析
- [x] CompletionCache.java - 完成缓存
- [x] AgentIdUtils.java - Agent ID 工具
- [x] AttachmentUtils.java - 附件工具
- [x] AttributionUtils.java - 归属追踪
- [x] AnalyzerContext.java - 分析上下文
- [x] AnsiUtils.java - ANSI 处理
- [x] SecureStorage.java - 安全存储
- [x] ToolSchemaCache.java - 工具模式缓存

### 工具/模型模块 (utils/model)
- [x] ModelUtils.java - 模型工具

### 工具/Bash模块 (utils/bash)
- [x] ShellQuoteUtils.java - Shell 引号处理
- [x] ShellQuotingUtils.java - Shell 命令引用
- [x] HeredocUtils.java - Heredoc 提取/恢复
- [x] IParsedCommand.java - 解析命令接口
- [x] OutputRedirection.java - 输出重定向
- [x] TreeSitterAnalysis.java - Tree-sitter 分析结果
- [x] RegexParsedCommand.java - 正则解析命令
- [x] CommandUtils.java - 命令工具
- [x] ParsedCommand.java - 解析命令

### 权限工具模块 (utils/permissions)
- [x] DangerousPatterns.java - 危险模式列表
- [x] BashClassifier.java - Bash 命令分类
- [x] PathValidation.java - 路径验证
- [x] PermissionDecisionReason.java - 权限决策原因
- [x] PermissionRuleValue.java - 权限规则值

### 常量模块 (constants) - 21/21 完成
- [x] Common.java - 通用常量
- [x] ApiLimits.java - API 限制
- [x] ToolLimits.java - 工具限制
- [x] Messages.java - 消息常量
- [x] Tools.java - 工具常量
- [x] Files.java - 文件常量
- [x] Betas.java - Beta 头
- [x] ErrorIds.java - 错误 ID
- [x] Figures.java - Unicode 符号
- [x] SpinnerVerbs.java - 加载动词
- [x] TurnCompletionVerbs.java - 完成动词
- [x] XmlTags.java - XML 标签
- [x] CyberRiskInstruction.java - 安全指令
- [x] GitHubApp.java - GitHub App 配置
- [x] Product.java - 产品 URL
- [x] Keys.java - API 密钥
- [x] Oauth.java - OAuth 配置
- [x] OutputStyles.java - 输出样式
- [x] SystemPromptSections.java - 系统提示段
- [x] SystemConstants.java - 系统常量
- [x] Prompts.java - 系统提示

### 分析服务 (services/analytics) - 5/5 完成
- [x] AnalyticsConfig.java - 分析配置
- [x] AnalyticsMetadata.java - 分析元数据
- [x] AnalyticsSink.java - 事件接收器
- [x] DatadogAnalytics.java - Datadog 集成
- [x] GrowthBookService.java - Feature Flag 服务

### 策略服务 (services/policy) - 2/2 完成
- [x] PolicyLimitsService.java - 企业策略限制服务
- [x] RemoteManagedSettingsService.java - 远程设置同步服务
- [x] McpTypes.java - MCP 类型定义
- [x] McpConfig.java - MCP 配置
- [x] McpUtils.java - MCP 工具函数
- [x] McpNormalization.java - MCP 名称规范化
- [x] McpStringUtils.java - MCP 字符串解析工具

### 压缩服务 (services/compact)
- [x] CompactService.java - 压缩服务
- [x] CompactPrompts.java - 压缩提示

### 类型模块 (types)
- [x] Ids.java - ID 类型
- [x] CommandTypes.java - 命令类型
- [x] HookTypes.java - 钩子类型
- [x] MessageTypes.java - 消息类型

### 命令模块 (commands) - 60/60 完成 ✓
- [x] Command.java - 命令接口
- [x] CommandContext.java - 命令上下文
- [x] CommandResult.java - 命令结果
- [x] CommandRegistry.java - 命令注册表
- [x] InitCommand.java - 初始化命令
- [x] VersionCommand.java - 版本命令
- [x] HelpCommand.java - 帮助命令
- [x] ClearCommand.java - 清除命令
- [x] AddDirCommand.java - 添加目录命令
- [x] CopyCommand.java - 复制命令
- [x] CommitCommand.java - Git 提交命令
- [x] BranchCommand.java - Git 分支命令
- [x] DiffCommand.java - 差异对比命令
- [x] CostCommand.java - 费用命令
- [x] StatsCommand.java - 统计命令
- [x] ModelCommand.java - 模型切换命令
- [x] ExitCommand.java - 退出命令
- [x] ReviewCommand.java - 代码审查命令
- [x] CompactCommand.java - 压缩命令
- [x] ConfigCommand.java - 配置命令
- [x] DoctorCommand.java - 诊断命令
- [x] StatusCommand.java - 状态命令
- [x] UsageCommand.java - 使用量命令
- [x] LogoutCommand.java - 登出命令
- [x] LoginCommand.java - 登录命令
- [x] UpgradeCommand.java - 升级命令
- [x] RewindCommand.java - 回退命令
- [x] ResumeCommand.java - 恢复会话命令
- [x] ExportCommand.java - 导出命令
- [x] SessionCommand.java - 会话命令
- [x] McpCommand.java - MCP 服务器管理命令
- [x] SkillsCommand.java - 技能管理命令
- [x] PermissionsCommand.java - 权限命令
- [x] HooksCommand.java - 钩子命令
- [x] FastCommand.java - 快速模式命令
- [x] MemoryCommand.java - 内存管理命令
- [x] ContextCommand.java - 上下文管理命令
- [x] RenameCommand.java - 重命名会话命令
- [x] FeedbackCommand.java - 反馈命令
- [x] TasksCommand.java - 任务管理命令
- [x] PlanCommand.java - 计划模式命令
- [x] ThemeCommand.java - 主题命令
- [x] TerminalSetupCommand.java - 终端设置命令
- [x] AdvisorCommand.java - 顾问模型配置命令
- [x] ColorCommand.java - 会话颜色命令
- [x] EffortCommand.java - 工作量级别命令
- [x] FilesCommand.java - 文件上下文命令
- [x] VimCommand.java - Vim 模式命令
- [x] KeybindingsCommand.java - 键绑定命令
- [x] HeapdumpCommand.java - 堆转储命令
- [x] IdeCommand.java - IDE 集成命令
- [x] StickersCommand.java - 贴纸命令
- [x] TagCommand.java - 标签命令
- [x] SandboxCommand.java - 沙箱命令
- [x] ReleaseNotesCommand.java - 发布说明命令
- [x] PrivacySettingsCommand.java - 隐私设置命令
- [x] DesktopCommand.java - 桌面应用命令
- [x] InsightsCommand.java - 使用洞察命令
- [x] PassesCommand.java - 访问令牌命令
- [x] AgentsCommand.java - Agent 管理命令
- [x] BriefCommand.java - Brief 模式命令
- [x] BtwCommand.java - 快速问答命令

### OAuth 服务 (services/oauth) - 4/4 完成 ✓
- [x] OAuthTypes.java - OAuth 类型定义
- [x] OAuthConfig.java - OAuth 配置
- [x] OAuthCrypto.java - PKCE 加密工具
- [x] OAuthClient.java - OAuth 客户端
- [x] NotifierService.java - 通知服务
- [x] TokenEstimation.java - Token 估算
- [x] RateLimitMessages.java - 限流消息

### 会话服务 (services/session)
- [x] ClaudeAiLimits.java - 配额限制追踪
- [x] SessionMemory.java - 会话记忆服务

### 插件服务 (services/plugins) - 3/3 完成 ✓
- [x] PluginTypes.java - 插件类型定义
- [x] PluginInstallationManager.java - 插件安装管理器
- [x] PluginOperations.java - 插件操作

### 记忆提取服务 (services/extractmemories) - 2/2 完成 ✓
- [x] ExtractMemoriesService.java - 记忆提取服务
- [x] ExtractMemoriesPrompts.java - 记忆提取提示

### 自动整理服务 (services/autodream) - 3/3 完成 ✓
- [x] AutoDreamConfig.java - 自动整理配置
- [x] AutoDreamService.java - 自动整理服务
- [x] ConsolidationPrompt.java - 整合提示

### 工具模块 (claude-code-tools)
- [x] AbstractTool.java - 抽象工具基类
- [x] AgentTool.java - 代理工具
- [x] AskUserQuestionTool.java - 提问工具
- [x] BashTool.java - Shell 命令工具
- [x] BashOutputTool.java - Bash 输出工具
- [x] CopyTool.java - 复制工具
- [x] CronTool.java - 定时任务工具
- [x] DeleteTool.java - 删除工具
- [x] DiffTool.java - 差异工具
- [x] EnterPlanModeTool.java - 进入计划模式
- [x] FileEditTool.java - 文件编辑工具
- [x] FileReadTool.java - 文件读取工具
- [x] FileWriteTool.java - 文件写入工具
- [x] GlobTool.java - 文件匹配工具
- [x] GrepTool.java - 内容搜索工具
- [x] ListTool.java - 列表工具
- [x] MkdirTool.java - 创建目录工具
- [x] MoveTool.java - 移动工具
- [x] NotebookEditTool.java - 笔记编辑工具
- [x] ReadImageTool.java - 图片读取工具
- [x] SkillTool.java - 技能工具
- [x] TaskTool.java - 任务工具
- [x] ToolFactory.java - 工具工厂
- [x] WebFetchTool.java - Web 获取工具
- [x] WebSearchTool.java - Web 搜索工具

### CLI 模块 (claude-code-cli)
- [x] Main.java - 入口
- [x] CliConfig.java - CLI 配置
- [x] Command.java - 命令接口
- [x] CommandContext.java - 命令上下文
- [x] CommandProcessor.java - 命令处理器
- [x] CommandRegistry.java - 命令注册表
- [x] CommandResult.java - 命令结果
- [x] CommandLineInterface.java - 命令行接口
- [x] CliCompleter.java - 命令补全
- [x] ConsolePlanApprovalHandler.java - 计划批准处理
- [x] ConsoleUserInteractionHandler.java - 用户交互处理
- [x] InputHandler.java - 输入处理
- [x] InputReader.java - 输入读取
- [x] InteractiveSession.java - 交互会话
- [x] OutputFormatter.java - 输出格式化
- [x] OutputWriter.java - 输出写入
- [x] ReplRunner.java - REPL 运行器
- [x] VersionProvider.java - 版本提供者

## 待移植模块

### 高优先级
- [ ] services/api/*.ts - 更多 API 服务
- [ ] components - UI 组件 (约 100+ 文件)
- [ ] commands - 命令实现 (部分完成，约 40+ 文件)

### 中优先级
- [x] utils/bash - Bash 相关工具 (已完成)
- [x] utils/permissions - 权限工具 (已完成)
- [x] services/lsp - LSP 服务 (已完成)
- [ ] services/mcp - 更多 MCP 服务

### 低优先级
- [ ] ink - Ink UI 框架
- [ ] vim - Vim 模式
- [ ] keybindings - 键绑定

## 更新日期
2026-04-03 (自动调度任务)

## 本次更新
- services: 完成 11 个新服务模块 ✓
  - fileops/FileOperationsService.java - 文件操作服务
  - diagnostics/DiagnosticTrackingService.java - 诊断追踪
  - git/GitService.java, GitDiffService.java - Git 服务
  - magicdocs/MagicDocsService.java - 文档分析
  - prompts/PromptSuggestionService.java - 提示建议
  - agentsummary/AgentSummaryService.java - Agent 摘要
  - away/AwaySummaryService.java - 离开摘要
  - internal/InternalLoggingService.java - 内部日志
  - ratelimit/RateLimitService.java - 限流服务
  - limits/ClaudeAiLimitsService.java - 配额限制
- utils: 完成 9 个新工具模块 ✓
  - network/NetworkUtils.java - 网络工具
  - encoding/EncodingUtils.java - 编码工具
  - crypto/HashUtils.java, CryptoUtils.java - 加密工具
  - UuidUtils.java - UUID 工具
  - validation/ValidationUtils.java - 验证工具
  - timer/TimerUtils.java - 计时器工具
  - ansi/AnsiUtils.java - ANSI 工具
- services/mcp: 完成 7 个 MCP 服务模块 ✓
  - transports/McpTransport.java - 传输接口
  - transports/InProcessTransport.java - 进程内传输
  - transports/SdkControlTransport.java - SDK 控制传输
  - McpAuth.java - 认证服务
  - McpChannelAllowlist.java - 通道白名单
  - McpChannelNotification.java - 通道通知
  - McpElicitationHandler.java - 请求处理器
  - McpEnvExpansion.java - 环境变量展开
  - McpOfficialRegistry.java - 官方注册表