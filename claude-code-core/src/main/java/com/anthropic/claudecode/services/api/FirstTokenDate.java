/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/api/firstTokenDate.ts
 */
package com.anthropic.claudecode.services.api;

import java.time.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * First token date tracking service.
 */
public final class FirstTokenDate {
    private FirstTokenDate() {}

    private static final AtomicReference<LocalDate> firstTokenDate = new AtomicReference<>();

    /**
     * Get the first token date.
     */
    public static LocalDate get() {
        return firstTokenDate.get();
    }

    /**
     * Set the first token date.
     */
    public static void set(LocalDate date) {
        firstTokenDate.set(date);
    }

    /**
     * Set the first token date from timestamp.
     */
    public static void setFromTimestamp(long timestamp) {
        LocalDate date = Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate();
        firstTokenDate.set(date);
    }

    /**
     * Clear the first token date.
     */
    public static void clear() {
        firstTokenDate.set(null);
    }

    /**
     * Check if first token date is set.
     */
    public static boolean isSet() {
        return firstTokenDate.get() != null;
    }

    /**
     * Get first token date as ISO string.
     */
    public static String toISOString() {
        LocalDate date = firstTokenDate.get();
        return date != null ? date.toString() : null;
    }

    /**
     * Parse ISO date string to set first token date.
     */
    public static void fromISOString(String isoDate) {
        if (isoDate != null && !isoDate.isEmpty()) {
            try {
                firstTokenDate.set(LocalDate.parse(isoDate));
            } catch (Exception ignored) {}
        }
    }
}