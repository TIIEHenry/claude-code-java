/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AnsiToPng.
 */
class AnsiToPngTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("AnsiToPng AnsiToPngOptions defaults")
    void optionsDefaults() {
        AnsiToPng.AnsiToPngOptions opts = AnsiToPng.AnsiToPngOptions.defaults();
        assertEquals("Fira Code", opts.fontFamily());
        assertEquals(24, opts.fontSize());
        assertEquals(48, opts.lineHeight());
        assertEquals(24, opts.paddingX());
        assertEquals(24, opts.paddingY());
        assertEquals(AnsiToSvg.DEFAULT_BG, opts.backgroundColor());
        assertEquals(8, opts.borderRadius());
        assertEquals(1.0, opts.scale());
    }

    @Test
    @DisplayName("AnsiToPng ansiToPng returns bytes")
    void ansiToPngReturnsBytes() throws IOException {
        String text = "Hello World";
        byte[] pngData = AnsiToPng.ansiToPng(text);

        assertNotNull(pngData);
        assertTrue(pngData.length > 0);

        // PNG signature (compare as unsigned bytes)
        assertEquals(0x89, Byte.toUnsignedInt(pngData[0]));
        assertEquals(0x50, Byte.toUnsignedInt(pngData[1])); // 'P'
        assertEquals(0x4E, Byte.toUnsignedInt(pngData[2])); // 'N'
        assertEquals(0x47, Byte.toUnsignedInt(pngData[3])); // 'G'
    }

    @Test
    @DisplayName("AnsiToPng ansiToPng with options")
    void ansiToPngWithOptions() throws IOException {
        AnsiToPng.AnsiToPngOptions opts = new AnsiToPng.AnsiToPngOptions(
            "Monospace", 16, 32, 10, 10,
            new AnsiToSvg.AnsiColor(0, 0, 0), 4, 1.0
        );
        byte[] pngData = AnsiToPng.ansiToPng("Test", opts);

        assertNotNull(pngData);
        assertTrue(pngData.length > 0);
    }

    @Test
    @DisplayName("AnsiToPng ansiToPng with colored text")
    void ansiToPngWithColors() throws IOException {
        String text = "\u001b[31mRed\u001b[32mGreen\u001b[0m";
        byte[] pngData = AnsiToPng.ansiToPng(text);

        assertNotNull(pngData);
        assertTrue(pngData.length > 0);
    }

    @Test
    @DisplayName("AnsiToPng ansiToPng empty string")
    void ansiToPngEmpty() throws IOException {
        byte[] pngData = AnsiToPng.ansiToPng("");
        assertNotNull(pngData);
    }

    @Test
    @DisplayName("AnsiToPng writePngFile creates file")
    void writePngFileCreates() throws IOException {
        File outputFile = tempDir.resolve("output.png").toFile();
        AnsiToPng.writePngFile("Test content", outputFile);

        assertTrue(outputFile.exists());
        assertTrue(outputFile.length() > 0);
    }

    @Test
    @DisplayName("AnsiToPng writePngFile with options")
    void writePngFileWithOptions() throws IOException {
        AnsiToPng.AnsiToPngOptions opts = AnsiToPng.AnsiToPngOptions.defaults();
        File outputFile = tempDir.resolve("output2.png").toFile();
        AnsiToPng.writePngFile("Test", outputFile, opts);

        assertTrue(outputFile.exists());
    }

    @Test
    @DisplayName("AnsiToPng scale option affects size")
    void scaleOption() throws IOException {
        AnsiToPng.AnsiToPngOptions small = new AnsiToPng.AnsiToPngOptions(
            "Fira Code", 24, 48, 24, 24, AnsiToSvg.DEFAULT_BG, 8, 0.5
        );
        AnsiToPng.AnsiToPngOptions large = new AnsiToPng.AnsiToPngOptions(
            "Fira Code", 24, 48, 24, 24, AnsiToSvg.DEFAULT_BG, 8, 2.0
        );

        byte[] smallPng = AnsiToPng.ansiToPng("Test", small);
        byte[] largePng = AnsiToPng.ansiToPng("Test", large);

        // Larger scale should produce larger file
        assertTrue(largePng.length > smallPng.length);
    }
}