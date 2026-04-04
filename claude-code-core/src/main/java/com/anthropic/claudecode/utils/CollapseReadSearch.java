/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code read/search collapsing utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

/**
 * Collapse consecutive Read/Search operations into summary groups.
 *
 * Groups consecutive search/read tool uses (Grep, Glob, Read, and Bash
 * search/read commands) with their corresponding tool results.
 */
public final class CollapseReadSearch {
    private CollapseReadSearch() {}

    private static final int MAX_HINT_CHARS = 300;

    /**
     * Search or read result info.
     */
    public record SearchOrReadResult(
            boolean isCollapsible,
            boolean isSearch,
            boolean isRead,
            boolean isList,
            boolean isREPL,
            boolean isMemoryWrite,
            boolean isAbsorbedSilently,
            String mcpServerName,
            boolean isBash
    ) {}

    /**
     * Group accumulator for collapsing.
     */
    public record GroupAccumulator(
            List<CollapsibleMessage> messages,
            int searchCount,
            Set<String> readFilePaths,
            int readOperationCount,
            int listCount,
            Set<String> toolUseIds,
            int memorySearchCount,
            Set<String> memoryReadFilePaths,
            int memoryWriteCount,
            List<String> nonMemSearchArgs,
            String latestDisplayHint,
            int mcpCallCount,
            Set<String> mcpServerNames,
            int bashCount,
            Map<String, String> bashCommands,
            int hookTotalMs,
            int hookCount
    ) {
        public static GroupAccumulator createEmpty() {
            return new GroupAccumulator(
                    new ArrayList<>(),
                    0,
                    new HashSet<>(),
                    0,
                    0,
                    new HashSet<>(),
                    0,
                    new HashSet<>(),
                    0,
                    new ArrayList<>(),
                    null,
                    0,
                    new HashSet<>(),
                    0,
                    new HashMap<>(),
                    0,
                    0
            );
        }
    }

    /**
     * Collapsed read/search group.
     */
    public record CollapsedReadSearchGroup(
            String type,  // "collapsed_read_search"
            int searchCount,
            int readCount,
            int listCount,
            int replCount,
            int memorySearchCount,
            int memoryReadCount,
            int memoryWriteCount,
            List<String> readFilePaths,
            List<String> searchArgs,
            String latestDisplayHint,
            List<CollapsibleMessage> messages,
            CollapsibleMessage displayMessage,
            String uuid,
            long timestamp,
            int mcpCallCount,
            List<String> mcpServerNames,
            int bashCount,
            int hookTotalMs,
            int hookCount
    ) {}

    /**
     * Collapsible message interface.
     */
    public sealed interface CollapsibleMessage permits
            CollapsibleMessage.AssistantMessage, CollapsibleMessage.UserMessage, CollapsibleMessage.GroupedMessage {

        String uuid();
        long timestamp();
        String type();

        record AssistantMessage(String uuid, long timestamp, ToolUseContent toolUse) implements CollapsibleMessage {
            @Override public String type() { return "assistant"; }
        }

        record UserMessage(String uuid, long timestamp, ToolResultContent toolResult) implements CollapsibleMessage {
            @Override public String type() { return "user"; }
        }

        record GroupedMessage(String uuid, long timestamp, String toolName, List<AssistantMessage> messages) implements CollapsibleMessage {
            @Override public String type() { return "grouped_tool_use"; }
        }
    }

    /**
     * Tool use content.
     */
    public record ToolUseContent(String type, String name, String id, Object input) {}

    /**
     * Tool result content.
     */
    public record ToolResultContent(String type, String toolUseId, Object output) {}

    /**
     * Format a bash command for display hint.
     */
    public static String commandAsHint(String command) {
        String cleaned = "$ " + command.lines()
                .map(l -> l.replaceAll("\\s+", " ").trim())
                .filter(l -> !l.isEmpty())
                .collect(Collectors.joining("\n"));
        return cleaned.length() > MAX_HINT_CHARS
                ? cleaned.substring(0, MAX_HINT_CHARS - 1) + "…"
                : cleaned;
    }

