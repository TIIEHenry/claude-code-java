/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code ANSI to PNG conversion
 */
package com.anthropic.claudecode.utils;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.zip.*;
import javax.imageio.*;

/**
 * Render ANSI-escaped terminal text directly to a PNG image.
 * Uses Java's built-in imaging capabilities instead of external WASM.
 */
public final class AnsiToPng {
    private AnsiToPng() {}

    // Glyph cell size
    private static final int GLYPH_W = 24;
    private static final int GLYPH_H = 48;

    /**
     * Options for PNG conversion.
     */
    public record AnsiToPngOptions(
        String fontFamily,
        int fontSize,
        int lineHeight,
        int paddingX,
        int paddingY,
        AnsiToSvg.AnsiColor backgroundColor,
        int borderRadius,
        double scale
    ) {
        public static AnsiToPngOptions defaults() {
            return new AnsiToPngOptions(
                "Fira Code",
                24,
                48,
                24,
                24,
                AnsiToSvg.DEFAULT_BG,
                8,
                1.0
            );
        }
    }

    /**
     * Convert ANSI text to PNG bytes.
     */
    public static byte[] ansiToPng(String ansiText) throws IOException {
        return ansiToPng(ansiText, AnsiToPngOptions.defaults());
    }

    /**
     * Convert ANSI text to PNG bytes with options.
     */
    public static byte[] ansiToPng(String ansiText, AnsiToPngOptions options) throws IOException {
        List<List<AnsiToSvg.TextSpan>> lines = AnsiToSvg.parseAnsi(ansiText);

        // Trim trailing empty lines
        while (!lines.isEmpty() && lines.get(lines.size() - 1).stream().allMatch(s -> s.text().trim().isEmpty())) {
            lines.remove(lines.size() - 1);
        }

        int fontSize = options.fontSize() > 0 ? options.fontSize() : 24;
        int lineHeight = options.lineHeight() > 0 ? options.lineHeight() : 48;
        int paddingX = options.paddingX() >= 0 ? options.paddingX() : 24;
        int paddingY = options.paddingY() >= 0 ? options.paddingY() : 24;
        double scale = options.scale() > 0 ? options.scale() : 1.0;

        // Calculate dimensions
        Font font = new Font(options.fontFamily() != null ? options.fontFamily() : "Fira Code", Font.PLAIN, fontSize);
        Font boldFont = new Font(options.fontFamily() != null ? options.fontFamily() : "Fira Code", Font.BOLD, fontSize);

        // Estimate character width
        BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D tempG = tempImage.createGraphics();
        tempG.setFont(font);
        FontMetrics fm = tempG.getFontMetrics();
        int charWidth = fm.charWidth('M');
        int maxLineLength = lines.stream()
                .mapToInt(spans -> spans.stream().mapToInt(s -> s.text().length()).sum())
                .max()
                .orElse(0);
        tempG.dispose();

        int width = (int) Math.ceil(maxLineLength * charWidth + paddingX * 2);
        int height = lines.size() * lineHeight + paddingY * 2;

        // Apply scale
        width = (int) Math.ceil(width * scale);
        height = (int) Math.ceil(height * scale);
        int scaledFontSize = (int) Math.ceil(fontSize * scale);
        int scaledLineHeight = (int) Math.ceil(lineHeight * scale);
        int scaledPaddingX = (int) Math.ceil(paddingX * scale);
        int scaledPaddingY = (int) Math.ceil(paddingY * scale);

        // Create image
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Fill background
        AnsiToSvg.AnsiColor bgColor = options.backgroundColor() != null ? options.backgroundColor() : AnsiToSvg.DEFAULT_BG;
        g.setColor(new Color(bgColor.r(), bgColor.g(), bgColor.b()));
        g.fillRect(0, 0, width, height);

        // Draw text
        Font scaledFont = new Font(options.fontFamily() != null ? options.fontFamily() : "Fira Code", Font.PLAIN, scaledFontSize);
        Font scaledBoldFont = new Font(options.fontFamily() != null ? options.fontFamily() : "Fira Code", Font.BOLD, scaledFontSize);

        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            List<AnsiToSvg.TextSpan> spans = lines.get(lineIndex);
            int y = scaledPaddingY + (lineIndex + 1) * scaledLineHeight - (scaledLineHeight - scaledFontSize) / 2;
            int x = scaledPaddingX;

            for (AnsiToSvg.TextSpan span : spans) {
                if (span.text().isEmpty()) continue;

                AnsiToSvg.AnsiColor color = span.color();
                g.setColor(new Color(color.r(), color.g(), color.b()));
                g.setFont(span.bold() ? scaledBoldFont : scaledFont);

                g.drawString(span.text(), x, y);
                x += g.getFontMetrics().stringWidth(span.text());
            }
        }

        g.dispose();

        // Encode to PNG
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", out);
        return out.toByteArray();
    }

    /**
     * Write ANSI text as PNG to file.
     */
    public static void writePngFile(String ansiText, File outputFile) throws IOException {
        byte[] pngData = ansiToPng(ansiText);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(pngData);
        }
    }

    /**
     * Write ANSI text as PNG to file with options.
     */
    public static void writePngFile(String ansiText, File outputFile, AnsiToPngOptions options) throws IOException {
        byte[] pngData = ansiToPng(ansiText, options);
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.write(pngData);
        }
    }
}