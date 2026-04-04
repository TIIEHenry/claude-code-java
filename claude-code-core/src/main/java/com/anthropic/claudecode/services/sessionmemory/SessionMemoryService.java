/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/SessionMemory/sessionMemory.ts
 */
package com.anthropic.claudecode.services.sessionmemory;

import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import com.anthropic.claudecode.*;
import com.anthropic.claudecode.permission.PermissionResult;
import com.anthropic.claudecode.types.MessageTypes;
import com.anthropic.claudecode.utils.Tokens;

/**
 * Session Memory automatically maintains a markdown file with notes about the current conversation.
 * It runs periodically in the background using a forked subagent to extract key information.
 */
public final class SessionMemoryService {
    private SessionMemoryService() {}

    private static volatile String lastMemoryMessageUuid = null;
    private static volatile boolean initialized = false;
    private static volatile boolean extractionInProgress = false;
    private static volatile int lastExtractionTokenCount = 0;
    private static SessionMemoryConfig config = SessionMemoryConfig.defaultConfig();

    /**
     * Session memory config.
     */
    public record SessionMemoryConfig(
        int minimumMessageTokensToInit,
        int minimumTokensBetweenUpdate,
        int toolCallsBetweenUpdates
    ) {
        public static SessionMemoryConfig defaultConfig() {
            return new SessionMemoryConfig(50000, 10000, 10);
        }
    }

    /**
     * Manual extraction result.
     */
    public record ManualExtractionResult(
        boolean success,
        String memoryPath,
        String error
    ) {}

