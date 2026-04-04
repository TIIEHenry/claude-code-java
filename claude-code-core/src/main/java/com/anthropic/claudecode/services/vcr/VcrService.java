/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/vcr
 */
package com.anthropic.claudecode.services.vcr;

import java.time.Instant;

import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * VCR service - Video Cassette Recorder for API fixtures.
 *
 * Records and replays API responses for testing.
 */
public final class VcrService {
    private volatile boolean enabled = false;
    private volatile boolean recordMode = false;
    private Path fixturesRoot;

    /**
     * Create VCR service.
     */
    public VcrService() {
        this.fixturesRoot = Paths.get(System.getProperty("user.dir"), "fixtures");
    }

    /**
     * Check if VCR should be used.
     */
    public boolean shouldUseVCR() {
        String nodeEnv = System.getenv("NODE_ENV");
        String userType = System.getenv("USER_TYPE");
        String forceVcr = System.getenv("FORCE_VCR");

        if ("test".equals(nodeEnv)) {
            return true;
        }

        if ("ant".equals(userType) && "true".equalsIgnoreCase(forceVcr)) {
            return true;
        }

        return enabled;
    }

    /**
     * Enable VCR.
     */
    public void enable(boolean record) {
        this.enabled = true;
        this.recordMode = record;
    }

    /**
     * Disable VCR.
     */
    public void disable() {
        this.enabled = false;
        this.recordMode = false;
    }

    /**
     * Set fixtures root.
     */
    public void setFixturesRoot(Path path) {
        this.fixturesRoot = path;
    }

    /**
     * Execute with fixture.
     */
    public <T> CompletableFuture<T> withFixture(Object input, String fixtureName, Supplier<CompletableFuture<T>> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            if (!shouldUseVCR()) {
                return supplier.get().join();
            }

            // Create hash of input for fixture filename
            String hash = createHash(input);
            Path fixturePath = fixturesRoot.resolve(fixtureName + "-" + hash + ".json");

            // Try to read cached fixture
            try {
                if (Files.exists(fixturePath)) {
                    String content = Files.readString(fixturePath);
                    return parseFixture(content);
                }
            } catch (Exception e) {
                // Fall through to create new fixture
            }

            // Check if in CI without record mode
            String ci = System.getenv("CI");
            String vcrRecord = System.getenv("VCR_RECORD");
            if (("true".equals(ci) || "1".equals(ci)) && !"true".equals(vcrRecord)) {
                throw new RuntimeException(
                    "Fixture missing: " + fixturePath + ". Re-run tests with VCR_RECORD=1, then commit the result."
                );
            }

            // Create and write new fixture
            T result = supplier.get().join();

            try {
                Files.createDirectories(fixturePath.getParent());
                Files.writeString(fixturePath, serializeFixture(result));
            } catch (Exception e) {
                // Ignore write errors
            }

            return result;
        });
    }

    /**
     * Execute with VCR for messages.
     */
    public <T> CompletableFuture<List<T>> withVCR(List<?> messages, Supplier<CompletableFuture<List<T>>> supplier) {
        return CompletableFuture.supplyAsync(() -> {
            if (!shouldUseVCR()) {
                return supplier.get().join();
            }

            // Create hash from messages
            String hash = createMessagesHash(messages);
            Path fixturePath = fixturesRoot.resolve(hash + ".json");

            // Try to read cached fixture
            try {
                if (Files.exists(fixturePath)) {
                    String content = Files.readString(fixturePath);
                    return parseMessagesFixture(content);
                }
            } catch (Exception e) {
                // Fall through
            }

            String ci = System.getenv("CI");
            String vcrRecord = System.getenv("VCR_RECORD");
            if (("true".equals(ci) || "1".equals(ci)) && !"true".equals(vcrRecord)) {
                throw new RuntimeException(
                    "Anthropic API fixture missing: " + fixturePath + ". Re-run with VCR_RECORD=1."
                );
            }

            // Create new fixture
            List<T> result = supplier.get().join();

            try {
                Files.createDirectories(fixturePath.getParent());
                Files.writeString(fixturePath, serializeMessagesFixture(result));
            } catch (Exception e) {
                // Ignore write errors
            }

            return result;
        });
    }

    /**
     * Create hash from object.
     */
    private String createHash(Object input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hash = digest.digest(input.toString().getBytes());
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < Math.min(hash.length, 6); i++) {
                hex.append(String.format("%02x", hash[i]));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            return UUID.randomUUID().toString().substring(0, 12);
        }
    }

    /**
     * Create hash from messages.
     */
    private String createMessagesHash(List<?> messages) {
        StringBuilder sb = new StringBuilder();
        for (Object msg : messages) {
            sb.append(createHash(msg));
        }
        return createHash(sb.toString());
    }

    /**
     * Parse fixture content.
     */
    @SuppressWarnings("unchecked")
    private <T> T parseFixture(String content) {
        // Simplified parsing
        return (T) content;
    }

    /**
     * Serialize fixture.
     */
    private <T> String serializeFixture(T result) {
        return result != null ? result.toString() : "null";
    }

    /**
     * Parse messages fixture.
     */
    @SuppressWarnings("unchecked")
    private <T> List<T> parseMessagesFixture(String content) {
        // Simplified parsing
        return Collections.emptyList();
    }

    /**
     * Serialize messages fixture.
     */
    private <T> String serializeMessagesFixture(List<T> messages) {
        StringBuilder sb = new StringBuilder("{\"output\":[");
        for (int i = 0; i < messages.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(messages.get(i) != null ? messages.get(i).toString() : "null");
        }
        sb.append("]}");
        return sb.toString();
    }

    /**
     * Dehydrate value for fixture.
     */
    public String dehydrateValue(String value) {
        if (value == null) return null;

        String cwd = System.getProperty("user.dir");
        String configHome = System.getProperty("user.home") + "/.claude";

        return value
            .replaceAll("num_files=\"\\d+\"", "num_files=\"[NUM]\"")
            .replaceAll("duration_ms=\"\\d+\"", "duration_ms=\"[DURATION]\"")
            .replaceAll("cost_usd=\"\\d+\"", "cost_usd=\"[COST]\"")
            .replace(configHome, "[CONFIG_HOME]")
            .replace(cwd, "[CWD]");
    }

    /**
     * Hydrate value from fixture.
     */
    public String hydrateValue(String value) {
        if (value == null) return null;

        String cwd = System.getProperty("user.dir");
        String configHome = System.getProperty("user.home") + "/.claude";

        return value
            .replace("[NUM]", "1")
            .replace("[DURATION]", "100")
            .replace("[CONFIG_HOME]", configHome)
            .replace("[CWD]", cwd);
    }
}