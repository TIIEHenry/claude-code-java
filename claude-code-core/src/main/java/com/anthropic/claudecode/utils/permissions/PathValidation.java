/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/permissions/pathValidation.ts
 */
package com.anthropic.claudecode.utils.permissions;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Path validation utilities for permission checks.
 */
public final class PathValidation {
    private PathValidation() {}

    private static final int MAX_DIRS_TO_LIST = 5;
    private static final Pattern GLOB_PATTERN_REGEX = Pattern.compile("[*?\\[\\]{}]");
    private static final Pattern WINDOWS_DRIVE_ROOT_REGEX = Pattern.compile("^[A-Za-z]:/?(\\\\)?$");
    private static final Pattern WINDOWS_DRIVE_CHILD_REGEX = Pattern.compile("^[A-Za-z]:/(\\\\)?[^/\\\\]+$");

    /**
     * File operation type enum.
     */
    public enum FileOperationType {
        READ, WRITE, CREATE
    }

    /**
     * Path check result record.
     */
    public record PathCheckResult(
        boolean allowed,
        PermissionDecisionReason decisionReason
    ) {
        public static PathCheckResult allow() {
            return new PathCheckResult(true, null);
        }

        public static PathCheckResult deny(PermissionDecisionReason reason) {
            return new PathCheckResult(false, reason);
        }

        public static PathCheckResult denyWithReason(String reason) {
            return new PathCheckResult(false, new PermissionDecisionReason.Other(reason));
        }
    }

    /**
     * Resolved path check result record.
     */
    public record ResolvedPathCheckResult(
        boolean allowed,
        String resolvedPath,
        PermissionDecisionReason decisionReason
    ) {
        public static ResolvedPathCheckResult allow(String resolvedPath) {
            return new ResolvedPathCheckResult(true, resolvedPath, null);
        }

        public static ResolvedPathCheckResult deny(String resolvedPath, PermissionDecisionReason reason) {
            return new ResolvedPathCheckResult(false, resolvedPath, reason);
        }

        public static ResolvedPathCheckResult denyWithReason(String resolvedPath, String reason) {
            return new ResolvedPathCheckResult(false, resolvedPath, new PermissionDecisionReason.Other(reason));
        }
    }

    /**
     * Format a list of directories for display.
     */
    public static String formatDirectoryList(List<String> directories) {
        if (directories == null || directories.isEmpty()) {
            return "";
        }

        int dirCount = directories.size();

        if (dirCount <= MAX_DIRS_TO_LIST) {
            return directories.stream()
                .map(dir -> "'" + dir + "'")
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
        }

        String firstDirs = directories.stream()
            .limit(MAX_DIRS_TO_LIST)
            .map(dir -> "'" + dir + "'")
            .reduce((a, b) -> a + ", " + b)
            .orElse("");

        return firstDirs + ", and " + (dirCount - MAX_DIRS_TO_LIST) + " more";
    }

    /**
     * Extract the base directory from a glob pattern.
     */
    public static String getGlobBaseDirectory(String path) {
        if (path == null || path.isEmpty()) {
            return ".";
        }

        Matcher matcher = GLOB_PATTERN_REGEX.matcher(path);
        if (!matcher.find()) {
            return path;
        }

        int globIndex = matcher.start();
        String beforeGlob = path.substring(0, globIndex);

        int lastSepIndex = Math.max(
            beforeGlob.lastIndexOf('/'),
            beforeGlob.lastIndexOf('\\')
        );

        if (lastSepIndex == -1) return ".";
        return beforeGlob.substring(0, lastSepIndex);
    }

    /**
     * Expand tilde (~) at the start of a path to the user's home directory.
     */
    public static String expandTilde(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }

        String home = System.getProperty("user.home");

        if (path.equals("~") || path.startsWith("~/") ||
            (isWindows() && path.startsWith("~\\"))) {
            return home + path.substring(1);
        }

