/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code version utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.regex.*;

/**
 * Version parsing and comparison utilities.
 */
public final class VersionUtils {
    private VersionUtils() {}

    /**
     * Semantic version representation.
     */
    public record SemanticVersion(int major, int minor, int patch, String prerelease, String build) {
        public static SemanticVersion of(int major, int minor, int patch) {
            return new SemanticVersion(major, minor, patch, null, null);
        }

        public static SemanticVersion parse(String version) {
            return VersionUtils.parseSemanticVersion(version);
        }

        public String format() {
            StringBuilder sb = new StringBuilder();
            sb.append(major).append(".").append(minor).append(".").append(patch);
            if (prerelease != null) sb.append("-").append(prerelease);
            if (build != null) sb.append("+").append(build);
            return sb.toString();
        }

        public boolean isStable() {
            return prerelease == null || prerelease.isEmpty();
        }

        public boolean isNewerThan(SemanticVersion other) {
            return compareTo(other) > 0;
        }

        public boolean isOlderThan(SemanticVersion other) {
            return compareTo(other) < 0;
        }

        public int compareTo(SemanticVersion other) {
            if (major != other.major) return major - other.major;
            if (minor != other.minor) return minor - other.minor;
            if (patch != other.patch) return patch - other.patch;

            // Pre-release versions have lower precedence
            if (prerelease != null && other.prerelease == null) return -1;
            if (prerelease == null && other.prerelease != null) return 1;

            return 0;
        }
    }

    private static final Pattern SEMVER_PATTERN = Pattern.compile(
        "v?(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:-([a-zA-Z0-9.-]+))?(?:\\+([a-zA-Z0-9.-]+))?"
    );

    /**
     * Parse semantic version string.
     */
    public static SemanticVersion parseSemanticVersion(String version) {
        if (version == null || version.isEmpty()) {
            return SemanticVersion.of(0, 0, 0);
        }

        Matcher matcher = SEMVER_PATTERN.matcher(version.trim());
        if (!matcher.matches()) {
            return SemanticVersion.of(0, 0, 0);
        }

        int major = Integer.parseInt(matcher.group(1));
        int minor = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
        int patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
        String prerelease = matcher.group(4);
        String build = matcher.group(5);

        return new SemanticVersion(major, minor, patch, prerelease, build);
    }

    /**
     * Compare two version strings.
     */
    public static int compareVersions(String v1, String v2) {
        SemanticVersion sv1 = parseSemanticVersion(v1);
        SemanticVersion sv2 = parseSemanticVersion(v2);
        return sv1.compareTo(sv2);
    }

    /**
     * Check if version is greater than another.
     */
    public static boolean isVersionGreater(String v1, String v2) {
        return compareVersions(v1, v2) > 0;
    }

    /**
     * Check if version is less than another.
     */
    public static boolean isVersionLess(String v1, String v2) {
        return compareVersions(v1, v2) < 0;
    }

    /**
     * Check if versions are equal (ignoring prerelease/build).
     */
    public static boolean isVersionEqual(String v1, String v2) {
        SemanticVersion sv1 = parseSemanticVersion(v1);
        SemanticVersion sv2 = parseSemanticVersion(v2);
        return sv1.major() == sv2.major() && sv1.minor() == sv2.minor() && sv1.patch() == sv2.patch();
    }

    /**
     * Check if version satisfies a range.
     * Supports: ^1.2.3, ~1.2.3, >=1.2.3, <1.2.3, 1.2.3 - 2.0.0
     */
    public static boolean satisfiesRange(String version, String range) {
        SemanticVersion sv = parseSemanticVersion(version);
        String trimmedRange = range.trim();

        // Caret range (^)
        if (trimmedRange.startsWith("^")) {
            SemanticVersion target = parseSemanticVersion(trimmedRange.substring(1));
            return satisfiesCaretRange(sv, target);
        }

        // Tilde range (~)
        if (trimmedRange.startsWith("~")) {
            SemanticVersion target = parseSemanticVersion(trimmedRange.substring(1));
            return satisfiesTildeRange(sv, target);
        }

        // Greater than or equal
        if (trimmedRange.startsWith(">=")) {
            SemanticVersion target = parseSemanticVersion(trimmedRange.substring(2));
            return sv.compareTo(target) >= 0;
        }

        // Less than
        if (trimmedRange.startsWith("<")) {
            if (trimmedRange.startsWith("<=")) {
                SemanticVersion target = parseSemanticVersion(trimmedRange.substring(2));
                return sv.compareTo(target) <= 0;
            }
            SemanticVersion target = parseSemanticVersion(trimmedRange.substring(1));
            return sv.compareTo(target) < 0;
        }

        // Greater than
        if (trimmedRange.startsWith(">")) {
            SemanticVersion target = parseSemanticVersion(trimmedRange.substring(1));
            return sv.compareTo(target) > 0;
        }

        // Hyphen range (1.2.3 - 2.0.0)
        if (trimmedRange.contains(" - ")) {
            String[] parts = trimmedRange.split(" - ");
            SemanticVersion lower = parseSemanticVersion(parts[0]);
            SemanticVersion upper = parseSemanticVersion(parts[1]);
            return sv.compareTo(lower) >= 0 && sv.compareTo(upper) <= 0;
        }

        // Exact version
        return isVersionEqual(version, trimmedRange);
    }

