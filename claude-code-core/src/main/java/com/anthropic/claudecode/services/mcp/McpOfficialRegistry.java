/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/mcp/officialRegistry
 */
package com.anthropic.claudecode.services.mcp;

import java.time.Instant;

import java.util.*;
import java.util.concurrent.*;

/**
 * Official registry - Official MCP server registry.
 */
public final class McpOfficialRegistry {
    private final Map<String, RegistryEntry> registry = new ConcurrentHashMap<>();
    private volatile Instant lastUpdated = null;

    /**
     * Registry entry record.
     */
    public record RegistryEntry(
        String id,
        String name,
        String description,
        String version,
        String publisher,
        String repositoryUrl,
        String documentationUrl,
        List<String> tags,
        List<CapabilityInfo> capabilities,
        InstallationInfo installation,
        VerificationStatus verification,
        Instant publishedAt,
        Instant updatedAt,
        int downloadCount,
        double rating
    ) {
        public boolean isVerified() {
            return verification == VerificationStatus.VERIFIED;
        }
    }

    /**
     * Capability info record.
     */
    public record CapabilityInfo(
        String name,
        String description,
        List<String> tools,
        List<String> resources
    ) {}

    /**
     * Installation info record.
     */
    public record InstallationInfo(
        String type,
        String command,
        List<String> args,
        Map<String, String> env,
        String platform
    ) {}

    /**
     * Verification status enum.
     */
    public enum VerificationStatus {
        UNVERIFIED,
        VERIFIED,
        OFFICIAL
    }

    /**
     * Initialize registry.
     */
    public McpOfficialRegistry() {
        loadDefaultEntries();
    }

    /**
     * Load default entries.
     */
    private void loadDefaultEntries() {
        register(new RegistryEntry(
            "filesystem",
            "Filesystem",
            "File system operations for local files",
            "1.0.0",
            "Anthropic",
            "https://github.com/anthropic/mcp-server-filesystem",
            "https://docs.anthropic.com/mcp/servers/filesystem",
            List.of("files", "io", "local"),
            List.of(
                new CapabilityInfo("files", "File operations", List.of("read", "write", "delete"), List.of("files"))
            ),
            new InstallationInfo("npm", "npx", List.of("@anthropic/mcp-server-filesystem"), Map.of(), "all"),
            VerificationStatus.OFFICIAL,
            Instant.now(),
            Instant.now(),
            0,
            5.0
        ));

        register(new RegistryEntry(
            "github",
            "GitHub",
            "GitHub API integration",
            "1.0.0",
            "Anthropic",
            "https://github.com/anthropic/mcp-server-github",
            "https://docs.anthropic.com/mcp/servers/github",
            List.of("git", "github", "vcs"),
            List.of(
                new CapabilityInfo("repos", "Repository operations", List.of("create-repo", "list-repos"), List.of("repos"))
            ),
            new InstallationInfo("npm", "npx", List.of("@anthropic/mcp-server-github"), Map.of(), "all"),
            VerificationStatus.OFFICIAL,
            Instant.now(),
            Instant.now(),
            0,
            5.0
        ));

        lastUpdated = Instant.now();
    }

    /**
     * Register entry.
     */
    public void register(RegistryEntry entry) {
        registry.put(entry.id(), entry);
    }

    /**
     * Get entry by ID.
     */
    public RegistryEntry get(String id) {
        return registry.get(id);
    }

    /**
     * Search entries.
     */
    public List<RegistryEntry> search(String query) {
        String lower = query.toLowerCase();
        return registry.values()
            .stream()
            .filter(e ->
                e.name().toLowerCase().contains(lower) ||
                e.description().toLowerCase().contains(lower) ||
                e.tags().stream().anyMatch(t -> t.toLowerCase().contains(lower))
            )
            .sorted(Comparator.comparing(RegistryEntry::name))
            .toList();
    }

    /**
     * Get all entries.
     */
    public List<RegistryEntry> getAll() {
        return new ArrayList<>(registry.values());
    }

    /**
     * Get by publisher.
     */
    public List<RegistryEntry> getByPublisher(String publisher) {
        return registry.values()
            .stream()
            .filter(e -> e.publisher().equalsIgnoreCase(publisher))
            .toList();
    }

    /**
     * Get verified entries.
     */
    public List<RegistryEntry> getVerified() {
        return registry.values()
            .stream()
            .filter(RegistryEntry::isVerified)
            .toList();
    }

    /**
     * Get by tag.
     */
    public List<RegistryEntry> getByTag(String tag) {
        return registry.values()
            .stream()
            .filter(e -> e.tags().contains(tag))
            .toList();
    }

    /**
     * Refresh registry.
     */
    public CompletableFuture<Void> refresh() {
        return CompletableFuture.runAsync(() -> {
            // Would fetch from remote registry
            lastUpdated = Instant.now();
        });
    }

    /**
     * Get last updated.
     */
    public Instant getLastUpdated() {
        return lastUpdated;
    }

    /**
     * Registry stats record.
     */
    public record RegistryStats(
        int totalEntries,
        int verifiedEntries,
        int officialEntries,
        Map<String, Integer> countByPublisher,
        Instant lastUpdated
    ) {
        public String format() {
            return String.format("Total: %d, Verified: %d, Official: %d",
                totalEntries, verifiedEntries, officialEntries);
        }
    }

    /**
     * Get stats.
     */
    public RegistryStats getStats() {
        int verified = 0;
        int official = 0;
        Map<String, Integer> byPublisher = new HashMap<>();

        for (RegistryEntry entry : registry.values()) {
            if (entry.verification() == VerificationStatus.VERIFIED) verified++;
            if (entry.verification() == VerificationStatus.OFFICIAL) official++;
            byPublisher.merge(entry.publisher(), 1, Integer::sum);
        }

        return new RegistryStats(registry.size(), verified, official, byPublisher, lastUpdated);
    }
}