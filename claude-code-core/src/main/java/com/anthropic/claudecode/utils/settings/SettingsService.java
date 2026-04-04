/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/settings service
 */
package com.anthropic.claudecode.utils.settings;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Settings service for managing configuration from multiple sources.
 *
 * Settings are loaded from multiple sources in priority order:
 * 1. Flag settings (highest priority)
 * 2. Local settings (.claude/settings.local.json)
 * 3. Project settings (.claude/settings.json)
 * 4. User settings (~/.claude.json)
 * 5. Policy settings (lowest priority, enterprise managed)
 */
public final class SettingsService {
    private SettingsService() {}

    // Cached settings
    private static volatile SettingsTypes.SettingsSchema flagSettings = null;
    private static volatile SettingsTypes.SettingsSchema localSettings = null;
    private static volatile SettingsTypes.SettingsSchema projectSettings = null;
    private static volatile SettingsTypes.SettingsSchema userSettings = null;
    private static volatile SettingsTypes.SettingsSchema policySettings = null;

    // Combined settings cache
    private static volatile SettingsTypes.SettingsSchema combinedSettings = null;

    // Setting sources to use (defaults to all)
    private static volatile SettingSource[] activeSources = SettingSource.SETTING_SOURCES;

    /**
     * Set active setting sources.
     */
    public static void setActiveSources(SettingSource[] sources) {
        activeSources = sources;
        invalidateCache();
    }

    /**
     * Set flag settings (from CLI flags).
     */
    public static void setFlagSettings(SettingsTypes.SettingsSchema settings) {
        flagSettings = settings;
        invalidateCache();
    }

    /**
     * Get combined settings from all sources.
     */
    public static SettingsTypes.SettingsSchema getSettings() {
        if (combinedSettings == null) {
            combinedSettings = loadCombinedSettings();
        }
        return combinedSettings;
    }

    /**
     * Load settings from a specific source.
     */
    public static SettingsTypes.SettingsSchema getSettingsFromSource(SettingSource source) {
        return switch (source) {
            case FLAG -> flagSettings != null ? flagSettings : SettingsTypes.SettingsSchema.createDefault();
            case LOCAL -> loadLocalSettings();
            case PROJECT -> loadProjectSettings();
            case USER -> loadUserSettings();
            case POLICY -> loadPolicySettings();
        };
    }

    /**
     * Load and combine settings from all active sources.
     */
    private static SettingsTypes.SettingsSchema loadCombinedSettings() {
        SettingsTypes.SettingsSchema result = SettingsTypes.SettingsSchema.createDefault();

        // Merge in reverse order (lowest priority first)
        if (isActive(SettingSource.POLICY)) {
            result = result.merge(loadPolicySettings());
        }
        if (isActive(SettingSource.USER)) {
            result = result.merge(loadUserSettings());
        }
        if (isActive(SettingSource.PROJECT)) {
            result = result.merge(loadProjectSettings());
        }
        if (isActive(SettingSource.LOCAL)) {
            result = result.merge(loadLocalSettings());
        }
        if (isActive(SettingSource.FLAG)) {
            result = result.merge(flagSettings);
        }

        return result;
    }

    /**
     * Check if a source is active.
     */
    private static boolean isActive(SettingSource source) {
        for (SettingSource s : activeSources) {
            if (s == source) return true;
        }
        return false;
    }

    /**
     * Load local settings from .claude/settings.local.json.
     */
    private static SettingsTypes.SettingsSchema loadLocalSettings() {
        if (localSettings != null) return localSettings;

        Path path = Paths.get(System.getProperty("user.dir"), ".claude", "settings.local.json");
        localSettings = loadSettingsFromPath(path);
        return localSettings;
    }

    /**
     * Load project settings from .claude/settings.json.
     */
    private static SettingsTypes.SettingsSchema loadProjectSettings() {
        if (projectSettings != null) return projectSettings;

        Path path = Paths.get(System.getProperty("user.dir"), ".claude", "settings.json");
        projectSettings = loadSettingsFromPath(path);
        return projectSettings;
    }

    /**
     * Load user settings from ~/.claude.json.
     */
    private static SettingsTypes.SettingsSchema loadUserSettings() {
        if (userSettings != null) return userSettings;

        Path path = getGlobalConfigPath();
        userSettings = loadSettingsFromPath(path);
        return userSettings;
    }

