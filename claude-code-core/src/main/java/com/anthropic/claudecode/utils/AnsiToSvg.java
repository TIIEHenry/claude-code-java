/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code ANSI to SVG conversion
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.regex.*;

/**
 * Converts ANSI-escaped terminal text to SVG format.
 * Supports basic ANSI color codes (foreground colors).
 */
public final class AnsiToSvg {
    private AnsiToSvg() {}

    /**
     * ANSI color record.
     */
    public record AnsiColor(int r, int g, int b) {}

    /**
     * Text span record.
     */
    public record TextSpan(String text, AnsiColor color, boolean bold) {}

    // Default terminal colors
    public static final AnsiColor DEFAULT_FG = new AnsiColor(229, 229, 229);
    public static final AnsiColor DEFAULT_BG = new AnsiColor(30, 30, 30);

    // ANSI color palette
    private static final Map<Integer, AnsiColor> ANSI_COLORS = new HashMap<>();

    static {
        // Basic colors
        ANSI_COLORS.put(30, new AnsiColor(0, 0, 0));       // black
        ANSI_COLORS.put(31, new AnsiColor(205, 49, 49));   // red
        ANSI_COLORS.put(32, new AnsiColor(13, 188, 121));  // green
        ANSI_COLORS.put(33, new AnsiColor(229, 229, 16));  // yellow
        ANSI_COLORS.put(34, new AnsiColor(36, 114, 200));  // blue
        ANSI_COLORS.put(35, new AnsiColor(188, 63, 188));  // magenta
        ANSI_COLORS.put(36, new AnsiColor(17, 168, 205));  // cyan
        ANSI_COLORS.put(37, new AnsiColor(229, 229, 229)); // white
        // Bright colors
        ANSI_COLORS.put(90, new AnsiColor(102, 102, 102));   // bright black
        ANSI_COLORS.put(91, new AnsiColor(241, 76, 76));     // bright red
        ANSI_COLORS.put(92, new AnsiColor(35, 209, 139));    // bright green
        ANSI_COLORS.put(93, new AnsiColor(245, 245, 67));    // bright yellow
        ANSI_COLORS.put(94, new AnsiColor(59, 142, 234));    // bright blue
        ANSI_COLORS.put(95, new AnsiColor(214, 112, 214));   // bright magenta
        ANSI_COLORS.put(96, new AnsiColor(41, 184, 219));    // bright cyan
        ANSI_COLORS.put(97, new AnsiColor(255, 255, 255));   // bright white
    }

    /**
     * Parse ANSI escape sequences from text.
     */
    public static List<List<TextSpan>> parseAnsi(String text) {
        List<List<TextSpan>> lines = new ArrayList<>();
        String[] rawLines = text.split("\n");

        for (String line : rawLines) {
            List<TextSpan> spans = new ArrayList<>();
            AnsiColor currentColor = DEFAULT_FG;
            boolean bold = false;
            int i = 0;

            while (i < line.length()) {
                // Check for ANSI escape sequence
                if (line.charAt(i) == '\u001b' && i + 1 < line.length() && line.charAt(i + 1) == '[') {
                    // Find the end of the escape sequence
                    int j = i + 2;
                    while (j < line.length() && !Character.isLetter(line.charAt(j))) {
                        j++;
                    }

                    if (j < line.length() && line.charAt(j) == 'm') {
                        // Color/style code
                        String codeStr = line.substring(i + 2, j);
                        String[] parts = codeStr.split(";");
                        int[] codes = new int[parts.length];
                        for (int k = 0; k < parts.length; k++) {
                            try {
                                codes[k] = Integer.parseInt(parts[k].isEmpty() ? "0" : parts[k]);
                            } catch (NumberFormatException e) {
                                codes[k] = 0;
                            }
                        }

                        int k = 0;
                        while (k < codes.length) {
                            int code = codes[k];
                            if (code == 0) {
                                currentColor = DEFAULT_FG;
                                bold = false;
                            } else if (code == 1) {
                                bold = true;
                            } else if (code >= 30 && code <= 37) {
                                currentColor = ANSI_COLORS.getOrDefault(code, DEFAULT_FG);
                            } else if (code >= 90 && code <= 97) {
                                currentColor = ANSI_COLORS.getOrDefault(code, DEFAULT_FG);
                            } else if (code == 39) {
                                currentColor = DEFAULT_FG;
                            } else if (code == 38) {
                                // Extended color
                                if (k + 2 < codes.length && codes[k + 1] == 5) {
                                    int colorIndex = codes[k + 2];
                                    currentColor = get256Color(colorIndex);
                                    k += 2;
                                } else if (k + 4 < codes.length && codes[k + 1] == 2) {
                                    currentColor = new AnsiColor(codes[k + 2], codes[k + 3], codes[k + 4]);
                                    k += 4;
                                }
                            }
                            k++;
                        }
                    }

                    i = j + 1;
                    continue;
                }

                // Regular character - find extent of same-styled text
                int textStart = i;
                while (i < line.length() && line.charAt(i) != '\u001b') {
                    i++;
                }

                String spanText = line.substring(textStart, i);
                if (!spanText.isEmpty()) {
                    spans.add(new TextSpan(spanText, currentColor, bold));
                }
            }

            // Add empty span if line is empty
            if (spans.isEmpty()) {
                spans.add(new TextSpan("", DEFAULT_FG, false));
            }

            lines.add(spans);
        }

        return lines;
    }

