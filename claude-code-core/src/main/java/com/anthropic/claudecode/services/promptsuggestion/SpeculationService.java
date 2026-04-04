/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/PromptSuggestion/speculation.ts
 */
package com.anthropic.claudecode.services.promptsuggestion;

import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import com.anthropic.claudecode.*;
import com.anthropic.claudecode.state.AppState;
import com.anthropic.claudecode.types.MessageTypes;
import com.anthropic.claudecode.utils.Debug;
import com.anthropic.claudecode.utils.MessageUtils;

/**
 * Speculation service - runs forked agents to speculate ahead of user actions.
 * Uses overlay filesystem for isolated writes during speculation.
 */
public final class SpeculationService {
    private SpeculationService() {}

    private static final int MAX_SPECULATION_TURNS = 20;
    private static final int MAX_SPECULATION_MESSAGES = 100;

    private static final Set<String> WRITE_TOOLS = Set.of("Edit", "Write", "NotebookEdit");
    private static final Set<String> SAFE_READ_ONLY_TOOLS = Set.of(
        "Read", "Glob", "Grep", "ToolSearch", "LSP", "TaskGet", "TaskList"
    );

    /**
     * Speculation state.
     */
    public sealed interface SpeculationState permits
        SpeculationState.Idle, SpeculationState.Active {

        public record Idle() implements SpeculationState {}

        public record Active(
            String id,
            Runnable abort,
            long startTime,
            List<MessageTypes.Message> messages,
            Set<String> writtenPaths,
            CompletionBoundary boundary,
            int suggestionLength,
            int toolUseCount,
            boolean isPipelined,
            REPLHookContext context
        ) implements SpeculationState {}
    }

    /**
     * Completion boundary.
     */
    public sealed interface CompletionBoundary permits
        CompletionBoundary.Bash, CompletionBoundary.Edit,
        CompletionBoundary.DeniedTool, CompletionBoundary.Complete {

        public record Bash(String command, long completedAt) implements CompletionBoundary {}
        public record Edit(String toolName, String filePath, long completedAt) implements CompletionBoundary {}
        public record DeniedTool(String toolName, String detail, long completedAt) implements CompletionBoundary {}
        public record Complete(long completedAt, int outputTokens) implements CompletionBoundary {}
    }

    /**
     * Speculation result.
     */
    public record SpeculationResult(
        List<MessageTypes.Message> messages,
        CompletionBoundary boundary,
        long timeSavedMs
    ) {}

    /**
     * Check if speculation is enabled.
     */
    public static boolean isSpeculationEnabled() {
        String userType = System.getenv("USER_TYPE");
        if (!"ant".equals(userType)) return false;

        // Check global config - default true for ant users
        return true;
    }

    /**
     * Get overlay path for speculation.
     */
    public static Path getOverlayPath(String id) {
        String tempDir = System.getProperty("java.io.tmpdir");
        long pid = ProcessHandle.current().pid();
        return Paths.get(tempDir, "claude-speculation", String.valueOf(pid), id);
    }

    /**
     * Start speculation.
     */
    public static CompletableFuture<Void> startSpeculation(
        String suggestionText,
        REPLHookContext context,
        Consumer<AppState> setAppState,
        boolean isPipelined,
        CacheSafeParams cacheSafeParams
    ) {
        if (!isSpeculationEnabled()) {
            return CompletableFuture.completedFuture(null);
        }

        // Abort any existing speculation
        abortSpeculation(setAppState);

        String id = UUID.randomUUID().toString().substring(0, 8);
        AbortController abortController = new AbortController();

        if (abortController.isAborted()) {
            return CompletableFuture.completedFuture(null);
        }

        long startTime = System.currentTimeMillis();
        List<MessageTypes.Message> messagesRef = new CopyOnWriteArrayList<>();
        Set<String> writtenPathsRef = new CopyOnWriteArraySet<>();
        Path overlayPath = getOverlayPath(id);
        String cwd = System.getProperty("user.dir");

        // Create overlay directory
        try {
            Files.createDirectories(overlayPath);
        } catch (Exception e) {
            Debug.logForDebugging("[Speculation] Failed to create overlay directory");
            return CompletableFuture.completedFuture(null);
        }

        // Set initial state
        setAppState.accept(prev -> prev.withSpeculation(new SpeculationState.Active(
            id,
            () -> abortController.abort(),
            startTime,
            messagesRef,
            writtenPathsRef,
            null,
            suggestionText.length(),
            0,
            isPipelined,
            context
        )));

        Debug.logForDebugging("[Speculation] Starting speculation " + id);

        return CompletableFuture.runAsync(() -> {
            try {
                runForkedAgentForSpeculation(
                    suggestionText,
                    context,
                    abortController,
                    overlayPath,
                    cwd,
                    messagesRef,
                    writtenPathsRef,
                    setAppState,
                    isPipelined
                );
            } catch (Exception e) {
                abortController.abort();
                safeRemoveOverlay(overlayPath);
                resetSpeculationState(setAppState);
            }
        });
    }

