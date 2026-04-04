# 持续工作指令

## 直接复制到原会话使用

当原会话停止时，复制以下指令：

```
继续Java移植工作。要求：

1. 先读取 MIGRATION_PROGRESS.md 了解当前进度
2. 解决所有阻塞问题
3. 继续实现下一个未完成的模块
4. 每完成一个模块，更新 MIGRATION_PROGRESS.md
5. 不要总结，不要停下来，直到遇到以下情况才停止：
   - 遇到需要用户决策的问题
   - 完成所有功能移植
   - 连续工作满2小时（此时更新进度文件后继续）

当前优先级：
1. 解决泛型类型推断问题
2. 完成剩余工具实现
3. 完善 CLI 模块
4. 添加测试

记住：目标是完成 51万行 TS 代码的完整移植，当前只完成了 1.3%
```

## 进度检查命令

在任意会话中运行以下命令检查进度：

```bash
# 查看进度文件
cat /Users/luo/agentlearning/frameworks/claude-code-java/MIGRATION_PROGRESS.md

# 统计已完成代码行数
find /Users/luo/agentlearning/frameworks/claude-code-java -name "*.java" | xargs wc -l

# 查看最后修改的文件
find /Users/luo/agentlearning/frameworks/claude-code-java -name "*.java" -mtime -1 | head -10
```

## 当前进度快照

- 创建时间: 2026-04-01
- 已安排监控任务: 每10分钟检查一次
- 监控任务ID: 961227f2