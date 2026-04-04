/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code JSON read utilities
 */
package com.anthropic.claudecode.utils;

import java.util.Map;

/**
 * JSON read utilities with BOM handling.
 */
public final class JsonRead {
    private JsonRead() {}

    // UTF-8 BOM character
    private static final String UTF8_BOM = "\uFEFF";

    /**
     * Strip UTF-8 BOM from content if present.
     * PowerShell 5.x writes UTF-8 with BOM by default.
     */
    public static String stripBOM(String content) {
        if (content == null) return null;
        return content.startsWith(UTF8_BOM) ? content.substring(1) : content;
    }

    /**
     * Parse JSON safely, stripping BOM first.
     */
    public static Map<String, Object> safeParseJson(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }

        String stripped = stripBOM(content).trim();
        if (stripped.isEmpty()) {
            return null;
        }

        try {
            return SlowOperations.jsonParseMap(stripped);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Parse JSON array safely.
     */
    public static java.util.List<Object> safeParseJsonArray(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }

        String stripped = stripBOM(content).trim();
        if (stripped.isEmpty()) {
            return null;
        }

        try {
            return SlowOperations.jsonParseArray(stripped);
        } catch (Exception e) {
            return null;
        }
    }
}