    private static boolean satisfiesCaretRange(SemanticVersion version, SemanticVersion target) {
        // ^1.2.3 := >=1.2.3 <2.0.0
        // ^0.2.3 := >=0.2.3 <0.3.0
        // ^0.0.3 := >=0.0.3 <0.0.4
        if (target.major() == 0) {
            if (target.minor() == 0) {
                return version.major() == 0 && version.minor() == 0 && version.patch() >= target.patch();
            }
            return version.major() == 0 && version.minor() == target.minor() && version.patch() >= target.patch();
        }
        return version.major() == target.major() && version.compareTo(target) >= 0;
    }

    private static boolean satisfiesTildeRange(SemanticVersion version, SemanticVersion target) {
        // ~1.2.3 := >=1.2.3 <1.3.0
        return version.major() == target.major() && version.minor() == target.minor() && version.patch() >= target.patch();
    }

    /**
     * Get the next version after incrementing a component.
     */
    public static String incrementVersion(String version, String component) {
        SemanticVersion sv = parseSemanticVersion(version);
        switch (component.toLowerCase()) {
            case "major":
                return SemanticVersion.of(sv.major() + 1, 0, 0).format();
            case "minor":
                return SemanticVersion.of(sv.major(), sv.minor() + 1, 0).format();
            case "patch":
                return SemanticVersion.of(sv.major(), sv.minor(), sv.patch() + 1).format();
            default:
                return version;
        }
    }

    /**
     * Get major version from string.
     */
    public static int getMajorVersion(String version) {
        return parseSemanticVersion(version).major();
    }

    /**
     * Get minor version from string.
     */
    public static int getMinorVersion(String version) {
        return parseSemanticVersion(version).minor();
    }

    /**
     * Get patch version from string.
     */
    public static int getPatchVersion(String version) {
        return parseSemanticVersion(version).patch();
    }

    /**
     * Check if version string is valid.
     */
    public static boolean isValidVersion(String version) {
        if (version == null || version.isEmpty()) return false;
        return SEMVER_PATTERN.matcher(version.trim()).matches();
    }

    /**
     * Normalize version string (remove 'v' prefix).
     */
    public static String normalizeVersion(String version) {
        if (version == null) return null;
        String trimmed = version.trim();
        if (trimmed.startsWith("v")) {
            return trimmed.substring(1);
        }
        return trimmed;
    }

    /**
     * Find the highest version from a list.
     */
    public static Optional<String> findHighestVersion(List<String> versions) {
        if (versions == null || versions.isEmpty()) {
            return Optional.empty();
        }

        String highest = versions.get(0);
        for (String version : versions) {
            if (isVersionGreater(version, highest)) {
                highest = version;
            }
        }
        return Optional.of(highest);
    }

    /**
     * Find the lowest version from a list.
     */
    public static Optional<String> findLowestVersion(List<String> versions) {
        if (versions == null || versions.isEmpty()) {
            return Optional.empty();
        }

        String lowest = versions.get(0);
        for (String version : versions) {
            if (isVersionLess(version, lowest)) {
                lowest = version;
            }
        }
        return Optional.of(lowest);
    }

    /**
     * Filter versions by range.
     */
    public static List<String> filterVersionsByRange(List<String> versions, String range) {
        List<String> result = new ArrayList<>();
        for (String version : versions) {
            if (satisfiesRange(version, range)) {
                result.add(version);
            }
        }
        return result;
    }
}