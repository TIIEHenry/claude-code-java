/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code IDE type definitions
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.nio.file.Paths;

/**
 * IDE type definitions and detection utilities.
 */
public final class IdeType {
    private IdeType() {}

    /**
     * IDE type enum.
     */
    public enum IdeTypeEnum {
        CURSOR("vscode", "Cursor"),
        WINDSURF("vscode", "Windsurf"),
        VSCODE("vscode", "VS Code"),
        PYCHARM("jetbrains", "PyCharm"),
        INTELLIJ("jetbrains", "IntelliJ IDEA"),
        WEBSTORM("jetbrains", "WebStorm"),
        PHPSTORM("jetbrains", "PhpStorm"),
        RUBYMINE("jetbrains", "RubyMine"),
        CLION("jetbrains", "CLion"),
        GOLAND("jetbrains", "GoLand"),
        RIDER("jetbrains", "Rider"),
        DATAGRIP("jetbrains", "DataGrip"),
        APPCODE("jetbrains", "AppCode"),
        DATASPELL("jetbrains", "DataSpell"),
        AQUA("jetbrains", "Aqua"),
        GATEWAY("jetbrains", "Gateway"),
        FLEET("jetbrains", "Fleet"),
        ANDROIDSTUDIO("jetbrains", "Android Studio");

        private final String ideKind;
        private final String displayName;

        IdeTypeEnum(String ideKind, String displayName) {
            this.ideKind = ideKind;
            this.displayName = displayName;
        }

        public String getIdeKind() {
            return ideKind;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isVSCode() {
            return ideKind.equals("vscode");
        }

        public boolean isJetBrains() {
            return ideKind.equals("jetbrains");
        }
    }

    /**
     * Editor display names mapping.
     */
    private static final Map<String, String> EDITOR_DISPLAY_NAMES = Map.ofEntries(
            Map.entry("code", "VS Code"),
            Map.entry("cursor", "Cursor"),
            Map.entry("windsurf", "Windsurf"),
            Map.entry("antigravity", "Antigravity"),
            Map.entry("vi", "Vim"),
            Map.entry("vim", "Vim"),
            Map.entry("nano", "nano"),
            Map.entry("notepad", "Notepad"),
            Map.entry("emacs", "Emacs"),
            Map.entry("subl", "Sublime Text"),
            Map.entry("atom", "Atom")
    );

    /**
     * Check if IDE is VS Code-based.
     */
    public static boolean isVSCodeIde(IdeTypeEnum ide) {
        return ide != null && ide.isVSCode();
    }

    /**
     * Check if IDE is JetBrains-based.
     */
    public static boolean isJetBrainsIde(IdeTypeEnum ide) {
        return ide != null && ide.isJetBrains();
    }

    /**
     * Convert terminal name to IDE display name.
     */
    public static String toIDEDisplayName(String terminal) {
        if (terminal == null || terminal.isEmpty()) return "IDE";

        // Check if it's a known IDE type
        try {
            IdeTypeEnum ideType = IdeTypeEnum.valueOf(terminal.toUpperCase());
            return ideType.getDisplayName();
        } catch (IllegalArgumentException e) {
            // Not a known IDE type
        }

        // Check editor command names
        String lowerTerminal = terminal.toLowerCase().trim();
        String editorName = EDITOR_DISPLAY_NAMES.get(lowerTerminal);
        if (editorName != null) {
            return editorName;
        }

        // Extract command name from path/arguments
        String command = terminal.split(" ")[0];
        String commandName = command != null ?
                Paths.get(command).getFileName().toString().toLowerCase() : null;
        if (commandName != null) {
            String mappedName = EDITOR_DISPLAY_NAMES.get(commandName);
            if (mappedName != null) {
                return mappedName;
            }
            // Fallback: capitalize the command basename
            return capitalize(commandName);
        }

        // Fallback: capitalize first letter
        return capitalize(terminal);
    }

    /**
     * Get IDE type from terminal environment.
     */
    public static IdeTypeEnum getTerminalIdeType() {
        String terminal = System.getenv("TERM_PROGRAM");
        if (terminal == null) return null;

        try {
            return IdeTypeEnum.valueOf(terminal.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Check if running in supported terminal.
     */
    public static boolean isSupportedTerminal() {
        String terminal = System.getenv("TERM_PROGRAM");
        String forceTerminal = System.getenv("FORCE_CODE_TERMINAL");
        return terminal != null || "true".equals(forceTerminal);
    }

    /**
     * Get connected IDE name from MCP clients.
     */
    public static String getConnectedIdeName(List<String> mcpClientNames) {
        return mcpClientNames.stream()
                .filter(name -> name.equals("ide"))
                .findFirst()
                .orElse(null);
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}