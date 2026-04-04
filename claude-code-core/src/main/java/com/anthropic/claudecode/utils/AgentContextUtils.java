/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code agent context utilities
 */
package com.anthropic.claudecode.utils;

import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Agent context for tracking agent identity across async operations.
 *
 * Supports two agent types:
 * 1. Subagents (Agent tool): Run in-process for quick, delegated tasks.
 * 2. In-process teammates: Part of a swarm with team coordination.
 *
 * Uses ThreadLocal for context isolation in concurrent scenarios.
 */
public final class AgentContextUtils {
    private AgentContextUtils() {}

    // ThreadLocal storage for agent context (equivalent to AsyncLocalStorage)
    private static final ThreadLocal<AgentContext> contextStorage = new ThreadLocal<>();

    /**
     * Agent type enum.
     */
    public enum AgentType {
        SUBAGENT, TEAMMATE
    }

    /**
     * Sealed interface for agent context.
     */
    public sealed interface AgentContext permits SubagentContext, TeammateAgentContext {
        String getAgentId();
        AgentType getAgentType();
        String getParentSessionId();
        String getInvokingRequestId();
        InvocationKind getInvocationKind();
        boolean isInvocationEmitted();
    }

    /**
     * Invocation kind enum.
     */
    public enum InvocationKind {
        SPAWN, RESUME
    }

    /**
     * Context for subagents (Agent tool agents).
     */
    public record SubagentContext(
            String agentId,
            String parentSessionId,
            AgentType agentType,
            String subagentName,
            boolean isBuiltIn,
            String invokingRequestId,
            InvocationKind invocationKind,
            boolean invocationEmitted
    ) implements AgentContext {
        public SubagentContext(
                String agentId,
                String parentSessionId,
                String subagentName,
                boolean isBuiltIn) {
            this(agentId, parentSessionId, AgentType.SUBAGENT, subagentName, isBuiltIn, null, null, false);
        }

        // Explicit implementation to match interface method name
        @Override
        public boolean isInvocationEmitted() {
            return invocationEmitted;
        }

        // Alias for invocationKind() to match interface
        @Override
        public InvocationKind getInvocationKind() {
            return invocationKind;
        }

        // Alias for invokingRequestId() to match interface
        @Override
        public String getInvokingRequestId() {
            return invokingRequestId;
        }

        // Alias for parentSessionId() to match interface
        @Override
        public String getParentSessionId() {
            return parentSessionId;
        }

        // Alias for agentType() to match interface
        @Override
        public AgentType getAgentType() {
            return agentType;
        }

        // Alias for agentId() to match interface
        @Override
        public String getAgentId() {
            return agentId;
        }

        public SubagentContext withInvocationEmitted(boolean emitted) {
            return new SubagentContext(agentId, parentSessionId, agentType, subagentName, isBuiltIn,
                invokingRequestId, invocationKind, emitted);
        }
    }

    /**
     * Context for in-process teammates.
     */
    public record TeammateAgentContext(
            String agentId,
            String agentName,
            String teamName,
            String agentColor,
            boolean planModeRequired,
            String parentSessionId,
            boolean isTeamLead,
            AgentType agentType,
            String invokingRequestId,
            InvocationKind invocationKind,
            boolean invocationEmitted
    ) implements AgentContext {
        public TeammateAgentContext(
                String agentId,
                String agentName,
                String teamName,
                String parentSessionId,
                boolean isTeamLead) {
            this(agentId, agentName, teamName, null, false, parentSessionId, isTeamLead,
                 AgentType.TEAMMATE, null, null, false);
        }

        // Explicit implementation to match interface method name
        @Override
        public boolean isInvocationEmitted() {
            return invocationEmitted;
        }

        // Alias for invocationKind() to match interface
        @Override
        public InvocationKind getInvocationKind() {
            return invocationKind;
        }

        // Alias for invokingRequestId() to match interface
        @Override
        public String getInvokingRequestId() {
            return invokingRequestId;
        }

        // Alias for parentSessionId() to match interface
        @Override
        public String getParentSessionId() {
            return parentSessionId;
        }

        // Alias for agentType() to match interface
        @Override
        public AgentType getAgentType() {
            return agentType;
        }

        // Alias for agentId() to match interface
        @Override
        public String getAgentId() {
            return agentId;
        }

        public TeammateAgentContext withInvocationEmitted(boolean emitted) {
            return new TeammateAgentContext(agentId, agentName, teamName, agentColor, planModeRequired,
                parentSessionId, isTeamLead, agentType, invokingRequestId, invocationKind, emitted);
        }
    }

    /**
     * Mutable agent context holder for tracking emission state.
     */
    public static final class MutableAgentContext {
        private final AgentContext context;
        private volatile boolean invocationEmitted;

        public MutableAgentContext(AgentContext context) {
            this.context = context;
            this.invocationEmitted = context.isInvocationEmitted();
        }

        public AgentContext getContext() {
            return context;
        }

