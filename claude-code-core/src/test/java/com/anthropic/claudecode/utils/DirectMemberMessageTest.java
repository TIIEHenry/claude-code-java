/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DirectMemberMessage.
 */
class DirectMemberMessageTest {

    @Test
    @DisplayName("DirectMemberMessage ParsedDirectMessage record")
    void parsedDirectMessageRecord() {
        DirectMemberMessage.ParsedDirectMessage parsed = new DirectMemberMessage.ParsedDirectMessage(
            "agent-name", "Hello there"
        );
        assertEquals("agent-name", parsed.recipientName());
        assertEquals("Hello there", parsed.message());
    }

    @Test
    @DisplayName("DirectMemberMessage DirectMessageResult.Success")
    void directMessageResultSuccess() {
        DirectMemberMessage.DirectMessageResult.Success success =
            new DirectMemberMessage.DirectMessageResult.Success("agent-name");
        assertEquals("agent-name", success.recipientName());
        assertTrue(success.success());
    }

    @Test
    @DisplayName("DirectMemberMessage DirectMessageResult.Failure")
    void directMessageResultFailure() {
        DirectMemberMessage.DirectMessageResult.Failure failure =
            new DirectMemberMessage.DirectMessageResult.Failure("error", "agent-name");
        assertEquals("error", failure.error());
        assertEquals("agent-name", failure.recipientName());
        assertFalse(failure.success());
    }

    @Test
    @DisplayName("DirectMemberMessage TeamContext record")
    void teamContextRecord() {
        Map<String, DirectMemberMessage.Teammate> teammates = new HashMap<>();
        teammates.put("id1", new DirectMemberMessage.Teammate("agent1", "id1"));

        DirectMemberMessage.TeamContext context = new DirectMemberMessage.TeamContext(
            "team-name", teammates
        );
        assertEquals("team-name", context.teamName());
        assertEquals(1, context.teammates().size());
    }

    @Test
    @DisplayName("DirectMemberMessage Teammate record")
    void teammateRecord() {
        DirectMemberMessage.Teammate teammate = new DirectMemberMessage.Teammate("agent-name", "id-123");
        assertEquals("agent-name", teammate.name());
        assertEquals("id-123", teammate.id());
    }

    @Test
    @DisplayName("DirectMemberMessage parseDirectMemberMessage valid")
    void parseDirectMemberMessageValid() {
        DirectMemberMessage.ParsedDirectMessage parsed =
            DirectMemberMessage.parseDirectMemberMessage("@agent-name Hello world");
        assertNotNull(parsed);
        assertEquals("agent-name", parsed.recipientName());
        assertEquals("Hello world", parsed.message());
    }

    @Test
    @DisplayName("DirectMemberMessage parseDirectMemberMessage null")
    void parseDirectMemberMessageNull() {
        assertNull(DirectMemberMessage.parseDirectMemberMessage(null));
    }

    @Test
    @DisplayName("DirectMemberMessage parseDirectMemberMessage no @ prefix")
    void parseDirectMemberMessageNoPrefix() {
        assertNull(DirectMemberMessage.parseDirectMemberMessage("agent-name Hello"));
    }

    @Test
    @DisplayName("DirectMemberMessage parseDirectMemberMessage empty message")
    void parseDirectMemberMessageEmptyMessage() {
        assertNull(DirectMemberMessage.parseDirectMemberMessage("@agent-name   "));
    }

    @Test
    @DisplayName("DirectMemberMessage parseDirectMemberMessage no message")
    void parseDirectMemberMessageNoMessage() {
        assertNull(DirectMemberMessage.parseDirectMemberMessage("@agent-name"));
    }

    @Test
    @DisplayName("DirectMemberMessage parseDirectMemberMessage with hyphen")
    void parseDirectMemberMessageWithHyphen() {
        DirectMemberMessage.ParsedDirectMessage parsed =
            DirectMemberMessage.parseDirectMemberMessage("@my-agent Hello");
        assertNotNull(parsed);
        assertEquals("my-agent", parsed.recipientName());
    }

    @Test
    @DisplayName("DirectMemberMessage sendDirectMemberMessage null context")
    void sendDirectMemberMessageNullContext() throws Exception {
        CompletableFuture<DirectMemberMessage.DirectMessageResult> future =
            DirectMemberMessage.sendDirectMemberMessage("agent", "message", null);

        DirectMemberMessage.DirectMessageResult result = future.get();
        assertFalse(result.success());
        assertTrue(result instanceof DirectMemberMessage.DirectMessageResult.Failure);
    }

    @Test
    @DisplayName("DirectMemberMessage sendDirectMemberMessage unknown recipient")
    void sendDirectMemberMessageUnknownRecipient() throws Exception {
        Map<String, DirectMemberMessage.Teammate> teammates = new HashMap<>();
        DirectMemberMessage.TeamContext context = new DirectMemberMessage.TeamContext("team", teammates);

        CompletableFuture<DirectMemberMessage.DirectMessageResult> future =
            DirectMemberMessage.sendDirectMemberMessage("unknown", "message", context);

        DirectMemberMessage.DirectMessageResult result = future.get();
        assertFalse(result.success());
    }
}