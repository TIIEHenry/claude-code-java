/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HexUtils.
 */
class HexUtilsTest {

    @Test
    @DisplayName("HexUtils encode converts bytes to hex")
    void encodeWorks() {
        byte[] bytes = {0x12, 0x34, (byte) 0xAB};
        assertEquals("1234ab", HexUtils.encode(bytes));
    }

    @Test
    @DisplayName("HexUtils encode uppercase works")
    void encodeUppercase() {
        byte[] bytes = {0x12, 0x34, (byte) 0xAB};
        assertEquals("1234AB", HexUtils.encode(bytes, true));
    }

    @Test
    @DisplayName("HexUtils encode handles null")
    void encodeNull() {
        assertEquals("", HexUtils.encode(null));
    }

    @Test
    @DisplayName("HexUtils decode converts hex to bytes")
    void decodeWorks() {
        byte[] result = HexUtils.decode("1234ab");
        assertArrayEquals(new byte[]{0x12, 0x34, (byte) 0xAB}, result);
    }

    @Test
    @DisplayName("HexUtils decode handles 0x prefix")
    void decodeWithPrefix() {
        byte[] result = HexUtils.decode("0x1234");
        assertArrayEquals(new byte[]{0x12, 0x34}, result);
    }

    @Test
    @DisplayName("HexUtils decode handles null")
    void decodeNull() {
        assertArrayEquals(new byte[0], HexUtils.decode(null));
    }

    @Test
    @DisplayName("HexUtils decode handles empty")
    void decodeEmpty() {
        assertArrayEquals(new byte[0], HexUtils.decode(""));
    }

    @Test
    @DisplayName("HexUtils decode throws on odd length")
    void decodeOddLength() {
        assertThrows(IllegalArgumentException.class, () -> HexUtils.decode("123"));
    }

    @Test
    @DisplayName("HexUtils decode throws on invalid char")
    void decodeInvalidChar() {
        assertThrows(IllegalArgumentException.class, () -> HexUtils.decode("xy"));
    }

    @Test
    @DisplayName("HexUtils toHex converts int")
    void toHexInt() {
        assertEquals("000000ff", HexUtils.toHex(255));
        assertEquals("0000000f", HexUtils.toHex(15, 8));
    }

    @Test
    @DisplayName("HexUtils toHex converts long")
    void toHexLong() {
        assertEquals("00000000000000ff", HexUtils.toHex(255L));
    }

    @Test
    @DisplayName("HexUtils parseInt parses hex")
    void parseIntWorks() {
        assertEquals(255, HexUtils.parseInt("ff"));
        assertEquals(255, HexUtils.parseInt("0xff"));
    }

    @Test
    @DisplayName("HexUtils parseLong parses hex")
    void parseLongWorks() {
        assertEquals(255L, HexUtils.parseLong("ff"));
        assertEquals(-255L, HexUtils.parseLong("-ff"));
    }

    @Test
    @DisplayName("HexUtils isValidHex validates hex")
    void isValidHexWorks() {
        assertTrue(HexUtils.isValidHex("123abc"));
        assertTrue(HexUtils.isValidHex("0x123abc"));
        assertFalse(HexUtils.isValidHex("xyz"));
        assertFalse(HexUtils.isValidHex(null));
        assertFalse(HexUtils.isValidHex(""));
    }

    @Test
    @DisplayName("HexUtils formatWithSeparator formats with separator")
    void formatWithSeparatorWorks() {
        byte[] bytes = {0x12, 0x34};
        assertEquals("12:34", HexUtils.formatWithSeparator(bytes, ":"));
    }

    @Test
    @DisplayName("HexUtils formatMacAddress formats MAC")
    void formatMacAddressWorks() {
        byte[] bytes = {0x12, 0x34, 0x56, 0x78, 0x09, 0x0A};
        String mac = HexUtils.formatMacAddress(bytes);
        assertTrue(mac.contains(":"));
        assertEquals(17, mac.length()); // xx:xx:xx:xx:xx:xx format
    }

    @Test
    @DisplayName("HexUtils formatUuid formats UUID")
    void formatUuidWorks() {
        byte[] bytes = new byte[16];
        for (int i = 0; i < 16; i++) bytes[i] = (byte) i;
        String uuid = HexUtils.formatUuid(bytes);
        assertTrue(uuid.contains("-"));
        assertEquals(36, uuid.length());
    }

    @Test
    @DisplayName("HexUtils parseMacAddress parses MAC")
    void parseMacAddressWorks() {
        byte[] result = HexUtils.parseMacAddress("12:34:56:78:9A:BC");
        assertEquals(6, result.length);
    }

    @Test
    @DisplayName("HexUtils reverse reverses bytes")
    void reverseWorks() {
        byte[] bytes = {1, 2, 3};
        byte[] reversed = HexUtils.reverse(bytes);
        assertArrayEquals(new byte[]{3, 2, 1}, reversed);
    }

    @Test
    @DisplayName("HexUtils toHexChars converts byte")
    void toHexCharsWorks() {
        char[] chars = HexUtils.toHexChars((byte) 0xAB);
        assertEquals('a', chars[0]);
        assertEquals('b', chars[1]);
    }

    @Test
    @DisplayName("HexUtils dump creates hex dump")
    void dumpWorks() {
        byte[] bytes = "Hello".getBytes();
        String dump = HexUtils.dump(bytes);
        assertTrue(dump.contains("48 65 6c 6c 6f")); // Hex for "Hello"
    }

    @Test
    @DisplayName("HexUtils digitsNeeded counts digits")
    void digitsNeededWorks() {
        assertEquals(1, HexUtils.digitsNeeded(0));
        assertEquals(1, HexUtils.digitsNeeded(15));
        assertEquals(2, HexUtils.digitsNeeded(16));
    }

    @Test
    @DisplayName("HexUtils compare compares hex strings")
    void compareWorks() {
        assertEquals(0, HexUtils.compare("ff", "ff"));
        assertTrue(HexUtils.compare("ff", "fe") > 0);
        assertTrue(HexUtils.compare("fe", "ff") < 0);
    }
}