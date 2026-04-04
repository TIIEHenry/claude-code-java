/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code constants/turnCompletionVerbs.ts
 */
package com.anthropic.claudecode.constants;

import java.util.List;

/**
 * Past tense verbs for turn completion messages.
 * These verbs work naturally with "for [duration]" (e.g., "Worked for 5s")
 */
public final class TurnCompletionVerbs {
    private TurnCompletionVerbs() {}

    public static final List<String> TURN_COMPLETION_VERBS = List.of(
        "Baked",
        "Brewed",
        "Churned",
        "Cogitated",
        "Cooked",
        "Crunched",
        "Sautéed",
        "Worked"
    );
}