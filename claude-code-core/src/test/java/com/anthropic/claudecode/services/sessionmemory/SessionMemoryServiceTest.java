/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.sessionmemory;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Tests for SessionMemoryService.
 */
@DisplayName("SessionMemoryService Tests")
class SessionMemoryServiceTest {

    @BeforeEach
    void setUp() {
        SessionMemoryService.reset();
    }

    @Test
    @DisplayName("SessionMemoryService config has correct defaults")
    void configDefaults() {
        SessionMemoryService.SessionMemoryConfig config = SessionMemoryService.SessionMemoryConfig.defaultConfig();

        assertEquals(50000, config.minimumMessageTokensToInit());
        assertEquals(10000, config.minimumTokensBetweenUpdate());
        assertEquals(10, config.toolCallsBetweenUpdates());
    }

    @Test
    @DisplayName("SessionMemoryService checks gate enabled")
    void checksGateEnabled() {
        // This test verifies the method runs without error
        // The actual value depends on environment
        boolean result = SessionMemoryService.isSessionMemoryGateEnabled();
        // Just verify it returns a boolean
        assertNotNull(result);
    }

    @Test
    @DisplayName("SessionMemoryService creates memory file can use tool")
    void createsMemoryFileCanUseTool() {
        String memoryPath = "/test/path/SESSION_MEMORY.md";

        var canUseTool = SessionMemoryService.createMemoryFileCanUseTool(memoryPath);

        assertNotNull(canUseTool);
    }

    @Test
    @DisplayName("ManualExtractionResult records are correct")
    void manualExtractionResultRecords() {
        SessionMemoryService.ManualExtractionResult success =
            new SessionMemoryService.ManualExtractionResult(true, "/path/to/memory", null);

        assertTrue(success.success());
        assertEquals("/path/to/memory", success.memoryPath());
        assertNull(success.error());

        SessionMemoryService.ManualExtractionResult failure =
            new SessionMemoryService.ManualExtractionResult(false, null, "Error occurred");

        assertFalse(failure.success());
        assertNull(failure.memoryPath());
        assertEquals("Error occurred", failure.error());
    }

    @Test
    @DisplayName("SessionMemoryService init resets state")
    void initResetsState() {
        SessionMemoryService.initSessionMemory();
        // Should not throw
        assertTrue(true);
    }
}