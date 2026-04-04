/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/Shell
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Shell utilities - Shell detection and configuration.
 */
public final class ShellUtils {
    private static final String DETECTED_SHELL = detectShell();
    private static final String DETECTED_SHELL_PATH = detectShellPath();

    /**
     * Get the detected shell name.
     */
    public static String getShell() {
        return DETECTED_SHELL;
    }

    /**
     * Get the detected shell path.
     */
    public static String getShellPath() {
        return DETECTED_SHELL_PATH;
    }

    /**
     * Check if running on Windows.
     */
    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /**
     * Check if running on macOS.
     */
    public static boolean isMacOS() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }

    /**
     * Check if running on Linux.
     */
    public static boolean isLinux() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("nix") || os.contains("nux") || os.contains("aix");
    }

    /**
     * Get shell-specific command prefix.
     */
    public static String[] getShellPrefix() {
        if (isWindows()) {
            return new String[]{"cmd", "/c"};
        }
        return new String[]{DETECTED_SHELL, "-c"};
    }

    /**
     * Get shell-specific environment format.
     */
    public static String getEnvFormat(String key, String value) {
        if (isWindows()) {
            return "set " + key + "=" + value;
        }
        return key + "=" + value;
    }

    /**
     * Get shell-specific path separator.
     */
    public static String getPathSeparator() {
        return isWindows() ? ";" : ":";
    }

    /**
     * Get shell-specific line separator.
     */
    public static String getLineSeparator() {
        return isWindows() ? "\r\n" : "\n";
    }

    /**
     * Get shell-specific home directory.
     */
    public static String getHomeDirectory() {
        if (isWindows()) {
            String userProfile = System.getenv("USERPROFILE");
            return userProfile != null ? userProfile : System.getProperty("user.home");
        }
        return System.getProperty("user.home");
    }

    /**
     * Get shell-specific config directory.
     */
    public static String getConfigDirectory() {
        String home = getHomeDirectory();

        if (isWindows()) {
            String appData = System.getenv("APPDATA");
            return appData != null ? appData : home;
        }

        String xdgConfig = System.getenv("XDG_CONFIG_HOME");
        return xdgConfig != null ? xdgConfig : home + "/.config";
    }

    /**
     * Quote a string for the shell.
     */
    public static String quote(String str) {
        if (str == null || str.isEmpty()) {
            return "''";
        }

        if (isWindows()) {
            // Windows: use double quotes
            if (str.contains("\"")) {
                return "\"" + str.replace("\"", "\\\"") + "\"";
            }
            return "\"" + str + "\"";
        }

        // Unix: prefer single quotes, escape if needed
        if (!str.contains("'")) {
            return "'" + str + "'";
        }

        // Use double quotes with escaping
        return "\"" + str.replace("\\", "\\\\")
                         .replace("\"", "\\\"")
                         .replace("$", "\\$")
                         .replace("`", "\\`") + "\"";
    }

    /**
     * Escape a string for the shell.
     */
    public static String escape(String str) {
        if (str == null || str.isEmpty()) {
            return "";
        }

        if (isWindows()) {
            return str.replace("^", "^^")
                     .replace("&", "^&")
                     .replace("|", "^|")
                     .replace("<", "^<")
                     .replace(">", "^>")
                     .replace("(", "^(")
                     .replace(")", "^)");
        }

        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("$", "\\$")
                 .replace("`", "\\`")
                 .replace("!", "\\!");
    }

    /**
     * Build a shell command.
     */
    public static String[] buildCommand(String... args) {
        String[] prefix = getShellPrefix();
        String[] result = new String[prefix.length + args.length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        System.arraycopy(args, 0, result, prefix.length, args.length);
        return result;
    }

    private static String detectShell() {
        String shell = System.getenv("SHELL");

        if (shell != null) {
            if (shell.contains("zsh")) return "zsh";
            if (shell.contains("bash")) return "bash";
            if (shell.contains("fish")) return "fish";
            if (shell.contains("sh")) return "sh";
        }

        if (isWindows()) {
            String comSpec = System.getenv("COMSPEC");
            if (comSpec != null) {
                if (comSpec.contains("powershell")) return "powershell";
                return "cmd";
            }
            return "cmd";
        }

        return "bash";
    }

    private static String detectShellPath() {
        String shell = System.getenv("SHELL");

        if (shell != null) {
            return shell;
        }

        if (isWindows()) {
            String comSpec = System.getenv("COMSPEC");
            return comSpec != null ? comSpec : "cmd.exe";
        }

        // Try common shells
        String[] shells = {"/bin/zsh", "/bin/bash", "/bin/sh", "/usr/bin/zsh", "/usr/bin/bash"};
        for (String s : shells) {
            if (new java.io.File(s).exists()) {
                return s;
            }
        }

        return "/bin/sh";
    }
}