/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/completionCache
 */
package com.anthropic.claudecode.utils.cache;

import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import java.util.stream.*;

/**
 * Completion cache - Cache for completion suggestions.
 */
public final class CompletionCache {
    private final Cache<String, List<CompletionEntry>> cache;
    private final int maxEntries = 1000;
    private final long maxAgeMs = 60 * 60 * 1000; // 1 hour

    /**
     * Completion entry record.
     */
    public record CompletionEntry(
        String text,
        String display,
        String description,
        CompletionType type,
        int score
    ) {
        public boolean matches(String query) {
            return text.startsWith(query) ||
                   display.toLowerCase().contains(query.toLowerCase());
        }
    }

    /**
     * Completion type enum.
     */
    public enum CompletionType {
        COMMAND,
        FILE,
        OPTION,
        VARIABLE,
        FUNCTION,
        KEYWORD,
        HISTORY,
        SNIPPET
    }

    /**
     * Create completion cache.
     */
    public CompletionCache() {
        this.cache = new Cache<>(maxEntries, maxAgeMs);
    }

    /**
     * Get completions for query.
     */
    public List<CompletionEntry> getCompletions(String query) {
        List<CompletionEntry> cached = cache.get(query);
        if (cached != null) {
            return cached;
        }

        // Generate new completions
        List<CompletionEntry> completions = generateCompletions(query);
        cache.put(query, completions);
        return completions;
    }

    /**
     * Generate completions.
     */
    private List<CompletionEntry> generateCompletions(String query) {
        List<CompletionEntry> completions = new ArrayList<>();

        // Add history completions
        completions.addAll(getHistoryCompletions(query));

        // Add file completions
        completions.addAll(getFileCompletions(query));

        // Add command completions
        completions.addAll(getCommandCompletions(query));

        // Sort by score
        completions.sort((a, b) -> b.score() - a.score());

        return completions;
    }

    /**
     * Get history completions.
     */
    private List<CompletionEntry> getHistoryCompletions(String query) {
        List<CompletionEntry> completions = new ArrayList<>();
        try {
            // Read command history file
            Path historyFile = Paths.get(System.getProperty("user.home"))
                .resolve(".claude")
                .resolve("history");

            if (!Files.exists(historyFile)) {
                return completions;
            }

            List<String> lines = Files.readAllLines(historyFile);
            Set<String> seen = new HashSet<>();

            // Search from most recent
            for (int i = lines.size() - 1; i >= 0 && completions.size() < 20; i--) {
                String line = lines.get(i).trim();
                if (line.isEmpty() || seen.contains(line)) continue;

                if (line.toLowerCase().contains(query.toLowerCase())) {
                    completions.add(new CompletionEntry(
                        line,
                        line.length() > 50 ? line.substring(0, 50) + "..." : line,
                        "History",
                        CompletionType.HISTORY,
                        100 - completions.size()
                    ));
                    seen.add(line);
                }
            }
        } catch (Exception e) {
            // Ignore history read errors
        }
        return completions;
    }

    /**
     * Get file completions.
     */
    private List<CompletionEntry> getFileCompletions(String query) {
        List<CompletionEntry> completions = new ArrayList<>();
        try {
            // Determine search path from query
            String searchPath;
            String prefix;

            if (query.contains("/")) {
                int lastSlash = query.lastIndexOf('/');
                searchPath = query.substring(0, lastSlash);
                prefix = query.substring(lastSlash + 1);
            } else {
                searchPath = ".";
                prefix = query;
            }

            Path baseDir = Paths.get(searchPath);
            if (!Files.exists(baseDir) || !Files.isDirectory(baseDir)) {
                return completions;
            }

            // List files matching prefix
            try (Stream<Path> files = Files.list(baseDir)) {
                files
                    .filter(p -> p.getFileName().toString().startsWith(prefix))
                    .limit(20)
                    .forEach(p -> {
                        String name = p.getFileName().toString();
                        boolean isDir = Files.isDirectory(p);
                        completions.add(new CompletionEntry(
                            searchPath + "/" + name,
                            name,
                            isDir ? "Directory" : "File",
                            CompletionType.FILE,
                            isDir ? 80 : 70
                        ));
                    });
            }
        } catch (Exception e) {
            // Ignore file system errors
        }
        return completions;
    }

    /**
     * Get command completions.
     */
    private List<CompletionEntry> getCommandCompletions(String query) {
        List<CompletionEntry> completions = new ArrayList<>();

        // Built-in slash commands
        String[] commands = {
            "/help", "/login", "/logout", "/config", "/model",
            "/init", "/compact", "/bug", "/usage", "/clear",
            "/cost", "/permissions", "/doctor", "/mcp", "/review",
            "/export", "/import", "/init", "/pr-comments", "/memory",
            "/status", "/upgrade", "/vim"
        };

        for (int i = 0; i < commands.length; i++) {
            String cmd = commands[i];
            if (cmd.startsWith(query) || query.isEmpty()) {
                completions.add(new CompletionEntry(
                    cmd,
                    cmd,
                    getCommandDescription(cmd),
                    CompletionType.COMMAND,
                    90 - i
                ));
            }
        }

        return completions;
    }

    /**
     * Get command description.
     */
    private String getCommandDescription(String cmd) {
        return switch (cmd) {
            case "/help" -> "Show help and available commands";
            case "/login" -> "Sign in to your Anthropic account";
            case "/logout" -> "Sign out from your account";
            case "/config" -> "Manage configuration settings";
            case "/model" -> "Switch between Claude models";
            case "/init" -> "Initialize project configuration";
            case "/compact" -> "Compact conversation history";
            case "/bug" -> "Report a bug";
            case "/usage" -> "Show API usage statistics";
            case "/clear" -> "Clear conversation history";
            case "/cost" -> "Show conversation cost";
            case "/permissions" -> "Manage tool permissions";
            case "/doctor" -> "Run diagnostics";
            case "/mcp" -> "Manage MCP servers";
            case "/review" -> "Review a pull request";
            case "/export" -> "Export conversation";
            case "/status" -> "Show current status";
            case "/upgrade" -> "Upgrade to latest version";
            default -> "Command";
        };
    }

    /**
     * Add completion to cache.
     */
    public void addCompletion(String key, CompletionEntry entry) {
        List<CompletionEntry> existing = cache.get(key);
        if (existing == null) {
            existing = new ArrayList<>();
        }
        existing.add(entry);
        cache.put(key, existing);
    }

    /**
     * Clear cache.
     */
    public void clear() {
        cache.clear();
    }

    /**
     * Invalidate entries starting with prefix.
     */
    public void invalidatePrefix(String prefix) {
        cache.keys()
            .stream()
            .filter(k -> k.startsWith(prefix))
            .forEach(cache::remove);
    }

    /**
     * Get cache stats.
     */
    public CacheStats getStats() {
        return new CacheStats(
            cache.size(),
            cache.keys().size(),
            cache.getHitCount(),
            cache.getMissCount()
        );
    }

    /**
     * Cache stats record.
     */
    public record CacheStats(
        int entries,
        int keys,
        long hitCount,
        long missCount
    ) {
        public double getHitRate() {
            long total = hitCount + missCount;
            return total == 0 ? 0 : (double) hitCount / total;
        }
    }
}