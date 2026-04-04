/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/mcp/InProcessTransport
 */
package com.anthropic.claudecode.services.mcp.transports;

import java.util.*;
import java.util.concurrent.*;

/**
 * In-process transport - In-process MCP transport.
 */
public final class InProcessTransport implements McpTransport {
    private final BlockingQueue<String> incomingQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<String> outgoingQueue = new LinkedBlockingQueue<>();
    private final List<McpTransport.MessageHandler> handlers = new CopyOnWriteArrayList<>();
    private volatile boolean connected = false;

    /**
     * Send message.
     */
    @Override
    public void send(String message) {
        if (!connected) {
            throw new IllegalStateException("Transport not connected");
        }
        outgoingQueue.offer(message);
    }

    /**
     * Receive message (blocking).
     */
    public String receive() throws InterruptedException {
        return incomingQueue.take();
    }

    /**
     * Receive message with timeout.
     */
    public String receive(long timeout, TimeUnit unit) throws InterruptedException {
        return incomingQueue.poll(timeout, unit);
    }

    /**
     * Inject message into incoming queue (for testing).
     */
    public void injectMessage(String message) {
        incomingQueue.offer(message);
    }

    /**
     * Get outgoing messages (for testing).
     */
    public List<String> getOutgoingMessages() {
        List<String> messages = new ArrayList<>();
        outgoingQueue.drainTo(messages);
        return messages;
    }

    /**
     * Connect transport.
     */
    @Override
    public void connect() {
        connected = true;
    }

    /**
     * Disconnect transport.
     */
    @Override
    public void disconnect() {
        connected = false;
        incomingQueue.clear();
        outgoingQueue.clear();
    }

    /**
     * Check if connected.
     */
    @Override
    public boolean isConnected() {
        return connected;
    }

    /**
     * Add message handler.
     */
    @Override
    public void addHandler(McpTransport.MessageHandler handler) {
        handlers.add(handler);
    }

    /**
     * Remove message handler.
     */
    @Override
    public void removeHandler(McpTransport.MessageHandler handler) {
        handlers.remove(handler);
    }

    /**
     * Process incoming messages.
     */
    public void processMessages() {
        String message;
        while ((message = incomingQueue.poll()) != null) {
            for (McpTransport.MessageHandler handler : handlers) {
                handler.handleMessage(message);
            }
        }
    }

    /**
     * Transport info record.
     */
    public record TransportInfo(
        String type,
        boolean connected,
        int pendingIncoming,
        int pendingOutgoing
    ) {
        public String format() {
            return String.format("InProcess[connected=%s, in=%d, out=%d]",
                connected, pendingIncoming, pendingOutgoing);
        }
    }

    /**
     * Get transport info.
     */
    public TransportInfo getInfo() {
        return new TransportInfo(
            "in-process",
            connected,
            incomingQueue.size(),
            outgoingQueue.size()
        );
    }
}