/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/mcp/elicitationHandler
 */
package com.anthropic.claudecode.services.mcp;

import java.time.Instant;

import java.util.*;
import java.util.concurrent.*;

/**
 * Elicitation handler - Handle MCP elicitation requests.
 */
public final class McpElicitationHandler {
    private final Map<String, ElicitationRequest> pendingRequests = new ConcurrentHashMap<>();
    private final List<ElicitationListener> listeners = new CopyOnWriteArrayList<>();
    private final Duration defaultTimeout = Duration.ofMinutes(5);

    /**
     * Duration record.
     */
    public record Duration(long seconds) {
        public static Duration ofMinutes(long minutes) {
            return new Duration(minutes * 60);
        }

        public static Duration ofSeconds(long seconds) {
            return new Duration(seconds);
        }
    }

    /**
     * Elicitation request record.
     */
    public record ElicitationRequest(
        String id,
        String serverName,
        String type,
        String prompt,
        Map<String, Object> schema,
        Instant createdAt,
        Instant expiresAt,
        ElicitationStatus status
    ) {
        public static ElicitationRequest create(
            String serverName,
            String type,
            String prompt,
            Map<String, Object> schema,
            Duration timeout
        ) {
            String id = UUID.randomUUID().toString();
            Instant now = Instant.now();
            Instant expires = now.plusSeconds(timeout.seconds());

            return new ElicitationRequest(
                id, serverName, type, prompt, schema, now, expires,
                ElicitationStatus.PENDING
            );
        }

        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }

        public ElicitationRequest withStatus(ElicitationStatus newStatus) {
            return new ElicitationRequest(id, serverName, type, prompt, schema, createdAt, expiresAt, newStatus);
        }
    }

    /**
     * Elicitation status enum.
     */
    public enum ElicitationStatus {
        PENDING,
        ACCEPTED,
        DECLINED,
        EXPIRED,
        ERROR
    }

    /**
     * Elicitation response record.
     */
    public record ElicitationResponse(
        String requestId,
        boolean accepted,
        Map<String, Object> data,
        String reason
    ) {
        public static ElicitationResponse accept(String requestId, Map<String, Object> data) {
            return new ElicitationResponse(requestId, true, data, null);
        }

        public static ElicitationResponse decline(String requestId, String reason) {
            return new ElicitationResponse(requestId, false, null, reason);
        }
    }

    /**
     * Create elicitation request.
     */
    public ElicitationRequest createRequest(
        String serverName,
        String type,
        String prompt,
        Map<String, Object> schema
    ) {
        ElicitationRequest request = ElicitationRequest.create(
            serverName, type, prompt, schema, defaultTimeout
        );

        pendingRequests.put(request.id(), request);
        notifyListeners(request);

        return request;
    }

    /**
     * Respond to elicitation.
     */
    public void respond(ElicitationResponse response) {
        ElicitationRequest request = pendingRequests.get(response.requestId());
        if (request == null || request.isExpired()) {
            return;
        }

        ElicitationStatus status = response.accepted()
            ? ElicitationStatus.ACCEPTED
            : ElicitationStatus.DECLINED;

        pendingRequests.put(response.requestId(), request.withStatus(status));
    }

    /**
     * Get pending request.
     */
    public ElicitationRequest getRequest(String id) {
        ElicitationRequest request = pendingRequests.get(id);
        if (request != null && request.isExpired()) {
            pendingRequests.put(id, request.withStatus(ElicitationStatus.EXPIRED));
            return null;
        }
        return request;
    }

    /**
     * Get all pending requests.
     */
    public List<ElicitationRequest> getPendingRequests() {
        cleanupExpired();
        return pendingRequests.values()
            .stream()
            .filter(r -> r.status() == ElicitationStatus.PENDING)
            .toList();
    }

    /**
     * Cancel request.
     */
    public void cancelRequest(String id) {
        ElicitationRequest request = pendingRequests.get(id);
        if (request != null) {
            pendingRequests.put(id, request.withStatus(ElicitationStatus.DECLINED));
        }
    }

    /**
     * Cleanup expired requests.
     */
    private void cleanupExpired() {
        for (Map.Entry<String, ElicitationRequest> entry : pendingRequests.entrySet()) {
            if (entry.getValue().isExpired()) {
                pendingRequests.put(entry.getKey(), entry.getValue().withStatus(ElicitationStatus.EXPIRED));
            }
        }
    }

    /**
     * Add listener.
     */
    public void addListener(ElicitationListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove listener.
     */
    public void removeListener(ElicitationListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(ElicitationRequest request) {
        for (ElicitationListener listener : listeners) {
            listener.onElicitation(request);
        }
    }

    /**
     * Elicitation listener interface.
     */
    public interface ElicitationListener {
        void onElicitation(ElicitationRequest request);
    }

    /**
     * Elicitation type enum.
     */
    public enum ElicitationType {
        INFORMATION,
        CONFIRMATION,
        INPUT,
        SELECTION,
        FILE_PICKER,
        CREDENTIALS
    }
}