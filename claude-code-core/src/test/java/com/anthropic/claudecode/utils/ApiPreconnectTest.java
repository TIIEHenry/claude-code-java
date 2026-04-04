/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ApiPreconnect.
 */
class ApiPreconnectTest {

    @BeforeEach
    void setUp() {
        ApiPreconnect.reset();
    }

    @Test
    @DisplayName("ApiPreconnect wasFired initially false")
    void wasFiredInitiallyFalse() {
        assertFalse(ApiPreconnect.wasFired());
    }

    @Test
    @DisplayName("ApiPreconnect reset clears fired")
    void resetClearsFired() {
        ApiPreconnect.preconnectAnthropicApi();
        ApiPreconnect.reset();
        assertFalse(ApiPreconnect.wasFired());
    }

    @Test
    @DisplayName("ApiPreconnect preconnectAnthropicApi does not throw")
    void preconnectAnthropicApiNoThrow() {
        // Should not throw even in test environment
        assertDoesNotThrow(() -> ApiPreconnect.preconnectAnthropicApi());
    }
}
