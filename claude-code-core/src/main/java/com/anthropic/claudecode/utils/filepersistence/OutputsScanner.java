/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/filePersistence/outputsScanner.ts
 */
package com.anthropic.claudecode.utils.filepersistence;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

/**
 * Outputs directory scanner for file persistence.
 */
public final class OutputsScanner {
    private OutputsScanner() {}

    /**
     * Get the environment kind from CLAUDE_CODE_ENVIRONMENT_KIND.
     */
    public static FilePersistenceTypes.EnvironmentKind getEnvironmentKind() {
        String kind = System.getenv("CLAUDE_CODE_ENVIRONMENT_KIND");
        if ("byoc".equals(kind)) {
            return FilePersistenceTypes.EnvironmentKind.BYOC;
        } else if ("anthropic_cloud".equals(kind)) {
            return FilePersistenceTypes.EnvironmentKind.ANTHROPIC_CLOUD;
        }
        return null;
    }

    /**
     * Find files that have been modified since the turn started.
     */
    public static List<String> findModifiedFiles(
            FilePersistenceTypes.TurnStartTime turnStartTime,
            String outputsDir) {

        List<String> modifiedFiles = new ArrayList<>();
        Path outputPath = Paths.get(outputsDir);

        if (!Files.exists(outputPath) || !Files.isDirectory(outputPath)) {
            return modifiedFiles;
        }

        try (Stream<Path> paths = Files.walk(outputPath)) {
            paths.filter(Files::isRegularFile)
                .filter(p -> !Files.isSymbolicLink(p))
                .forEach(path -> {
                    try {
                        long mtimeMs = Files.getLastModifiedTime(path).toMillis();
                        if (mtimeMs >= turnStartTime.timestampMs()) {
                            modifiedFiles.add(path.toString());
                        }
                    } catch (IOException e) {
                        // Skip files that can't be read
                    }
                });
        } catch (IOException e) {
            // Return empty list on error
        }

        return modifiedFiles;
    }

    /**
     * Log debug message.
     */
    public static void logDebug(String message) {
        if (System.getenv("CLAUDE_CODE_DEBUG") != null) {
            System.err.println("[file-persistence] " + message);
        }
    }
}