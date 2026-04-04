/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code hints protocol
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.regex.*;

/**
 * Claude Code hints protocol.
 *
 * CLIs and SDKs running under Claude Code can emit a self-closing
 * `<claude-code-hint />` tag to stderr. The harness scans tool output
 * for these tags, strips them before the output reaches the model,
 * and surfaces an install prompt to the user.
 */
public final class ClaudeCodeHints {
    private ClaudeCodeHints() {}

    /**
     * Hint type enum.
     */
    public enum HintType {
        PLUGIN
    }

    /**
     * Claude Code hint record.
     */
    public record ClaudeCodeHint(
            int v,                      // Spec version
            HintType type,              // Hint discriminator
            String value,               // Hint payload
            String sourceCommand        // First token of shell command
    ) {}

    // Supported versions
    private static final Set<Integer> SUPPORTED_VERSIONS = Set.of(1);

    // Supported types
    private static final Set<String> SUPPORTED_TYPES = Set.of("plugin");

    // Hint tag regex - anchored to whole lines
    private static final Pattern HINT_TAG_RE = Pattern.compile(
            "^[ \\t]*<claude-code-hint\\s+([^>]*?)\\s*/>[ \\t]*$",
            Pattern.MULTILINE
    );

    // Attribute matcher
    private static final Pattern ATTR_RE = Pattern.compile(
            "(\\w+)=(?:\"([^\"]*)\"|([^\\s/>]+))"
    );

    // Pending hint store
    private static volatile ClaudeCodeHint pendingHint = null;
    private static volatile boolean shownThisSession = false;
    private static final SignalNew pendingHintChanged = new SignalNew();

    /**
     * Extract Claude Code hints from shell tool output.
     *
     * @param output  Raw command output
     * @param command The command that produced the output
     * @return Extracted hints and stripped output
     */
    public static ExtractResult extractClaudeCodeHints(String output, String command) {
        // Fast path: no tag open sequence
        if (!output.contains("<claude-code-hint")) {
            return new ExtractResult(List.of(), output);
        }

        String sourceCommand = firstCommandToken(command);
        List<ClaudeCodeHint> hints = new ArrayList<>();

        Matcher matcher = HINT_TAG_RE.matcher(output);
        StringBuffer stripped = new StringBuffer();

        while (matcher.find()) {
            String rawLine = matcher.group(0);
            Map<String, String> attrs = parseAttrs(rawLine);

            int v = Integer.parseInt(attrs.getOrDefault("v", "0"));
            String typeStr = attrs.get("type");
            String value = attrs.get("value");

            if (!SUPPORTED_VERSIONS.contains(v)) {
                continue;
            }
            if (typeStr == null || !SUPPORTED_TYPES.contains(typeStr)) {
                continue;
            }
            if (value == null || value.isEmpty()) {
                continue;
            }

            hints.add(new ClaudeCodeHint(
                    v,
                    HintType.PLUGIN,
                    value,
                    sourceCommand
            ));

            matcher.appendReplacement(stripped, "");
        }
        matcher.appendTail(stripped);

        // Collapse runs of blank lines
        String collapsed = hints.isEmpty() && stripped.toString().equals(output)
                ? stripped.toString()
                : stripped.toString().replaceAll("\n{3,}", "\n\n");

        return new ExtractResult(hints, collapsed);
    }

    /**
     * Parse attributes from tag body.
     */
    private static Map<String, String> parseAttrs(String tagBody) {
        Map<String, String> attrs = new HashMap<>();
        Matcher matcher = ATTR_RE.matcher(tagBody);
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            attrs.put(key, value != null ? value : "");
        }
        return attrs;
    }

    /**
     * Get first token of command.
     */
    private static String firstCommandToken(String command) {
        if (command == null) return "";
        String trimmed = command.trim();
        int spaceIdx = trimmed.indexOf(' ');
        return spaceIdx == -1 ? trimmed : trimmed.substring(0, spaceIdx);
    }

    // Store methods

    /**
     * Set pending hint (writes if slot empty and not shown this session).
     */
    public static void setPendingHint(ClaudeCodeHint hint) {
        if (shownThisSession) return;
        pendingHint = hint;
        pendingHintChanged.emit(hint);
    }

    /**
     * Clear pending hint without flipping session flag.
     */
    public static void clearPendingHint() {
        if (pendingHint != null) {
            pendingHint = null;
            pendingHintChanged.emit(null);
        }
    }

    /**
     * Mark that a dialog was shown this session.
     */
    public static void markShownThisSession() {
        shownThisSession = true;
    }

    /**
     * Subscribe to pending hint changes.
     */
    public static void subscribeToPendingHint(Runnable callback) {
        pendingHintChanged.subscribe(callback);
    }

    /**
     * Get current pending hint.
     */
    public static ClaudeCodeHint getPendingHintSnapshot() {
        return pendingHint;
    }

    /**
     * Check if hint was shown this session.
     */
    public static boolean hasShownHintThisSession() {
        return shownThisSession;
    }

    /**
     * Reset store (for testing).
     */
    public static void reset() {
        pendingHint = null;
        shownThisSession = false;
    }

    /**
     * Extract result record.
     */
    public record ExtractResult(List<ClaudeCodeHint> hints, String stripped) {}
}