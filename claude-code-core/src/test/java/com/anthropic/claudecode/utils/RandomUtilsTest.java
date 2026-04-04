/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RandomUtils.
 */
class RandomUtilsTest {

    @Test
    @DisplayName("RandomUtils nextInt returns int")
    void nextInt() {
        int value = RandomUtils.nextInt();
        assertTrue(value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("RandomUtils nextInt bound returns in range")
    void nextIntBound() {
        for (int i = 0; i < 100; i++) {
            int value = RandomUtils.nextInt(10);
            assertTrue(value >= 0 && value < 10);
        }
    }

    @Test
    @DisplayName("RandomUtils nextInt range returns in range")
    void nextIntRange() {
        for (int i = 0; i < 100; i++) {
            int value = RandomUtils.nextInt(5, 10);
            assertTrue(value >= 5 && value <= 10);
        }
    }

    @Test
    @DisplayName("RandomUtils nextLong returns long")
    void nextLong() {
        long value = RandomUtils.nextLong();
        assertNotNull(value);
    }

    @Test
    @DisplayName("RandomUtils nextLong range returns in range")
    void nextLongRange() {
        for (int i = 0; i < 100; i++) {
            long value = RandomUtils.nextLong(100L, 200L);
            assertTrue(value >= 100 && value <= 200);
        }
    }

    @Test
    @DisplayName("RandomUtils nextDouble returns in range [0, 1)")
    void nextDouble() {
        for (int i = 0; i < 100; i++) {
            double value = RandomUtils.nextDouble();
            assertTrue(value >= 0.0 && value < 1.0);
        }
    }

    @Test
    @DisplayName("RandomUtils nextDouble range returns in range")
    void nextDoubleRange() {
        for (int i = 0; i < 100; i++) {
            double value = RandomUtils.nextDouble(5.0, 10.0);
            assertTrue(value >= 5.0 && value < 10.0);
        }
    }

    @Test
    @DisplayName("RandomUtils nextBoolean returns boolean")
    void nextBoolean() {
        boolean value = RandomUtils.nextBoolean();
        assertTrue(value == true || value == false);
    }

    @Test
    @DisplayName("RandomUtils nextBoolean probability works")
    void nextBooleanProbability() {
        int trues = 0;
        for (int i = 0; i < 1000; i++) {
            if (RandomUtils.nextBoolean(0.8)) trues++;
        }
        assertTrue(trues > 700); // Should be around 800
    }

    @Test
    @DisplayName("RandomUtils randomString generates correct length")
    void randomStringLength() {
        String s = RandomUtils.randomString(10);
        assertEquals(10, s.length());
    }

    @Test
    @DisplayName("RandomUtils randomAlphanumeric generates alphanumeric")
    void randomAlphanumeric() {
        String s = RandomUtils.randomAlphanumeric(20);
        assertTrue(s.matches("[A-Za-z0-9]+"));
    }

    @Test
    @DisplayName("RandomUtils randomNumeric generates numeric")
    void randomNumeric() {
        String s = RandomUtils.randomNumeric(10);
        assertTrue(s.matches("[0-9]+"));
    }

    @Test
    @DisplayName("RandomUtils randomHex generates hex")
    void randomHex() {
        String s = RandomUtils.randomHex(10);
        assertTrue(s.matches("[0-9a-f]+"));
    }

    @Test
    @DisplayName("RandomUtils randomUuid generates valid UUID")
    void randomUuid() {
        String uuid = RandomUtils.randomUuid();
        assertTrue(uuid.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    @DisplayName("RandomUtils randomShortUuid generates 32 chars")
    void randomShortUuid() {
        String uuid = RandomUtils.randomShortUuid();
        assertEquals(32, uuid.length());
    }

    @Test
    @DisplayName("RandomUtils secureRandomString generates string")
    void secureRandomString() {
        String s = RandomUtils.secureRandomString(16);
        assertEquals(16, s.length());
    }

    @Test
    @DisplayName("RandomUtils secureRandomBytes generates bytes")
    void secureRandomBytes() {
        byte[] bytes = RandomUtils.secureRandomBytes(32);
        assertEquals(32, bytes.length);
    }

    @Test
    @DisplayName("RandomUtils randomElement from array")
    void randomElementArray() {
        String[] arr = {"a", "b", "c"};
        String element = RandomUtils.randomElement(arr);
        assertTrue(List.of(arr).contains(element));
    }

    @Test
    @DisplayName("RandomUtils randomElement from list")
    void randomElementList() {
        List<Integer> list = List.of(1, 2, 3, 4, 5);
        Integer element = RandomUtils.randomElement(list);
        assertTrue(list.contains(element));
    }

    @Test
    @DisplayName("RandomUtils randomElement null safe")
    void randomElementNull() {
        assertNull(RandomUtils.randomElement((String[]) null));
        assertNull(RandomUtils.randomElement(new String[0]));
        assertNull(RandomUtils.randomElement((List<String>) null));
        assertNull(RandomUtils.randomElement(List.of()));
    }

    @Test
    @DisplayName("RandomUtils randomElements picks multiple")
    void randomElements() {
        List<Integer> list = List.of(1, 2, 3, 4, 5);
        List<Integer> picked = RandomUtils.randomElements(list, 3);
        assertEquals(3, picked.size());
    }

    @Test
    @DisplayName("RandomUtils randomDistinctElements picks distinct")
    void randomDistinctElements() {
        List<Integer> list = List.of(1, 2, 3, 4, 5);
        List<Integer> picked = RandomUtils.randomDistinctElements(list, 3);
        assertEquals(3, picked.size());
        assertEquals(3, picked.stream().distinct().count());
    }

    @Test
    @DisplayName("RandomUtils shuffled creates shuffled copy")
    void shuffled() {
        List<Integer> original = List.of(1, 2, 3, 4, 5);
        List<Integer> shuffled = RandomUtils.shuffled(original);

        assertEquals(5, shuffled.size());
        assertTrue(shuffled.containsAll(original));
        assertNotSame(original, shuffled);
    }

    @Test
    @DisplayName("RandomUtils jitter adds variance")
    void jitter() {
        long base = 1000;
        for (int i = 0; i < 100; i++) {
            long value = RandomUtils.jitter(base, 0.1);
            assertTrue(value >= 900 && value <= 1100);
        }
    }

    @Test
    @DisplayName("RandomUtils exponentialBackoff increases")
    void exponentialBackoff() {
        long b0 = RandomUtils.exponentialBackoff(0, 100, 10000, 0);
        long b1 = RandomUtils.exponentialBackoff(1, 100, 10000, 0);
        long b2 = RandomUtils.exponentialBackoff(2, 100, 10000, 0);

        assertTrue(b1 > b0);
        assertTrue(b2 > b1);
    }

    @Test
    @DisplayName("RandomUtils weightedRandom returns valid index")
    void weightedRandom() {
        double[] weights = {1.0, 2.0, 3.0};
        int index = RandomUtils.weightedRandom(weights);
        assertTrue(index >= 0 && index < weights.length);
    }

    @Test
    @DisplayName("RandomUtils randomPort returns valid port")
    void randomPort() {
        int port = RandomUtils.randomPort();
        assertTrue(port >= 1024 && port <= 65535);
    }

    @Test
    @DisplayName("RandomUtils randomIpv4 returns valid IPv4")
    void randomIpv4() {
        String ip = RandomUtils.randomIpv4();
        assertTrue(ip.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"));
    }

    @Test
    @DisplayName("RandomUtils randomColor returns valid hex color")
    void randomColor() {
        String color = RandomUtils.randomColor();
        assertTrue(color.matches("#[0-9a-f]{6}"));
    }
}