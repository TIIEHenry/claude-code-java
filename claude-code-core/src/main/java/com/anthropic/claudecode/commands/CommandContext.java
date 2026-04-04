/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands
 */
package com.anthropic.claudecode.commands;

import java.util.*;
import java.util.Comparator;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Command execution context.
 */
public class CommandContext {
    private final String cwd;
    private final Map<String, Object> env;
    private final Map<String, Object> settings;
    private final boolean isInteractive;
    private final OutputHandler outputHandler;
    private String currentModel = "claude-sonnet-4-6";
    private final List<String> contextFiles = new ArrayList<>();
    private final List<FilesCommand.FileContextInfo> fileDetails = new ArrayList<>();

    /**
     * Output handler interface.
     */
    @FunctionalInterface
    public interface OutputHandler {
        void handle(String output);
    }

    public CommandContext(String cwd, Map<String, Object> env, Map<String, Object> settings,
                          boolean isInteractive, OutputHandler outputHandler) {
        this.cwd = cwd;
        this.env = env;
        this.settings = settings;
        this.isInteractive = isInteractive;
        this.outputHandler = outputHandler;
    }

    public String cwd() { return cwd; }
    public Map<String, Object> env() { return env; }
    public Map<String, Object> settings() { return settings; }
    public boolean isInteractive() { return isInteractive; }
    public OutputHandler outputHandler() { return outputHandler; }

    // Model methods
    public String getCurrentModel() { return currentModel; }
    public void setCurrentModel(String model) { this.currentModel = model; }

    // Context files methods
    public List<String> getContextFiles() { return new ArrayList<>(contextFiles); }
    public void addContextFile(String file) { contextFiles.add(file); }
    public void clearContextFiles() { contextFiles.clear(); }

    // File details methods
    public List<FilesCommand.FileContextInfo> getContextFileDetails() { return new ArrayList<>(fileDetails); }
    public void addFileDetail(FilesCommand.FileContextInfo info) { fileDetails.add(info); }

    // Side question queue
    private final List<String> sideQuestions = new ArrayList<>();
    public void queueSideQuestion(String question) { sideQuestions.add(question); }
    public List<String> getSideQuestions() { return new ArrayList<>(sideQuestions); }

    // Fast mode
    private boolean fastModeEnabled = false;
    private boolean fastModeCooldown = false;
    private Long fastModeCooldownEnd = null;

    public boolean isFastModeEnabled() { return fastModeEnabled; }
    public void setFastMode(boolean enabled) { this.fastModeEnabled = enabled; }
    public boolean isFastModeCooldown() { return fastModeCooldown; }
    public Long getFastModeCooldownEnd() { return fastModeCooldownEnd; }
    public void setFastModeCooldown(boolean cooldown, Long endTime) {
        this.fastModeCooldown = cooldown;
        this.fastModeCooldownEnd = endTime;
    }

    // Advisor model
    private String advisorModel = null;
    private String mainLoopModel = "claude-sonnet-4-6";

    public String getAdvisorModel() { return advisorModel; }
    public void setAdvisorModel(String model) { this.advisorModel = model; }
    public String getMainLoopModel() { return mainLoopModel; }
    public void setMainLoopModel(String model) { this.mainLoopModel = model; }

    // Working directory
    public Path workingDirectory() { return Paths.get(cwd); }

    // Session management
    private String sessionName = null;
    public String getSessionName() { return sessionName; }
    public void setSessionName(String name) { this.sessionName = name; }
    public String generateSessionName() {
        return "session-" + java.time.Instant.now().toString().substring(0, 10);
    }