    /**
     * Load policy settings (enterprise managed).
     */
    private static SettingsTypes.SettingsSchema loadPolicySettings() {
        if (policySettings != null) return policySettings;

        // Policy settings are typically in system-wide locations
        String policyDir = System.getenv("CLAUDE_POLICY_DIR");
        if (policyDir != null) {
            Path path = Paths.get(policyDir, "claude-policy.json");
            policySettings = loadSettingsFromPath(path);
        } else {
            policySettings = SettingsTypes.SettingsSchema.createDefault();
        }
        return policySettings;
    }

    /**
     * Load settings from a JSON file path.
     */
    private static SettingsTypes.SettingsSchema loadSettingsFromPath(Path path) {
        try {
            if (Files.exists(path)) {
                String content = Files.readString(path);
                return parseSettings(content);
            }
        } catch (Exception e) {
            // Return defaults on error
        }
        return SettingsTypes.SettingsSchema.createDefault();
    }

    /**
     * Parse settings from JSON string.
     */
    private static SettingsTypes.SettingsSchema parseSettings(String json) {
        if (json == null || json.isBlank()) {
            return SettingsTypes.SettingsSchema.createDefault();
        }

        try {
            Map<String, Object> parsed = parseJsonToMap(json);

            String model = (String) parsed.get("model");
            String apiKey = (String) parsed.get("apiKey");
            String theme = (String) parsed.get("theme");
            String permissionMode = (String) parsed.get("permissionMode");

            // Parse permissions
            SettingsTypes.PermissionsSchema permissions = null;
            Object permsObj = parsed.get("permissions");
            if (permsObj instanceof Map permsMap) {
                List<String> allow = toStringList(permsMap.get("allow"));
                List<String> deny = toStringList(permsMap.get("deny"));
                List<String> ask = toStringList(permsMap.get("ask"));
                permissions = new SettingsTypes.PermissionsSchema(null, allow, deny, ask);
            }

            SettingsTypes.PermissionMode mode = SettingsTypes.PermissionMode.DEFAULT;
            if (permissionMode != null) {
                mode = SettingsTypes.PermissionMode.fromId(permissionMode);
            }

            return new SettingsTypes.SettingsSchema(
                model,
                apiKey,
                theme,
                mode,
                permissions,
                null, // hooks
                null, // mcp
                null, // autoMode
                null, // enabledMcpServers
                null, // disabledMcpServers
                null, // autoUpdate
                null, // releaseChannel
                null, // installMethod
                null, // preferredNotifChannel
                null, // hasTrustDialogAccepted
                null, // hasCompletedOnboarding
                null, // plugins
                parsed // custom
            );
        } catch (Exception e) {
            return SettingsTypes.SettingsSchema.createDefault();
        }
    }

    /**
     * Parse JSON to map.
     */
    private static Map<String, Object> parseJsonToMap(String json) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (json == null || json.isBlank()) return result;

        json = json.trim();
        if (!json.startsWith("{") || !json.endsWith("}")) return result;

        json = json.substring(1, json.length() - 1).trim();
        if (json.isEmpty()) return result;

        int i = 0;
        while (i < json.length()) {
            // Find key
            while (i < json.length() && json.charAt(i) != '"') i++;
            if (i >= json.length()) break;
            i++;
            int keyStart = i;
            while (i < json.length() && json.charAt(i) != '"') {
                if (json.charAt(i) == '\\') i++;
                i++;
            }
            String key = json.substring(keyStart, i);
            i++;

            // Find colon
            while (i < json.length() && json.charAt(i) != ':') i++;
            i++;

            // Find value
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= json.length()) break;

