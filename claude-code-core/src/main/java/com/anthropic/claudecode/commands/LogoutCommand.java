/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/logout
 */
package com.anthropic.claudecode.commands;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Logout command - Sign out from Anthropic account.
 */
public final class LogoutCommand implements Command {
    @Override
    public String name() {
        return "logout";
    }

    @Override
    public String description() {
        return "Sign out from your Anthropic account";
    }

    @Override
    public boolean isEnabled() {
        String disabled = System.getenv("DISABLE_LOGOUT_COMMAND");
        return disabled == null || !"true".equalsIgnoreCase(disabled);
    }

    @Override
    public boolean supportsNonInteractive() {
        return true;
    }

    @Override
    public CompletableFuture<CommandResult> execute(String args, CommandContext context) {
        StringBuilder sb = new StringBuilder();

        // Clear stored credentials
        boolean cleared = clearCredentials();

        if (cleared) {
            sb.append("Successfully logged out.\n");
            sb.append("Run /login to sign in again.\n");
        } else {
            sb.append("No stored credentials found.\n");
            sb.append("You may have been using environment variables for authentication.\n");
        }

        // Clear session data
        context.clearAllContext();

        return CompletableFuture.completedFuture(CommandResult.success(sb.toString()));
    }

    private boolean clearCredentials() {
        boolean cleared = false;

        try {
            // Get config directory
            String configDir = System.getenv("CLAUDE_CONFIG_DIR");
            Path claudeDir;
            if (configDir != null && !configDir.isEmpty()) {
                claudeDir = Paths.get(configDir);
            } else {
                claudeDir = Paths.get(System.getProperty("user.home"), ".claude");
            }

            // Clear OAuth tokens
            Path oauthPath = claudeDir.resolve("oauth_tokens.json");
            if (Files.exists(oauthPath)) {
                Files.delete(oauthPath);
                cleared = true;
            }

            // Clear API key from settings
            Path settingsPath = claudeDir.resolve("settings.json");
            if (Files.exists(settingsPath)) {
                String content = Files.readString(settingsPath);
                // Remove apiKey from settings
                String updated = removeApiKey(content);
                Files.writeString(settingsPath, updated);
                cleared = true;
            }

            // Clear credentials file
            Path credentialsPath = claudeDir.resolve("credentials.json");
            if (Files.exists(credentialsPath)) {
                Files.delete(credentialsPath);
                cleared = true;
            }

            // Clear trusted device token
            Path trustedPath = claudeDir.resolve("trusted_device.json");
            if (Files.exists(trustedPath)) {
                Files.delete(trustedPath);
                cleared = true;
            }

            // Clear session files
            Path sessionsPath = claudeDir.resolve("sessions");
            if (Files.exists(sessionsPath)) {
                Files.walk(sessionsPath)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (Exception ignored) {}
                    });
                cleared = true;
            }

        } catch (Exception e) {
            // Ignore errors
        }

        return cleared;
    }

    /**
     * Remove apiKey field from settings JSON.
     */
    private String removeApiKey(String json) {
        if (json == null || json.isEmpty()) return "{}";

        StringBuilder result = new StringBuilder();
        result.append("{\n");

        boolean first = true;
        int i = 0;

        // Skip opening brace
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);

        while (i < json.length()) {
            // Skip whitespace
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;
            if (i >= json.length() || json.charAt(i) != '"') break;

            // Read key
            i++;
            int keyStart = i;
            while (i < json.length() && json.charAt(i) != '"') {
                if (json.charAt(i) == '\\') i++;
                i++;
            }
            String key = json.substring(keyStart, i);
            i++;

            // Skip to value
            while (i < json.length() && json.charAt(i) != ':') i++;
            i++;
            while (i < json.length() && Character.isWhitespace(json.charAt(i))) i++;

            // Find value end
            int valueStart = i;
            if (i < json.length()) {
                char c = json.charAt(i);
                if (c == '"') {
                    i++;
                    while (i < json.length() && json.charAt(i) != '"') {
                        if (json.charAt(i) == '\\') i++;
                        i++;
                    }
                    i++;
                } else if (c == '{') {
                    int depth = 1;
                    i++;
                    while (i < json.length() && depth > 0) {
                        if (json.charAt(i) == '{') depth++;
                        else if (json.charAt(i) == '}') depth--;
                        i++;
                    }
                } else if (c == '[') {
                    int depth = 1;
                    i++;
                    while (i < json.length() && depth > 0) {
                        if (json.charAt(i) == '[') depth++;
                        else if (json.charAt(i) == ']') depth--;
                        i++;
                    }
                } else {
                    while (i < json.length() && json.charAt(i) != ',' && json.charAt(i) != '}') {
                        i++;
                    }
                }
            }

            // Skip apiKey
            if (!"apiKey".equals(key)) {
                if (!first) result.append(",\n");
                result.append("  \"").append(key).append("\": ");
                result.append(json.substring(valueStart, i).trim());
                first = false;
            }

            // Skip comma
            while (i < json.length() && json.charAt(i) != ',') i++;
            if (i < json.length()) i++;
        }

        result.append("\n}");
        return result.toString();
    }
}