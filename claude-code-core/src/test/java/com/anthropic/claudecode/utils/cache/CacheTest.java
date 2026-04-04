/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.cache;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Cache.
 */
class CacheTest {

    private Cache<String, String> cache;

    @BeforeEach
    void setUp() {
        cache = new Cache<>(10, 5000); // 10 entries, 5 second TTL
    }

    @Test
    @DisplayName("Cache get returns null for missing key")
    void getMissing() {
        assertNull(cache.get("missing"));
    }

    @Test
    @DisplayName("Cache put and get returns value")
    void putAndGet() {
        cache.put("key1", "value1");
        assertEquals("value1", cache.get("key1"));
    }

    @Test
    @DisplayName("Cache hit count increments on successful get")
    void hitCount() {
        cache.put("key1", "value1");
        cache.get("key1");
        cache.get("key1");
        assertEquals(2, cache.getHitCount());
    }

    @Test
    @DisplayName("Cache miss count increments on missing get")
    void missCount() {
        cache.get("missing1");
        cache.get("missing2");
        assertEquals(2, cache.getMissCount());
    }

    @Test
    @DisplayName("Cache remove deletes entry")
    void remove() {
        cache.put("key1", "value1");
        cache.remove("key1");
        assertNull(cache.get("key1"));
    }

    @Test
    @DisplayName("Cache clear removes all entries")
    void clear() {
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        cache.clear();
        assertEquals(0, cache.size());
        assertNull(cache.get("key1"));
        assertNull(cache.get("key2"));
    }

    @Test
    @DisplayName("Cache clear resets hit and miss counts")
    void clearResetsCounts() {
        cache.put("key1", "value1");
        cache.get("key1");
        cache.get("missing");
        cache.clear();
        assertEquals(0, cache.getHitCount());
        assertEquals(0, cache.getMissCount());
    }

    @Test
    @DisplayName("Cache size returns correct count")
    void size() {
        assertEquals(0, cache.size());
        cache.put("key1", "value1");
        assertEquals(1, cache.size());
        cache.put("key2", "value2");
        assertEquals(2, cache.size());
    }

    @Test
    @DisplayName("Cache containsKey returns true for existing key")
    void containsKeyTrue() {
        cache.put("key1", "value1");
        assertTrue(cache.containsKey("key1"));
    }

    @Test
    @DisplayName("Cache containsKey returns false for missing key")
    void containsKeyFalse() {
        assertFalse(cache.containsKey("missing"));
    }

    @Test
    @DisplayName("Cache keys returns all keys")
    void keys() {
        cache.put("key1", "value1");
        cache.put("key2", "value2");
        var keys = cache.keys();
        assertTrue(keys.contains("key1"));
        assertTrue(keys.contains("key2"));
        assertEquals(2, keys.size());
    }

    @Test
    @DisplayName("Cache evicts oldest when over max entries")
    void eviction() {
        Cache<String, String> smallCache = new Cache<>(2, 60000);
        smallCache.put("key1", "value1");
        smallCache.put("key2", "value2");
        smallCache.put("key3", "value3"); // Should evict oldest

        // One entry should be evicted
        assertTrue(smallCache.size() <= 2);
    }

    @Test
    @DisplayName("Cache handles null key gracefully")
    void nullKey() {
        try {
            cache.put(null, "value");
            // ConcurrentHashMap doesn't accept null keys, should throw
            fail("Expected NullPointerException");
        } catch (NullPointerException e) {
            assertTrue(true);
        }
    }

    @Test
    @DisplayName("Cache handles null value")
    void nullValue() {
        cache.put("key1", null);
        assertNull(cache.get("key1"));
    }

    @Test
    @DisplayName("Cache with integer keys")
    void integerKeys() {
        Cache<Integer, String> intCache = new Cache<>(5, 60000);
        intCache.put(1, "one");
        intCache.put(2, "two");
        assertEquals("one", intCache.get(1));
        assertEquals("two", intCache.get(2));
    }

    @Test
    @DisplayName("Cache with object values")
    void objectValues() {
        Cache<String, TestObject> objCache = new Cache<>(5, 60000);
        objCache.put("obj1", new TestObject("name", 42));
        TestObject obj = objCache.get("obj1");
        assertNotNull(obj);
        assertEquals("name", obj.name);
        assertEquals(42, obj.value);
    }

    @Test
    @DisplayName("Cache TTL expiration")
    void ttlExpiration() throws InterruptedException {
        Cache<String, String> ttlCache = new Cache<>(5, 100); // 100ms TTL
        ttlCache.put("key1", "value1");
        assertEquals("value1", ttlCache.get("key1"));

        Thread.sleep(150); // Wait for TTL

        assertNull(ttlCache.get("key1")); // Should be expired
    }

    @Test
    @DisplayName("Cache entry isExpired check")
    void cacheEntryExpired() throws InterruptedException {
        Cache<String, String> ttlCache = new Cache<>(5, 50);
        ttlCache.put("key1", "value1");

        Thread.sleep(100);

        assertFalse(ttlCache.containsKey("key1"));
    }

    private static class TestObject {
        final String name;
        final int value;
        TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }
    }
}