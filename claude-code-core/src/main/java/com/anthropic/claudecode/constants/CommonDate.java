/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code constants/common
 */
package com.anthropic.claudecode.constants;

import java.time.*;
import java.time.format.*;

/**
 * Common date utilities.
 */
public final class CommonDate {
    private CommonDate() {}

    private static volatile String cachedDate;

    /**
     * Get local ISO date.
     */
    public static String getLocalISODate() {
        String override = System.getenv("CLAUDE_CODE_OVERRIDE_DATE");
        if (override != null) {
            return override;
        }

        LocalDate now = LocalDate.now();
        return now.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /**
     * Get session start date (memoized).
     * Captures the date once at session start for prompt-cache stability.
     */
    public static String getSessionStartDate() {
        if (cachedDate == null) {
            cachedDate = getLocalISODate();
        }
        return cachedDate;
    }

    /**
     * Reset cached date (for testing).
     */
    public static void resetCachedDate() {
        cachedDate = null;
    }

    /**
     * Get local month year string (e.g. "February 2026").
     * Changes monthly, not daily — used in tool prompts to minimize cache busting.
     */
    public static String getLocalMonthYear() {
        String override = System.getenv("CLAUDE_CODE_OVERRIDE_DATE");
        LocalDate date;
        if (override != null) {
            date = LocalDate.parse(override);
        } else {
            date = LocalDate.now();
        }

        return date.format(DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.US));
    }
}