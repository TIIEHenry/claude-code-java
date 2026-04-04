/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code Task context
 */
package com.anthropic.claudecode.tasks;

import java.util.*;

/**
 * Task execution context.
 */
public final class TaskContext {
    private final String taskId;
    private final Map<String, Object> properties;
    private volatile boolean cancelled = false;

    public TaskContext() {
        this.taskId = UUID.randomUUID().toString();
        this.properties = new HashMap<>();
    }

    public TaskContext(String taskId) {
        this.taskId = taskId;
        this.properties = new HashMap<>();
    }

    /**
     * Get task ID.
     */
    public String getTaskId() {
        return taskId;
    }

    /**
     * Get a property.
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }

    /**
     * Get a property with default value.
     */
    public Object getPropertyOrDefault(String key, Object defaultValue) {
        Object value = properties.get(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Set a property.
     */
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }

    /**
     * Check if cancelled.
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Cancel the task.
     */
    public void cancel() {
        this.cancelled = true;
    }

    /**
     * Get all properties.
     */
    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }
}