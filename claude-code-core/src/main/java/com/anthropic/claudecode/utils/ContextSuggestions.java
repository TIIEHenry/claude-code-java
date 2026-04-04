/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code context suggestions utilities
 */
package com.anthropic.claudecode.utils;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Generate suggestions for optimizing context window usage.
 */
public final class ContextSuggestions {
    private ContextSuggestions() {}

    /**
     * Suggestion severity enum.
     */
    public enum Severity {
        INFO, WARNING
    }

    /**
     * Context suggestion record.
     */
    public record ContextSuggestion(
            Severity severity,
            String title,
            String detail,
            int savingsTokens
    ) {}

    // Thresholds
    private static final int LARGE_TOOL_RESULT_PERCENT = 15;
    private static final int LARGE_TOOL_RESULT_TOKENS = 10_000;
    private static final int READ_BLOAT_PERCENT = 5;
    private static final int NEAR_CAPACITY_PERCENT = 80;
    private static final int MEMORY_HIGH_PERCENT = 5;
    private static final int MEMORY_HIGH_TOKENS = 5_000;

    /**
     * Tool call breakdown record.
     */
    public record ToolCallBreakdown(
            String name,
            int callTokens,
            int resultTokens
    ) {}

    /**
     * Memory file record.
     */
    public record MemoryFile(String path, int tokens) {}

    /**
     * Message breakdown record.
     */
    public record MessageBreakdown(List<ToolCallBreakdown> toolCallsByType) {}

    /**
     * Context data for suggestions.
     */
    public record ContextData(
            int percentage,
            boolean isAutoCompactEnabled,
            int rawMaxTokens,
            MessageBreakdown messageBreakdown,
            List<MemoryFile> memoryFiles
    ) {}

    /**
     * Generate context suggestions from data.
     */
    public static List<ContextSuggestion> generateContextSuggestions(ContextData data) {
        List<ContextSuggestion> suggestions = new ArrayList<>();

        checkNearCapacity(data, suggestions);
        checkLargeToolResults(data, suggestions);
        checkReadResultBloat(data, suggestions);
        checkMemoryBloat(data, suggestions);
        checkAutoCompactDisabled(data, suggestions);

        // Sort: warnings first, then by savings descending
        suggestions.sort((a, b) -> {
            if (a.severity() != b.severity()) {
                return a.severity() == Severity.WARNING ? -1 : 1;
            }
            return Integer.compare(b.savingsTokens(), a.savingsTokens());
        });

        return suggestions;
    }

    private static void checkNearCapacity(ContextData data, List<ContextSuggestion> suggestions) {
        if (data.percentage() >= NEAR_CAPACITY_PERCENT) {
            String detail = data.isAutoCompactEnabled()
                    ? "Autocompact will trigger soon, which discards older messages. Use /compact now to control what gets kept."
                    : "Autocompact is disabled. Use /compact to free space, or enable autocompact in /config.";
            suggestions.add(new ContextSuggestion(
                    Severity.WARNING,
                    String.format("Context is %d%% full", data.percentage()),
                    detail,
                    0
            ));
        }
    }

    private static void checkLargeToolResults(ContextData data, List<ContextSuggestion> suggestions) {
        if (data.messageBreakdown() == null) return;

        for (ToolCallBreakdown tool : data.messageBreakdown().toolCallsByType()) {
            int totalToolTokens = tool.callTokens() + tool.resultTokens();
            double percent = (totalToolTokens * 100.0) / data.rawMaxTokens();

            if (percent < LARGE_TOOL_RESULT_PERCENT || totalToolTokens < LARGE_TOOL_RESULT_TOKENS) {
                continue;
            }

            ContextSuggestion suggestion = getLargeToolSuggestion(
                    tool.name(), totalToolTokens, percent
            );
            if (suggestion != null) {
                suggestions.add(suggestion);
            }
        }
    }

