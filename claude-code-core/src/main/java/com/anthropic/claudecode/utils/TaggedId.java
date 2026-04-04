/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code tagged ID utilities
 */
package com.anthropic.claudecode.utils;

import java.math.BigInteger;
import java.util.UUID;

/**
 * Tagged ID encoding compatible with the API's tagged_id.py format.
 *
 * Produces IDs like "user_01PaGUP2rbg1XDh7Z9W1CEpd" from a UUID string.
 * The format is: {tag}_{version}{base58(uuid_as_128bit_int)}
 */
public final class TaggedId {
    private TaggedId() {}

    private static final String BASE_58_CHARS = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz";
    private static final String VERSION = "01";
    private static final int ENCODED_LENGTH = 22; // ceil(128 / log2(58)) = 22

    /**
     * Encode a 128-bit unsigned integer as a fixed-length base58 string.
     */
    private static String base58Encode(BigInteger n) {
        BigInteger base = BigInteger.valueOf(BASE_58_CHARS.length());
        char[] result = new char[ENCODED_LENGTH];
        java.util.Arrays.fill(result, BASE_58_CHARS.charAt(0));

        int i = ENCODED_LENGTH - 1;
        BigInteger value = n;

        while (value.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] divRem = value.divideAndRemainder(base);
            int rem = divRem[1].intValue();
            result[i] = BASE_58_CHARS.charAt(rem);
            value = divRem[0];
            i--;
        }

        return new String(result);
    }

    /**
     * Parse a UUID string (with or without hyphens) into a 128-bit BigInteger.
     */
    private static BigInteger uuidToBigInt(String uuid) {
        String hex = uuid.replace("-", "");
        if (hex.length() != 32) {
            throw new IllegalArgumentException("Invalid UUID hex length: " + hex.length());
        }
        return new BigInteger(hex, 16);
    }

    /**
     * Convert an account UUID to a tagged ID in the API's format.
     *
     * @param tag  The tag prefix (e.g. "user", "org")
     * @param uuid A UUID string (with or without hyphens)
     * @return Tagged ID string like "user_01PaGUP2rbg1XDh7Z9W1CEpd"
     */
    public static String toTaggedId(String tag, String uuid) {
        BigInteger n = uuidToBigInt(uuid);
        return tag + "_" + VERSION + base58Encode(n);
    }

    /**
     * Convert a UUID to a tagged ID.
     */
    public static String toTaggedId(String tag, UUID uuid) {
        return toTaggedId(tag, uuid.toString());
    }

    /**
     * Create a user tagged ID from a UUID.
     */
    public static String userTaggedId(String uuid) {
        return toTaggedId("user", uuid);
    }

    /**
     * Create an org tagged ID from a UUID.
     */
    public static String orgTaggedId(String uuid) {
        return toTaggedId("org", uuid);
    }

    /**
     * Create a message tagged ID from a UUID.
     */
    public static String messageTaggedId(String uuid) {
        return toTaggedId("msg", uuid);
    }

    /**
     * Create a conversation tagged ID from a UUID.
     */
    public static String conversationTaggedId(String uuid) {
        return toTaggedId("conv", uuid);
    }

    /**
     * Generate a new random tagged ID with the given tag.
     */
    public static String generate(String tag) {
        return toTaggedId(tag, UUID.randomUUID());
    }

    /**
     * Generate a new user tagged ID.
     */
    public static String generateUserId() {
        return generate("user");
    }

    /**
     * Generate a new org tagged ID.
     */
    public static String generateOrgId() {
        return generate("org");
    }

    /**
     * Generate a new message tagged ID.
     */
    public static String generateMessageId() {
        return generate("msg");
    }

    /**
     * Decode a base58 string back to BigInteger.
     */
    public static BigInteger base58Decode(String encoded) {
        BigInteger result = BigInteger.ZERO;
        BigInteger base = BigInteger.valueOf(BASE_58_CHARS.length());

        for (int i = 0; i < encoded.length(); i++) {
            int charIndex = BASE_58_CHARS.indexOf(encoded.charAt(i));
            if (charIndex < 0) {
                throw new IllegalArgumentException("Invalid base58 character: " + encoded.charAt(i));
            }
            result = result.multiply(base).add(BigInteger.valueOf(charIndex));
        }

        return result;
    }

    /**
     * Extract UUID from a tagged ID.
     */
    public static UUID extractUuid(String taggedId) {
        // Format: {tag}_{version}{base58}
        int underscoreIndex = taggedId.indexOf('_');
        if (underscoreIndex < 0) {
            throw new IllegalArgumentException("Invalid tagged ID format: " + taggedId);
        }

        String encoded = taggedId.substring(underscoreIndex + 3); // Skip tag_version
        BigInteger bigInt = base58Decode(encoded);

        String hex = bigInt.toString(16);
        // Pad to 32 characters
        while (hex.length() < 32) {
            hex = "0" + hex;
        }

        // Format as UUID
        String uuidStr = hex.substring(0, 8) + "-" +
                         hex.substring(8, 12) + "-" +
                         hex.substring(12, 16) + "-" +
                         hex.substring(16, 20) + "-" +
                         hex.substring(20);

        return UUID.fromString(uuidStr);
    }
}