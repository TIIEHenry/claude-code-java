/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/mcp/SdkControlTransport
 */
package com.anthropic.claudecode.services.mcp.transports;

import java.util.*;
import java.util.concurrent.*;

/**
 * SDK control transport - SDK control MCP transport.
 */
public final class SdkControlTransport implements McpTransport {
    private final String endpoint;
    private volatile boolean connected = false;
    private final List<McpTransport.MessageHandler> handlers = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> heartbeatFuture;

    /**
     * Create SDK control transport.
     */
    public SdkControlTransport(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Send message.
     */
    @Override
    public void send(String message) {
        if (!connected) {
            throw new IllegalStateException("Transport not connected");
        }
        // Would send via HTTP/WebSocket to endpoint
    }

    /**
     * Connect.
     */
    @Override
    public void connect() {
        connected = true;
        startHeartbeat();
    }

    /**
     * Disconnect.
     */
    @Override
    public void disconnect() {
        connected = false;
        stopHeartbeat();
    }

    /**
     * Check if connected.
     */
    @Override
    public boolean isConnected() {
        return connected;
    }

    /**
     * Add handler.
     */
    @Override
    public void addHandler(McpTransport.MessageHandler handler) {
        handlers.add(handler);
    }

    /**
     * Remove handler.
     */
    @Override
    public void removeHandler(McpTransport.MessageHandler handler) {
        handlers.remove(handler);
    }

    /**
     * Start heartbeat.
     */
    private void startHeartbeat() {
        heartbeatFuture = scheduler.scheduleAtFixedRate(() -> {
            if (connected) {
                sendHeartbeat();
            }
        }, 30, 30, TimeUnit.SECONDS);
    }

    /**
     * Stop heartbeat.
     */
    private void stopHeartbeat() {
        if (heartbeatFuture != null) {
            heartbeatFuture.cancel(false);
        }
    }

    /**
     * Send heartbeat.
     */
    private void sendHeartbeat() {
        // Would send heartbeat message
    }

    /**
     * Handle incoming message.
     */
    public void handleIncoming(String message) {
        for (McpTransport.MessageHandler handler : handlers) {
            handler.handleMessage(message);
        }
    }

    /**
     * Shutdown.
     */
    public void shutdown() {
        disconnect();
        scheduler.shutdown();
    }

    /**
     * Transport config record.
     */
    public record SdkTransportConfig(
        String endpoint,
        String apiKey,
        int timeoutMs,
        int heartbeatIntervalSec
    ) {
        public static SdkTransportConfig defaultConfig(String endpoint) {
            return new SdkTransportConfig(endpoint, null, 30000, 30);
        }
    }
}