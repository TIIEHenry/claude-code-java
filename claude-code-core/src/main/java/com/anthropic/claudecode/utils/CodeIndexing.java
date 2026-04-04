/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code code indexing detection
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.regex.*;

/**
 * Utilities for detecting code indexing tool usage.
 */
public final class CodeIndexing {
    private CodeIndexing() {}

    /**
     * Code indexing tool identifiers.
     */
    public enum CodeIndexingTool {
        SOURCEGRAPH, HOUND, SEAGOAT, BLOOP, GITLOOP,
        CODY, AIDER, CONTINUE, GITHUB_COPILOT, CURSOR,
        TABBY, CODEIUM, TABNINE, AUGMENT, WINDSURF,
        AIDE, PIECES, QODO, AMAZON_Q, GEMINI,
        CLAUDE_CONTEXT, CODE_INDEX_MCP, LOCAL_CODE_SEARCH,
        AUTODEV_CODEBASE, OPENCTX
    }

    private static final Map<String, CodeIndexingTool> CLI_COMMAND_MAPPING = Map.ofEntries(
            Map.entry("src", CodeIndexingTool.SOURCEGRAPH),
            Map.entry("cody", CodeIndexingTool.CODY),
            Map.entry("aider", CodeIndexingTool.AIDER),
            Map.entry("tabby", CodeIndexingTool.TABBY),
            Map.entry("tabnine", CodeIndexingTool.TABNINE),
            Map.entry("augment", CodeIndexingTool.AUGMENT),
            Map.entry("pieces", CodeIndexingTool.PIECES),
            Map.entry("qodo", CodeIndexingTool.QODO),
            Map.entry("aide", CodeIndexingTool.AIDE),
            Map.entry("hound", CodeIndexingTool.HOUND),
            Map.entry("seagoat", CodeIndexingTool.SEAGOAT),
            Map.entry("bloop", CodeIndexingTool.BLOOP),
            Map.entry("gitloop", CodeIndexingTool.GITLOOP),
            Map.entry("q", CodeIndexingTool.AMAZON_Q),
            Map.entry("gemini", CodeIndexingTool.GEMINI)
    );

    private static final List<PatternTool> MCP_SERVER_PATTERNS = List.of(
            new PatternTool(Pattern.compile("^sourcegraph$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.SOURCEGRAPH),
            new PatternTool(Pattern.compile("^cody$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.CODY),
            new PatternTool(Pattern.compile("^openctx$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.OPENCTX),
            new PatternTool(Pattern.compile("^aider$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.AIDER),
            new PatternTool(Pattern.compile("^continue$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.CONTINUE),
            new PatternTool(Pattern.compile("^github[-_]?copilot$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.GITHUB_COPILOT),
            new PatternTool(Pattern.compile("^copilot$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.GITHUB_COPILOT),
            new PatternTool(Pattern.compile("^cursor$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.CURSOR),
            new PatternTool(Pattern.compile("^tabby$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.TABBY),
            new PatternTool(Pattern.compile("^codeium$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.CODEIUM),
            new PatternTool(Pattern.compile("^tabnine$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.TABNINE),
            new PatternTool(Pattern.compile("^augment[-_]?code$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.AUGMENT),
            new PatternTool(Pattern.compile("^augment$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.AUGMENT),
            new PatternTool(Pattern.compile("^windsurf$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.WINDSURF),
            new PatternTool(Pattern.compile("^aide$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.AIDE),
            new PatternTool(Pattern.compile("^codestory$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.AIDE),
            new PatternTool(Pattern.compile("^pieces$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.PIECES),
            new PatternTool(Pattern.compile("^qodo$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.QODO),
            new PatternTool(Pattern.compile("^amazon[-_]?q$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.AMAZON_Q),
            new PatternTool(Pattern.compile("^gemini[-_]?code[-_]?assist$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.GEMINI),
            new PatternTool(Pattern.compile("^gemini$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.GEMINI),
            new PatternTool(Pattern.compile("^hound$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.HOUND),
            new PatternTool(Pattern.compile("^seagoat$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.SEAGOAT),
            new PatternTool(Pattern.compile("^bloop$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.BLOOP),
            new PatternTool(Pattern.compile("^gitloop$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.GITLOOP),
            new PatternTool(Pattern.compile("^claude[-_]?context$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.CLAUDE_CONTEXT),
            new PatternTool(Pattern.compile("^code[-_]?index[-_]?mcp$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.CODE_INDEX_MCP),
            new PatternTool(Pattern.compile("^code[-_]?index$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.CODE_INDEX_MCP),
            new PatternTool(Pattern.compile("^local[-_]?code[-_]?search$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.LOCAL_CODE_SEARCH),
            new PatternTool(Pattern.compile("^codebase$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.AUTODEV_CODEBASE),
            new PatternTool(Pattern.compile("^autodev[-_]?codebase$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.AUTODEV_CODEBASE),
            new PatternTool(Pattern.compile("^code[-_]?context$", Pattern.CASE_INSENSITIVE), CodeIndexingTool.CLAUDE_CONTEXT)
    );

    private record PatternTool(Pattern pattern, CodeIndexingTool tool) {}

    /**
     * Detect code indexing tool from command.
     */
    public static CodeIndexingTool detectCodeIndexingFromCommand(String command) {
        if (command == null || command.isBlank()) return null;

        String trimmed = command.trim().toLowerCase();
        String[] parts = trimmed.split("\\s+");
        if (parts.length == 0) return null;

        String firstWord = parts[0];

        // Check for npx/bunx prefixed commands
        if ("npx".equals(firstWord) || "bunx".equals(firstWord)) {
            if (parts.length > 1) {
                String secondWord = parts[1];
                if (CLI_COMMAND_MAPPING.containsKey(secondWord)) {
                    return CLI_COMMAND_MAPPING.get(secondWord);
                }
            }
        }

        return CLI_COMMAND_MAPPING.get(firstWord);
    }

    /**
     * Detect code indexing tool from MCP tool name.
     */
    public static CodeIndexingTool detectCodeIndexingFromMcpTool(String toolName) {
        if (toolName == null || !toolName.startsWith("mcp__")) return null;

        String[] parts = toolName.split("__");
        if (parts.length < 3) return null;

        String serverName = parts[1];
        if (serverName == null) return null;

        for (PatternTool pt : MCP_SERVER_PATTERNS) {
            if (pt.pattern().matcher(serverName).matches()) {
                return pt.tool();
            }
        }

        return null;
    }

    /**
     * Detect code indexing tool from MCP server name.
     */
    public static CodeIndexingTool detectCodeIndexingFromMcpServerName(String serverName) {
        if (serverName == null) return null;

        for (PatternTool pt : MCP_SERVER_PATTERNS) {
            if (pt.pattern().matcher(serverName).matches()) {
                return pt.tool();
            }
        }

        return null;
    }
}