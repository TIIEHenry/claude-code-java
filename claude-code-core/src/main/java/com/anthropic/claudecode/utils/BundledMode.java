/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code bundled mode detection
 */
package com.anthropic.claudecode.utils;

/**
 * Bundled mode detection utilities.
 */
public final class BundledMode {
    private BundledMode() {}

    /**
     * Check if running as a bundled application.
     */
    public static boolean isInBundledMode() {
        // Check for bundled mode indicators
        String bundled = System.getenv("CLAUDE_CODE_BUNDLED");
        return "true".equalsIgnoreCase(bundled) || "1".equals(bundled);
    }

    /**
     * Check if running with native image.
     */
    public static boolean isNativeImage() {
        // Check for GraalVM native image
        return System.getProperty("org.graalvm.nativeimage.kind") != null;
    }

    /**
     * Check if running from a JAR.
     */
    public static boolean isRunningFromJar() {
        String protocol = BundledMode.class.getResource("").getProtocol();
        return "jar".equals(protocol);
    }

    /**
     * Get the application executable path.
     */
    public static String getExecutablePath() {
        if (isNativeImage()) {
            return ProcessHandle.current().info().command().orElse("claude");
        }
        return "java -jar claude-code.jar";
    }
}