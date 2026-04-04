/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DoctorContextWarnings.
 */
class DoctorContextWarningsTest {

    @Test
    @DisplayName("DoctorContextWarnings WarningType enum")
    void warningTypeEnum() {
        assertEquals(4, DoctorContextWarnings.WarningType.values().length);
    }

    @Test
    @DisplayName("DoctorContextWarnings Severity enum")
    void severityEnum() {
        assertEquals(2, DoctorContextWarnings.Severity.values().length);
    }

    @Test
    @DisplayName("DoctorContextWarnings ContextWarning record")
    void contextWarningRecord() {
        DoctorContextWarnings.ContextWarning warning = new DoctorContextWarnings.ContextWarning(
            DoctorContextWarnings.WarningType.CLAUDEMD_FILES,
            DoctorContextWarnings.Severity.WARNING,
            "Large file detected",
            List.of("detail1"),
            50000,
            40000
        );

        assertEquals(DoctorContextWarnings.WarningType.CLAUDEMD_FILES, warning.type());
        assertEquals(DoctorContextWarnings.Severity.WARNING, warning.severity());
        assertEquals("Large file detected", warning.message());
        assertEquals(50000, warning.currentValue());
        assertEquals(40000, warning.threshold());
    }

    @Test
    @DisplayName("DoctorContextWarnings ContextWarnings record")
    void contextWarningsRecord() {
        DoctorContextWarnings.ContextWarnings warnings = new DoctorContextWarnings.ContextWarnings(
            null, null, null, null
        );

        assertNull(warnings.claudeMdWarning());
        assertNull(warnings.agentWarning());
    }

    @Test
    @DisplayName("DoctorContextWarnings MemoryFile record")
    void memoryFileRecord() {
        DoctorContextWarnings.MemoryFile file = new DoctorContextWarnings.MemoryFile(
            "/path/to/file.md", "content"
        );
        assertEquals("/path/to/file.md", file.path());
        assertEquals("content", file.content());
    }

    @Test
    @DisplayName("DoctorContextWarnings McpToolInfo record")
    void mcpToolInfoRecord() {
        DoctorContextWarnings.McpToolInfo info = new DoctorContextWarnings.McpToolInfo(
            "tool_name", 1000
        );
        assertEquals("tool_name", info.name());
        assertEquals(1000, info.tokens());
    }

    @Test
    @DisplayName("DoctorContextWarnings AgentInfo record")
    void agentInfoRecord() {
        DoctorContextWarnings.AgentInfo info = new DoctorContextWarnings.AgentInfo(
            "test-agent", "When to use description", "built-in"
        );
        assertEquals("test-agent", info.agentType());
        assertEquals("When to use description", info.whenToUse());
        assertEquals("built-in", info.source());
    }

    @Test
    @DisplayName("DoctorContextWarnings checkClaudeMdFiles null")
    void checkClaudeMdFilesNull() {
        DoctorContextWarnings.ContextWarning warning = DoctorContextWarnings.checkClaudeMdFiles(null);
        assertNull(warning);
    }

    @Test
    @DisplayName("DoctorContextWarnings checkClaudeMdFiles small files")
    void checkClaudeMdFilesSmall() {
        List<DoctorContextWarnings.MemoryFile> files = List.of(
            new DoctorContextWarnings.MemoryFile("/path.md", "small content")
        );
        DoctorContextWarnings.ContextWarning warning = DoctorContextWarnings.checkClaudeMdFiles(files);
        assertNull(warning);
    }

    @Test
    @DisplayName("DoctorContextWarnings checkClaudeMdFiles large files")
    void checkClaudeMdFilesLarge() {
        String largeContent = "a".repeat(50000);
        List<DoctorContextWarnings.MemoryFile> files = List.of(
            new DoctorContextWarnings.MemoryFile("/path.md", largeContent)
        );
        DoctorContextWarnings.ContextWarning warning = DoctorContextWarnings.checkClaudeMdFiles(files);
        assertNotNull(warning);
        assertEquals(DoctorContextWarnings.WarningType.CLAUDEMD_FILES, warning.type());
    }

    @Test
    @DisplayName("DoctorContextWarnings checkAgentDescriptions null")
    void checkAgentDescriptionsNull() {
        assertNull(DoctorContextWarnings.checkAgentDescriptions(null));
    }

    @Test
    @DisplayName("DoctorContextWarnings checkAgentDescriptions empty")
    void checkAgentDescriptionsEmpty() {
        DoctorContextWarnings.AgentDefinitionsResult result =
            new DoctorContextWarnings.AgentDefinitionsResult(new ArrayList<>());
        assertNull(DoctorContextWarnings.checkAgentDescriptions(result));
    }

    @Test
    @DisplayName("DoctorContextWarnings checkMcpTools null")
    void checkMcpToolsNull() {
        assertNull(DoctorContextWarnings.checkMcpTools(null));
    }

    @Test
    @DisplayName("DoctorContextWarnings checkMcpTools empty")
    void checkMcpToolsEmpty() {
        assertNull(DoctorContextWarnings.checkMcpTools(new ArrayList<>()));
    }

    @Test
    @DisplayName("DoctorContextWarnings checkMcpTools small")
    void checkMcpToolsSmall() {
        List<DoctorContextWarnings.McpToolInfo> tools = List.of(
            new DoctorContextWarnings.McpToolInfo("tool", 100)
        );
        assertNull(DoctorContextWarnings.checkMcpTools(tools));
    }

    @Test
    @DisplayName("DoctorContextWarnings checkUnreachableRules null")
    void checkUnreachableRulesNull() {
        assertNull(DoctorContextWarnings.checkUnreachableRules(null));
    }

    @Test
    @DisplayName("DoctorContextWarnings checkUnreachableRules empty")
    void checkUnreachableRulesEmpty() {
        assertNull(DoctorContextWarnings.checkUnreachableRules(new ArrayList<>()));
    }

    @Test
    @DisplayName("DoctorContextWarnings checkUnreachableRules with rules")
    void checkUnreachableRulesWithRules() {
        List<String> rules = List.of("rule1", "rule2");
        DoctorContextWarnings.ContextWarning warning = DoctorContextWarnings.checkUnreachableRules(rules);
        assertNotNull(warning);
        assertEquals(DoctorContextWarnings.WarningType.UNREACHABLE_RULES, warning.type());
    }

    @Test
    @DisplayName("DoctorContextWarnings checkContextWarnings returns future")
    void checkContextWarningsReturnsFuture() {
        CompletableFuture<DoctorContextWarnings.ContextWarnings> future =
            DoctorContextWarnings.checkContextWarnings(null, null, null, null);
        assertNotNull(future);
    }
}