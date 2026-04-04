/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UuidUtils.
 */
class UuidUtilsTest {

    @Test
    @DisplayName("UuidUtils random generates valid UUID")
    void random() {
        String uuid = UuidUtils.random();

        assertTrue(UuidUtils.isValid(uuid));
        assertEquals(36, uuid.length());
    }

    @Test
    @DisplayName("UuidUtils randomNoDashes generates 32 chars")
    void randomNoDashes() {
        String uuid = UuidUtils.randomNoDashes();

        assertEquals(32, uuid.length());
        assertFalse(uuid.contains("-"));
        assertTrue(UuidUtils.isValidNoDashes(uuid));
    }

    @Test
    @DisplayName("UuidUtils shortUuid generates 8 chars")
    void shortUuid() {
        String uuid = UuidUtils.shortUuid();

        assertEquals(8, uuid.length());
    }

    @Test
    @DisplayName("UuidUtils timeBased generates valid format")
    void timeBased() {
        String uuid = UuidUtils.timeBased();

        assertNotNull(uuid);
        assertTrue(uuid.length() >= 32); // May or may not have dashes
    }

    @Test
    @DisplayName("UuidUtils fromString generates deterministic UUID")
    void fromString() {
        String uuid1 = UuidUtils.fromString("test");
        String uuid2 = UuidUtils.fromString("test");

        assertEquals(uuid1, uuid2);
        assertTrue(UuidUtils.isValid(uuid1));
    }

    @Test
    @DisplayName("UuidUtils fromNamespace combines namespace and name")
    void fromNamespace() {
        String uuid = UuidUtils.fromNamespace("user", "123");

        assertNotNull(uuid);
        assertTrue(UuidUtils.isValid(uuid));
    }

    @Test
    @DisplayName("UuidUtils isValid validates UUIDs")
    void isValid() {
        assertTrue(UuidUtils.isValid("550e8400-e29b-41d4-a716-446655440000"));
        assertTrue(UuidUtils.isValid(UuidUtils.random()));

        assertFalse(UuidUtils.isValid(null));
        assertFalse(UuidUtils.isValid(""));
        assertFalse(UuidUtils.isValid("invalid"));
        assertFalse(UuidUtils.isValid("550e8400-e29b-41d4-a716")); // Too short
    }

    @Test
    @DisplayName("UuidUtils isValidNoDashes validates UUIDs without dashes")
    void isValidNoDashes() {
        assertTrue(UuidUtils.isValidNoDashes("550e8400e29b41d4a716446655440000"));
        assertTrue(UuidUtils.isValidNoDashes(UuidUtils.randomNoDashes()));

        assertFalse(UuidUtils.isValidNoDashes(null));
        assertFalse(UuidUtils.isValidNoDashes(""));
        assertFalse(UuidUtils.isValidNoDashes("550e8400e29b41d4a716")); // Too short
        assertFalse(UuidUtils.isValidNoDashes("550e8400e29b41d4a71644665544ZZZZ")); // Invalid chars
    }

    @Test
    @DisplayName("UuidUtils parse returns ParsedUuid")
    void parse() {
        String uuid = "550e8400-e29b-41d4-a716-446655440000";
        UuidUtils.ParsedUuid parsed = UuidUtils.parse(uuid);

        assertNotNull(parsed);
        assertEquals(uuid, parsed.string());
    }

    @Test
    @DisplayName("UuidUtils parse returns null for invalid")
    void parseInvalid() {
        assertNull(UuidUtils.parse("invalid"));
    }

    @Test
    @DisplayName("UuidUtils ParsedUuid getVersion returns version")
    void parsedUuidGetVersion() {
        // Random UUID (version 4)
        String uuid = UuidUtils.random();
        UuidUtils.ParsedUuid parsed = UuidUtils.parse(uuid);

        assertEquals(4, parsed.getVersion());
    }

    @Test
    @DisplayName("UuidUtils ParsedUuid format returns formatted string")
    void parsedUuidFormat() {
        String uuid = "550e8400-e29b-41d4-a716-446655440000";
        UuidUtils.ParsedUuid parsed = UuidUtils.parse(uuid);

        String formatted = parsed.format();
        assertTrue(formatted.contains("-"));
    }

    @Test
    @DisplayName("UuidUtils ordered generates ordered UUID")
    void ordered() {
        String uuid1 = UuidUtils.ordered();
        String uuid2 = UuidUtils.ordered();

        assertEquals(32, uuid1.length());
        assertFalse(uuid1.contains("-"));

        // First 16 chars should be hex timestamp
        assertTrue(uuid1.substring(0, 16).matches("[0-9a-f]+"));
    }

    @Test
    @DisplayName("UuidUtils UuidVersion enum works")
    void uuidVersionEnum() {
        assertEquals(1, UuidUtils.UuidVersion.TIME_BASED.getValue());
        assertEquals(4, UuidUtils.UuidVersion.RANDOM.getValue());

        assertEquals(UuidUtils.UuidVersion.RANDOM, UuidUtils.UuidVersion.fromInt(4));
        assertEquals(UuidUtils.UuidVersion.RANDOM, UuidUtils.UuidVersion.fromInt(999)); // Unknown
    }
}