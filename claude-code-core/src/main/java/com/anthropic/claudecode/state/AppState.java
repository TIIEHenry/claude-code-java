/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code state/AppState.tsx
 */
package com.anthropic.claudecode.state;

import java.util.*;
import java.util.concurrent.*;
import com.anthropic.claudecode.services.promptsuggestion.SpeculationService.SpeculationState;

/**
 * Application state definition.
 */
public class AppState {
    // Session state
    private String sessionId;
    private String conversationId;
    private String model;
    private PermissionMode permissionMode;

    // Query state
    private QueryState queryState = QueryState.IDLE;
    private int currentTurn = 0;
    private int maxTurns = 100;

    // Tool state
    private Set<String> inProgressToolIds = ConcurrentHashMap.newKeySet();
    private boolean hasInterruptibleToolInProgress = false;

    // Cost tracking
    private long inputTokens = 0;
    private long outputTokens = 0;
    private double totalCost = 0.0;

    // Metadata
    private final Map<String, Object> metadata = new ConcurrentHashMap<>();

    // Speculation state
    private SpeculationState speculation = null;
    private long speculationSessionTimeSavedMs = 0;

    public AppState() {
        this.sessionId = UUID.randomUUID().toString();
        this.model = "claude-sonnet-4-6";
        this.permissionMode = PermissionMode.DEFAULT;
    }

    // Getters
    public String getSessionId() { return sessionId; }
    public String getConversationId() { return conversationId; }
    public String getModel() { return model; }
    public PermissionMode getPermissionMode() { return permissionMode; }
    public QueryState getQueryState() { return queryState; }
    public int getCurrentTurn() { return currentTurn; }
    public int getMaxTurns() { return maxTurns; }
    public Set<String> getInProgressToolIds() { return new HashSet<>(inProgressToolIds); }
    public boolean isHasInterruptibleToolInProgress() { return hasInterruptibleToolInProgress; }
    public long getInputTokens() { return inputTokens; }
    public long getOutputTokens() { return outputTokens; }
    public double getTotalCost() { return totalCost; }
    public Object getMetadata(String key) { return metadata.get(key); }

    // Speculation getters
    public SpeculationState speculation() { return speculation; }
    public long speculationSessionTimeSavedMs() { return speculationSessionTimeSavedMs; }

    // Setters
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public void setModel(String model) { this.model = model; }
    public void setPermissionMode(PermissionMode permissionMode) { this.permissionMode = permissionMode; }
    public void setQueryState(QueryState queryState) { this.queryState = queryState; }
    public void setCurrentTurn(int currentTurn) { this.currentTurn = currentTurn; }
    public void setMaxTurns(int maxTurns) { this.maxTurns = maxTurns; }
    public void setHasInterruptibleToolInProgress(boolean hasInterruptibleToolInProgress) { this.hasInterruptibleToolInProgress = hasInterruptibleToolInProgress; }

    // Tool tracking
    public void addInProgressTool(String toolId) { inProgressToolIds.add(toolId); }
    public void removeInProgressTool(String toolId) { inProgressToolIds.remove(toolId); }
    public boolean hasInProgressTool(String toolId) { return inProgressToolIds.contains(toolId); }

    // Token tracking
    public void addTokens(long input, long output) {
        this.inputTokens += input;
        this.outputTokens += output;
    }

    public void addCost(double cost) {
        this.totalCost += cost;
    }

    // Metadata
    public void setMetadata(String key, Object value) { metadata.put(key, value); }
    public void removeMetadata(String key) { metadata.remove(key); }

    // With methods for functional updates
    public AppState withSpeculation(SpeculationState speculation) {
        AppState copy = this.copy();
        copy.speculation = speculation;
        return copy;
    }

    public AppState withSpeculationSessionTimeSavedMs(long timeSaved) {
        AppState copy = this.copy();
        copy.speculationSessionTimeSavedMs = timeSaved;
        return copy;
    }

    // Reset
    public void reset() {
        sessionId = UUID.randomUUID().toString();
        conversationId = null;
        queryState = QueryState.IDLE;
        currentTurn = 0;
        inProgressToolIds.clear();
        hasInterruptibleToolInProgress = false;
        inputTokens = 0;
        outputTokens = 0;
        totalCost = 0.0;
        metadata.clear();
    }

    /**
     * Create a copy.
     */
    public AppState copy() {
        AppState copy = new AppState();
        copy.sessionId = this.sessionId;
        copy.conversationId = this.conversationId;
        copy.model = this.model;
        copy.permissionMode = this.permissionMode;
        copy.queryState = this.queryState;
        copy.currentTurn = this.currentTurn;
        copy.maxTurns = this.maxTurns;
        copy.inProgressToolIds.addAll(this.inProgressToolIds);
        copy.hasInterruptibleToolInProgress = this.hasInterruptibleToolInProgress;
        copy.inputTokens = this.inputTokens;
        copy.outputTokens = this.outputTokens;
        copy.totalCost = this.totalCost;
        copy.metadata.putAll(this.metadata);
        copy.speculation = this.speculation;
        copy.speculationSessionTimeSavedMs = this.speculationSessionTimeSavedMs;
        return copy;
    }

    // Enums
    public enum QueryState {
        IDLE,
        RUNNING,
        STREAMING,
        WAITING_FOR_INPUT,
        INTERRUPTED,
        COMPLETED,
        FAILED
    }

    public enum PermissionMode {
        DEFAULT,
        ACCEPT_EDITS,
        BYPASS_PERMISSIONS,
        DONT_ASK,
        PLAN,
        AUTO
    }
}