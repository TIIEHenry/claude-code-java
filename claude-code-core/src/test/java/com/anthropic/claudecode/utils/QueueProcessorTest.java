/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for QueueProcessor.
 */
class QueueProcessorTest {

    @BeforeEach
    void clearQueue() {
        QueueProcessor.clearQueue();
    }

    @Test
    @DisplayName("QueueProcessor QueuedCommand record")
    void queuedCommandRecord() {
        QueueProcessor.QueuedCommand cmd = new QueueProcessor.QueuedCommand(
            "/test", "bash", "agent123"
        );

        assertEquals("/test", cmd.value());
        assertEquals("bash", cmd.mode());
        assertEquals("agent123", cmd.agentId());
    }

    @Test
    @DisplayName("QueueProcessor QueuedCommand isSlashCommand true")
    void isSlashCommandTrue() {
        QueueProcessor.QueuedCommand cmd = new QueueProcessor.QueuedCommand(
            "/help", null, null
        );

        assertTrue(cmd.isSlashCommand());
    }

    @Test
    @DisplayName("QueueProcessor QueuedCommand isSlashCommand false")
    void isSlashCommandFalse() {
        QueueProcessor.QueuedCommand cmd = new QueueProcessor.QueuedCommand(
            "hello", null, null
        );

        assertFalse(cmd.isSlashCommand());
    }

    @Test
    @DisplayName("QueueProcessor QueuedCommand isSlashCommand null value")
    void isSlashCommandNull() {
        QueueProcessor.QueuedCommand cmd = new QueueProcessor.QueuedCommand(
            null, null, null
        );

        assertFalse(cmd.isSlashCommand());
    }

    @Test
    @DisplayName("QueueProcessor enqueue adds command")
    void enqueue() {
        QueueProcessor.QueuedCommand cmd = new QueueProcessor.QueuedCommand(
            "test", null, null
        );

        QueueProcessor.enqueue(cmd);

        assertEquals(1, QueueProcessor.queueSize());
    }

    @Test
    @DisplayName("QueueProcessor peek returns command")
    void peek() {
        QueueProcessor.QueuedCommand cmd = new QueueProcessor.QueuedCommand(
            "test", null, null
        );

        QueueProcessor.enqueue(cmd);

        QueueProcessor.QueuedCommand result = QueueProcessor.peek(c -> true);

        assertEquals(cmd, result);
        assertEquals(1, QueueProcessor.queueSize()); // Not removed
    }

    @Test
    @DisplayName("QueueProcessor peek returns null if no match")
    void peekNoMatch() {
        QueueProcessor.QueuedCommand cmd = new QueueProcessor.QueuedCommand(
            "test", null, null
        );

        QueueProcessor.enqueue(cmd);

        QueueProcessor.QueuedCommand result = QueueProcessor.peek(c -> false);

        assertNull(result);
    }

    @Test
    @DisplayName("QueueProcessor dequeue removes command")
    void dequeue() {
        QueueProcessor.QueuedCommand cmd = new QueueProcessor.QueuedCommand(
            "test", null, null
        );

        QueueProcessor.enqueue(cmd);

        QueueProcessor.QueuedCommand result = QueueProcessor.dequeue(c -> true);

        assertEquals(cmd, result);
        assertEquals(0, QueueProcessor.queueSize());
    }

    @Test
    @DisplayName("QueueProcessor dequeue returns null if no match")
    void dequeueNoMatch() {
        QueueProcessor.QueuedCommand cmd = new QueueProcessor.QueuedCommand(
            "test", null, null
        );

        QueueProcessor.enqueue(cmd);

        QueueProcessor.QueuedCommand result = QueueProcessor.dequeue(c -> false);

        assertNull(result);
        assertEquals(1, QueueProcessor.queueSize());
    }

    @Test
    @DisplayName("QueueProcessor dequeue multiple commands")
    void dequeueMultiple() {
        QueueProcessor.enqueue(new QueueProcessor.QueuedCommand("a", null, null));
        QueueProcessor.enqueue(new QueueProcessor.QueuedCommand("b", null, null));

        QueueProcessor.dequeue(c -> c.value().equals("a"));
        assertEquals(1, QueueProcessor.queueSize());

        QueueProcessor.dequeue(c -> c.value().equals("b"));
        assertEquals(0, QueueProcessor.queueSize());
    }