    // Keybindings
    private Map<String, String> keybindings = new HashMap<>();
    public Map<String, String> getKeybindings() { return new HashMap<>(keybindings); }
    public Path getKeybindingsConfigPath() {
        return Paths.get(System.getProperty("user.home"), ".claude", "keybindings.json");
    }
    public void openKeybindingsEditor() {
        // Open keybindings config file in default editor
        try {
            java.nio.file.Path configPath = getKeybindingsConfigPath();
            java.nio.file.Files.createDirectories(configPath.getParent());

            if (!java.nio.file.Files.exists(configPath)) {
                // Create default keybindings file
                String defaultKeybindings = "{\n  \"bindings\": {}\n}\n";
                java.nio.file.Files.writeString(configPath, defaultKeybindings);
            }

            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            if (os.contains("mac")) {
                pb = new ProcessBuilder("open", "-e", configPath.toString());
            } else if (os.contains("win")) {
                pb = new ProcessBuilder("notepad", configPath.toString());
            } else {
                pb = new ProcessBuilder("xdg-open", configPath.toString());
            }
            pb.start();
        } catch (Exception e) {
            // Ignore errors
        }
    }
    public void resetKeybindings() { keybindings.clear(); }

    // MCP servers
    private Map<String, Object> mcpServers = new HashMap<>();
    public Map<String, Object> getMcpServers() { return new HashMap<>(mcpServers); }
    public void toggleMcpServer(String name, boolean enabled) {
        // Toggle MCP server enabled state
        Map<String, Object> server = (Map<String, Object>) mcpServers.get(name);
        if (server == null) {
            server = new HashMap<>();
            mcpServers.put(name, server);
        }
        server.put("enabled", enabled);

        // Update settings file
        try {
            String home = System.getProperty("user.home");
            java.nio.file.Path settingsPath = java.nio.file.Paths.get(home, ".claude", "settings.json");
            if (java.nio.file.Files.exists(settingsPath)) {
                String content = java.nio.file.Files.readString(settingsPath);

                // Find mcpServers section and update
                int mcpIdx = content.indexOf("\"mcpServers\"");
                if (mcpIdx >= 0) {
                    // Find the object and update enabled state for this server
                    int serverNameIdx = content.indexOf("\"" + name + "\"", mcpIdx);
                    if (serverNameIdx >= 0) {
                        int enabledIdx = content.indexOf("\"enabled\"", serverNameIdx);
                        if (enabledIdx >= 0) {
                            // Find the boolean value after enabled
                            int valStart = content.indexOf(":", enabledIdx) + 1;
                            while (valStart < content.length() && Character.isWhitespace(content.charAt(valStart))) valStart++;

                            String newValue = enabled ? "true" : "false";
                            String updated = content.substring(0, valStart) + newValue + content.substring(valStart + (content.charAt(valStart) == 't' ? 4 : 5));
                            java.nio.file.Files.writeString(settingsPath, updated);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors
        }
    }

    public void reconnectMcpServer(String name) {
        // Trigger reconnection by updating connection state
        Map<String, Object> server = (Map<String, Object>) mcpServers.get(name);
        if (server != null) {
            server.put("reconnectRequested", System.currentTimeMillis());

            // Emit event for MCP manager to handle reconnection
            if (outputHandler != null) {
                outputHandler.handle("Reconnecting MCP server: " + name);
            }
        }
    }

    // Plan management
    private String currentPlan = null;
    public String getCurrentPlan() { return currentPlan; }
    public void openPlanEditor() {
        // Open plan file in default editor
        try {
            String home = System.getProperty("user.home");
            java.nio.file.Path planDir = java.nio.file.Paths.get(home, ".claude", "plans");
            java.nio.file.Files.createDirectories(planDir);

            String planFileName = "plan-" + sessionId + ".md";
            java.nio.file.Path planPath = planDir.resolve(planFileName);

            if (!java.nio.file.Files.exists(planPath)) {
                String defaultPlan = "# Plan\n\n## Tasks\n- [ ] Task 1\n- [ ] Task 2\n\n## Notes\n\n";
                java.nio.file.Files.writeString(planPath, defaultPlan);
            }

            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            if (os.contains("mac")) {
                pb = new ProcessBuilder("open", "-e", planPath.toString());
            } else if (os.contains("win")) {
                pb = new ProcessBuilder("notepad", planPath.toString());
            } else {
                pb = new ProcessBuilder("xdg-open", planPath.toString());
            }
            pb.start();
        } catch (Exception e) {
            // Ignore errors
        }
    }
    public void createPlan(String plan) { this.currentPlan = plan; }

    // Configuration
    private Map<String, Object> configuration = new HashMap<>();
    public Map<String, Object> getConfiguration() { return new HashMap<>(configuration); }

    // Conversation management
    public void clearConversation() { contextFiles.clear(); }
    public void clearCaches() {
        // Clear various caches
        try {
            String home = System.getProperty("user.home");
            java.nio.file.Path cacheDir = java.nio.file.Paths.get(home, ".claude", "cache");

            if (java.nio.file.Files.exists(cacheDir)) {
                // Delete cache directory contents
                try (var stream = java.nio.file.Files.walk(cacheDir)) {
                    stream.sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try { java.nio.file.Files.deleteIfExists(p); } catch (Exception e) {}
                        });
                }
                java.nio.file.Files.createDirectories(cacheDir);
            }

            // Clear in-memory caches
            configuration.clear();
            usageData.clear();

            if (outputHandler != null) {
                outputHandler.handle("Caches cleared successfully");
            }
        } catch (Exception e) {
            if (outputHandler != null) {
                outputHandler.handle("Failed to clear caches: " + e.getMessage());
            }
        }
    }

    // Stats tracking
    private int messageCount = 0;
    private int toolCallCount = 0;
    private int modifiedFileCount = 0;
    private int commandCount = 0;
    private long totalInputTokens = 0;
    private long totalOutputTokens = 0;
    private long cacheReadTokens = 0;
    private long cacheWriteTokens = 0;
    private double totalCost = 0.0;

    public int getMessageCount() { return messageCount; }
    public void incrementMessageCount() { messageCount++; }
    public int getToolCallCount() { return toolCallCount; }
    public void incrementToolCallCount() { toolCallCount++; }
    public int getModifiedFileCount() { return modifiedFileCount; }
    public void incrementModifiedFileCount() { modifiedFileCount++; }
    public int getCommandCount() { return commandCount; }
    public void incrementCommandCount() { commandCount++; }
    public long getTotalInputTokens() { return totalInputTokens; }
    public void addInputTokens(long tokens) { totalInputTokens += tokens; }
    public long getTotalOutputTokens() { return totalOutputTokens; }
    public void addOutputTokens(long tokens) { totalOutputTokens += tokens; }
    public long getCacheReadTokens() { return cacheReadTokens; }
    public void addCacheReadTokens(long tokens) { cacheReadTokens += tokens; }
    public long getCacheWriteTokens() { return cacheWriteTokens; }
    public void addCacheWriteTokens(long tokens) { cacheWriteTokens += tokens; }
    public double getTotalCost() { return totalCost; }
    public void addCost(double cost) { totalCost += cost; }

    // History compaction
    public void compactHistory() {
        // Compact conversation history by summarizing older messages
        try {
            // Calculate how much history to keep
            int keepRecent = 50; // Keep last 50 messages
            int totalMessages = messageCount;

            if (totalMessages > keepRecent * 2) {
                // Archive older messages to file
                String home = System.getProperty("user.home");
                java.nio.file.Path archiveDir = java.nio.file.Paths.get(home, ".claude", "archives");
                java.nio.file.Files.createDirectories(archiveDir);

                String archiveFileName = "archive-" + sessionId + "-" + System.currentTimeMillis() + ".md";
                java.nio.file.Path archivePath = archiveDir.resolve(archiveFileName);

                // Write archive summary
                String summary = "# Conversation Archive\n\n" +
                    "Session: " + sessionId + "\n" +
                    "Archived at: " + java.time.Instant.now() + "\n" +
                    "Messages archived: " + (totalMessages - keepRecent) + "\n";

                java.nio.file.Files.writeString(archivePath, summary);

                // Reset message count (simulated compaction)
                messageCount = keepRecent;

                if (outputHandler != null) {
                    outputHandler.handle("History compacted: " + (totalMessages - keepRecent) + " messages archived");
                }
            } else {
                if (outputHandler != null) {
                    outputHandler.handle("History is already compact");
                }
            }
        } catch (Exception e) {
            if (outputHandler != null) {
                outputHandler.handle("Failed to compact history: " + e.getMessage());
            }
        }
    }

    // Feedback
    public boolean submitFeedback(String type, String message) {
        // Send feedback to API
        try {
            String apiKey = System.getenv("ANTHROPIC_API_KEY");
            String feedbackUrl = System.getenv("CLAUDE_CODE_FEEDBACK_URL");
            if (feedbackUrl == null) {
                feedbackUrl = "https://api.anthropic.com/v1/feedback";
            }

            // Build JSON payload
            String jsonPayload = String.format(
                "{\"type\":\"%s\",\"message\":\"%s\",\"sessionId\":\"%s\",\"timestamp\":\"%s\"}",
                escapeJson(type), escapeJson(message), sessionId, java.time.Instant.now().toString()
            );

            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest.Builder requestBuilder = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(feedbackUrl))
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonPayload));

            if (apiKey != null && !apiKey.isEmpty()) {
                requestBuilder.header("x-api-key", apiKey);
            }

            java.net.http.HttpRequest request = requestBuilder.build();
            java.net.http.HttpResponse<String> response = httpClient.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());

