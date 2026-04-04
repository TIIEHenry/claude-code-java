/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VersionUtils.
 */
class VersionUtilsTest {

    @Test
    @DisplayName("VersionUtils SemanticVersion of creates version")
    void semanticVersionOf() {
        VersionUtils.SemanticVersion sv = VersionUtils.SemanticVersion.of(1, 2, 3);

        assertEquals(1, sv.major());
        assertEquals(2, sv.minor());
        assertEquals(3, sv.patch());
    }

    @Test
    @DisplayName("VersionUtils SemanticVersion parse")
    void semanticVersionParse() {
        VersionUtils.SemanticVersion sv = VersionUtils.SemanticVersion.parse("1.2.3");

        assertEquals(1, sv.major());
        assertEquals(2, sv.minor());
        assertEquals(3, sv.patch());
    }

    @Test
    @DisplayName("VersionUtils SemanticVersion format")
    void semanticVersionFormat() {
        VersionUtils.SemanticVersion sv = new VersionUtils.SemanticVersion(1, 2, 3, "alpha", "build123");
        String result = sv.format();

        assertEquals("1.2.3-alpha+build123", result);
    }

    @Test
    @DisplayName("VersionUtils SemanticVersion isStable true")
    void semanticVersionIsStableTrue() {
        VersionUtils.SemanticVersion sv = VersionUtils.SemanticVersion.of(1, 0, 0);

        assertTrue(sv.isStable());
    }

    @Test
    @DisplayName("VersionUtils SemanticVersion isStable false")
    void semanticVersionIsStableFalse() {
        VersionUtils.SemanticVersion sv = new VersionUtils.SemanticVersion(1, 0, 0, "alpha", null);

        assertFalse(sv.isStable());
    }

    @Test
    @DisplayName("VersionUtils SemanticVersion isNewerThan")
    void semanticVersionIsNewerThan() {
        VersionUtils.SemanticVersion v1 = VersionUtils.SemanticVersion.of(1, 2, 0);
        VersionUtils.SemanticVersion v2 = VersionUtils.SemanticVersion.of(1, 1, 0);

        assertTrue(v1.isNewerThan(v2));
        assertFalse(v2.isNewerThan(v1));
    }

    @Test
    @DisplayName("VersionUtils SemanticVersion isOlderThan")
    void semanticVersionIsOlderThan() {
        VersionUtils.SemanticVersion v1 = VersionUtils.SemanticVersion.of(1, 1, 0);
        VersionUtils.SemanticVersion v2 = VersionUtils.SemanticVersion.of(1, 2, 0);

        assertTrue(v1.isOlderThan(v2));
        assertFalse(v2.isOlderThan(v1));
    }

    @Test
    @DisplayName("VersionUtils parseSemanticVersion full version")
    void parseSemanticVersionFull() {
        VersionUtils.SemanticVersion sv = VersionUtils.parseSemanticVersion("v1.2.3-alpha+build");

        assertEquals(1, sv.major());
        assertEquals(2, sv.minor());
        assertEquals(3, sv.patch());
        assertEquals("alpha", sv.prerelease());
        assertEquals("build", sv.build());
    }

    @Test
    @DisplayName("VersionUtils parseSemanticVersion null returns 0.0.0")
    void parseSemanticVersionNull() {
        VersionUtils.SemanticVersion sv = VersionUtils.parseSemanticVersion(null);

        assertEquals(0, sv.major());
        assertEquals(0, sv.minor());
        assertEquals(0, sv.patch());
    }

    @Test
    @DisplayName("VersionUtils parseSemanticVersion invalid returns 0.0.0")
    void parseSemanticVersionInvalid() {
        VersionUtils.SemanticVersion sv = VersionUtils.parseSemanticVersion("invalid");

        assertEquals(0, sv.major());
    }

    @Test
    @DisplayName("VersionUtils compareVersions")
    void compareVersions() {
        assertTrue(VersionUtils.compareVersions("2.0.0", "1.0.0") > 0);
        assertTrue(VersionUtils.compareVersions("1.0.0", "2.0.0") < 0);
        assertEquals(0, VersionUtils.compareVersions("1.0.0", "1.0.0"));
    }

    @Test
    @DisplayName("VersionUtils isVersionGreater")
    void isVersionGreater() {
        assertTrue(VersionUtils.isVersionGreater("2.0.0", "1.0.0"));
        assertFalse(VersionUtils.isVersionGreater("1.0.0", "2.0.0"));
    }

    @Test
    @DisplayName("VersionUtils isVersionLess")
    void isVersionLess() {
        assertTrue(VersionUtils.isVersionLess("1.0.0", "2.0.0"));
        assertFalse(VersionUtils.isVersionLess("2.0.0", "1.0.0"));
    }

    @Test
    @DisplayName("VersionUtils isVersionEqual")
    void isVersionEqual() {
        assertTrue(VersionUtils.isVersionEqual("1.2.3", "1.2.3"));
        assertTrue(VersionUtils.isVersionEqual("1.2.3-alpha", "1.2.3-beta"));
        assertFalse(VersionUtils.isVersionEqual("1.2.3", "1.2.4"));
    }

    @Test
    @DisplayName("VersionUtils satisfiesRange caret")
    void satisfiesRangeCaret() {
        assertTrue(VersionUtils.satisfiesRange("1.2.3", "^1.0.0"));
        assertTrue(VersionUtils.satisfiesRange("1.5.0", "^1.2.3"));
        assertFalse(VersionUtils.satisfiesRange("2.0.0", "^1.0.0"));
    }

