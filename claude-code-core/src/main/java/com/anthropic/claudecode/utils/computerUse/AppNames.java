/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/computerUse/appNames
 */
package com.anthropic.claudecode.utils.computerUse;

import java.util.*;

/**
 * App names - Application name utilities for computer use.
 */
public final class AppNames {
    private static final Map<String, AppInfo> APP_REGISTRY = new HashMap<>();

    /**
     * App info record.
     */
    public record AppInfo(
        String name,
        String displayName,
        String bundleId,
        String executablePath,
        AppCategory category,
        boolean isSystemApp
    ) {}

    /**
     * App category enum.
     */
    public enum AppCategory {
        BROWSER,
        EDITOR,
        TERMINAL,
        FILE_MANAGER,
        COMMUNICATION,
        DEVELOPMENT,
        MEDIA,
        SYSTEM,
        OTHER
    }

    static {
        // Register common macOS apps
        register(new AppInfo(
            "Safari", "Safari", "com.apple.Safari",
            "/Applications/Safari.app",
            AppCategory.BROWSER, true
        ));

        register(new AppInfo(
            "Chrome", "Google Chrome", "com.google.Chrome",
            "/Applications/Google Chrome.app",
            AppCategory.BROWSER, false
        ));

        register(new AppInfo(
            "Terminal", "Terminal", "com.apple.Terminal",
            "/Applications/Utilities/Terminal.app",
            AppCategory.TERMINAL, true
        ));

        register(new AppInfo(
            "iTerm", "iTerm2", "com.googlecode.iterm2",
            "/Applications/iTerm.app",
            AppCategory.TERMINAL, false
        ));

        register(new AppInfo(
            "Finder", "Finder", "com.apple.finder",
            "/System/Library/CoreServices/Finder.app",
            AppCategory.FILE_MANAGER, true
        ));

        register(new AppInfo(
            "VSCode", "Visual Studio Code", "com.microsoft.VSCode",
            "/Applications/Visual Studio Code.app",
            AppCategory.EDITOR, false
        ));

        register(new AppInfo(
            "IntelliJ", "IntelliJ IDEA", "com.jetbrains.intellij",
            "/Applications/IntelliJ IDEA.app",
            AppCategory.DEVELOPMENT, false
        ));
    }

    /**
     * Register app.
     */
    public static void register(AppInfo info) {
        APP_REGISTRY.put(info.name(), info);
    }

    /**
     * Get app info.
     */
    public static AppInfo getAppInfo(String name) {
        return APP_REGISTRY.get(name);
    }

    /**
     * Find app by bundle ID.
     */
    public static AppInfo findByBundleId(String bundleId) {
        return APP_REGISTRY.values()
            .stream()
            .filter(a -> a.bundleId().equals(bundleId))
            .findFirst()
            .orElse(null);
    }

    /**
     * Get apps by category.
     */
    public static List<AppInfo> getAppsByCategory(AppCategory category) {
        return APP_REGISTRY.values()
            .stream()
            .filter(a -> a.category() == category)
            .toList();
    }

    /**
     * Get all apps.
     */
    public static Map<String, AppInfo> getAllApps() {
        return Collections.unmodifiableMap(APP_REGISTRY);
    }

    /**
     * Check if app is installed.
     */
    public static boolean isInstalled(String name) {
        AppInfo info = APP_REGISTRY.get(name);
        if (info == null) return false;

        // Would check actual file existence
        return true;
    }

    /**
     * Get app window title format.
     */
    public static String getWindowTitleFormat(String appName) {
        return switch (appName) {
            case "Safari" -> "%s — Safari";
            case "Chrome" -> "%s - Google Chrome";
            case "Terminal" -> "%s — Terminal";
            case "Finder" -> "%s";
            case "VSCode" -> "%s - Visual Studio Code";
            default -> appName;
        };
    }
}