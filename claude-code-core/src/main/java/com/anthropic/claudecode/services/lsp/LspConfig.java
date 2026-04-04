/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/lsp/config.ts
 */
package com.anthropic.claudecode.services.lsp;

import java.util.*;
import java.util.concurrent.*;

/**
 * LSP server configuration.
 */
public final class LspConfig {
    private LspConfig() {}

    /**
     * Scoped LSP server configuration.
     */
    public record ScopedLspServerConfig(
        String scope,         // plugin name or scope identifier
        String name,          // server name
        String command,       // command to start server
        List<String> args,    // command arguments
        Map<String, String> env,  // environment variables
        String cwd,           // working directory
        List<String> fileExtensions,  // supported file extensions
        List<String> languages         // supported language IDs
    ) {}

    /**
     * LSP servers loaded from plugins.
     */
    private static volatile Map<String, ScopedLspServerConfig> allServers = new ConcurrentHashMap<>();

    /**
     * Get all configured LSP servers.
     */
    public static CompletableFuture<Map<String, ScopedLspServerConfig>> getAllLspServers() {
        return CompletableFuture.supplyAsync(() -> {
            // Load from configuration files if not already loaded
            if (allServers.isEmpty()) {
                loadServersFromConfig();
            }
            return new HashMap<>(allServers);
        });
    }

    /**
     * Load LSP servers from configuration files.
     */
    private static void loadServersFromConfig() {
        // Load from user settings
        loadFromUserSettings();

        // Load from project settings
        loadFromProjectSettings();

        // Load built-in servers for common languages
        loadBuiltinServers();
    }

    /**
     * Load LSP servers from user settings (~/.claude/settings.json).
     */
    private static void loadFromUserSettings() {
        try {
            java.nio.file.Path settingsPath = java.nio.file.Paths.get(
                System.getProperty("user.home"),
                ".claude",
                "settings.json"
            );

            if (java.nio.file.Files.exists(settingsPath)) {
                String content = java.nio.file.Files.readString(settingsPath);
                parseAndLoadServers(content, "user");
            }
        } catch (Exception e) {
            // Ignore errors
        }
    }

    /**
     * Load LSP servers from project settings (.claude/settings.json).
     */
    private static void loadFromProjectSettings() {
        try {
            String cwd = System.getProperty("user.dir");
            java.nio.file.Path settingsPath = java.nio.file.Paths.get(cwd, ".claude", "settings.json");

            if (java.nio.file.Files.exists(settingsPath)) {
                String content = java.nio.file.Files.readString(settingsPath);
                parseAndLoadServers(content, "project");
            }
        } catch (Exception e) {
            // Ignore errors
        }
    }

    /**
     * Load built-in LSP servers for common languages.
     */
    private static void loadBuiltinServers() {
        // Java language server
        registerServer("java", new ScopedLspServerConfig(
            "builtin",
            "jdtls",
            "jdtls",
            List.of(),
            Map.of(),
            null,
            List.of("java"),
            List.of("java")
        ));

        // TypeScript/JavaScript language server
        registerServer("typescript", new ScopedLspServerConfig(
            "builtin",
            "typescript-language-server",
            "typescript-language-server",
            List.of("--stdio"),
            Map.of(),
            null,
            List.of("ts", "tsx", "js", "jsx"),
            List.of("typescript", "javascript", "typescriptreact", "javascriptreact")
        ));

        // Python language server
        registerServer("python", new ScopedLspServerConfig(
            "builtin",
            "pylsp",
            "pylsp",
            List.of(),
            Map.of(),
            null,
            List.of("py"),
            List.of("python")
        ));

        // Go language server
        registerServer("go", new ScopedLspServerConfig(
            "builtin",
            "gopls",
            "gopls",
            List.of(),
            Map.of(),
            null,
            List.of("go"),
            List.of("go")
        ));

        // Rust language server
        registerServer("rust", new ScopedLspServerConfig(
            "builtin",
            "rust-analyzer",
            "rust-analyzer",
            List.of(),
            Map.of(),
            null,
            List.of("rs"),
            List.of("rust")
        ));
    }

