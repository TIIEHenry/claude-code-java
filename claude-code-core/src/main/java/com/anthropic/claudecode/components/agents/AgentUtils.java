/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code components/agents/utils
 */
package com.anthropic.claudecode.components.agents;

/**
 * Agent utilities - Utility functions for agent system.
 */
public final class AgentUtils {

    /**
     * Get display name for agent source.
     */
    public static String getAgentSourceDisplayName(AgentTypes.SettingSource source) {
        if (source == null) {
            return "Agents";
        }

        return switch (source) {
            case USER -> "User agents";
            case PROJECT -> "Project agents";
            case ENTERPRISE -> "Enterprise agents";
            case MANAGED -> "Managed agents";
            case PLUGIN -> "Plugin agents";
        };
    }

    /**
     * Get source name.
     */
    public static String getSourceName(AgentTypes.SettingSource source) {
        if (source == null) {
            return "unknown";
        }

        return source.name().toLowerCase().replace("_", " ");
    }

    /**
     * Capitalize first letter.
     */
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * Format agent name for display.
     */
    public static String formatAgentName(String name) {
        if (name == null || name.isEmpty()) {
            return "Unnamed agent";
        }

        // Replace hyphens and underscores with spaces
        String formatted = name.replace("-", " ").replace("_", " ");

        // Capitalize each word
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = true;
        for (char c : formatted.toCharArray()) {
            if (Character.isWhitespace(c)) {
                capitalizeNext = true;
                result.append(c);
            } else if (capitalizeNext) {
                result.append(Character.toUpperCase(c));
                capitalizeNext = false;
            } else {
                result.append(Character.toLowerCase(c));
            }
        }

        return result.toString();
    }

    /**
     * Validate agent name.
     */
    public static boolean isValidAgentName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }

        // Name must be alphanumeric with hyphens and underscores
        return name.matches("^[a-zA-Z0-9_-]+$");
    }

    /**
     * Normalize agent name.
     */
    public static String normalizeAgentName(String name) {
        if (name == null || name.isEmpty()) {
            return "unnamed-agent";
        }

        // Convert to lowercase, replace spaces and special chars
        return name.toLowerCase()
            .replace(" ", "-")
            .replaceAll("[^a-z0-9_-]", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
    }

    /**
     * Get agent file path.
     */
    public static String getAgentFilePath(String agentName, AgentTypes.SettingSource source) {
        String basePath = switch (source) {
            case USER -> System.getProperty("user.home") + "/.claude/agents";
            case PROJECT -> System.getProperty("user.dir") + "/.claude/agents";
            default -> System.getProperty("user.home") + "/.claude/agents";
        };

        return basePath + "/" + normalizeAgentName(agentName) + ".md";
    }

    /**
     * Check if source is editable.
     */
    public static boolean isSourceEditable(AgentTypes.SettingSource source) {
        return source == AgentTypes.SettingSource.USER || source == AgentTypes.SettingSource.PROJECT;
    }

    /**
     * Check if source is deletable.
     */
    public static boolean isSourceDeletable(AgentTypes.SettingSource source) {
        return source == AgentTypes.SettingSource.USER || source == AgentTypes.SettingSource.PROJECT;
    }

    /**
     * Compare agents by name.
     */
    public static int compareByName(AgentTypes.AgentDefinition a1, AgentTypes.AgentDefinition a2) {
        return a1.name().compareToIgnoreCase(a2.name());
    }

    /**
     * Compare agents by source priority.
     */
    public static int compareBySource(AgentTypes.AgentDefinition a1, AgentTypes.AgentDefinition a2) {
        int p1 = getSourcePriority(a1.source());
        int p2 = getSourcePriority(a2.source());
        if (p1 != p2) return p1 - p2;
        return a1.name().compareToIgnoreCase(a2.name());
    }

    private static int getSourcePriority(AgentTypes.SettingSource source) {
        return switch (source) {
            case PROJECT -> 0;
            case USER -> 1;
            case PLUGIN -> 2;
            case ENTERPRISE -> 3;
            case MANAGED -> 4;
        };
    }
}