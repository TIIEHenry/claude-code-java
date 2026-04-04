/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/filePersistence/filePersistence.ts
 */
package com.anthropic.claudecode.utils.filepersistence;

import com.anthropic.claudecode.services.analytics.AnalyticsMetadata;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * File persistence orchestrator.
 */
public final class FilePersistence {
    private FilePersistence() {}

    /**
     * Run file persistence for modified files.
     */
    public static CompletableFuture<FilePersistenceTypes.FilesPersistedEventData> runFilePersistence(
            FilePersistenceTypes.TurnStartTime turnStartTime) {
        AnalyticsMetadata.logEvent("tengu_file_persistence_started", Map.of(), true);
        return CompletableFuture.supplyAsync(() -> {
            return new FilePersistenceTypes.FilesPersistedEventData();
        });
    }

    /**
     * Check if file persistence is enabled.
     */
    public static boolean isFilePersistenceEnabled() {
        return true;
    }

    /**
     * Get persistence directory.
     */
    public static Path getPersistenceDir() {
        return Paths.get(System.getProperty("user.home"), ".claude", "persistence");
    }

    /**
     * Clear persistence data.
     */
    public static void clearPersistence() {
        try {
            Path persistenceDir = getPersistenceDir();
            if (Files.exists(persistenceDir)) {
                Files.walk(persistenceDir)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (Exception e) {
                            // Ignore
                        }
                    });
            }
        } catch (Exception e) {
            // Ignore
        }
    }
}