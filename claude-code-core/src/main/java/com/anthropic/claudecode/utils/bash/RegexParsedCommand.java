/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/bash/ParsedCommand.ts
 */
package com.anthropic.claudecode.utils.bash;

import java.util.*;
import java.util.regex.*;

/**
 * Regex-based parsed command implementation.
 * Fallback used when tree-sitter is not available.
 * @deprecated Legacy regex path, only used when tree-sitter unavailable
 */
@Deprecated
public class RegexParsedCommand implements IParsedCommand {
    private final String originalCommand;

    public RegexParsedCommand(String command) {
        this.originalCommand = command != null ? command : "";
    }

    @Override
    public String originalCommand() {
        return originalCommand;
    }

    @Override
    public String toString() {
        return originalCommand;
    }

    @Override
    public List<String> getPipeSegments() {
        if (!originalCommand.contains("|")) {
            return Collections.singletonList(originalCommand);
        }

        try {
            List<String> segments = new ArrayList<>();
            String[] parts = splitOnPipe(originalCommand);
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    segments.add(trimmed);
                }
            }
            return segments.isEmpty() ? Collections.singletonList(originalCommand) : segments;
        } catch (Exception e) {
            return Collections.singletonList(originalCommand);
        }
    }

    @Override
    public String withoutOutputRedirections() {
        if (!originalCommand.contains(">")) {
            return originalCommand;
        }
        return CommandUtils.removeOutputRedirections(originalCommand);
    }

    @Override
    public List<OutputRedirection> getOutputRedirections() {
        return CommandUtils.extractOutputRedirections(originalCommand);
    }

    @Override
    public TreeSitterAnalysis getTreeSitterAnalysis() {
        return null; // No tree-sitter analysis for regex fallback
    }

    private String[] splitOnPipe(String command) {
        // Simple split on |, handling quoted strings
        List<String> segments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean escaped = false;

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);

            if (escaped) {
                current.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\' && !inSingleQuote) {
                escaped = true;
                current.append(c);
                continue;
            }

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                current.append(c);
                continue;
            }

            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                current.append(c);
                continue;
            }

            if (c == '|' && !inSingleQuote && !inDoubleQuote) {
                segments.add(current.toString().trim());
                current = new StringBuilder();
                continue;
            }

            current.append(c);
        }

        if (current.length() > 0) {
            segments.add(current.toString().trim());
        }

        return segments.toArray(new String[0]);
    }
}