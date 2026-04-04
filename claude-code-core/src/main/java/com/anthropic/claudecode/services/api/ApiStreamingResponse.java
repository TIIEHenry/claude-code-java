/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/api
 */
package com.anthropic.claudecode.services.api;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * API Streaming Response.
 */
public record ApiStreamingResponse(
    String id,
    CompletableFuture<List<Object>> eventsFuture
) {
    /**
     * Get the events.
     */
    public List<Object> getEvents() {
        try {
            return eventsFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new ApiException("Failed to get streaming events", e);
        }
    }

    /**
     * Subscribe to event updates.
     */
    public void subscribe(Consumer<Object> consumer) {
        eventsFuture.thenAccept(events -> events.forEach(consumer));
    }
}