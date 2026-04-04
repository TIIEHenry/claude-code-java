/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code auth portable utilities
 */
package com.anthropic.claudecode.utils;

/**
 * Portable auth utilities.
 */
public final class AuthPortable {
    private AuthPortable() {}

    /**
     * Normalize API key for config display (show last 20 chars).
     */
    public static String normalizeApiKeyForConfig(String apiKey) {
        if (apiKey == null || apiKey.length() <= 20) {
            return apiKey;
        }
        return apiKey.substring(apiKey.length() - 20);
    }

    /**
     * Maybe remove API key from macOS keychain.
     * On non-macOS platforms, this is a no-op.
     */
    public static void maybeRemoveApiKeyFromMacOSKeychain() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (!osName.contains("mac")) {
            return;
        }

        try {
            String serviceName = getMacOsKeychainStorageServiceName();
            ProcessBuilder pb = new ProcessBuilder(
                    "security", "delete-generic-password",
                    "-a", System.getenv("USER"),
                    "-s", serviceName
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new RuntimeException("Failed to delete keychain entry");
            }
        } catch (Exception e) {
            Debug.log("Failed to remove API key from macOS keychain: " + e.getMessage());
        }
    }

    /**
     * Get macOS keychain storage service name.
     */
    private static String getMacOsKeychainStorageServiceName() {
        return "Claude Code API Key";
    }
}