        return path;
    }

    /**
     * Check if a resolved path is dangerous for removal operations.
     */
    public static boolean isDangerousRemovalPath(String resolvedPath) {
        if (resolvedPath == null || resolvedPath.isEmpty()) {
            return false;
        }

        // Normalize slashes
        String forwardSlashed = resolvedPath.replace("\\", "/");

        // Wildcard patterns
        if (forwardSlashed.equals("*") || forwardSlashed.endsWith("/*")) {
            return true;
        }

        // Normalize trailing slash
        String normalizedPath = forwardSlashed.equals("/")
            ? forwardSlashed
            : forwardSlashed.replaceAll("/+$", "");

        // Root directory
        if (normalizedPath.equals("/")) {
            return true;
        }

        // Windows drive root
        if (WINDOWS_DRIVE_ROOT_REGEX.matcher(normalizedPath).matches()) {
            return true;
        }

        // Home directory
        String normalizedHome = System.getProperty("user.home").replace("\\", "/");
        if (normalizedPath.equals(normalizedHome)) {
            return true;
        }

        // Direct children of root
        Path path = Paths.get(normalizedPath);
        Path parent = path.getParent();
        if (parent != null && parent.toString().equals("/")) {
            return true;
        }

        // Windows drive children
        if (WINDOWS_DRIVE_CHILD_REGEX.matcher(normalizedPath).matches()) {
            return true;
        }

        return false;
    }

    /**
     * Validate a path for file operations.
     */
    public static ResolvedPathCheckResult validatePath(
            String path,
            String cwd,
            FileOperationType operationType) {
        return validatePath(path, cwd, operationType, null);
    }

    /**
     * Validate a path for file operations with permission context.
     */
    public static ResolvedPathCheckResult validatePath(
            String path,
            String cwd,
            FileOperationType operationType,
            PermissionContext context) {
        if (path == null || path.isEmpty()) {
            return ResolvedPathCheckResult.denyWithReason("", "Path is empty");
        }

        // Remove surrounding quotes
        String cleanPath = expandTilde(path.replaceAll("^['\"]|['\"]$", ""));

        // Check for tilde variants
        if (cleanPath.startsWith("~")) {
            return ResolvedPathCheckResult.denyWithReason(cleanPath,
                "Tilde expansion variants (~user, ~+, ~-) in paths require manual approval");
        }

        // Check for shell expansion syntax
        if (cleanPath.contains("$") || cleanPath.contains("%") || cleanPath.startsWith("=")) {
            return ResolvedPathCheckResult.denyWithReason(cleanPath,
                "Shell expansion syntax in paths requires manual approval");
        }

        // Check for glob patterns in write operations
        if (GLOB_PATTERN_REGEX.matcher(cleanPath).find()) {
            if (operationType == FileOperationType.WRITE || operationType == FileOperationType.CREATE) {
                return ResolvedPathCheckResult.denyWithReason(cleanPath,
                    "Glob patterns are not allowed in write operations. Please specify an exact file path.");
            }

            // For read operations, validate base directory
            String basePath = getGlobBaseDirectory(cleanPath);
            String absoluteBasePath = Paths.get(basePath).isAbsolute()
                ? basePath
                : Paths.get(cwd, basePath).toString();

            return ResolvedPathCheckResult.allow(absoluteBasePath);
        }

        // Resolve path
        Path resolvedPath = Paths.get(cleanPath);
        if (!resolvedPath.isAbsolute()) {
            resolvedPath = Paths.get(cwd, cleanPath);
        }

        String resolved = resolvedPath.normalize().toString();

        // Check against allowed working directories
        if (context != null && !isPathInAllowedWorkingPath(resolved, context)) {
            return ResolvedPathCheckResult.denyWithReason(resolved,
                "Path is outside the allowed working directory");
        }

        // Check dangerous removal paths
        if (operationType == FileOperationType.WRITE && isDangerousRemovalPath(resolved)) {
            return ResolvedPathCheckResult.denyWithReason(resolved,
                "Cannot modify dangerous system path: " + resolved);
        }

        return ResolvedPathCheckResult.allow(resolved);
    }

    /**
     * Check if a path is within the allowed working directory.
     */
    public static boolean isPathInAllowedWorkingPath(String resolvedPath, PermissionContext context) {
        if (context == null || context.allowedWorkingPaths == null) {
            return true; // No restriction if no context
        }

        Path path = Paths.get(resolvedPath).normalize();

        for (String allowedPath : context.allowedWorkingPaths) {
            Path allowed = Paths.get(allowedPath).normalize();
            if (path.startsWith(allowed)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if path contains path traversal patterns.
     */
    public static boolean containsPathTraversal(String path) {
        if (path == null) return false;

        // Check for ..
        if (path.contains("..")) {
            return true;
        }

        // Check for encoded traversals
        if (path.contains("%2e%2e") || path.contains("%252e")) {
            return true;
        }

        return false;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /**
     * Permission context for path validation.
     */
    public static class PermissionContext {
        public final List<String> allowedWorkingPaths;
        public final String mode;

        public PermissionContext(List<String> allowedWorkingPaths, String mode) {
            this.allowedWorkingPaths = allowedWorkingPaths;
            this.mode = mode;
        }

        public static PermissionContext unrestricted() {
            return new PermissionContext(null, "default");
        }
    }
}