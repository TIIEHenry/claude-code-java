/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BinaryCheckUtils.
 */
class BinaryCheckUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("BinaryCheckUtils isBinaryByExtension null")
    void isBinaryByExtensionNull() {
        assertFalse(BinaryCheckUtils.isBinaryByExtension(null));
    }

    @Test
    @DisplayName("BinaryCheckUtils isBinaryByExtension empty")
    void isBinaryByExtensionEmpty() {
        assertFalse(BinaryCheckUtils.isBinaryByExtension(""));
    }

    @Test
    @DisplayName("BinaryCheckUtils isBinaryByExtension no extension")
    void isBinaryByExtensionNoExtension() {
        assertFalse(BinaryCheckUtils.isBinaryByExtension("filename"));
    }

    @Test
    @DisplayName("BinaryCheckUtils isBinaryByExtension dot at end")
    void isBinaryByExtensionDotAtEnd() {
        assertFalse(BinaryCheckUtils.isBinaryByExtension("filename."));
    }

    @Test
    @DisplayName("BinaryCheckUtils isBinaryByExtension image png")
    void isBinaryByExtensionPng() {
        assertTrue(BinaryCheckUtils.isBinaryByExtension("test.png"));
    }

    @Test
    @DisplayName("BinaryCheckUtils isBinaryByExtension image jpg")
    void isBinaryByExtensionJpg() {
        assertTrue(BinaryCheckUtils.isBinaryByExtension("test.jpg"));
        assertTrue(BinaryCheckUtils.isBinaryByExtension("test.jpeg"));
    }

    @Test
    @DisplayName("BinaryCheckUtils isBinaryByExtension image gif")
    void isBinaryByExtensionGif() {
        assertTrue(BinaryCheckUtils.isBinaryByExtension("test.gif"));
    }

    @Test
    @DisplayName("BinaryCheckUtils isBinaryByExtension pdf")
    void isBinaryByExtensionPdf() {
        assertTrue(BinaryCheckUtils.isBinaryByExtension("test.pdf"));
    }

    @Test
    @DisplayName("BinaryCheckUtils isBinaryByExtension zip")
    void isBinaryByExtensionZip() {
        assertTrue(BinaryCheckUtils.isBinaryByExtension("test.zip"));
    }

    @Test
    @DisplayName("BinaryCheckUtils isBinaryByExtension exe")
    void isBinaryByExtensionExe() {
        assertTrue(BinaryCheckUtils.isBinaryByExtension("test.exe"));
    }

    @Test
    @DisplayName("BinaryCheckUtils isBinaryByExtension class")
    void isBinaryByExtensionClass() {
        assertTrue(BinaryCheckUtils.isBinaryByExtension("Test.class"));
    }

    @Test
    @DisplayName("BinaryCheckUtils isBinaryByExtension jar")
    void isBinaryByExtensionJar() {
        assertTrue(BinaryCheckUtils.isBinaryByExtension("app.jar"));
    }

    @Test
    @DisplayName("BinaryCheckUtils isBinaryByExtension text txt")
    void isBinaryByExtensionTxt() {
        assertFalse(BinaryCheckUtils.isBinaryByExtension("test.txt"));
    }

    @Test
    @DisplayName("BinaryCheckUtils isBinaryByExtension text java")
    void isBinaryByExtensionJava() {
        assertFalse(BinaryCheckUtils.isBinaryByExtension("Test.java"));
    }

    @Test
    @DisplayName("BinaryCheckUtils isBinaryByExtension case insensitive")
    void isBinaryByExtensionCaseInsensitive() {
        assertTrue(BinaryCheckUtils.isBinaryByExtension("test.PNG"));
        assertTrue(BinaryCheckUtils.isBinaryByExtension("test.JPG"));
    }

    @Test
    @DisplayName("BinaryCheckUtils isBinaryByContent null")
    void isBinaryByContentNull() {
        assertFalse(BinaryCheckUtils.isBinaryByContent(null));
    }

    @Test
    @DisplayName("BinaryCheckUtils isBinaryByContent empty")
    void isBinaryByContentEmpty() {
        assertFalse(BinaryCheckUtils.isBinaryByContent(new byte[0]));
    }

    @Test
    @DisplayName("BinaryCheckUtils isBinaryByContent with null byte")
    void isBinaryByContentNullByte() {
        byte[] content = {0, 1, 2, 3};
        assertTrue(BinaryCheckUtils.isBinaryByContent(content));
    }

    @Test
    @DisplayName("BinaryCheckUtils isBinaryByContent text")
    void isBinaryByContentText() {
        byte[] content = "Hello, World!".getBytes();
        assertFalse(BinaryCheckUtils.isBinaryByContent(content));
    }

    @Test
    @DisplayName("BinaryCheckUtils isBinaryByContent with newline")
    void isBinaryByContentWithNewline() {
        byte[] content = "Hello\nWorld\r\n".getBytes();
        assertFalse(BinaryCheckUtils.isBinaryByContent(content));
    }

    @Test
    @DisplayName("BinaryCheckUtils isBinaryByContent with tab")
    void isBinaryByContentWithTab() {
        byte[] content = "Hello\tWorld".getBytes();
        assertFalse(BinaryCheckUtils.isBinaryByContent(content));
    }

    @Test
    @DisplayName("BinaryCheckUtils isBinaryByContent high non-printable ratio")
    void isBinaryByContentHighNonPrintableRatio() {
        // Create content with >30% non-printable characters (but no null bytes)
        byte[] content = new byte[100];
        for (int i = 0; i < 40; i++) {
            content[i] = 1; // Non-printable
        }
        for (int i = 40; i < 100; i++) {
            content[i] = 'A';
        }
        assertTrue(BinaryCheckUtils.isBinaryByContent(content));
    }

    @Test
    @DisplayName("BinaryCheckUtils isBinaryByContent low non-printable ratio")
    void isBinaryByContentLowNonPrintableRatio() {
        // Create content with <30% non-printable characters
        byte[] content = new byte[100];
        for (int i = 0; i < 20; i++) {
            content[i] = 1; // Non-printable
        }
        for (int i = 20; i < 100; i++) {
            content[i] = 'A';
        }
        assertFalse(BinaryCheckUtils.isBinaryByContent(content));
    }

    @Test
    @DisplayName("BinaryCheckUtils isBinary by extension")
    void isBinaryByPathExtension() throws Exception {
        Path binaryFile = tempDir.resolve("test.png");
        Files.writeString(binaryFile, "fake png");
        assertTrue(BinaryCheckUtils.isBinary(binaryFile));
    }

    @Test
    @DisplayName("BinaryCheckUtils isBinary by content")
    void isBinaryByPathContent() throws Exception {
        Path textFile = tempDir.resolve("test.unknown");
        Files.writeString(textFile, "Hello, World!");
        assertFalse(BinaryCheckUtils.isBinary(textFile));
    }

    @Test
    @DisplayName("BinaryCheckUtils isBinary with binary content")
    void isBinaryByPathBinaryContent() throws Exception {
        Path binaryFile = tempDir.resolve("test.dat");
        Files.write(binaryFile, new byte[]{0, 1, 2, 3, 0});
        assertTrue(BinaryCheckUtils.isBinary(binaryFile));
    }

    @Test
    @DisplayName("BinaryCheckUtils isText byte array")
    void isTextByteArray() {
        byte[] content = "Hello".getBytes();
        assertTrue(BinaryCheckUtils.isText(content));
    }

    @Test
    @DisplayName("BinaryCheckUtils isText byte array binary")
    void isTextByteArrayBinary() {
        byte[] content = new byte[]{0, 1, 2};
        assertFalse(BinaryCheckUtils.isText(content));
    }

    @Test
    @DisplayName("BinaryCheckUtils isText string")
    void isTextString() {
        assertTrue(BinaryCheckUtils.isText("Hello, World!"));
    }

    @Test
    @DisplayName("BinaryCheckUtils isText string null")
    void isTextStringNull() {
        assertTrue(BinaryCheckUtils.isText((String) null));
    }

    @Test
    @DisplayName("BinaryCheckUtils isText string empty")
    void isTextStringEmpty() {
        assertTrue(BinaryCheckUtils.isText(""));
    }

    @Test
    @DisplayName("BinaryCheckUtils detectFileType png")
    void detectFileTypePng() {
        byte[] content = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A};
        Optional<String> type = BinaryCheckUtils.detectFileType(content);
        assertTrue(type.isPresent());
        assertEquals("png", type.get());
    }

    @Test
    @DisplayName("BinaryCheckUtils detectFileType jpg")
    void detectFileTypeJpg() {
        byte[] content = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
        Optional<String> type = BinaryCheckUtils.detectFileType(content);
        assertTrue(type.isPresent());
        assertEquals("jpg", type.get());
    }

    @Test
    @DisplayName("BinaryCheckUtils detectFileType gif")
    void detectFileTypeGif() {
        byte[] content = new byte[]{'G', 'I', 'F', '8'};
        Optional<String> type = BinaryCheckUtils.detectFileType(content);
        assertTrue(type.isPresent());
        assertEquals("gif", type.get());
    }

    @Test
    @DisplayName("BinaryCheckUtils detectFileType pdf")
    void detectFileTypePdf() {
        byte[] content = new byte[]{'%', 'P', 'D', 'F', '-'};
        Optional<String> type = BinaryCheckUtils.detectFileType(content);
        assertTrue(type.isPresent());
        assertEquals("pdf", type.get());
    }

    @Test
    @DisplayName("BinaryCheckUtils detectFileType zip")
    void detectFileTypeZip() {
        byte[] content = new byte[]{0x50, 0x4B, 0x03, 0x04};
        Optional<String> type = BinaryCheckUtils.detectFileType(content);
        assertTrue(type.isPresent());
        assertEquals("zip", type.get());
    }

    @Test
    @DisplayName("BinaryCheckUtils detectFileType class")
    void detectFileTypeClass() {
        byte[] content = new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE, 0x00, 0x00};
        Optional<String> type = BinaryCheckUtils.detectFileType(content);
        assertTrue(type.isPresent());
        assertEquals("class", type.get());
    }

    @Test
    @DisplayName("BinaryCheckUtils detectFileType unknown")
    void detectFileTypeUnknown() {
        byte[] content = "Hello, World!".getBytes();
        Optional<String> type = BinaryCheckUtils.detectFileType(content);
        assertFalse(type.isPresent());
    }

    @Test
    @DisplayName("BinaryCheckUtils detectFileType null")
    void detectFileTypeNull() {
        Optional<String> type = BinaryCheckUtils.detectFileType(null);
        assertFalse(type.isPresent());
    }

    @Test
    @DisplayName("BinaryCheckUtils detectFileType too short")
    void detectFileTypeTooShort() {
        byte[] content = new byte[]{1, 2};
        Optional<String> type = BinaryCheckUtils.detectFileType(content);
        assertFalse(type.isPresent());
    }

    @Test
    @DisplayName("BinaryCheckUtils getMimeType png")
    void getMimeTypePng() {
        assertEquals("image/png", BinaryCheckUtils.getMimeType("test.png"));
    }

    @Test
    @DisplayName("BinaryCheckUtils getMimeType jpg")
    void getMimeTypeJpg() {
        assertEquals("image/jpeg", BinaryCheckUtils.getMimeType("test.jpg"));
        assertEquals("image/jpeg", BinaryCheckUtils.getMimeType("test.jpeg"));
    }

    @Test
    @DisplayName("BinaryCheckUtils getMimeType gif")
    void getMimeTypeGif() {
        assertEquals("image/gif", BinaryCheckUtils.getMimeType("test.gif"));
    }

    @Test
    @DisplayName("BinaryCheckUtils getMimeType svg")
    void getMimeTypeSvg() {
        assertEquals("image/svg+xml", BinaryCheckUtils.getMimeType("test.svg"));
    }

    @Test
    @DisplayName("BinaryCheckUtils getMimeType pdf")
    void getMimeTypePdf() {
        assertEquals("application/pdf", BinaryCheckUtils.getMimeType("test.pdf"));
    }

    @Test
    @DisplayName("BinaryCheckUtils getMimeType json")
    void getMimeTypeJson() {
        assertEquals("application/json", BinaryCheckUtils.getMimeType("test.json"));
    }

    @Test
    @DisplayName("BinaryCheckUtils getMimeType html")
    void getMimeTypeHtml() {
        assertEquals("text/html", BinaryCheckUtils.getMimeType("test.html"));
        assertEquals("text/html", BinaryCheckUtils.getMimeType("test.htm"));
    }

    @Test
    @DisplayName("BinaryCheckUtils getMimeType css")
    void getMimeTypeCss() {
        assertEquals("text/css", BinaryCheckUtils.getMimeType("test.css"));
    }

    @Test
    @DisplayName("BinaryCheckUtils getMimeType js")
    void getMimeTypeJs() {
        assertEquals("application/javascript", BinaryCheckUtils.getMimeType("test.js"));
    }

    @Test
    @DisplayName("BinaryCheckUtils getMimeType txt")
    void getMimeTypeTxt() {
        assertEquals("text/plain", BinaryCheckUtils.getMimeType("test.txt"));
    }

    @Test
    @DisplayName("BinaryCheckUtils getMimeType md")
    void getMimeTypeMd() {
        assertEquals("text/plain", BinaryCheckUtils.getMimeType("test.md"));
    }

    @Test
    @DisplayName("BinaryCheckUtils getMimeType unknown")
    void getMimeTypeUnknown() {
        assertEquals("application/octet-stream", BinaryCheckUtils.getMimeType("test.xyz"));
    }

    @Test
    @DisplayName("BinaryCheckUtils getMimeType no extension")
    void getMimeTypeNoExtension() {
        assertEquals("application/octet-stream", BinaryCheckUtils.getMimeType("testfile"));
    }

    @Test
    @DisplayName("BinaryCheckUtils getMimeType null")
    void getMimeTypeNull() {
        assertEquals("application/octet-stream", BinaryCheckUtils.getMimeType(null));
    }

    @Test
    @DisplayName("BinaryCheckUtils getMimeType xml")
    void getMimeTypeXml() {
        assertEquals("application/xml", BinaryCheckUtils.getMimeType("test.xml"));
    }
}