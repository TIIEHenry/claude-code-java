/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.updates;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VersionCompare.
 */
class VersionCompareTest {

    @Test
    @DisplayName("VersionCompare compare equal versions")
    void compareEqual() {
        assertEquals(0, VersionCompare.compare("1.0.0", "1.0.0"));
        assertEquals(0, VersionCompare.compare("2.5.3", "2.5.3"));
    }

    @Test
    @DisplayName("VersionCompare compare greater version")
    void compareGreater() {
        assertTrue(VersionCompare.compare("2.0.0", "1.0.0") > 0);
        assertTrue(VersionCompare.compare("1.1.0", "1.0.0") > 0);
        assertTrue(VersionCompare.compare("1.0.1", "1.0.0") > 0);
        assertTrue(VersionCompare.compare("1.0.10", "1.0.9") > 0);
    }

    @Test
    @DisplayName("VersionCompare compare lesser version")
    void compareLesser() {
        assertTrue(VersionCompare.compare("1.0.0", "2.0.0") < 0);
        assertTrue(VersionCompare.compare("1.0.0", "1.1.0") < 0);
        assertTrue(VersionCompare.compare("1.0.0", "1.0.1") < 0);
    }

    @Test
    @DisplayName("VersionCompare compare different lengths")
    void compareDifferentLengths() {
        assertTrue(VersionCompare.compare("1.0.0", "1.0") > 0);
        assertTrue(VersionCompare.compare("1.0", "1.0.0") < 0);
        assertEquals(0, VersionCompare.compare("1.0.0", "1.0.0.0"));
    }

    @Test
    @DisplayName("VersionCompare isGreater")
    void isGreater() {
        assertTrue(VersionCompare.isGreater("2.0.0", "1.0.0"));
        assertFalse(VersionCompare.isGreater("1.0.0", "2.0.0"));
        assertFalse(VersionCompare.isGreater("1.0.0", "1.0.0"));
    }

    @Test
    @DisplayName("VersionCompare isLess")
    void isLess() {
        assertTrue(VersionCompare.isLess("1.0.0", "2.0.0"));
        assertFalse(VersionCompare.isLess("2.0.0", "1.0.0"));
        assertFalse(VersionCompare.isLess("1.0.0", "1.0.0"));
    }

    @Test
    @DisplayName("VersionCompare isEqual")
    void isEqual() {
        assertTrue(VersionCompare.isEqual("1.0.0", "1.0.0"));
        assertFalse(VersionCompare.isEqual("1.0.0", "1.0.1"));
    }

    @Test
    @DisplayName("VersionCompare isGreaterOrEqual")
    void isGreaterOrEqual() {
        assertTrue(VersionCompare.isGreaterOrEqual("2.0.0", "1.0.0"));
        assertTrue(VersionCompare.isGreaterOrEqual("1.0.0", "1.0.0"));
        assertFalse(VersionCompare.isGreaterOrEqual("1.0.0", "2.0.0"));
    }

    @Test
    @DisplayName("VersionCompare isLessOrEqual")
    void isLessOrEqual() {
        assertTrue(VersionCompare.isLessOrEqual("1.0.0", "2.0.0"));
        assertTrue(VersionCompare.isLessOrEqual("1.0.0", "1.0.0"));
        assertFalse(VersionCompare.isLessOrEqual("2.0.0", "1.0.0"));
    }

    @Test
    @DisplayName("VersionCompare with v prefix")
    void withVPrefix() {
        assertEquals(0, VersionCompare.compare("v1.0.0", "1.0.0"));
        assertTrue(VersionCompare.isEqual("v2.5.0", "2.5.0"));
    }

    @Test
    @DisplayName("VersionCompare with version prefix")
    void withVersionPrefix() {
        assertEquals(0, VersionCompare.compare("version1.0.0", "1.0.0"));
    }

    @Test
    @DisplayName("VersionCompare with prerelease suffix")
    void withPrereleaseSuffix() {
        assertEquals(0, VersionCompare.compare("1.0.0-beta", "1.0.0"));
        assertEquals(0, VersionCompare.compare("1.0.0-alpha.1", "1.0.0"));
    }

    @Test
    @DisplayName("VersionCompare getMajor")
    void getMajor() {
        assertEquals(1, VersionCompare.getMajor("1.2.3"));
        assertEquals(2, VersionCompare.getMajor("2.0.0"));
        assertEquals(0, VersionCompare.getMajor("0.1.0"));
    }

    @Test
    @DisplayName("VersionCompare getMinor")
    void getMinor() {
        assertEquals(2, VersionCompare.getMinor("1.2.3"));
        assertEquals(0, VersionCompare.getMinor("1.0.3"));
        assertEquals(0, VersionCompare.getMinor("1"));
    }

    @Test
    @DisplayName("VersionCompare getPatch")
    void getPatch() {
        assertEquals(3, VersionCompare.getPatch("1.2.3"));
        assertEquals(0, VersionCompare.getPatch("1.2"));
        assertEquals(0, VersionCompare.getPatch("1"));
    }

