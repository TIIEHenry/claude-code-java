/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code display tags utilities
 */
package com.anthropic.claudecode.utils;

import java.util.regex.*;

/**
 * Strip XML-like tag blocks from text for use in UI titles.
 * System-injected context — IDE metadata, hook output, task notifications —
 * arrives wrapped in tags and should never surface as a title.
 */
public final class DisplayTags {
    private DisplayTags() {}

    /**
     * Matches any XML-like `<tag>…</tag>` block (lowercase tag names, optional
     * attributes, multi-line content).
     */
    private static final Pattern XML_TAG_BLOCK_PATTERN = Pattern.compile(
            "<([a-z][\\w-]*)(?:\\s[^>]*)?>[\\s\\S]*?</\\1>\\n?",
            Pattern.MULTILINE
    );

    /**
     * Pattern for IDE context tags only.
     */
    private static final Pattern IDE_CONTEXT_TAGS_PATTERN = Pattern.compile(
            "<(ide_opened_file|ide_selection)(?:\\s[^>]*)?>[\\s\\S]*?</\\1>\\n?",
            Pattern.MULTILINE
    );

    /**
     * Strip XML-like tag blocks from text for use in UI titles.
     * If stripping would result in empty text, returns the original unchanged.
     */
    public static String stripDisplayTags(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = XML_TAG_BLOCK_PATTERN.matcher(text).replaceAll("").trim();
        return result.isEmpty() ? text : result;
    }

    /**
     * Like stripDisplayTags but returns empty string when all content is tags.
     * Used to detect command-only prompts (e.g. /clear) so they can fall
     * through to the next title fallback.
     */
    public static String stripDisplayTagsAllowEmpty(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        return XML_TAG_BLOCK_PATTERN.matcher(text).replaceAll("").trim();
    }

    /**
     * Strip only IDE-injected context tags (ide_opened_file, ide_selection).
     * Used by textForResubmit so UP-arrow resubmit preserves user-typed content
     * including lowercase HTML like `<code>foo</code>` while dropping IDE noise.
     */
    public static String stripIdeContextTags(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return IDE_CONTEXT_TAGS_PATTERN.matcher(text).replaceAll("").trim();
    }
}