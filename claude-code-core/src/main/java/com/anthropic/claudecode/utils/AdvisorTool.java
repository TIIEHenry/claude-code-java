/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/advisor.ts
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;

/**
 * Advisor tool integration - provides access to a stronger reviewer model.
 *
 * The advisor tool is backed by a stronger reviewer model that sees the entire
 * conversation history, all tool calls, and all results.
 */
public final class AdvisorTool {
    private AdvisorTool() {}

    /**
     * Advisor server tool use block.
     */
    public record AdvisorServerToolUseBlock(
        String type,
        String id,
        String name,
        Map<String, Object> input
    ) implements AdvisorBlock {
        public AdvisorServerToolUseBlock(String id, Map<String, Object> input) {
            this("server_tool_use", id, "advisor", input);
        }
    }

    /**
     * Advisor tool result block.
     */
    public sealed interface AdvisorToolResultBlock extends AdvisorBlock permits
        AdvisorToolResultBlock.AdvisorResult,
        AdvisorToolResultBlock.AdvisorRedactedResult,
        AdvisorToolResultBlock.AdvisorToolResultError {

        String type();
        String toolUseId();

        record AdvisorResult(
            String type,
            String toolUseId,
            String text
        ) implements AdvisorToolResultBlock {
            public AdvisorResult(String toolUseId, String text) {
                this("advisor_tool_result", toolUseId, text);
            }
        }

        record AdvisorRedactedResult(
            String type,
            String toolUseId,
            String encryptedContent
        ) implements AdvisorToolResultBlock {
            public AdvisorRedactedResult(String toolUseId, String encryptedContent) {
                this("advisor_tool_result", toolUseId, encryptedContent);
            }
        }

        record AdvisorToolResultError(
            String type,
            String toolUseId,
            String errorCode
        ) implements AdvisorToolResultBlock {
            public AdvisorToolResultError(String toolUseId, String errorCode) {
                this("advisor_tool_result", toolUseId, errorCode);
            }
        }
    }

    /**
     * Advisor block type.
     */
    public sealed interface AdvisorBlock permits
        AdvisorServerToolUseBlock,
        AdvisorToolResultBlock {}

    /**
     * Check if a block is an advisor block.
     */
    public static boolean isAdvisorBlock(Map<String, Object> param) {
        String type = (String) param.get("type");
        if ("advisor_tool_result".equals(type)) {
            return true;
        }
        if ("server_tool_use".equals(type)) {
            String name = (String) param.get("name");
            return "advisor".equals(name);
        }
        return false;
    }

    /**
     * Advisor configuration.
     */
    public record AdvisorConfig(
        boolean enabled,
        boolean canUserConfigure,
        String baseModel,
        String advisorModel
    ) {}

    // Cached config
    private static volatile AdvisorConfig cachedConfig = null;

    /**
     * Get advisor configuration.
     */
    public static AdvisorConfig getAdvisorConfig() {
        if (cachedConfig != null) {
            return cachedConfig;
        }
        // Default configuration - in production this would come from GrowthBook
        cachedConfig = new AdvisorConfig(false, false, null, null);
        return cachedConfig;
    }

    /**
     * Check if advisor is enabled.
     */
    public static boolean isAdvisorEnabled() {
        if (EnvUtils.isEnvTruthy("CLAUDE_CODE_DISABLE_ADVISOR_TOOL")) {
            return false;
        }
        // First-party only beta header check
        if (!shouldIncludeFirstPartyOnlyBetas()) {
            return false;
        }
        return getAdvisorConfig().enabled();
    }

    /**
     * Check if user can configure advisor.
     */
    public static boolean canUserConfigureAdvisor() {
        return isAdvisorEnabled() && getAdvisorConfig().canUserConfigure();
    }

    /**
     * Get experiment advisor models.
     */
    public static Optional<AdvisorModels> getExperimentAdvisorModels() {
        AdvisorConfig config = getAdvisorConfig();
        if (isAdvisorEnabled() &&
            !canUserConfigureAdvisor() &&
            config.baseModel() != null &&
            config.advisorModel() != null) {
            return Optional.of(new AdvisorModels(config.baseModel(), config.advisorModel()));
        }
        return Optional.empty();
    }

