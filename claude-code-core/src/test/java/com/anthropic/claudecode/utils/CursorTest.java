/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Cursor.
 */
class CursorTest {

    @Test
    @DisplayName("Cursor default constructor initializes to 0")
    void defaultConstructor() {
        Cursor cursor = new Cursor();
        assertEquals(0, cursor.getPosition());
        assertEquals(Long.MAX_VALUE, cursor.getLimit());
        assertFalse(cursor.isExhausted());
    }

    @Test
    @DisplayName("Cursor constructor with limit")
    void constructorWithLimit() {
        Cursor cursor = new Cursor(100);
        assertEquals(0, cursor.getPosition());
        assertEquals(100, cursor.getLimit());
    }

    @Test
    @DisplayName("Cursor getPosition and setPosition")
    void getSetPosition() {
        Cursor cursor = new Cursor();
        cursor.setPosition(50);
        assertEquals(50, cursor.getPosition());
    }

    @Test
    @DisplayName("Cursor advance increments by 1")
    void advance() {
        Cursor cursor = new Cursor();
        cursor.advance();
        assertEquals(1, cursor.getPosition());
        cursor.advance();
        assertEquals(2, cursor.getPosition());
    }

    @Test
    @DisplayName("Cursor advance by n")
    void advanceByN() {
        Cursor cursor = new Cursor();
        cursor.advance(10);
        assertEquals(10, cursor.getPosition());
    }

    @Test
    @DisplayName("Cursor getLimit and setLimit")
    void getSetLimit() {
        Cursor cursor = new Cursor();
        cursor.setLimit(500);
        assertEquals(500, cursor.getLimit());
    }

    @Test
    @DisplayName("Cursor hasMore true when not exhausted and below limit")
    void hasMoreTrue() {
        Cursor cursor = new Cursor(100);
        cursor.setPosition(50);
        assertTrue(cursor.hasMore());
    }

    @Test
    @DisplayName("Cursor hasMore false when exhausted")
    void hasMoreFalseExhausted() {
        Cursor cursor = new Cursor(100);
        cursor.markExhausted();
        assertFalse(cursor.hasMore());
    }

    @Test
    @DisplayName("Cursor hasMore false when at limit")
    void hasMoreFalseAtLimit() {
        Cursor cursor = new Cursor(100);
        cursor.setPosition(100);
        assertFalse(cursor.hasMore());
    }

    @Test
    @DisplayName("Cursor markExhausted sets exhausted")
    void markExhausted() {
        Cursor cursor = new Cursor();
        cursor.markExhausted();
        assertTrue(cursor.isExhausted());
    }

    @Test
    @DisplayName("Cursor isExhausted initially false")
    void isExhaustedInitiallyFalse() {
        Cursor cursor = new Cursor();
        assertFalse(cursor.isExhausted());
    }

    @Test
    @DisplayName("Cursor reset clears position and exhausted")
    void reset() {
        Cursor cursor = new Cursor(100);
        cursor.setPosition(50);
        cursor.markExhausted();
        cursor.reset();

        assertEquals(0, cursor.getPosition());
        assertFalse(cursor.isExhausted());
    }

    @Test
    @DisplayName("Cursor forPage creates correct cursor")
    void forPage() {
        Cursor cursor = Cursor.forPage(2, 10);
        assertEquals(20, cursor.getPosition()); // Page 2 starts at 20
        assertEquals(30, cursor.getLimit()); // Ends at 30
    }

    @Test
    @DisplayName("Cursor getRemaining returns correct value")
    void getRemaining() {
        Cursor cursor = new Cursor(100);
        cursor.setPosition(30);
        assertEquals(70, cursor.getRemaining());
    }

    @Test
    @DisplayName("Cursor getRemaining never negative")
    void getRemainingNegative() {
        Cursor cursor = new Cursor(50);
        cursor.setPosition(100);
        assertEquals(0, cursor.getRemaining());
    }

    @Test
    @DisplayName("Cursor getProgressPercent returns percentage")
    void getProgressPercent() {
        Cursor cursor = new Cursor(100);
        cursor.setPosition(25);
        assertEquals(25.0, cursor.getProgressPercent(), 0.01);
    }

    @Test
    @DisplayName("Cursor getProgressPercent zero limit")
    void getProgressPercentZeroLimit() {
        Cursor cursor = new Cursor(0);
        assertEquals(0, cursor.getProgressPercent(), 0.01);
    }

    @Test
    @DisplayName("Cursor encode produces string")
    void encode() {
        Cursor cursor = new Cursor(100);
        cursor.setPosition(50);
        String encoded = cursor.encode();
        assertTrue(encoded.contains("50"));
        assertTrue(encoded.contains("100"));
        assertTrue(encoded.contains("false"));
    }

    @Test
    @DisplayName("Cursor decode reconstructs cursor")
    void decode() {
        Cursor cursor = Cursor.decode("50:100:true");
        assertEquals(50, cursor.getPosition());
        assertEquals(100, cursor.getLimit());
        assertTrue(cursor.isExhausted());
    }

    @Test
    @DisplayName("Cursor decode null returns new cursor")
    void decodeNull() {
        Cursor cursor = Cursor.decode(null);
        assertEquals(0, cursor.getPosition());
    }

    @Test
    @DisplayName("Cursor decode empty returns new cursor")
    void decodeEmpty() {
        Cursor cursor = Cursor.decode("");
        assertEquals(0, cursor.getPosition());
    }

    @Test
    @DisplayName("Cursor decode partial string")
    void decodePartial() {
        Cursor cursor = Cursor.decode("50");
        assertEquals(50, cursor.getPosition());
        assertEquals(Long.MAX_VALUE, cursor.getLimit());
    }

    @Test
    @DisplayName("Cursor toString contains values")
    void toStringContainsValues() {
        Cursor cursor = new Cursor(100);
        cursor.setPosition(50);
        String str = cursor.toString();
        assertTrue(str.contains("50"));
        assertTrue(str.contains("100"));
    }

    @Test
    @DisplayName("Cursor encode decode roundtrip")
    void encodeDecodeRoundtrip() {
        Cursor original = new Cursor(200);
        original.setPosition(75);
        original.markExhausted();

        String encoded = original.encode();
        Cursor decoded = Cursor.decode(encoded);

        assertEquals(original.getPosition(), decoded.getPosition());
        assertEquals(original.getLimit(), decoded.getLimit());
        assertEquals(original.isExhausted(), decoded.isExhausted());
    }
}