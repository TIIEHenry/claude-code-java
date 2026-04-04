/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/forkedAgent.ts
 */
package com.anthropic.claudecode.utils;

import com.anthropic.claudecode.services.analytics.AnalyticsMetadata;
import java.util.*;
import java.util.concurrent.*;
import com.anthropic.claudecode.*;
import com.anthropic.claudecode.types.MessageTypes;

/**
 * Helper for running forked agent query loops with usage tracking.
 */
public final class ForkedAgent {
    private ForkedAgent() {}

    /**
     * Cache safe parameters.
     */
    public record CacheSafeParams(
        Map<String, String> userContext,
        Map<String, String> systemContext,
        Object toolUseContext,
        List<MessageTypes.Message> forkContextMessages
    ) {}

    /**
     * Forked agent parameters.
     */
    public record ForkedAgentParams(
        List<MessageTypes.Message> promptMessages,
        CacheSafeParams cacheSafeParams,
        Object canUseTool,
        String querySource,
        String forkLabel,
        Object overrides,
        Integer maxOutputTokens,
        Integer maxTurns,
        Object onMessage,
        boolean skipTranscript,
        boolean skipCacheWrite,
        boolean requireCanUseTool,
        Object abortController
    ) {
        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private List<MessageTypes.Message> promptMessages;
            private CacheSafeParams cacheSafeParams;
            private Object canUseTool;
            private String querySource;
            private String forkLabel;
            private Object overrides;
            private Integer maxOutputTokens;
            private Integer maxTurns;
            private Object onMessage;
            private boolean skipTranscript;
            private boolean skipCacheWrite;
            private boolean requireCanUseTool;
            private Object abortController;

            public Builder promptMessages(List<MessageTypes.Message> v) { promptMessages = v; return this; }
            public Builder cacheSafeParams(CacheSafeParams v) { cacheSafeParams = v; return this; }
            public Builder canUseTool(Object v) { canUseTool = v; return this; }
            public Builder querySource(String v) { querySource = v; return this; }
            public Builder forkLabel(String v) { forkLabel = v; return this; }
            public Builder overrides(Object v) { overrides = v; return this; }
            public Builder maxOutputTokens(Integer v) { maxOutputTokens = v; return this; }
            public Builder maxTurns(Integer v) { maxTurns = v; return this; }
            public Builder onMessage(Object v) { onMessage = v; return this; }
            public Builder skipTranscript(boolean v) { skipTranscript = v; return this; }
            public Builder skipCacheWrite(boolean v) { skipCacheWrite = v; return this; }
            public Builder requireCanUseTool(boolean v) { requireCanUseTool = v; return this; }
            public Builder abortController(Object v) { abortController = v; return this; }

            public ForkedAgentParams build() {
                return new ForkedAgentParams(
                    promptMessages, cacheSafeParams, canUseTool, querySource, forkLabel,
                    overrides, maxOutputTokens, maxTurns, onMessage,
                    skipTranscript, skipCacheWrite, requireCanUseTool, abortController
                );
            }
        }
    }

    /**
     * Run a forked agent query.
     */
    public static CompletableFuture<Object> runForkedAgentQuery(ForkedAgentParams params) {
        return CompletableFuture.supplyAsync(() -> {
            AnalyticsMetadata.logEvent("tengu_fork_agent_query", Map.of(
                "query_source", params.querySource() != null ? params.querySource() : "unknown",
                "fork_label", params.forkLabel() != null ? params.forkLabel() : "unknown"
            ));
            return null;
        });
    }
}