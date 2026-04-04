/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.constants;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SystemConstants.
 */
class SystemConstantsTest {

    @Test
    @DisplayName("SystemConstants DEFAULT_PREFIX")
    void defaultPrefix() {
        assertEquals("You are Claude Code, Anthropic's official CLI for Claude.", SystemConstants.DEFAULT_PREFIX);
    }

    @Test
    @DisplayName("SystemConstants AGENT_SDK_CLAUDE_CODE_PRESET_PREFIX")
    void agentSdkClaudeCodePresetPrefix() {
        assertTrue(SystemConstants.AGENT_SDK_CLAUDE_CODE_PRESET_PREFIX.contains("Claude Agent SDK"));
    }

    @Test
    @DisplayName("SystemConstants AGENT_SDK_PREFIX")
    void agentSdkPrefix() {
        assertTrue(SystemConstants.AGENT_SDK_PREFIX.contains("Claude Agent SDK"));
    }

    @Test
    @DisplayName("SystemConstants CLI_SYSPROMPT_PREFIXES not empty")
    void cliSyspromptPrefixesNotEmpty() {
        assertEquals(3, SystemConstants.CLI_SYSPROMPT_PREFIXES.size());
    }

    @Test
    @DisplayName("SystemConstants CLI_SYSPROMPT_PREFIXES contains default")
    void cliSyspromptPrefixesContainsDefault() {
        assertTrue(SystemConstants.CLI_SYSPROMPT_PREFIXES.contains(SystemConstants.DEFAULT_PREFIX));
    }

    @Test
    @DisplayName("SystemConstants getCLISyspromptPrefix returns default for vertex")
    void getCLISyspromptPrefixVertex() {
        String prefix = SystemConstants.getCLISyspromptPrefix("vertex", false, false);
        assertEquals(SystemConstants.DEFAULT_PREFIX, prefix);
    }

    @Test
    @DisplayName("SystemConstants getCLISyspromptPrefix returns default for interactive")
    void getCLISyspromptPrefixInteractive() {
        String prefix = SystemConstants.getCLISyspromptPrefix("anthropic", false, false);
        assertEquals(SystemConstants.DEFAULT_PREFIX, prefix);
    }

    @Test
    @DisplayName("SystemConstants getCLISyspromptPrefix returns agent SDK for non-interactive")
    void getCLISyspromptPrefixNonInteractive() {
        String prefix = SystemConstants.getCLISyspromptPrefix("anthropic", true, false);
        assertEquals(SystemConstants.AGENT_SDK_PREFIX, prefix);
    }

    @Test
    @DisplayName("SystemConstants getCLISyspromptPrefix returns preset for non-interactive with append")
    void getCLISyspromptPrefixNonInteractiveWithAppend() {
        String prefix = SystemConstants.getCLISyspromptPrefix("anthropic", true, true);
        assertEquals(SystemConstants.AGENT_SDK_CLAUDE_CODE_PRESET_PREFIX, prefix);
    }

    @Test
    @DisplayName("SystemConstants getAttributionHeader basic")
    void getAttributionHeaderBasic() {
        String header = SystemConstants.getAttributionHeader("1.0.0", "abc123", "cli", null, false);

        assertTrue(header.contains("cc_version=1.0.0.abc123"));
        assertTrue(header.contains("cc_entrypoint=cli"));
    }

    @Test
    @DisplayName("SystemConstants getAttributionHeader with native attestation")
    void getAttributionHeaderWithNativeAttestation() {
        String header = SystemConstants.getAttributionHeader("1.0.0", "abc", "cli", null, true);

        assertTrue(header.contains("cch=00000"));
    }

    @Test
    @DisplayName("SystemConstants getAttributionHeader without native attestation")
    void getAttributionHeaderWithoutNativeAttestation() {
        String header = SystemConstants.getAttributionHeader("1.0.0", "abc", "cli", null, false);

        assertFalse(header.contains("cch=00000"));
    }

    @Test
    @DisplayName("SystemConstants getAttributionHeader with workload")
    void getAttributionHeaderWithWorkload() {
        String header = SystemConstants.getAttributionHeader("1.0.0", "abc", "cli", "cron", false);

        assertTrue(header.contains("cc_workload=cron"));
    }

    @Test
    @DisplayName("SystemConstants getAttributionHeader without workload")
    void getAttributionHeaderWithoutWorkload() {
        String header = SystemConstants.getAttributionHeader("1.0.0", "abc", "cli", null, false);

        assertFalse(header.contains("cc_workload"));
    }

    @Test
    @DisplayName("SystemConstants CLI_SYSPROMPT_PREFIXES is immutable")
    void cliSyspromptPrefixesImmutable() {
        assertThrows(UnsupportedOperationException.class, () -> {
            SystemConstants.CLI_SYSPROMPT_PREFIXES.add("test");
        });
    }
}