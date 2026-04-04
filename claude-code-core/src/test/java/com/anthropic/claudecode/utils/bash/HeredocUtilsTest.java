/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.bash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for HeredocUtils.
 */
class HeredocUtilsTest {

    @Test
    @DisplayName("HeredocUtils extractHeredocs null returns empty result")
    void extractHeredocsNull() {
        HeredocUtils.HeredocExtractionResult result = HeredocUtils.extractHeredocs(null);
        assertEquals("", result.processedCommand());
        assertTrue(result.heredocs().isEmpty());
    }

    @Test
    @DisplayName("HeredocUtils extractHeredocs empty returns empty result")
    void extractHeredocsEmpty() {
        HeredocUtils.HeredocExtractionResult result = HeredocUtils.extractHeredocs("");
        assertEquals("", result.processedCommand());
        assertTrue(result.heredocs().isEmpty());
    }

    @Test
    @DisplayName("HeredocUtils extractHeredocs no heredoc returns original")
    void extractHeredocsNoHeredoc() {
        HeredocUtils.HeredocExtractionResult result = HeredocUtils.extractHeredocs("ls -la");
        assertEquals("ls -la", result.processedCommand());
        assertTrue(result.heredocs().isEmpty());
    }

    @Test
    @DisplayName("HeredocUtils extractHeredocs with heredoc")
    void extractHeredocsWithHeredoc() {
        String command = "cat <<EOF\nhello\nEOF";
        HeredocUtils.HeredocExtractionResult result = HeredocUtils.extractHeredocs(command);
        assertNotNull(result.processedCommand());
        assertFalse(result.heredocs().isEmpty());
    }

    @Test
    @DisplayName("HeredocUtils extractHeredocs quotedOnly false extracts all")
    void extractHeredocsQuotedOnlyFalse() {
        String command = "cat <<EOF\ncontent\nEOF";
        HeredocUtils.HeredocExtractionResult result = HeredocUtils.extractHeredocs(command, false);
        assertFalse(result.heredocs().isEmpty());
    }

    @Test
    @DisplayName("HeredocUtils restoreHeredocs null parts returns empty")
    void restoreHeredocsNullParts() {
        List<String> result = HeredocUtils.restoreHeredocs(null, Map.of());
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("HeredocUtils restoreHeredocs empty heredocs returns same")
    void restoreHeredocsEmptyHeredocs() {
        List<String> parts = List.of("cat", "file.txt");
        List<String> result = HeredocUtils.restoreHeredocs(parts, Map.of());
        assertEquals(parts, result);
    }

    @Test
    @DisplayName("HeredocUtils restoreHeredocsInString null returns empty")
    void restoreHeredocsInStringNull() {
        String result = HeredocUtils.restoreHeredocsInString(null, Map.of());
        assertEquals("", result);
    }

    @Test
    @DisplayName("HeredocUtils restoreHeredocsInString empty heredocs returns same")
    void restoreHeredocsInStringEmptyHeredocs() {
        String result = HeredocUtils.restoreHeredocsInString("cat file", Map.of());
        assertEquals("cat file", result);
    }

    @Test
    @DisplayName("HeredocUtils containsHeredocPattern true")
    void containsHeredocPatternTrue() {
        assertTrue(HeredocUtils.containsHeredocPattern("cat <<EOF"));
        assertTrue(HeredocUtils.containsHeredocPattern("cat <<'EOF'"));
        assertTrue(HeredocUtils.containsHeredocPattern("cat <<-EOF"));
    }

    @Test
    @DisplayName("HeredocUtils containsHeredocPattern false")
    void containsHeredocPatternFalse() {
        assertFalse(HeredocUtils.containsHeredocPattern("ls -la"));
        assertFalse(HeredocUtils.containsHeredocPattern("echo hello"));
    }

    @Test
    @DisplayName("HeredocUtils containsHeredocPattern null returns false")
    void containsHeredocPatternNull() {
        assertFalse(HeredocUtils.containsHeredocPattern(null));
    }

    @Test
    @DisplayName("HeredocUtils HeredocInfo record")
    void heredocInfoRecord() {
        HeredocUtils.HeredocInfo info = new HeredocUtils.HeredocInfo(
            "<<EOF\ncontent\nEOF", "EOF", 0, 5, 6, 15
        );
        assertEquals("<<EOF\ncontent\nEOF", info.fullText());
        assertEquals("EOF", info.delimiter());
        assertEquals(0, info.operatorStartIndex());
        assertEquals(5, info.operatorEndIndex());
        assertEquals(6, info.contentStartIndex());
        assertEquals(15, info.contentEndIndex());
    }

    @Test
    @DisplayName("HeredocUtils HeredocExtractionResult record")
    void heredocExtractionResultRecord() {
        HeredocUtils.HeredocExtractionResult result = new HeredocUtils.HeredocExtractionResult(
            "cat __HEREDOC__", Map.of()
        );
        assertEquals("cat __HEREDOC__", result.processedCommand());
        assertTrue(result.heredocs().isEmpty());
    }
}