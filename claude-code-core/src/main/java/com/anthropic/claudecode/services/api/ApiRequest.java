/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/api
 */
package com.anthropic.claudecode.services.api;

import java.util.*;

/**
 * API Request.
 */
public record ApiRequest(
    String model,
    List<Map<String, Object>> messages,
    int maxTokens,
    Double temperature,
    List<Map<String, Object>> tools,
    String system,
    boolean stream
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String model = "glm-5";
        private List<Map<String, Object>> messages = new ArrayList<>();
        private int maxTokens = 4096;
        private Double temperature;
        private List<Map<String, Object>> tools = new ArrayList<>();
        private String system;
        private boolean stream = false;

        public Builder model(String model) { this.model = model; return this; }
        public Builder messages(List<Map<String, Object>> messages) { this.messages = messages; return this; }
        public Builder addMessage(Map<String, Object> message) { this.messages.add(message); return this; }
        public Builder maxTokens(int maxTokens) { this.maxTokens = maxTokens; return this; }
        public Builder temperature(Double temperature) { this.temperature = temperature; return this; }
        public Builder tools(List<Map<String, Object>> tools) { this.tools = tools; return this; }
        public Builder addTool(Map<String, Object> tool) { this.tools.add(tool); return this; }
        public Builder system(String system) { this.system = system; return this; }
        public Builder stream(boolean stream) { this.stream = stream; return this; }

        public ApiRequest build() {
            return new ApiRequest(
                model,
                Collections.unmodifiableList(messages),
                maxTokens,
                temperature,
                tools.isEmpty() ? null : Collections.unmodifiableList(tools),
                system,
                stream
            );
        }
    }
}