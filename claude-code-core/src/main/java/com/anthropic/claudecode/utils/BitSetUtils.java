/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code bit set utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.stream.*;

/**
 * Bit set utilities.
 */
public final class BitSetUtils {
    private BitSetUtils() {}

    /**
     * Create bit set from integers.
     */
    public static BitSet of(int... bits) {
        BitSet bs = new BitSet();
        for (int bit : bits) {
            bs.set(bit);
        }
        return bs;
    }

    /**
     * Create bit set from range.
     */
    public static BitSet range(int from, int to) {
        BitSet bs = new BitSet();
        bs.set(from, to);
        return bs;
    }

    /**
     * Union (OR) of bit sets.
     */
    public static BitSet union(BitSet... sets) {
        BitSet result = new BitSet();
        for (BitSet set : sets) {
            result.or(set);
        }
        return result;
    }

    /**
     * Intersection (AND) of bit sets.
     */
    public static BitSet intersection(BitSet... sets) {
        if (sets.length == 0) return new BitSet();
        BitSet result = (BitSet) sets[0].clone();
        for (int i = 1; i < sets.length; i++) {
            result.and(sets[i]);
        }
        return result;
    }

    /**
     * Difference (AND NOT) of bit sets.
     */
    public static BitSet difference(BitSet a, BitSet b) {
        BitSet result = (BitSet) a.clone();
        result.andNot(b);
        return result;
    }

    /**
     * Symmetric difference (XOR) of bit sets.
     */
    public static BitSet symmetricDifference(BitSet a, BitSet b) {
        BitSet result = (BitSet) a.clone();
        result.xor(b);
        return result;
    }

    /**
     * Complement of bit set.
     */
    public static BitSet complement(BitSet bs, int length) {
        BitSet result = new BitSet(length);
        result.set(0, length);
        result.andNot(bs);
        return result;
    }

    /**
     * Check if empty.
     */
    public static boolean isEmpty(BitSet bs) {
        return bs.isEmpty();
    }

    /**
     * Check if contains all bits.
     */
    public static boolean containsAll(BitSet bs, BitSet other) {
        BitSet temp = (BitSet) other.clone();
        temp.andNot(bs);
        return temp.isEmpty();
    }

    /**
     * Check if contains any bits.
     */
    public static boolean containsAny(BitSet bs, BitSet other) {
        return bs.intersects(other);
    }

    /**
     * Count set bits.
     */
    public static int count(BitSet bs) {
        return bs.cardinality();
    }

    /**
     * Get highest set bit.
     */
    public static int highestSetBit(BitSet bs) {
        return bs.length() - 1;
    }

    /**
     * Get lowest set bit.
     */
    public static int lowestSetBit(BitSet bs) {
        return bs.nextSetBit(0);
    }

    /**
     * Iterate over set bits.
     */
    public static IntStream stream(BitSet bs) {
        return bs.stream();
    }

    /**
     * Convert to list.
     */
    public static List<Integer> toList(BitSet bs) {
        return bs.stream().boxed().toList();
    }

    /**
     * Convert to array.
     */
    public static int[] toArray(BitSet bs) {
        return bs.stream().toArray();
    }

    /**
     * Convert to binary string.
     */
    public static String toBinaryString(BitSet bs, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = length - 1; i >= 0; i--) {
            sb.append(bs.get(i) ? '1' : '0');
        }
        return sb.toString();
    }

    /**
     * Parse from binary string.
     */
    public static BitSet fromBinaryString(String binary) {
        BitSet bs = new BitSet();
        for (int i = 0; i < binary.length(); i++) {
            char c = binary.charAt(binary.length() - 1 - i);
            if (c == '1') {
                bs.set(i);
            }
        }
        return bs;
    }

    /**
     * Convert to long.
     */
    public static long toLong(BitSet bs) {
        long[] array = bs.toLongArray();
        return array.length > 0 ? array[0] : 0L;
    }

    /**
     * From long.
     */
    public static BitSet fromLong(long value) {
        return BitSet.valueOf(new long[]{value});
    }

    /**
     * Flip bit.
     */
    public static void flip(BitSet bs, int index) {
        bs.flip(index);
    }

    /**
     * Flip range.
     */
    public static void flip(BitSet bs, int from, int to) {
        bs.flip(from, to);
    }

    /**
     * Shift left.
     */
    public static BitSet shiftLeft(BitSet bs, int shift) {
        if (shift <= 0) return (BitSet) bs.clone();
        BitSet result = new BitSet();
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            result.set(i + shift);
        }
        return result;
    }

    /**
     * Shift right.
     */
    public static BitSet shiftRight(BitSet bs, int shift) {
        if (shift <= 0) return (BitSet) bs.clone();
        BitSet result = new BitSet();
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            if (i >= shift) {
                result.set(i - shift);
            }
        }
        return result;
    }

    /**
     * Rotate left.
     */
    public static BitSet rotateLeft(BitSet bs, int shift, int size) {
        shift = shift % size;
        if (shift < 0) shift += size;
        if (shift == 0) return (BitSet) bs.clone();

        BitSet result = new BitSet(size);
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            int newPos = (i + shift) % size;
            result.set(newPos);
        }
        return result;
    }

    /**
     * Rotate right.
     */
    public static BitSet rotateRight(BitSet bs, int shift, int size) {
        return rotateLeft(bs, -shift, size);
    }

    /**
     * Get chunks of bits.
     */
    public static List<BitSet> chunks(BitSet bs, int chunkSize) {
        List<BitSet> chunks = new ArrayList<>();
        int length = bs.length();

        for (int i = 0; i < length; i += chunkSize) {
            BitSet chunk = bs.get(i, Math.min(i + chunkSize, length));
            chunks.add(chunk);
        }

        return chunks;
    }

    /**
     * Compare bit sets.
     */
    public static int compare(BitSet a, BitSet b) {
        int maxLen = Math.max(a.length(), b.length());
        for (int i = maxLen - 1; i >= 0; i--) {
            boolean aBit = a.get(i);
            boolean bBit = b.get(i);
            if (aBit != bBit) {
                return aBit ? 1 : -1;
            }
        }
        return 0;
    }
}