            Object value;
            char c = json.charAt(i);
            if (c == '"') {
                i++;
                int valueStart = i;
                while (i < json.length() && json.charAt(i) != '"') {
                    if (json.charAt(i) == '\\') i++;
                    i++;
                }
                value = unescapeJson(json.substring(valueStart, i));
                i++;
            } else if (c == '[') {
                int arrayStart = i;
                int depth = 1;
                i++;
                while (i < json.length() && depth > 0) {
                    if (json.charAt(i) == '[') depth++;
                    else if (json.charAt(i) == ']') depth--;
                    i++;
                }
                value = parseJsonArray(json.substring(arrayStart, i));
            } else if (c == '{') {
                int objStart = i;
                int depth = 1;
                i++;
                while (i < json.length() && depth > 0) {
                    if (json.charAt(i) == '{') depth++;
                    else if (json.charAt(i) == '}') depth--;
                    i++;
                }
                value = parseJsonToMap(json.substring(objStart, i));
            } else if (Character.isDigit(c) || c == '-') {
                int valueStart = i;
                while (i < json.length() && (Character.isDigit(json.charAt(i)) || json.charAt(i) == '.' || json.charAt(i) == '-' || json.charAt(i) == 'e' || json.charAt(i) == 'E')) {
                    i++;
                }
                String numStr = json.substring(valueStart, i);
                try {
                    if (numStr.contains(".")) {
                        value = Double.parseDouble(numStr);
                    } else {
                        value = Long.parseLong(numStr);
                    }
                } catch (NumberFormatException e) {
                    value = numStr;
                }
            } else if (json.substring(i).startsWith("true")) {
                value = true;
                i += 4;
            } else if (json.substring(i).startsWith("false")) {
                value = false;
                i += 5;
            } else if (json.substring(i).startsWith("null")) {
                value = null;
                i += 4;
            } else {
                value = null;
                while (i < json.length() && json.charAt(i) != ',' && json.charAt(i) != '}') i++;
            }

            result.put(key, value);

