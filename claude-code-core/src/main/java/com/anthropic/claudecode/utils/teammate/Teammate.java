/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code teammate utilities
 */
package com.anthropic.claudecode.utils.teammate;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Teammate utilities for agent swarm coordination.
 *
 * These helpers identify whether this Claude Code instance is running as a
 * spawned teammate in a swarm. Teammates receive their identity via CLI
 * arguments which are stored in dynamicTeamContext.
 */
public final class Teammate {
    private Teammate() {}

    /**
     * Teammate context.
     */
    public record TeammateContext(
            String agentId,
            String agentName,
            String teamName,
            String color,
            boolean planModeRequired,
            String parentSessionId
    ) {}

    // Dynamic team context for runtime team joining
    private static volatile TeammateContext dynamicTeamContext = null;

    // In-process teammate context storage (thread-local)
    private static final ThreadLocal<TeammateContext> inProcessContext = new ThreadLocal<>();

    /**
     * Set the dynamic team context.
     */
    public static void setDynamicTeamContext(TeammateContext context) {
        dynamicTeamContext = context;
    }

    /**
     * Clear the dynamic team context.
     */
    public static void clearDynamicTeamContext() {
        dynamicTeamContext = null;
    }

    /**
     * Get the current dynamic team context.
     */
    public static TeammateContext getDynamicTeamContext() {
        return dynamicTeamContext;
    }

    /**
     * Set in-process teammate context.
     */
    public static void setInProcessContext(TeammateContext context) {
        inProcessContext.set(context);
    }

    /**
     * Get in-process teammate context.
     */
    public static TeammateContext getInProcessContext() {
        return inProcessContext.get();
    }

    /**
     * Clear in-process teammate context.
     */
    public static void clearInProcessContext() {
        inProcessContext.remove();
    }

    /**
     * Run code with teammate context (for in-process teammates).
     */
    public static <T> T runWithTeammateContext(TeammateContext context, Supplier<T> supplier) {
        TeammateContext previous = inProcessContext.get();
        try {
            inProcessContext.set(context);
            return supplier.get();
        } finally {
            if (previous != null) {
                inProcessContext.set(previous);
            } else {
                inProcessContext.remove();
            }
        }
    }

    /**
     * Run code with teammate context (void version).
     */
    public static void runWithTeammateContext(TeammateContext context, Runnable runnable) {
        TeammateContext previous = inProcessContext.get();
        try {
            inProcessContext.set(context);
            runnable.run();
        } finally {
            if (previous != null) {
                inProcessContext.set(previous);
            } else {
                inProcessContext.remove();
            }
        }
    }

    /**
     * Get the parent session ID for this teammate.
     */
    public static String getParentSessionId() {
        TeammateContext inProcess = getInProcessContext();
        if (inProcess != null) return inProcess.parentSessionId();

        if (dynamicTeamContext != null) {
            return dynamicTeamContext.parentSessionId();
        }

        return null;
    }

    /**
     * Get the agent ID if running as a teammate.
     */
    public static String getAgentId() {
        TeammateContext inProcess = getInProcessContext();
        if (inProcess != null) return inProcess.agentId();

        if (dynamicTeamContext != null) {
            return dynamicTeamContext.agentId();
        }

        return null;
    }

    /**
     * Get the agent name if running as a teammate.
     */
    public static String getAgentName() {
        TeammateContext inProcess = getInProcessContext();
        if (inProcess != null) return inProcess.agentName();

        if (dynamicTeamContext != null) {
            return dynamicTeamContext.agentName();
        }

        return null;
    }

    /**
     * Get the team name if part of a team.
     */
    public static String getTeamName() {
        return getTeamName(null);
    }

    /**
     * Get the team name with optional team context.
     */
    public static String getTeamName(TeamContext teamContext) {
        TeammateContext inProcess = getInProcessContext();
        if (inProcess != null) return inProcess.teamName();

        if (dynamicTeamContext != null && dynamicTeamContext.teamName() != null) {
            return dynamicTeamContext.teamName();
        }

        if (teamContext != null) {
            return teamContext.teamName();
        }

        return null;
    }