    @Test
    @DisplayName("QueueProcessor hasCommandsInQueue true")
    void hasCommandsInQueueTrue() {
        QueueProcessor.enqueue(new QueueProcessor.QueuedCommand("test", null, null));

        assertTrue(QueueProcessor.hasCommandsInQueue());
    }

    @Test
    @DisplayName("QueueProcessor hasCommandsInQueue false")
    void hasCommandsInQueueFalse() {
        assertFalse(QueueProcessor.hasCommandsInQueue());
    }

    @Test
    @DisplayName("QueueProcessor clearQueue empties queue")
    void testClearQueue() {
        QueueProcessor.enqueue(new QueueProcessor.QueuedCommand("test", null, null));
        QueueProcessor.enqueue(new QueueProcessor.QueuedCommand("test2", null, null));

        QueueProcessor.clearQueue();

        assertEquals(0, QueueProcessor.queueSize());
    }

    @Test
    @DisplayName("QueueProcessor queueSize returns correct count")
    void queueSize() {
        assertEquals(0, QueueProcessor.queueSize());

        QueueProcessor.enqueue(new QueueProcessor.QueuedCommand("test", null, null));
        assertEquals(1, QueueProcessor.queueSize());

        QueueProcessor.enqueue(new QueueProcessor.QueuedCommand("test2", null, null));
        assertEquals(2, QueueProcessor.queueSize());
    }

    @Test
    @DisplayName("QueueProcessor processQueueIfReady no commands")
    void processQueueIfReadyNoCommands() throws Exception {
        CompletableFuture<QueueProcessor.ProcessQueueResult> result =
            QueueProcessor.processQueueIfReady(cmds -> CompletableFuture.completedFuture(null));

        QueueProcessor.ProcessQueueResult r = result.get();
        assertFalse(r.processed());
    }

    @Test
    @DisplayName("QueueProcessor processQueueIfReady slash command")
    void processQueueIfReadySlashCommand() throws Exception {
        QueueProcessor.enqueue(new QueueProcessor.QueuedCommand("/help", null, null));

        CompletableFuture<QueueProcessor.ProcessQueueResult> result =
            QueueProcessor.processQueueIfReady(cmds -> {
                assertEquals(1, cmds.size());
                return CompletableFuture.completedFuture(null);
            });

        QueueProcessor.ProcessQueueResult r = result.get();
        assertTrue(r.processed());
    }

    @Test
    @DisplayName("QueueProcessor processQueueIfReady bash mode")
    void processQueueIfReadyBashMode() throws Exception {
        QueueProcessor.enqueue(new QueueProcessor.QueuedCommand("ls", "bash", null));

        CompletableFuture<QueueProcessor.ProcessQueueResult> result =
            QueueProcessor.processQueueIfReady(cmds -> {
                assertEquals(1, cmds.size());
                return CompletableFuture.completedFuture(null);
            });

        QueueProcessor.ProcessQueueResult r = result.get();
        assertTrue(r.processed());
    }

    @Test
    @DisplayName("QueueProcessor processQueueIfReady skips agent commands")
    void processQueueIfReadySkipsAgentCommands() throws Exception {
        QueueProcessor.enqueue(new QueueProcessor.QueuedCommand("test", null, "agent123"));

        CompletableFuture<QueueProcessor.ProcessQueueResult> result =
            QueueProcessor.processQueueIfReady(cmds -> CompletableFuture.completedFuture(null));

        QueueProcessor.ProcessQueueResult r = result.get();
        assertFalse(r.processed());
    }

    @Test
    @DisplayName("QueueProcessor ProcessQueueResult record")
    void processQueueResultRecord() {
        QueueProcessor.ProcessQueueResult result = new QueueProcessor.ProcessQueueResult(true);

        assertTrue(result.processed());
    }
}