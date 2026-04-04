/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code XDG Base Directory utilities
 */
package com.anthropic.claudecode.utils;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * XDG Base Directory utilities for Claude CLI Native Installer.
 *
 * Implements the XDG Base Directory specification for organizing
 * native installer components across appropriate system directories.
 *
 * @see https://specifications.freedesktop.org/basedir-spec/latest/
 */
public final class Xdg {
    private Xdg() {}

    /**
     * Get the user home directory.
     */
    private static String getHome() {
        String home = System.getenv("HOME");
        if (home != null && !home.isEmpty()) {
            return home;
        }
        return System.getProperty("user.home", "");
    }

    /**
     * Get XDG state home directory.
     * Default: ~/.local/state
     */
    public static String getXDGStateHome() {
        String xdgStateHome = System.getenv("XDG_STATE_HOME");
        if (xdgStateHome != null && !xdgStateHome.isEmpty()) {
            return xdgStateHome;
        }
        return Paths.get(getHome(), ".local", "state").toString();
    }

    /**
     * Get XDG cache home directory.
     * Default: ~/.cache
     */
    public static String getXDGCacheHome() {
        String xdgCacheHome = System.getenv("XDG_CACHE_HOME");
        if (xdgCacheHome != null && !xdgCacheHome.isEmpty()) {
            return xdgCacheHome;
        }
        return Paths.get(getHome(), ".cache").toString();
    }

    /**
     * Get XDG data home directory.
     * Default: ~/.local/share
     */
    public static String getXDGDataHome() {
        String xdgDataHome = System.getenv("XDG_DATA_HOME");
        if (xdgDataHome != null && !xdgDataHome.isEmpty()) {
            return xdgDataHome;
        }
        return Paths.get(getHome(), ".local", "share").toString();
    }

    /**
     * Get XDG config home directory.
     * Default: ~/.config
     */
    public static String getXDGConfigHome() {
        String xdgConfigHome = System.getenv("XDG_CONFIG_HOME");
        if (xdgConfigHome != null && !xdgConfigHome.isEmpty()) {
            return xdgConfigHome;
        }
        return Paths.get(getHome(), ".config").toString();
    }

    /**
     * Get user bin directory (not technically XDG but follows the convention).
     * Default: ~/.local/bin
     */
    public static String getUserBinDir() {
        return Paths.get(getHome(), ".local", "bin").toString();
    }

    /**
     * Get Claude-specific state directory.
     */
    public static String getClaudeStateHome() {
        return Paths.get(getXDGStateHome(), "claude-code").toString();
    }

    /**
     * Get Claude-specific cache directory.
     */
    public static String getClaudeCacheHome() {
        return Paths.get(getXDGCacheHome(), "claude-code").toString();
    }

    /**
     * Get Claude-specific data directory.
     */
    public static String getClaudeDataHome() {
        return Paths.get(getXDGDataHome(), "claude-code").toString();
    }

    /**
     * Get Claude-specific config directory.
     */
    public static String getClaudeConfigHome() {
        return Paths.get(getXDGConfigHome(), "claude-code").toString();
    }

    /**
     * Get the path for a specific file in XDG state home.
     */
    public static Path getStatePath(String... components) {
        Path base = Paths.get(getClaudeStateHome());
        for (String component : components) {
            base = base.resolve(component);
        }
        return base;
    }

    /**
     * Get the path for a specific file in XDG cache home.
     */
    public static Path getCachePath(String... components) {
        Path base = Paths.get(getClaudeCacheHome());
        for (String component : components) {
            base = base.resolve(component);
        }
        return base;
    }

    /**
     * Get the path for a specific file in XDG data home.
     */
    public static Path getDataPath(String... components) {
        Path base = Paths.get(getClaudeDataHome());
        for (String component : components) {
            base = base.resolve(component);
        }
        return base;
    }

    /**
     * Get the path for a specific file in XDG config home.
     */
    public static Path getConfigPath(String... components) {
        Path base = Paths.get(getClaudeConfigHome());
        for (String component : components) {
            base = base.resolve(component);
        }
        return base;
    }

    /**
     * Resolve XDG directories with custom environment and home.
     * Useful for testing.
     */
    public static XdgConfig resolve(String home, String xdgStateHome, String xdgCacheHome, String xdgDataHome, String xdgConfigHome) {
        return new XdgConfig(
                xdgStateHome != null ? xdgStateHome : Paths.get(home, ".local", "state").toString(),
                xdgCacheHome != null ? xdgCacheHome : Paths.get(home, ".cache").toString(),
                xdgDataHome != null ? xdgDataHome : Paths.get(home, ".local", "share").toString(),
                xdgConfigHome != null ? xdgConfigHome : Paths.get(home, ".config").toString(),
                Paths.get(home, ".local", "bin").toString()
        );
    }

    /**
     * XDG configuration record.
     */
    public record XdgConfig(
            String stateHome,
            String cacheHome,
            String dataHome,
            String configHome,
            String binDir
    ) {}
}