/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/stringUtils.ts EndTruncatingAccumulator
 */
package com.anthropic.claudecode.utils;

/**
 * A string accumulator that safely handles large outputs by truncating from the end
 * when a size limit is exceeded. This prevents memory issues while preserving
 * the beginning of the output.
 */
public final class EndTruncatingAccumulator {
    private StringBuilder content = new StringBuilder();
    private boolean isTruncated = false;
    private long totalCharsReceived = 0;

    private final int maxSize;

    public static final int DEFAULT_MAX_SIZE = StringUtil.MAX_STRING_LENGTH;

    public EndTruncatingAccumulator() {
        this(DEFAULT_MAX_SIZE);
    }

    public EndTruncatingAccumulator(int maxSize) {
        this.maxSize = maxSize;
    }

    /**
     * Appends data to the accumulator. If the total size exceeds maxSize,
     * the end is truncated to maintain the size limit.
     */
    public void append(String data) {
        if (data == null) return;
        totalCharsReceived += data.length();

        // If already at capacity and truncated, don't modify content
        if (isTruncated && content.length() >= maxSize) {
            return;
        }

        // Check if adding the string would exceed the limit
        if (content.length() + data.length() > maxSize) {
            int remainingSpace = maxSize - content.length();
            if (remainingSpace > 0) {
                content.append(data.substring(0, remainingSpace));
            }
            isTruncated = true;
        } else {
            content.append(data);
        }
    }

    /**
     * Appends bytes converted to string.
     */
    public void append(byte[] data) {
        append(new String(data));
    }

    /**
     * Returns the accumulated string, with truncation marker if truncated.
     */
    @Override
    public String toString() {
        if (!isTruncated) {
            return content.toString();
        }

        long truncatedChars = totalCharsReceived - maxSize;
        long truncatedKB = truncatedChars / 1024;
        return content.toString() + "\n... [output truncated - " + truncatedKB + "KB removed]";
    }

    /**
     * Clears all accumulated data.
     */
    public void clear() {
        content = new StringBuilder();
        isTruncated = false;
        totalCharsReceived = 0;
    }

    /**
     * Returns the current size of accumulated data.
     */
    public int length() {
        return content.length();
    }

    /**
     * Returns whether truncation has occurred.
     */
    public boolean isTruncated() {
        return isTruncated;
    }

    /**
     * Returns total chars received (before truncation).
     */
    public long getTotalCharsReceived() {
        return totalCharsReceived;
    }
}