    @Test
    @DisplayName("VersionCompare isPrerelease")
    void isPrerelease() {
        assertTrue(VersionCompare.isPrerelease("1.0.0-alpha"));
        assertTrue(VersionCompare.isPrerelease("1.0.0-beta"));
        assertTrue(VersionCompare.isPrerelease("1.0.0-rc.1"));
        assertTrue(VersionCompare.isPrerelease("1.0.0-pre"));
        assertTrue(VersionCompare.isPrerelease("1.0.0-dev"));
        assertFalse(VersionCompare.isPrerelease("1.0.0"));
        assertFalse(VersionCompare.isPrerelease(null));
    }

    @Test
    @DisplayName("VersionCompare getPrereleaseType")
    void getPrereleaseType() {
        assertEquals(VersionCompare.PrereleaseType.ALPHA, VersionCompare.getPrereleaseType("1.0.0-alpha"));
        assertEquals(VersionCompare.PrereleaseType.BETA, VersionCompare.getPrereleaseType("1.0.0-beta"));
        assertEquals(VersionCompare.PrereleaseType.RELEASE_CANDIDATE, VersionCompare.getPrereleaseType("1.0.0-rc"));
        assertEquals(VersionCompare.PrereleaseType.PRE, VersionCompare.getPrereleaseType("1.0.0-pre"));
        assertEquals(VersionCompare.PrereleaseType.DEV, VersionCompare.getPrereleaseType("1.0.0-dev"));
        assertEquals(VersionCompare.PrereleaseType.NONE, VersionCompare.getPrereleaseType("1.0.0"));
    }

    @Test
    @DisplayName("VersionCompare PrereleaseType enum")
    void prereleaseTypeEnum() {
        assertEquals(6, VersionCompare.PrereleaseType.values().length);
        assertEquals("alpha", VersionCompare.PrereleaseType.ALPHA.getSuffix());
        assertEquals("beta", VersionCompare.PrereleaseType.BETA.getSuffix());
    }

    @Test
    @DisplayName("VersionCompare VersionRange contains")
    void versionRangeContains() {
        VersionCompare.VersionRange range = new VersionCompare.VersionRange("1.0.0", "2.0.0", true, true);

        assertTrue(range.contains("1.0.0"));
        assertTrue(range.contains("1.5.0"));
        assertTrue(range.contains("2.0.0"));
        assertFalse(range.contains("0.9.0"));
        assertFalse(range.contains("2.1.0"));
    }

    @Test
    @DisplayName("VersionCompare VersionRange exclusive bounds")
    void versionRangeExclusiveBounds() {
        VersionCompare.VersionRange range = new VersionCompare.VersionRange("1.0.0", "2.0.0", false, false);

        assertFalse(range.contains("1.0.0"));
        assertTrue(range.contains("1.5.0"));
        assertFalse(range.contains("2.0.0"));
    }

    @Test
    @DisplayName("VersionCompare VersionRange from")
    void versionRangeFrom() {
        VersionCompare.VersionRange range = VersionCompare.VersionRange.from("1.0.0");

        assertTrue(range.contains("1.0.0"));
        assertTrue(range.contains("100.0.0"));
        assertFalse(range.contains("0.9.0"));
    }

    @Test
    @DisplayName("VersionCompare VersionRange range")
    void versionRangeRange() {
        VersionCompare.VersionRange range = VersionCompare.VersionRange.range("1.0.0", "3.0.0");

        assertTrue(range.contains("1.0.0"));
        assertTrue(range.contains("2.0.0"));
        assertTrue(range.contains("3.0.0"));
        assertFalse(range.contains("0.9.0"));
        assertFalse(range.contains("3.1.0"));
    }

    @Test
    @DisplayName("VersionCompare findHighest")
    void findHighest() {
        List<String> versions = List.of("1.0.0", "3.0.0", "2.0.0");

        assertEquals("3.0.0", VersionCompare.findHighest(versions));
    }

    @Test
    @DisplayName("VersionCompare findHighest empty list")
    void findHighestEmpty() {
        assertNull(VersionCompare.findHighest(List.of()));
    }

    @Test
    @DisplayName("VersionCompare findLowest")
    void findLowest() {
        List<String> versions = List.of("1.0.0", "3.0.0", "2.0.0");

        assertEquals("1.0.0", VersionCompare.findLowest(versions));
    }

    @Test
    @DisplayName("VersionCompare sortVersions")
    void sortVersions() {
        List<String> versions = List.of("3.0.0", "1.0.0", "2.0.0");
        List<String> sorted = VersionCompare.sortVersions(versions);

        assertEquals("1.0.0", sorted.get(0));
        assertEquals("2.0.0", sorted.get(1));
        assertEquals("3.0.0", sorted.get(2));
    }

    @Test
    @DisplayName("VersionCompare sortVersionsDescending")
    void sortVersionsDescending() {
        List<String> versions = List.of("1.0.0", "3.0.0", "2.0.0");
        List<String> sorted = VersionCompare.sortVersionsDescending(versions);

        assertEquals("3.0.0", sorted.get(0));
        assertEquals("2.0.0", sorted.get(1));
        assertEquals("1.0.0", sorted.get(2));
    }

    @Test
    @DisplayName("VersionCompare null version")
    void nullVersion() {
        assertEquals(0, VersionCompare.compare(null, "0.0.0"));
        assertEquals(0, VersionCompare.compare("0.0.0", null));
        assertFalse(VersionCompare.isPrerelease(null));
        assertEquals(VersionCompare.PrereleaseType.NONE, VersionCompare.getPrereleaseType(null));
    }
}