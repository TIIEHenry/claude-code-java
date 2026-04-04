/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/permissions/dangerousPatterns.ts
 */
package com.anthropic.claudecode.utils.permissions;

import java.util.*;

/**
 * Pattern lists for dangerous shell-tool allow-rule prefixes.
 *
 * An allow rule like Bash(python:*) or PowerShell(node:*) lets the model
 * run arbitrary code via that interpreter, bypassing the auto-mode classifier.
 */
public final class DangerousPatterns {
    private DangerousPatterns() {}

    /**
     * Cross-platform code-execution entry points present on both Unix and Windows.
     */
    public static final List<String> CROSS_PLATFORM_CODE_EXEC = List.of(
        // Interpreters
        "python",
        "python3",
        "python2",
        "node",
        "deno",
        "tsx",
        "ruby",
        "perl",
        "php",
        "lua",
        // Package runners
        "npx",
        "bunx",
        "npm run",
        "yarn run",
        "pnpm run",
        "bun run",
        // Shells reachable from both (Git Bash / WSL on Windows, native on Unix)
        "bash",
        "sh",
        // Remote arbitrary-command wrapper (native OpenSSH on Win10+)
        "ssh"
    );

    /**
     * Dangerous Bash command patterns.
     * These patterns indicate commands that could bypass security restrictions.
     */
    public static final List<String> DANGEROUS_BASH_PATTERNS = List.of(
        "python", "python3", "python2",
        "node", "deno", "tsx",
        "ruby", "perl", "php", "lua",
        "npx", "bunx",
        "npm run", "yarn run", "pnpm run", "bun run",
        "bash", "sh",
        "ssh",
        // Unix-specific shells
        "zsh", "fish",
        // Command execution
        "eval", "exec", "env", "xargs",
        // Privilege escalation
        "sudo"
    );

    /**
     * Dangerous PowerShell command patterns.
     */
    public static final List<String> DANGEROUS_POWERSHELL_PATTERNS = List.of(
        "python", "python3", "python2",
        "node", "deno", "tsx",
        "ruby", "perl", "php", "lua",
        "npx", "bunx",
        "npm run", "yarn run", "pnpm run", "bun run",
        "bash", "sh", "ssh",
        // PowerShell-specific
        "pwsh",
        "powershell",
        "iex",  // Invoke-Expression
        "Invoke-Expression",
        "Invoke-Command",
        "Start-Process",
        "cmd",
        "cmd.exe"
    );

    /**
     * Patterns that indicate network operations.
     */
    public static final List<String> NETWORK_PATTERNS = List.of(
        "curl", "wget",
        "nc", "netcat",
        "telnet",
        "ssh", "scp", "rsync",
        "ftp", "sftp",
        "dig", "nslookup", "host"
    );

    /**
     * Patterns that indicate file system modification.
     */
    public static final List<String> FILESYSTEM_MODIFY_PATTERNS = List.of(
        "rm", "rmdir",
        "mv", "cp",
        "chmod", "chown",
        "dd",
        "mkfs",
        "fdisk", "parted",
        "wipefs"
    );

    /**
     * Check if a command matches a dangerous pattern.
     */
    public static boolean matchesDangerousPattern(String command, List<String> patterns) {
        if (command == null || patterns == null) {
            return false;
        }

        String lowerCommand = command.toLowerCase().trim();

        for (String pattern : patterns) {
            String lowerPattern = pattern.toLowerCase();

            // Exact match
            if (lowerCommand.equals(lowerPattern)) {
                return true;
            }

            // Prefix match (command followed by space or end)
            if (lowerCommand.startsWith(lowerPattern + " ") ||
                lowerCommand.startsWith(lowerPattern + ":")) {
                return true;
            }

            // Check if command starts with the pattern
            if (lowerCommand.startsWith(lowerPattern) &&
                (lowerCommand.length() == lowerPattern.length() ||
                 Character.isWhitespace(lowerCommand.charAt(lowerPattern.length())) ||
                 lowerCommand.charAt(lowerPattern.length()) == ':')) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a Bash command is dangerous.
     */
    public static boolean isDangerousBashCommand(String command) {
        return matchesDangerousPattern(command, DANGEROUS_BASH_PATTERNS);
    }

    /**
     * Check if a PowerShell command is dangerous.
     */
    public static boolean isDangerousPowerShellCommand(String command) {
        return matchesDangerousPattern(command, DANGEROUS_POWERSHELL_PATTERNS);
    }

    /**
     * Check if a permission rule is overly broad for Bash.
     */
    public static boolean isOverlyBroadBashPermission(String ruleContent) {
        if (ruleContent == null) return false;

        // Wildcard permissions
        if ("*".equals(ruleContent.trim())) {
            return true;
        }

        // Check for dangerous command patterns
        return isDangerousBashCommand(ruleContent);
    }

    /**
     * Check if a permission rule is overly broad for PowerShell.
     */
    public static boolean isOverlyBroadPowerShellPermission(String ruleContent) {
        if (ruleContent == null) return false;

        // Wildcard permissions
        if ("*".equals(ruleContent.trim())) {
            return true;
        }

        // Check for dangerous command patterns
        return isDangerousPowerShellCommand(ruleContent);
    }
}