        public boolean isInvocationEmitted() {
            return invocationEmitted;
        }

        public void setInvocationEmitted(boolean emitted) {
            this.invocationEmitted = emitted;
        }

        public String getAgentId() {
            return context.getAgentId();
        }

        public AgentType getAgentType() {
            return context.getAgentType();
        }

        public String getInvokingRequestId() {
            return context.getInvokingRequestId();
        }

        public InvocationKind getInvocationKind() {
            return context.getInvocationKind();
        }
    }

    // Mutable context holder for emission tracking
    private static final ThreadLocal<MutableAgentContext> mutableContextStorage = new ThreadLocal<>();

    /**
     * Get the current agent context, if any.
     */
    public static AgentContext getAgentContext() {
        MutableAgentContext mutable = mutableContextStorage.get();
        return mutable != null ? mutable.getContext() : contextStorage.get();
    }

    /**
     * Get the mutable agent context for emission tracking.
     */
    public static MutableAgentContext getMutableAgentContext() {
        return mutableContextStorage.get();
    }

    /**
     * Run a function with the given agent context.
     */
    public static <T> T runWithAgentContext(AgentContext context, Callable<T> fn) {
        MutableAgentContext mutable = new MutableAgentContext(context);
        contextStorage.set(context);
        mutableContextStorage.set(mutable);
        try {
            return fn.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            contextStorage.remove();
            mutableContextStorage.remove();
        }
    }

    /**
     * Run a runnable with the given agent context.
     */
    public static void runWithAgentContext(AgentContext context, Runnable fn) {
        MutableAgentContext mutable = new MutableAgentContext(context);
        contextStorage.set(context);
        mutableContextStorage.set(mutable);
        try {
            fn.run();
        } finally {
            contextStorage.remove();
            mutableContextStorage.remove();
        }
    }

    /**
     * Run an async function with agent context preserved across async boundaries.
     */
    public static <T> CompletableFuture<T> runWithAgentContextAsync(AgentContext context, Supplier<CompletableFuture<T>> fn) {
        MutableAgentContext mutable = new MutableAgentContext(context);
        contextStorage.set(context);
        mutableContextStorage.set(mutable);

        return fn.get().whenComplete((result, error) -> {
            contextStorage.remove();
            mutableContextStorage.remove();
        });
    }

    /**
     * Type guard to check if context is a SubagentContext.
     */
    public static boolean isSubagentContext(AgentContext context) {
        return context != null && context.getAgentType() == AgentType.SUBAGENT;
    }

    /**
     * Type guard to check if context is a TeammateAgentContext.
     */
    public static boolean isTeammateAgentContext(AgentContext context) {
        return context != null && context.getAgentType() == AgentType.TEAMMATE;
    }

    /**
     * Get the subagent name suitable for analytics logging.
     * Returns the agent type name for built-in agents, "user-defined" for custom agents.
     */
    public static String getSubagentLogName() {
        AgentContext context = getAgentContext();
        if (!isSubagentContext(context)) {
            return null;
        }
        SubagentContext subagent = (SubagentContext) context;
        if (subagent.subagentName() == null) {
            return null;
        }
        return subagent.isBuiltIn() ? subagent.subagentName() : "user-defined";
    }

    /**
     * Consume the invoking request_id for the current agent context - once per invocation.
     * Returns the id on the first call after a spawn/resume, then null until next boundary.
     */
    public static InvocationInfo consumeInvokingRequestId() {
        MutableAgentContext mutable = getMutableAgentContext();
        if (mutable == null || mutable.getInvokingRequestId() == null || mutable.isInvocationEmitted()) {
            return null;
        }
        mutable.setInvocationEmitted(true);
        return new InvocationInfo(mutable.getInvokingRequestId(), mutable.getInvocationKind());
    }

    /**
     * Invocation info record.
     */
    public record InvocationInfo(
            String invokingRequestId,
            InvocationKind invocationKind
    ) {}

    /**
     * Clear the agent context.
     */
    public static void clearContext() {
        contextStorage.remove();
        mutableContextStorage.remove();
    }

    /**
     * Check if currently running in an agent context.
     */
    public static boolean hasAgentContext() {
        return contextStorage.get() != null;
    }

    /**
     * Get agent ID from environment variables (for cross-process teammates).
     */
    public static String getAgentIdFromEnv() {
        return System.getenv("CLAUDE_CODE_AGENT_ID");
    }

    /**
     * Get parent session ID from environment variables.
     */
    public static String getParentSessionIdFromEnv() {
        return System.getenv("CLAUDE_CODE_PARENT_SESSION_ID");
    }

    /**
     * Check if agent swarms are enabled.
     */
    public static boolean isAgentSwarmsEnabled() {
        String enabled = System.getenv("CLAUDE_CODE_AGENT_SWARMS_ENABLED");
        return "true".equalsIgnoreCase(enabled) || "1".equals(enabled);
    }
}