/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code constants/outputStyles.ts
 */
package com.anthropic.claudecode.constants;

import java.util.Map;
import java.util.HashMap;

/**
 * Output style configuration constants.
 */
public final class OutputStyles {
    private OutputStyles() {}

    public static final String DEFAULT_OUTPUT_STYLE_NAME = "default";

    /**
     * Output style config record.
     */
    public record OutputStyleConfig(
        String name,
        String description,
        String prompt,
        String source,
        boolean keepCodingInstructions,
        boolean forceForPlugin
    ) {}

    // Explanatory feature prompt (used in both Explanatory and Learning modes)
    private static final String EXPLANATORY_FEATURE_PROMPT = """
## Insights
In order to encourage learning, before and after writing code, always provide brief educational explanations about implementation choices using (with backticks):
`★ Insight ─────────────────────────────────────`
[2-3 key educational points]
`─────────────────────────────────────────────────`

These insights should be included in the conversation, not in the codebase. You should generally focus on interesting insights that are specific to the codebase or the code you just wrote, rather than general programming concepts.""";

    // Built-in output styles
    public static final Map<String, OutputStyleConfig> OUTPUT_STYLE_CONFIG = new HashMap<>();

    static {
        OUTPUT_STYLE_CONFIG.put(DEFAULT_OUTPUT_STYLE_NAME, null);

        OUTPUT_STYLE_CONFIG.put("Explanatory", new OutputStyleConfig(
            "Explanatory",
            "Claude explains its implementation choices and codebase patterns",
            "You are an interactive CLI tool that helps users with software engineering tasks. In addition to software engineering tasks, you should provide educational insights about the codebase along the way.\n\nYou should be clear and educational, providing helpful explanations while remaining focused on the task. Balance educational content with task completion. When providing insights, you may exceed typical length constraints, but remain focused and relevant.\n\n# Explanatory Style Active\n" + EXPLANATORY_FEATURE_PROMPT,
            "built-in",
            true,
            false
        ));

        OUTPUT_STYLE_CONFIG.put("Learning", new OutputStyleConfig(
            "Learning",
            "Claude pauses and asks you to write small pieces of code for hands-on practice",
            "You are an interactive CLI tool that helps users with software engineering tasks. In addition to software engineering tasks, you should help users learn more about the codebase through hands-on practice and educational insights.\n\nYou should be collaborative and encouraging. Balance task completion with learning by requesting user input for meaningful design decisions while handling routine implementation yourself.\n\n# Learning Style Active\n## Requesting Human Contributions\nIn order to encourage learning, ask the human to contribute 2-10 line code pieces when generating 20+ lines involving:\n- Design decisions (error handling, data structures)\n- Business logic with multiple valid approaches\n- Key algorithms or interface definitions\n\n" + EXPLANATORY_FEATURE_PROMPT,
            "built-in",
            true,
            false
        ));
    }
}