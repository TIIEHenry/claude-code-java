#!/bin/bash
# 移植进度监控脚本
# 用法: ./monitor_progress.sh

PROJECT_DIR="/Users/luo/agentlearning/frameworks/claude-code-java"
PROGRESS_FILE="$PROJECT_DIR/MIGRATION_PROGRESS.md"

echo "=== Claude Code Java 移植进度监控 ==="
echo "检查时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo ""

# 检查进度文件
if [ -f "$PROGRESS_FILE" ]; then
    echo "📄 进度文件状态:"
    echo "最后修改: $(stat -f "%Sm" "$PROGRESS_FILE" 2>/dev/null || stat -c "%y" "$PROGRESS_FILE" 2>/dev/null)"
    echo ""

    # 检查是否需要更新（超过30分钟）
    LAST_MOD=$(stat -f "%m" "$PROGRESS_FILE" 2>/dev/null || stat -c "%Y" "$PROGRESS_FILE" 2>/dev/null)
    NOW=$(date +%s)
    DIFF=$((NOW - LAST_MOD))
    MINUTES=$((DIFF / 60))

    if [ $MINUTES -gt 30 ]; then
        echo "⚠️  警告: 进度文件超过 $MINUTES 分钟未更新"
        echo "原会话可能已停止，需要发送继续指令"
    else
        echo "✅ 进度文件 $MINUTES 分钟前更新，工作正在进行"
    fi
else
    echo "❌ 进度文件不存在"
fi

echo ""
echo "📊 代码统计:"
TOTAL_LINES=$(find "$PROJECT_DIR" -name "*.java" -exec cat {} \; 2>/dev/null | wc -l | tr -d ' ')
TOTAL_FILES=$(find "$PROJECT_DIR" -name "*.java" 2>/dev/null | wc -l | tr -d ' ')
echo "Java 文件数: $TOTAL_FILES"
echo "代码总行数: $TOTAL_LINES"
echo "预估进度: $(echo "scale=2; $TOTAL_LINES / 700000 * 100" | bc)%"

echo ""
echo "📁 最近修改的文件:"
find "$PROJECT_DIR" -name "*.java" -mtime -1 -type f 2>/dev/null | head -5

echo ""
echo "======================================"
echo "如需继续工作，请复制以下指令到原会话:"
echo ""
cat "$PROJECT_DIR/CONTINUE_WORK.md" 2>/dev/null | grep -A 20 "继续Java移植工作" | head -15