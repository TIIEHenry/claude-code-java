/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code bootstrap/state.ts
 */
package com.anthropic.claudecode.bootstrap;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Global application state.
 * Singleton pattern for session-scoped state management.
 */
public final class AppState {
    private AppState() {}

    // Singleton instance
    private static AppState instance;

    // State fields
    private String originalCwd = "";
    private String projectRoot = "";
    private double totalCostUSD = 0;
    private long totalAPIDuration = 0;
    private long totalToolDuration = 0;
    private long startTime = System.currentTimeMillis();
    private long lastInteractionTime = System.currentTimeMillis();
    private int totalLinesAdded = 0;
    private int totalLinesRemoved = 0;
    private boolean hasUnknownModelCost = false;
    private String cwd = "";
    private Map<String, ModelUsage> modelUsage = new ConcurrentHashMap<>();
    private String mainLoopModelOverride;
    private String initialMainLoopModel;
    private boolean isInteractive = false;
    private String clientType = "cli";
    private String sessionId = UUID.randomUUID().toString();
    private String parentSessionId;

    // Counters for turn tracking
    private int turnToolCount = 0;
    private int turnHookCount = 0;

    // Lists for tracking
    private List<Map<String, Object>> inMemoryErrorLog = new ArrayList<>();
    private List<String> inlinePlugins = new ArrayList<>();

    // Flags
    private boolean sessionBypassPermissionsMode = false;
    private boolean scheduledTasksEnabled = false;
    private boolean sessionTrustAccepted = false;
    private boolean sessionPersistenceDisabled = false;
    private boolean hasExitedPlanMode = false;

    /**
     * Get singleton instance.
     */
    public static synchronized AppState getInstance() {
        if (instance == null) {
            instance = new AppState();
        }
        return instance;
    }

    /**
     * Reset state (for tests).
     */
    public static synchronized void resetState() {
        instance = null;
    }

    // Getters and setters

    public String getOriginalCwd() { return originalCwd; }
    public void setOriginalCwd(String cwd) { this.originalCwd = cwd; }

    public String getProjectRoot() { return projectRoot; }
    public void setProjectRoot(String root) { this.projectRoot = root; }

    public double getTotalCostUSD() { return totalCostUSD; }
    public void addCost(double cost) { this.totalCostUSD += cost; }

    public long getTotalAPIDuration() { return totalAPIDuration; }
    public void addAPIDuration(long duration) { this.totalAPIDuration += duration; }

    public long getTotalToolDuration() { return totalToolDuration; }
    public void addToolDuration(long duration) { this.totalToolDuration += duration; }

    public long getStartTime() { return startTime; }
    public long getLastInteractionTime() { return lastInteractionTime; }
    public void updateInteractionTime() { this.lastInteractionTime = System.currentTimeMillis(); }

    public int getTotalLinesAdded() { return totalLinesAdded; }
    public void addLines(int lines) { this.totalLinesAdded += lines; }

    public int getTotalLinesRemoved() { return totalLinesRemoved; }
    public void removeLines(int lines) { this.totalLinesRemoved += lines; }

    public String getCwd() { return cwd; }
    public void setCwd(String cwd) { this.cwd = cwd; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String id) { this.sessionId = id; }

    public String getParentSessionId() { return parentSessionId; }
    public void setParentSessionId(String id) { this.parentSessionId = id; }

    public boolean isInteractive() { return isInteractive; }
    public void setInteractive(boolean interactive) { this.isInteractive = interactive; }

    public String getClientType() { return clientType; }
    public void setClientType(String type) { this.clientType = type; }

    public String getMainLoopModel() {
        return mainLoopModelOverride != null ? mainLoopModelOverride : initialMainLoopModel;
    }
    public void setMainLoopModelOverride(String model) { this.mainLoopModelOverride = model; }
    public void setInitialMainLoopModel(String model) { this.initialMainLoopModel = model; }

    public Map<String, ModelUsage> getModelUsage() { return modelUsage; }
    public void addModelUsage(String model, int inputTokens, int outputTokens, double cost) {
        modelUsage.compute(model, (k, v) -> {
            if (v == null) {
                return new ModelUsage(inputTokens, outputTokens, cost);
            }
            return new ModelUsage(
                v.inputTokens + inputTokens,
                v.outputTokens + outputTokens,
                v.cost + cost
            );
        });
    }

    public int getTurnToolCount() { return turnToolCount; }
    public void incrementToolCount() { this.turnToolCount++; }
    public void resetTurnCounts() {
        this.turnToolCount = 0;
        this.turnHookCount = 0;
    }

    public boolean isSessionBypassPermissionsMode() { return sessionBypassPermissionsMode; }
    public void setSessionBypassPermissionsMode(boolean bypass) { this.sessionBypassPermissionsMode = bypass; }

    public boolean isSessionTrustAccepted() { return sessionTrustAccepted; }
    public void setSessionTrustAccepted(boolean accepted) { this.sessionTrustAccepted = accepted; }

    public boolean hasExitedPlanMode() { return hasExitedPlanMode; }
    public void setHasExitedPlanMode(boolean exited) { this.hasExitedPlanMode = exited; }

    public List<Map<String, Object>> getInMemoryErrorLog() { return inMemoryErrorLog; }
    public void addError(String error) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("error", error);
        entry.put("timestamp", new Date().toString());
        inMemoryErrorLog.add(entry);
        if (inMemoryErrorLog.size() > 100) {
            inMemoryErrorLog.remove(0);
        }
    }

    /**
     * Model usage record.
     */
    public record ModelUsage(int inputTokens, int outputTokens, double cost) {}

    /**
     * Get session duration in milliseconds.
     */
    public long getSessionDuration() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Get session start date string.
     */
    public String getSessionStartDate() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date(startTime));
    }
}