    /**
     * Get color from 256-color palette.
     */
    private static AnsiColor get256Color(int index) {
        // Standard colors (0-15)
        if (index < 16) {
            AnsiColor[] standardColors = {
                new AnsiColor(0, 0, 0),         // 0 black
                new AnsiColor(128, 0, 0),       // 1 red
                new AnsiColor(0, 128, 0),       // 2 green
                new AnsiColor(128, 128, 0),     // 3 yellow
                new AnsiColor(0, 0, 128),       // 4 blue
                new AnsiColor(128, 0, 128),     // 5 magenta
                new AnsiColor(0, 128, 128),     // 6 cyan
                new AnsiColor(192, 192, 192),   // 7 white
                new AnsiColor(128, 128, 128),   // 8 bright black
                new AnsiColor(255, 0, 0),       // 9 bright red
                new AnsiColor(0, 255, 0),       // 10 bright green
                new AnsiColor(255, 255, 0),     // 11 bright yellow
                new AnsiColor(0, 0, 255),       // 12 bright blue
                new AnsiColor(255, 0, 255),     // 13 bright magenta
                new AnsiColor(0, 255, 255),     // 14 bright cyan
                new AnsiColor(255, 255, 255),   // 15 bright white
            };
            return index < standardColors.length ? standardColors[index] : DEFAULT_FG;
        }

        // 216 color cube (16-231)
        if (index < 232) {
            int i = index - 16;
            int r = i / 36;
            int g = (i % 36) / 6;
            int b = i % 6;
            return new AnsiColor(
                r == 0 ? 0 : 55 + r * 40,
                g == 0 ? 0 : 55 + g * 40,
                b == 0 ? 0 : 55 + b * 40
            );
        }

        // Grayscale (232-255)
        int gray = (index - 232) * 10 + 8;
        return new AnsiColor(gray, gray, gray);
    }

    /**
     * Options for SVG conversion.
     */
    public record AnsiToSvgOptions(
        String fontFamily,
        int fontSize,
        int lineHeight,
        int paddingX,
        int paddingY,
        String backgroundColor,
        int borderRadius
    ) {
        public static AnsiToSvgOptions defaults() {
            return new AnsiToSvgOptions(
                "Menlo, Monaco, monospace",
                14,
                22,
                24,
                24,
                "rgb(30, 30, 30)",
                8
            );
        }
    }

    /**
     * Convert ANSI text to SVG.
     */
    public static String ansiToSvg(String ansiText) {
        return ansiToSvg(ansiText, AnsiToSvgOptions.defaults());
    }

    /**
     * Convert ANSI text to SVG with options.
     */
    public static String ansiToSvg(String ansiText, AnsiToSvgOptions options) {
        String fontFamily = options.fontFamily() != null ? options.fontFamily() : "Menlo, Monaco, monospace";
        int fontSize = options.fontSize() > 0 ? options.fontSize() : 14;
        int lineHeight = options.lineHeight() > 0 ? options.lineHeight() : 22;
        int paddingX = options.paddingX() >= 0 ? options.paddingX() : 24;
        int paddingY = options.paddingY() >= 0 ? options.paddingY() : 24;
        String backgroundColor = options.backgroundColor() != null ? options.backgroundColor() : "rgb(30, 30, 30)";
        int borderRadius = options.borderRadius() >= 0 ? options.borderRadius() : 8;

        List<List<TextSpan>> lines = parseAnsi(ansiText);

        // Trim trailing empty lines
        while (!lines.isEmpty() && lines.get(lines.size() - 1).stream().allMatch(s -> s.text().trim().isEmpty())) {
            lines.remove(lines.size() - 1);
        }

        // Estimate width
        double charWidthEstimate = fontSize * 0.6;
        int maxLineLength = lines.stream()
                .mapToInt(spans -> spans.stream().mapToInt(s -> s.text().length()).sum())
                .max()
                .orElse(0);
        int width = (int) Math.ceil(maxLineLength * charWidthEstimate + paddingX * 2);
        int height = lines.size() * lineHeight + paddingY * 2;

        StringBuilder svg = new StringBuilder();
        svg.append(String.format("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"%d\" height=\"%d\" viewBox=\"0 0 %d %d\">\n",
                width, height, width, height));
        svg.append(String.format("  <rect width=\"100%%\" height=\"100%%\" fill=\"%s\" rx=\"%d\" ry=\"%d\"/>\n",
                backgroundColor, borderRadius, borderRadius));
        svg.append("  <style>\n");
        svg.append(String.format("    text { font-family: %s; font-size: %dpx; white-space: pre; }\n", fontFamily, fontSize));
        svg.append("    .b { font-weight: bold; }\n");
        svg.append("  </style>\n");

        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            List<TextSpan> spans = lines.get(lineIndex);
            int y = paddingY + (lineIndex + 1) * lineHeight - (lineHeight - fontSize) / 2;

            svg.append(String.format("  <text x=\"%d\" y=\"%d\" xml:space=\"preserve\">", paddingX, y));

            for (TextSpan span : spans) {
                if (span.text().isEmpty()) continue;

                String colorStr = String.format("rgb(%d, %d, %d)", span.color().r(), span.color().g(), span.color().b());
                String boldClass = span.bold() ? " class=\"b\"" : "";
                String escapedText = escapeXml(span.text());

                svg.append(String.format("<tspan fill=\"%s\"%s>%s</tspan>", colorStr, boldClass, escapedText));
            }

            svg.append("</text>\n");
        }

        svg.append("</svg>");

        return svg.toString();
    }

    /**
     * Escape XML special characters.
     */
    private static String escapeXml(String text) {
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}