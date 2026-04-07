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
    @DisplayName("QueryEngine executeAgenticLoop returns Flux")
    void executeAgenticLoopReturnsFlux() {
        QueryEngine engine = QueryEngine.create("/tmp");

        reactor.core.publisher.Flux<QueryEvent> flux = engine.executeAgenticLoop("test prompt");

        assertNotNull(flux);
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
    @DisplayName("QueryState initial creates default state")
    void queryStateInitialCreatesDefaultState() {
        QueryState state = QueryState.initial(new ArrayList<>(), null);

        assertNotNull(state);
        assertTrue(state.messages().isEmpty());
        assertEquals(1, state.turnCount());
        assertNull(state.transition());
    }

    @Test
    @DisplayName("QueryState builder works correctly")
    void queryStateBuilderWorksCorrectly() {
        List<Object> messages = new ArrayList<>();
        messages.add("test");

        QueryState state = QueryState.builder()
            .messages(messages)
            .turnCount(5)
            .build();

        assertEquals(1, state.messages().size());
        assertEquals(5, state.turnCount());
    }

    @Test
    @DisplayName("LoopTransition Continue is not terminal")
    void loopTransitionContinueIsNotTerminal() {
        LoopTransition.Continue transition = LoopTransition.Continue.toolResults(new ArrayList<>());

        assertFalse(transition.isTerminal());
        assertEquals("tool_results", transition.reason());
    }

    @Test
    @DisplayName("LoopTransition Terminal is terminal")
    void loopTransitionTerminalIsTerminal() {
        LoopTransition.Terminal terminal = LoopTransition.Terminal.complete("result");

        assertTrue(terminal.isTerminal());
        assertFalse(terminal.isError());
        assertEquals("complete", terminal.reason());
    }

    @Test
    @DisplayName("LoopTransition Terminal error is error")
    void loopTransitionTerminalErrorIsError() {
        LoopTransition.Terminal terminal = LoopTransition.Terminal.error("something went wrong");

        assertTrue(terminal.isTerminal());
        assertTrue(terminal.isError());
        assertEquals("error", terminal.reason());
    }

    @Test
    @DisplayName("LoopTransition Terminal maxTurnsReached works")
    void loopTransitionTerminalMaxTurnsReachedWorks() {
        LoopTransition.Terminal terminal = LoopTransition.Terminal.maxTurnsReached(10);

        assertTrue(terminal.isTerminal());
        assertFalse(terminal.isError());
        assertTrue(terminal.result().toString().contains("10"));
    }

    @Test
    @DisplayName("QueryEvent types work correctly")
    void queryEventTypesWorkCorrectly() {
        QueryEvent.RequestStart start = QueryEvent.RequestStart.instance;
        QueryEvent.Message msg = new QueryEvent.Message("test");
        QueryEvent.ToolsExecuting executing = new QueryEvent.ToolsExecuting(3);
        QueryEvent.ToolsComplete complete = new QueryEvent.ToolsComplete(5);
        LoopTransition.Terminal terminal = LoopTransition.Terminal.complete("done");
        QueryEvent.Terminal terminalEvent = new QueryEvent.Terminal(terminal);

        assertEquals("request_start", start.getTypeName());
        assertEquals("message", msg.getTypeName());
        assertEquals("tools_executing", executing.getTypeName());
        assertEquals("tools_complete", complete.getTypeName());
        assertEquals("terminal", terminalEvent.getTypeName());

        assertEquals(3, executing.toolCount());
        assertEquals(5, complete.resultCount());
    }
}