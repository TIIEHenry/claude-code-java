/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/envUtils.ts
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.nio.file.*;

/**
 * Environment utilities - Environment variable helpers.
 */
public final class EnvUtils {
    private EnvUtils() {}

    /**
     * Get Claude config home directory.
     * Memoized: keyed off CLAUDE_CONFIG_DIR so tests get fresh value.
     */
    public static String getClaudeConfigHomeDir() {
        String configDir = System.getenv("CLAUDE_CONFIG_DIR");
        if (configDir != null && !configDir.isEmpty()) {
            return configDir;
        }
        return System.getProperty("user.home") + "/.claude";
    }

    /**
     * Get teams directory.
     */
    public static String getTeamsDir() {
        return getClaudeConfigHomeDir() + "/teams";
    }

    /**
     * Check if running in bare mode.
     * --bare / CLAUDE_CODE_SIMPLE — skip hooks, LSP, plugin sync, etc.
     */
    public static boolean isBareMode() {
        return isEnvTruthy("CLAUDE_CODE_SIMPLE") || isEnvTruthy("CLAUDE_BARE_MODE");
    }

    /**
     * Check if environment variable is truthy.
     */
    public static boolean isEnvTruthy(String name) {
        String value = System.getenv(name);
        return isEnvTruthyValue(value);
    }

    /**
     * Alias for isEnvTruthy.
     */
    public static boolean isTruthy(String name) {
        return isEnvTruthy(name);
    }

    /**
     * Check if user type is "ant" (internal user).
     */
    public static boolean isUserTypeAnt() {
        return "ant".equals(System.getenv("USER_TYPE"));
    }

    /**
     * Check if a value string is truthy.
     */
    public static boolean isEnvTruthyValue(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        String normalized = value.toLowerCase().trim();
        return "1".equals(normalized) ||
               "true".equals(normalized) ||
               "yes".equals(normalized) ||
               "on".equals(normalized);
    }

