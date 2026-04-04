/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/Shell
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;

/**
 * Shell - Shell environment utilities.
 */
public final class Shell {
    private static final String DEFAULT_SHELL = "/bin/sh";
    private static volatile String detectedShell = null;

    /**
     * Get current shell.
     */
    public static String getCurrentShell() {
        if (detectedShell != null) {
            return detectedShell;
        }

        String shell = System.getenv("SHELL");
        if (shell != null && !shell.isEmpty()) {
            detectedShell = shell;
            return shell;
        }

        // Check parent process
        String parentShell = detectParentShell();
        if (parentShell != null) {
            detectedShell = parentShell;
            return parentShell;
        }

        return DEFAULT_SHELL;
    }

    /**
     * Detect parent shell from environment.
     */
    private static String detectParentShell() {
        // macOS Terminal detection
        String term = System.getenv("TERM_PROGRAM");
        if (term != null) {
            switch (term) {
                case "Apple_Terminal":
                case "Terminal.app":
                    return "/bin/zsh";
                case "iTerm.app":
                    return "/bin/zsh";
            }
        }

        // Linux detection
        String termEnv = System.getenv("TERM");
        if (termEnv != null && !termEnv.isEmpty()) {
            // Default to bash for Linux terminals
            return "/bin/bash";
        }

        return null;
    }

    /**
     * Check if shell is zsh.
     */
    public static boolean isZsh() {
        return getCurrentShell().contains("zsh");
    }

    /**
     * Check if shell is bash.
     */
    public static boolean isBash() {
        return getCurrentShell().contains("bash");
    }

    /**
     * Check if shell is fish.
     */
    public static boolean isFish() {
        return getCurrentShell().contains("fish");
    }

    /**
     * Get shell config file path.
     */
    public static String getShellConfigPath() {
        String shell = getCurrentShell();
        String home = System.getProperty("user.home");

        if (shell.contains("zsh")) {
            return home + "/.zshrc";
        } else if (shell.contains("bash")) {
            return home + "/.bashrc";
        } else if (shell.contains("fish")) {
            return home + "/.config/fish/config.fish";
        }

        return home + "/.profile";
    }

    /**
     * Get shell type enum.
     */
    public static ShellType getShellType() {
        String shell = getCurrentShell();
        if (shell.contains("zsh")) return ShellType.ZSH;
        if (shell.contains("bash")) return ShellType.BASH;
        if (shell.contains("fish")) return ShellType.FISH;
        if (shell.contains("sh")) return ShellType.SH;
        return ShellType.UNKNOWN;
    }

    /**
     * Shell type enum.
     */
    public enum ShellType {
        ZSH("zsh"),
        BASH("bash"),
        FISH("fish"),
        SH("sh"),
        UNKNOWN("unknown");

        private final String name;

        ShellType(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }
}