/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/claudeInChrome/common
 */
package com.anthropic.claudecode.utils.claudeInChrome;

import java.util.*;

/**
 * Chrome integration common - Common utilities for Chrome integration.
 */
public final class ChromeCommon {
    private static final String NATIVE_HOST_NAME = "com.anthropic.claudecode";
    private static final int DEFAULT_PORT = 9515;

    /**
     * Chrome connection status enum.
     */
    public enum ConnectionStatus {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }

    /**
     * Chrome connection info record.
     */
    public record ChromeConnectionInfo(
        ConnectionStatus status,
        int port,
        String sessionId,
        long connectedAt,
        String error
    ) {
        public static ChromeConnectionInfo disconnected() {
            return new ChromeConnectionInfo(ConnectionStatus.DISCONNECTED, 0, null, 0, null);
        }

        public static ChromeConnectionInfo connected(int port, String sessionId) {
            return new ChromeConnectionInfo(
                ConnectionStatus.CONNECTED,
                port,
                sessionId,
                System.currentTimeMillis(),
                null
            );
        }

        public static ChromeConnectionInfo error(String message) {
            return new ChromeConnectionInfo(ConnectionStatus.ERROR, 0, null, 0, message);
        }
    }

    /**
     * Chrome message record.
     */
    public record ChromeMessage(
        String type,
        Map<String, Object> data,
        long timestamp
    ) {
        public static ChromeMessage of(String type, Map<String, Object> data) {
            return new ChromeMessage(type, data, System.currentTimeMillis());
        }

        public Object getField(String key) {
            return data.get(key);
        }

        public String getStringField(String key) {
            Object value = data.get(key);
            return value != null ? value.toString() : null;
        }
    }

    /**
     * Get native host name.
     */
    public static String getNativeHostName() {
        return NATIVE_HOST_NAME;
    }

    /**
     * Get default port.
     */
    public static int getDefaultPort() {
        return DEFAULT_PORT;
    }

    /**
     * Chrome extension config record.
     */
    public record ExtensionConfig(
        String extensionId,
        String version,
        boolean enabled,
        List<String> permissions
    ) {}

    /**
     * Browser info record.
     */
    public record BrowserInfo(
        String name,
        String version,
        String platform,
        boolean isHeadless
    ) {}

    /**
     * Tab info record.
     */
    public record TabInfo(
        int tabId,
        int windowId,
        String url,
        String title,
        boolean active,
        boolean isClaudePage
    ) {
        public boolean matchesUrl(String pattern) {
            return url.contains(pattern);
        }
    }

    /**
     * Build native host manifest.
     */
    public static String buildNativeHostManifest(String path) {
        return String.format(
            "{\"name\": \"%s\", \"description\": \"Claude Code Native Host\", \"path\": \"%s\", \"type\": \"stdio\", \"allowed_origins\": [\"chrome-extension://*/\"]}",
            NATIVE_HOST_NAME,
            path
        );
    }

    /**
     * Message types enum.
     */
    public enum MessageType {
        CONNECT,
        DISCONNECT,
        PROMPT,
        RESPONSE,
        STATUS,
        ERROR,
        AUTH,
        CONFIG,
        ACTION
    }
}