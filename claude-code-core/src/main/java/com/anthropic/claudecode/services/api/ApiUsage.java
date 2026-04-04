/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/api/usage.ts
 */
package com.anthropic.claudecode.services.api;

import java.util.*;
import java.util.concurrent.*;

/**
 * API usage and rate limit types.
 */
public final class ApiUsage {
    private ApiUsage() {}

    /**
     * Rate limit information.
     */
    public record RateLimit(
        Double utilization,  // percentage from 0 to 100
        String resetsAt      // ISO 8601 timestamp
    ) {}

    /**
     * Extra usage information.
     */
    public record ExtraUsage(
        boolean isEnabled,
        Double monthlyLimit,
        Double usedCredits,
        Double utilization
    ) {}

    /**
     * Utilization response from API.
     */
    public record Utilization(
        RateLimit fiveHour,
        RateLimit sevenDay,
        RateLimit sevenDayOauthApps,
        RateLimit sevenDayOpus,
        RateLimit sevenDaySonnet,
        ExtraUsage extraUsage
    ) {}

    /**
     * Fetches utilization data from API.
     */
    public static CompletableFuture<Utilization> fetchUtilization(ApiClient client) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get API key for authentication
                String apiKey = System.getenv("ANTHROPIC_API_KEY");
                if (apiKey == null || apiKey.isEmpty()) {
                    return new Utilization(null, null, null, null, null, null);
                }

                // Get base URL
                String baseUrl = System.getenv("ANTHROPIC_BASE_URL");
                if (baseUrl == null) {
                    baseUrl = "https://api.anthropic.com";
                }