    private static ContextSuggestion getLargeToolSuggestion(String toolName, int tokens, double percent) {
        String tokenStr = formatTokens(tokens);

        return switch (toolName) {
            case "Bash" -> new ContextSuggestion(
                    Severity.WARNING,
                    String.format("Bash results using %s tokens (%.0f%%)", tokenStr, percent),
                    "Pipe output through head, tail, or grep to reduce result size. Avoid cat on large files — use Read with offset/limit instead.",
                    (int) (tokens * 0.5)
            );
            case "Read" -> new ContextSuggestion(
                    Severity.INFO,
                    String.format("Read results using %s tokens (%.0f%%)", tokenStr, percent),
                    "Use offset and limit parameters to read only the sections you need. Avoid re-reading entire files when you only need a few lines.",
                    (int) (tokens * 0.3)
            );
            case "Grep" -> new ContextSuggestion(
                    Severity.INFO,
                    String.format("Grep results using %s tokens (%.0f%%)", tokenStr, percent),
                    "Add more specific patterns or use the glob or type parameter to narrow file types. Consider Glob for file discovery instead of Grep.",
                    (int) (tokens * 0.3)
            );
            case "WebFetch" -> new ContextSuggestion(
                    Severity.INFO,
                    String.format("WebFetch results using %s tokens (%.0f%%)", tokenStr, percent),
                    "Web page content can be very large. Consider extracting only the specific information needed.",
                    (int) (tokens * 0.4)
            );
            default -> {
                if (percent >= 20) {
                    yield new ContextSuggestion(
                            Severity.INFO,
                            String.format("%s using %s tokens (%.0f%%)", toolName, tokenStr, percent),
                            "This tool is consuming a significant portion of context.",
                            (int) (tokens * 0.2)
                    );
                }
                yield null;
            }
        };
    }

    private static void checkReadResultBloat(ContextData data, List<ContextSuggestion> suggestions) {
        if (data.messageBreakdown() == null) return;

        ToolCallBreakdown readTool = data.messageBreakdown().toolCallsByType().stream()
                .filter(t -> "Read".equals(t.name()))
                .findFirst()
                .orElse(null);
        if (readTool == null) return;

        int totalReadTokens = readTool.callTokens() + readTool.resultTokens();
        double totalReadPercent = (totalReadTokens * 100.0) / data.rawMaxTokens();
        double readPercent = (readTool.resultTokens() * 100.0) / data.rawMaxTokens();

        if (totalReadPercent >= LARGE_TOOL_RESULT_PERCENT && totalReadTokens >= LARGE_TOOL_RESULT_TOKENS) {
            return;
        }

        if (readPercent >= READ_BLOAT_PERCENT && readTool.resultTokens() >= LARGE_TOOL_RESULT_TOKENS) {
            suggestions.add(new ContextSuggestion(
                    Severity.INFO,
                    String.format("File reads using %s tokens (%.0f%%)",
                            formatTokens(readTool.resultTokens()), readPercent),
                    "If you are re-reading files, consider referencing earlier reads. Use offset/limit for large files.",
                    (int) (readTool.resultTokens() * 0.3)
            ));
        }
    }

    private static void checkMemoryBloat(ContextData data, List<ContextSuggestion> suggestions) {
        int totalMemoryTokens = data.memoryFiles().stream()
                .mapToInt(MemoryFile::tokens)
                .sum();
        double memoryPercent = (totalMemoryTokens * 100.0) / data.rawMaxTokens();

        if (memoryPercent >= MEMORY_HIGH_PERCENT && totalMemoryTokens >= MEMORY_HIGH_TOKENS) {
            String largestFiles = data.memoryFiles().stream()
                    .sorted((a, b) -> Integer.compare(b.tokens(), a.tokens()))
                    .limit(3)
                    .map(f -> String.format("%s (%s)", getDisplayPath(f.path()), formatTokens(f.tokens())))
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");

            suggestions.add(new ContextSuggestion(
                    Severity.INFO,
                    String.format("Memory files using %s tokens (%.0f%%)",
                            formatTokens(totalMemoryTokens), memoryPercent),
                    "Largest: " + largestFiles + ". Use /memory to review and prune stale entries.",
                    (int) (totalMemoryTokens * 0.3)
            ));
        }
    }

    private static void checkAutoCompactDisabled(ContextData data, List<ContextSuggestion> suggestions) {
        if (!data.isAutoCompactEnabled() && data.percentage() >= 50 && data.percentage() < NEAR_CAPACITY_PERCENT) {
            suggestions.add(new ContextSuggestion(
                    Severity.INFO,
                    "Autocompact is disabled",
                    "Without autocompact, you will hit context limits and lose the conversation. Enable it in /config or use /compact manually.",
                    0
            ));
        }
    }

    private static String formatTokens(int tokens) {
        if (tokens >= 1_000_000) {
            return String.format("%.1fM", tokens / 1_000_000.0);
        } else if (tokens >= 1_000) {
            return String.format("%.1fK", tokens / 1_000.0);
        }
        return String.valueOf(tokens);
    }

    private static String getDisplayPath(String path) {
        Path p = Paths.get(path);
        String fileName = p.getFileName() != null ? p.getFileName().toString() : path;
        Path parent = p.getParent();
        if (parent != null) {
            return parent.getFileName() + "/" + fileName;
        }
        return fileName;
    }
}