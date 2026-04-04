/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/shell/shellToolUtils
 */
package com.anthropic.claudecode.utils.shell;

/**
 * Shell tool utilities - Runtime gates for shell tools.
 */
public final class ShellToolUtils {
    public static final String BASH_TOOL_NAME = "Bash";
    public static final String POWERSHELL_TOOL_NAME = "PowerShell";

    /**
     * Get shell tool names.
     */
    public static String[] getShellToolNames() {
        return new String[] { BASH_TOOL_NAME, POWERSHELL_TOOL_NAME };
    }

    /**
     * Check if PowerShell tool is enabled.
     * Windows-only (the permission engine uses Win32-specific path normalizations).
     * Ant defaults on (opt-out via env=0);
     * External defaults off (opt-in via env=1).
     */
    public static boolean isPowerShellToolEnabled() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (!os.contains("win")) {
            return false;
        }

        String userType = System.getenv("USER_TYPE");
        String usePowerShell = System.getenv("CLAUDE_CODE_USE_POWERSHELL_TOOL");

        if ("ant".equals(userType)) {
            // Ant defaults on, opt-out with env=0
            return !("0".equals(usePowerShell) || "false".equalsIgnoreCase(usePowerShell));
        } else {
            // External defaults off, opt-in with env=1
            return "1".equals(usePowerShell) || "true".equalsIgnoreCase(usePowerShell);
        }
    }

    /**
     * Check if Bash tool is enabled.
     */
    public static boolean isBashToolEnabled() {
        // Bash is always enabled
        return true;
    }

    /**
     * Get enabled shell tools.
     */
    public static String[] getEnabledShellTools() {
        if (isPowerShellToolEnabled()) {
            return getShellToolNames();
        }
        return new String[] { BASH_TOOL_NAME };
    }

    /**
     * Check if tool name is a shell tool.
     */
    public static boolean isShellTool(String toolName) {
        return BASH_TOOL_NAME.equals(toolName) || POWERSHELL_TOOL_NAME.equals(toolName);
    }
}