/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code Apple Terminal backup utilities
 */
package com.anthropic.claudecode.utils;

import java.time.Instant;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Apple Terminal backup and recovery utilities.
 * Handles backup/restore of Terminal.app preferences on macOS.
 */
public final class AppleTerminalBackup {
    private AppleTerminalBackup() {}

    private static final String TERMINAL_PLIST_NAME = "com.apple.Terminal.plist";

    /**
     * Terminal restore result.
     */
    public sealed interface RestoreResult permits Restored, NoBackup, Failed {
        String status();
    }

    public record Restored() implements RestoreResult {
        @Override public String status() { return "restored"; }
    }

    public record NoBackup() implements RestoreResult {
        @Override public String status() { return "no_backup"; }
    }

    public record Failed(String backupPath) implements RestoreResult {
        @Override public String status() { return "failed"; }
    }

    /**
     * Get the Terminal plist path.
     */
    public static Path getTerminalPlistPath() {
        return Paths.get(System.getProperty("user.home"),
                "Library", "Preferences", TERMINAL_PLIST_NAME);
    }

    /**
     * Get the backup plist path.
     */
    public static Path getBackupPlistPath() {
        return Paths.get(getTerminalPlistPath().toString() + ".bak");
    }

    /**
     * Mark terminal setup in progress in config.
     */
    public static void markTerminalSetupInProgress(String backupPath) {
        try {
            Map<String, Object> config = ConfigManager.loadGlobalConfig();
            config.put("appleTerminalSetupInProgress", true);
            config.put("appleTerminalBackupPath", backupPath);
            ConfigManager.saveGlobalConfig(config);
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Mark terminal setup complete.
     */
    public static void markTerminalSetupComplete() {
        try {
            Map<String, Object> config = ConfigManager.loadGlobalConfig();
            config.put("appleTerminalSetupInProgress", false);
            ConfigManager.saveGlobalConfig(config);
        } catch (Exception e) {
            // Ignore
        }
    }

    /**
     * Get terminal recovery info from config.
     */
    private static TerminalRecoveryInfo getTerminalRecoveryInfo() {
        try {
            Map<String, Object> config = ConfigManager.loadGlobalConfig();
            boolean inProgress = Boolean.TRUE.equals(config.get("appleTerminalSetupInProgress"));
            String backupPath = (String) config.get("appleTerminalBackupPath");
            return new TerminalRecoveryInfo(inProgress, backupPath);
        } catch (Exception e) {
            return new TerminalRecoveryInfo(false, null);
        }
    }

    private record TerminalRecoveryInfo(boolean inProgress, String backupPath) {}

    /**
     * Backup Terminal preferences.
     * Uses 'defaults export' command on macOS.
     */
    public static CompletableFuture<Path> backupTerminalPreferences() {
        return CompletableFuture.supplyAsync(() -> {
            if (!PlatformUtil.isMacOS()) {
                return null;
            }

            Path plistPath = getTerminalPlistPath();
            Path backupPath = getBackupPlistPath();

            try {
                // Check if plist exists
                if (!Files.exists(plistPath)) {
                    return null;
                }

                // Export current Terminal preferences
                ProcessBuilder pb = new ProcessBuilder("defaults", "export",
                        "com.apple.Terminal", plistPath.toString());
                pb.redirectErrorStream(true);
                Process p = pb.start();
                if (p.waitFor() != 0) {
                    return null;
                }

                // Create backup
                pb = new ProcessBuilder("defaults", "export",
                        "com.apple.Terminal", backupPath.toString());
                p = pb.start();
                if (p.waitFor() != 0) {
                    return null;
                }

                markTerminalSetupInProgress(backupPath.toString());
                return backupPath;

            } catch (Exception e) {
                return null;
            }
        });
    }

    /**
     * Check and restore Terminal backup if needed.
     */
    public static CompletableFuture<RestoreResult> checkAndRestoreTerminalBackup() {
        return CompletableFuture.supplyAsync(() -> {
            if (!PlatformUtil.isMacOS()) {
                return new NoBackup();
            }

            TerminalRecoveryInfo info = getTerminalRecoveryInfo();
            if (!info.inProgress()) {
                return new NoBackup();
            }

            if (info.backupPath() == null) {
                markTerminalSetupComplete();
                return new NoBackup();
            }

            Path backupPath = Paths.get(info.backupPath());
            if (!Files.exists(backupPath)) {
                markTerminalSetupComplete();
                return new NoBackup();
            }

            try {
                // Import backup
                ProcessBuilder pb = new ProcessBuilder("defaults", "import",
                        "com.apple.Terminal", backupPath.toString());
                pb.redirectErrorStream(true);
                Process p = pb.start();
                if (p.waitFor() != 0) {
                    return new Failed(info.backupPath());
                }

                // Restart cfprefsd
                pb = new ProcessBuilder("killall", "cfprefsd");
                p = pb.start();
                p.waitFor();

                markTerminalSetupComplete();
                return new Restored();

            } catch (Exception e) {
                markTerminalSetupComplete();
                return new Failed(info.backupPath());
            }
        });
    }
}