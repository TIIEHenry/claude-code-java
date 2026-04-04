/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code constants/figures.ts
 */
package com.anthropic.claudecode.constants;

/**
 * Unicode figure/symbol constants for terminal display.
 */
public final class Figures {
    private Figures() {}

    // Platform-dependent symbols
    public static final String BLACK_CIRCLE = isDarwin() ? "⏺" : "●";
    public static final String BULLET_OPERATOR = "∙";
    public static final String TEARDROP_ASTERISK = "✻";

    // Arrow symbols
    public static final String UP_ARROW = "\u2191";     // ↑
    public static final String DOWN_ARROW = "\u2193";   // ↓
    public static final String LIGHTNING_BOLT = "↯";    // fast mode indicator
    public static final String REFRESH_ARROW = "\u21bb"; // ↻
    public static final String CHANNEL_ARROW = "\u2190"; // ←
    public static final String INJECTED_ARROW = "\u2192"; // →

    // Effort level indicators
    public static final String EFFORT_LOW = "\u25cb";    // ○
    public static final String EFFORT_MEDIUM = "\u25d0"; // ◐
    public static final String EFFORT_HIGH = "\u25cf";   // ●
    public static final String EFFORT_MAX = "\u25c9";    // ◉

    // Media/trigger status indicators
    public static final String PLAY_ICON = "\u25b6";  // ▶
    public static final String PAUSE_ICON = "\u23f8"; // ⏸

    // Fork glyph
    public static final String FORK_GLYPH = "\u2442"; // ⑂

    // Review status indicators (ultrareview diamond states)
    public static final String DIAMOND_OPEN = "\u25c7";   // ◇
    public static final String DIAMOND_FILLED = "\u25c6"; // ◆
    public static final String REFERENCE_MARK = "\u203b"; // ※

    // Issue flag indicator
    public static final String FLAG_ICON = "\u2691"; // ⚑

    // Blockquote indicator
    public static final String BLOCKQUOTE_BAR = "\u258e";      // ▎
    public static final String HEAVY_HORIZONTAL = "\u2501";    // ━

    // Bridge status indicators
    public static final String[] BRIDGE_SPINNER_FRAMES = {
        "·|·",
        "·/·",
        "·—·",
        "·\\·"
    };
    public static final String BRIDGE_READY_INDICATOR = "·✔︎·";
    public static final String BRIDGE_FAILED_INDICATOR = "×";

    // Check if running on macOS
    private static boolean isDarwin() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }
}