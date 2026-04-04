/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.context;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SessionContext.
 */
class SessionContextTest {

    private SessionContext context;

    @BeforeEach
    void setUp() {
        context = new SessionContext();
    }

    @Test
    @DisplayName("SessionContext default constructor creates session ID")
    void defaultConstructorCreatesSessionId() {
        assertNotNull(context.getSessionId());
        assertFalse(context.getSessionId().isEmpty());
    }

    @Test
    @DisplayName("SessionContext constructor with custom session ID")
    void customSessionId() {
        SessionContext custom = new SessionContext("custom-id-123");
        assertEquals("custom-id-123", custom.getSessionId());
    }

    @Test
    @DisplayName("SessionContext createdAt is set")
    void createdAtIsSet() {
        long before = System.currentTimeMillis();
        SessionContext ctx = new SessionContext();
        long after = System.currentTimeMillis();

        assertTrue(ctx.getCreatedAt() >= before);
        assertTrue(ctx.getCreatedAt() <= after);
    }

    @Test
    @DisplayName("SessionContext is active by default")
    void isActiveDefault() {
        assertTrue(context.isActive());
    }

    @Test
    @DisplayName("SessionContext end deactivates session")
    void endDeactivatesSession() {
        context.end();
        assertFalse(context.isActive());
    }

    @Test
    @DisplayName("SessionContext getMetadata returns empty for missing key")
    void getMetadataMissing() {
        Optional<Object> value = context.getMetadata("missing");
        assertFalse(value.isPresent());
    }

    @Test
    @DisplayName("SessionContext setMetadata and getMetadata")
    void setAndGetMetadata() {
        context.setMetadata("key1", "value1");
        Optional<Object> value = context.getMetadata("key1");

        assertTrue(value.isPresent());
        assertEquals("value1", value.get());
    }

    @Test
    @DisplayName("SessionContext metadata with various types")
    void metadataVariousTypes() {
        context.setMetadata("string", "text");
        context.setMetadata("integer", 42);
        context.setMetadata("boolean", true);

        assertEquals("text", context.getMetadata("string").get());
        assertEquals(42, context.getMetadata("integer").get());
        assertEquals(true, context.getMetadata("boolean").get());
    }

    @Test
    @DisplayName("SessionContext getDurationMs returns positive value")
    void getDurationMs() throws InterruptedException {
        Thread.sleep(10);
        assertTrue(context.getDurationMs() >= 10);
    }

    @Test
    @DisplayName("SessionContext toString contains session ID")
    void toStringContainsSessionId() {
        String str = context.toString();
        assertTrue(str.contains(context.getSessionId()));
    }

    @Test
    @DisplayName("SessionContext toString contains active status")
    void toStringContainsActiveStatus() {
        String str = context.toString();
        assertTrue(str.contains("active=true"));

        context.end();
        String strAfterEnd = context.toString();
        assertTrue(strAfterEnd.contains("active=false"));
    }

    @Test
    @DisplayName("SessionContext unique session IDs")
    void uniqueSessionIds() {
        SessionContext ctx1 = new SessionContext();
        SessionContext ctx2 = new SessionContext();

        assertNotEquals(ctx1.getSessionId(), ctx2.getSessionId());
    }

    @Test
    @DisplayName("SessionContext metadata can be updated")
    void metadataCanBeUpdated() {
        context.setMetadata("key", "value1");
        context.setMetadata("key", "value2");

        assertEquals("value2", context.getMetadata("key").get());
    }

    @Test
    @DisplayName("SessionContext metadata null value throws NullPointerException")
    void metadataNullValueThrows() {
        context.setMetadata("key", "value");
        assertThrows(NullPointerException.class, () -> {
            context.setMetadata("key", null);
        });
    }
}