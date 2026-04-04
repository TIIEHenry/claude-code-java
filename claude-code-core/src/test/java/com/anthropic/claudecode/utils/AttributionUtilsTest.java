/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AttributionUtils.
 */
class AttributionUtilsTest {

    @Test
    @DisplayName("AttributionUtils create creates attribution")
    void create() {
        AttributionUtils.Attribution attr = AttributionUtils.create("source", "author");
        assertNotNull(attr);
        assertNotNull(attr.id());
        assertEquals("source", attr.source());
        assertEquals("author", attr.author());
    }

    @Test
    @DisplayName("AttributionUtils forAI creates AI attribution")
    void forAI() {
        AttributionUtils.Attribution attr = AttributionUtils.forAI("claude-3", "session-123");
        assertNotNull(attr);
        assertEquals("ai:claude-3", attr.source());
        assertEquals("session-123", attr.author());
        assertTrue(attr.isAI());
    }

    @Test
    @DisplayName("AttributionUtils forUser creates user attribution")
    void forUser() {
        AttributionUtils.Attribution attr = AttributionUtils.forUser("user-123");
        assertNotNull(attr);
        assertEquals("user", attr.source());
        assertEquals("user-123", attr.author());
        assertTrue(attr.isUser());
    }

    @Test
    @DisplayName("AttributionUtils parse valid string")
    void parseValid() {
        String str = "id123|source|author|12345";
        AttributionUtils.Attribution attr = AttributionUtils.parse(str);
        assertNotNull(attr);
        assertEquals("id123", attr.id());
        assertEquals("source", attr.source());
        assertEquals("author", attr.author());
        assertEquals(12345, attr.timestamp());
    }

    @Test
    @DisplayName("AttributionUtils parse null returns null")
    void parseNull() {
        assertNull(AttributionUtils.parse(null));
    }

    @Test
    @DisplayName("AttributionUtils parse empty returns null")
    void parseEmpty() {
        assertNull(AttributionUtils.parse(""));
    }

    @Test
    @DisplayName("AttributionUtils parse invalid returns null")
    void parseInvalid() {
        assertNull(AttributionUtils.parse("invalid"));
    }

    @Test
    @DisplayName("AttributionUtils isAI true for AI source")
    void isAITrue() {
        assertTrue(AttributionUtils.isAI("ai:claude-3"));
    }

    @Test
    @DisplayName("AttributionUtils isAI false for non-AI source")
    void isAIFalse() {
        assertFalse(AttributionUtils.isAI("user"));
        assertFalse(AttributionUtils.isAI(null));
    }

    @Test
    @DisplayName("AttributionUtils isUser true for user source")
    void isUserTrue() {
        assertTrue(AttributionUtils.isUser("user"));
    }

    @Test
    @DisplayName("AttributionUtils isUser false for non-user source")
    void isUserFalse() {
        assertFalse(AttributionUtils.isUser("ai:claude"));
        assertFalse(AttributionUtils.isUser("other"));
    }

    @Test
    @DisplayName("AttributionUtils getModelId extracts model ID")
    void getModelId() {
        assertEquals("claude-3", AttributionUtils.getModelId("ai:claude-3"));
    }

    @Test
    @DisplayName("AttributionUtils getModelId null for non-AI")
    void getModelIdNonAI() {
        assertNull(AttributionUtils.getModelId("user"));
    }

    @Test
    @DisplayName("AttributionUtils merge single returns it")
    void mergeSingle() {
        AttributionUtils.Attribution attr = AttributionUtils.create("source", "author");
        AttributionUtils.Attribution merged = AttributionUtils.merge(List.of(attr));
        assertEquals(attr.id(), merged.id());
    }

    @Test
    @DisplayName("AttributionUtils merge null returns null")
    void mergeNull() {
        assertNull(AttributionUtils.merge(null));
    }

    @Test
    @DisplayName("AttributionUtils merge empty returns null")
    void mergeEmpty() {
        assertNull(AttributionUtils.merge(List.of()));
    }

    @Test
    @DisplayName("AttributionUtils merge multiple returns most recent")
    void mergeMultiple() throws InterruptedException {
        AttributionUtils.Attribution attr1 = AttributionUtils.create("source1", "author1");
        Thread.sleep(10);
        AttributionUtils.Attribution attr2 = AttributionUtils.create("source2", "author2");
        
        AttributionUtils.Attribution merged = AttributionUtils.merge(List.of(attr1, attr2));
        assertEquals(attr2.id(), merged.id());
    }

    @Test
    @DisplayName("AttributionUtils Attribution serialize")
    void attributionSerialize() {
        AttributionUtils.Attribution attr = new AttributionUtils.Attribution(
            "id123", "source", "author", 12345L
        );
        String serialized = attr.serialize();
        assertEquals("id123|source|author|12345", serialized);
    }

    @Test
    @DisplayName("AttributionUtils Attribution isAI")
    void attributionIsAI() {
        AttributionUtils.Attribution attr = new AttributionUtils.Attribution(
            "id", "ai:claude", "session", 0L
        );
        assertTrue(attr.isAI());
    }

    @Test
    @DisplayName("AttributionUtils Attribution isUser")
    void attributionIsUser() {
        AttributionUtils.Attribution attr = new AttributionUtils.Attribution(
            "id", "user", "userId", 0L
        );
        assertTrue(attr.isUser());
    }

    @Test
    @DisplayName("AttributionUtils Attribution getModelId")
    void attributionGetModelId() {
        AttributionUtils.Attribution attr = new AttributionUtils.Attribution(
            "id", "ai:claude-3", "session", 0L
        );
        assertEquals("claude-3", attr.getModelId());
    }

    @Test
    @DisplayName("AttributionUtils AttributionBlock lineCount")
    void attributionBlockLineCount() {
        AttributionUtils.Attribution attr = AttributionUtils.create("source", "author");
        AttributionUtils.AttributionBlock block = new AttributionUtils.AttributionBlock(
            "file.java", 1, 10, attr
        );
        assertEquals(10, block.lineCount());
    }
}
