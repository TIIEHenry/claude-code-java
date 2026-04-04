/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/hooks/skillImprovement.ts
 */
package com.anthropic.claudecode.utils.hooks;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import java.util.function.*;

/**
 * Skill improvement detection and application.
 */
public final class SkillImprovement {
    private SkillImprovement() {}

    private static final int TURN_BATCH_SIZE = 5;
    private static volatile int lastAnalyzedCount = 0;
    private static volatile int lastAnalyzedIndex = 0;
    private static volatile boolean featureEnabled = false;

    /**
     * Skill update suggestion.
     */
    public record SkillUpdate(
        String section,
        String change,
        String reason
    ) {}

    /**
     * Skill improvement suggestion.
     */
    public record SkillImprovementSuggestion(
        String skillName,
        List<SkillUpdate> updates
    ) {}

    /**
     * Initialize skill improvement hook.
     */
    public static void initSkillImprovement() {
        if (featureEnabled) {
            PostSamplingHooks.registerPostSamplingHook(createSkillImprovementHook());
        }
    }

    /**
     * Set feature enabled flag.
     */
    public static void setFeatureEnabled(boolean enabled) {
        featureEnabled = enabled;
    }

    private static PostSamplingHooks.PostSamplingHook createSkillImprovementHook() {
        return context -> CompletableFuture.runAsync(() -> {
            try {
                // Check if should run
                if (!"repl_main_thread".equals(context.querySource())) {
                    return;
                }

                // Find project skill
                String projectSkillContent = findProjectSkillContent();
                if (projectSkillContent == null) {
                    return;
                }

                // Only run every TURN_BATCH_SIZE user messages
                int userCount = countUserMessages(context.messages());
                if (userCount - lastAnalyzedCount < TURN_BATCH_SIZE) {
                    return;
                }

                lastAnalyzedCount = userCount;

                // Build messages
                List<Map<String, Object>> newMessages = context.messages()
                    .subList(lastAnalyzedIndex, context.messages().size());
                lastAnalyzedIndex = context.messages().size();

                String messagesStr = formatRecentMessages(newMessages);

                // Create query message
                String queryContent = "You are analyzing a conversation where a user is executing a skill.\n" +
                    "Your job: identify if the user's recent messages contain preferences, requests, or corrections " +
                    "that should be permanently added to the skill definition for future runs.\n\n" +
                    "<skill_definition>\n" + projectSkillContent + "\n</skill_definition>\n\n" +
                    "<recent_messages>\n" + messagesStr + "\n</recent_messages>\n\n" +
                    "Look for:\n" +
                    "- Requests to add, change, or remove steps\n" +
                    "- Preferences about how steps should work\n" +
                    "- Corrections\n\n" +
                    "Ignore:\n" +
                    "- Routine conversation that doesn't generalize\n" +
                    "- Things the skill already does\n\n" +
                    "Output a JSON array inside <updates> tags. Each item: " +
                    "{\"section\": \"which step\", \"change\": \"what to add\", \"reason\": \"which user message prompted this\"}.\n" +
                    "Output <updates>[]</updates> if no updates are needed.";

                // Query model (placeholder)
                String responseContent = queryModel(queryContent);

                // Parse response
                List<SkillUpdate> updates = parseResponse(responseContent);

                if (!updates.isEmpty()) {
                    String skillName = findProjectSkillName();

                    logEvent("tengu_skill_improvement_detected", Map.of(
                        "updateCount", updates.size(),
                        "skillName", skillName
                    ));

                    // Would update app state with suggestion
                    // context.toolUseContext().setAppState(...)
                }
            } catch (Exception e) {
                logError(e);
            }
        });
    }

    /**
     * Apply skill improvements by rewriting the skill file.
     */
    public static CompletableFuture<Void> applySkillImprovement(
            String skillName,
            List<SkillUpdate> updates) {

        return CompletableFuture.runAsync(() -> {
            if (skillName == null || skillName.isEmpty()) return;

            try {
                // Skills live at .claude/skills/<name>/SKILL.md relative to CWD
                String cwd = System.getProperty("user.dir");
                Path filePath = Paths.get(cwd, ".claude", "skills", skillName, "SKILL.md");

                if (!Files.exists(filePath)) {
                    logError(new Exception("Failed to read skill file for improvement: " + filePath));
                    return;
                }

                String currentContent = Files.readString(filePath);

                StringBuilder updateList = new StringBuilder();
                for (SkillUpdate u : updates) {
                    updateList.append("- ").append(u.section()).append(": ").append(u.change()).append("\n");
                }

                String queryContent = "You are editing a skill definition file. Apply the following improvements.\n\n" +
                    "<current_skill_file>\n" + currentContent + "\n</current_skill_file>\n\n" +
                    "<improvements>\n" + updateList + "\n</improvements>\n\n" +
                    "Rules:\n" +
                    "- Integrate the improvements naturally\n" +
                    "- Preserve frontmatter (--- block) exactly as-is\n" +
                    "- Preserve the overall format and style\n" +
                    "- Output the complete updated file inside <updated_file> tags";

                String responseContent = queryModel(queryContent);
                String updatedContent = extractTag(responseContent, "updated_file");

                if (updatedContent == null) {
                    logError(new Exception("Skill improvement apply: no updated_file tag in response"));
                    return;
                }

                Files.writeString(filePath, updatedContent);

            } catch (Exception e) {
                logError(e);
            }
        });
    }

