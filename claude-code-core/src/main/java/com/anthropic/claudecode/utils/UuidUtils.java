/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/uuid
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.time.*;
import java.security.*;

/**
 * UUID utils - UUID generation utilities.
 */
public final class UuidUtils {

    /**
     * Generate random UUID.
     */
    public static String random() {
        return UUID.randomUUID().toString();
    }

    /**
     * Generate UUID without dashes.
     */
    public static String randomNoDashes() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Generate short UUID.
     */
    public static String shortUuid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Generate time-based UUID.
     */
    public static String timeBased() {
        long timestamp = Instant.now().toEpochMilli();
        byte[] random = new byte[8];
        new SecureRandom().nextBytes(random);

        // Construct time-based UUID
        String hexTime = String.format("%016x", timestamp);
        String hexRandom = bytesToHex(random);

        return String.format("%s-%s-%s-%s-%s",
            hexTime.substring(0, 8),
            hexTime.substring(8, 12),
            "4" + hexRandom.substring(0, 3),  // Version 4
            hexRandom.substring(4, 8),
            hexRandom.substring(8)
        );
    }

    /**
     * Generate deterministic UUID from string.
     */
    public static String fromString(String input) {
        return UUID.nameUUIDFromBytes(input.getBytes()).toString();
    }

    /**
     * Generate UUID from namespace and name.
     */
    public static String fromNamespace(String namespace, String name) {
        String combined = namespace + ":" + name;
        return fromString(combined);
    }

    /**
     * Validate UUID format.
     */
    public static boolean isValid(String uuid) {
        if (uuid == null) return false;

        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Validate UUID without dashes.
     */
    public static boolean isValidNoDashes(String uuid) {
        if (uuid == null || uuid.length() != 32) return false;
        return uuid.matches("[0-9a-fA-F]{32}");
    }

    /**
     * Convert bytes to hex.
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Parse UUID.
     */
    public static ParsedUuid parse(String uuid) {
        if (!isValid(uuid)) return null;

        UUID u = UUID.fromString(uuid);
        return new ParsedUuid(
            u.toString(),
            u.getMostSignificantBits(),
            u.getLeastSignificantBits(),
            (int) (u.getMostSignificantBits() >> 32),
            (int) (u.getMostSignificantBits() >> 16 & 0xFFFF),
            (int) (u.getMostSignificantBits() & 0xFFFF),
            (int) (u.getLeastSignificantBits() >> 48 & 0xFFFF),
            u.getLeastSignificantBits() & 0xFFFFFFFFFFFFL
        );
    }

    /**
     * Parsed UUID record.
     */
    public record ParsedUuid(
        String string,
        long mostSignificantBits,
        long leastSignificantBits,
        int timeLow,
        int timeMid,
        int timeHiAndVersion,
        int clockSeqHiAndReserved,
        long node
    ) {
        public int getVersion() {
            return (timeHiAndVersion >> 12) & 0xF;
        }

        public int getVariant() {
            return (clockSeqHiAndReserved >> 6) & 0x3;
        }

        public String format() {
            return String.format("%08x-%04x-%04x-%04x-%012x",
                timeLow, timeMid, timeHiAndVersion, clockSeqHiAndReserved, node);
        }
    }

    /**
     * Generate ordered UUID (sortable).
     */
    public static String ordered() {
        long timestamp = Instant.now().toEpochMilli();
        String hexTime = String.format("%016x", timestamp);
        String random = UUID.randomUUID().toString().replace("-", "").substring(16);

        return hexTime + random;
    }

    /**
     * UUID version enum.
     */
    public enum UuidVersion {
        TIME_BASED(1),
        DCE_SECURITY(2),
        NAME_BASED_MD5(3),
        RANDOM(4),
        NAME_BASED_SHA1(5);

        private final int value;

        UuidVersion(int value) {
            this.value = value;
        }

        public int getValue() { return value; }

        public static UuidVersion fromInt(int value) {
            for (UuidVersion v : values()) {
                if (v.value == value) return v;
            }
            return RANDOM;
        }
    }
}