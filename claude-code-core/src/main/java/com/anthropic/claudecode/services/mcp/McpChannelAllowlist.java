/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/mcp/channelAllowlist
 */
package com.anthropic.claudecode.services.mcp;

import java.time.Instant;

import java.util.*;
import java.util.concurrent.*;

/**
 * Channel allowlist - MCP channel permissions.
 */
public final class McpChannelAllowlist {
    private final Map<String, ChannelPermission> channelPermissions = new ConcurrentHashMap<>();
    private final Set<String> allowedChannels = ConcurrentHashMap.newKeySet();
    private final Set<String> blockedChannels = ConcurrentHashMap.newKeySet();

    /**
     * Channel permission record.
     */
    public record ChannelPermission(
        String channelId,
        PermissionType type,
        String grantedBy,
        Instant grantedAt,
        Instant expiresAt,
        String reason
    ) {
        public boolean isExpired() {
            return expiresAt != null && Instant.now().isAfter(expiresAt);
        }
    }

    /**
     * Permission type enum.
     */
    public enum PermissionType {
        ALLOW,
        BLOCK,
        TEMPORARY
    }

    /**
     * Check if channel is allowed.
     */
    public boolean isAllowed(String channelId) {
        if (blockedChannels.contains(channelId)) {
            return false;
        }

        if (allowedChannels.contains(channelId)) {
            return true;
        }

        ChannelPermission permission = channelPermissions.get(channelId);
        if (permission == null) {
            return false;
        }

        if (permission.isExpired()) {
            channelPermissions.remove(channelId);
            return false;
        }

        return permission.type() == PermissionType.ALLOW ||
               permission.type() == PermissionType.TEMPORARY;
    }

    /**
     * Allow channel.
     */
    public void allow(String channelId, String grantedBy, String reason) {
        allowedChannels.add(channelId);
        blockedChannels.remove(channelId);

        channelPermissions.put(channelId, new ChannelPermission(
            channelId,
            PermissionType.ALLOW,
            grantedBy,
            Instant.now(),
            null,
            reason
        ));
    }

    /**
     * Allow channel temporarily.
     */
    public void allowTemporarily(String channelId, String grantedBy, Duration duration, String reason) {
        Instant expiresAt = Instant.now().plusSeconds(duration.seconds());

        channelPermissions.put(channelId, new ChannelPermission(
            channelId,
            PermissionType.TEMPORARY,
            grantedBy,
            Instant.now(),
            expiresAt,
            reason
        ));
    }

    /**
     * Block channel.
     */
    public void block(String channelId, String grantedBy, String reason) {
        blockedChannels.add(channelId);
        allowedChannels.remove(channelId);

        channelPermissions.put(channelId, new ChannelPermission(
            channelId,
            PermissionType.BLOCK,
            grantedBy,
            Instant.now(),
            null,
            reason
        ));
    }

    /**
     * Remove channel permission.
     */
    public void remove(String channelId) {
        allowedChannels.remove(channelId);
        blockedChannels.remove(channelId);
        channelPermissions.remove(channelId);
    }

    /**
     * Get channel permission.
     */
    public ChannelPermission getPermission(String channelId) {
        ChannelPermission permission = channelPermissions.get(channelId);
        if (permission != null && permission.isExpired()) {
            channelPermissions.remove(channelId);
            return null;
        }
        return permission;
    }

    /**
     * Get all allowed channels.
     */
    public List<String> getAllowedChannels() {
        return new ArrayList<>(allowedChannels);
    }

    /**
     * Get all blocked channels.
     */
    public List<String> getBlockedChannels() {
        return new ArrayList<>(blockedChannels);
    }

    /**
     * Clear all permissions.
     */
    public void clearAll() {
        allowedChannels.clear();
        blockedChannels.clear();
        channelPermissions.clear();
    }

    /**
     * Clear expired permissions.
     */
    public void clearExpired() {
        List<String> expired = channelPermissions.entrySet()
            .stream()
            .filter(e -> e.getValue().isExpired())
            .map(Map.Entry::getKey)
            .toList();

        for (String channelId : expired) {
            channelPermissions.remove(channelId);
            allowedChannels.remove(channelId);
        }
    }

    /**
     * Duration record.
     */
    public record Duration(long seconds) {
        public static Duration ofMinutes(long minutes) {
            return new Duration(minutes * 60);
        }

        public static Duration ofHours(long hours) {
            return new Duration(hours * 3600);
        }

        public static Duration ofDays(long days) {
            return new Duration(days * 86400);
        }
    }

    /**
     * Allowlist summary record.
     */
    public record AllowlistSummary(
        int allowedCount,
        int blockedCount,
        int temporaryCount,
        int expiredCount
    ) {
        public String format() {
            return String.format("Allowed: %d, Blocked: %d, Temporary: %d, Expired: %d",
                allowedCount, blockedCount, temporaryCount, expiredCount);
        }
    }

    /**
     * Get summary.
     */
    public AllowlistSummary getSummary() {
        clearExpired();

        int allowed = allowedChannels.size();
        int blocked = blockedChannels.size();
        int temp = (int) channelPermissions.values()
            .stream()
            .filter(p -> p.type() == PermissionType.TEMPORARY)
            .count();

        return new AllowlistSummary(allowed, blocked, temp, 0);
    }
}