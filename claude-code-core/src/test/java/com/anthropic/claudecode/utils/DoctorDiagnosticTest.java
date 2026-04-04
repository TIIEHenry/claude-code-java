/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DoctorDiagnostic.
 */
class DoctorDiagnosticTest {

    @Test
    @DisplayName("DoctorDiagnostic InstallationType enum")
    void installationTypeEnum() {
        assertEquals(6, DoctorDiagnostic.InstallationType.values().length);
        assertEquals("npm-global", DoctorDiagnostic.InstallationType.NPM_GLOBAL.getValue());
        assertEquals("npm-local", DoctorDiagnostic.InstallationType.NPM_LOCAL.getValue());
        assertEquals("native", DoctorDiagnostic.InstallationType.NATIVE.getValue());
        assertEquals("development", DoctorDiagnostic.InstallationType.DEVELOPMENT.getValue());
        assertEquals("unknown", DoctorDiagnostic.InstallationType.UNKNOWN.getValue());
    }

    @Test
    @DisplayName("DoctorDiagnostic DiagnosticInfo record")
    void diagnosticInfoRecord() {
        DoctorDiagnostic.DiagnosticInfo info = new DoctorDiagnostic.DiagnosticInfo(
            DoctorDiagnostic.InstallationType.NATIVE,
            "1.0.0",
            "/usr/local/bin",
            "claude",
            "native",
            "enabled",
            true,
            List.of(),
            List.of(),
            "npm",
            new DoctorDiagnostic.RipgrepStatus(true, "system", "/usr/bin/rg")
        );

        assertEquals(DoctorDiagnostic.InstallationType.NATIVE, info.installationType());
        assertEquals("1.0.0", info.version());
        assertEquals("/usr/local/bin", info.installationPath());
    }

    @Test
    @DisplayName("DoctorDiagnostic Installation record")
    void installationRecord() {
        DoctorDiagnostic.Installation install = new DoctorDiagnostic.Installation(
            "npm-local", "/path/to/install"
        );
        assertEquals("npm-local", install.type());
        assertEquals("/path/to/install", install.path());
    }

    @Test
    @DisplayName("DoctorDiagnostic Warning record")
    void warningRecord() {
        DoctorDiagnostic.Warning warning = new DoctorDiagnostic.Warning(
            "PATH not set", "Add to PATH"
        );
        assertEquals("PATH not set", warning.issue());
        assertEquals("Add to PATH", warning.fix());
    }

    @Test
    @DisplayName("DoctorDiagnostic RipgrepStatus record")
    void ripgrepStatusRecord() {
        DoctorDiagnostic.RipgrepStatus status = new DoctorDiagnostic.RipgrepStatus(
            true, "system", "/usr/bin/rg"
        );
        assertTrue(status.working());
        assertEquals("system", status.mode());
        assertEquals("/usr/bin/rg", status.systemPath());
    }

    @Test
    @DisplayName("DoctorDiagnostic getCurrentInstallationType returns value")
    void getCurrentInstallationType() {
        DoctorDiagnostic.InstallationType type = DoctorDiagnostic.getCurrentInstallationType();
        assertNotNull(type);
    }

    @Test
    @DisplayName("DoctorDiagnostic getInvokedBinary returns string")
    void getInvokedBinary() {
        String binary = DoctorDiagnostic.getInvokedBinary();
        assertNotNull(binary);
    }

    @Test
    @DisplayName("DoctorDiagnostic detectMultipleInstallations returns list")
    void detectMultipleInstallations() {
        List<DoctorDiagnostic.Installation> installations =
            DoctorDiagnostic.detectMultipleInstallations();
        assertNotNull(installations);
    }

    @Test
    @DisplayName("DoctorDiagnostic detectConfigurationIssues returns list")
    void detectConfigurationIssues() {
        List<DoctorDiagnostic.Warning> warnings =
            DoctorDiagnostic.detectConfigurationIssues(DoctorDiagnostic.InstallationType.NATIVE);
        assertNotNull(warnings);
    }

    @Test
    @DisplayName("DoctorDiagnostic getDoctorDiagnostic returns info")
    void getDoctorDiagnostic() {
        DoctorDiagnostic.DiagnosticInfo info = DoctorDiagnostic.getDoctorDiagnostic();
        assertNotNull(info);
        assertNotNull(info.installationType());
        assertNotNull(info.version());
    }

    @Test
    @DisplayName("DoctorDiagnostic formatDiagnostic returns string")
    void formatDiagnostic() {
        DoctorDiagnostic.DiagnosticInfo info = DoctorDiagnostic.getDoctorDiagnostic();
        String report = DoctorDiagnostic.formatDiagnostic(info);

        assertNotNull(report);
        assertTrue(report.contains("Claude Code Diagnostic Report"));
        assertTrue(report.contains("Installation Type:"));
        assertTrue(report.contains("Version:"));
    }

    @Test
    @DisplayName("DoctorDiagnostic formatDiagnostic with warnings")
    void formatDiagnosticWithWarnings() {
        DoctorDiagnostic.DiagnosticInfo info = new DoctorDiagnostic.DiagnosticInfo(
            DoctorDiagnostic.InstallationType.NATIVE,
            "1.0.0",
            "/path",
            "claude",
            "native",
            "enabled",
            true,
            List.of(),
            List.of(new DoctorDiagnostic.Warning("Test issue", "Test fix")),
            null,
            new DoctorDiagnostic.RipgrepStatus(true, "system", null)
        );

        String report = DoctorDiagnostic.formatDiagnostic(info);
        assertTrue(report.contains("Warnings:"));
        assertTrue(report.contains("Test issue"));
        assertTrue(report.contains("Test fix"));
    }
}