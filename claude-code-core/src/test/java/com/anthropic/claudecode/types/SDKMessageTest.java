/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.types;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import com.anthropic.claudecode.message.ContentBlock;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SDKMessage.
 */
class SDKMessageTest {

    @Test
    @DisplayName("SDKMessage User with text")
    void userWithText() {
        SDKMessage.User user = new SDKMessage.User("Hello");

        assertEquals(1, user.content().size());
        assertTrue(user.content().get(0) instanceof ContentBlock.Text);
    }

    @Test
    @DisplayName("SDKMessage User with content list")
    void userWithContentList() {
        List<ContentBlock> content = List.of(new ContentBlock.Text("Hello"));
        SDKMessage.User user = new SDKMessage.User(content);

        assertEquals(content, user.content());
    }

    @Test
    @DisplayName("SDKMessage Assistant with text")
    void assistantWithText() {
        SDKMessage.Assistant assistant = new SDKMessage.Assistant("Response");

        assertEquals(1, assistant.content().size());
    }

    @Test
    @DisplayName("SDKMessage Assistant with content list")
    void assistantWithContentList() {
        List<ContentBlock> content = List.of(new ContentBlock.Text("Response"));
        SDKMessage.Assistant assistant = new SDKMessage.Assistant(content);

        assertEquals(content, assistant.content());
    }

    @Test
    @DisplayName("SDKMessage Result default not error")
    void resultDefaultNotError() {
        SDKMessage.Result result = new SDKMessage.Result("output");

        assertEquals("output", result.result());
        assertFalse(result.isError());
    }

    @Test
    @DisplayName("SDKMessage Result error factory")
    void resultErrorFactory() {
        SDKMessage.Result result = SDKMessage.Result.error("error message");

        assertEquals("error message", result.result());
        assertTrue(result.isError());
    }

    @Test
    @DisplayName("SDKMessage Progress with status only")
    void progressWithStatusOnly() {
        SDKMessage.Progress progress = new SDKMessage.Progress("Processing");

        assertEquals("Processing", progress.status());
        assertEquals(0.0, progress.percentage(), 0.001);
    }

    @Test
    @DisplayName("SDKMessage Progress with percentage")
    void progressWithPercentage() {
        SDKMessage.Progress progress = new SDKMessage.Progress("Processing", 0.75);

        assertEquals("Processing", progress.status());
        assertEquals(0.75, progress.percentage(), 0.001);
    }

    @Test
    @DisplayName("SDKMessage is sealed interface")
    void isSealedInterface() {
        SDKMessage.User user = new SDKMessage.User("text");
        SDKMessage.Assistant assistant = new SDKMessage.Assistant("text");
        SDKMessage.Result result = new SDKMessage.Result("result");
        SDKMessage.Progress progress = new SDKMessage.Progress("status");

        assertTrue(user instanceof SDKMessage);
        assertTrue(assistant instanceof SDKMessage);
        assertTrue(result instanceof SDKMessage);
        assertTrue(progress instanceof SDKMessage);
    }
}