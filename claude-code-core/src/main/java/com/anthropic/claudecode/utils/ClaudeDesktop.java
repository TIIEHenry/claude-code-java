/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code claude desktop utilities
 */
package com.anthropic.claudecode.utils;

import java.nio.file.*;
import java.util.*;

/**
 * Claude Desktop integration utilities.
 */
public final class ClaudeDesktop {
    private ClaudeDesktop() {}

    /**
     * Get Claude Desktop config path.
     */
    public static Path getClaudeDesktopConfigPath() {
        String os = System.getProperty("os.name", "").toLowerCase();
        String home = System.getProperty("user.home");

        if (os.contains("mac")) {
            return Paths.get(home, "Library", "Application Support", "Claude", "claude_desktop_config.json");
        }

        // Windows/WSL path
        String userProfile = System.getenv("USERPROFILE");
        if (userProfile != null) {
            // Convert Windows path to WSL path
            userProfile = userProfile.replace("\\", "/");
            String driveLetter = "";
            if (userProfile.length() > 1 && userProfile.charAt(1) == ':') {
                driveLetter = userProfile.substring(0, 1).toLowerCase();
                userProfile = userProfile.substring(2);
            }
            return Paths.get("/mnt/" + driveLetter, userProfile, "AppData/Roaming/Claude/claude_desktop_config.json");
        }

        throw new UnsupportedOperationException("Claude Desktop integration only works on macOS and WSL");
    }

    /**
     * Read Claude Desktop MCP servers config.
     */
    public static Map<String, McpServerConfig> readClaudeDesktopMcpServers() {
        try {
            Path configPath = getClaudeDesktopConfigPath();
            if (!Files.exists(configPath)) {
                return Map.of();
            }

            String content = Files.readString(configPath);
            Map<String, Object> config = JsonUtils.parse(content);
            Object mcpServers = config.get("mcpServers");

            if (!(mcpServers instanceof Map)) {
                return Map.of();
            }

            Map<String, McpServerConfig> result = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) mcpServers).entrySet()) {
                if (entry.getValue() instanceof Map) {
                    Map<String, Object> serverConfig = (Map<String, Object>) entry.getValue();
                    result.put(entry.getKey(), new McpServerConfig(
                            (String) serverConfig.get("command"),
                            (List<String>) serverConfig.get("args"),
                            (Map<String, String>) serverConfig.get("env")
                    ));
                }
            }

            return result;
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * MCP server config.
     */
    public record McpServerConfig(
            String command,
            List<String> args,
            Map<String, String> env
    ) {}
}