/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for EncodingUtils.
 */
class EncodingUtilsTest {

    @Test
    @DisplayName("EncodingUtils UTF_8 constant is correct")
    void utf8Constant() {
        assertEquals(StandardCharsets.UTF_8, EncodingUtils.UTF_8);
    }

    @Test
    @DisplayName("EncodingUtils toUtf8Bytes converts string")
    void toUtf8Bytes() {
        byte[] bytes = EncodingUtils.toUtf8Bytes("hello");
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), bytes);
    }

    @Test
    @DisplayName("EncodingUtils toUtf8Bytes handles null")
    void toUtf8BytesNull() {
        assertNull(EncodingUtils.toUtf8Bytes(null));
    }

    @Test
    @DisplayName("EncodingUtils fromUtf8Bytes converts bytes")
    void fromUtf8Bytes() {
        String str = EncodingUtils.fromUtf8Bytes("hello".getBytes());
        assertEquals("hello", str);
    }

    @Test
    @DisplayName("EncodingUtils fromUtf8Bytes handles null")
    void fromUtf8BytesNull() {
        assertNull(EncodingUtils.fromUtf8Bytes(null));
    }

    @Test
    @DisplayName("EncodingUtils detectCharset detects UTF-8")
    void detectCharsetUtf8() {
        byte[] bytes = "hello".getBytes(StandardCharsets.UTF_8);
        assertEquals(StandardCharsets.UTF_8, EncodingUtils.detectCharset(bytes));
    }

    @Test
    @DisplayName("EncodingUtils detectCharset detects ASCII-compatible")
    void detectCharsetAscii() {
        byte[] bytes = "hello".getBytes(StandardCharsets.US_ASCII);
        // ASCII bytes are also valid UTF-8
        assertEquals(StandardCharsets.UTF_8, EncodingUtils.detectCharset(bytes));
    }

    @Test
    @DisplayName("EncodingUtils detectCharset handles null")
    void detectCharsetNull() {
        assertEquals(StandardCharsets.UTF_8, EncodingUtils.detectCharset(null));
    }

    @Test
    @DisplayName("EncodingUtils detectCharset handles empty")
    void detectCharsetEmpty() {
        assertEquals(StandardCharsets.UTF_8, EncodingUtils.detectCharset(new byte[0]));
    }

    @Test
    @DisplayName("EncodingUtils isValidUtf8 validates UTF-8")
    void isValidUtf8True() {
        assertTrue(EncodingUtils.isValidUtf8("hello".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("EncodingUtils bytesToString converts bytes")
    void bytesToString() {
        String str = EncodingUtils.bytesToString("hello".getBytes());
        assertEquals("hello", str);
    }

    @Test
    @DisplayName("EncodingUtils stringToBytes converts string")
    void stringToBytes() {
        byte[] bytes = EncodingUtils.stringToBytes("hello", StandardCharsets.UTF_8);
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), bytes);
    }

    @Test
    @DisplayName("EncodingUtils isCharsetAvailable checks availability")
    void isCharsetAvailable() {
        assertTrue(EncodingUtils.isCharsetAvailable("UTF-8"));
        assertFalse(EncodingUtils.isCharsetAvailable("INVALID-CHARSET"));
    }

    @Test
    @DisplayName("EncodingUtils getAvailableCharsets returns charsets")
    void getAvailableCharsets() {
        assertTrue(EncodingUtils.getAvailableCharsets().contains("UTF-8"));
    }

    @Test
    @DisplayName("EncodingUtils detectLineEnding detects LF")
    void detectLineEndingLf() {
        assertEquals(EncodingUtils.LineEnding.LF, EncodingUtils.detectLineEnding("a\nb"));
    }

    @Test
    @DisplayName("EncodingUtils detectLineEnding detects CRLF")
    void detectLineEndingCrlf() {
        assertEquals(EncodingUtils.LineEnding.CRLF, EncodingUtils.detectLineEnding("a\r\nb"));
    }

    @Test
    @DisplayName("EncodingUtils detectLineEnding detects CR")
    void detectLineEndingCr() {
        assertEquals(EncodingUtils.LineEnding.CR, EncodingUtils.detectLineEnding("a\rb"));
    }

    @Test
    @DisplayName("EncodingUtils detectLineEnding handles empty")
    void detectLineEndingEmpty() {
        assertEquals(EncodingUtils.LineEnding.NONE, EncodingUtils.detectLineEnding(""));
    }

    @Test
    @DisplayName("EncodingUtils normalizeLineEndingsToLf normalizes")
    void normalizeLineEndingsToLf() {
        assertEquals("a\nb\nc", EncodingUtils.normalizeLineEndingsToLf("a\r\nb\nc"));
    }

    @Test
    @DisplayName("EncodingUtils normalizeLineEndingsToCrLf normalizes")
    void normalizeLineEndingsToCrLf() {
        assertEquals("a\r\nb\r\nc", EncodingUtils.normalizeLineEndingsToCrLf("a\nb\nc"));
    }

    @Test
    @DisplayName("EncodingUtils countCharacters counts characters")
    void countCharacters() {
        assertEquals(5, EncodingUtils.countCharacters("hello"));
    }

    @Test
    @DisplayName("EncodingUtils countCharacters handles null")
    void countCharactersNull() {
        assertEquals(0, EncodingUtils.countCharacters(null));
    }

    @Test
    @DisplayName("EncodingUtils countUtf8Bytes counts bytes")
    void countUtf8Bytes() {
        assertEquals(5, EncodingUtils.countUtf8Bytes("hello"));
    }

    @Test
    @DisplayName("EncodingUtils containsNonAscii detects non-ASCII")
    void containsNonAsciiTrue() {
        assertTrue(EncodingUtils.containsNonAscii("héllo"));
    }

    @Test
    @DisplayName("EncodingUtils containsNonAscii returns false for ASCII")
    void containsNonAsciiFalse() {
        assertFalse(EncodingUtils.containsNonAscii("hello"));
    }

    @Test
    @DisplayName("EncodingUtils stripNonAscii removes non-ASCII")
    void stripNonAscii() {
        assertEquals("hllo", EncodingUtils.stripNonAscii("héllo"));
    }

    @Test
    @DisplayName("EncodingUtils base64Encode encodes bytes")
    void base64Encode() {
        byte[] bytes = "hello".getBytes();
        String encoded = EncodingUtils.base64Encode(bytes);
        assertEquals(Base64.getEncoder().encodeToString(bytes), encoded);
    }

    @Test
    @DisplayName("EncodingUtils base64Decode decodes string")
    void base64Decode() {
        String encoded = Base64.getEncoder().encodeToString("hello".getBytes());
        byte[] decoded = EncodingUtils.base64Decode(encoded);
        assertArrayEquals("hello".getBytes(), decoded);
    }

    @Test
    @DisplayName("EncodingUtils base64Encode string encodes")
    void base64EncodeString() {
        String encoded = EncodingUtils.base64Encode("hello");
        assertNotNull(encoded);
    }

    @Test
    @DisplayName("EncodingUtils base64DecodeToString decodes")
    void base64DecodeToString() {
        String encoded = Base64.getEncoder().encodeToString("hello".getBytes());
        String decoded = EncodingUtils.base64DecodeToString(encoded);
        assertEquals("hello", decoded);
    }

    @Test
    @DisplayName("EncodingUtils hexEncode encodes bytes")
    void hexEncode() {
        byte[] bytes = {0x01, 0x02, 0x0f, 0x10};
        String hex = EncodingUtils.hexEncode(bytes);
        assertEquals("01020f10", hex);
    }

    @Test
    @DisplayName("EncodingUtils hexDecode decodes string")
    void hexDecode() {
        byte[] bytes = EncodingUtils.hexDecode("01020f10");
        assertArrayEquals(new byte[]{0x01, 0x02, 0x0f, 0x10}, bytes);
    }

    @Test
    @DisplayName("EncodingUtils LineEnding getChars returns chars")
    void lineEndingGetChars() {
        assertEquals("\n", EncodingUtils.LineEnding.LF.getChars());
        assertEquals("\r\n", EncodingUtils.LineEnding.CRLF.getChars());
        assertEquals("\r", EncodingUtils.LineEnding.CR.getChars());
    }
}