    /**
     * Check if environment variable is defined and falsy.
     */
    public static boolean isEnvDefinedFalsy(String name) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            return false;
        }
        String normalized = value.toLowerCase().trim();
        return "0".equals(normalized) ||
               "false".equals(normalized) ||
               "no".equals(normalized) ||
               "off".equals(normalized);
    }

    /**
     * Get environment variable.
     */
    public static Optional<String> getEnv(String name) {
        return Optional.ofNullable(System.getenv(name));
    }

    /**
     * Get environment variable with default.
     */
    public static String getEnv(String name, String defaultValue) {
        return getEnv(name).orElse(defaultValue);
    }

    /**
     * Get environment variable as integer.
     */
    public static Optional<Integer> getEnvInt(String name) {
        String value = System.getenv(name);
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    /**
     * Get environment variable as integer with default.
     */
    public static int getEnvInt(String name, int defaultValue) {
        return getEnvInt(name).orElse(defaultValue);
    }

    /**
     * Get environment variable as long.
     */
    public static long getEnvLong(String name, long defaultValue) {
        String value = System.getenv(name);
        if (value != null && !value.isEmpty()) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Parse environment variable strings into key-value map.
     */
    public static Map<String, String> parseEnvVars(List<String> rawEnvArgs) {
        Map<String, String> parsedEnv = new LinkedHashMap<>();
        if (rawEnvArgs != null) {
            for (String envStr : rawEnvArgs) {
                int eqIndex = envStr.indexOf('=');
                if (eqIndex <= 0) {
                    throw new IllegalArgumentException(
                        "Invalid environment variable format: " + envStr +
                        ", expected KEY=value"
                    );
                }
                String key = envStr.substring(0, eqIndex);
                String value = envStr.substring(eqIndex + 1);
                parsedEnv.put(key, value);
            }
        }
        return parsedEnv;
    }

    /**
     * Check if running in CI.
     */
    public static boolean isCI() {
        return isEnvTruthy("CI") || isEnvTruthy("GITHUB_ACTIONS");
    }

    /**
     * Check if running in debug mode.
     */
    public static boolean isDebug() {
        return isEnvTruthy("DEBUG") || isEnvTruthy("CLAUDE_DEBUG");
    }

    /**
     * Get AWS region with fallback to default.
     */
    public static String getAWSRegion() {
        String region = System.getenv("AWS_REGION");
        if (region != null && !region.isEmpty()) {
            return region;
        }
        region = System.getenv("AWS_DEFAULT_REGION");
        if (region != null && !region.isEmpty()) {
            return region;
        }
        return "us-east-1";
    }

    /**
     * Get default Vertex AI region.
     */
    public static String getDefaultVertexRegion() {
        String region = System.getenv("CLOUD_ML_REGION");
        if (region != null && !region.isEmpty()) {
            return region;
        }
        return "us-east5";
    }

    /**
     * Check if bash commands should maintain project working directory.
     */
    public static boolean shouldMaintainProjectWorkingDir() {
        return isEnvTruthy("CLAUDE_BASH_MAINTAIN_PROJECT_WORKING_DIR");
    }

    /**
     * Check if running on Homespace (ant-internal cloud environment).
     */
    public static boolean isRunningOnHomespace() {
        return "ant".equals(System.getenv("USER_TYPE")) &&
               isEnvTruthy("COO_RUNNING_ON_HOMESPACE");
    }

    /**
     * Check if running in protected namespace.
     */
    public static boolean isInProtectedNamespace() {
        return "ant".equals(System.getenv("USER_TYPE"));
    }

    /**
     * Get Vertex AI region for a specific model.
     */
    public static String getVertexRegionForModel(String model) {
        // Model prefix → env var for Vertex region overrides
        String[][] regionOverrides = {
            {"claude-haiku-4-5", "VERTEX_REGION_CLAUDE_HAIKU_4_5"},
            {"claude-3-5-haiku", "VERTEX_REGION_CLAUDE_3_5_HAIKU"},
            {"claude-3-5-sonnet", "VERTEX_REGION_CLAUDE_3_5_SONNET"},
            {"claude-3-7-sonnet", "VERTEX_REGION_CLAUDE_3_7_SONNET"},
            {"claude-opus-4-1", "VERTEX_REGION_CLAUDE_4_1_OPUS"},
            {"claude-opus-4", "VERTEX_REGION_CLAUDE_4_0_OPUS"},
            {"claude-sonnet-4-6", "VERTEX_REGION_CLAUDE_4_6_SONNET"},
            {"claude-sonnet-4-5", "VERTEX_REGION_CLAUDE_4_5_SONNET"},
            {"claude-sonnet-4", "VERTEX_REGION_CLAUDE_4_0_SONNET"},
        };

        if (model != null) {
            for (String[] entry : regionOverrides) {
                String prefix = entry[0];
                String envVar = entry[1];
                if (model.startsWith(prefix)) {
                    String region = System.getenv(envVar);
                    if (region != null && !region.isEmpty()) {
                        return region;
                    }
                    break;
                }
            }
        }
        return getDefaultVertexRegion();
    }

    /**
     * Get the platform.
     */
    public static String getPlatform() {
        return System.getProperty("os.name", "unknown").toLowerCase();
    }

    /**
     * Check if Windows.
     */
    public static boolean isWindows() {
        return getPlatform().contains("win");
    }

    /**
     * Check if macOS.
     */
    public static boolean isMac() {
        return getPlatform().contains("mac");
    }

    /**
     * Check if Linux.
     */
    public static boolean isLinux() {
        return getPlatform().contains("linux");
    }

    /**
     * Check if running under WSL (Windows Subsystem for Linux).
     */
    public static boolean isWSL() {
        if (!isLinux()) {
            return false;
        }
        // Check for WSL indicators
        try {
            Path wslInterop = Paths.get("/proc/sys/fs/binfmt_misc/WSLInterop");
            if (Files.exists(wslInterop)) {
                return true;
            }
            Path versionFile = Paths.get("/proc/version");
            if (Files.exists(versionFile)) {
                String content = Files.readString(versionFile);
                return content.toLowerCase().contains("wsl") ||
                       content.toLowerCase().contains("microsoft");
            }
        } catch (Exception e) {
            // Ignore errors reading files
        }
        // Check environment variable
        return isEnvTruthy("WSL_DISTRO_NAME");
    }

    /**
     * Get terminal type from environment.
     */
    public static String getTerminal() {
        return System.getenv("TERM");
    }
}