    /**
     * Get search/read summary text.
     */
    public static String getSearchReadSummaryText(
            int searchCount, int readCount, boolean isActive,
            int replCount, int listCount) {

        List<String> parts = new ArrayList<>();

        if (searchCount > 0) {
            String verb = isActive
                    ? (parts.isEmpty() ? "Searching for" : "searching for")
                    : (parts.isEmpty() ? "Searched for" : "searched for");
            parts.add(String.format("%s %d %s", verb, searchCount,
                    searchCount == 1 ? "pattern" : "patterns"));
        }

        if (readCount > 0) {
            String verb = isActive
                    ? (parts.isEmpty() ? "Reading" : "reading")
                    : (parts.isEmpty() ? "Read" : "read");
            parts.add(String.format("%s %d %s", verb, readCount,
                    readCount == 1 ? "file" : "files"));
        }

        if (listCount > 0) {
            String verb = isActive
                    ? (parts.isEmpty() ? "Listing" : "listing")
                    : (parts.isEmpty() ? "Listed" : "listed");
            parts.add(String.format("%s %d %s", verb, listCount,
                    listCount == 1 ? "directory" : "directories"));
        }

        if (replCount > 0) {
            String verb = isActive ? "REPL'ing" : "REPL'd";
            parts.add(String.format("%s %d %s", verb, replCount,
                    replCount == 1 ? "time" : "times"));
        }

        String text = String.join(", ", parts);
        return isActive ? text + "…" : text;
    }

    /**
     * Check if a tool is a search/read operation.
     */
    public static SearchOrReadResult getToolSearchOrReadInfo(String toolName, Object toolInput) {
        // Common search/read tools
        boolean isSearch = toolName.equals("Grep") || toolName.equals("Glob");
        boolean isRead = toolName.equals("Read") || toolName.equals("Head") || toolName.equals("Tail");
        boolean isList = toolName.equals("Ls") || toolName.equals("Tree");

        if (toolName.equals("Bash")) {
            // Check if bash command is a search/read
            String command = extractCommand(toolInput);
            if (command != null) {
                if (isSearchCommand(command)) {
                    isSearch = true;
                } else if (isReadCommand(command)) {
                    isRead = true;
                } else if (isListCommand(command)) {
                    isList = true;
                }
            }
        }

        boolean isCollapsible = isSearch || isRead || isList;

        return new SearchOrReadResult(
                isCollapsible, isSearch, isRead, isList,
                false, false, false, null, false
        );
    }

    /**
     * Extract command from bash tool input.
     */
    private static String extractCommand(Object input) {
        if (input instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) input;
            Object cmd = map.get("command");
            return cmd instanceof String ? (String) cmd : null;
        }
        return null;
    }

    /**
     * Check if bash command is a search command.
     */
    private static boolean isSearchCommand(String command) {
        String lower = command.toLowerCase();
        return lower.contains("grep") || lower.contains("rg") ||
                lower.contains("find") || lower.contains("ag");
    }

    /**
     * Check if bash command is a read command.
     */
    private static boolean isReadCommand(String command) {
        String lower = command.toLowerCase();
        return lower.contains("cat") || lower.contains("head") ||
                lower.contains("tail") || lower.contains("less") ||
                lower.contains("more");
    }

    /**
     * Check if bash command is a list command.
     */
    private static boolean isListCommand(String command) {
        String lower = command.toLowerCase();
        return lower.startsWith("ls") || lower.startsWith("tree") ||
                lower.startsWith("dir") || lower.startsWith("du");
    }

    /**
     * Extract file path from tool input.
     */
    public static String getFilePathFromToolInput(Object toolInput) {
        if (toolInput instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) toolInput;
            Object path = map.get("file_path");
            if (path == null) path = map.get("path");
            return path instanceof String ? (String) path : null;
        }
        return null;
    }
}