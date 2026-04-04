/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code cron jitter config
 */
package com.anthropic.claudecode.utils;

/**
 * Cron jitter configuration for task scheduling.
 */
public final class CronJitterConfig {
    private CronJitterConfig() {}

    /**
     * Cron jitter configuration values.
     */
    public record Config(
            double recurringFrac,
            long recurringCapMs,
            long oneShotMaxMs,
            long oneShotFloorMs,
            int oneShotMinuteMod,
            long recurringMaxAgeMs
    ) {}

    /**
     * Default cron jitter configuration.
     */
    public static final Config DEFAULT_CONFIG = new Config(
            0.1,                        // recurringFrac
            15L * 60 * 1000,           // recurringCapMs: 15 minutes
            90L * 1000,                // oneShotMaxMs: 90 seconds
            0L,                         // oneShotFloorMs
            30,                         // oneShotMinuteMod
            7L * 24 * 60 * 60 * 1000   // recurringMaxAgeMs: 7 days
    );

    private static Config currentConfig = DEFAULT_CONFIG;
    private static long lastRefreshTime = 0;
    private static final long REFRESH_INTERVAL_MS = 60 * 1000; // 1 minute

    /**
     * Get the current jitter configuration.
     * In production, this would fetch from a config service.
     */
    public static Config getConfig() {
        long now = System.currentTimeMillis();
        if (now - lastRefreshTime > REFRESH_INTERVAL_MS) {
            lastRefreshTime = now;
            // Try to load config from environment or config file
            currentConfig = loadConfigFromEnvironment();
        }
        return currentConfig;
    }

    /**
     * Load configuration from environment variables.
     */
    private static Config loadConfigFromEnvironment() {
        try {
            double recurringFrac = getEnvDouble("CLAUDE_CRON_JITTER_RECURRING_FRAC", DEFAULT_CONFIG.recurringFrac());
            long recurringCapMs = getEnvLong("CLAUDE_CRON_JITTER_RECURRING_CAP_MS", DEFAULT_CONFIG.recurringCapMs());
            long oneShotMaxMs = getEnvLong("CLAUDE_CRON_JITTER_ONESHOT_MAX_MS", DEFAULT_CONFIG.oneShotMaxMs());
            long oneShotFloorMs = getEnvLong("CLAUDE_CRON_JITTER_ONESHOT_FLOOR_MS", DEFAULT_CONFIG.oneShotFloorMs());
            int oneShotMinuteMod = getEnvInt("CLAUDE_CRON_JITTER_ONESHOT_MINUTE_MOD", DEFAULT_CONFIG.oneShotMinuteMod());
            long recurringMaxAgeMs = getEnvLong("CLAUDE_CRON_JITTER_RECURRING_MAX_AGE_MS", DEFAULT_CONFIG.recurringMaxAgeMs());

            return new Config(recurringFrac, recurringCapMs, oneShotMaxMs, oneShotFloorMs, oneShotMinuteMod, recurringMaxAgeMs);
        } catch (Exception e) {
            return DEFAULT_CONFIG;
        }
    }

    private static double getEnvDouble(String key, double defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    private static long getEnvLong(String key, long defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    private static int getEnvInt(String key, int defaultValue) {
        String value = System.getenv(key);
        if (value != null && !value.isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {}
        }
        return defaultValue;
    }

    /**
     * Set the jitter configuration (for testing).
     */
    public static void setConfig(Config config) {
        currentConfig = config;
    }

    /**
     * Reset to default configuration.
     */
    public static void reset() {
        currentConfig = DEFAULT_CONFIG;
        lastRefreshTime = 0;
    }
}