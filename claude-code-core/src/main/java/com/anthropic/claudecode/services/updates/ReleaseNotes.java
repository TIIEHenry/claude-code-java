/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/updates/releaseNotes
 */
package com.anthropic.claudecode.services.updates;

import java.util.*;
import java.time.*;

/**
 * Release notes - Release notes management.
 */
public final class ReleaseNotes {
    private final List<ReleaseEntry> releases = new ArrayList<>();

    /**
     * Release entry record.
     */
    public record ReleaseEntry(
        String version,
        LocalDate releaseDate,
        String title,
        String summary,
        List<ReleaseChange> changes,
        List<String> knownIssues,
        boolean isPrerelease,
        String downloadUrl,
        String releaseUrl
    ) {
        public String formatFull() {
            StringBuilder sb = new StringBuilder();
            sb.append("## ").append(title).append("\n\n");
            sb.append("Version: ").append(version).append("\n");
            sb.append("Released: ").append(releaseDate).append("\n\n");
            sb.append(summary).append("\n\n");

            sb.append("### Changes\n\n");
            for (ReleaseChange change : changes) {
                sb.append("- ").append(change.format()).append("\n");
            }

            if (!knownIssues.isEmpty()) {
                sb.append("\n### Known Issues\n\n");
                for (String issue : knownIssues) {
                    sb.append("- ").append(issue).append("\n");
                }
            }

            return sb.toString();
        }

        public String formatBrief() {
            return String.format(
                "%s (%s): %s",
                version,
                releaseDate,
                summary
            );
        }
    }

    /**
     * Release change record.
     */
    public record ReleaseChange(
        ChangeType type,
        String description,
        String component,
        String issueRef
    ) {
        public String format() {
            StringBuilder sb = new StringBuilder();
            sb.append("[").append(type.getSymbol()).append("] ");
            if (component != null) {
                sb.append(component).append(": ");
            }
            sb.append(description);
            if (issueRef != null) {
                sb.append(" (").append(issueRef).append(")");
            }
            return sb.toString();
        }
    }

    /**
     * Change type enum.
     */
    public enum ChangeType {
        FEATURE("✨", "New feature"),
        FIX("🐛", "Bug fix"),
        IMPROVEMENT("⚡", "Improvement"),
        REFACTOR("♻", "Refactoring"),
        DOC("📝", "Documentation"),
        TEST("✅", "Tests"),
        SECURITY("🔒", "Security"),
        DEPRECATION("⚠", "Deprecation"),
        BREAKING("💥", "Breaking change");

        private final String symbol;
        private final String label;

        ChangeType(String symbol, String label) {
            this.symbol = symbol;
            this.label = label;
        }

        public String getSymbol() { return symbol; }
        public String getLabel() { return label; }
    }

    /**
     * Add release.
     */
    public void addRelease(ReleaseEntry release) {
        releases.add(release);
    }

    /**
     * Get release by version.
     */
    public ReleaseEntry getRelease(String version) {
        return releases.stream()
            .filter(r -> r.version().equals(version))
            .findFirst()
            .orElse(null);
    }

    /**
     * Get latest release.
     */
    public ReleaseEntry getLatestRelease() {
        if (releases.isEmpty()) return null;
        return releases.stream()
            .filter(r -> !r.isPrerelease())
            .max(Comparator.comparing(ReleaseEntry::releaseDate))
            .orElse(null);
    }

    /**
     * Get all releases.
     */
    public List<ReleaseEntry> getAllReleases() {
        return Collections.unmodifiableList(releases);
    }

    /**
     * Get releases since version.
     */
    public List<ReleaseEntry> getReleasesSince(String version) {
        return releases.stream()
            .filter(r -> isNewer(r.version(), version))
            .sorted(Comparator.comparing(ReleaseEntry::releaseDate).reversed())
            .toList();
    }

    /**
     * Check if version is newer.
     */
    private boolean isNewer(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");

        for (int i = 0; i < Math.max(parts1.length, parts2.length); i++) {
            int n1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int n2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (n1 != n2) return n1 > n2;
        }

        return false;
    }

    /**
     * Format release summary.
     */
    public String formatSummary(int limit) {
        StringBuilder sb = new StringBuilder();
        sb.append("## Recent Releases\n\n");

        int count = 0;
        for (ReleaseEntry release : releases) {
            if (count >= limit) break;
            sb.append(release.formatBrief()).append("\n");
            count++;
        }

        return sb.toString();
    }
}