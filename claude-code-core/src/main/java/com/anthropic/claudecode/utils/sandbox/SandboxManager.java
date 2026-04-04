/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/sandbox/sandbox-adapter.ts
 */
package com.anthropic.claudecode.utils.sandbox;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Sandbox manager for managing sandboxed command execution.
 *
 * This provides a bridge between sandbox configuration and actual execution.
 */
public final class SandboxManager {
    private SandboxManager() {}

    private static volatile boolean sandboxingEnabled = false;
    private static volatile boolean autoAllowBashIfSandboxed = false;
    private static volatile SandboxConfig config = SandboxConfig.createDefault();

    // Allowed and denied lists
    private static volatile Set<String> allowedDomains = ConcurrentHashMap.newKeySet();
    private static volatile Set<String> deniedDomains = ConcurrentHashMap.newKeySet();
    private static volatile Set<String> allowedReadPaths = ConcurrentHashMap.newKeySet();
    private static volatile Set<String> deniedReadPaths = ConcurrentHashMap.newKeySet();
    private static volatile Set<String> allowedWritePaths = ConcurrentHashMap.newKeySet();
    private static volatile Set<String> deniedWritePaths = ConcurrentHashMap.newKeySet();

    /**
     * Sandbox configuration.
     */
    public record SandboxConfig(
        boolean enabled,
        boolean autoAllowBash,
        List<String> excludedCommands,
        List<String> allowedDomains,
        List<String> deniedDomains,
        List<String> allowedReadPaths,
        List<String> deniedReadPaths,
        List<String> allowedWritePaths,
        List<String> deniedWritePaths,
        boolean allowManagedDomainsOnly,
        boolean allowManagedReadPathsOnly
    ) {
        public static SandboxConfig createDefault() {
            return new SandboxConfig(
                false,
                true,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                false,
                false
            );
        }
    }

    /**
     * Initialize sandbox from settings.
     */
    public static void initialize(SandboxConfig newConfig) {
        config = newConfig != null ? newConfig : SandboxConfig.createDefault();
        sandboxingEnabled = config.enabled();
        autoAllowBashIfSandboxed = config.autoAllowBash();

        allowedDomains.clear();
        if (config.allowedDomains() != null) {
            allowedDomains.addAll(config.allowedDomains());
        }

        deniedDomains.clear();
        if (config.deniedDomains() != null) {
            deniedDomains.addAll(config.deniedDomains());
        }

        allowedReadPaths.clear();
        if (config.allowedReadPaths() != null) {
            allowedReadPaths.addAll(config.allowedReadPaths());
        }

        deniedReadPaths.clear();
        if (config.deniedReadPaths() != null) {
            deniedReadPaths.addAll(config.deniedReadPaths());
        }

        allowedWritePaths.clear();
        if (config.allowedWritePaths() != null) {
            allowedWritePaths.addAll(config.allowedWritePaths());
        }

        deniedWritePaths.clear();
        if (config.deniedWritePaths() != null) {
            deniedWritePaths.addAll(config.deniedWritePaths());
        }
    }

    /**
     * Check if sandboxing is enabled.
     */
    public static boolean isSandboxingEnabled() {
        return sandboxingEnabled;
    }

    /**
     * Set sandboxing enabled state.
     */
    public static void setSandboxingEnabled(boolean enabled) {
        sandboxingEnabled = enabled;
    }

    /**
     * Check if auto-allow Bash if sandboxed is enabled.
     */
    public static boolean isAutoAllowBashIfSandboxedEnabled() {
        return autoAllowBashIfSandboxed;
    }

    /**
     * Get current sandbox config.
     */
    public static SandboxConfig getConfig() {
        return config;
    }

    /**
     * Check if a domain is allowed.
     */
    public static boolean isDomainAllowed(String domain) {
        if (domain == null) return false;

        // Check deny list first
        if (deniedDomains.contains(domain)) return false;

        // If allow list is empty, all non-denied are allowed
        if (allowedDomains.isEmpty()) return true;

        return allowedDomains.contains(domain);
    }

