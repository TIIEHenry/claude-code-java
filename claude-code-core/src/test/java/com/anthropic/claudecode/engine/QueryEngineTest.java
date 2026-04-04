/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.engine;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;

import com.anthropic.claudecode.permission.PermissionMode;

/**
 * Tests for QueryEngine.
 */
@DisplayName("QueryEngine Tests")
class QueryEngineTest {

    @Test
    @DisplayName("QueryEngine creates with valid config")
    void createsWithValidConfig() {
        QueryEngineConfig config = QueryEngineConfig.builder()
            .cwd("/tmp")
            .build();

        QueryEngine engine = new QueryEngine(config);

        assertNotNull(engine);
        assertEquals("/tmp", engine.getCwd());
    }

    @Test
    @DisplayName("QueryEngine create factory method works")
    void createFactoryMethodWorks() {
        QueryEngine engine = QueryEngine.create("/project");

        assertNotNull(engine);
        assertEquals("/project", engine.getCwd());
    }

    @Test
    @DisplayName("QueryEngine starts with no messages")
    void startsWithNoMessages() {
        QueryEngine engine = QueryEngine.create("/tmp");

        assertTrue(engine.getMessages().isEmpty());
    }

    @Test
    @DisplayName("QueryEngine starts with initial messages from config")
    void startsWithInitialMessagesFromConfig() {
        List<Object> initialMessages = new ArrayList<>();
        initialMessages.add("message1");
        initialMessages.add("message2");

        QueryEngineConfig config = QueryEngineConfig.builder()
            .cwd("/tmp")
            .initialMessages(initialMessages)
            .build();

        QueryEngine engine = new QueryEngine(config);

        assertEquals(2, engine.getMessages().size());
    }

    @Test
    @DisplayName("QueryEngine addMessage adds to messages")
    void addMessageAddsToMessages() {
        QueryEngine engine = QueryEngine.create("/tmp");

        engine.addMessage("new message");

        assertEquals(1, engine.getMessages().size());
        assertEquals("new message", engine.getMessages().get(0));
    }

    @Test
    @DisplayName("QueryEngine clearMessages clears all messages")
    void clearMessagesClearsAllMessages() {
        QueryEngine engine = QueryEngine.create("/tmp");
        engine.addMessage("msg1");
        engine.addMessage("msg2");

        engine.clearMessages();

        assertTrue(engine.getMessages().isEmpty());
    }

    @Test
    @DisplayName("QueryEngine isInterrupted starts false")
    void isInterruptedStartsFalse() {
        QueryEngine engine = QueryEngine.create("/tmp");

        assertFalse(engine.isInterrupted());
    }

    @Test
    @DisplayName("QueryEngine interrupt sets interrupted")
    void interruptSetsInterrupted() {
        QueryEngine engine = QueryEngine.create("/tmp");

        engine.interrupt();

        assertTrue(engine.isInterrupted());
    }

    @Test
    @DisplayName("QueryEngine submitMessage returns QueryResult")
    void submitMessageReturnsQueryResult() {
        QueryEngine engine = QueryEngine.create("/tmp");
        QueryEngine.SubmitOptions options = QueryEngine.SubmitOptions.empty();

        CompletableFuture<QueryEngine.QueryResult> future = engine.submitMessage("test prompt", options);
        QueryEngine.QueryResult result = future.join();

        assertNotNull(result);
        assertNotNull(result.queryId());
        assertEquals("test prompt", result.prompt());
        assertEquals(QueryEngine.QueryStatus.COMPLETED, result.status());
    }

    @Test
    @DisplayName("QueryEngine getConfig returns config")
    void getConfigReturnsConfig() {
        QueryEngineConfig config = QueryEngineConfig.builder()
            .cwd("/custom")
            .verbose(true)
            .build();

        QueryEngine engine = new QueryEngine(config);

        assertEquals(config, engine.getConfig());
    }

