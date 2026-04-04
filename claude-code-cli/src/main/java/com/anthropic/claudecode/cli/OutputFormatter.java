/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 *
 * Java port of Claude Code CLI output formatter
 */
package com.anthropic.claudecode.cli;

import java.util.*;

/**
 * Output formatter for CLI output.
 *
 * <p>Provides formatted output utilities.
 */
public class OutputFormatter {

    private final boolean useColors;
    private final int terminalWidth;

    // ANSI color codes
    private static final String RESET = "\u001B[0m";
    private static final String BOLD = "\u001B[1m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String MAGENTA = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String GRAY = "\u001B[90m";

    public OutputFormatter() {
        this(true, 80);
    }

    public OutputFormatter(boolean useColors, int terminalWidth) {
        this.useColors = useColors;
        this.terminalWidth = terminalWidth;
    }

    /**
     * Format header.
     */
    public String header(String text) {
        return color(BOLD + BLUE, text);
    }

    /**
     * Format subheader.
     */
    public String subheader(String text) {
        return color(BOLD, text);
    }

    /**
     * Format success message.
     */
    public String success(String text) {
        return color(GREEN, "✓ " + text);
    }

    /**
     * Format error message.
     */
    public String error(String text) {
        return color(RED, "✗ " + text);
    }

    /**
     * Format warning message.
     */
    public String warning(String text) {
        return color(YELLOW, "⚠ " + text);
    }

    /**
     * Format info message.
     */
    public String info(String text) {
        return color(CYAN, "ℹ " + text);
    }

    /**
     * Format dimmed text.
     */
    public String dim(String text) {
        return color(GRAY, text);
    }

    /**
     * Format highlighted text.
     */
    public String highlight(String text) {
        return color(BOLD + YELLOW, text);
    }

    /**
     * Format code block.
     */
    public String codeBlock(String code, String language) {
        StringBuilder sb = new StringBuilder();
        sb.append(dim("```")).append(language != null ? language : "").append("\n");
        sb.append(code);
        if (!code.endsWith("\n")) {
            sb.append("\n");
        }
        sb.append(dim("```")).append("\n");
        return sb.toString();
    }

    /**
     * Format key-value pair.
     */
    public String keyValue(String key, Object value) {
        return color(CYAN, key) + ": " + value;
    }

    /**
     * Format list.
     */
    public String list(String... items) {
        return list(Arrays.asList(items));
    }

    /**
     * Format list.
     */
    public String list(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (String item : items) {
            sb.append("  • ").append(item).append("\n");
        }
        return sb.toString();
    }

    /**
     * Format numbered list.
     */
    public String numberedList(List<String> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            sb.append("  ").append(i + 1).append(". ").append(items.get(i)).append("\n");
        }
        return sb.toString();
    }

    /**
     * Format table.
     */
    public String table(String[] headers, List<String[]> rows) {
        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
        }

        for (String[] row : rows) {
            for (int i = 0; i < row.length && i < widths.length; i++) {
                widths[i] = Math.max(widths[i], row[i].length());
            }
        }

        StringBuilder sb = new StringBuilder();

        // Header
        sb.append(color(BOLD, padRow(headers, widths))).append("\n");
        sb.append(dim(separator(widths))).append("\n");

        // Rows
        for (String[] row : rows) {
            sb.append(padRow(row, widths)).append("\n");
        }

        return sb.toString();
    }

    /**
     * Format progress bar.
     */
    public String progressBar(int current, int total, int width) {
        double percent = (double) current / total;
        int filled = (int) (width * percent);

        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append(color(GREEN, "█".repeat(filled)));
        sb.append(dim("░".repeat(width - filled)));
        sb.append("] ");
        sb.append(String.format("%3d%%", (int) (percent * 100)));

        return sb.toString();
    }

    /**
     * Format spinner.
     */
    public String spinner(int frame) {
        String[] frames = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};
        return color(CYAN, frames[frame % frames.length]);
    }

    /**
     * Wrap text to terminal width.
     */
    public String wrap(String text) {
        return wrap(text, terminalWidth);
    }

    /**
     * Wrap text to specified width.
     */
    public String wrap(String text, int width) {
        if (text == null || text.length() <= width) {
            return text;
        }

        StringBuilder sb = new StringBuilder();
        int lineStart = 0;

        while (lineStart < text.length()) {
            int lineEnd = Math.min(lineStart + width, text.length());

            if (lineEnd < text.length()) {
                int lastSpace = text.lastIndexOf(' ', lineEnd);
                if (lastSpace > lineStart) {
                    lineEnd = lastSpace;
                }
            }

            sb.append(text, lineStart, lineEnd).append("\n");
            lineStart = lineEnd;
            while (lineStart < text.length() && text.charAt(lineStart) == ' ') {
                lineStart++;
            }
        }

        return sb.toString();
    }

    /**
     * Center text.
     */
    public String center(String text) {
        int padding = (terminalWidth - text.length()) / 2;
        if (padding <= 0) return text;
        return " ".repeat(padding) + text;
    }

    private String color(String colorCode, String text) {
        if (!useColors) return text;
        return colorCode + text + RESET;
    }

    private String padRow(String[] row, int[] widths) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < widths.length; i++) {
            String value = i < row.length ? row[i] : "";
            sb.append(String.format("%-" + widths[i] + "s  ", value));
        }
        return sb.toString();
    }

    private String separator(int[] widths) {
        int total = 0;
        for (int w : widths) {
            total += w + 2;
        }
        return "─".repeat(total);
    }
}
