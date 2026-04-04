/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/Cursor
 */
package com.anthropic.claudecode.utils;

import java.util.concurrent.atomic.*;

/**
 * Cursor - Position tracking for pagination and iteration.
 */
public final class Cursor {
    private final AtomicLong position = new AtomicLong(0);
    private final AtomicLong limit = new AtomicLong(Long.MAX_VALUE);
    private final AtomicBoolean exhausted = new AtomicBoolean(false);

    /**
     * Create a cursor at position 0.
     */
    public Cursor() {}

    /**
     * Create a cursor with a limit.
     */
    public Cursor(long limit) {
        this.limit.set(limit);
    }

    /**
     * Get current position.
     */
    public long getPosition() {
        return position.get();
    }

    /**
     * Set position.
     */
    public void setPosition(long pos) {
        position.set(pos);
    }

    /**
     * Advance position by 1.
     */
    public long advance() {
        return position.incrementAndGet();
    }

    /**
     * Advance position by n.
     */
    public long advance(long n) {
        return position.addAndGet(n);
    }

    /**
     * Get the limit.
     */
    public long getLimit() {
        return limit.get();
    }

    /**
     * Set the limit.
     */
    public void setLimit(long limit) {
        this.limit.set(limit);
    }

    /**
     * Check if cursor has more items.
     */
    public boolean hasMore() {
        return !exhausted.get() && position.get() < limit.get();
    }

    /**
     * Mark cursor as exhausted.
     */
    public void markExhausted() {
        exhausted.set(true);
    }

    /**
     * Check if cursor is exhausted.
     */
    public boolean isExhausted() {
        return exhausted.get();
    }

    /**
     * Reset cursor to beginning.
     */
    public void reset() {
        position.set(0);
        exhausted.set(false);
    }

    /**
     * Create a cursor for pagination.
     */
    public static Cursor forPage(int page, int pageSize) {
        Cursor cursor = new Cursor(page * pageSize + pageSize);
        cursor.setPosition(page * pageSize);
        return cursor;
    }

    /**
     * Get remaining count.
     */
    public long getRemaining() {
        long remaining = limit.get() - position.get();
        return Math.max(0, remaining);
    }

    /**
     * Get progress as percentage (0-100).
     */
    public double getProgressPercent() {
        long lim = limit.get();
        if (lim <= 0) return 0;
        return (position.get() * 100.0) / lim;
    }

    /**
     * Encode cursor as string for serialization.
     */
    public String encode() {
        return position.get() + ":" + limit.get() + ":" + exhausted.get();
    }

    /**
     * Decode cursor from string.
     */
    public static Cursor decode(String encoded) {
        if (encoded == null || encoded.isEmpty()) {
            return new Cursor();
        }

        String[] parts = encoded.split(":");
        Cursor cursor = new Cursor();

        if (parts.length >= 1) {
            cursor.setPosition(Long.parseLong(parts[0]));
        }
        if (parts.length >= 2) {
            cursor.setLimit(Long.parseLong(parts[1]));
        }
        if (parts.length >= 3) {
            cursor.exhausted.set(Boolean.parseBoolean(parts[2]));
        }

        return cursor;
    }

    @Override
    public String toString() {
        return "Cursor{position=" + position.get() + ", limit=" + limit.get() + ", exhausted=" + exhausted.get() + "}";
    }
}