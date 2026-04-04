/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils.bash;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Heredoc.
 */
class HeredocTest {

    @Test
    @DisplayName("Heredoc parse returns info for heredoc command")
    void parseHeredocCommand() {
        Heredoc.HeredocInfo info = Heredoc.parse("cat <<EOF\nhello\nEOF");
        assertTrue(info.hasHeredoc());
        assertEquals("EOF", info.delimiter());
    }

    @Test
    @DisplayName("Heredoc parse returns empty for non-heredoc")
    void parseNonHeredoc() {
        Heredoc.HeredocInfo info = Heredoc.parse("echo hello");
        assertFalse(info.hasHeredoc());
    }

    @Test
    @DisplayName("Heredoc parse null returns empty")
    void parseNull() {
        Heredoc.HeredocInfo info = Heredoc.parse(null);
        assertFalse(info.hasHeredoc());
    }

    @Test
    @DisplayName("Heredoc parse strip tabs variant")
    void parseStripTabs() {
        Heredoc.HeredocInfo info = Heredoc.parse("cat <<-EOF\nhello\nEOF");
        assertTrue(info.hasHeredoc());
        assertTrue(info.stripTabs());
    }

    @Test
    @DisplayName("Heredoc hasHeredoc true for heredoc")
    void hasHeredocTrue() {
        assertTrue(Heredoc.hasHeredoc("cat <<EOF"));
        assertTrue(Heredoc.hasHeredoc("cat <<'EOF'"));
        assertTrue(Heredoc.hasHeredoc("cat <<-EOF"));
    }

    @Test
    @DisplayName("Heredoc hasHeredoc false for non-heredoc")
    void hasHeredocFalse() {
        assertFalse(Heredoc.hasHeredoc("echo hello"));
        assertFalse(Heredoc.hasHeredoc("ls -la"));
    }

    @Test
    @DisplayName("Heredoc hasHeredoc null returns false")
    void hasHeredocNull() {
        assertFalse(Heredoc.hasHeredoc(null));
    }

    @Test
    @DisplayName("Heredoc buildHeredocCommand standard")
    void buildHeredocCommandStandard() {
        String cmd = Heredoc.buildHeredocCommand("cat", "EOF", "hello", false);
        assertTrue(cmd.contains("cat << EOF"));
        assertTrue(cmd.contains("hello"));
        assertTrue(cmd.endsWith("EOF"));
    }

    @Test
    @DisplayName("Heredoc buildHeredocCommand strip tabs")
    void buildHeredocCommandStripTabs() {
        String cmd = Heredoc.buildHeredocCommand("cat", "EOF", "hello", true);
        assertTrue(cmd.contains("cat <<-"));
    }

    @Test
    @DisplayName("Heredoc stripLeadingTabs")
    void stripLeadingTabs() {
        assertEquals("hello", Heredoc.stripLeadingTabs("\thello"));
        assertEquals("hello", Heredoc.stripLeadingTabs("\t\thello"));
        assertEquals("hello", Heredoc.stripLeadingTabs("hello"));
    }

    @Test
    @DisplayName("Heredoc stripLeadingTabs null returns empty")
    void stripLeadingTabsNull() {
        assertEquals("", Heredoc.stripLeadingTabs(null));
    }

    @Test
    @DisplayName("Heredoc HeredocInfo empty")
    void heredocInfoEmpty() {
        Heredoc.HeredocInfo info = Heredoc.HeredocInfo.empty();
        assertFalse(info.hasHeredoc());
        assertEquals("", info.delimiter());
        assertEquals("", info.content());
        assertFalse(info.stripTabs());
        assertEquals(-1, info.startPosition());
    }

    @Test
    @DisplayName("Heredoc HeredocInfo hasHeredoc")
    void heredocInfoHasHeredoc() {
        Heredoc.HeredocInfo info = new Heredoc.HeredocInfo("EOF", "content", false, 5);
        assertTrue(info.hasHeredoc());
    }

    @Test
    @DisplayName("Heredoc HeredocInfo getProcessedContent no strip")
    void heredocInfoGetProcessedContentNoStrip() {
        Heredoc.HeredocInfo info = new Heredoc.HeredocInfo("EOF", "content", false, 5);
        assertEquals("content", info.getProcessedContent());
    }

    @Test
    @DisplayName("Heredoc HeredocInfo getProcessedContent with strip")
    void heredocInfoGetProcessedContentWithStrip() {
        Heredoc.HeredocInfo info = new Heredoc.HeredocInfo("EOF", "\tcontent", true, 5);
        assertEquals("content", info.getProcessedContent());
    }

    @Test
    @DisplayName("Heredoc HeredocType enum values")
    void heredocTypeEnum() {
        Heredoc.HeredocType[] types = Heredoc.HeredocType.values();
        assertEquals(3, types.length);
        assertEquals(Heredoc.HeredocType.STANDARD, Heredoc.HeredocType.valueOf("STANDARD"));
        assertEquals(Heredoc.HeredocType.STRIP_TABS, Heredoc.HeredocType.valueOf("STRIP_TABS"));
        assertEquals(Heredoc.HeredocType.QUOTED, Heredoc.HeredocType.valueOf("QUOTED"));
    }

    @Test
    @DisplayName("Heredoc detectType standard")
    void detectTypeStandard() {
        assertEquals(Heredoc.HeredocType.STANDARD, Heredoc.detectType("cat <<EOF"));
    }

    @Test
    @DisplayName("Heredoc detectType strip tabs")
    void detectTypeStripTabs() {
        assertEquals(Heredoc.HeredocType.STRIP_TABS, Heredoc.detectType("cat <<-EOF"));
    }

    @Test
    @DisplayName("Heredoc detectType quoted")
    void detectTypeQuoted() {
        assertEquals(Heredoc.HeredocType.QUOTED, Heredoc.detectType("cat <<'EOF'"));
        assertEquals(Heredoc.HeredocType.QUOTED, Heredoc.detectType("cat <<\"EOF\""));
    }

    @Test
    @DisplayName("Heredoc detectType null returns standard")
    void detectTypeNull() {
        assertEquals(Heredoc.HeredocType.STANDARD, Heredoc.detectType(null));
    }
}