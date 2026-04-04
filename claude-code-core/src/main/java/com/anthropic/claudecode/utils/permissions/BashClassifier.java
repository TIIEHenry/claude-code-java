/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/permissions/bashClassifier.ts
 */
package com.anthropic.claudecode.utils.permissions;

import java.util.*;

/**
 * Bash command classifier for permission checks.
 * Stub implementation - classifier permissions feature is disabled by default.
 */
public final class BashClassifier {
    private BashClassifier() {}

    public static final String PROMPT_PREFIX = "prompt:";

    /**
     * Classifier result record.
     */
    public record ClassifierResult(
        boolean matches,
        String matchedDescription,
        Confidence confidence,
        String reason
    ) {
        public enum Confidence {
            HIGH, MEDIUM, LOW
        }

        public static ClassifierResult noMatch() {
            return new ClassifierResult(false, null, Confidence.HIGH, "No match");
        }

        public static ClassifierResult match(String description, String reason) {
            return new ClassifierResult(true, description, Confidence.HIGH, reason);
        }
    }

    /**
     * Classifier behavior enum.
     */
    public enum ClassifierBehavior {
        DENY, ASK, ALLOW
    }

    /**
     * Extract prompt description from rule content.
     */
    public static String extractPromptDescription(String ruleContent) {
        if (ruleContent == null || !ruleContent.startsWith(PROMPT_PREFIX)) {
            return null;
        }
        return ruleContent.substring(PROMPT_PREFIX.length()).trim();
    }

    /**
     * Create prompt rule content from description.
     */
    public static String createPromptRuleContent(String description) {
        if (description == null || description.isEmpty()) {
            return PROMPT_PREFIX;
        }
        return PROMPT_PREFIX + " " + description.trim();
    }

    /**
     * Check if classifier permissions are enabled.
     * Default: false (stub implementation)
     */
    public static boolean isClassifierPermissionsEnabled() {
        return false;
    }

    /**
     * Get Bash prompt deny descriptions.
     */
    public static List<String> getBashPromptDenyDescriptions(Object context) {
        return Collections.emptyList();
    }

    /**
     * Get Bash prompt ask descriptions.
     */
    public static List<String> getBashPromptAskDescriptions(Object context) {
        return Collections.emptyList();
    }

    /**
     * Get Bash prompt allow descriptions.
     */
    public static List<String> getBashPromptAllowDescriptions(Object context) {
        return Collections.emptyList();
    }

    /**
     * Classify a Bash command.
     */
    public static ClassifierResult classifyBashCommand(
            String command,
            String cwd,
            List<String> descriptions,
            ClassifierBehavior behavior) {
        // Stub implementation - feature disabled
        return new ClassifierResult(
            false,
            null,
            ClassifierResult.Confidence.HIGH,
            "This feature is disabled"
        );
    }

    /**
     * Generate a generic description for a command.
     */
    public static String generateGenericDescription(
            String command,
            String specificDescription) {
        return specificDescription != null ? specificDescription : null;
    }

    /**
     * Check if a command appears to be a help command.
     */
    public static boolean isHelpCommand(String command) {
        if (command == null) return false;

        String trimmed = command.trim();
        return trimmed.endsWith("--help") || trimmed.endsWith("-h") || trimmed.equals("help");
    }

    /**
     * Check if a command is a read-only operation.
     */
    public static boolean isReadOnlyCommand(String command) {
        if (command == null) return false;

        String trimmed = command.trim().toLowerCase();
        String cmdName = extractCommandName(trimmed);

        // Commands that are generally read-only
        Set<String> readOnlyCommands = Set.of(
            "cat", "less", "more", "head", "tail",
            "ls", "dir", "find", "locate",
            "grep", "egrep", "fgrep", "rg",
            "echo", "printf", "wc",
            "pwd", "whoami", "id", "uname",
            "date", "cal", "uptime",
            "git status", "git log", "git diff", "git show", "git branch",
            "npm list", "npm ls", "npm outdated",
            "which", "type", "whereis",
            "file", "stat"
        );

        return readOnlyCommands.contains(cmdName);
    }

    /**
     * Check if a command modifies the filesystem.
     */
    public static boolean isFilesystemModifyingCommand(String command) {
        if (command == null) return false;

        String trimmed = command.trim().toLowerCase();
        String cmdName = extractCommandName(trimmed);

        return DangerousPatterns.matchesDangerousPattern(cmdName, DangerousPatterns.FILESYSTEM_MODIFY_PATTERNS);
    }

    /**
     * Check if a command performs network operations.
     */
    public static boolean isNetworkCommand(String command) {
        if (command == null) return false;

        String trimmed = command.trim().toLowerCase();
        String cmdName = extractCommandName(trimmed);

        return DangerousPatterns.matchesDangerousPattern(cmdName, DangerousPatterns.NETWORK_PATTERNS);
    }

    /**
     * Assess risk level of a command.
     */
    public static RiskLevel assessRisk(String command) {
        if (command == null) return RiskLevel.LOW;

        String trimmed = command.trim().toLowerCase();

        // Check for dangerous patterns
        if (DangerousPatterns.isDangerousBashCommand(trimmed)) {
            return RiskLevel.HIGH;
        }

        // Check for network operations
        if (isNetworkCommand(trimmed)) {
            return RiskLevel.MEDIUM;
        }

        // Check for filesystem modifications
        if (isFilesystemModifyingCommand(trimmed)) {
            return RiskLevel.MEDIUM;
        }

        // Check for privilege escalation
        if (trimmed.startsWith("sudo ") || trimmed.startsWith("su ")) {
            return RiskLevel.HIGH;
        }

        // Check for command substitution (potential injection)
        if (trimmed.contains("$(") || trimmed.contains("`")) {
            return RiskLevel.MEDIUM;
        }

        // Check for pipes to dangerous commands
        if (trimmed.contains("|") && (
            trimmed.contains("| sh") ||
            trimmed.contains("| bash") ||
            trimmed.contains("| xargs"))) {
            return RiskLevel.HIGH;
        }

        return RiskLevel.LOW;
    }

    /**
     * Risk level enum.
     */
    public enum RiskLevel {
        LOW, MEDIUM, HIGH
    }

    /**
     * Extract the command name from a command string.
     */
    private static String extractCommandName(String command) {
        if (command == null || command.isEmpty()) {
            return "";
        }

        String trimmed = command.trim();
        int spaceIndex = trimmed.indexOf(' ');

        if (spaceIndex > 0) {
            return trimmed.substring(0, spaceIndex);
        }

        return trimmed;
    }
}