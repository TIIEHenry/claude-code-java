/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code shell detection utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.nio.file.*;

/**
 * Shell detection and configuration utilities.
 */
public final class ShellDetection {
    private ShellDetection() {}

    /**
     * Supported shell types.
     */
    public enum ShellType {
        BASH("bash", ".bashrc", ".bash_profile", ".bash_history"),
        ZSH("zsh", ".zshrc", ".zprofile", ".zsh_history"),
        FISH("fish", ".config/fish/config.fish", null, ".config/fish/fish_history"),
        SH("sh", ".profile", null, ".sh_history"),
        POWERSHELL("powershell", null, null, null),
        CMD("cmd", null, null, null),
        UNKNOWN("unknown", null, null, null);

        private final String name;
        private final String rcFile;
        private final String profileFile;
        private final String historyFile;

        ShellType(String name, String rcFile, String profileFile, String historyFile) {
            this.name = name;
            this.rcFile = rcFile;
            this.profileFile = profileFile;
            this.historyFile = historyFile;
        }

        public String getName() { return name; }
        public String getRcFile() { return rcFile; }
        public String getProfileFile() { return profileFile; }
        public String getHistoryFile() { return historyFile; }
    }

    /**
     * Detect current shell type.
     */
    public static ShellType detectShell() {
        String shellPath = System.getenv("SHELL");
        if (shellPath == null || shellPath.isEmpty()) {
            // Try to detect on Windows
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                String psModule = System.getenv("PSModulePath");
                if (psModule != null && !psModule.isEmpty()) {
                    return ShellType.POWERSHELL;
                }
                return ShellType.CMD;
            }
            return ShellType.UNKNOWN;
        }

