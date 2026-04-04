/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CodeIndexing.
 */
class CodeIndexingTest {

    @Test
    @DisplayName("CodeIndexing CodeIndexingTool enum values")
    void codeIndexingToolEnum() {
        CodeIndexing.CodeIndexingTool[] tools = CodeIndexing.CodeIndexingTool.values();
        assertEquals(25, tools.length);

        assertEquals(CodeIndexing.CodeIndexingTool.SOURCEGRAPH, CodeIndexing.CodeIndexingTool.valueOf("SOURCEGRAPH"));
        assertEquals(CodeIndexing.CodeIndexingTool.CODY, CodeIndexing.CodeIndexingTool.valueOf("CODY"));
        assertEquals(CodeIndexing.CodeIndexingTool.AIDER, CodeIndexing.CodeIndexingTool.valueOf("AIDER"));
        assertEquals(CodeIndexing.CodeIndexingTool.CURSOR, CodeIndexing.CodeIndexingTool.valueOf("CURSOR"));
        assertEquals(CodeIndexing.CodeIndexingTool.GITHUB_COPILOT, CodeIndexing.CodeIndexingTool.valueOf("GITHUB_COPILOT"));
    }

    @Test
    @DisplayName("CodeIndexing detectCodeIndexingFromCommand null")
    void detectFromCommandNull() {
        assertNull(CodeIndexing.detectCodeIndexingFromCommand(null));
    }

    @Test
    @DisplayName("CodeIndexing detectCodeIndexingFromCommand empty")
    void detectFromCommandEmpty() {
        assertNull(CodeIndexing.detectCodeIndexingFromCommand(""));
        assertNull(CodeIndexing.detectCodeIndexingFromCommand("   "));
    }

    @Test
    @DisplayName("CodeIndexing detectCodeIndexingFromCommand aider")
    void detectFromCommandAider() {
        assertEquals(CodeIndexing.CodeIndexingTool.AIDER,
            CodeIndexing.detectCodeIndexingFromCommand("aider"));
    }

    @Test
    @DisplayName("CodeIndexing detectCodeIndexingFromCommand cody")
    void detectFromCommandCody() {
        assertEquals(CodeIndexing.CodeIndexingTool.CODY,
            CodeIndexing.detectCodeIndexingFromCommand("cody"));
    }

    @Test
    @DisplayName("CodeIndexing detectCodeIndexingFromCommand npx aider")
    void detectFromCommandNpxAider() {
        assertEquals(CodeIndexing.CodeIndexingTool.AIDER,
            CodeIndexing.detectCodeIndexingFromCommand("npx aider"));
    }

    @Test
    @DisplayName("CodeIndexing detectCodeIndexingFromCommand bunx aider")
    void detectFromCommandBunxAider() {
        assertEquals(CodeIndexing.CodeIndexingTool.AIDER,
            CodeIndexing.detectCodeIndexingFromCommand("bunx aider"));
    }

    @Test
    @DisplayName("CodeIndexing detectCodeIndexingFromCommand unknown")
    void detectFromCommandUnknown() {
        assertNull(CodeIndexing.detectCodeIndexingFromCommand("unknown-command"));
    }

    @Test
    @DisplayName("CodeIndexing detectCodeIndexingFromCommand tabby")
    void detectFromCommandTabby() {
        assertEquals(CodeIndexing.CodeIndexingTool.TABBY,
            CodeIndexing.detectCodeIndexingFromCommand("tabby"));
    }

    @Test
    @DisplayName("CodeIndexing detectCodeIndexingFromCommand augment")
    void detectFromCommandAugment() {
        assertEquals(CodeIndexing.CodeIndexingTool.AUGMENT,
            CodeIndexing.detectCodeIndexingFromCommand("augment"));
    }

    @Test
    @DisplayName("CodeIndexing detectCodeIndexingFromCommand pieces")
    void detectFromCommandPieces() {
        assertEquals(CodeIndexing.CodeIndexingTool.PIECES,
            CodeIndexing.detectCodeIndexingFromCommand("pieces"));
    }

