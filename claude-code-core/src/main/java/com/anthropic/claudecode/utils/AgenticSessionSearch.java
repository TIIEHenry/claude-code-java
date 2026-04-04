/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code agentic session search
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Agentic session search using semantic understanding.
 */
public final class AgenticSessionSearch {
    private AgenticSessionSearch() {}

    // Limits for transcript extraction
    private static final int MAX_TRANSCRIPT_CHARS = 2000;
    private static final int MAX_MESSAGES_TO_SCAN = 100;
    private static final int MAX_SESSIONS_TO_SEARCH = 100;

    private static final String SESSION_SEARCH_SYSTEM_PROMPT = """
Your goal is to find relevant sessions based on a user's search query.

You will be given a list of sessions with their metadata and a search query. Identify which sessions are most relevant to the query.

Each session may include:
- Title (display name or custom title)
- Tag (user-assigned category, shown as [tag: name])
- Branch (git branch name, shown as [branch: name])
- Summary (AI-generated summary)
- First message (beginning of the conversation)
- Transcript (excerpt of conversation content)

IMPORTANT: Tags are user-assigned labels. If the query matches a tag, those sessions should be highly prioritized.

For each session, consider (in order of priority):
1. Exact tag matches (highest priority)
2. Partial tag matches or tag-related terms
3. Title matches
4. Branch name matches
5. Summary and transcript content matches
6. Semantic similarity

CRITICAL: Be VERY inclusive in your matching.

Return sessions ordered by relevance (most relevant first).

Respond with ONLY the JSON object, no markdown formatting:
{"relevant_indices": [2, 5, 0]}""";

    /**
     * Session log for search.
     */
    public record SessionLog(
            String id,
            String displayTitle,
            String customTitle,
            String tag,
            String gitBranch,
            String summary,
            String firstPrompt,
            List<SessionMessage> messages
    ) {}

    /**
     * Session message.
     */
    public record SessionMessage(String type, Object content) {}

    /**
     * Search result.
     */
    public record SearchResult(List<Integer> relevantIndices) {}

    /**
     * Performs agentic search to find relevant sessions.
     */
    public static CompletableFuture<List<SessionLog>> agenticSessionSearch(
            String query,
            List<SessionLog> logs,
            CompletableFuture<Void> signal) {

        if (query == null || query.trim().isEmpty() || logs.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        String queryLower = query.toLowerCase();

        // Pre-filter: find sessions that contain the query term
        List<SessionLog> matchingLogs = logs.stream()
                .filter(log -> logContainsQuery(log, queryLower))
                .toList();

        // Take up to MAX_SESSIONS_TO_SEARCH matching logs
        List<SessionLog> logsToSearch;
        if (matchingLogs.size() >= MAX_SESSIONS_TO_SEARCH) {
            logsToSearch = matchingLogs.subList(0, MAX_SESSIONS_TO_SEARCH);
        } else {
            List<SessionLog> nonMatchingLogs = logs.stream()
                    .filter(log -> !logContainsQuery(log, queryLower))
                    .toList();
            int remainingSlots = MAX_SESSIONS_TO_SEARCH - matchingLogs.size();
            logsToSearch = new ArrayList<>();
            logsToSearch.addAll(matchingLogs);
            logsToSearch.addAll(nonMatchingLogs.subList(0, Math.min(remainingSlots, nonMatchingLogs.size())));
        }

        // Build session list for the prompt
        String sessionList = buildSessionList(logsToSearch);

        String userMessage = "Sessions:\n" + sessionList + "\n\nSearch query: \"" + query + "\"\n\nFind the sessions that are most relevant to this query.";

        // For semantic search, we can use API-based search when available
        // Otherwise, use text matching with ranking
        return performSemanticSearch(query, logsToSearch, matchingLogs, userMessage, signal);
    }

    /**
     * Perform semantic search using API when available.
     */
    private static CompletableFuture<List<SessionLog>> performSemanticSearch(
            String query,
            List<SessionLog> logsToSearch,
            List<SessionLog> textMatches,
            String userMessage,
            CompletableFuture<Void> signal) {

        return CompletableFuture.supplyAsync(() -> {
            // Try API-based semantic search
            try {
                // Check if semantic search API is configured
                String apiKey = System.getenv("ANTHROPIC_API_KEY");
                if (apiKey != null && !apiKey.isEmpty()) {
                    // Use Claude API for semantic ranking
                    String semanticResult = callSemanticSearchAPI(query, logsToSearch, apiKey);
                    if (semanticResult != null) {
                        return parseAndRankResults(semanticResult, logsToSearch);
                    }
                }
            } catch (Exception e) {
                // Fall back to text matching
            }

            // Return text-based matches ranked by relevance
            return rankByTextRelevance(query, textMatches);
        });
    }

    /**
     * Call API for semantic search ranking.
     */
    private static String callSemanticSearchAPI(String query, List<SessionLog> logs, String apiKey) {
        try {
            String baseUrl = System.getenv("ANTHROPIC_BASE_URL");
            if (baseUrl == null) baseUrl = "https://api.anthropic.com";

            // Build request for semantic ranking
            String sessionList = buildSessionList(logs);

            org.json.JSONObject request = new org.json.JSONObject();
            request.put("model", "claude-haiku-4-5");
            request.put("max_tokens", 200);

            org.json.JSONObject systemObj = new org.json.JSONObject();
            systemObj.put("type", "text");
            systemObj.put("text", SESSION_SEARCH_SYSTEM_PROMPT);
            request.put("system", SESSION_SEARCH_SYSTEM_PROMPT);

            org.json.JSONArray messages = new org.json.JSONArray();
            org.json.JSONObject userMsg = new org.json.JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", "Sessions:\n" + sessionList + "\n\nSearch query: \"" + query + "\"");
            messages.put(userMsg);
            request.put("messages", messages);

            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest httpRequest = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(baseUrl + "/v1/messages"))
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .header("Content-Type", "application/json")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(request.toString()))
                .build();