        String shellName = Paths.get(shellPath).getFileName().toString();
        return fromShellName(shellName);
    }

    /**
     * Get shell type from shell name.
     */
    public static ShellType fromShellName(String shellName) {
        if (shellName == null) return ShellType.UNKNOWN;

        String lower = shellName.toLowerCase();
        if (lower.contains("zsh")) return ShellType.ZSH;
        if (lower.contains("bash")) return ShellType.BASH;
        if (lower.contains("fish")) return ShellType.FISH;
        if (lower.contains("sh")) return ShellType.SH;
        if (lower.contains("powershell") || lower.contains("pwsh")) return ShellType.POWERSHELL;
        if (lower.contains("cmd")) return ShellType.CMD;

        return ShellType.UNKNOWN;
    }

    /**
     * Get current shell path.
     */
    public static String getShellPath() {
        return System.getenv("SHELL");
    }

    /**
     * Get shell RC file path.
     */
    public static Optional<Path> getShellRcFile() {
        ShellType shell = detectShell();
        if (shell.getRcFile() == null) {
            return Optional.empty();
        }

        String home = System.getProperty("user.home");
        return Optional.of(Paths.get(home, shell.getRcFile()));
    }

    /**
     * Get shell profile file path.
     */
    public static Optional<Path> getShellProfileFile() {
        ShellType shell = detectShell();
        if (shell.getProfileFile() == null) {
            return Optional.empty();
        }

        String home = System.getProperty("user.home");
        return Optional.of(Paths.get(home, shell.getProfileFile()));
    }

    /**
     * Get shell history file path.
     */
    public static Optional<Path> getShellHistoryFile() {
        ShellType shell = detectShell();
        if (shell.getHistoryFile() == null) {
            return Optional.empty();
        }

        String home = System.getProperty("user.home");
        return Optional.of(Paths.get(home, shell.getHistoryFile()));
    }

    /**
     * Get shell config directory.
     */
    public static Optional<Path> getShellConfigDir() {
        ShellType shell = detectShell();
        String home = System.getProperty("user.home");

        switch (shell) {
            case ZSH:
                return Optional.of(Paths.get(home, ".zsh"));
            case BASH:
                return Optional.of(Paths.get(home));
            case FISH:
                return Optional.of(Paths.get(home, ".config", "fish"));
            default:
                return Optional.empty();
        }
    }

    /**
     * Check if current shell is POSIX-compatible.
     */
    public static boolean isPosixShell() {
        ShellType shell = detectShell();
        return shell == ShellType.BASH ||
               shell == ShellType.ZSH ||
               shell == ShellType.SH;
    }

    /**
     * Check if current shell supports hooking.
     */
    public static boolean supportsHooking() {
        ShellType shell = detectShell();
        return shell == ShellType.BASH ||
               shell == ShellType.ZSH ||
               shell == ShellType.FISH;
    }

    /**
     * Get shell prompt variable name.
     */
    public static String getPromptVariable() {
        ShellType shell = detectShell();
        switch (shell) {
            case BASH:
                return "PS1";
            case ZSH:
                return "PROMPT"; // or PROMPT2, PROMPT3, PROMPT4
            case FISH:
                return "fish_prompt"; // function name
            default:
                return "PS1";
        }
    }

    /**
     * Get shell initialization command.
     */
    public static String getInitCommand() {
        ShellType shell = detectShell();
        switch (shell) {
            case BASH:
                return "source ~/.bashrc";
            case ZSH:
                return "source ~/.zshrc";
            case FISH:
                return "source ~/.config/fish/config.fish";
            case SH:
                return "source ~/.profile";
            default:
                return "";
        }
    }

    /**
     * Get shell exec prefix.
     */
    public static String getExecPrefix() {
        ShellType shell = detectShell();
        switch (shell) {
            case BASH:
                return "bash -c";
            case ZSH:
                return "zsh -c";
            case FISH:
                return "fish -c";
            case SH:
                return "sh -c";
            default:
                return "sh -c";
        }
    }

    /**
     * Build a command for the current shell.
     */
    public static String buildShellCommand(String command) {
        return getExecPrefix() + " '" + escapeForShell(command) + "'";
    }

    /**
     * Escape a string for shell execution.
     */
    public static String escapeForShell(String str) {
        if (str == null) return "";
        // Replace single quotes with '\'' and wrap in single quotes
        return str.replace("'", "'\\''");
    }

    /**
     * Get environment variable set command for current shell.
     */
    public static String getEnvSetCommand(String key, String value) {
        ShellType shell = detectShell();
        String escapedValue = escapeForShell(value);

        switch (shell) {
            case FISH:
                return "set -x " + key + " '" + escapedValue + "'";
            default:
                return "export " + key + "='" + escapedValue + "'";
        }
    }

    /**
     * Get alias set command for current shell.
     */
    public static String getAliasSetCommand(String name, String command) {
        ShellType shell = detectShell();
        String escapedCommand = escapeForShell(command);

        switch (shell) {
            case FISH:
                return "function " + name + "; " + escapedCommand + "; end";
            default:
                return "alias " + name + "='" + escapedCommand + "'";
        }
    }

    /**
     * Get path append command for current shell.
     */
    public static String getPathAppendCommand(String pathEntry) {
        ShellType shell = detectShell();
        String escapedPath = escapeForShell(pathEntry);

        switch (shell) {
            case FISH:
                return "set -x PATH $PATH '" + escapedPath + "'";
            default:
                return "export PATH=\"$PATH:'" + escapedPath + "'\"";
        }
    }

    /**
     * Get shell completions directory.
     */
    public static Optional<Path> getCompletionsDir() {
        ShellType shell = detectShell();
        String home = System.getProperty("user.home");

        switch (shell) {
            case BASH:
                return Optional.of(Paths.get(home, ".bash_completion"));
            case ZSH:
                // Check for Oh My Zsh or custom completions
                Path ohMyZsh = Paths.get(home, ".oh-my-zsh", "completions");
                if (ohMyZsh.toFile().exists()) {
                    return Optional.of(ohMyZsh);
                }
                Path zshCompletions = Paths.get(home, ".zsh", "completions");
                return Optional.of(zshCompletions);
            case FISH:
                return Optional.of(Paths.get(home, ".config", "fish", "completions"));
            default:
                return Optional.empty();
        }
    }
}