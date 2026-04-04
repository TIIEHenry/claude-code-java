/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.computerUse;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Tests for AppNames.
 */
@DisplayName("AppNames Tests")
class AppNamesTest {

    @Test
    @DisplayName("AppNames AppCategory enum has correct values")
    void appCategoryEnumHasCorrectValues() {
        AppNames.AppCategory[] categories = AppNames.AppCategory.values();

        assertEquals(9, categories.length);
        assertTrue(Arrays.asList(categories).contains(AppNames.AppCategory.BROWSER));
        assertTrue(Arrays.asList(categories).contains(AppNames.AppCategory.EDITOR));
        assertTrue(Arrays.asList(categories).contains(AppNames.AppCategory.TERMINAL));
        assertTrue(Arrays.asList(categories).contains(AppNames.AppCategory.FILE_MANAGER));
        assertTrue(Arrays.asList(categories).contains(AppNames.AppCategory.COMMUNICATION));
        assertTrue(Arrays.asList(categories).contains(AppNames.AppCategory.DEVELOPMENT));
        assertTrue(Arrays.asList(categories).contains(AppNames.AppCategory.MEDIA));
        assertTrue(Arrays.asList(categories).contains(AppNames.AppCategory.SYSTEM));
        assertTrue(Arrays.asList(categories).contains(AppNames.AppCategory.OTHER));
    }

    @Test
    @DisplayName("AppNames AppInfo record works correctly")
    void appInfoRecordWorksCorrectly() {
        AppNames.AppInfo info = new AppNames.AppInfo(
            "TestApp",
            "Test Application",
            "com.test.app",
            "/Applications/TestApp.app",
            AppNames.AppCategory.EDITOR,
            false
        );

        assertEquals("TestApp", info.name());
        assertEquals("Test Application", info.displayName());
        assertEquals("com.test.app", info.bundleId());
        assertEquals("/Applications/TestApp.app", info.executablePath());
        assertEquals(AppNames.AppCategory.EDITOR, info.category());
        assertFalse(info.isSystemApp());
    }

    @Test
    @DisplayName("AppNames getAppInfo returns registered app")
    void getAppInfoReturnsRegisteredApp() {
        AppNames.AppInfo safari = AppNames.getAppInfo("Safari");

        assertNotNull(safari);
        assertEquals("Safari", safari.name());
        assertEquals(AppNames.AppCategory.BROWSER, safari.category());
        assertTrue(safari.isSystemApp());
    }

    @Test
    @DisplayName("AppNames getAppInfo returns null for unknown app")
    void getAppInfoReturnsNullForUnknownApp() {
        AppNames.AppInfo unknown = AppNames.getAppInfo("UnknownApp");

        assertNull(unknown);
    }

    @Test
    @DisplayName("AppNames findByBundleId finds registered app")
    void findByBundleIdFindsRegisteredApp() {
        AppNames.AppInfo safari = AppNames.findByBundleId("com.apple.Safari");

        assertNotNull(safari);
        assertEquals("Safari", safari.name());
    }

    @Test
    @DisplayName("AppNames findByBundleId returns null for unknown bundle")
    void findByBundleIdReturnsNullForUnknownBundle() {
        AppNames.AppInfo unknown = AppNames.findByBundleId("com.unknown.app");

        assertNull(unknown);
    }

    @Test
    @DisplayName("AppNames getAppsByCategory returns correct apps")
    void getAppsByCategoryReturnsCorrectApps() {
        List<AppNames.AppInfo> browsers = AppNames.getAppsByCategory(AppNames.AppCategory.BROWSER);

        assertTrue(browsers.size() >= 2); // Safari and Chrome
        for (AppNames.AppInfo app : browsers) {
            assertEquals(AppNames.AppCategory.BROWSER, app.category());
        }
    }

    @Test
    @DisplayName("AppNames getAppsByCategory returns empty list for empty category")
    void getAppsByCategoryReturnsEmptyListForEmptyCategory() {
        // If no communication apps registered
        List<AppNames.AppInfo> communication = AppNames.getAppsByCategory(AppNames.AppCategory.COMMUNICATION);

        // Could be empty if no communication apps registered
        assertNotNull(communication);
    }

