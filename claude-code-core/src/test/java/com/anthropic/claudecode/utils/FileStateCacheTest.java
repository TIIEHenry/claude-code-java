/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FileStateCache.
 */
class FileStateCacheTest {

    @Test
    @DisplayName("FileStateCache creates empty")
    void createsEmpty() {
        FileStateCache cache = new FileStateCache();

        assertEquals(0, cache.size());
    }

    @Test
    @DisplayName("FileStateCache creates with max size")
    void createsWithMaxSize() {
        FileStateCache cache = new FileStateCache(5);

        assertEquals(0, cache.size());
    }

    @Test
    @DisplayName("FileStateCache put and get works")
    void putGet() {
        FileStateCache cache = new FileStateCache();

        cache.put("/path/file.txt", "content", "hash123");

        assertEquals(1, cache.size());
        Optional<FileStateCache.FileState> state = cache.get("/path/file.txt");
        assertTrue(state.isPresent());
        assertEquals("content", state.get().content());
        assertEquals("hash123", state.get().hash());
    }

    @Test
    @DisplayName("FileStateCache get returns empty for missing")
    void getMissing() {
        FileStateCache cache = new FileStateCache();

        Optional<FileStateCache.FileState> state = cache.get("/missing");

        assertFalse(state.isPresent());
    }

    @Test
    @DisplayName("FileStateCache has checks existence")
    void hasWorks() {
        FileStateCache cache = new FileStateCache();

        cache.put("/path/file.txt", "content", "hash");

        assertTrue(cache.has("/path/file.txt"));
        assertFalse(cache.has("/missing"));
    }

    @Test
    @DisplayName("FileStateCache remove removes entry")
    void removeWorks() {
        FileStateCache cache = new FileStateCache();

        cache.put("/path/file.txt", "content", "hash");
        cache.remove("/path/file.txt");

        assertFalse(cache.has("/path/file.txt"));
        assertEquals(0, cache.size());
    }

    @Test
    @DisplayName("FileStateCache clear removes all")
    void clearWorks() {
        FileStateCache cache = new FileStateCache();

        cache.put("/a", "content1", "hash1");
        cache.put("/b", "content2", "hash2");
        cache.clear();

        assertEquals(0, cache.size());
    }

    @Test
    @DisplayName("FileStateCache respects max size")
    void respectsMaxSize() {
        FileStateCache cache = new FileStateCache(2);

        cache.put("/a", "content1", "hash1");
        cache.put("/b", "content2", "hash2");
        cache.put("/c", "content3", "hash3"); // Should evict oldest

        assertEquals(2, cache.size());
    }

    @Test
    @DisplayName("FileStateCache clone creates copy")
    void cloneWorks() {
        FileStateCache cache = new FileStateCache();

        cache.put("/a", "content1", "hash1");
        FileStateCache cloned = cache.clone();

        assertEquals(cache.size(), cloned.size());
        assertTrue(cloned.has("/a"));

        // Modifying original doesn't affect clone
        cache.put("/b", "content2", "hash2");
        assertEquals(2, cache.size());
        assertEquals(1, cloned.size());
    }

    @Test
    @DisplayName("FileStateCache FileState record works")
    void fileStateRecord() {
        FileStateCache.FileState state = new FileStateCache.FileState("content", "hash", 12345L);

        assertEquals("content", state.content());
        assertEquals("hash", state.hash());
        assertEquals(12345L, state.timestamp());
    }

    @Test
    @DisplayName("FileStateCache updates existing path")
    void updatesExisting() {
        FileStateCache cache = new FileStateCache();

        cache.put("/path", "old", "hash1");
        cache.put("/path", "new", "hash2");

        assertEquals(1, cache.size());
        assertEquals("new", cache.get("/path").get().content());
    }
}