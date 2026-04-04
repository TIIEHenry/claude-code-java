/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code displayer utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.function.*;

/**
 * Console display utilities.
 */
public final class ConsoleDisplay {
    private ConsoleDisplay() {}

    /**
     * Progress bar.
     */
    public static String progressBar(double progress, int width) {
        int filled = (int) (progress * width);
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < width; i++) {
            sb.append(i < filled ? '=' : ' ');
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Progress bar with percentage.
     */
    public static String progressBar(double progress, int width, String label) {
        return String.format("%s %s %.1f%%", label, progressBar(progress, width), progress * 100);
    }

    /**
     * Spinner frames.
     */
    public static final String[] SPINNER_FRAMES = {"⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"};

    /**
     * Get spinner frame.
     */
    public static String spinner(int frame) {
        return SPINNER_FRAMES[frame % SPINNER_FRAMES.length];
    }

    /**
     * Table builder.
     */
    public static TableBuilder table() {
        return new TableBuilder();
    }

    /**
     * Table builder class.
     */
    public static final class TableBuilder {
        private final List<String> headers = new ArrayList<>();
        private final List<List<String>> rows = new ArrayList<>();
        private int padding = 1;

        public TableBuilder headers(String... headers) {
            this.headers.addAll(Arrays.asList(headers));
            return this;
        }

        public TableBuilder row(String... values) {
            this.rows.add(Arrays.asList(values));
            return this;
        }

        public TableBuilder padding(int padding) {
            this.padding = padding;
            return this;
        }

        public String build() {
            if (headers.isEmpty() && rows.isEmpty()) return "";

            // Calculate column widths
            int columns = Math.max(headers.size(),
                rows.stream().mapToInt(List::size).max().orElse(0));
            int[] widths = new int[columns];

            for (int i = 0; i < headers.size(); i++) {
                widths[i] = Math.max(widths[i], headers.get(i).length());
            }

            for (List<String> row : rows) {
                for (int i = 0; i < row.size(); i++) {
                    widths[i] = Math.max(widths[i], row.get(i).length());
                }
            }

            // Build table
            StringBuilder sb = new StringBuilder();
            String padStr = " ".repeat(padding);

            // Header
            sb.append("┌");
            for (int i = 0; i < columns; i++) {
                sb.append("─".repeat(widths[i] + padding * 2));
                if (i < columns - 1) sb.append("┬");
            }
            sb.append("┐\n");

            // Header row
            sb.append("│");
            for (int i = 0; i < headers.size(); i++) {
                sb.append(padStr).append(padRight(headers.get(i), widths[i])).append(padStr).append("│");
            }
            sb.append("\n");

            // Header separator
            sb.append("├");
            for (int i = 0; i < columns; i++) {
                sb.append("─".repeat(widths[i] + padding * 2));
                if (i < columns - 1) sb.append("┼");
            }
            sb.append("┤\n");

            // Rows
            for (List<String> row : rows) {
                sb.append("│");
                for (int i = 0; i < columns; i++) {
                    String value = i < row.size() ? row.get(i) : "";
                    sb.append(padStr).append(padRight(value, widths[i])).append(padStr).append("│");
                }
                sb.append("\n");
            }

            // Footer
            sb.append("└");
            for (int i = 0; i < columns; i++) {
                sb.append("─".repeat(widths[i] + padding * 2));
                if (i < columns - 1) sb.append("┴");
            }
            sb.append("┘");

            return sb.toString();
        }

        private String padRight(String s, int width) {
            return s + " ".repeat(Math.max(0, width - s.length()));
        }
    }

    /**
     * Tree printer.
     */
    public static String tree(String root, List<String> children) {
        StringBuilder sb = new StringBuilder();
        sb.append(root).append("\n");
        for (int i = 0; i < children.size(); i++) {
            boolean isLast = i == children.size() - 1;
            sb.append(isLast ? "└── " : "├── ").append(children.get(i)).append("\n");
        }
        return sb.toString();
    }

    /**
     * Histogram.
     */
    public static String histogram(Map<String, Integer> data, int maxWidth) {
        int maxValue = data.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        int maxKeyLen = data.keySet().stream().mapToInt(String::length).max().orElse(0);

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            int barWidth = (int) ((double) entry.getValue() / maxValue * maxWidth);
            sb.append(padRight(entry.getKey(), maxKeyLen))
              .append(" │")
              .append("█".repeat(barWidth))
              .append(" ")
              .append(entry.getValue())
              .append("\n");
        }
        return sb.toString();
    }

    /**
     * Key-value display.
     */
    public static String keyValue(Map<String, Object> data) {
        int maxKeyLen = data.keySet().stream().mapToInt(String::length).max().orElse(0);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            sb.append(padRight(entry.getKey(), maxKeyLen))
              .append(" : ")
              .append(entry.getValue())
              .append("\n");
        }
        return sb.toString();
    }

    private static String padRight(String s, int width) {
        return s + " ".repeat(Math.max(0, width - s.length()));
    }

    /**
     * List display.
     */
    public static String list(Iterable<?> items) {
        StringBuilder sb = new StringBuilder();
        for (Object item : items) {
            sb.append("• ").append(item).append("\n");
        }
        return sb.toString();
    }

    /**
     * Numbered list.
     */
    public static String numberedList(Iterable<?> items) {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (Object item : items) {
            sb.append(i++).append(". ").append(item).append("\n");
        }
        return sb.toString();
    }

    /**
     * Columns display.
     */
    public static String columns(List<String> items, int columnWidth, int consoleWidth) {
        int columns = Math.max(1, consoleWidth / columnWidth);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            sb.append(padRight(items.get(i), columnWidth));
            if ((i + 1) % columns == 0) {
                sb.append("\n");
            }
        }
        if (items.size() % columns != 0) {
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Box text.
     */
    public static String box(String text) {
        String[] lines = text.split("\n");
        int maxWidth = Arrays.stream(lines).mapToInt(String::length).max().orElse(0);
        StringBuilder sb = new StringBuilder();
        sb.append("┌").append("─".repeat(maxWidth + 2)).append("┐\n");
        for (String line : lines) {
            sb.append("│ ").append(padRight(line, maxWidth)).append(" │\n");
        }
        sb.append("└").append("─".repeat(maxWidth + 2)).append("┘");
        return sb.toString();
    }

    /**
     * Divider line.
     */
    public static String divider(char c, int width) {
        return String.valueOf(c).repeat(width);
    }

    /**
     * Center text.
     */
    public static String center(String text, int width) {
        int padding = Math.max(0, (width - text.length()) / 2);
        return " ".repeat(padding) + text + " ".repeat(Math.max(0, width - padding - text.length()));
    }
}