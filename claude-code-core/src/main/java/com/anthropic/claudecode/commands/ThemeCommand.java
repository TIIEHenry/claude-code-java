/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/theme
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Theme command - Change the color theme.
 */
public final class ThemeCommand implements Command {
    @Override
    public String name() {
        return "theme";
    }

    @Override
    public String description() {
        return "Change the color theme";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean supportsNonInteractive() {
        return true;
    }

    private static final Map<String, String> THEMES = Map.of(
        "default", "Default dark theme",
        "light", "Light theme for bright environments",
        "dark", "Dark theme (same as default)",
        "monokai", "Monokai-inspired colors",
        "dracula", "Dracula color scheme",
        "nord", "Nord color palette",
        "gruvbox", "Gruvbox color scheme"
    );

    @Override
    public CompletableFuture<CommandResult> execute(String args, CommandContext context) {
        if (args == null || args.trim().isEmpty()) {
            return CompletableFuture.completedFuture(listThemes(context));
        }

        String action = args.trim().toLowerCase();

        if ("list".equals(action) || "ls".equals(action)) {
            return CompletableFuture.completedFuture(listThemes(context));
        }

        return CompletableFuture.completedFuture(setTheme(context, action));
    }

    private CommandResult listThemes(CommandContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("Available Themes\n");
        sb.append("================\n\n");

        String currentTheme = context.getCurrentTheme() != null ? context.getCurrentTheme() : "default";

        for (Map.Entry<String, String> entry : THEMES.entrySet()) {
            String name = entry.getKey();
            String marker = name.equals(currentTheme) ? "* " : "  ";
            sb.append(marker).append(name).append(" - ").append(entry.getValue()).append("\n");
        }

        sb.append("\nCurrent: ").append(currentTheme).append("\n");
        sb.append("\nUsage: theme <name>\n");

        return CommandResult.success(sb.toString());
    }

    private CommandResult setTheme(CommandContext context, String themeName) {
        if (!THEMES.containsKey(themeName)) {
            return CommandResult.failure("Unknown theme: " + themeName + "\n\nAvailable themes: " + String.join(", ", THEMES.keySet()) + "\n");
        }

        context.setTheme(themeName);
        return CommandResult.success("Theme set to: " + themeName + "\n" + THEMES.get(themeName) + "\n");
    }
}