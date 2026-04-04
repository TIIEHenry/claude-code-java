/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.BitSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BitSetUtils.
 */
class BitSetUtilsTest {

    @Test
    @DisplayName("BitSetUtils of creates bit set")
    void ofWorks() {
        BitSet bs = BitSetUtils.of(1, 3, 5);

        assertTrue(bs.get(1));
        assertTrue(bs.get(3));
        assertTrue(bs.get(5));
        assertFalse(bs.get(0));
        assertFalse(bs.get(2));
    }

    @Test
    @DisplayName("BitSetUtils range creates range")
    void rangeWorks() {
        BitSet bs = BitSetUtils.range(2, 5);

        assertTrue(bs.get(2));
        assertTrue(bs.get(3));
        assertTrue(bs.get(4));
        assertFalse(bs.get(1));
        assertFalse(bs.get(5));
    }

    @Test
    @DisplayName("BitSetUtils union combines bit sets")
    void unionWorks() {
        BitSet a = BitSetUtils.of(1, 2);
        BitSet b = BitSetUtils.of(2, 3);

        BitSet result = BitSetUtils.union(a, b);

        assertTrue(result.get(1));
        assertTrue(result.get(2));
        assertTrue(result.get(3));
    }

    @Test
    @DisplayName("BitSetUtils intersection finds common")
    void intersectionWorks() {
        BitSet a = BitSetUtils.of(1, 2, 3);
        BitSet b = BitSetUtils.of(2, 3, 4);

        BitSet result = BitSetUtils.intersection(a, b);

        assertTrue(result.get(2));
        assertTrue(result.get(3));
        assertFalse(result.get(1));
        assertFalse(result.get(4));
    }

    @Test
    @DisplayName("BitSetUtils difference removes bits")
    void differenceWorks() {
        BitSet a = BitSetUtils.of(1, 2, 3);
        BitSet b = BitSetUtils.of(2);

        BitSet result = BitSetUtils.difference(a, b);

        assertTrue(result.get(1));
        assertTrue(result.get(3));
        assertFalse(result.get(2));
    }

    @Test
    @DisplayName("BitSetUtils symmetricDifference finds exclusive bits")
    void symmetricDifferenceWorks() {
        BitSet a = BitSetUtils.of(1, 2);
        BitSet b = BitSetUtils.of(2, 3);

        BitSet result = BitSetUtils.symmetricDifference(a, b);

        assertTrue(result.get(1));
        assertTrue(result.get(3));
        assertFalse(result.get(2));
    }

    @Test
    @DisplayName("BitSetUtils complement inverts bits")
    void complementWorks() {
        BitSet bs = BitSetUtils.of(1, 3);

        BitSet result = BitSetUtils.complement(bs, 5);

        assertTrue(result.get(0));
        assertFalse(result.get(1));
        assertTrue(result.get(2));
        assertFalse(result.get(3));
        assertTrue(result.get(4));
    }

    @Test
    @DisplayName("BitSetUtils isEmpty checks empty")
    void isEmptyWorks() {
        assertTrue(BitSetUtils.isEmpty(new BitSet()));
        assertFalse(BitSetUtils.isEmpty(BitSetUtils.of(1)));
    }

    @Test
    @DisplayName("BitSetUtils containsAll checks subset")
    void containsAllWorks() {
        BitSet a = BitSetUtils.of(1, 2, 3);
        BitSet b = BitSetUtils.of(1, 2);

        assertTrue(BitSetUtils.containsAll(a, b));
        assertFalse(BitSetUtils.containsAll(b, a));
    }

    @Test
    @DisplayName("BitSetUtils containsAny checks overlap")
    void containsAnyWorks() {
        BitSet a = BitSetUtils.of(1, 2);
        BitSet b = BitSetUtils.of(2, 3);

        assertTrue(BitSetUtils.containsAny(a, b));

        BitSet c = BitSetUtils.of(5, 6);
        assertFalse(BitSetUtils.containsAny(a, c));
    }

    @Test
    @DisplayName("BitSetUtils count returns cardinality")
    void countWorks() {
        BitSet bs = BitSetUtils.of(1, 3, 5, 7);

        assertEquals(4, BitSetUtils.count(bs));
    }

    @Test
    @DisplayName("BitSetUtils highestSetBit returns highest")
    void highestSetBit() {
        BitSet bs = BitSetUtils.of(1, 5, 10);

        assertEquals(10, BitSetUtils.highestSetBit(bs));
    }

    @Test
    @DisplayName("BitSetUtils lowestSetBit returns lowest")
    void lowestSetBit() {
        BitSet bs = BitSetUtils.of(5, 10, 15);

        assertEquals(5, BitSetUtils.lowestSetBit(bs));
    }

