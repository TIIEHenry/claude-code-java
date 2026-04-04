/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BinaryCheck.
 */
class BinaryCheckTest {

    @Test
    @DisplayName("BinaryCheck isBinaryInstalled returns true for ls")
    void isBinaryInstalledLs() {
        // ls should exist on all Unix systems
        assertTrue(BinaryCheck.isBinaryInstalled("ls"));
    }

    @Test
    @DisplayName("BinaryCheck isBinaryInstalled returns false for non-existent command")
    void isBinaryInstalledNonExistent() {
        assertFalse(BinaryCheck.isBinaryInstalled("thisCommandDefinitelyDoesNotExist12345"));
    }

    @Test
    @DisplayName("BinaryCheck isBinaryInstalled returns false for null")
    void isBinaryInstalledNull() {
        assertFalse(BinaryCheck.isBinaryInstalled(null));
    }

    @Test
    @DisplayName("BinaryCheck isBinaryInstalled returns false for empty string")
    void isBinaryInstalledEmpty() {
        assertFalse(BinaryCheck.isBinaryInstalled(""));
    }

    @Test
    @DisplayName("BinaryCheck isBinaryInstalled returns false for whitespace")
    void isBinaryInstalledWhitespace() {
        assertFalse(BinaryCheck.isBinaryInstalled("   "));
    }

    @Test
    @DisplayName("BinaryCheck isBinaryInstalledAsync returns result")
    void isBinaryInstalledAsync() throws Exception {
        CompletableFuture<Boolean> future = BinaryCheck.isBinaryInstalledAsync("ls");

        Boolean result = future.get();

        assertTrue(result);
    }

    @Test
    @DisplayName("BinaryCheck findBinaryPath finds ls")
    void findBinaryPathLs() {
        Optional<String> path = BinaryCheck.findBinaryPath("ls");

        assertTrue(path.isPresent());
        assertTrue(path.get().contains("ls"));
    }

    @Test
    @DisplayName("BinaryCheck findBinaryPath returns empty for non-existent")
    void findBinaryPathNonExistent() {
        Optional<String> path = BinaryCheck.findBinaryPath("thisCommandDefinitelyDoesNotExist12345");

        assertFalse(path.isPresent());
    }

    @Test
    @DisplayName("BinaryCheck findBinaryPath returns empty for null")
    void findBinaryPathNull() {
        Optional<String> path = BinaryCheck.findBinaryPath(null);

        assertFalse(path.isPresent());
    }

    @Test
    @DisplayName("BinaryCheck findBinaryPath returns empty for empty string")
    void findBinaryPathEmpty() {
        Optional<String> path = BinaryCheck.findBinaryPath("");

        assertFalse(path.isPresent());
    }

    @Test
    @DisplayName("BinaryCheck checkBinaries checks multiple commands")
    void checkBinaries() {
        Map<String, Boolean> results = BinaryCheck.checkBinaries("ls", "thisCommandDefinitelyDoesNotExist12345");

        assertEquals(2, results.size());
        assertTrue(results.get("ls"));
        assertFalse(results.get("thisCommandDefinitelyDoesNotExist12345"));
    }

    @Test
    @DisplayName("BinaryCheck clearBinaryCache clears cache")
    void clearBinaryCache() {
        // Populate cache
        BinaryCheck.isBinaryInstalled("ls");

        int sizeBefore = BinaryCheck.getCacheSize();
        assertTrue(sizeBefore > 0);

        BinaryCheck.clearBinaryCache();

        assertEquals(0, BinaryCheck.getCacheSize());
    }

    @Test
    @DisplayName("BinaryCheck caches results")
    void cachesResults() {
        BinaryCheck.clearBinaryCache();

        // First call populates cache
        BinaryCheck.isBinaryInstalled("ls");
        int sizeAfterFirst = BinaryCheck.getCacheSize();

        // Second call should use cache (same size)
        BinaryCheck.isBinaryInstalled("ls");
        int sizeAfterSecond = BinaryCheck.getCacheSize();

        assertEquals(sizeAfterFirst, sizeAfterSecond);
    }
}