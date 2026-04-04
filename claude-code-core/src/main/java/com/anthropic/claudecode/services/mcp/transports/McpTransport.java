/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/mcp/transport
 */
package com.anthropic.claudecode.services.mcp.transports;

/**
 * MCP transport interface - Base interface for MCP transports.
 */
public interface McpTransport {

    /**
     * Send message.
     */
    void send(String message);

    /**
     * Connect transport.
     */
    void connect();

    /**
     * Disconnect transport.
     */
    void disconnect();

    /**
     * Check if connected.
     */
    boolean isConnected();

    /**
     * Add message handler.
     */
    void addHandler(MessageHandler handler);

    /**
     * Remove message handler.
     */
    void removeHandler(MessageHandler handler);

    /**
     * Message handler interface.
     */
    interface MessageHandler {
        void handleMessage(String message);
    }
}