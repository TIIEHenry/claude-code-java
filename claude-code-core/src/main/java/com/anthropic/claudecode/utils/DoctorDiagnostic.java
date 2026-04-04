/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code doctor diagnostic utilities
 */
package com.anthropic.claudecode.utils;

import java.nio.file.*;
import java.util.*;

/**
 * Doctor diagnostic utilities for checking installation health.
 */
public final class DoctorDiagnostic {
    private DoctorDiagnostic() {}

    /**
     * Installation type enum.
     */
    public enum InstallationType {
        NPM_GLOBAL("npm-global"),
        NPM_LOCAL("npm-local"),
        NATIVE("native"),
        PACKAGE_MANAGER("package-manager"),
        DEVELOPMENT("development"),
        UNKNOWN("unknown");

        private final String value;

        InstallationType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * Diagnostic info record.
     */
    public record DiagnosticInfo(
            InstallationType installationType,
            String version,
            String installationPath,
            String invokedBinary,
            String configInstallMethod,
            String autoUpdates,
            Boolean hasUpdatePermissions,
            List<Installation> multipleInstallations,
            List<Warning> warnings,
            String packageManager,
            RipgrepStatus ripgrepStatus
    ) {}

    /**
     * Installation record.
     */
    public record Installation(String type, String path) {}

    /**
     * Warning record.
     */
    public record Warning(String issue, String fix) {}

    /**
     * Ripgrep status record.
     */
    public record RipgrepStatus(boolean working, String mode, String systemPath) {}

    /**
     * Get current installation type.
     */
    public static InstallationType getCurrentInstallationType() {
        String nodeEnv = System.getenv("NODE_ENV");
        if ("development".equals(nodeEnv)) {
            return InstallationType.DEVELOPMENT;
        }

        // Check if running in bundled mode
        String execPath = System.getProperty("java.class.path", "");
        if (execPath.contains(".local") || execPath.contains("claude-code")) {
            return InstallationType.NATIVE;
        }

        // Check for npm global
        String path = System.getenv("PATH");
        if (path != null) {
            if (path.contains("/usr/local/lib/node_modules") ||
                path.contains("/usr/lib/node_modules") ||
                path.contains("/opt/homebrew/lib/node_modules")) {
                return InstallationType.NPM_GLOBAL;
            }
        }

        return InstallationType.UNKNOWN;
    }

    /**
     * Get invoked binary path.
     */
    public static String getInvokedBinary() {
        return ProcessHandle.current().info().command().orElse("unknown");
    }

    /**
     * Detect multiple installations.
     */
    public static List<Installation> detectMultipleInstallations() {
        List<Installation> installations = new ArrayList<>();
        String home = System.getProperty("user.home");

        // Check for local installation
        Path localPath = Paths.get(home, ".claude", "local");
        if (Files.exists(localPath)) {
            installations.add(new Installation("npm-local", localPath.toString()));
        }

        // Check for native installation
        Path nativeBinPath = Paths.get(home, ".local", "bin", "claude");
        if (Files.exists(nativeBinPath)) {
            installations.add(new Installation("native", nativeBinPath.toString()));
        }

        return installations;
    }

    /**
     * Detect configuration issues.
     */
    public static List<Warning> detectConfigurationIssues(InstallationType type) {
        List<Warning> warnings = new ArrayList<>();

        // Check PATH for native installations
        if (type == InstallationType.NATIVE) {
            String path = System.getenv("PATH");
            String home = System.getProperty("user.home");
            String localBinPath = Paths.get(home, ".local", "bin").toString();

            if (path == null || !path.contains(localBinPath)) {
                warnings.add(new Warning(
                        "Native installation exists but ~/.local/bin is not in your PATH",
                        "Run: echo 'export PATH=\"$HOME/.local/bin:$PATH\"' >> ~/.bashrc then source ~/.bashrc"
                ));
            }
        }

        // Check for leftover npm installations
        if (type == InstallationType.NATIVE) {
            List<Installation> multiple = detectMultipleInstallations();
            for (Installation install : multiple) {
                if (install.type().contains("npm")) {
                    warnings.add(new Warning(
                            "Leftover npm installation at " + install.path(),
                            "Run: npm -g uninstall @anthropic-ai/claude-code"
                    ));
                }
            }
        }

        return warnings;
    }

    /**
     * Get doctor diagnostic.
     */
    public static DiagnosticInfo getDoctorDiagnostic() {
        InstallationType installationType = getCurrentInstallationType();
        String version = "1.0.0"; // Should be set from build
        String invokedBinary = getInvokedBinary();
        List<Installation> multipleInstallations = detectMultipleInstallations();
        List<Warning> warnings = detectConfigurationIssues(installationType);

        return new DiagnosticInfo(
                installationType,
                version,
                installationType.getValue(),
                invokedBinary,
                "not set",
                "enabled",
                null,
                multipleInstallations,
                warnings,
                null,
                new RipgrepStatus(true, "system", null)
        );
    }

    /**
     * Format diagnostic info for display.
     */
    public static String formatDiagnostic(DiagnosticInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append("Claude Code Diagnostic Report\n");
        sb.append("============================\n\n");
        sb.append("Installation Type: ").append(info.installationType().getValue()).append("\n");
        sb.append("Version: ").append(info.version()).append("\n");
        sb.append("Invoked Binary: ").append(info.invokedBinary()).append("\n");
        sb.append("Auto Updates: ").append(info.autoUpdates()).append("\n\n");

        if (!info.multipleInstallations().isEmpty()) {
            sb.append("Multiple Installations Found:\n");
            for (Installation install : info.multipleInstallations()) {
                sb.append("  - ").append(install.type()).append(": ").append(install.path()).append("\n");
            }
            sb.append("\n");
        }

        if (!info.warnings().isEmpty()) {
            sb.append("Warnings:\n");
            for (Warning warning : info.warnings()) {
                sb.append("  Issue: ").append(warning.issue()).append("\n");
                sb.append("  Fix: ").append(warning.fix()).append("\n\n");
            }
        }

        return sb.toString();
    }
}