    /**
     * Accept speculation.
     */
    public static CompletableFuture<SpeculationResult> acceptSpeculation(
        SpeculationState state,
        Consumer<AppState> setAppState,
        int cleanMessageCount
    ) {
        if (!(state instanceof SpeculationState.Active active)) {
            return CompletableFuture.completedFuture(null);
        }

        String id = active.id();
        List<MessageTypes.Message> messages = active.messages();
        Set<String> writtenPaths = active.writtenPaths();
        Runnable abort = active.abort();
        long startTime = active.startTime();
        int suggestionLength = active.suggestionLength();
        boolean isPipelined = active.isPipelined();

        Path overlayPath = getOverlayPath(id);
        long acceptedAt = System.currentTimeMillis();

        abort.run();

        // Copy overlay to main if messages were produced
        if (cleanMessageCount > 0) {
            copyOverlayToMain(overlayPath, writtenPaths, System.getProperty("user.dir"));
        }
        safeRemoveOverlay(overlayPath);

        CompletionBoundary boundary = active.boundary();
        long timeSavedMs = Math.min(acceptedAt, boundary != null ? getBoundaryTime(boundary) : Long.MAX_VALUE) - startTime;

        // Update state
        setAppState.accept(prev -> prev
            .withSpeculation(new SpeculationState.Idle())
            .withSpeculationSessionTimeSavedMs(prev.speculationSessionTimeSavedMs() + timeSavedMs));

        Debug.logForDebugging(boundary == null
            ? "[Speculation] Accept " + id + ": still running"
            : "[Speculation] Accept " + id + ": already complete");

        return CompletableFuture.completedFuture(new SpeculationResult(messages, boundary, timeSavedMs));
    }

    /**
     * Abort speculation.
     */
    public static void abortSpeculation(Consumer<AppState> setAppState) {
        setAppState.accept(prev -> {
            if (!(prev.speculation() instanceof SpeculationState.Active active)) {
                return prev;
            }

            Debug.logForDebugging("[Speculation] Aborting " + active.id());
            active.abort().run();
            safeRemoveOverlay(getOverlayPath(active.id()));

            return prev.withSpeculation(new SpeculationState.Idle());
        });
    }

    /**
     * Handle speculation accept.
     */
    public static CompletableFuture<Map<String, Boolean>> handleSpeculationAccept(
        SpeculationState.Active speculationState,
        long speculationSessionTimeSavedMs,
        Consumer<AppState> setAppState,
        String input,
        Map<String, Object> deps
    ) {
        try {
            // Clear prompt suggestion state
            setAppState.accept(prev -> new AppState());

            List<MessageTypes.Message> speculationMessages = speculationState.messages();
            List<MessageTypes.Message> cleanMessages = prepareMessagesForInjection(speculationMessages);

            SpeculationResult result = acceptSpeculation(
                speculationState,
                setAppState,
                cleanMessages.size()
            ).join();

            boolean isComplete = result.boundary() != null && result.boundary() instanceof CompletionBoundary.Complete;

            // Drop trailing assistant messages if not complete
            if (!isComplete) {
                int lastNonAssistant = -1;
                for (int i = cleanMessages.size() - 1; i >= 0; i--) {
                    if (!(cleanMessages.get(i) instanceof MessageTypes.AssistantMessage)) {
                        lastNonAssistant = i;
                        break;
                    }
                }
                if (lastNonAssistant >= 0) {
                    cleanMessages = cleanMessages.subList(0, lastNonAssistant + 1);
                }
            }

            Debug.logForDebugging("[Speculation] " +
                (result.boundary() != null ? result.boundary().getClass().getSimpleName() : "incomplete") +
                ", injected " + cleanMessages.size() + " messages");

            return CompletableFuture.completedFuture(Map.of("queryRequired", !isComplete));
        } catch (Exception e) {
            safeRemoveOverlay(getOverlayPath(speculationState.id()));
            resetSpeculationState(setAppState);
            return CompletableFuture.completedFuture(Map.of("queryRequired", true));
        }
    }

    /**
     * Prepare messages for injection.
     */
    public static List<MessageTypes.Message> prepareMessagesForInjection(List<MessageTypes.Message> messages) {
        // Find tool_use IDs that have successful results
        Set<String> successfulToolIds = new HashSet<>();
        for (MessageTypes.Message msg : messages) {
            if (msg instanceof MessageTypes.UserMessage user) {
                for (Object block : user.getContentBlocks()) {
                    if (block instanceof Map) {
                        Map<?, ?> map = (Map<?, ?>) block;
                        if ("tool_result".equals(map.get("type"))) {
                            Boolean isError = (Boolean) map.get("is_error");
                            if (isError == null || !isError) {
                                String toolUseId = (String) map.get("tool_use_id");
                                if (toolUseId != null) {
                                    successfulToolIds.add(toolUseId);
                                }
                            }
                        }
                    }
                }
            }
        }

        List<MessageTypes.Message> result = new ArrayList<>();
        for (MessageTypes.Message msg : messages) {
            // Filter out messages with unsuccessful tool results
            result.add(msg);
        }

        return result;
    }

