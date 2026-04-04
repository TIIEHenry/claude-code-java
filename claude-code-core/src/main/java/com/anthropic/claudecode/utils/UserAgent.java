/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code user agent utilities
 */
package com.anthropic.claudecode.utils;

/**
 * User-Agent string helpers.
 *
 * Kept dependency-free for SDK-bundled code.
 */
public final class UserAgent {
    private UserAgent() {}

    /**
     * Version constant - should be set during build.
     */
    private static String VERSION = "1.0.0";

    /**
     * Set the version (typically called during initialization).
     */
    public static void setVersion(String version) {
        VERSION = version;
    }

    /**
     * Get the current version.
     */
    public static String getVersion() {
        return VERSION;
    }

    /**
     * Get the Claude Code user agent string.
     */
    public static String getClaudeCodeUserAgent() {
        return "claude-code/" + VERSION;
    }

    /**
     * Get user agent with platform info.
     */
    public static String getUserAgentWithPlatform() {
        String osName = System.getProperty("os.name", "unknown");
        String osVersion = System.getProperty("os.version", "");
        String javaVersion = System.getProperty("java.version", "");

        return String.format("claude-code/%s (%s %s; Java %s)",
                VERSION, osName, osVersion, javaVersion);
    }

    /**
     * Get user agent for API requests.
     */
    public static String getAPIUserAgent() {
        return getClaudeCodeUserAgent();
    }

    /**
     * Get user agent for HTTP requests with additional info.
     */
    public static String getHttpUserAgent() {
        return getUserAgentWithPlatform();
    }

    /**
     * Build custom user agent string.
     */
    public static String buildUserAgent(String... components) {
        StringBuilder sb = new StringBuilder("claude-code/");
        sb.append(VERSION);

        for (String component : components) {
            if (component != null && !component.isEmpty()) {
                sb.append(" ").append(component);
            }
        }

        return sb.toString();
    }

    /**
     * Parse version from user agent string.
     */
    public static String parseVersion(String userAgent) {
        if (userAgent == null || userAgent.isEmpty()) {
            return null;
        }

        if (userAgent.startsWith("claude-code/")) {
            int slashIndex = userAgent.indexOf('/');
            int spaceIndex = userAgent.indexOf(' ', slashIndex);
            if (spaceIndex > slashIndex) {
                return userAgent.substring(slashIndex + 1, spaceIndex);
            }
            return userAgent.substring(slashIndex + 1);
        }

        return null;
    }

    /**
     * Check if user agent is Claude Code.
     */
    public static boolean isClaudeCode(String userAgent) {
        return userAgent != null && userAgent.startsWith("claude-code/");
    }

    /**
     * Get user agent for CLI.
     */
    public static String getCliUserAgent() {
        String os = Platform.getOsName();
        String arch = System.getProperty("os.arch", "unknown");
        return String.format("claude-code/%s (%s; %s)", VERSION, os, arch);
    }
}