    /**
     * Check if a path is allowed for reading.
     */
    public static boolean isReadAllowed(String path) {
        if (path == null) return false;

        String normalized = normalizePath(path);

        // Check deny list first
        for (String denied : deniedReadPaths) {
            if (matchesPathPattern(normalized, denied)) {
                return false;
            }
        }

        // If allow list is empty, all non-denied are allowed
        if (allowedReadPaths.isEmpty()) return true;

        for (String allowed : allowedReadPaths) {
            if (matchesPathPattern(normalized, allowed)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a path is allowed for writing.
     */
    public static boolean isWriteAllowed(String path) {
        if (path == null) return false;

        String normalized = normalizePath(path);

        // Check deny list first
        for (String denied : deniedWritePaths) {
            if (matchesPathPattern(normalized, denied)) {
                return false;
            }
        }

        // If allow list is empty, all non-denied are allowed
        if (allowedWritePaths.isEmpty()) return true;

        for (String allowed : allowedWritePaths) {
            if (matchesPathPattern(normalized, allowed)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a command should be excluded from sandboxing.
     */
    public static boolean isCommandExcluded(String command) {
        if (command == null || config.excludedCommands() == null) {
            return false;
        }

        String cmdName = extractCommandName(command);
        return config.excludedCommands().contains(cmdName);
    }

    /**
     * Check if sandbox should be used for a command.
     */
    public static boolean shouldUseSandbox(Map<String, Object> input) {
        if (!sandboxingEnabled) return false;

        Object command = input.get("command");
        if (command == null) return false;

        String cmdStr = command.toString();

        // Check if command is excluded
        if (isCommandExcluded(cmdStr)) return false;

        // Check if explicitly disabled
        Object disableSandbox = input.get("dangerouslyDisableSandbox");
        if (disableSandbox != null && Boolean.TRUE.equals(disableSandbox)) {
            return false;
        }

        return true;
    }

    /**
     * Normalize a path for comparison.
     */
    private static String normalizePath(String path) {
        if (path == null) return "";

        // Expand home directory
        if (path.startsWith("~")) {
            String home = System.getProperty("user.home");
            path = home + path.substring(1);
        }

        // Normalize separators
        path = path.replace('\\', '/');

        return path;
    }

    /**
     * Check if a path matches a pattern (supports * and ** wildcards).
     */
    private static boolean matchesPathPattern(String path, String pattern) {
        if (pattern == null || path == null) return false;

        String normalizedPattern = normalizePath(pattern);

        // Exact match
        if (normalizedPattern.equals(path)) return true;

        // Wildcard patterns
        if (normalizedPattern.endsWith("/**")) {
            String prefix = normalizedPattern.substring(0, normalizedPattern.length() - 3);
            return path.startsWith(prefix);
        }

        if (normalizedPattern.endsWith("/*")) {
            String prefix = normalizedPattern.substring(0, normalizedPattern.length() - 2);
            if (!path.startsWith(prefix)) return false;
            String remainder = path.substring(prefix.length());
            return !remainder.contains("/");
        }

        return false;
    }

    /**
     * Extract command name from a command string.
     */
    private static String extractCommandName(String command) {
        if (command == null) return "";

        String trimmed = command.trim();

        // Find first space or end
        int spaceIdx = trimmed.indexOf(' ');
        if (spaceIdx > 0) {
            return trimmed.substring(0, spaceIdx);
        }

        return trimmed;
    }

    /**
     * Add an allowed domain.
     */
    public static void addAllowedDomain(String domain) {
        if (domain != null) {
            allowedDomains.add(domain);
        }
    }

    /**
     * Add a denied domain.
     */
    public static void addDeniedDomain(String domain) {
        if (domain != null) {
            deniedDomains.add(domain);
        }
    }

    /**
     * Add an allowed read path.
     */
    public static void addAllowedReadPath(String path) {
        if (path != null) {
            allowedReadPaths.add(normalizePath(path));
        }
    }

    /**
     * Add an allowed write path.
     */
    public static void addAllowedWritePath(String path) {
        if (path != null) {
            allowedWritePaths.add(normalizePath(path));
        }
    }

    /**
     * Reset all sandbox state.
     */
    public static void reset() {
        config = SandboxConfig.createDefault();
        sandboxingEnabled = false;
        autoAllowBashIfSandboxed = true;
        allowedDomains.clear();
        deniedDomains.clear();
        allowedReadPaths.clear();
        deniedReadPaths.clear();
        allowedWritePaths.clear();
        deniedWritePaths.clear();
    }
}