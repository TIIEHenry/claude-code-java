/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code IDE path conversion utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.*;

/**
 * Path conversion utilities for IDE communication.
 * Handles conversions between Claude's environment and the IDE's environment.
 */
public final class IdePathConversion {
    private IdePathConversion() {}

    /**
     * IDE path converter interface.
     */
    public interface IDEPathConverter {
        /**
         * Convert path from IDE format to Claude's local format.
         */
        String toLocalPath(String idePath);

        /**
         * Convert path from Claude's local format to IDE format.
         */
        String toIDEPath(String localPath);
    }

    /**
     * Converter for Windows IDE + WSL Claude scenario.
     */
    public static class WindowsToWSLConverter implements IDEPathConverter {
        private final String wslDistroName;

        public WindowsToWSLConverter(String wslDistroName) {
            this.wslDistroName = wslDistroName;
        }

        @Override
        public String toLocalPath(String windowsPath) {
            if (windowsPath == null || windowsPath.isEmpty()) return windowsPath;

            // Check if this is a path from a different WSL distro
            if (wslDistroName != null) {
                Pattern wslUncPattern = Pattern.compile("^\\\\\\\\wsl(?:\\.localhost|\\$)\\\\([^\\\\]+)(.*)$");
                Matcher matcher = wslUncPattern.matcher(windowsPath);
                if (matcher.find() && !matcher.group(1).equals(wslDistroName)) {
                    // Different distro - return original path
                    return windowsPath;
                }
            }

            try {
                // Use wslpath to convert Windows paths to WSL paths
                ProcessBuilder pb = new ProcessBuilder("wslpath", "-u", windowsPath);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line);
                    }
                }

                process.waitFor(5, TimeUnit.SECONDS);
                String result = output.toString().trim();
                if (!result.isEmpty()) {
                    return result;
                }
            } catch (Exception e) {
                // Fall through to manual conversion
            }

            // Manual conversion fallback
            String result = windowsPath.replace("\\", "/");
            // Convert C: to /mnt/c
            if (result.length() >= 2 && result.charAt(1) == ':') {
                char driveLetter = Character.toLowerCase(result.charAt(0));
                result = "/mnt/" + driveLetter + result.substring(2);
            }
            return result;
        }

        @Override
        public String toIDEPath(String wslPath) {
            if (wslPath == null || wslPath.isEmpty()) return wslPath;

            try {
                // Use wslpath to convert WSL paths to Windows paths
                ProcessBuilder pb = new ProcessBuilder("wslpath", "-w", wslPath);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line);
                    }
                }

                process.waitFor(5, TimeUnit.SECONDS);
                String result = output.toString().trim();
                if (!result.isEmpty()) {
                    return result;
                }
            } catch (Exception e) {
                // Fall through to return original
            }

            return wslPath;
        }
    }

    /**
     * Check if distro names match for WSL UNC paths.
     */
    public static boolean checkWSLDistroMatch(String windowsPath, String wslDistroName) {
        Pattern wslUncPattern = Pattern.compile("^\\\\\\\\wsl(?:\\.localhost|\\$)\\\\([^\\\\]+)(.*)$");
        Matcher matcher = wslUncPattern.matcher(windowsPath);
        if (matcher.find()) {
            return matcher.group(1).equals(wslDistroName);
        }
        return true; // Not a WSL UNC path, so no distro mismatch
    }

    /**
     * Identity converter (no conversion needed).
     */
    public static class IdentityConverter implements IDEPathConverter {
        @Override
        public String toLocalPath(String idePath) {
            return idePath;
        }

        @Override
        public String toIDEPath(String localPath) {
            return localPath;
        }
    }
}