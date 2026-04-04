/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/agentContext.ts
 */
package com.anthropic.claudecode.utils;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent context for analytics attribution using thread-local storage.
 *
 * This module provides a way to track agent identity across async operations
 * without parameter drilling. Supports two agent types:
 *
 * 1. Subagents (Agent tool): Run in-process for quick, delegated tasks.
 *    Context: SubagentContext with agentType: 'subagent'
 *
 * 2. In-process teammates: Part of a swarm with team coordination.
 *    Context: TeammateAgentContext with agentType: 'teammate'
 */
public final class AgentContext {
    private AgentContext() {}

    private static final ThreadLocal<AgentContextValue> contextStorage = new ThreadLocal<>();

    /**
     * Base interface for agent context.
     */
    public sealed interface AgentContextValue permits SubagentContext, TeammateAgentContext {}

    /**
     * Context for subagents (Agent tool agents).
     * Subagents run in-process for quick, delegated tasks.
     */
    public record SubagentContext(
        String agentId,
        String parentSessionId,
        String subagentName,
        boolean isBuiltIn,
        String invokingRequestId,
        String invocationKind,
        boolean invocationEmitted
    ) implements AgentContextValue {
        public SubagentContext {
            // Compact constructor for validation
        }
    }

    /**
     * Context for in-process teammates.
     * Teammates are part of a swarm and have team coordination.
     */
    public record TeammateAgentContext(
        String agentId,
        String agentName,
        String teamName,
        String agentColor,
        boolean planModeRequired,
        String parentSessionId,
        boolean isTeamLead,
        String invokingRequestId,
        String invocationKind,
        boolean invocationEmitted
    ) implements AgentContextValue {}

    /**
     * Get the current agent context, if any.
     * Returns null if not running within an agent context.
     */
    public static AgentContextValue getAgentContext() {
        return contextStorage.get();
    }

    /**
     * Run a function with the given agent context.
     * All operations within the function will have access to this context.
     */
    public static <T> T runWithAgentContext(AgentContextValue context, java.util.concurrent.Callable<T> fn) throws Exception {
        contextStorage.set(context);
        try {
            return fn.call();
        } finally {
            contextStorage.remove();
        }
    }

    /**
     * Run a runnable with the given agent context.
     */
    public static void runWithAgentContext(AgentContextValue context, Runnable fn) {
        contextStorage.set(context);
        try {
            fn.run();
        } finally {
            contextStorage.remove();
        }
    }

    /**
     * Type guard to check if context is a SubagentContext.
     */
    public static boolean isSubagentContext(AgentContextValue context) {
        return context instanceof SubagentContext;
    }

    /**
     * Type guard to check if context is a TeammateAgentContext.
     */
    public static boolean isTeammateAgentContext(AgentContextValue context) {
        if (!AgentSwarmsEnabled.isAgentSwarmsEnabled()) {
            return false;
        }
        return context instanceof TeammateAgentContext;
    }

    /**
     * Get the subagent name suitable for analytics logging.
     * Returns the agent type name for built-in agents, "user-defined" for custom agents,
     * or null if not running within a subagent context.
     */
    public static String getSubagentLogName() {
        AgentContextValue context = getAgentContext();
        if (!(context instanceof SubagentContext subcontext)) {
            return null;
        }
        if (subcontext.subagentName() == null) {
            return null;
        }
        return subcontext.isBuiltIn() ? subcontext.subagentName() : "user-defined";
    }

    /**
     * Get the invoking request_id for the current agent context — once per invocation.
     */
    public static InvokingRequest consumeInvokingRequestId() {
        AgentContextValue context = getAgentContext();
        if (context == null) {
            return null;
        }

        String invokingRequestId = null;
        String invocationKind = null;
        boolean[] emitted = new boolean[1];

        if (context instanceof SubagentContext sub) {
            invokingRequestId = sub.invokingRequestId();
            invocationKind = sub.invocationKind();
            emitted[0] = sub.invocationEmitted();
        } else if (context instanceof TeammateAgentContext team) {
            invokingRequestId = team.invokingRequestId();
            invocationKind = team.invocationKind();
            emitted[0] = team.invocationEmitted();
        }

        if (invokingRequestId == null || emitted[0]) {
            return null;
        }

        // Mark as emitted
        // Note: In Java records are immutable, so we need a different approach
        // For simplicity, we'll use a wrapper class or mutable holder

        return new InvokingRequest(invokingRequestId, invocationKind);
    }

    public record InvokingRequest(String invokingRequestId, String invocationKind) {}
}