    @Test
    @DisplayName("CodeIndexing detectCodeIndexingFromMcpTool null")
    void detectFromMcpToolNull() {
        assertNull(CodeIndexing.detectCodeIndexingFromMcpTool(null));
    }

    @Test
    @DisplayName("CodeIndexing detectCodeIndexingFromMcpTool no prefix")
    void detectFromMcpToolNoPrefix() {
        assertNull(CodeIndexing.detectCodeIndexingFromMcpTool("some-tool"));
    }

    @Test
    @DisplayName("CodeIndexing detectCodeIndexingFromMcpTool valid")
    void detectFromMcpToolValid() {
        assertEquals(CodeIndexing.CodeIndexingTool.AIDER,
            CodeIndexing.detectCodeIndexingFromMcpTool("mcp__aider__some_function"));
    }

    @Test
    @DisplayName("CodeIndexing detectCodeIndexingFromMcpTool cursor")
    void detectFromMcpToolCursor() {
        assertEquals(CodeIndexing.CodeIndexingTool.CURSOR,
            CodeIndexing.detectCodeIndexingFromMcpTool("mcp__cursor__search"));
    }

    @Test
    @DisplayName("CodeIndexing detectCodeIndexingFromMcpTool sourcegraph")
    void detectFromMcpToolSourcegraph() {
        assertEquals(CodeIndexing.CodeIndexingTool.SOURCEGRAPH,
            CodeIndexing.detectCodeIndexingFromMcpTool("mcp__sourcegraph__search"));
    }

    @Test
    @DisplayName("CodeIndexing detectCodeIndexingFromMcpTool too few parts")
    void detectFromMcpToolTooFewParts() {
        assertNull(CodeIndexing.detectCodeIndexingFromMcpTool("mcp__aider"));
    }

    @Test
    @DisplayName("CodeIndexing detectCodeIndexingFromMcpTool unknown server")
    void detectFromMcpToolUnknownServer() {
        assertNull(CodeIndexing.detectCodeIndexingFromMcpTool("mcp__unknown__function"));
    }

    @Test
    @DisplayName("CodeIndexing detectCodeIndexingFromMcpServerName null")
    void detectFromMcpServerNameNull() {
        assertNull(CodeIndexing.detectCodeIndexingFromMcpServerName(null));
    }

    @Test
    @DisplayName("CodeIndexing detectCodeIndexingFromMcpServerName aider")
    void detectFromMcpServerNameAider() {
        assertEquals(CodeIndexing.CodeIndexingTool.AIDER,
            CodeIndexing.detectCodeIndexingFromMcpServerName("aider"));
    }

    @Test
    @DisplayName("CodeIndexing detectCodeIndexingFromMcpServerName case insensitive")
    void detectFromMcpServerNameCaseInsensitive() {
        assertEquals(CodeIndexing.CodeIndexingTool.AIDER,
            CodeIndexing.detectCodeIndexingFromMcpServerName("AIDER"));
        assertEquals(CodeIndexing.CodeIndexingTool.CURSOR,
            CodeIndexing.detectCodeIndexingFromMcpServerName("Cursor"));
    }

    @Test
    @DisplayName("CodeIndexing detectCodeIndexingFromMcpServerName with dash")
    void detectFromMcpServerNameWithDash() {
        assertEquals(CodeIndexing.CodeIndexingTool.GITHUB_COPILOT,
            CodeIndexing.detectCodeIndexingFromMcpServerName("github-copilot"));
        assertEquals(CodeIndexing.CodeIndexingTool.CODE_INDEX_MCP,
            CodeIndexing.detectCodeIndexingFromMcpServerName("code-index-mcp"));
    }

    @Test
    @DisplayName("CodeIndexing detectCodeIndexingFromMcpServerName unknown")
    void detectFromMcpServerNameUnknown() {
        assertNull(CodeIndexing.detectCodeIndexingFromMcpServerName("unknown-server"));
    }
}