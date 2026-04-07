/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code query events
 */
package com.anthropic.claudecode.engine;

import java.util.List;

/**
 * Events emitted during agentic loop execution.
 *
 * <p>Corresponds to StreamEvent types in query.ts.
 */
public sealed interface QueryEvent permits
    QueryEvent.RequestStart,
    QueryEvent.Message,
    QueryEvent.ToolsExecuting,
    QueryEvent.ToolsComplete,
    QueryEvent.Terminal {

    /**
     * Request started event.
     */
    record RequestStart() implements QueryEvent {
        public static RequestStart instance = new RequestStart();
    }

    /**
     * Message event (assistant, user, tombstone, etc).
     */
    record Message(Object message) implements QueryEvent {}

    /**
     * Tools are being executed.
     */
    record ToolsExecuting(int toolCount) implements QueryEvent {}

    /**
     * Tools execution complete.
     */
    record ToolsComplete(int resultCount) implements QueryEvent {}

    /**
     * Terminal event (loop ended).
     */
    record Terminal(LoopTransition.Terminal transition) implements QueryEvent {
        public boolean isError() {
            return transition.isError();
        }

        public Object getResult() {
            return transition.result();
        }

        public String getReason() {
            return transition.reason();
        }
    }

    /**
     * Stream request start helper.
     */
    public static RequestStart streamRequestStart() {
        return RequestStart.instance;
    }

    /**
     * Create message event.
     */
    public static Message message(Object msg) {
        return new Message(msg);
    }

    /**
     * Create terminal event.
     */
    public static Terminal terminal(LoopTransition.Terminal transition) {
        return new Terminal(transition);
    }

    /**
     * Get event type name for logging.
     */
    default String getTypeName() {
        if (this instanceof RequestStart) {
            return "request_start";
        } else if (this instanceof Message) {
            return "message";
        } else if (this instanceof ToolsExecuting) {
            return "tools_executing";
        } else if (this instanceof ToolsComplete) {
            return "tools_complete";
        } else if (this instanceof Terminal) {
            return "terminal";
        }
        return "unknown";
    }
}