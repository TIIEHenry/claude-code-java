/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code QueryEngine.ts config
 */
package com.anthropic.claudecode.engine;

import com.anthropic.claudecode.*;
import com.anthropic.claudecode.permission.PermissionResult;
import com.anthropic.claudecode.permission.PermissionMode;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * QueryEngine configuration.
 */
public record QueryEngineConfig(
    String cwd,
    List<Object> tools,
    List<Object> commands,
    List<Object> mcpClients,
    List<Object> agents,
    CanUseToolFn canUseTool,
    List<Object> initialMessages,
    Object readFileCache,
    String customSystemPrompt,
    String appendSystemPrompt,
    String userSpecifiedModel,
    String fallbackModel,
    Object thinkingConfig,
    Integer maxTurns,
    Double maxBudgetUsd,
    Object taskBudget,
    Object jsonSchema,
    boolean verbose,
    boolean debug,
    boolean isNonInteractiveSession,
    PermissionMode permissionMode,
    PermissionMode prePlanMode,
    Object fileHistoryState,
    Object attributionState
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String cwd = System.getProperty("user.dir");
        private List<Object> tools = List.of();
        private List<Object> commands = List.of();
        private List<Object> mcpClients = List.of();
        private List<Object> agents = List.of();
        private CanUseToolFn canUseTool = (tool, input, context, assistantMessage, toolUseId) ->
            CompletableFuture.completedFuture(PermissionResult.allow(input));
        private List<Object> initialMessages;
        private Object readFileCache;
        private String customSystemPrompt;
        private String appendSystemPrompt;
        private String userSpecifiedModel;
        private String fallbackModel;
        private Object thinkingConfig;
        private Integer maxTurns;
        private Double maxBudgetUsd;
        private Object taskBudget;
        private Object jsonSchema;
        private boolean verbose = false;
        private boolean debug = false;
        private boolean isNonInteractiveSession = false;
        private PermissionMode permissionMode = PermissionMode.DEFAULT;
        private PermissionMode prePlanMode;
        private Object fileHistoryState;
        private Object attributionState;

        public Builder cwd(String cwd) { this.cwd = cwd; return this; }
        public Builder tools(List<Object> tools) { this.tools = tools; return this; }
        public Builder commands(List<Object> commands) { this.commands = commands; return this; }
        public Builder mcpClients(List<Object> mcpClients) { this.mcpClients = mcpClients; return this; }
        public Builder agents(List<Object> agents) { this.agents = agents; return this; }
        public Builder canUseTool(CanUseToolFn canUseTool) { this.canUseTool = canUseTool; return this; }
        public Builder initialMessages(List<Object> initialMessages) { this.initialMessages = initialMessages; return this; }
        public Builder readFileCache(Object readFileCache) { this.readFileCache = readFileCache; return this; }
        public Builder customSystemPrompt(String customSystemPrompt) { this.customSystemPrompt = customSystemPrompt; return this; }
        public Builder appendSystemPrompt(String appendSystemPrompt) { this.appendSystemPrompt = appendSystemPrompt; return this; }
        public Builder userSpecifiedModel(String userSpecifiedModel) { this.userSpecifiedModel = userSpecifiedModel; return this; }
        public Builder fallbackModel(String fallbackModel) { this.fallbackModel = fallbackModel; return this; }
        public Builder thinkingConfig(Object thinkingConfig) { this.thinkingConfig = thinkingConfig; return this; }
        public Builder maxTurns(Integer maxTurns) { this.maxTurns = maxTurns; return this; }
        public Builder maxBudgetUsd(Double maxBudgetUsd) { this.maxBudgetUsd = maxBudgetUsd; return this; }
        public Builder taskBudget(Object taskBudget) { this.taskBudget = taskBudget; return this; }
        public Builder jsonSchema(Object jsonSchema) { this.jsonSchema = jsonSchema; return this; }
        public Builder verbose(boolean verbose) { this.verbose = verbose; return this; }
        public Builder debug(boolean debug) { this.debug = debug; return this; }
        public Builder isNonInteractiveSession(boolean isNonInteractiveSession) { this.isNonInteractiveSession = isNonInteractiveSession; return this; }
        public Builder permissionMode(PermissionMode permissionMode) { this.permissionMode = permissionMode; return this; }
        public Builder prePlanMode(PermissionMode prePlanMode) { this.prePlanMode = prePlanMode; return this; }
        public Builder fileHistoryState(Object fileHistoryState) { this.fileHistoryState = fileHistoryState; return this; }
        public Builder attributionState(Object attributionState) { this.attributionState = attributionState; return this; }

        public QueryEngineConfig build() {
            return new QueryEngineConfig(
                cwd, tools, commands, mcpClients, agents, canUseTool,
                initialMessages, readFileCache, customSystemPrompt,
                appendSystemPrompt, userSpecifiedModel, fallbackModel,
                thinkingConfig, maxTurns, maxBudgetUsd, taskBudget,
                jsonSchema, verbose, debug, isNonInteractiveSession,
                permissionMode, prePlanMode, fileHistoryState, attributionState
            );
        }
    }
}