            // Skip comma
            while (i < json.length() && json.charAt(i) != ',') i++;
            if (i < json.length()) i++;
        }

        return result;
    }

    /**
     * Parse JSON array.
     */
    private static List<String> parseJsonArray(String json) {
        List<String> result = new ArrayList<>();
        if (json == null || !json.startsWith("[") || !json.endsWith("]")) return result;

        json = json.substring(1, json.length() - 1).trim();
        if (json.isEmpty()) return result;

        int i = 0;
        while (i < json.length()) {
            while (i < json.length() && json.charAt(i) != '"') i++;
            if (i >= json.length()) break;
            i++;
            int start = i;
            while (i < json.length() && json.charAt(i) != '"') {
                if (json.charAt(i) == '\\') i++;
                i++;
            }
            result.add(unescapeJson(json.substring(start, i)));
            i++;
        }

        return result;
    }

    /**
     * Convert object to string list.
     */
    @SuppressWarnings("unchecked")
    private static List<String> toStringList(Object obj) {
        if (obj == null) return null;
        if (obj instanceof List list) {
            List<String> result = new ArrayList<>();
            for (Object item : list) {
                result.add(item != null ? item.toString() : null);
            }
            return result;
        }
        return null;
    }

    /**
     * Unescape JSON string.
     */
    private static String unescapeJson(String s) {
        if (s == null) return null;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case 'n' -> { sb.append('\n'); i++; }
                    case 't' -> { sb.append('\t'); i++; }
                    case 'r' -> { sb.append('\r'); i++; }
                    case '"' -> { sb.append('"'); i++; }
                    case '\\' -> { sb.append('\\'); i++; }
                    default -> sb.append(c);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Get global config path.
     */
    private static Path getGlobalConfigPath() {
        String configDir = System.getenv("CLAUDE_CONFIG_DIR");
        if (configDir != null && !configDir.isEmpty()) {
            return Paths.get(configDir, ".claude.json");
        }
        return Paths.get(System.getProperty("user.home"), ".claude.json");
    }

    /**
     * Save settings to a specific source.
     */
    public static void saveSettings(SettingSource source, SettingsTypes.SettingsSchema settings) {
        Path path = switch (source) {
            case LOCAL -> Paths.get(System.getProperty("user.dir"), ".claude", "settings.local.json");
            case PROJECT -> Paths.get(System.getProperty("user.dir"), ".claude", "settings.json");
            case USER -> getGlobalConfigPath();
            default -> throw new IllegalArgumentException("Cannot save to source: " + source);
        };

        // Serialize to JSON and write
        try {
            String json = serializeSettings(settings);
            Files.createDirectories(path.getParent());
            Files.writeString(path, json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save settings to " + path, e);
        }

        // Update cache
        switch (source) {
            case LOCAL -> localSettings = settings;
            case PROJECT -> projectSettings = settings;
            case USER -> userSettings = settings;
            default -> {}
        }

        invalidateCache();
    }

    /**
     * Serialize settings to JSON.
     */
    private static String serializeSettings(SettingsTypes.SettingsSchema settings) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");

        boolean first = true;

        if (settings.model() != null) {
            sb.append("  \"model\": \"").append(escapeJson(settings.model())).append("\"");
            first = false;
        }

        if (settings.apiKey() != null) {
            if (!first) sb.append(",\n");
            sb.append("  \"apiKey\": \"").append(escapeJson(settings.apiKey())).append("\"");
            first = false;
        }

        if (settings.theme() != null) {
            if (!first) sb.append(",\n");
            sb.append("  \"theme\": \"").append(escapeJson(settings.theme())).append("\"");
            first = false;
        }

        if (settings.permissionMode() != null) {
            if (!first) sb.append(",\n");
            sb.append("  \"permissionMode\": \"").append(settings.permissionMode().getId()).append("\"");
            first = false;
        }

        if (settings.permissions() != null) {
            if (!first) sb.append(",\n");
            sb.append("  \"permissions\": {\n");
            SettingsTypes.PermissionsSchema perms = settings.permissions();

            if (perms.allow() != null && !perms.allow().isEmpty()) {
                sb.append("    \"allow\": [");
                for (int i = 0; i < perms.allow().size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append("\"").append(escapeJson(perms.allow().get(i))).append("\"");
                }
                sb.append("]");
            }

            if (perms.deny() != null && !perms.deny().isEmpty()) {
                if (perms.allow() != null && !perms.allow().isEmpty()) sb.append(",\n");
                sb.append("    \"deny\": [");
                for (int i = 0; i < perms.deny().size(); i++) {
                    if (i > 0) sb.append(", ");
                    sb.append("\"").append(escapeJson(perms.deny().get(i))).append("\"");
                }
                sb.append("]");
            }

            sb.append("\n  }");
            first = false;
        }

        // Add custom settings
        if (settings.custom() != null && !settings.custom().isEmpty()) {
            for (Map.Entry<String, Object> entry : settings.custom().entrySet()) {
                if (!first) sb.append(",\n");
                sb.append("  \"").append(escapeJson(entry.getKey())).append("\": ");
                sb.append(serializeValue(entry.getValue()));
                first = false;
            }
        }

        sb.append("\n}");
        return sb.toString();
    }

    /**
     * Serialize a value to JSON.
     */
    private static String serializeValue(Object value) {
        if (value == null) return "null";
        if (value instanceof String s) return "\"" + escapeJson(s) + "\"";
        if (value instanceof Number n) return n.toString();
        if (value instanceof Boolean b) return b.toString();
        if (value instanceof Map m) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Object key : m.keySet()) {
                if (!first) sb.append(", ");
                sb.append("\"").append(escapeJson(key.toString())).append("\": ");
                sb.append(serializeValue(m.get(key)));
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
        if (value instanceof List l) {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < l.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(serializeValue(l.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        return "\"" + escapeJson(value.toString()) + "\"";
    }

    /**
     * Escape JSON string.
     */
    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Invalidate all caches.
     */
    public static void invalidateCache() {
        combinedSettings = null;
    }

    /**
     * Clear all cached settings.
     */
    public static void clearCaches() {
        flagSettings = null;
        localSettings = null;
        projectSettings = null;
        userSettings = null;
        policySettings = null;
        combinedSettings = null;
    }

    /**
     * Get a specific setting value with source information.
     */
    public static <T> SettingsTypes.SettingValue<T> getSettingWithSource(
        java.util.function.Function<SettingsTypes.SettingsSchema, T> getter
    ) {
        for (SettingSource source : SettingSource.SETTING_SOURCES) {
            if (!isActive(source)) continue;

            SettingsTypes.SettingsSchema settings = getSettingsFromSource(source);
            T value = getter.apply(settings);
            if (value != null) {
                return SettingsTypes.SettingValue.of(value, source);
            }
        }
        return SettingsTypes.SettingValue.of(null, SettingSource.POLICY);
    }

    /**
     * Get the permission mode.
     */
    public static SettingsTypes.PermissionMode getPermissionMode() {
        return getSettings().permissionMode();
    }

    /**
     * Get the model.
     */
    public static String getModel() {
        return getSettings().model();
    }

    /**
     * Check if a tool is allowed.
     */
    public static boolean isToolAllowed(String toolName) {
        SettingsTypes.SettingsSchema settings = getSettings();
        if (settings.permissions() == null) return true;

        List<String> allow = settings.permissions().allow();
        List<String> deny = settings.permissions().deny();

        // Explicit deny takes precedence
        if (deny != null && deny.contains(toolName)) return false;

        // Explicit allow
        if (allow != null && allow.contains(toolName)) return true;

        // Default based on mode
        return settings.permissionMode() != SettingsTypes.PermissionMode.DONT_ASK;
    }
}