/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/claudeInChrome/chromeNativeHost
 */
package com.anthropic.claudecode.utils.claudeInChrome;

import java.util.*;
import java.util.concurrent.*;
import java.io.*;

/**
 * Chrome native host - Native messaging host for Chrome extension.
 */
public final class ChromeNativeHost {
    private volatile boolean running = false;
    private final BlockingQueue<ChromeCommon.ChromeMessage> messageQueue = new LinkedBlockingQueue<>();
    private final List<MessageHandler> handlers = new CopyOnWriteArrayList<>();
    private Thread readerThread;
    private Thread writerThread;

    /**
     * Start native host.
     */
    public void start() {
        if (running) return;

        running = true;

        // Start reader thread
        readerThread = new Thread(this::readMessages);
        readerThread.start();

        // Start writer thread
        writerThread = new Thread(this::writeMessages);
        writerThread.start();
    }

    /**
     * Stop native host.
     */
    public void stop() {
        running = false;

        if (readerThread != null) {
            readerThread.interrupt();
        }
        if (writerThread != null) {
            writerThread.interrupt();
        }
    }

    /**
     * Read messages from Chrome.
     */
    private void readMessages() {
        try {
            InputStream input = System.in;

            while (running) {
                // Read message length (4 bytes)
                byte[] lengthBytes = new byte[4];
                int read = input.read(lengthBytes);
                if (read < 4) break;

                int length = bytesToInt(lengthBytes);

                // Read message body
                byte[] messageBytes = new byte[length];
                read = input.read(messageBytes);
                if (read < length) break;

                String messageStr = new String(messageBytes, "UTF-8");

                // Parse message
                ChromeCommon.ChromeMessage message = parseMessage(messageStr);
                handleMessage(message);
            }
        } catch (Exception e) {
            handleError(e);
        }
    }

    /**
     * Write messages to Chrome.
     */
    private void writeMessages() {
        try {
            OutputStream output = System.out;

            while (running) {
                ChromeCommon.ChromeMessage message = messageQueue.poll(1, TimeUnit.SECONDS);
                if (message != null) {
                    String messageStr = serializeMessage(message);
                    byte[] messageBytes = messageStr.getBytes("UTF-8");

                    // Write length prefix
                    output.write(intToBytes(messageBytes.length));
                    output.write(messageBytes);
                    output.flush();
                }
            }
        } catch (Exception e) {
            handleError(e);
        }
    }

    /**
     * Send message to Chrome.
     */
    public void sendMessage(ChromeCommon.ChromeMessage message) {
        messageQueue.offer(message);
    }

    /**
     * Add message handler.
     */
    public void addHandler(MessageHandler handler) {
        handlers.add(handler);
    }

    /**
     * Remove message handler.
     */
    public void removeHandler(MessageHandler handler) {
        handlers.remove(handler);
    }

    /**
     * Handle received message.
     */
    private void handleMessage(ChromeCommon.ChromeMessage message) {
        for (MessageHandler handler : handlers) {
            handler.handle(message);
        }
    }

    /**
     * Handle error.
     */
    private void handleError(Exception e) {
        ChromeCommon.ChromeMessage error = ChromeCommon.ChromeMessage.of(
            ChromeCommon.MessageType.ERROR.name(),
            Map.of("error", e.getMessage())
        );
        sendMessage(error);
    }

    /**
     * Parse message from JSON.
     */
    private ChromeCommon.ChromeMessage parseMessage(String json) {
        // Simple parsing
        Map<String, Object> data = new HashMap<>();
        data.put("raw", json);
        return ChromeCommon.ChromeMessage.of("raw", data);
    }

    /**
     * Serialize message to JSON.
     */
    private String serializeMessage(ChromeCommon.ChromeMessage message) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"").append(message.type()).append("\"");
        if (message.data() != null && !message.data().isEmpty()) {
            sb.append(",\"data\":{");
            boolean first = true;
            for (Map.Entry<String, Object> entry : message.data().entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(entry.getKey()).append("\":");
                sb.append("\"").append(entry.getValue()).append("\"");
                first = false;
            }
            sb.append("}");
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert bytes to int.
     */
    private int bytesToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) |
               ((bytes[1] & 0xFF) << 16) |
               ((bytes[2] & 0xFF) << 8) |
               (bytes[3] & 0xFF);
    }

    /**
     * Convert int to bytes.
     */
    private byte[] intToBytes(int value) {
        return new byte[] {
            (byte) (value >> 24),
            (byte) (value >> 16),
            (byte) (value >> 8),
            (byte) value
        };
    }

    /**
     * Message handler interface.
     */
    public interface MessageHandler {
        void handle(ChromeCommon.ChromeMessage message);
    }

    /**
     * Check if running.
     */
    public boolean isRunning() {
        return running;
    }
}