            java.net.http.HttpResponse<String> response = httpClient.send(httpRequest,
                java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                org.json.JSONObject resp = new org.json.JSONObject(response.body());
                org.json.JSONArray content = resp.getJSONArray("content");
                if (content.length() > 0) {
                    org.json.JSONObject first = content.getJSONObject(0);
                    return first.getString("text");
                }
            }
        } catch (Exception e) {
            // API call failed
        }
        return null;
    }

    /**
     * Parse API response and return ranked sessions.
     */
    private static List<SessionLog> parseAndRankResults(String response, List<SessionLog> logs) {
        try {
            // Parse JSON response
            org.json.JSONObject json = new org.json.JSONObject(response.trim());
            org.json.JSONArray indices = json.getJSONArray("relevant_indices");

            List<SessionLog> results = new ArrayList<>();
            for (int i = 0; i < indices.length(); i++) {
                int idx = indices.getInt(i);
                if (idx >= 0 && idx < logs.size()) {
                    results.add(logs.get(idx));
                }
            }

            return results;
        } catch (Exception e) {
            // Parse error, return original order
            return logs;
        }
    }

    /**
     * Rank sessions by text relevance.
     */
    private static List<SessionLog> rankByTextRelevance(String query, List<SessionLog> matches) {
        String queryLower = query.toLowerCase();

        // Score each match
        return matches.stream()
            .sorted((a, b) -> {
                int scoreA = scoreRelevance(a, queryLower);
                int scoreB = scoreRelevance(b, queryLower);
                return Integer.compare(scoreB, scoreA); // Higher score first
            })
            .collect(Collectors.toList());
    }

    /**
     * Score session relevance.
     */
    private static int scoreRelevance(SessionLog log, String queryLower) {
        int score = 0;

        // Exact tag match = highest priority
        if (log.tag() != null && log.tag().toLowerCase().equals(queryLower)) {
            score += 1000;
        }

        // Partial tag match
        if (log.tag() != null && log.tag().toLowerCase().contains(queryLower)) {
            score += 500;
        }

        // Title match
        if (log.displayTitle() != null && log.displayTitle().toLowerCase().contains(queryLower)) {
            score += 200;
        }
        if (log.customTitle() != null && log.customTitle().toLowerCase().contains(queryLower)) {
            score += 200;
        }

        // Branch match
        if (log.gitBranch() != null && log.gitBranch().toLowerCase().contains(queryLower)) {
            score += 100;
        }

        // Summary match
        if (log.summary() != null && log.summary().toLowerCase().contains(queryLower)) {
            score += 50;
        }

        // First prompt match
        if (log.firstPrompt() != null && log.firstPrompt().toLowerCase().contains(queryLower)) {
            score += 30;
        }

        return score;
    }

    /**
     * Checks if a log contains the query term.
     */
    private static boolean logContainsQuery(SessionLog log, String queryLower) {
        // Check title
        if (log.displayTitle() != null && log.displayTitle().toLowerCase().contains(queryLower)) {
            return true;
        }

        // Check custom title
        if (log.customTitle() != null && log.customTitle().toLowerCase().contains(queryLower)) {
            return true;
        }

        // Check tag
        if (log.tag() != null && log.tag().toLowerCase().contains(queryLower)) {
            return true;
        }

        // Check branch
        if (log.gitBranch() != null && log.gitBranch().toLowerCase().contains(queryLower)) {
            return true;
        }

        // Check summary
        if (log.summary() != null && log.summary().toLowerCase().contains(queryLower)) {
            return true;
        }

        // Check first prompt
        if (log.firstPrompt() != null && log.firstPrompt().toLowerCase().contains(queryLower)) {
            return true;
        }

        // Check transcript
        if (log.messages() != null && !log.messages().isEmpty()) {
            String transcript = extractTranscript(log.messages()).toLowerCase();
            if (transcript.contains(queryLower)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Extracts a truncated transcript from session messages.
     */
    private static String extractTranscript(List<SessionMessage> messages) {
        if (messages.isEmpty()) return "";

        // Take messages from start and end
        List<SessionMessage> messagesToScan;
        if (messages.size() <= MAX_MESSAGES_TO_SCAN) {
            messagesToScan = messages;
        } else {
            int half = MAX_MESSAGES_TO_SCAN / 2;
            messagesToScan = new ArrayList<>();
            messagesToScan.addAll(messages.subList(0, half));
            messagesToScan.addAll(messages.subList(messages.size() - half, messages.size()));
        }

        String text = messagesToScan.stream()
                .map(AgenticSessionSearch::extractMessageText)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" "))
                .replaceAll("\\s+", " ")
                .trim();

        if (text.length() > MAX_TRANSCRIPT_CHARS) {
            return text.substring(0, MAX_TRANSCRIPT_CHARS) + "…";
        }
        return text;
    }

    /**
     * Extracts searchable text from a message.
     */
    private static String extractMessageText(SessionMessage message) {
        if (!"user".equals(message.type()) && !"assistant".equals(message.type())) {
            return "";
        }

        Object content = message.content();
        if (content == null) return "";

        if (content instanceof String) {
            return (String) content;
        }

        if (content instanceof List) {
            return ((List<?>) content).stream()
                    .map(block -> {
                        if (block instanceof String) return (String) block;
                        if (block instanceof Map) {
                            Object text = ((Map<?, ?>) block).get("text");
                            if (text instanceof String) return (String) text;
                        }
                        return "";
                    })
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.joining(" "));
        }

        return "";
    }

    /**
     * Builds session list string for the prompt.
     */
    private static String buildSessionList(List<SessionLog> logs) {
        return logs.stream()
                .map(log -> {
                    List<String> parts = new ArrayList<>();
                    parts.add(log.displayTitle() != null ? log.displayTitle() : "Untitled");

                    if (log.customTitle() != null) {
                        parts.add("[custom title: " + log.customTitle() + "]");
                    }
                    if (log.tag() != null) {
                        parts.add("[tag: " + log.tag() + "]");
                    }
                    if (log.gitBranch() != null) {
                        parts.add("[branch: " + log.gitBranch() + "]");
                    }
                    if (log.summary() != null) {
                        parts.add("- Summary: " + log.summary());
                    }
                    if (log.firstPrompt() != null && !"No prompt".equals(log.firstPrompt())) {
                        String truncated = log.firstPrompt().length() > 300
                                ? log.firstPrompt().substring(0, 300)
                                : log.firstPrompt();
                        parts.add("- First message: " + truncated);
                    }
                    if (log.messages() != null && !log.messages().isEmpty()) {
                        String transcript = extractTranscript(log.messages());
                        if (!transcript.isEmpty()) {
                            parts.add("- Transcript: " + transcript);
                        }
                    }

                    return parts.stream().collect(Collectors.joining(" "));
                })
                .collect(Collectors.joining("\n"));
    }
}