/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/plans.ts
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Plans utility - manages plan files for sessions.
 */
public final class Plans {
    private Plans() {}

    private static final int MAX_SLUG_RETRIES = 10;
    private static final ConcurrentHashMap<String, String> planSlugCache = new ConcurrentHashMap<>();
    private static volatile String plansDirectory = null;

    /**
     * Get or generate a plan slug for a session.
     */
    public static String getPlanSlug(String sessionId) {
        if (sessionId == null) {
            sessionId = UUID.randomUUID().toString();
        }

        return planSlugCache.computeIfAbsent(sessionId, id -> {
            String plansDir = getPlansDirectory();
            for (int i = 0; i < MAX_SLUG_RETRIES; i++) {
                String slug = generateWordSlug();
                Path filePath = Paths.get(plansDir, slug + ".md");
                if (!Files.exists(filePath)) {
                    return slug;
                }
            }
            // Fallback to UUID if all retries fail
            return UUID.randomUUID().toString().substring(0, 8);
        });
    }

    /**
     * Set a specific plan slug for a session.
     */
    public static void setPlanSlug(String sessionId, String slug) {
        planSlugCache.put(sessionId, slug);
    }

    /**
     * Clear the plan slug for a session.
     */
    public static void clearPlanSlug(String sessionId) {
        planSlugCache.remove(sessionId);
    }

    /**
     * Clear all plan slugs.
     */
    public static void clearAllPlanSlugs() {
        planSlugCache.clear();
    }

    /**
     * Get the plans directory path.
     */
    public static String getPlansDirectory() {
        if (plansDirectory == null) {
            synchronized (Plans.class) {
                if (plansDirectory == null) {
                    String homeDir = System.getProperty("user.home");
                    String configDir = System.getenv("CLAUDE_CONFIG_DIR");

                    if (configDir != null && !configDir.isEmpty()) {
                        plansDirectory = Paths.get(configDir, "plans").toString();
                    } else {
                        plansDirectory = Paths.get(homeDir, ".claude", "plans").toString();
                    }

                    // Ensure directory exists
                    try {
                        Files.createDirectories(Paths.get(plansDirectory));
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }
        }
        return plansDirectory;
    }

    /**
     * Set plans directory override.
     */
    public static void setPlansDirectory(String directory) {
        plansDirectory = directory;
    }

    /**
     * Get plan file path for a session.
     */
    public static String getPlanFilePath(String sessionId) {
        return getPlanFilePath(sessionId, null);
    }

    /**
     * Get plan file path for a session or agent.
     */
    public static String getPlanFilePath(String sessionId, String agentId) {
        String slug = getPlanSlug(sessionId);
        String filename;

        if (agentId == null || agentId.isEmpty()) {
            filename = slug + ".md";
        } else {
            filename = slug + "-agent-" + agentId + ".md";
        }

        return Paths.get(getPlansDirectory(), filename).toString();
    }

    /**
     * Get plan content for a session.
     */
    public static String getPlan(String sessionId) {
        return getPlan(sessionId, null);
    }

    /**
     * Get plan content for a session or agent.
     */
    public static String getPlan(String sessionId, String agentId) {
        String filePath = getPlanFilePath(sessionId, agentId);
        try {
            return Files.readString(Paths.get(filePath));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Save plan content for a session.
     */
    public static void savePlan(String sessionId, String content) {
        savePlan(sessionId, null, content);
    }

    /**
     * Save plan content for a session or agent.
     */
    public static void savePlan(String sessionId, String agentId, String content) {
        String filePath = getPlanFilePath(sessionId, agentId);
        try {
            Files.writeString(Paths.get(filePath), content != null ? content : "");
        } catch (IOException e) {
            // Ignore
        }
    }

    /**
     * Delete plan file for a session.
     */
    public static boolean deletePlan(String sessionId) {
        return deletePlan(sessionId, null);
    }

    /**
     * Delete plan file for a session or agent.
     */
    public static boolean deletePlan(String sessionId, String agentId) {
        String filePath = getPlanFilePath(sessionId, agentId);
        try {
            return Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Check if plan exists for a session.
     */
    public static boolean hasPlan(String sessionId) {
        return hasPlan(sessionId, null);
    }

    /**
     * Check if plan exists for a session or agent.
     */
    public static boolean hasPlan(String sessionId, String agentId) {
        String filePath = getPlanFilePath(sessionId, agentId);
        return Files.exists(Paths.get(filePath));
    }

    /**
     * Generate a random word slug.
     */
    private static String generateWordSlug() {
        String[] adjectives = {
            "quick", "slow", "bright", "dark", "calm", "wild", "fresh", "cold",
            "warm", "light", "swift", "bold", "keen", "sharp", "smart", "clear",
            "brave", "calm", "eager", "fair", "grand", "happy", "jolly", "kind"
        };

        String[] nouns = {
            "river", "mountain", "forest", "ocean", "valley", "meadow", "stream",
            "wind", "storm", "cloud", "star", "moon", "sun", "sky", "field",
            "garden", "path", "road", "bridge", "tower", "castle", "village"
        };

        Random random = new Random();
        String adj = adjectives[random.nextInt(adjectives.length)];
        String noun = nouns[random.nextInt(nouns.length)];
        int num = random.nextInt(1000);

        return adj + "-" + noun + "-" + num;
    }
}