/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code LSP message connection.
 */
package com.anthropic.claudecode.services.lsp;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * JSON-RPC message connection for LSP communication.
 */
public final class MessageConnection {
    private final InputStream input;
    private final OutputStream output;
    private final Map<String, Consumer<Object>> notificationHandlers = new ConcurrentHashMap<>();
    private final Map<String, Function<Object, Object>> requestHandlers = new ConcurrentHashMap<>();
    private volatile boolean listening = false;
    private volatile boolean disposed = false;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final BlockingQueue<CompletableFuture<Object>> pendingRequests = new LinkedBlockingQueue<>();

    public MessageConnection(InputStream input, OutputStream output) {
        this.input = input;
        this.output = output;
    }

    /**
     * Start listening for messages.
     */
    public void listen() {
        if (listening) return;
        listening = true;

        executor.submit(() -> {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                String line;
                StringBuilder contentBuilder = new StringBuilder();
                int contentLength = -1;

                while (!disposed && (line = reader.readLine()) != null) {
                    // Parse headers
                    if (line.startsWith("Content-Length: ")) {
                        contentLength = Integer.parseInt(line.substring(16).trim());
                    } else if (line.isEmpty() && contentLength > 0) {
                        // End of headers, read content
                        char[] content = new char[contentLength];
                        reader.read(content, 0, contentLength);
                        String message = new String(content);
                        contentLength = -1;

                        // Parse and handle message
                        handleMessage(message);
                    }
                }
            } catch (IOException e) {
                if (!disposed) {
                    errorHandler.accept(e);
                }
            }
        });
    }

    /**
     * Send a request and wait for response.
     */
    public <T> CompletableFuture<T> sendRequest(String method, Object params) {
        int requestId = generateRequestId();

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", requestId);
        request.put("method", method);
        request.put("params", params);

        CompletableFuture<T> future = new CompletableFuture<>();
        pendingRequests.add((CompletableFuture<Object>) future);

        sendMessage(request);

        return future;
    }

    /**
     * Send a notification (no response expected).
     */
    public CompletableFuture<Void> sendNotification(String method, Object params) {
        return CompletableFuture.runAsync(() -> {
            Map<String, Object> notification = new LinkedHashMap<>();
            notification.put("jsonrpc", "2.0");
            notification.put("method", method);
            notification.put("params", params);

            sendMessage(notification);
        });
    }

    /**
     * Register notification handler.
     */
    public void onNotification(String method, Consumer<Object> handler) {
        notificationHandlers.put(method, handler);
    }

    /**
     * Register request handler.
     */
    public void onRequest(String method, Function<Object, Object> handler) {
        requestHandlers.put(method, handler);
    }

    /**
     * Register error handler.
     */
    public void onError(Consumer<Throwable> handler) {
        this.errorHandler = handler;
    }

    private Consumer<Throwable> errorHandler = e -> {};

    /**
     * Register close handler.
     */
    public void onClose(Runnable handler) {
        this.closeHandler = handler;
    }

    private Runnable closeHandler = () -> {};

    /**
     * Dispose the connection.
     */
    public void dispose() {
        disposed = true;
        listening = false;
        executor.shutdown();
        try {
            input.close();
            output.close();
        } catch (IOException e) {
            // Ignore close errors
        }
        closeHandler.run();
    }

    private void sendMessage(Map<String, Object> message) {
        try {
            String content = toJson(message);
            String header = "Content-Length: " + content.length() + "\r\n\r\n";

            output.write(header.getBytes());
            output.write(content.getBytes());
            output.flush();
        } catch (IOException e) {
            errorHandler.accept(e);
        }
    }

    private void handleMessage(String message) {
        try {
            Map<String, Object> parsed = parseJson(message);

            // Check if this is a response to a pending request
            if (parsed.containsKey("id") && !parsed.containsKey("method")) {
                // Response to request
                int id = ((Number) parsed.get("id")).intValue();

                if (parsed.containsKey("error")) {
                    Map<String, Object> error = (Map<String, Object>) parsed.get("error");
                    String errorMsg = (String) error.get("message");
                    // Find pending request and complete with error
                    errorHandler.accept(new RuntimeException(errorMsg));
                } else if (parsed.containsKey("result")) {
                    // Find pending request and complete with result
                    Object result = parsed.get("result");
                    // Would match request ID to pending future
                    // For simplicity, complete first pending
                    CompletableFuture<Object> future = pendingRequests.poll();
                    if (future != null) {
                        future.complete(result);
                    }
                }
            } else if (parsed.containsKey("method")) {
                // Notification or request from server
                String method = (String) parsed.get("method");
                Object params = parsed.get("params");

                if (parsed.containsKey("id")) {
                    // Request from server - needs response
                    int id = ((Number) parsed.get("id")).intValue();
                    Function<Object, Object> handler = requestHandlers.get(method);

                    if (handler != null) {
                        Object result = handler.apply(params);
                        sendResponse(id, result);
                    } else {
                        sendError(id, "Method not found: " + method);
                    }
                } else {
                    // Notification from server
                    Consumer<Object> handler = notificationHandlers.get(method);
                    if (handler != null) {
                        handler.accept(params);
                    }
                }
            }
        } catch (Exception e) {
            errorHandler.accept(e);
        }
    }

    private void sendResponse(int id, Object result) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        response.put("result", result);
        sendMessage(response);
    }

    private void sendError(int id, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("jsonrpc", "2.0");
        response.put("id", id);
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", -32601);
        error.put("message", message);
        response.put("error", error);
        sendMessage(response);
    }

    private int generateRequestId() {
        return (int) (System.currentTimeMillis() % 1000000);
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value == null) {
                sb.append("null");
            } else if (value instanceof String) {
                sb.append("\"").append(escapeJson((String) value)).append("\"");
            } else if (value instanceof Number || value instanceof Boolean) {
                sb.append(value);
            } else if (value instanceof Map) {
                sb.append(toJson((Map<String, Object>) value));
            } else if (value instanceof List) {
                sb.append(toJsonList((List<?>) value));
            } else {
                sb.append("\"").append(escapeJson(value.toString())).append("\"");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    private String toJsonList(List<?> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        boolean first = true;
        for (Object item : list) {
            if (!first) sb.append(",");
            first = false;
            if (item == null) {
                sb.append("null");
            } else if (item instanceof String) {
                sb.append("\"").append(escapeJson((String) item)).append("\"");
            } else if (item instanceof Number || item instanceof Boolean) {
                sb.append(item);
            } else if (item instanceof Map) {
                sb.append(toJson((Map<String, Object>) item));
            } else if (item instanceof List) {
                sb.append(toJsonList((List<?>) item));
            } else {
                sb.append("\"").append(escapeJson(item.toString())).append("\"");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private Map<String, Object> parseJson(String json) {
        // Simple JSON parser - would use Jackson in real implementation
        // This is a placeholder for the port
        return new LinkedHashMap<>();
    }
}