    @Test
    @DisplayName("QueryEngine getMessages returns copy")
    void getMessagesReturnsCopy() {
        QueryEngine engine = QueryEngine.create("/tmp");
        engine.addMessage("msg1");

        List<Object> messages1 = engine.getMessages();
        List<Object> messages2 = engine.getMessages();

        // Modifications to one list shouldn't affect the other
        assertNotSame(messages1, messages2);
    }

    @Test
    @DisplayName("QueryEngine AbortController starts not aborted")
    void abortControllerStartsNotAborted() {
        QueryEngine.AbortController controller = new QueryEngine.AbortController();

        assertFalse(controller.isAborted());
    }

    @Test
    @DisplayName("QueryEngine AbortController abort sets aborted")
    void abortControllerAbortSetsAborted() {
        QueryEngine.AbortController controller = new QueryEngine.AbortController();

        controller.abort();

        assertTrue(controller.isAborted());
    }

    @Test
    @DisplayName("QueryEngine AbortController listeners are called on abort")
    void abortControllerListenersAreCalledOnAbort() {
        QueryEngine.AbortController controller = new QueryEngine.AbortController();
        List<String> called = new ArrayList<>();

        controller.addListener(() -> called.add("listener1"));
        controller.addListener(() -> called.add("listener2"));

        controller.abort();

        assertEquals(2, called.size());
        assertTrue(called.contains("listener1"));
        assertTrue(called.contains("listener2"));
    }

    @Test
    @DisplayName("QueryEngine QueryResult record works correctly")
    void queryResultRecordWorksCorrectly() {
        QueryEngine.QueryResult result = new QueryEngine.QueryResult(
            "query-123",
            "test prompt",
            QueryEngine.QueryStatus.COMPLETED,
            "result data"
        );

        assertEquals("query-123", result.queryId());
        assertEquals("test prompt", result.prompt());
        assertEquals(QueryEngine.QueryStatus.COMPLETED, result.status());
        assertEquals("result data", result.result());
    }

    @Test
    @DisplayName("QueryEngine QueryStatus enum has correct values")
    void queryStatusEnumHasCorrectValues() {
        QueryEngine.QueryStatus[] statuses = QueryEngine.QueryStatus.values();

        assertEquals(5, statuses.length);
        assertTrue(Arrays.asList(statuses).contains(QueryEngine.QueryStatus.PENDING));
        assertTrue(Arrays.asList(statuses).contains(QueryEngine.QueryStatus.RUNNING));
        assertTrue(Arrays.asList(statuses).contains(QueryEngine.QueryStatus.COMPLETED));
        assertTrue(Arrays.asList(statuses).contains(QueryEngine.QueryStatus.FAILED));
        assertTrue(Arrays.asList(statuses).contains(QueryEngine.QueryStatus.INTERRUPTED));
    }

    @Test
    @DisplayName("QueryEngine SubmitOptions empty creates default options")
    void submitOptionsEmptyCreatesDefaultOptions() {
        QueryEngine.SubmitOptions options = QueryEngine.SubmitOptions.empty();

        assertEquals(PermissionMode.DEFAULT, options.permissionMode());
        assertFalse(options.verbose());
        assertNull(options.maxTurns());
    }

    @Test
    @DisplayName("QueryEngine SubmitOptions default constructor works")
    void submitOptionsDefaultConstructorWorks() {
        QueryEngine.SubmitOptions options = new QueryEngine.SubmitOptions();

        assertEquals(PermissionMode.DEFAULT, options.permissionMode());
        assertFalse(options.verbose());
        assertNull(options.maxTurns());
    }

    @Test
    @DisplayName("QueryEngine SubmitOptions with all parameters")
    void submitOptionsWithAllParameters() {
        QueryEngine.SubmitOptions options = new QueryEngine.SubmitOptions(
            PermissionMode.ACCEPT_EDITS,
            true,
            10
        );

        assertEquals(PermissionMode.ACCEPT_EDITS, options.permissionMode());
        assertTrue(options.verbose());
        assertEquals(10, options.maxTurns());
    }
}