    /**
     * Parse settings JSON and load LSP servers.
     */
    private static void parseAndLoadServers(String json, String scope) {
        // Find mcp.servers or lsp.servers section
        try {
            // Try lsp.servers first
            int lspStart = json.indexOf("\"lsp\"");
            if (lspStart >= 0) {
                int serversStart = json.indexOf("\"servers\"", lspStart);
                if (serversStart >= 0) {
                    int objStart = json.indexOf("{", serversStart + 9);
                    if (objStart >= 0) {
                        // Find matching closing brace
                        int depth = 1;
                        int objEnd = objStart + 1;
                        while (objEnd < json.length() && depth > 0) {
                            char c = json.charAt(objEnd);
                            if (c == '{') depth++;
                            else if (c == '}') depth--;
                            objEnd++;
                        }
                        String serversObj = json.substring(objStart, objEnd);
                        parseServersObject(serversObj, scope);
                    }
                }
            }

            // Also try mcp.servers
            int mcpStart = json.indexOf("\"mcp\"");
            if (mcpStart >= 0) {
                int serversStart = json.indexOf("\"servers\"", mcpStart);
                if (serversStart >= 0) {
                    int objStart = json.indexOf("{", serversStart + 9);
                    if (objStart >= 0) {
                        int depth = 1;
                        int objEnd = objStart + 1;
                        while (objEnd < json.length() && depth > 0) {
                            char c = json.charAt(objEnd);
                            if (c == '{') depth++;
                            else if (c == '}') depth--;
                            objEnd++;
                        }
                        String serversObj = json.substring(objStart, objEnd);
                        parseServersObject(serversObj, scope);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
    }

    /**
     * Parse servers object and register each server.
     */
    private static void parseServersObject(String serversObj, String scope) {
        int i = 0;
        while (i < serversObj.length()) {
            // Find server key
            int keyStart = serversObj.indexOf("\"", i);
            if (keyStart < 0) break;
            int keyEnd = serversObj.indexOf("\"", keyStart + 1);
            if (keyEnd < 0) break;
            String serverKey = serversObj.substring(keyStart + 1, keyEnd);

            // Find server config object
            int configStart = serversObj.indexOf("{", keyEnd);
            if (configStart < 0) break;

            int depth = 1;
            int configEnd = configStart + 1;
            while (configEnd < serversObj.length() && depth > 0) {
                char c = serversObj.charAt(configEnd);
                if (c == '{') depth++;
                else if (c == '}') depth--;
                configEnd++;
            }

            String configObj = serversObj.substring(configStart, configEnd);
            ScopedLspServerConfig config = parseServerConfig(configObj, scope, serverKey);
            if (config != null) {
                registerServer(serverKey, config);
            }

            i = configEnd;
        }
    }

    /**
     * Parse individual server config.
     */
    private static ScopedLspServerConfig parseServerConfig(String configObj, String scope, String name) {
        try {
            String command = extractJsonString(configObj, "command");
            if (command == null) command = name;

            List<String> args = extractJsonArray(configObj, "args");
            if (args == null) args = List.of("--stdio");

            Map<String, String> env = extractJsonStringMap(configObj, "env");
            if (env == null) env = Map.of();

            String cwd = extractJsonString(configObj, "cwd");

            List<String> fileExtensions = extractJsonArray(configObj, "fileExtensions");
            List<String> languages = extractJsonArray(configObj, "languages");

            return new ScopedLspServerConfig(
                scope, name, command, args, env, cwd, fileExtensions, languages
            );
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract string value from JSON.
     */
    private static String extractJsonString(String json, String key) {
        int keyIdx = json.indexOf("\"" + key + "\"");
        if (keyIdx < 0) return null;

        int valStart = json.indexOf("\"", keyIdx + key.length() + 2);
        if (valStart < 0) return null;

        int valEnd = json.indexOf("\"", valStart + 1);
        if (valEnd < 0) return null;

        return json.substring(valStart + 1, valEnd);
    }

    /**
     * Extract array from JSON.
     */
    private static List<String> extractJsonArray(String json, String key) {
        int keyIdx = json.indexOf("\"" + key + "\"");
        if (keyIdx < 0) return null;

        int arrStart = json.indexOf("[", keyIdx);
        if (arrStart < 0) return null;

        int depth = 1;
        int arrEnd = arrStart + 1;
        while (arrEnd < json.length() && depth > 0) {
            char c = json.charAt(arrEnd);
            if (c == '[') depth++;
            else if (c == ']') depth--;
            arrEnd++;
        }

        String arrContent = json.substring(arrStart + 1, arrEnd - 1);
        List<String> result = new ArrayList<>();

        // Parse array elements
        int i = 0;
        while (i < arrContent.length()) {
            while (i < arrContent.length() && Character.isWhitespace(arrContent.charAt(i))) i++;
            if (i >= arrContent.length()) break;

            if (arrContent.charAt(i) == '"') {
                i++;
                int start = i;
                while (i < arrContent.length() && arrContent.charAt(i) != '"') {
                    if (arrContent.charAt(i) == '\\') i++;
                    i++;
                }
                result.add(arrContent.substring(start, i));
                i++;
            }

            while (i < arrContent.length() && arrContent.charAt(i) != ',' && arrContent.charAt(i) != ']') i++;
            if (i < arrContent.length() && arrContent.charAt(i) == ',') i++;
        }

        return result;
    }

    /**
     * Extract string map from JSON.
     */
    private static Map<String, String> extractJsonStringMap(String json, String key) {
        int keyIdx = json.indexOf("\"" + key + "\"");
        if (keyIdx < 0) return null;

        int objStart = json.indexOf("{", keyIdx);
        if (objStart < 0) return null;

        int depth = 1;
        int objEnd = objStart + 1;
        while (objEnd < json.length() && depth > 0) {
            char c = json.charAt(objEnd);
            if (c == '{') depth++;
            else if (c == '}') depth--;
            objEnd++;
        }

        String objContent = json.substring(objStart, objEnd);
        Map<String, String> result = new HashMap<>();

        int i = 0;
        while (i < objContent.length()) {
            int kStart = objContent.indexOf("\"", i);
            if (kStart < 0) break;
            int kEnd = objContent.indexOf("\"", kStart + 1);
            if (kEnd < 0) break;
            String k = objContent.substring(kStart + 1, kEnd);

            int vStart = objContent.indexOf("\"", kEnd + 1);
            if (vStart < 0) break;
            int vEnd = objContent.indexOf("\"", vStart + 1);
            if (vEnd < 0) break;
            String v = objContent.substring(vStart + 1, vEnd);

            result.put(k, v);
            i = vEnd + 1;
        }

        return result;
    }

    /**
     * Register an LSP server.
     */
    public static void registerServer(String key, ScopedLspServerConfig config) {
        allServers.put(key, config);
    }

    /**
     * Unregister an LSP server.
     */
    public static void unregisterServer(String key) {
        allServers.remove(key);
    }

    /**
     * Get server by key.
     */
    public static ScopedLspServerConfig getServer(String key) {
        return allServers.get(key);
    }

    /**
     * Get server for file extension.
     */
    public static ScopedLspServerConfig getServerForExtension(String extension) {
        for (ScopedLspServerConfig config : allServers.values()) {
            if (config.fileExtensions() != null &&
                config.fileExtensions().contains(extension)) {
                return config;
            }
        }
        return null;
    }

    /**
     * Get server for language ID.
     */
    public static ScopedLspServerConfig getServerForLanguage(String languageId) {
        for (ScopedLspServerConfig config : allServers.values()) {
            if (config.languages() != null &&
                config.languages().contains(languageId)) {
                return config;
            }
        }
        return null;
    }

    /**
     * Clear all servers.
     */
    public static void clearServers() {
        allServers.clear();
    }

    /**
     * Get server count.
     */
    public static int getServerCount() {
        return allServers.size();
    }
}