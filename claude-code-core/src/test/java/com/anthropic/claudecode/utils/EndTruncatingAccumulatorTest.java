/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EndTruncatingAccumulator.
 */
class EndTruncatingAccumulatorTest {

    @Test
    @DisplayName("EndTruncatingAccumulator default constructor")
    void defaultConstructor() {
        EndTruncatingAccumulator acc = new EndTruncatingAccumulator();
        assertNotNull(acc);
        assertEquals(0, acc.length());
    }

    @Test
    @DisplayName("EndTruncatingAccumulator custom maxSize")
    void customMaxSize() {
        EndTruncatingAccumulator acc = new EndTruncatingAccumulator(10);
        acc.append("hello world this is long");
        assertTrue(acc.length() <= 10);
    }

    @Test
    @DisplayName("EndTruncatingAccumulator append adds content")
    void appendAddsContent() {
        EndTruncatingAccumulator acc = new EndTruncatingAccumulator(100);
        acc.append("hello");
        assertEquals(5, acc.length());
        assertEquals("hello", acc.toString());
    }

    @Test
    @DisplayName("EndTruncatingAccumulator append null does nothing")
    void appendNull() {
        EndTruncatingAccumulator acc = new EndTruncatingAccumulator(100);
        acc.append((String) null);
        assertEquals(0, acc.length());
    }

    @Test
    @DisplayName("EndTruncatingAccumulator append bytes")
    void appendBytes() {
        EndTruncatingAccumulator acc = new EndTruncatingAccumulator(100);
        acc.append("hello".getBytes());
        assertEquals(5, acc.length());
    }

    @Test
    @DisplayName("EndTruncatingAccumulator truncates when over limit")
    void truncatesWhenOverLimit() {
        EndTruncatingAccumulator acc = new EndTruncatingAccumulator(10);
        acc.append("hello world");
        assertTrue(acc.isTruncated());
        assertTrue(acc.length() <= 10);
    }

    @Test
    @DisplayName("EndTruncatingAccumulator toString shows truncation marker")
    void toStringShowsTruncation() {
        EndTruncatingAccumulator acc = new EndTruncatingAccumulator(5);
        acc.append("hello world");
        String result = acc.toString();
        assertTrue(result.contains("[output truncated"));
    }

    @Test
    @DisplayName("EndTruncatingAccumulator clear resets state")
    void clearResets() {
        EndTruncatingAccumulator acc = new EndTruncatingAccumulator(100);
        acc.append("hello");
        acc.clear();
        assertEquals(0, acc.length());
        assertFalse(acc.isTruncated());
        assertEquals(0, acc.getTotalCharsReceived());
    }

    @Test
    @DisplayName("EndTruncatingAccumulator length returns current size")
    void lengthReturnsSize() {
        EndTruncatingAccumulator acc = new EndTruncatingAccumulator(100);
        acc.append("hello");
        acc.append(" world");
        assertEquals(11, acc.length());
    }

    @Test
    @DisplayName("EndTruncatingAccumulator isTruncated initially false")
    void isTruncatedInitiallyFalse() {
        EndTruncatingAccumulator acc = new EndTruncatingAccumulator(100);
        assertFalse(acc.isTruncated());
    }

    @Test
    @DisplayName("EndTruncatingAccumulator isTruncated true after truncate")
    void isTruncatedTrueAfterTruncate() {
        EndTruncatingAccumulator acc = new EndTruncatingAccumulator(5);
        acc.append("hello world");
        assertTrue(acc.isTruncated());
    }

    @Test
    @DisplayName("EndTruncatingAccumulator getTotalCharsReceived tracks total")
    void getTotalCharsReceived() {
        EndTruncatingAccumulator acc = new EndTruncatingAccumulator(10);
        acc.append("hello");
        acc.append(" world");
        assertEquals(11, acc.getTotalCharsReceived());
    }

    @Test
    @DisplayName("EndTruncatingAccumulator getTotalCharsReceived tracks truncated")
    void getTotalCharsReceivedTruncated() {
        EndTruncatingAccumulator acc = new EndTruncatingAccumulator(5);
        acc.append("hello world");
        assertEquals(11, acc.getTotalCharsReceived());
        assertTrue(acc.length() <= 5);
    }

    @Test
    @DisplayName("EndTruncatingAccumulator multiple appends")
    void multipleAppends() {
        EndTruncatingAccumulator acc = new EndTruncatingAccumulator(100);
        acc.append("hello");
        acc.append(" ");
        acc.append("world");
        assertEquals("hello world", acc.toString());
    }

    @Test
    @DisplayName("EndTruncatingAccumulator stops accepting after truncated")
    void stopsAcceptingAfterTruncated() {
        EndTruncatingAccumulator acc = new EndTruncatingAccumulator(5);
        acc.append("hello"); // fills to capacity
        acc.append(" more"); // should be ignored or truncated
        assertTrue(acc.length() <= 5);
    }

    @Test
    @DisplayName("EndTruncatingAccumulator DEFAULT_MAX_SIZE constant")
    void defaultMaxSizeConstant() {
        assertTrue(EndTruncatingAccumulator.DEFAULT_MAX_SIZE > 0);
    }
}