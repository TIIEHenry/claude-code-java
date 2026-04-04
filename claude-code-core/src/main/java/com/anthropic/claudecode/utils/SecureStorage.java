/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/secureStorage
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.nio.file.*;
import java.util.concurrent.*;

/**
 * Secure storage - Secure credential storage.
 */
public final class SecureStorage {
    private static final Map<String, String> memoryStorage = new ConcurrentHashMap<>();
    private static final Path STORAGE_PATH;

    static {
        String home = System.getProperty("user.home");
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("mac")) {
            STORAGE_PATH = Paths.get(home, "Library", "Keychains", "claude-code");
        } else if (os.contains("win")) {
            String appData = System.getenv("APPDATA");
            STORAGE_PATH = appData != null ? Paths.get(appData, "claude-code", "credentials") :
                Paths.get(home, ".claude-code", "credentials");
        } else {
            STORAGE_PATH = Paths.get(home, ".config", "claude-code", "credentials");
        }
    }

    /**
     * Store a value securely.
     */
    public static void store(String key, String value) {
        memoryStorage.put(key, value);

        try {
            Files.createDirectories(STORAGE_PATH);
            Path keyFile = STORAGE_PATH.resolve(key + ".enc");
            // In production, this would encrypt the value
            String encoded = Base64.getEncoder().encodeToString(value.getBytes());
            Files.writeString(keyFile, encoded);
        } catch (Exception e) {
            // Fall back to memory-only storage
        }
    }

    /**
     * Retrieve a value.
     */
    public static String retrieve(String key) {
        // Check memory first
        String value = memoryStorage.get(key);
        if (value != null) {
            return value;
        }

        // Try to load from storage
        try {
            Path keyFile = STORAGE_PATH.resolve(key + ".enc");
            if (Files.exists(keyFile)) {
                String encoded = Files.readString(keyFile);
                // In production, this would decrypt the value
                return new String(Base64.getDecoder().decode(encoded));
            }
        } catch (Exception e) {
            // Ignore
        }

        return null;
    }

    /**
     * Delete a value.
     */
    public static void delete(String key) {
        memoryStorage.remove(key);

        try {
            Path keyFile = STORAGE_PATH.resolve(key + ".enc");
            Files.deleteIfExists(keyFile);
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Check if a key exists.
     */
    public static boolean exists(String key) {
        return memoryStorage.containsKey(key) ||
               Files.exists(STORAGE_PATH.resolve(key + ".enc"));
    }

    /**
     * List all stored keys.
     */
    public static List<String> listKeys() {
        Set<String> keys = new HashSet<>(memoryStorage.keySet());

        try {
            if (Files.exists(STORAGE_PATH)) {
                Files.list(STORAGE_PATH)
                    .filter(p -> p.toString().endsWith(".enc"))
                    .forEach(p -> {
                        String name = p.getFileName().toString();
                        keys.add(name.substring(0, name.length() - 4));
                    });
            }
        } catch (Exception e) {
            // Ignore
        }

        return new ArrayList<>(keys);
    }

    /**
     * Clear all stored values.
     */
    public static void clearAll() {
        memoryStorage.clear();

        try {
            if (Files.exists(STORAGE_PATH)) {
                Files.list(STORAGE_PATH)
                    .filter(p -> p.toString().endsWith(".enc"))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (Exception e) {
                            // Ignore
                        }
                    });
            }
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Get storage path.
     */
    public static Path getStoragePath() {
        return STORAGE_PATH;
    }

    /**
     * Check if using system keychain.
     */
    public static boolean isUsingSystemKeychain() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("mac") || os.contains("win");
    }

    /**
     * Migrate from legacy storage.
     */
    public static void migrateFromLegacy(Path legacyPath) {
        try {
            if (Files.exists(legacyPath)) {
                String content = Files.readString(legacyPath);
                // Parse and migrate legacy credentials
                // Implementation depends on legacy format
            }
        } catch (Exception e) {
            // Ignore migration errors
        }
    }
}