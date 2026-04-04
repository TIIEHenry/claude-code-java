/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code hooks
 */
package com.anthropic.claudecode.hooks;

import java.util.*;

/**
 * Hook context - provides context for hook execution.
 */
public record HookContext(
    String event,
    String toolName,
    Object input,
    Object output,
    Throwable error,
    long timestamp,
    Map<String, Object> metadata
) {
    public HookContext(String event) {
        this(event, null, null, null, null, System.currentTimeMillis(), Map.of());
    }

    public HookContext(String event, String toolName, Object input) {
        this(event, toolName, input, null, null, System.currentTimeMillis(), Map.of());
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String event;
        private String toolName;
        private Object input;
        private Object output;
        private Throwable error;
        private long timestamp = System.currentTimeMillis();
        private Map<String, Object> metadata = new HashMap<>();

        public Builder event(String event) { this.event = event; return this; }
        public Builder toolName(String toolName) { this.toolName = toolName; return this; }
        public Builder input(Object input) { this.input = input; return this; }
        public Builder output(Object output) { this.output = output; return this; }
        public Builder error(Throwable error) { this.error = error; return this; }
        public Builder timestamp(long timestamp) { this.timestamp = timestamp; return this; }
        public Builder metadata(String key, Object value) { this.metadata.put(key, value); return this; }

        public HookContext build() {
            return new HookContext(event, toolName, input, output, error, timestamp, Collections.unmodifiableMap(metadata));
        }
    }
}