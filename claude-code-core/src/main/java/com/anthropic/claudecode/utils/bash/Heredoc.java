/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/bash/heredoc
 */
package com.anthropic.claudecode.utils.bash;

import java.util.*;
import java.util.regex.*;

/**
 * Heredoc - Heredoc parsing utilities.
 */
public final class Heredoc {
    private static final Pattern HEREDOC_PATTERN = Pattern.compile(
        "<<(-)?\\s*['\"]?([^'\"\\s]+)['\"]?"
    );

    /**
     * Parse heredoc from command.
     */
    public static HeredocInfo parse(String command) {
        if (command == null || !command.contains("<<")) {
            return HeredocInfo.empty();
        }

        Matcher matcher = HEREDOC_PATTERN.matcher(command);
        if (!matcher.find()) {
            return HeredocInfo.empty();
        }

        boolean stripTabs = matcher.group(1) != null;
        String delimiter = matcher.group(2);

        // Find heredoc content
        int heredocStart = matcher.end();
        String remaining = command.substring(heredocStart).trim();
        String content = extractContent(remaining, delimiter);

        return new HeredocInfo(
            delimiter,
            content,
            stripTabs,
            heredocStart
        );
    }

    /**
     * Extract heredoc content.
     */
    private static String extractContent(String remaining, String delimiter) {
        // Simple extraction - find delimiter line
        int delimiterPos = remaining.indexOf("\n" + delimiter);
        if (delimiterPos >= 0) {
            return remaining.substring(0, delimiterPos);
        }

        // Check if delimiter at end
        delimiterPos = remaining.indexOf(delimiter);
        if (delimiterPos >= 0) {
            return remaining.substring(0, delimiterPos);
        }

        return remaining;
    }

    /**
     * Check if command has heredoc.
     */
    public static boolean hasHeredoc(String command) {
        if (command == null) return false;
        return HEREDOC_PATTERN.matcher(command).find();
    }

    /**
     * Build heredoc command.
     */
    public static String buildHeredocCommand(
        String prefixCommand,
        String delimiter,
        String content,
        boolean stripTabs
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefixCommand);
        sb.append(" <<");
        if (stripTabs) sb.append("-");
        sb.append(" ").append(delimiter).append("\n");
        sb.append(content).append("\n");
        sb.append(delimiter);
        return sb.toString();
    }

    /**
     * Strip leading tabs from heredoc content.
     */
    public static String stripLeadingTabs(String content) {
        if (content == null) return "";
        return content.replaceAll("^\\t+", "");
    }

    /**
     * Heredoc info record.
     */
    public record HeredocInfo(
        String delimiter,
        String content,
        boolean stripTabs,
        int startPosition
    ) {
        public static HeredocInfo empty() {
            return new HeredocInfo("", "", false, -1);
        }

        public boolean hasHeredoc() {
            return startPosition >= 0;
        }

        public String getProcessedContent() {
            if (stripTabs) {
                return stripLeadingTabs(content);
            }
            return content;
        }
    }

    /**
     * Heredoc type enum.
     */
    public enum HeredocType {
        STANDARD,       // << delimiter
        STRIP_TABS,     // <<- delimiter
        QUOTED          // << 'delimiter' or << "delimiter"
    }

    /**
     * Detect heredoc type.
     */
    public static HeredocType detectType(String command) {
        if (command == null) return HeredocType.STANDARD;

        if (command.contains("<<-")) {
            return HeredocType.STRIP_TABS;
        }
        if (command.contains("<<'") || command.contains("<<\"")) {
            return HeredocType.QUOTED;
        }

        return HeredocType.STANDARD;
    }
}