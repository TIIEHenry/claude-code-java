/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code constants/keys.ts
 */
package com.anthropic.claudecode.constants;

/**
 * API key constants for GrowthBook and other services.
 */
public final class Keys {
    private Keys() {}

    // GrowthBook client keys
    private static final String GROWTHBOOK_KEY_ANT_PROD = "sdk-xRVcrliHIlrg4og4";
    private static final String GROWTHBOOK_KEY_ANT_DEV = "sdk-yZQvlplybuXjYh6L";
    private static final String GROWTHBOOK_KEY_EXTERNAL = "sdk-zAZezfDKGoZuXXKe";

    /**
     * Get GrowthBook client key based on user type and environment.
     */
    public static String getGrowthBookClientKey(String userType, boolean enableDev) {
        if ("ant".equals(userType)) {
            return enableDev ? GROWTHBOOK_KEY_ANT_DEV : GROWTHBOOK_KEY_ANT_PROD;
        }
        return GROWTHBOOK_KEY_EXTERNAL;
    }

    /**
     * Get GrowthBook client key based on current environment.
     */
    public static String getGrowthBookClientKey() {
        String userType = System.getenv("USER_TYPE");
        boolean enableDev = isEnvTruthy(System.getenv("CLAUDE_CODE_GB_DEV"));
        return getGrowthBookClientKey(userType, enableDev);
    }

    private static boolean isEnvTruthy(String value) {
        return value != null && ("1".equals(value) || "true".equalsIgnoreCase(value));
    }
}