    @Test
    @DisplayName("BitSetUtils toList returns list")
    void toListWorks() {
        BitSet bs = BitSetUtils.of(1, 3, 5);

        List<Integer> list = BitSetUtils.toList(bs);

        assertEquals(List.of(1, 3, 5), list);
    }

    @Test
    @DisplayName("BitSetUtils toArray returns array")
    void toArrayWorks() {
        BitSet bs = BitSetUtils.of(1, 3, 5);

        int[] arr = BitSetUtils.toArray(bs);

        assertArrayEquals(new int[]{1, 3, 5}, arr);
    }

    @Test
    @DisplayName("BitSetUtils toBinaryString returns string")
    void toBinaryStringWorks() {
        BitSet bs = BitSetUtils.of(0, 2); // Binary: 101

        assertEquals("101", BitSetUtils.toBinaryString(bs, 3));
    }

    @Test
    @DisplayName("BitSetUtils fromBinaryString parses string")
    void fromBinaryStringWorks() {
        BitSet bs = BitSetUtils.fromBinaryString("101");

        assertTrue(bs.get(0));
        assertFalse(bs.get(1));
        assertTrue(bs.get(2));
    }

    @Test
    @DisplayName("BitSetUtils toLong converts to long")
    void toLongWorks() {
        BitSet bs = BitSetUtils.of(0, 1); // Binary: 11 = 3

        assertEquals(3, BitSetUtils.toLong(bs));
    }

    @Test
    @DisplayName("BitSetUtils fromLong creates from long")
    void fromLongWorks() {
        BitSet bs = BitSetUtils.fromLong(5); // Binary: 101

        assertTrue(bs.get(0));
        assertFalse(bs.get(1));
        assertTrue(bs.get(2));
    }

    @Test
    @DisplayName("BitSetUtils flip flips bit")
    void flipBit() {
        BitSet bs = new BitSet();
        bs.set(1);

        BitSetUtils.flip(bs, 1);
        assertFalse(bs.get(1));

        BitSetUtils.flip(bs, 2);
        assertTrue(bs.get(2));
    }

    @Test
    @DisplayName("BitSetUtils flip range flips range")
    void flipRange() {
        BitSet bs = BitSetUtils.of(1, 2, 3, 4);

        BitSetUtils.flip(bs, 2, 4);

        assertTrue(bs.get(1));
        assertFalse(bs.get(2));
        assertFalse(bs.get(3));
        assertTrue(bs.get(4));
    }

    @Test
    @DisplayName("BitSetUtils shiftLeft shifts bits")
    void shiftLeftWorks() {
        BitSet bs = BitSetUtils.of(1, 2);

        BitSet shifted = BitSetUtils.shiftLeft(bs, 2);

        assertTrue(shifted.get(3));
        assertTrue(shifted.get(4));
        assertFalse(shifted.get(1));
    }

    @Test
    @DisplayName("BitSetUtils shiftRight shifts bits")
    void shiftRightWorks() {
        BitSet bs = BitSetUtils.of(3, 4);

        BitSet shifted = BitSetUtils.shiftRight(bs, 2);

        assertTrue(shifted.get(1));
        assertTrue(shifted.get(2));
        assertFalse(shifted.get(3));
    }

    @Test
    @DisplayName("BitSetUtils rotateLeft rotates bits")
    void rotateLeftWorks() {
        BitSet bs = BitSetUtils.of(0, 2); // 8-bit: 00000101

        BitSet rotated = BitSetUtils.rotateLeft(bs, 1, 8); // Should be 00001010

        assertFalse(rotated.get(0));
        assertTrue(rotated.get(1));
        assertFalse(rotated.get(2));
        assertTrue(rotated.get(3));
    }

    @Test
    @DisplayName("BitSetUtils rotateRight rotates bits")
    void rotateRightWorks() {
        BitSet bs = BitSetUtils.of(0, 2); // 8-bit: 00000101

        BitSet rotated = BitSetUtils.rotateRight(bs, 1, 8); // Should be 10000010

        assertTrue(rotated.get(7));
        assertFalse(rotated.get(0));
        assertTrue(rotated.get(1));
    }

    @Test
    @DisplayName("BitSetUtils chunks splits bit set")
    void chunksWorks() {
        BitSet bs = BitSetUtils.of(0, 1, 5, 6, 10);

        List<BitSet> chunks = BitSetUtils.chunks(bs, 4);

        assertFalse(chunks.isEmpty());
    }

    @Test
    @DisplayName("BitSetUtils compare compares bit sets")
    void compareWorks() {
        BitSet a = BitSetUtils.of(2); // 100
        BitSet b = BitSetUtils.of(1); // 010

        assertTrue(BitSetUtils.compare(a, b) > 0);
        assertTrue(BitSetUtils.compare(b, a) < 0);
        assertEquals(0, BitSetUtils.compare(a, a));
    }
}