    private static String formatRecentMessages(List<Map<String, Object>> messages) {
        StringBuilder sb = new StringBuilder();
        for (Map<String, Object> m : messages) {
            String type = (String) m.get("type");
            if (type == null) type = (String) m.get("role");

            if ("user".equals(type) || "assistant".equals(type)) {
                String role = "user".equals(type) ? "User" : "Assistant";
                Object content = m.get("content");

                String text;
                if (content instanceof String s) {
                    text = s.length() > 500 ? s.substring(0, 500) : s;
                } else if (content instanceof List<?> list) {
                    StringBuilder contentSb = new StringBuilder();
                    for (Object item : list) {
                        if (item instanceof Map<?, ?> map &&
                            "text".equals(map.get("type"))) {
                            contentSb.append(map.get("text"));
                        }
                    }
                    text = contentSb.toString();
                    if (text.length() > 500) text = text.substring(0, 500);
                } else {
                    text = "";
                }

                sb.append(role).append(": ").append(text).append("\n\n");
            }
        }
        return sb.toString();
    }

    private static int countUserMessages(List<Map<String, Object>> messages) {
        int count = 0;
        for (Map<String, Object> m : messages) {
            String type = (String) m.get("type");
            if (type == null) type = (String) m.get("role");
            if ("user".equals(type)) count++;
        }
        return count;
    }

    private static String findProjectSkillContent() {
        // Read from .claude/skills directory in current working directory
        try {
            String cwd = System.getProperty("user.dir");
            Path skillsDir = Paths.get(cwd, ".claude", "skills");

            if (!Files.exists(skillsDir) || !Files.isDirectory(skillsDir)) {
                return null;
            }

            // Find first skill with SKILL.md file
            try (var stream = Files.list(skillsDir)) {
                for (Path skillPath : stream.toList()) {
                    if (Files.isDirectory(skillPath)) {
                        Path skillFile = skillPath.resolve("SKILL.md");
                        if (Files.exists(skillFile)) {
                            return Files.readString(skillFile);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logError(e);
        }
        return null;
    }

    private static String findProjectSkillName() {
        // Read skill name from .claude/skills directory
        try {
            String cwd = System.getProperty("user.dir");
            Path skillsDir = Paths.get(cwd, ".claude", "skills");

            if (!Files.exists(skillsDir) || !Files.isDirectory(skillsDir)) {
                return "unknown";
            }

            // Find first skill directory
            try (var stream = Files.list(skillsDir)) {
                for (Path skillPath : stream.toList()) {
                    if (Files.isDirectory(skillPath)) {
                        Path skillFile = skillPath.resolve("SKILL.md");
                        if (Files.exists(skillFile)) {
                            return skillPath.getFileName().toString();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logError(e);
        }
        return "unknown";
    }

    private static List<SkillUpdate> parseResponse(String content) {
        String updatesStr = extractTag(content, "updates");
        if (updatesStr == null) {
            return new ArrayList<>();
        }

        try {
            org.json.JSONArray arr = new org.json.JSONArray(updatesStr);
            List<SkillUpdate> updates = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject obj = arr.getJSONObject(i);
                updates.add(new SkillUpdate(
                    obj.optString("section", ""),
                    obj.optString("change", ""),
                    obj.optString("reason", "")
                ));
            }
            return updates;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static String extractTag(String content, String tag) {
        Pattern pattern = Pattern.compile("<" + tag + ">(.*?)</" + tag + ">", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private static String queryModel(String content) {
        // Make actual API call to Claude
        try {
            String apiKey = System.getenv("ANTHROPIC_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                return "<updates>[]</updates>";
            }

            String baseUrl = System.getenv("ANTHROPIC_BASE_URL");
            if (baseUrl == null) baseUrl = "https://api.anthropic.com";

            // Get model from environment or default to haiku for fast analysis
            String model = System.getenv("CLAUDE_CODE_SKILL_IMPROVEMENT_MODEL");
            if (model == null) model = "claude-haiku-4-5-20251001";

            // Build request JSON - single user message with the query content
            StringBuilder sb = new StringBuilder();
            sb.append("{\"model\":\"").append(model).append("\",");
            sb.append("\"max_tokens\":1024,");
            sb.append("\"messages\":[");
            sb.append("{\"role\":\"user\",\"content\":\"").append(escapeJson(content)).append("\"}");
            sb.append("]}");

            // Make HTTP request
            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl + "/v1/messages"))
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(sb.toString()))
                .build();

            java.net.http.HttpResponse<String> response = httpClient.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Extract content from response
                return extractContent(response.body());
            }
        } catch (Exception e) {
            logError(e);
        }
        return "<updates>[]</updates>";
    }

    /**
     * Extract content from API response JSON.
     */
    private static String extractContent(String json) {
        int contentIdx = json.indexOf("\"content\":");
        if (contentIdx < 0) return "<updates>[]</updates>";

        int arrStart = json.indexOf("[", contentIdx);
        if (arrStart < 0) return "<updates>[]</updates>";

        // Find first text block
        int textIdx = json.indexOf("\"text\":", arrStart);
        if (textIdx < 0) return "<updates>[]</updates>";

        int valStart = json.indexOf("\"", textIdx + 7) + 1;
        int valEnd = json.indexOf("\"", valStart);

        if (valStart > 0 && valEnd > valStart) {
            return unescapeJson(json.substring(valStart, valEnd));
        }
        return "<updates>[]</updates>";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String unescapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t");
    }

    private static void logEvent(String eventName, Map<String, Object> metadata) {
        if (System.getenv("CLAUDE_CODE_DEBUG") != null) {
            System.err.println("[analytics] " + eventName + ": " + metadata);
        }
    }

    private static void logError(Exception e) {
        System.err.println("[skill-improvement] Error: " + e.getMessage());
    }
}