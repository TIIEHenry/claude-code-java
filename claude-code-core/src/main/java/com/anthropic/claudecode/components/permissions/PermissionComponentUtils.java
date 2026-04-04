/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code components/permissions/utils
 */
package com.anthropic.claudecode.components.permissions;

import java.util.*;
import java.util.regex.*;

/**
 * Permission utilities - Utility functions for permission handling.
 */
public final class PermissionComponentUtils {

    /**
     * Permission action enum.
     */
    public enum PermissionAction {
        ALLOW,
        DENY,
        ASK
    }

    /**
     * Permission type enum.
     */
    public enum PermissionType {
        READ,
        WRITE,
        EXECUTE,
        DELETE,
        NETWORK
    }

    /**
     * Check if a path matches a pattern.
     */
    public static boolean matchesPattern(String path, String pattern) {
        if (pattern == null || path == null) {
            return false;
        }

        // Convert glob pattern to regex
        String regex = pattern
            .replace(".", "\\.")
            .replace("*", ".*")
            .replace("?", ".");

        return Pattern.matches(regex, path);
    }

    /**
     * Check if a command is allowed.
     */
    public static boolean isCommandAllowed(String command, List<String> allowedPatterns) {
        if (command == null || command.isEmpty()) {
            return false;
        }

        for (String pattern : allowedPatterns) {
            if (matchesPattern(command, pattern)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if a path is allowed for reading.
     */
    public static boolean isReadAllowed(String path, List<String> allowedPaths) {
        return isPathAllowed(path, allowedPaths);
    }

    /**
     * Check if a path is allowed for writing.
     */
    public static boolean isWriteAllowed(String path, List<String> allowedPaths) {
        return isPathAllowed(path, allowedPaths);
    }

    private static boolean isPathAllowed(String path, List<String> allowedPaths) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        for (String allowed : allowedPaths) {
            if (path.startsWith(allowed) || matchesPattern(path, allowed)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get permission description.
     */
    public static String getPermissionDescription(PermissionType type, String resource) {
        return switch (type) {
            case READ -> "Read access to: " + resource;
            case WRITE -> "Write access to: " + resource;
            case EXECUTE -> "Execute permission for: " + resource;
            case DELETE -> "Delete permission for: " + resource;
            case NETWORK -> "Network access to: " + resource;
        };
    }

    /**
     * Format permission for display.
     */
    public static String formatPermission(String tool, String action, String resource) {
        return String.format("[%s] %s: %s", tool, action, resource);
    }

    /**
     * Parse permission string.
     */
    public static ParsedPermission parsePermission(String permission) {
        if (permission == null || permission.isEmpty()) {
            return null;
        }

        // Format: [tool] action: resource
        Pattern pattern = Pattern.compile("\\[(\\w+)\\]\\s*(\\w+):\\s*(.+)");
        Matcher matcher = pattern.matcher(permission);

        if (matcher.matches()) {
            return new ParsedPermission(matcher.group(1), matcher.group(2), matcher.group(3));
        }

        return null;
    }

    /**
     * Check if permission is dangerous.
     */
    public static boolean isDangerousPermission(PermissionType type, String resource) {
        if (type == PermissionType.DELETE) {
            return true;
        }

        if (type == PermissionType.WRITE) {
            // Check for sensitive paths
            String lowerResource = resource.toLowerCase();
            return lowerResource.contains("/etc/") ||
                   lowerResource.contains("config") ||
                   lowerResource.contains(".env") ||
                   lowerResource.contains("credentials");
        }

        if (type == PermissionType.NETWORK) {
            // Check for sensitive network access
            return resource.contains("localhost") ||
                   resource.contains("127.0.0.1") ||
                   resource.contains("internal");
        }

        return false;
    }

    /**
     * Get recommended action for a permission.
     */
    public static PermissionAction getRecommendedAction(PermissionType type, String resource) {
        if (isDangerousPermission(type, resource)) {
            return PermissionAction.ASK;
        }

        return switch (type) {
            case READ -> PermissionAction.ALLOW;
            case WRITE, EXECUTE -> PermissionAction.ASK;
            case DELETE -> PermissionAction.ASK;
            case NETWORK -> PermissionAction.ASK;
        };
    }

    /**
     * Parsed permission record.
     */
    public record ParsedPermission(String tool, String action, String resource) {}
}