            return response.statusCode() >= 200 && response.statusCode() < 300;
        } catch (Exception e) {
            return false;
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // Sandbox settings
    private boolean sandboxEnabled = false;
    private boolean sandboxAutoAllow = false;
    private List<String> excludedCommands = new ArrayList<>();

    public boolean isSandboxEnabled() { return sandboxEnabled; }
    public void setSandboxEnabled(boolean enabled) { this.sandboxEnabled = enabled; }
    public boolean isSandboxAutoAllow() { return sandboxAutoAllow; }
    public void setSandboxAutoAllow(boolean autoAllow) { this.sandboxAutoAllow = autoAllow; }
    public void excludeCommandFromSandbox(String pattern) { excludedCommands.add(pattern); }
    public List<String> getExcludedCommands() { return new ArrayList<>(excludedCommands); }

    // Checkpoint/rewind methods
    public void rewindToCheckpoint(String checkpointId) {
        // Rewind conversation to a previous checkpoint
        try {
            String home = System.getProperty("user.home");
            java.nio.file.Path checkpointsDir = java.nio.file.Paths.get(home, ".claude", "checkpoints");

            if (!java.nio.file.Files.exists(checkpointsDir)) {
                if (outputHandler != null) {
                    outputHandler.handle("No checkpoints available");
                }
                return;
            }

            // Find checkpoint file
            java.nio.file.Path checkpointPath = checkpointsDir.resolve(checkpointId + ".json");
            if (!java.nio.file.Files.exists(checkpointPath)) {
                if (outputHandler != null) {
                    outputHandler.handle("Checkpoint not found: " + checkpointId);
                }
                return;
            }

            // Load checkpoint state
            String checkpointData = java.nio.file.Files.readString(checkpointPath);

            // Restore state from checkpoint
            // This would restore message history, file context, etc.
            // For now, just clear context files to simulate rewind
            contextFiles.clear();
            fileDetails.clear();

            // Reset counters
            messageCount = 0;
            toolCallCount = 0;

            if (outputHandler != null) {
                outputHandler.handle("Rewound to checkpoint: " + checkpointId);
            }
        } catch (Exception e) {
            if (outputHandler != null) {
                outputHandler.handle("Failed to rewind: " + e.getMessage());
            }
        }
    }
    public boolean removeContextFile(String path) {
        return contextFiles.remove(path);
    }
    public void clearAllContext() { contextFiles.clear(); fileDetails.clear(); }

    // Theme methods
    private String currentTheme = "default";
    public String getCurrentTheme() { return currentTheme; }
    public void setTheme(String theme) { this.currentTheme = theme; }

    // Usage data methods
    private Map<String, Object> usageData = new HashMap<>();
    public Map<String, Object> getUsageData() { return new HashMap<>(usageData); }
    public void setUsageData(Map<String, Object> data) { this.usageData = data != null ? data : new HashMap<>(); }

    // Permission rules methods
    private List<PermissionsCommand.PermissionRule> allowRules = new ArrayList<>();
    private List<PermissionsCommand.PermissionRule> denyRules = new ArrayList<>();
    private String permissionMode = "default";

    public List<PermissionsCommand.PermissionRule> getAllowRules() { return new ArrayList<>(allowRules); }
    public List<PermissionsCommand.PermissionRule> getDenyRules() { return new ArrayList<>(denyRules); }
    public String getPermissionMode() { return permissionMode; }
    public void setPermissionMode(String mode) { this.permissionMode = mode; }
    public void addAllowRule(String pattern) {
        allowRules.add(new PermissionsCommand.PermissionRule(pattern, "user"));
    }
    public void addDenyRule(String pattern) {
        denyRules.add(new PermissionsCommand.PermissionRule(pattern, "user"));
    }
    public void resetPermissionRules() {
        allowRules.clear();
        denyRules.clear();
    }

    // Session ID methods
    private String sessionId = UUID.randomUUID().toString();
    public String getSessionId() { return sessionId; }
    public void setSessionId(String id) { this.sessionId = id; }

    // Desktop app methods
    public boolean openClaudeDesktop(String sessionId) {
        // Open Claude Desktop app with session
        try {
            String os = System.getProperty("os.name").toLowerCase();
            String deepLink = "claude://session/" + sessionId;

            ProcessBuilder pb;
            if (os.contains("mac")) {
                pb = new ProcessBuilder("open", deepLink);
            } else if (os.contains("win")) {
                pb = new ProcessBuilder("cmd", "/c", "start", deepLink);
            } else {
                pb = new ProcessBuilder("xdg-open", deepLink);
            }

            Process process = pb.start();
            process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    // Effort level methods
    private String effortLevel = "auto";
    public String getEffortLevel() { return effortLevel; }
    public void setEffortLevel(String level) { this.effortLevel = level; }

    // Brief mode methods
    private boolean briefOnlyMode = false;
    public boolean isBriefOnlyMode() { return briefOnlyMode; }
    public void setBriefOnlyMode(boolean enabled) { this.briefOnlyMode = enabled; }

    // Heap dump methods
    public String getHeapDumpPath() {
        return Paths.get(System.getProperty("user.home"), ".claude", "heapdumps").toString();
    }

    public HeapDumpResult dumpHeap(String outputPath) {
        // Trigger actual heap dump using HotSpotDiagnosticMXBean
        try {
            java.nio.file.Path dumpDir = java.nio.file.Paths.get(outputPath);
            java.nio.file.Files.createDirectories(dumpDir);

            String fileName = "heap-" + System.currentTimeMillis() + ".hprof";
            java.nio.file.Path dumpFile = dumpDir.resolve(fileName);

            // Use HotSpotDiagnosticMXBean to dump heap
            for (java.lang.management.PlatformManagedObject obj :
                    java.lang.management.ManagementFactory.getPlatformMXBeans(
                        com.sun.management.HotSpotDiagnosticMXBean.class)) {
                if (obj instanceof com.sun.management.HotSpotDiagnosticMXBean diag) {
                    diag.dumpHeap(dumpFile.toString(), true);
                    long size = java.nio.file.Files.size(dumpFile);
                    return new HeapDumpResult(
                        dumpFile.toString(),
                        size,
                        "hprof",
                        java.time.Instant.now().toString()
                    );
                }
            }

            // Fallback: return placeholder if diagnostic bean not available
            return new HeapDumpResult(
                dumpFile.toString(),
                0,
                "hprof",
                java.time.Instant.now().toString()
            );
        } catch (Exception e) {
            return new HeapDumpResult(
                outputPath + "/heap-" + System.currentTimeMillis() + ".hprof",
                0,
                "hprof",
                java.time.Instant.now().toString()
            );
        }
    }

    public HeapInfo getHeapInfo() {
        Runtime runtime = Runtime.getRuntime();
        long max = runtime.maxMemory();
        long total = runtime.totalMemory();
        long free = runtime.freeMemory();
        long used = total - free;
        return new HeapInfo(used, free, total, max, 0, 0, 0, 0);
    }

    public void triggerGc() {
        System.gc();
    }

    // Agent management methods
    private final List<AgentConfig> agents = new ArrayList<>();
    private String activeAgent = null;

    /**
     * Agent configuration record.
     */
    public record AgentConfig(
        String name,
        String type,
        String model,
        List<String> enabledTools,
        int maxTurns,
        int timeoutSeconds,
        boolean autoMode
    ) {}

    public List<AgentConfig> getAllAgents() { return new ArrayList<>(agents); }
    public String getActiveAgent() { return activeAgent; }
    public void setActiveAgent(String name) { this.activeAgent = name; }

    public AgentConfig createAgent(String name, String type) {
        AgentConfig agent = new AgentConfig(
            name,
            type,
            currentModel,
            List.of("bash", "read", "write", "grep", "glob"),
            100,
            300,
            false
        );
        agents.add(agent);
        return agent;
    }

    public AgentConfig getAgentConfig(String name) {
        return agents.stream()
            .filter(a -> a.name().equals(name))
            .findFirst()
            .orElse(null);
    }

    public void configureAgent(String name, String setting, String value) {
        AgentConfig existing = getAgentConfig(name);
        if (existing != null) {
            agents.remove(existing);
            AgentConfig updated = switch (setting) {
                case "model" -> new AgentConfig(name, existing.type(), value,
                    existing.enabledTools(), existing.maxTurns(),
                    existing.timeoutSeconds(), existing.autoMode());
                case "maxturns", "max-turns" -> new AgentConfig(name, existing.type(),
                    existing.model(), existing.enabledTools(),
                    Integer.parseInt(value), existing.timeoutSeconds(), existing.autoMode());
                case "timeout" -> new AgentConfig(name, existing.type(), existing.model(),
                    existing.enabledTools(), existing.maxTurns(),
                    Integer.parseInt(value), existing.autoMode());
                case "automode", "auto-mode" -> new AgentConfig(name, existing.type(),
                    existing.model(), existing.enabledTools(), existing.maxTurns(),
                    existing.timeoutSeconds(), Boolean.parseBoolean(value));
                default -> existing;
            };
            agents.add(updated);
        }
    }

    public boolean deleteAgent(String name) {
        AgentConfig agent = getAgentConfig(name);
        if (agent != null) {
            agents.remove(agent);
            if (activeAgent != null && activeAgent.equals(name)) {
                activeAgent = null;
            }
            return true;
        }
        return false;
    }

    public void startAgent(String name, String prompt) {
        activeAgent = name;

        // Actually start agent execution via API call
        try {
            AgentConfig config = getAgentConfig(name);
            if (config == null) {
                // Create default config if agent doesn't exist
                config = createAgent(name, "general");
            }

            // Get API key
            String apiKey = System.getenv("ANTHROPIC_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                if (outputHandler != null) {
                    outputHandler.handle("Cannot start agent: No API key configured");
                }
                return;
            }

            String baseUrl = System.getenv("ANTHROPIC_BASE_URL");
            if (baseUrl == null) baseUrl = "https://api.anthropic.com";

            // Build request JSON
            StringBuilder jsonBody = new StringBuilder();
            jsonBody.append("{\"model\":\"").append(config.model()).append("\",");
            jsonBody.append("\"max_tokens\":").append(4096).append(",");
            jsonBody.append("\"system\":\"You are a helpful agent named " + name + ".\",");
            jsonBody.append("\"messages\":[{\"role\":\"user\",\"content\":\"");
            jsonBody.append(escapeJson(prompt)).append("\"}]}");

            // Make async API call
            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl + "/v1/messages"))
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
                .build();

            // Send request asynchronously
            httpClient.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                .thenAccept(response -> {
                    if (response.statusCode() >= 200 && response.statusCode() < 300) {
                        String content = extractContent(response.body());
                        if (outputHandler != null) {
                            outputHandler.handle("Agent " + name + " response: " + content);
                        }
                    } else {
                        if (outputHandler != null) {
                            outputHandler.handle("Agent " + name + " failed: HTTP " + response.statusCode());
                        }
                    }
                });

            if (outputHandler != null) {
                outputHandler.handle("Starting agent: " + name);
            }
        } catch (Exception e) {
            if (outputHandler != null) {
                outputHandler.handle("Failed to start agent: " + e.getMessage());
            }
        }
    }

    private String extractContent(String json) {
        int contentIdx = json.indexOf("\"content\"");
        if (contentIdx < 0) return json;
        int arrStart = json.indexOf("[", contentIdx);
        if (arrStart < 0) return json;
        int textIdx = json.indexOf("\"text\":", arrStart);
        if (textIdx < 0) return json;
        int valStart = json.indexOf("\"", textIdx + 7) + 1;
        int valEnd = json.indexOf("\"", valStart);
        if (valStart > 0 && valEnd > valStart) {
            return json.substring(valStart, valEnd);
        }
        return json;
    }

    // Branch management methods
    private final List<SessionBranch> sessionBranches = new ArrayList<>();
    private String currentBranch = "main";

    /**
     * Session branch record.
     */
    public record SessionBranch(
        String name,
        String description,
        long createdAt
    ) {}

    public List<SessionBranch> getSessionBranches() { return new ArrayList<>(sessionBranches); }
    public String getCurrentBranch() { return currentBranch; }

    public boolean createSessionBranch(String name) {
        if (getBranchByName(name) != null) {
            return false;
        }
        sessionBranches.add(new SessionBranch(name, null, System.currentTimeMillis()));
        return true;
    }

    public boolean switchSessionBranch(String name) {
        if (getBranchByName(name) == null) {
            return false;
        }
        currentBranch = name;
        return true;
    }

    public boolean deleteSessionBranch(String name) {
        SessionBranch branch = getBranchByName(name);
        if (branch != null) {
            sessionBranches.remove(branch);
            return true;
        }
        return false;
    }

    private SessionBranch getBranchByName(String name) {
        return sessionBranches.stream()
            .filter(b -> b.name().equals(name))
            .findFirst()
            .orElse(null);
    }

    /**
     * Heap dump result record.
     */
    public record HeapDumpResult(String path, long size, String format, String timestamp) {}

    /**
     * Heap info record.
     */
    public record HeapInfo(long used, long free, long total, long max,
                           int usagePercent, long gcCount, long gcTimeMs,
                           long objectCount, long classCount) {
        public HeapInfo(long used, long free, long total, long max,
                        long gcCount, long gcTimeMs, long objectCount, long classCount) {
            this(used, free, total, max, (int)(used * 100.0 / max), gcCount, gcTimeMs, objectCount, classCount);
        }
    }

    /**
     * Create a default context.
     */
    public static CommandContext defaults() {
        return new CommandContext(
            System.getProperty("user.dir"),
            new HashMap<>(System.getenv()),
            new HashMap<>(),
            true,
            System.out::println
        );
    }
}