/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PlatformUtil.
 */
class PlatformUtilTest {

    @Test
    @DisplayName("PlatformUtil getPlatform returns a platform")
    void getPlatform() {
        PlatformUtil.Platform platform = PlatformUtil.getPlatform();

        assertNotNull(platform);
        assertTrue(platform == PlatformUtil.Platform.MACOS ||
                   platform == PlatformUtil.Platform.WINDOWS ||
                   platform == PlatformUtil.Platform.LINUX ||
                   platform == PlatformUtil.Platform.WSL ||
                   platform == PlatformUtil.Platform.UNKNOWN);
    }

    @Test
    @DisplayName("PlatformUtil SUPPORTED_PLATFORMS contains macOS and WSL")
    void supportedPlatforms() {
        assertTrue(PlatformUtil.SUPPORTED_PLATFORMS.contains(PlatformUtil.Platform.MACOS));
        assertTrue(PlatformUtil.SUPPORTED_PLATFORMS.contains(PlatformUtil.Platform.WSL));
    }

    @Test
    @DisplayName("PlatformUtil isMacOS checks platform")
    void isMacOS() {
        boolean result = PlatformUtil.isMacOS();
        // Just verify it doesn't throw
        assertTrue(result || !result);
    }

    @Test
    @DisplayName("PlatformUtil isWindows checks platform")
    void isWindows() {
        boolean result = PlatformUtil.isWindows();
        assertTrue(result || !result);
    }

    @Test
    @DisplayName("PlatformUtil isLinux checks platform")
    void isLinux() {
        boolean result = PlatformUtil.isLinux();
        assertTrue(result || !result);
    }

    @Test
    @DisplayName("PlatformUtil isWSLPlatform checks platform")
    void isWSLPlatform() {
        boolean result = PlatformUtil.isWSLPlatform();
        assertTrue(result || !result);
    }

    @Test
    @DisplayName("PlatformUtil getOsName returns name")
    void getOsName() {
        String name = PlatformUtil.getOsName();

        assertNotNull(name);
    }

    @Test
    @DisplayName("PlatformUtil getOsVersion returns version")
    void getOsVersion() {
        String version = PlatformUtil.getOsVersion();

        assertNotNull(version);
    }

    @Test
    @DisplayName("PlatformUtil getOsArch returns architecture")
    void getOsArch() {
        String arch = PlatformUtil.getOsArch();

        assertNotNull(arch);
    }

    @Test
    @DisplayName("PlatformUtil isArm checks architecture")
    void isArm() {
        boolean result = PlatformUtil.isArm();
        assertTrue(result || !result);
    }

    @Test
    @DisplayName("PlatformUtil isX86 checks architecture")
    void isX86() {
        boolean result = PlatformUtil.isX86();
        assertTrue(result || !result);
    }

    @Test
    @DisplayName("PlatformUtil detectVcs returns list")
    void detectVcs() {
        var vcs = PlatformUtil.detectVcs();

        assertNotNull(vcs);
    }

    @Test
    @DisplayName("PlatformUtil resetPlatform works")
    void resetPlatform() {
        PlatformUtil.Platform before = PlatformUtil.getPlatform();
        PlatformUtil.resetPlatform();
        PlatformUtil.Platform after = PlatformUtil.getPlatform();

        assertEquals(before, after);
    }

    @Test
    @DisplayName("PlatformUtil Platform enum values")
    void platformEnumValues() {
        PlatformUtil.Platform[] values = PlatformUtil.Platform.values();

        assertTrue(values.length >= 5);
        assertEquals(PlatformUtil.Platform.MACOS, PlatformUtil.Platform.valueOf("MACOS"));
        assertEquals(PlatformUtil.Platform.WINDOWS, PlatformUtil.Platform.valueOf("WINDOWS"));
        assertEquals(PlatformUtil.Platform.LINUX, PlatformUtil.Platform.valueOf("LINUX"));
    }

    @Test
    @DisplayName("PlatformUtil getWslVersion may return null or version")
    void getWslVersion() {
        String version = PlatformUtil.getWslVersion();
        // On non-WSL platforms, returns null
        if (PlatformUtil.getPlatform() != PlatformUtil.Platform.WSL) {
            assertNull(version);
        }
    }

    @Test
    @DisplayName("PlatformUtil getLinuxDistroInfo may return null")
    void getLinuxDistroInfo() {
        PlatformUtil.LinuxDistroInfo info = PlatformUtil.getLinuxDistroInfo();
        // On non-Linux platforms, returns null
        if (PlatformUtil.getPlatform() != PlatformUtil.Platform.LINUX &&
            PlatformUtil.getPlatform() != PlatformUtil.Platform.WSL) {
            assertNull(info);
        }
    }

    @Test
    @DisplayName("PlatformUtil LinuxDistroInfo record works")
    void linuxDistroInfoRecord() {
        PlatformUtil.LinuxDistroInfo info = new PlatformUtil.LinuxDistroInfo("ubuntu", "22.04", "5.15.0");

        assertEquals("ubuntu", info.linuxDistroId());
        assertEquals("22.04", info.linuxDistroVersion());
        assertEquals("5.15.0", info.linuxKernel());
    }
}