    /**
     * Check if session memory gate is enabled.
     */
    public static boolean isSessionMemoryGateEnabled() {
        // Check feature flag from settings or environment
        String envFlag = System.getenv("CLAUDE_CODE_SESSION_MEMORY_ENABLED");
        if (envFlag != null) {
            return "true".equalsIgnoreCase(envFlag);
        }

        // Check settings file
        try {
            String home = System.getProperty("user.home");
            java.nio.file.Path settingsPath = java.nio.file.Paths.get(home, ".claude", "settings.json");

            if (java.nio.file.Files.exists(settingsPath)) {
                String content = java.nio.file.Files.readString(settingsPath);

                // Find sessionMemory setting
                int sessionIdx = content.indexOf("\"sessionMemory\"");
                if (sessionIdx >= 0) {
                    int valStart = content.indexOf(":", sessionIdx + 14) + 1;
                    while (valStart < content.length() && Character.isWhitespace(content.charAt(valStart))) valStart++;

                    if (valStart < content.length()) {
                        String val = content.substring(valStart);
                        return val.startsWith("true");
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        // Default to enabled
        return true;
    }

    /**
     * Check if should extract memory.
     */
    public static boolean shouldExtractMemory(List<MessageTypes.Message> messages) {
        int currentTokenCount = Tokens.tokenCountWithEstimation(messages);

        if (!initialized) {
            if (!hasMetInitializationThreshold(currentTokenCount)) {
                return false;
            }
            initialized = true;
        }

        boolean hasMetTokenThreshold = hasMetUpdateThreshold(currentTokenCount);
        int toolCallsSinceLastUpdate = countToolCallsSince(messages, lastMemoryMessageUuid);
        boolean hasMetToolCallThreshold = toolCallsSinceLastUpdate >= config.toolCallsBetweenUpdates();
        boolean hasToolCallsInLastTurn = hasToolCallsInLastAssistantTurn(messages);

        boolean shouldExtract =
            (hasMetTokenThreshold && hasMetToolCallThreshold) ||
            (hasMetTokenThreshold && !hasToolCallsInLastTurn);

        if (shouldExtract) {
            MessageTypes.Message lastMessage = messages.get(messages.size() - 1);
            if (lastMessage != null && lastMessage.uuid() != null) {
                lastMemoryMessageUuid = lastMessage.uuid();
            }
            return true;
        }

        return false;
    }

    /**
     * Initialize session memory.
     */
    public static void initSessionMemory() {
        // Register post-sampling hook
        initialized = false;
        lastMemoryMessageUuid = null;
    }

    /**
     * Manually extract session memory.
     */
    public static CompletableFuture<ManualExtractionResult> manuallyExtractSessionMemory(
        List<MessageTypes.Message> messages,
        ToolUseContext toolUseContext
    ) {
        return CompletableFuture.supplyAsync(() -> {
            if (messages.isEmpty()) {
                return new ManualExtractionResult(false, null, "No messages to summarize");
            }

            extractionInProgress = true;
            try {
                // Set up file system and read current state
                String memoryPath = getSessionMemoryPath();
                String currentMemory = readCurrentMemory(memoryPath);

                // Run extraction
                String userPrompt = buildSessionMemoryUpdatePrompt(currentMemory, memoryPath);

                // Run forked agent
                runForkedAgentForMemory(userPrompt, messages, toolUseContext);

                // Record extraction
                recordExtractionTokenCount(Tokens.tokenCountWithEstimation(messages));

                return new ManualExtractionResult(true, memoryPath, null);
            } catch (Exception error) {
                return new ManualExtractionResult(false, null, error.getMessage());
            } finally {
                extractionInProgress = false;
            }
        });
    }

    /**
     * Extract session memory from REPL hook context.
     */
    public static CompletableFuture<Void> extractSessionMemory(REPLHookContext context) {
        return CompletableFuture.runAsync(() -> {
            if (!isSessionMemoryGateEnabled()) {
                return;
            }

            if (!shouldExtractMemory(context.messages())) {
                return;
            }

            extractionInProgress = true;
            try {
                String memoryPath = getSessionMemoryPath();
                String currentMemory = readCurrentMemory(memoryPath);
                String userPrompt = buildSessionMemoryUpdatePrompt(currentMemory, memoryPath);

                runForkedAgentForMemory(userPrompt, context.messages(), context.toolUseContext());

                recordExtractionTokenCount(Tokens.tokenCountWithEstimation(context.messages()));
            } catch (Exception e) {
                // Log error
            } finally {
                extractionInProgress = false;
            }
        });
    }

    /**
     * Create can use tool function for memory file.
     */
    public static CanUseToolFn createMemoryFileCanUseTool(String memoryPath) {
        return (tool, input, context, assistantMessage, toolUseId) -> {
            if (tool.name().equals("Edit") && input instanceof Map) {
                Map<?, ?> mapInput = (Map<?, ?>) input;
                Object filePath = mapInput.get("file_path");
                if (filePath instanceof String && filePath.equals(memoryPath)) {
                    return CompletableFuture.completedFuture(
                        new PermissionResult.Allow<>(input)
                    );
                }
            }
            return CompletableFuture.completedFuture(
                new PermissionResult.Deny(
                    "only Edit on " + memoryPath + " is allowed"
                )
            );
        };
    }

    // Helper methods

    private static boolean hasMetInitializationThreshold(int tokenCount) {
        return tokenCount >= config.minimumMessageTokensToInit();
    }

    private static boolean hasMetUpdateThreshold(int tokenCount) {
        // Check against last extraction token count
        if (lastExtractionTokenCount <= 0) {
            return tokenCount >= config.minimumMessageTokensToInit();
        }

        int tokenIncrease = tokenCount - lastExtractionTokenCount;
        return tokenIncrease >= config.minimumTokensBetweenUpdate();
    }

    private static int countToolCallsSince(List<MessageTypes.Message> messages, String sinceUuid) {
        int toolCallCount = 0;
        boolean foundStart = sinceUuid == null;

        for (MessageTypes.Message message : messages) {
            if (!foundStart) {
                if (message.uuid() != null && message.uuid().equals(sinceUuid)) {
                    foundStart = true;
                }
                continue;
            }

            if (message instanceof MessageTypes.AssistantMessage) {
                MessageTypes.AssistantMessage assistant = (MessageTypes.AssistantMessage) message;
                toolCallCount += assistant.getToolUseCount();
            }
        }

        return toolCallCount;
    }

    private static boolean hasToolCallsInLastAssistantTurn(List<MessageTypes.Message> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            MessageTypes.Message message = messages.get(i);
            if (message instanceof MessageTypes.AssistantMessage) {
                MessageTypes.AssistantMessage assistant = (MessageTypes.AssistantMessage) message;
                return assistant.getToolUseCount() > 0;
            }
        }
        return false;
    }

    private static String getSessionMemoryPath() {
        String home = System.getProperty("user.home");
        String cwd = System.getProperty("user.dir");
        String slug = cwd.replaceAll("[^a-zA-Z0-9]", "-");
        return home + "/.claude/projects/" + slug + "/memory/SESSION_MEMORY.md";
    }

    private static String readCurrentMemory(String path) {
        try {
            return Files.readString(Paths.get(path));
        } catch (Exception e) {
            return "";
        }
    }

    private static String buildSessionMemoryUpdatePrompt(String currentMemory, String memoryPath) {
        return "Update session memory at " + memoryPath + " with recent conversation highlights.\n\n" +
               "Current memory:\n" + currentMemory + "\n\n" +
               "Add any new important information about the session.";
    }

    private static void runForkedAgentForMemory(String prompt, List<MessageTypes.Message> messages, ToolUseContext context) {
        // Run a forked agent to extract session memory
        CompletableFuture.runAsync(() -> {
            try {
                // Create a minimal message context for the agent
                List<Map<String, Object>> agentMessages = new ArrayList<>();

                // Add the prompt as a user message
                Map<String, Object> userMsg = new LinkedHashMap<>();
                userMsg.put("role", "user");
                userMsg.put("content", prompt);
                agentMessages.add(userMsg);

                // Get API key
                String apiKey = System.getenv("ANTHROPIC_API_KEY");
                if (apiKey == null || apiKey.isEmpty()) {
                    return;
                }

                String baseUrl = System.getenv("ANTHROPIC_BASE_URL");
                if (baseUrl == null) baseUrl = "https://api.anthropic.com";

                // Build request
                StringBuilder jsonBody = new StringBuilder();
                jsonBody.append("{\"model\":\"claude-haiku-4-5-20251001\",");
                jsonBody.append("\"max_tokens\":1024,");
                jsonBody.append("\"messages\":[");

                for (int i = 0; i < agentMessages.size(); i++) {
                    if (i > 0) jsonBody.append(",");
                    Map<String, Object> msg = agentMessages.get(i);
                    jsonBody.append("{\"role\":\"").append(msg.get("role")).append("\",");
                    jsonBody.append("\"content\":\"").append(escapeJson(String.valueOf(msg.get("content")))).append("\"}");
                }

                jsonBody.append("]}");

                // Make API call
                java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(baseUrl + "/v1/messages"))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
                    .build();

                java.net.http.HttpResponse<String> response = httpClient.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    // Extract the memory content and write to file
                    String memoryContent = extractContent(response.body());
                    if (memoryContent != null && !memoryContent.isEmpty()) {
                        String memoryPath = getSessionMemoryPath();
                        java.nio.file.Files.writeString(java.nio.file.Paths.get(memoryPath), memoryContent);
                    }
                }
            } catch (Exception e) {
                // Ignore errors in memory extraction
            }
        });
    }

    private static String extractContent(String json) {
        int contentIdx = json.indexOf("\"content\"");
        if (contentIdx < 0) return null;

        int arrStart = json.indexOf("[", contentIdx);
        if (arrStart < 0) return null;

        int textIdx = json.indexOf("\"text\":", arrStart);
        if (textIdx < 0) return null;

        int valStart = json.indexOf("\"", textIdx + 7) + 1;
        int valEnd = json.indexOf("\"", valStart);

        if (valStart > 0 && valEnd > valStart) {
            return json.substring(valStart, valEnd);
        }
        return null;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static void recordExtractionTokenCount(int tokenCount) {
        // Record for tracking
        lastExtractionTokenCount = tokenCount;

        try {
            String home = System.getProperty("user.home");
            String cwd = System.getProperty("user.dir");
            String slug = cwd.replaceAll("[^a-zA-Z0-9]", "-");
            java.nio.file.Path trackingPath = java.nio.file.Paths.get(home, ".claude", "projects", slug, "memory-tracking.json");

            java.nio.file.Files.createDirectories(trackingPath.getParent());

            String json = String.format(
                "{\"lastExtractionTokenCount\":%d,\"lastExtractionTime\":\"%s\"}",
                tokenCount,
                java.time.Instant.now().toString()
            );

            java.nio.file.Files.writeString(trackingPath, json);
        } catch (Exception e) {
            // Ignore errors
        }
    }

    /**
     * Reset state (for testing).
     */
    public static void reset() {
        lastMemoryMessageUuid = null;
        initialized = false;
        extractionInProgress = false;
    }

    /**
     * REPL hook context.
     */
    public record REPLHookContext(
        List<MessageTypes.Message> messages,
        ToolUseContext toolUseContext,
        String querySource
    ) {}
}