    /**
     * Check if this session is running as a teammate.
     */
    public static boolean isTeammate() {
        // In-process teammates
        if (getInProcessContext() != null) return true;

        // Tmux teammates require both agent ID and team name
        return dynamicTeamContext != null &&
               dynamicTeamContext.agentId() != null &&
               dynamicTeamContext.teamName() != null;
    }

    /**
     * Check if this is an in-process teammate.
     */
    public static boolean isInProcessTeammate() {
        return getInProcessContext() != null;
    }

    /**
     * Get the teammate's assigned color.
     */
    public static String getTeammateColor() {
        TeammateContext inProcess = getInProcessContext();
        if (inProcess != null) return inProcess.color();

        if (dynamicTeamContext != null) {
            return dynamicTeamContext.color();
        }

        return null;
    }

    /**
     * Check if plan mode is required.
     */
    public static boolean isPlanModeRequired() {
        TeammateContext inProcess = getInProcessContext();
        if (inProcess != null) return inProcess.planModeRequired();

        if (dynamicTeamContext != null) {
            return dynamicTeamContext.planModeRequired();
        }

        // Check environment variable
        return Boolean.parseBoolean(System.getenv("CLAUDE_CODE_PLAN_MODE_REQUIRED"));
    }

    /**
     * Check if this session is a team lead.
     */
    public static boolean isTeamLead(TeamContext teamContext) {
        if (teamContext == null || teamContext.leadAgentId() == null) {
            return false;
        }

        String myAgentId = getAgentId();
        String leadAgentId = teamContext.leadAgentId();

        // If my agent ID matches the lead agent ID, I'm the lead
        if (Objects.equals(myAgentId, leadAgentId)) {
            return true;
        }

        // Backwards compat: if no agent ID is set and we have a team context
        if (myAgentId == null) {
            return true;
        }

        return false;
    }

    /**
     * Team context from AppState.
     */
    public record TeamContext(
            String teamName,
            String leadAgentId
    ) {}

    /**
     * Task info for teammate management.
     */
    public record TaskInfo(
            String id,
            String type,
            String status,
            boolean isIdle,
            List<Runnable> onIdleCallbacks
    ) {}

    /**
     * Check if there are active in-process teammates.
     */
    public static boolean hasActiveInProcessTeammates(Map<String, TaskInfo> tasks) {
        for (TaskInfo task : tasks.values()) {
            if ("in_process_teammate".equals(task.type()) &&
                "running".equals(task.status())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if there are working in-process teammates.
     */
    public static boolean hasWorkingInProcessTeammates(Map<String, TaskInfo> tasks) {
        for (TaskInfo task : tasks.values()) {
            if ("in_process_teammate".equals(task.type()) &&
                "running".equals(task.status()) &&
                !task.isIdle()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Wait for teammates to become idle.
     */
    public static CompletableFuture<Void> waitForTeammatesToBecomeIdle(
            Map<String, TaskInfo> tasks,
            Consumer<String> registerIdleCallback) {

        List<String> workingTaskIds = new ArrayList<>();

        for (Map.Entry<String, TaskInfo> entry : tasks.entrySet()) {
            TaskInfo task = entry.getValue();
            if ("in_process_teammate".equals(task.type()) &&
                "running".equals(task.status()) &&
                !task.isIdle()) {
                workingTaskIds.add(entry.getKey());
            }
        }

        if (workingTaskIds.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<Void> future = new CompletableFuture<>();
        AtomicInteger remaining = new AtomicInteger(workingTaskIds.size());

        Runnable onIdle = () -> {
            if (remaining.decrementAndGet() == 0) {
                future.complete(null);
            }
        };

        // Register callback on each working teammate
        for (String taskId : workingTaskIds) {
            registerIdleCallback.accept(taskId);
        }

        return future;
    }
}