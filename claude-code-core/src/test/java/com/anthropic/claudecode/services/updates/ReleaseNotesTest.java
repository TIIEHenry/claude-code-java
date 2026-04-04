/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.updates;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ReleaseNotes.
 */
class ReleaseNotesTest {

    private ReleaseNotes releaseNotes;

    @BeforeEach
    void setUp() {
        releaseNotes = new ReleaseNotes();
    }

    @Test
    @DisplayName("ReleaseNotes ChangeType enum values")
    void changeTypeEnum() {
        ReleaseNotes.ChangeType[] types = ReleaseNotes.ChangeType.values();
        assertEquals(9, types.length);
        assertEquals("✨", ReleaseNotes.ChangeType.FEATURE.getSymbol());
        assertEquals("🐛", ReleaseNotes.ChangeType.FIX.getSymbol());
        assertEquals("⚡", ReleaseNotes.ChangeType.IMPROVEMENT.getSymbol());
    }

    @Test
    @DisplayName("ReleaseNotes ReleaseChange format")
    void releaseChangeFormat() {
        ReleaseNotes.ReleaseChange change = new ReleaseNotes.ReleaseChange(
            ReleaseNotes.ChangeType.FEATURE,
            "Added new tool",
            "core",
            "#123"
        );

        String formatted = change.format();
        assertTrue(formatted.contains("[✨]"));
        assertTrue(formatted.contains("core:"));
        assertTrue(formatted.contains("Added new tool"));
        assertTrue(formatted.contains("(#123)"));
    }

    @Test
    @DisplayName("ReleaseNotes ReleaseChange format without component")
    void releaseChangeFormatNoComponent() {
        ReleaseNotes.ReleaseChange change = new ReleaseNotes.ReleaseChange(
            ReleaseNotes.ChangeType.FIX,
            "Fixed bug",
            null,
            null
        );

        String formatted = change.format();
        assertTrue(formatted.contains("[🐛]"));
        assertTrue(formatted.contains("Fixed bug"));
    }

    @Test
    @DisplayName("ReleaseNotes ReleaseEntry formatFull")
    void releaseEntryFormatFull() {
        ReleaseNotes.ReleaseEntry entry = new ReleaseNotes.ReleaseEntry(
            "1.0.0",
            LocalDate.of(2024, 1, 15),
            "First Release",
            "Initial release summary",
            List.of(new ReleaseNotes.ReleaseChange(ReleaseNotes.ChangeType.FEATURE, "New feature", null, null)),
            List.of("Known issue 1"),
            false,
            "https://download.example.com",
            "https://release.example.com"
        );

        String formatted = entry.formatFull();
        assertTrue(formatted.contains("# First Release"));
        assertTrue(formatted.contains("Version: 1.0.0"));
        assertTrue(formatted.contains("Initial release summary"));
        assertTrue(formatted.contains("### Changes"));
        assertTrue(formatted.contains("### Known Issues"));
    }

    @Test
    @DisplayName("ReleaseNotes ReleaseEntry formatBrief")
    void releaseEntryFormatBrief() {
        ReleaseNotes.ReleaseEntry entry = new ReleaseNotes.ReleaseEntry(
            "1.0.0",
            LocalDate.of(2024, 1, 15),
            "First Release",
            "Summary here",
            List.of(),
            List.of(),
            false,
            null,
            null
        );

        String brief = entry.formatBrief();
        assertTrue(brief.contains("1.0.0"));
        assertTrue(brief.contains("Summary here"));
    }

    @Test
    @DisplayName("ReleaseNotes addRelease and getRelease")
    void addAndGetRelease() {
        ReleaseNotes.ReleaseEntry entry = new ReleaseNotes.ReleaseEntry(
            "2.0.0", LocalDate.now(), "v2", "Summary", List.of(), List.of(), false, null, null
        );

        releaseNotes.addRelease(entry);

        ReleaseNotes.ReleaseEntry found = releaseNotes.getRelease("2.0.0");
        assertNotNull(found);
        assertEquals("2.0.0", found.version());
    }

    @Test
    @DisplayName("ReleaseNotes getRelease returns null for unknown")
    void getReleaseUnknown() {
        assertNull(releaseNotes.getRelease("999.0.0"));
    }

    @Test
    @DisplayName("ReleaseNotes getLatestRelease returns null when empty")
    void getLatestReleaseEmpty() {
        assertNull(releaseNotes.getLatestRelease());
    }

    @Test
    @DisplayName("ReleaseNotes getLatestRelease ignores prereleases")
    void getLatestReleaseIgnoresPrereleases() {
        releaseNotes.addRelease(new ReleaseNotes.ReleaseEntry(
            "2.0.0-beta", LocalDate.now(), "Beta", "Beta", List.of(), List.of(), true, null, null
        ));
        releaseNotes.addRelease(new ReleaseNotes.ReleaseEntry(
            "1.0.0", LocalDate.now().minusDays(1), "v1", "v1", List.of(), List.of(), false, null, null
        ));

        ReleaseNotes.ReleaseEntry latest = releaseNotes.getLatestRelease();
        assertNotNull(latest);
        assertEquals("1.0.0", latest.version());
    }

    @Test
    @DisplayName("ReleaseNotes getAllReleases")
    void getAllReleases() {
        releaseNotes.addRelease(new ReleaseNotes.ReleaseEntry(
            "1.0.0", LocalDate.now(), "v1", "v1", List.of(), List.of(), false, null, null
        ));
        releaseNotes.addRelease(new ReleaseNotes.ReleaseEntry(
            "2.0.0", LocalDate.now(), "v2", "v2", List.of(), List.of(), false, null, null
        ));

        List<ReleaseNotes.ReleaseEntry> all = releaseNotes.getAllReleases();
        assertEquals(2, all.size());
    }

    @Test
    @DisplayName("ReleaseNotes getReleasesSince")
    void getReleasesSince() {
        releaseNotes.addRelease(new ReleaseNotes.ReleaseEntry(
            "1.0.0", LocalDate.now().minusDays(2), "v1", "v1", List.of(), List.of(), false, null, null
        ));
        releaseNotes.addRelease(new ReleaseNotes.ReleaseEntry(
            "1.1.0", LocalDate.now().minusDays(1), "v1.1", "v1.1", List.of(), List.of(), false, null, null
        ));
        releaseNotes.addRelease(new ReleaseNotes.ReleaseEntry(
            "2.0.0", LocalDate.now(), "v2", "v2", List.of(), List.of(), false, null, null
        ));

        List<ReleaseNotes.ReleaseEntry> since = releaseNotes.getReleasesSince("1.0.0");
        assertEquals(2, since.size());
    }

    @Test
    @DisplayName("ReleaseNotes formatSummary")
    void formatSummary() {
        releaseNotes.addRelease(new ReleaseNotes.ReleaseEntry(
            "1.0.0", LocalDate.now(), "v1", "First release", List.of(), List.of(), false, null, null
        ));

        String summary = releaseNotes.formatSummary(5);
        assertTrue(summary.contains("## Recent Releases"));
        assertTrue(summary.contains("1.0.0"));
    }
}