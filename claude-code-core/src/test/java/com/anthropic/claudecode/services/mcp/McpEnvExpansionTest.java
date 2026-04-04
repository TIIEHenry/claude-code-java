/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.mcp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for McpEnvExpansion.
 */
class McpEnvExpansionTest {

    @Test
    @DisplayName("McpEnvExpansion expand handles null")
    void expandNull() {
        assertNull(McpEnvExpansion.expand((String) null));
    }

    @Test
    @DisplayName("McpEnvExpansion expand handles empty")
    void expandEmpty() {
        assertEquals("", McpEnvExpansion.expand(""));
    }

    @Test
    @DisplayName("McpEnvExpansion expand preserves plain text")
    void expandPlainText() {
        assertEquals("hello world", McpEnvExpansion.expand("hello world"));
    }

    @Test
    @DisplayName("McpEnvExpansion expand ${VAR} format")
    void expandBracedFormat() {
        String result = McpEnvExpansion.expand("Path: ${PATH}");
        // PATH env var exists on most systems
        assertNotNull(result);
        assertTrue(result.startsWith("Path: "));
    }

    @Test
    @DisplayName("McpEnvExpansion expand $VAR format")
    void expandSimpleFormat() {
        String result = McpEnvExpansion.expand("User: $USER");
        // USER env var exists on most systems
        assertNotNull(result);
        assertTrue(result.startsWith("User: "));
    }

    @Test
    @DisplayName("McpEnvExpansion expand ${VAR:-default} uses default")
    void expandDefaultFormat() {
        String result = McpEnvExpansion.expand("${NONEXISTENT_VAR_12345:-default_value}");
        assertEquals("default_value", result);
    }

    @Test
    @DisplayName("McpEnvExpansion expand ${VAR:-default} uses env if set")
    void expandDefaultFormatUsesEnv() {
        // HOME usually exists
        String result = McpEnvExpansion.expand("${HOME:-default_home}");
        assertNotNull(result);
        // Should be actual home, not default
        assertNotEquals("default_home", result);
    }

    @Test
    @DisplayName("McpEnvExpansion expand ${VAR:=default} uses default")
    void expandAssignFormat() {
        String result = McpEnvExpansion.expand("${NONEXISTENT_VAR_12345:=assigned_value}");
        assertEquals("assigned_value", result);
    }