    @Test
    @DisplayName("VersionUtils satisfiesRange tilde")
    void satisfiesRangeTilde() {
        assertTrue(VersionUtils.satisfiesRange("1.2.5", "~1.2.3"));
        assertFalse(VersionUtils.satisfiesRange("1.3.0", "~1.2.3"));
    }

    @Test
    @DisplayName("VersionUtils satisfiesRange greater equal")
    void satisfiesRangeGreaterEqual() {
        assertTrue(VersionUtils.satisfiesRange("2.0.0", ">=1.0.0"));
        assertTrue(VersionUtils.satisfiesRange("1.0.0", ">=1.0.0"));
        assertFalse(VersionUtils.satisfiesRange("0.9.0", ">=1.0.0"));
    }

    @Test
    @DisplayName("VersionUtils satisfiesRange less than")
    void satisfiesRangeLess() {
        assertTrue(VersionUtils.satisfiesRange("0.9.0", "<1.0.0"));
        assertFalse(VersionUtils.satisfiesRange("1.0.0", "<1.0.0"));
    }

    @Test
    @DisplayName("VersionUtils satisfiesRange hyphen")
    void satisfiesRangeHyphen() {
        assertTrue(VersionUtils.satisfiesRange("1.5.0", "1.0.0 - 2.0.0"));
        assertFalse(VersionUtils.satisfiesRange("3.0.0", "1.0.0 - 2.0.0"));
    }

    @Test
    @DisplayName("VersionUtils incrementVersion major")
    void incrementVersionMajor() {
        assertEquals("2.0.0", VersionUtils.incrementVersion("1.2.3", "major"));
    }

    @Test
    @DisplayName("VersionUtils incrementVersion minor")
    void incrementVersionMinor() {
        assertEquals("1.3.0", VersionUtils.incrementVersion("1.2.3", "minor"));
    }

    @Test
    @DisplayName("VersionUtils incrementVersion patch")
    void incrementVersionPatch() {
        assertEquals("1.2.4", VersionUtils.incrementVersion("1.2.3", "patch"));
    }

    @Test
    @DisplayName("VersionUtils getMajorVersion")
    void getMajorVersion() {
        assertEquals(1, VersionUtils.getMajorVersion("1.2.3"));
    }

    @Test
    @DisplayName("VersionUtils getMinorVersion")
    void getMinorVersion() {
        assertEquals(2, VersionUtils.getMinorVersion("1.2.3"));
    }

    @Test
    @DisplayName("VersionUtils getPatchVersion")
    void getPatchVersion() {
        assertEquals(3, VersionUtils.getPatchVersion("1.2.3"));
    }

    @Test
    @DisplayName("VersionUtils isValidVersion true")
    void isValidVersionTrue() {
        assertTrue(VersionUtils.isValidVersion("1.2.3"));
        assertTrue(VersionUtils.isValidVersion("v1.2.3"));
        assertTrue(VersionUtils.isValidVersion("1.2.3-alpha"));
    }

    @Test
    @DisplayName("VersionUtils isValidVersion false")
    void isValidVersionFalse() {
        assertFalse(VersionUtils.isValidVersion(null));
        assertFalse(VersionUtils.isValidVersion(""));
        assertFalse(VersionUtils.isValidVersion("abc"));
    }

    @Test
    @DisplayName("VersionUtils normalizeVersion removes v prefix")
    void normalizeVersion() {
        assertEquals("1.2.3", VersionUtils.normalizeVersion("v1.2.3"));
        assertEquals("1.2.3", VersionUtils.normalizeVersion("1.2.3"));
    }

    @Test
    @DisplayName("VersionUtils normalizeVersion null returns null")
    void normalizeVersionNull() {
        assertNull(VersionUtils.normalizeVersion(null));
    }

    @Test
    @DisplayName("VersionUtils findHighestVersion")
    void findHighestVersion() {
        Optional<String> result = VersionUtils.findHighestVersion(List.of("1.0.0", "2.0.0", "1.5.0"));

        assertTrue(result.isPresent());
        assertEquals("2.0.0", result.get());
    }

    @Test
    @DisplayName("VersionUtils findHighestVersion empty returns empty")
    void findHighestVersionEmpty() {
        Optional<String> result = VersionUtils.findHighestVersion(List.of());

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("VersionUtils findLowestVersion")
    void findLowestVersion() {
        Optional<String> result = VersionUtils.findLowestVersion(List.of("1.5.0", "1.0.0", "2.0.0"));

        assertTrue(result.isPresent());
        assertEquals("1.0.0", result.get());
    }

    @Test
    @DisplayName("VersionUtils filterVersionsByRange")
    void filterVersionsByRange() {
        List<String> versions = List.of("1.0.0", "1.2.0", "2.0.0", "2.5.0");
        List<String> result = VersionUtils.filterVersionsByRange(versions, "^1.0.0");

        assertEquals(2, result.size());
        assertTrue(result.contains("1.0.0"));
        assertTrue(result.contains("1.2.0"));
    }

    @Test
    @DisplayName("VersionUtils prerelease comparison")
    void prereleaseComparison() {
        VersionUtils.SemanticVersion stable = VersionUtils.SemanticVersion.of(1, 0, 0);
        VersionUtils.SemanticVersion prerelease = new VersionUtils.SemanticVersion(1, 0, 0, "alpha", null);

        // Pre-release has lower precedence
        assertTrue(prerelease.compareTo(stable) < 0);
        assertTrue(stable.compareTo(prerelease) > 0);
    }
}