                // Call usage API endpoint
                java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(baseUrl + "/v1/usage"))
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("Content-Type", "application/json")
                    .GET()
                    .build();

                java.net.http.HttpResponse<String> response = httpClient.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return parseUtilizationResponse(response.body());
                } else {
                    // Try OAuth usage endpoint
                    return fetchOAuthUtilization(apiKey, baseUrl);
                }
            } catch (Exception e) {
                return new Utilization(null, null, null, null, null, null);
            }
        });
    }

    /**
     * Fetch OAuth-based utilization.
     */
    private static Utilization fetchOAuthUtilization(String apiKey, String baseUrl) {
        try {
            // Try OAuth usage endpoint
            String oauthUrl = System.getenv("CLAUDE_CODE_API_URL");
            if (oauthUrl == null) {
                oauthUrl = "https://api.claude.ai";
            }

            java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create(oauthUrl + "/api/oauth/usage"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .GET()
                .build();

            java.net.http.HttpResponse<String> response = httpClient.send(request,
                java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseUtilizationResponse(response.body());
            }
        } catch (Exception e) {
            // Ignore OAuth attempt
        }

        return new Utilization(null, null, null, null, null, null);
    }

    /**
     * Parse utilization response JSON.
     */
    private static Utilization parseUtilizationResponse(String json) {
        try {
            RateLimit fiveHour = null;
            RateLimit sevenDay = null;
            RateLimit sevenDayOauthApps = null;
            RateLimit sevenDayOpus = null;
            RateLimit sevenDaySonnet = null;
            ExtraUsage extraUsage = null;

            // Parse five_hour limit
            int fiveHourIdx = json.indexOf("\"five_hour\"");
            if (fiveHourIdx >= 0) {
                fiveHour = parseRateLimit(json, fiveHourIdx);
            }

            // Parse seven_day limit
            int sevenDayIdx = json.indexOf("\"seven_day\"");
            if (sevenDayIdx >= 0) {
                sevenDay = parseRateLimit(json, sevenDayIdx);
            }

            // Parse seven_day_opus limit
            int opusIdx = json.indexOf("\"seven_day_opus\"");
            if (opusIdx >= 0) {
                sevenDayOpus = parseRateLimit(json, opusIdx);
            }

            // Parse seven_day_sonnet limit
            int sonnetIdx = json.indexOf("\"seven_day_sonnet\"");
            if (sonnetIdx >= 0) {
                sevenDaySonnet = parseRateLimit(json, sonnetIdx);
            }

            // Parse extra_usage
            int extraIdx = json.indexOf("\"extra_usage\"");
            if (extraIdx >= 0) {
                extraUsage = parseExtraUsage(json, extraIdx);
            }

            return new Utilization(fiveHour, sevenDay, sevenDayOauthApps, sevenDayOpus, sevenDaySonnet, extraUsage);
        } catch (Exception e) {
            return new Utilization(null, null, null, null, null, null);
        }
    }

    /**
     * Parse rate limit object.
     */
    private static RateLimit parseRateLimit(String json, int startIdx) {
        try {
            int objStart = json.indexOf("{", startIdx);
            if (objStart < 0) return null;

            int depth = 1;
            int objEnd = objStart + 1;
            while (objEnd < json.length() && depth > 0) {
                char c = json.charAt(objEnd);
                if (c == '{') depth++;
                else if (c == '}') depth--;
                objEnd++;
            }

            String obj = json.substring(objStart, objEnd);

            Double utilization = null;
            String resetsAt = null;

            int utilIdx = obj.indexOf("\"utilization\"");
            if (utilIdx >= 0) {
                int valStart = obj.indexOf(":", utilIdx) + 1;
                while (valStart < obj.length() && Character.isWhitespace(obj.charAt(valStart))) valStart++;
                int valEnd = valStart;
                while (valEnd < obj.length() && (Character.isDigit(obj.charAt(valEnd)) || obj.charAt(valEnd) == '.')) valEnd++;
                utilization = Double.parseDouble(obj.substring(valStart, valEnd));
            }

            int resetIdx = obj.indexOf("\"resets_at\"");
            if (resetIdx >= 0) {
                int valStart = obj.indexOf("\"", resetIdx + 11) + 1;
                int valEnd = obj.indexOf("\"", valStart);
                if (valStart > 0 && valEnd > valStart) {
                    resetsAt = obj.substring(valStart, valEnd);
                }
            }

            return new RateLimit(utilization, resetsAt);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse extra usage object.
     */
    private static ExtraUsage parseExtraUsage(String json, int startIdx) {
        try {
            int objStart = json.indexOf("{", startIdx);
            if (objStart < 0) return null;

            int depth = 1;
            int objEnd = objStart + 1;
            while (objEnd < json.length() && depth > 0) {
                char c = json.charAt(objEnd);
                if (c == '{') depth++;
                else if (c == '}') depth--;
                objEnd++;
            }

            String obj = json.substring(objStart, objEnd);

            boolean isEnabled = obj.contains("\"is_enabled\":true");
            Double monthlyLimit = extractDouble(obj, "monthly_limit");
            Double usedCredits = extractDouble(obj, "used_credits");
            Double utilization = extractDouble(obj, "utilization");

            return new ExtraUsage(isEnabled, monthlyLimit, usedCredits, utilization);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract double value from JSON object.
     */
    private static Double extractDouble(String json, String key) {
        int idx = json.indexOf("\"" + key + "\"");
        if (idx < 0) return null;

        int valStart = json.indexOf(":", idx) + 1;
        while (valStart < json.length() && Character.isWhitespace(json.charAt(valStart))) valStart++;
        int valEnd = valStart;
        while (valEnd < json.length() && (Character.isDigit(json.charAt(valEnd)) || json.charAt(valEnd) == '.' || json.charAt(valEnd) == '-')) valEnd++;

        try {
            return Double.parseDouble(json.substring(valStart, valEnd));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if any rate limit is approaching threshold.
     */
    public static boolean isApproachingLimit(Utilization utilization, double threshold) {
        if (utilization == null) return false;

        return checkRateLimit(utilization.fiveHour(), threshold) ||
               checkRateLimit(utilization.sevenDay(), threshold) ||
               checkRateLimit(utilization.sevenDayOpus(), threshold) ||
               checkRateLimit(utilization.sevenDaySonnet(), threshold);
    }

    private static boolean checkRateLimit(RateLimit limit, double threshold) {
        if (limit == null || limit.utilization() == null) {
            return false;
        }
        return limit.utilization() >= threshold;
    }

    /**
     * Get the most restrictive rate limit.
     */
    public static RateLimit getMostRestrictiveLimit(Utilization utilization) {
        if (utilization == null) return null;

        List<RateLimit> limits = new ArrayList<>();
        if (utilization.fiveHour() != null) limits.add(utilization.fiveHour());
        if (utilization.sevenDay() != null) limits.add(utilization.sevenDay());
        if (utilization.sevenDayOpus() != null) limits.add(utilization.sevenDayOpus());
        if (utilization.sevenDaySonnet() != null) limits.add(utilization.sevenDaySonnet());

        return limits.stream()
            .filter(l -> l.utilization() != null)
            .max(Comparator.comparingDouble(RateLimit::utilization))
            .orElse(null);
    }

    /**
     * Format utilization as human-readable string.
     */
    public static String formatUtilization(Utilization utilization) {
        if (utilization == null) {
            return "No usage data available";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Claude AI Usage:\n");

        if (utilization.sevenDay() != null) {
            sb.append(formatRateLimit("Weekly", utilization.sevenDay()));
        }
        if (utilization.sevenDayOpus() != null) {
            sb.append(formatRateLimit("Opus Weekly", utilization.sevenDayOpus()));
        }
        if (utilization.sevenDaySonnet() != null) {
            sb.append(formatRateLimit("Sonnet Weekly", utilization.sevenDaySonnet()));
        }
        if (utilization.fiveHour() != null) {
            sb.append(formatRateLimit("Session", utilization.fiveHour()));
        }
        if (utilization.extraUsage() != null && utilization.extraUsage().isEnabled()) {
            sb.append(formatExtraUsage(utilization.extraUsage()));
        }

        return sb.toString();
    }

    private static String formatRateLimit(String name, RateLimit limit) {
        StringBuilder sb = new StringBuilder();
        sb.append("  ").append(name).append(": ");
        if (limit.utilization() != null) {
            sb.append(String.format("%.0f%% used", limit.utilization()));
        } else {
            sb.append("unknown");
        }
        if (limit.resetsAt() != null) {
            sb.append(" · resets ").append(limit.resetsAt());
        }
        sb.append("\n");
        return sb.toString();
    }

    private static String formatExtraUsage(ExtraUsage usage) {
        StringBuilder sb = new StringBuilder();
        sb.append("  Extra Usage: ");
        if (usage.utilization() != null) {
            sb.append(String.format("%.0f%% used", usage.utilization()));
        }
        if (usage.monthlyLimit() != null) {
            sb.append(String.format(" of $%.0f limit", usage.monthlyLimit()));
        }
        sb.append("\n");
        return sb.toString();
    }
}