    @Test
    @DisplayName("McpEnvExpansion expand ${VAR:?error} throws on missing")
    void expandErrorFormatThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            McpEnvExpansion.expand("${NONEXISTENT_VAR_12345:?Variable not set!}")
        );
    }

    @Test
    @DisplayName("McpEnvExpansion expand ${VAR:?error} returns value if set")
    void expandErrorFormatReturnsValue() {
        // HOME usually exists
        String result = McpEnvExpansion.expand("${HOME:?Home not set}");
        assertNotNull(result);
    }

    @Test
    @DisplayName("McpEnvExpansion expand missing var returns empty")
    void expandMissingVarReturnsEmpty() {
        String result = McpEnvExpansion.expand("${NONEXISTENT_VAR_12345}");
        assertEquals("", result);
    }

    @Test
    @DisplayName("McpEnvExpansion expand multiple variables")
    void expandMultipleVars() {
        String result = McpEnvExpansion.expand("User $USER at ${HOME}");
        assertNotNull(result);
        assertTrue(result.contains(" at "));
    }

    @Test
    @DisplayName("McpEnvExpansion expand in map")
    void expandMap() {
        Map<String, String> input = Map.of(
            "key1", "value1",
            "key2", "${HOME}"
        );

        Map<String, String> result = McpEnvExpansion.expand(input);

        assertEquals("value1", result.get("key1"));
        assertNotNull(result.get("key2"));
    }

    @Test
    @DisplayName("McpEnvExpansion expand in map null")
    void expandMapNull() {
        Map<String, String> result = McpEnvExpansion.expand((java.util.Map<String, String>) null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("McpEnvExpansion expand in list")
    void expandList() {
        List<String> input = List.of("arg1", "$USER", "${HOME}");

        List<String> result = McpEnvExpansion.expand(input);

        assertEquals(3, result.size());
        assertEquals("arg1", result.get(0));
        assertNotNull(result.get(1));
        assertNotNull(result.get(2));
    }

    @Test
    @DisplayName("McpEnvExpansion expand in list null")
    void expandListNull() {
        List<String> result = McpEnvExpansion.expand((List<String>) null);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("McpEnvExpansion containsEnvVars detects braced")
    void containsEnvVarsBraced() {
        assertTrue(McpEnvExpansion.containsEnvVars("${HOME}"));
        assertTrue(McpEnvExpansion.containsEnvVars("text ${VAR} more"));
    }

    @Test
    @DisplayName("McpEnvExpansion containsEnvVars detects simple")
    void containsEnvVarsSimple() {
        assertTrue(McpEnvExpansion.containsEnvVars("$USER"));
        assertTrue(McpEnvExpansion.containsEnvVars("text $VAR more"));
    }

    @Test
    @DisplayName("McpEnvExpansion containsEnvVars returns false for plain text")
    void containsEnvVarsPlainText() {
        assertFalse(McpEnvExpansion.containsEnvVars("hello world"));
        assertFalse(McpEnvExpansion.containsEnvVars("no variables here"));
    }

    @Test
    @DisplayName("McpEnvExpansion containsEnvVars handles null")
    void containsEnvVarsNull() {
        assertFalse(McpEnvExpansion.containsEnvVars(null));
    }

    @Test
    @DisplayName("McpEnvExpansion extractEnvVars from braced format")
    void extractEnvVarsBraced() {
        Set<String> vars = McpEnvExpansion.extractEnvVars("${HOME} and ${PATH}");

        assertTrue(vars.contains("HOME"));
        assertTrue(vars.contains("PATH"));
    }

    @Test
    @DisplayName("McpEnvExpansion extractEnvVars from simple format")
    void extractEnvVarsSimple() {
        Set<String> vars = McpEnvExpansion.extractEnvVars("$USER and $SHELL");

        assertTrue(vars.contains("USER"));
        assertTrue(vars.contains("SHELL"));
    }

    @Test
    @DisplayName("McpEnvExpansion extractEnvVars from mixed format")
    void extractEnvVarsMixed() {
        Set<String> vars = McpEnvExpansion.extractEnvVars("$USER at ${HOME}");

        assertTrue(vars.contains("USER"));
        assertTrue(vars.contains("HOME"));
    }

    @Test
    @DisplayName("McpEnvExpansion extractEnvVars with modifiers")
    void extractEnvVarsWithModifiers() {
        Set<String> vars = McpEnvExpansion.extractEnvVars("${VAR:-default}");

        assertTrue(vars.contains("VAR"));
    }

    @Test
    @DisplayName("McpEnvExpansion extractEnvVars handles null")
    void extractEnvVarsNull() {
        Set<String> vars = McpEnvExpansion.extractEnvVars(null);
        assertTrue(vars.isEmpty());
    }

    @Test
    @DisplayName("McpEnvExpansion McpServerConfig record")
    void mcpServerConfigRecord() {
        McpEnvExpansion.McpServerConfig config = new McpEnvExpansion.McpServerConfig(
            "my-server",
            "node",
            List.of("server.js"),
            Map.of("NODE_ENV", "production"),
            "/app",
            true,
            Set.of("tools")
        );

        assertEquals("my-server", config.name());
        assertEquals("node", config.command());
        assertEquals(1, config.args().size());
        assertEquals("production", config.env().get("NODE_ENV"));
        assertEquals("/app", config.cwd());
        assertTrue(config.autoStart());
        assertTrue(config.capabilities().contains("tools"));
    }
}