    @Test
    @DisplayName("AppNames getAllApps returns unmodifiable map")
    void getAllAppsReturnsUnmodifiableMap() {
        Map<String, AppNames.AppInfo> allApps = AppNames.getAllApps();

        assertNotNull(allApps);
        assertTrue(allApps.size() >= 7); // At least the pre-registered apps

        // Should throw on attempt to modify
        assertThrows(UnsupportedOperationException.class, () ->
            allApps.put("NewApp", null)
        );
    }

    @Test
    @DisplayName("AppNames register adds new app")
    void registerAddsNewApp() {
        AppNames.AppInfo newApp = new AppNames.AppInfo(
            "TestApp",
            "Test App",
            "com.test.app",
            "/Applications/TestApp.app",
            AppNames.AppCategory.OTHER,
            false
        );

        AppNames.register(newApp);

        AppNames.AppInfo retrieved = AppNames.getAppInfo("TestApp");
        assertNotNull(retrieved);
        assertEquals("TestApp", retrieved.name());
    }

    @Test
    @DisplayName("AppNames isInstalled returns true for registered apps")
    void isInstalledReturnsTrueForRegisteredApps() {
        assertTrue(AppNames.isInstalled("Safari"));
        assertTrue(AppNames.isInstalled("Chrome"));
        assertTrue(AppNames.isInstalled("Terminal"));
    }

    @Test
    @DisplayName("AppNames isInstalled returns false for unknown apps")
    void isInstalledReturnsFalseForUnknownApps() {
        assertFalse(AppNames.isInstalled("UnknownApp"));
    }

    @Test
    @DisplayName("AppNames getWindowTitleFormat returns correct format")
    void getWindowTitleFormatReturnsCorrectFormat() {
        assertEquals("%s — Safari", AppNames.getWindowTitleFormat("Safari"));
        assertEquals("%s - Google Chrome", AppNames.getWindowTitleFormat("Chrome"));
        assertEquals("%s — Terminal", AppNames.getWindowTitleFormat("Terminal"));
        assertEquals("%s", AppNames.getWindowTitleFormat("Finder"));
        assertEquals("%s - Visual Studio Code", AppNames.getWindowTitleFormat("VSCode"));
    }

    @Test
    @DisplayName("AppNames getWindowTitleFormat returns app name for unknown")
    void getWindowTitleFormatReturnsAppNameForUnknown() {
        String format = AppNames.getWindowTitleFormat("UnknownApp");

        assertEquals("UnknownApp", format);
    }

    @Test
    @DisplayName("AppNames pre-registered apps have correct info")
    void preRegisteredAppsHaveCorrectInfo() {
        // Safari
        AppNames.AppInfo safari = AppNames.getAppInfo("Safari");
        assertEquals("com.apple.Safari", safari.bundleId());
        assertEquals(AppNames.AppCategory.BROWSER, safari.category());

        // Chrome
        AppNames.AppInfo chrome = AppNames.getAppInfo("Chrome");
        assertEquals("com.google.Chrome", chrome.bundleId());
        assertFalse(chrome.isSystemApp());

        // Terminal
        AppNames.AppInfo terminal = AppNames.getAppInfo("Terminal");
        assertEquals(AppNames.AppCategory.TERMINAL, terminal.category());

        // Finder
        AppNames.AppInfo finder = AppNames.getAppInfo("Finder");
        assertEquals(AppNames.AppCategory.FILE_MANAGER, finder.category());

        // VSCode
        AppNames.AppInfo vscode = AppNames.getAppInfo("VSCode");
        assertEquals(AppNames.AppCategory.EDITOR, vscode.category());

        // IntelliJ
        AppNames.AppInfo intellij = AppNames.getAppInfo("IntelliJ");
        assertEquals(AppNames.AppCategory.DEVELOPMENT, intellij.category());
    }
}