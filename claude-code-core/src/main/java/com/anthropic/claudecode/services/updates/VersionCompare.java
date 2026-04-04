/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/updates/versionCompare
 */
package com.anthropic.claudecode.services.updates;

import java.util.*;

/**
 * Version compare - Version comparison utilities.
 */
public final class VersionCompare {

    /**
     * Compare two versions.
     */
    public static int compare(String v1, String v2) {
        String[] parts1 = normalizeVersion(v1).split("\\.");
        String[] parts2 = normalizeVersion(v2).split("\\.");

        for (int i = 0; i < Math.max(parts1.length, parts2.length); i++) {
            int n1 = i < parts1.length ? parsePart(parts1[i]) : 0;
            int n2 = i < parts2.length ? parsePart(parts2[i]) : 0;

            if (n1 != n2) {
                return n1 - n2;
            }
        }

        return 0;
    }

    /**
     * Check if v1 is greater than v2.
     */
    public static boolean isGreater(String v1, String v2) {
        return compare(v1, v2) > 0;
    }

    /**
     * Check if v1 is less than v2.
     */
    public static boolean isLess(String v1, String v2) {
        return compare(v1, v2) < 0;
    }

    /**
     * Check if versions are equal.
     */
    public static boolean isEqual(String v1, String v2) {
        return compare(v1, v2) == 0;
    }

    /**
     * Check if v1 is greater or equal to v2.
     */
    public static boolean isGreaterOrEqual(String v1, String v2) {
        return compare(v1, v2) >= 0;
    }

    /**
     * Check if v1 is less or equal to v2.
     */
    public static boolean isLessOrEqual(String v1, String v2) {
        return compare(v1, v2) <= 0;
    }

    /**
     * Normalize version string.
     */
    private static String normalizeVersion(String version) {
        if (version == null) return "0.0.0";

        // Remove prefix like 'v' or 'version'
        version = version.replaceAll("^v|^version", "");

        // Remove suffix like '-beta', '-alpha'
        version = version.replaceAll("-[a-zA-Z]+.*$", "");

        return version;
    }

    /**
     * Parse version part.
     */
    private static int parsePart(String part) {
        try {
            return Integer.parseInt(part.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Get major version.
     */
    public static int getMajor(String version) {
        String[] parts = normalizeVersion(version).split("\\.");
        return parts.length > 0 ? parsePart(parts[0]) : 0;
    }

    /**
     * Get minor version.
     */
    public static int getMinor(String version) {
        String[] parts = normalizeVersion(version).split("\\.");
        return parts.length > 1 ? parsePart(parts[1]) : 0;
    }

    /**
     * Get patch version.
     */
    public static int getPatch(String version) {
        String[] parts = normalizeVersion(version).split("\\.");
        return parts.length > 2 ? parsePart(parts[2]) : 0;
    }

    /**
     * Check if version is prerelease.
     */
    public static boolean isPrerelease(String version) {
        if (version == null) return false;
        return version.contains("-alpha") ||
               version.contains("-beta") ||
               version.contains("-rc") ||
               version.contains("-pre") ||
               version.contains("-dev");
    }

    /**
     * Get prerelease type.
     */
    public static PrereleaseType getPrereleaseType(String version) {
        if (version == null) return PrereleaseType.NONE;

        String lower = version.toLowerCase();
        if (lower.contains("-alpha")) return PrereleaseType.ALPHA;
        if (lower.contains("-beta")) return PrereleaseType.BETA;
        if (lower.contains("-rc")) return PrereleaseType.RELEASE_CANDIDATE;
        if (lower.contains("-pre")) return PrereleaseType.PRE;
        if (lower.contains("-dev")) return PrereleaseType.DEV;

        return PrereleaseType.NONE;
    }

    /**
     * Prerelease type enum.
     */
    public enum PrereleaseType {
        NONE(""),
        ALPHA("alpha"),
        BETA("beta"),
        RELEASE_CANDIDATE("rc"),
        PRE("pre"),
        DEV("dev");

        private final String suffix;

        PrereleaseType(String suffix) {
            this.suffix = suffix;
        }

        public String getSuffix() { return suffix; }
    }

    /**
     * Version range record.
     */
    public record VersionRange(String min, String max, boolean includeMin, boolean includeMax) {
        public boolean contains(String version) {
            boolean minOk = includeMin
                ? VersionCompare.isGreaterOrEqual(version, min)
                : VersionCompare.isGreater(version, min);

            boolean maxOk = max == null || (includeMax
                ? VersionCompare.isLessOrEqual(version, max)
                : VersionCompare.isLess(version, max));

            return minOk && maxOk;
        }

        public static VersionRange from(String min) {
            return new VersionRange(min, null, true, true);
        }

        public static VersionRange range(String min, String max) {
            return new VersionRange(min, max, true, true);
        }
    }

    /**
     * Find highest version.
     */
    public static String findHighest(List<String> versions) {
        return versions.stream()
            .max((v1, v2) -> compare(v1, v2))
            .orElse(null);
    }

    /**
     * Find lowest version.
     */
    public static String findLowest(List<String> versions) {
        return versions.stream()
            .min((v1, v2) -> compare(v1, v2))
            .orElse(null);
    }

    /**
     * Sort versions.
     */
    public static List<String> sortVersions(List<String> versions) {
        return versions.stream()
            .sorted((v1, v2) -> compare(v1, v2))
            .toList();
    }

    /**
     * Sort versions descending.
     */
    public static List<String> sortVersionsDescending(List<String> versions) {
        return versions.stream()
            .sorted((v1, v2) -> compare(v2, v1))
            .toList();
    }
}