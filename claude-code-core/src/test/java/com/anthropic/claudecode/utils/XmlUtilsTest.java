/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for XmlUtils.
 */
class XmlUtilsTest {

    @Test
    @DisplayName("XmlUtils escapeXml escapes ampersand")
    void escapeXmlAmpersand() {
        assertEquals("&amp;", XmlUtils.escapeXml("&"));
    }

    @Test
    @DisplayName("XmlUtils escapeXml escapes less than")
    void escapeXmlLessThan() {
        assertEquals("&lt;", XmlUtils.escapeXml("<"));
    }

    @Test
    @DisplayName("XmlUtils escapeXml escapes greater than")
    void escapeXmlGreaterThan() {
        assertEquals("&gt;", XmlUtils.escapeXml(">"));
    }

    @Test
    @DisplayName("XmlUtils escapeXml mixed content")
    void escapeXmlMixed() {
        String result = XmlUtils.escapeXml("a < b & c > d");
        assertEquals("a &lt; b &amp; c &gt; d", result);
    }

    @Test
    @DisplayName("XmlUtils escapeXml null returns null")
    void escapeXmlNull() {
        assertNull(XmlUtils.escapeXml(null));
    }

    @Test
    @DisplayName("XmlUtils escapeXml empty returns empty")
    void escapeXmlEmpty() {
        assertEquals("", XmlUtils.escapeXml(""));
    }

    @Test
    @DisplayName("XmlUtils escapeXml no special chars unchanged")
    void escapeXmlNoSpecial() {
        assertEquals("hello world", XmlUtils.escapeXml("hello world"));
    }

    @Test
    @DisplayName("XmlUtils escapeXmlAttr escapes double quote")
    void escapeXmlAttrDoubleQuote() {
        assertEquals("&quot;", XmlUtils.escapeXmlAttr("\""));
    }

    @Test
    @DisplayName("XmlUtils escapeXmlAttr escapes single quote")
    void escapeXmlAttrSingleQuote() {
        assertEquals("&apos;", XmlUtils.escapeXmlAttr("'"));
    }

    @Test
    @DisplayName("XmlUtils escapeXmlAttr escapes all special chars")
    void escapeXmlAttrAll() {
        String result = XmlUtils.escapeXmlAttr("<a href=\"test\">");
        assertTrue(result.contains("&lt;"));
        assertTrue(result.contains("&quot;"));
    }

    @Test
    @DisplayName("XmlUtils escapeXmlAttr null returns null")
    void escapeXmlAttrNull() {
        assertNull(XmlUtils.escapeXmlAttr(null));
    }

    @Test
    @DisplayName("XmlUtils unescapeXml reverses escaping")
    void unescapeXml() {
        assertEquals("<&>", XmlUtils.unescapeXml("&lt;&amp;&gt;"));
    }

    @Test
    @DisplayName("XmlUtils unescapeXml null returns null")
    void unescapeXmlNull() {
        assertNull(XmlUtils.unescapeXml(null));
    }

    @Test
    @DisplayName("XmlUtils unescapeXml empty returns empty")
    void unescapeXmlEmpty() {
        assertEquals("", XmlUtils.unescapeXml(""));
    }

    @Test
    @DisplayName("XmlUtils needsEscaping true for special chars")
    void needsEscapingTrue() {
        assertTrue(XmlUtils.needsEscaping("&"));
        assertTrue(XmlUtils.needsEscaping("<"));
        assertTrue(XmlUtils.needsEscaping(">"));
    }

    @Test
    @DisplayName("XmlUtils needsEscaping false for normal string")
    void needsEscapingFalse() {
        assertFalse(XmlUtils.needsEscaping("hello world"));
    }

    @Test
    @DisplayName("XmlUtils needsEscaping null returns false")
    void needsEscapingNull() {
        assertFalse(XmlUtils.needsEscaping(null));
    }

    @Test
    @DisplayName("XmlUtils needsAttrEscaping true for quotes")
    void needsAttrEscapingTrue() {
        assertTrue(XmlUtils.needsAttrEscaping("\""));
        assertTrue(XmlUtils.needsAttrEscaping("'"));
    }

    @Test
    @DisplayName("XmlUtils needsAttrEscaping false for normal")
    void needsAttrEscapingFalse() {
        assertFalse(XmlUtils.needsAttrEscaping("hello"));
    }

    @Test
    @DisplayName("XmlUtils wrapInTag simple")
    void wrapInTagSimple() {
        String result = XmlUtils.wrapInTag("div", "content");
        assertEquals("<div>content</div>", result);
    }

    @Test
    @DisplayName("XmlUtils wrapInTag escapes content")
    void wrapInTagEscapes() {
        String result = XmlUtils.wrapInTag("div", "<script>");
        assertTrue(result.contains("&lt;"));
        assertTrue(result.contains("script"));
    }

    @Test
    @DisplayName("XmlUtils wrapInTag with attributes")
    void wrapInTagWithAttributes() {
        String result = XmlUtils.wrapInTag("div", Map.of("class", "container"), "content");
        assertTrue(result.contains("class=\"container\""));
        assertTrue(result.contains(">content<"));
    }

    @Test
    @DisplayName("XmlUtils wrapInTag with null attributes")
    void wrapInTagNullAttributes() {
        String result = XmlUtils.wrapInTag("div", null, "content");
        assertEquals("<div>content</div>", result);
    }

    @Test
    @DisplayName("XmlUtils selfClosingTag with attributes")
    void selfClosingTag() {
        String result = XmlUtils.selfClosingTag("img", Map.of("src", "test.png"));
        assertTrue(result.contains("<img"));
        assertTrue(result.contains("src=\"test.png\""));
        assertTrue(result.endsWith("/>"));
    }

    @Test
    @DisplayName("XmlUtils selfClosingTag with null attributes")
    void selfClosingTagNull() {
        String result = XmlUtils.selfClosingTag("br", null);
        assertEquals("<br/>", result);
    }

    @Test
    @DisplayName("XmlUtils escapeXmlAttr escapes XML first")
    void escapeXmlAttrOrder() {
        String result = XmlUtils.escapeXmlAttr("<test\"");
        assertTrue(result.contains("&lt;"));
        assertTrue(result.contains("&quot;"));
    }

    @Test
    @DisplayName("XmlUtils roundtrip preserves content")
    void roundtrip() {
        String original = "<hello & goodbye>";
        String escaped = XmlUtils.escapeXml(original);
        String unescaped = XmlUtils.unescapeXml(escaped);
        assertEquals(original, unescaped);
    }
}