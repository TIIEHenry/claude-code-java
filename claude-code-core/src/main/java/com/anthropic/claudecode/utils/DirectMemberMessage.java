/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code direct member message utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import java.nio.file.*;
import java.time.*;

/**
 * Parse `@agent-name message` syntax for direct team member messaging.
 */
public final class DirectMemberMessage {
    private DirectMemberMessage() {}

    private static final Pattern DIRECT_MESSAGE_PATTERN = Pattern.compile("^@([\\w-]+)\\s+(.+)$", Pattern.DOTALL);

    /**
     * Parsed direct message result.
     */
    public record ParsedDirectMessage(String recipientName, String message) {}

    /**
     * Direct message result.
     */
    public sealed interface DirectMessageResult permits
            DirectMessageResult.Success,
            DirectMessageResult.Failure {

        boolean success();

        public static final class Success implements DirectMessageResult {
            private final String recipientName;

            public Success(String recipientName) {
                this.recipientName = recipientName;
            }

            public String recipientName() { return recipientName; }
            @Override public boolean success() { return true; }
        }

        public static final class Failure implements DirectMessageResult {
            private final String error;
            private final String recipientName;

            public Failure(String error, String recipientName) {
                this.error = error;
                this.recipientName = recipientName;
            }

            public String error() { return error; }
            public String recipientName() { return recipientName; }
            @Override public boolean success() { return false; }
        }
    }

    /**
     * Team context for messaging.
     */
    public record TeamContext(
            String teamName,
            Map<String, Teammate> teammates
    ) {}

    /**
     * Teammate record.
     */
    public record Teammate(String name, String id) {}

    /**
     * Parse `@agent-name message` syntax.
     */
    public static ParsedDirectMessage parseDirectMemberMessage(String input) {
        Matcher matcher = DIRECT_MESSAGE_PATTERN.matcher(input);
        if (!matcher.matches()) {
            return null;
        }

        String recipientName = matcher.group(1);
        String message = matcher.group(2);

        if (recipientName == null || message == null) {
            return null;
        }

        String trimmedMessage = message.trim();
        if (trimmedMessage.isEmpty()) {
            return null;
        }

        return new ParsedDirectMessage(recipientName, trimmedMessage);
    }

    /**
     * Send a direct message to a team member.
     */
    public static CompletableFuture<DirectMessageResult> sendDirectMemberMessage(
            String recipientName,
            String message,
            TeamContext teamContext) {

        return CompletableFuture.supplyAsync(() -> {
            if (teamContext == null) {
                return new DirectMessageResult.Failure("no_team_context", null);
            }

            // Find team member by name
            Teammate member = null;
            if (teamContext.teammates() != null) {
                for (Teammate t : teamContext.teammates().values()) {
                    if (t.name().equals(recipientName)) {
                        member = t;
                        break;
                    }
                }
            }

            if (member == null) {
                return new DirectMessageResult.Failure("unknown_recipient", recipientName);
            }

            // Write to mailbox
            try {
                writeToMailbox(recipientName, message, teamContext.teamName(), member.id());
            } catch (Exception e) {
                return new DirectMessageResult.Failure("mailbox_error: " + e.getMessage(), recipientName);
            }

            return new DirectMessageResult.Success(recipientName);
        });
    }

    /**
     * Write message to mailbox.
     */
    private static void writeToMailbox(String recipientName, String message, String teamName, String recipientId) throws Exception {
        // Get mailbox path
        String home = System.getProperty("user.home");
        Path mailboxDir = Paths.get(home, ".claude", "teams", teamName, "mailbox");
        Files.createDirectories(mailboxDir);

        // Create message file
        String timestamp = Instant.now().toString().replace(":", "-").replace(".", "-");
        String filename = recipientId + "-" + timestamp + ".json";
        Path messageFile = mailboxDir.resolve(filename);

        // Build message JSON
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"recipient\": \"").append(escapeJson(recipientName)).append("\",\n");
        json.append("  \"recipientId\": \"").append(escapeJson(recipientId)).append("\",\n");
        json.append("  \"message\": \"").append(escapeJson(message)).append("\",\n");
        json.append("  \"team\": \"").append(escapeJson(teamName)).append("\",\n");
        json.append("  \"timestamp\": \"").append(Instant.now().toString()).append("\",\n");
        json.append("  \"read\": false\n");
        json.append("}");

        Files.writeString(messageFile, json.toString());
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}