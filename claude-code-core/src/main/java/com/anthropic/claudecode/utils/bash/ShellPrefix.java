/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/bash/shellPrefix
 */
package com.anthropic.claudecode.utils.bash;

import java.util.*;
import java.util.regex.*;

/**
 * Shell prefix - Shell prefix parsing utilities.
 */
public final class ShellPrefix {
    private static final Pattern PREFIX_PATTERN = Pattern.compile(
        "^\\s*([#$>%~❯➜])\\s*"
    );
    private static final Pattern FULL_PREFIX_PATTERN = Pattern.compile(
        "^\\s*(\\S+@[\\w.-]+:[^#$>%~]+)?([#$>%~❯➜])\\s*"
    );

    /**
     * Parse shell prefix.
     */
    public static PrefixInfo parse(String line) {
        if (line == null || line.isEmpty()) {
            return PrefixInfo.empty();
        }

        Matcher matcher = FULL_PREFIX_PATTERN.matcher(line);
        if (matcher.find()) {
            String hostInfo = matcher.group(1);
            String promptChar = matcher.group(2);
            int prefixLength = matcher.end();

            return new PrefixInfo(
                hostInfo != null ? hostInfo : "",
                promptChar,
                prefixLength,
                line.substring(prefixLength)
            );
        }

        return PrefixInfo.empty();
    }

    /**
     * Get prompt character.
     */
    public static String getPromptChar(String line) {
        Matcher matcher = PREFIX_PATTERN.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    /**
     * Strip prefix.
     */
    public static String stripPrefix(String line) {
        return parse(line).content();
    }

    /**
     * Check if line has prefix.
     */
    public static boolean hasPrefix(String line) {
        if (line == null || line.isEmpty()) return false;
        return PREFIX_PATTERN.matcher(line).find();
    }

    /**
     * Detect shell type from prefix.
     */
    public static ShellQuote.ShellType detectShellType(String line) {
        String promptChar = getPromptChar(line);

        if (promptChar.equals("%")) {
            return ShellQuote.ShellType.ZSH;
        }
        if (promptChar.equals("$")) {
            return ShellQuote.ShellType.BASH;
        }
        if (promptChar.equals(">")) {
            return ShellQuote.ShellType.FISH;
        }
        if (promptChar.equals("❯")) {
            return ShellQuote.ShellType.ZSH; // Common zsh prompt
        }

        return ShellQuote.ShellType.UNKNOWN;
    }

    /**
     * Prefix info record.
     */
    public record PrefixInfo(
        String hostInfo,
        String promptChar,
        int prefixLength,
        String content
    ) {
        public static PrefixInfo empty() {
            return new PrefixInfo("", "", 0, "");
        }

        public boolean hasPrefix() {
            return prefixLength > 0;
        }

        public String format() {
            if (hostInfo.isEmpty()) {
                return promptChar + " ";
            }
            return hostInfo + promptChar + " ";
        }
    }

    /**
     * Build prefix for shell.
     */
    public static String buildPrefix(String user, String host, String path, String promptChar) {
        StringBuilder sb = new StringBuilder();
        if (user != null && host != null) {
            sb.append(user).append("@").append(host).append(":");
        }
        if (path != null) {
            sb.append(path);
        }
        sb.append(promptChar).append(" ");
        return sb.toString();
    }
}