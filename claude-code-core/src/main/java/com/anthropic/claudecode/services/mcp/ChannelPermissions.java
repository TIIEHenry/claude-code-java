/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/mcp/channelPermissions
 */
package com.anthropic.claudecode.services.mcp;

import com.anthropic.claudecode.services.analytics.GrowthBookService;

import java.time.Instant;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.*;

/**
 * Channel permissions - Permission prompts over channels (Telegram, iMessage, Discord).
 *
 * Mirrors BridgePermissionCallbacks - when CC hits a permission dialog,
 * it ALSO sends the prompt via active channels and races the reply against
 * local UI / bridge / hooks / classifier. First resolver wins via claim().
 */
public final class ChannelPermissions {
    private static final Pattern PERMISSION_REPLY_RE =
        Pattern.compile("^\\s*(y|yes|n|no)\\s+([a-km-z]{5})\\s*$", Pattern.CASE_INSENSITIVE);

    // 25-letter alphabet: a-z minus 'l' (looks like 1/I)
    private static final String ID_ALPHABET = "abcdefghijkmnopqrstuvwxyz";

    // Substring blocklist for generated IDs
    private static final Set<String> ID_AVOID_SUBSTRINGS = Set.of(
        "fuck", "shit", "cunt", "cock", "dick", "twat", "piss", "crap",
        "bitch", "whore", "ass", "tit", "cum", "fag", "dyke", "nig",
        "kike", "rape", "nazi", "damn", "poo", "pee", "wank", "anus"
    );

    /**
     * Create channel permissions.
     */
    public ChannelPermissions() {
        // No instance state needed - GrowthBookService uses static methods
    }

    /**
     * Channel permission response.
     */
    public record ChannelPermissionResponse(
        String behavior,  // "allow" or "deny"
        String fromServer
    ) {}

    /**
     * Channel permission callbacks.
     */
    public interface ChannelPermissionCallbacks {
        /**
         * Register a resolver for a request ID.
         * Returns unsubscribe function.
         */
        java.util.function.Supplier<Void> onResponse(
            String requestId,
            java.util.function.Consumer<ChannelPermissionResponse> handler
        );

        /**
         * Resolve a pending request from a structured channel event.
         * Returns true if the ID was pending.
         */
        boolean resolve(String requestId, String behavior, String fromServer);
    }

    /**
     * Check if channel permission relay is enabled.
     */
    public boolean isChannelPermissionRelayEnabled() {
        return GrowthBookService.getFeatureValueCached("tengu_harbor_permissions", false);
    }

    /**
     * Get permission reply regex (exported for plugins).
     */
    public static Pattern getPermissionReplyRegex() {
        return PERMISSION_REPLY_RE;
    }

    /**
     * Short ID from a toolUseID.
     * 5 letters from a 25-char alphabet.
     */
    public static String shortRequestId(String toolUseID) {
        String candidate = hashToId(toolUseID);
        for (int salt = 0; salt < 10; salt++) {
            if (!containsBlockedSubstring(candidate)) {
                return candidate;
            }
            candidate = hashToId(toolUseID + ":" + salt);
        }
        return candidate;
    }

    /**
     * Truncate tool input to phone-sized preview.
     */
    public static String truncateForPreview(Object input) {
        try {
            String s = serialize(input);
            return s.length() > 200 ? s.substring(0, 200) + "…" : s;
        } catch (Exception e) {
            return "(unserializable)";
        }
    }

    /**
     * Filter MCP clients that can relay permission prompts.
     */
    public static <T extends McpClientCapabilities> List<T> filterPermissionRelayClients(
        List<T> clients,
        java.util.function.Predicate<String> isInAllowlist
    ) {
        List<T> filtered = new ArrayList<>();
        for (T client : clients) {
            if (client.isConnected() &&
                isInAllowlist.test(client.getName()) &&
                client.hasCapability("claude/channel") &&
                client.hasCapability("claude/channel/permission")) {
                filtered.add(client);
            }
        }
        return filtered;
    }

    /**
     * Create channel permission callbacks.
     */
    public static ChannelPermissionCallbacks createCallbacks() {
        Map<String, java.util.function.Consumer<ChannelPermissionResponse>> pending =
            new ConcurrentHashMap<>();

        return new ChannelPermissionCallbacks() {
            @Override
            public java.util.function.Supplier<Void> onResponse(
                String requestId,
                java.util.function.Consumer<ChannelPermissionResponse> handler
            ) {
                String key = requestId.toLowerCase();
                pending.put(key, handler);
                return () -> {
                    pending.remove(key);
                    return null;
                };
            }

            @Override
            public boolean resolve(String requestId, String behavior, String fromServer) {
                String key = requestId.toLowerCase();
                java.util.function.Consumer<ChannelPermissionResponse> resolver = pending.remove(key);
                if (resolver == null) {
                    return false;
                }
                resolver.accept(new ChannelPermissionResponse(behavior, fromServer));
                return true;
            }
        };
    }

    // Helper methods
    private static String hashToId(String input) {
        // FNV-1a hash to uint32, then base-25 encode
        int h = 0x811c9dc5;
        for (int i = 0; i < input.length(); i++) {
            h ^= input.charAt(i);
            // Java equivalent of JavaScript Math.imul - 32-bit integer multiplication
            h = (h * 0x01000193);  // automatically truncates to int in Java
        }
        h = h >>> 0;  // ensure unsigned interpretation

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(ID_ALPHABET.charAt(h % 25));
            h = h / 25;
        }
        return sb.toString();
    }

    private static boolean containsBlockedSubstring(String candidate) {
        for (String bad : ID_AVOID_SUBSTRINGS) {
            if (candidate.contains(bad)) {
                return true;
            }
        }
        return false;
    }

    private static String serialize(Object obj) {
        // Simplified JSON serialization
        if (obj == null) return "null";
        if (obj instanceof String) return "\"" + obj + "\"";
        if (obj instanceof Number || obj instanceof Boolean) return obj.toString();
        return obj.toString();
    }

    // Interface for MCP client capabilities
    public interface McpClientCapabilities {
        boolean isConnected();
        String getName();
        boolean hasCapability(String capability);
    }
}