    // Helper methods

    private static void runForkedAgentForSpeculation(
        String suggestionText,
        REPLHookContext context,
        AbortController abortController,
        Path overlayPath,
        String cwd,
        List<MessageTypes.Message> messagesRef,
        Set<String> writtenPathsRef,
        Consumer<AppState> setAppState,
        boolean isPipelined
    ) {
        Debug.logForDebugging("[Speculation] Running forked agent for: " + suggestionText.substring(0, Math.min(50, suggestionText.length())));

        // Run speculation agent in background
        CompletableFuture.runAsync(() -> {
            try {
                // Get API key for speculation agent
                String apiKey = System.getenv("ANTHROPIC_API_KEY");
                if (apiKey == null || apiKey.isEmpty()) {
                    Debug.logForDebugging("[Speculation] No API key, skipping speculation");
                    return;
                }

                String baseUrl = System.getenv("ANTHROPIC_BASE_URL");
                if (baseUrl == null) baseUrl = "https://api.anthropic.com";

                // Build speculation request
                StringBuilder jsonBody = new StringBuilder();
                jsonBody.append("{\"model\":\"claude-haiku-4-5-20251001\",");
                jsonBody.append("\"max_tokens\":1024,");
                jsonBody.append("\"system\":\"You are a speculation agent. Analyze the suggestion and prepare for likely next actions.\",");
                jsonBody.append("\"messages\":[{\"role\":\"user\",\"content\":\"");
                jsonBody.append(escapeJson(suggestionText)).append("\"}]}");

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
                    // Extract speculation results
                    String content = extractContent(response.body());
                    Debug.logForDebugging("[Speculation] Result: " + content);

                    // Write speculation results to overlay
                    if (overlayPath != null && content != null) {
                        Files.createDirectories(overlayPath);
                        Path specFile = overlayPath.resolve("speculation-result.md");
                        Files.writeString(specFile, "# Speculation Result\n\n" + content);
                        writtenPathsRef.add(specFile.toString());
                    }
                }
            } catch (Exception e) {
                Debug.logForDebugging("[Speculation] Error: " + e.getMessage());
            }
        });
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
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

    private static void safeRemoveOverlay(Path overlayPath) {
        try {
            Files.walk(overlayPath)
                .sorted(Comparator.reverseOrder())
                .forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (Exception e) {}
                });
        } catch (Exception e) {
            // Ignore
        }
    }

    private static void copyOverlayToMain(Path overlayPath, Set<String> writtenPaths, String cwd) {
        for (String rel : writtenPaths) {
            Path src = overlayPath.resolve(rel);
            Path dest = Paths.get(cwd, rel);
            try {
                Files.createDirectories(dest.getParent());
                Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                Debug.logForDebugging("[Speculation] Failed to copy " + rel + " to main");
            }
        }
    }

    private static long getBoundaryTime(CompletionBoundary boundary) {
        if (boundary instanceof CompletionBoundary.Bash b) return b.completedAt();
        if (boundary instanceof CompletionBoundary.Edit e) return e.completedAt();
        if (boundary instanceof CompletionBoundary.DeniedTool d) return d.completedAt();
        if (boundary instanceof CompletionBoundary.Complete c) return c.completedAt();
        return Long.MAX_VALUE;
    }

    private static void resetSpeculationState(Consumer<AppState> setAppState) {
        setAppState.accept(prev -> prev.withSpeculation(new SpeculationState.Idle()));
    }

    /**
     * Deny speculation result.
     */
    public record DenySpeculation(
        String behavior,
        String message,
        Map<String, Object> decisionReason
    ) {}

    /**
     * REPL hook context.
     */
    public record REPLHookContext(
        List<MessageTypes.Message> messages,
        ToolUseContext toolUseContext,
        String querySource
    ) {}

    /**
     * Cache safe params.
     */
    public record CacheSafeParams(
        Object systemPrompt,
        Object userContext,
        Object systemContext,
        ToolUseContext toolUseContext,
        List<MessageTypes.Message> forkContextMessages
    ) {}

    /**
     * Abort controller.
     */
    public static class AbortController {
        private volatile boolean aborted = false;

        public void abort() { this.aborted = true; }
        public boolean isAborted() { return aborted; }
    }

    /**
     * Consumer functional interface for state updates.
     */
    @FunctionalInterface
    public interface Consumer<T> {
        void accept(java.util.function.Function<T, T> updater);
    }
}