    /**
     * Advisor models pair.
     */
    public record AdvisorModels(String baseModel, String advisorModel) {}

    /**
     * Check if model supports advisor tool.
     */
    public static boolean modelSupportsAdvisor(String model) {
        if (model == null) return false;
        String m = model.toLowerCase();
        return m.contains("opus-4-6") ||
               m.contains("sonnet-4-6") ||
               "ant".equals(System.getenv("USER_TYPE"));
    }

    /**
     * Check if model is valid advisor model.
     */
    public static boolean isValidAdvisorModel(String model) {
        if (model == null) return false;
        String m = model.toLowerCase();
        return m.contains("opus-4-6") ||
               m.contains("sonnet-4-6") ||
               "ant".equals(System.getenv("USER_TYPE"));
    }

    /**
     * Get initial advisor setting.
     */
    public static Optional<String> getInitialAdvisorSetting() {
        if (!isAdvisorEnabled()) {
            return Optional.empty();
        }
        // In production, this would come from initial settings
        return Optional.empty();
    }

    /**
     * Check if should include first-party only betas.
     */
    private static boolean shouldIncludeFirstPartyOnlyBetas() {
        // In production, this would check actual beta configuration
        return "ant".equals(System.getenv("USER_TYPE"));
    }

    /**
     * Advisor tool instructions.
     */
    public static final String ADVISOR_TOOL_INSTRUCTIONS = """
# Advisor Tool

You have access to an `advisor` tool backed by a stronger reviewer model. It takes NO parameters -- when you call it, your entire conversation history is automatically forwarded. The advisor sees the task, every tool call you've made, every result you've seen.

Call advisor BEFORE substantive work -- before writing code, before committing to an interpretation, before building on an assumption. If the task requires orientation first (finding files, reading code, seeing what's there), do that, then call advisor. Orientation is not substantive work. Writing, editing, and declaring an answer are.

Also call advisor:
- When you believe the task is complete. BEFORE this call, make your deliverable durable: write the file, stage the change, save the result. The advisor call takes time; if the session ends during it, a durable result persists and an unwritten one doesn't.
- When stuck -- errors recurring, approach not converging, results that don't fit.
- When considering a change of approach.

On tasks longer than a few steps, call advisor at least once before committing to an approach and once before declaring done. On short reactive tasks where the next action is dictated by tool output you just read, you don't need to keep calling -- the advisor adds most of its value on the first call, before the approach crystallizes.

Give the advice serious weight. If you follow a step and it fails empirically, or you have primary-source evidence that contradicts a specific claim (the file says X, the code does Y), adapt. A passing self-test is not evidence the advice is wrong -- it's evidence your test doesn't check what the advice is checking.

If you've already retrieved data pointing one way and the advisor points another: don't silently switch. Surface the conflict in one more advisor call -- "I found X, you suggest Y, which constraint breaks the tie?" The advisor saw your evidence but may have underweighted it; a reconcile call is cheaper than committing to the wrong branch.
""";

    /**
     * Advisor usage record.
     */
    public record AdvisorUsage(
        String model,
        int inputTokens,
        int outputTokens
    ) {}

    /**
     * Get advisor usage from beta usage.
     */
    public static List<AdvisorUsage> getAdvisorUsage(Map<String, Object> usage) {
        List<Map<String, Object>> iterations = (List<Map<String, Object>>) usage.get("iterations");
        if (iterations == null) {
            return new ArrayList<>();
        }

        List<AdvisorUsage> result = new ArrayList<>();
        for (Map<String, Object> it : iterations) {
            String type = (String) it.get("type");
            if ("advisor_message".equals(type)) {
                String model = (String) it.get("model");
                Integer inputTokens = (Integer) it.get("input_tokens");
                Integer outputTokens = (Integer) it.get("output_tokens");
                result.add(new AdvisorUsage(
                    model != null ? model : "unknown",
                    inputTokens != null ? inputTokens : 0,
                    outputTokens != null ? outputTokens : 0
                ));
            }
        }
        return result;
    }

    /**
     * Set advisor config (for testing/configuration).
     */
    public static void setAdvisorConfig(AdvisorConfig config) {
        cachedConfig = config;
    }

    /**
     * Clear cached config.
     */
    public static void clearCache() {
        cachedConfig = null;
    }
}