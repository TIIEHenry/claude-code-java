/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code XML utilities
 */
package com.anthropic.claudecode.utils;

import java.util.Map;

/**
 * XML/HTML escaping utilities for safe interpolation.
 */
public final class XmlUtils {
    private XmlUtils() {}

    /**
     * Escape XML/HTML special characters for safe interpolation into element
     * text content (between tags). Use when untrusted strings (process stdout,
     * user input, external data) go inside `<tag>${here}</tag>`.
     *
     * Escapes: & -> &amp;, < -> &lt;, > -> &gt;
     */
    public static String escapeXml(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '&' -> result.append("&amp;");
                case '<' -> result.append("&lt;");
                case '>' -> result.append("&gt;");
                default -> result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Escape for interpolation into a double- or single-quoted attribute value:
     * `<tag attr="${here}">`. Escapes quotes in addition to `& < >`.
     *
     * Escapes: & -> &amp;, < -> &lt;, > -> &gt;, " -> &quot;, ' -> &apos;
     */
    public static String escapeXmlAttr(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }

        String escaped = escapeXml(s);
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < escaped.length(); i++) {
            char c = escaped.charAt(i);
            switch (c) {
                case '"' -> result.append("&quot;");
                case '\'' -> result.append("&apos;");
                default -> result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * Unescape XML entities back to original characters.
     */
    public static String unescapeXml(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }

        return s.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'");
    }

    /**
     * Check if a string contains XML special characters that need escaping.
     */
    public static boolean needsEscaping(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&' || c == '<' || c == '>') {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if a string needs attribute escaping (includes quotes).
     */
    public static boolean needsAttrEscaping(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&' || c == '<' || c == '>' || c == '"' || c == '\'') {
                return true;
            }
        }
        return false;
    }

    /**
     * Wrap content in an XML tag.
     */
    public static String wrapInTag(String tagName, String content) {
        return "<" + tagName + ">" + escapeXml(content) + "</" + tagName + ">";
    }

    /**
     * Wrap content in an XML tag with attributes.
     */
    public static String wrapInTag(String tagName, Map<String, String> attributes, String content) {
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(tagName);

        if (attributes != null && !attributes.isEmpty()) {
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                sb.append(" ")
                  .append(entry.getKey())
                  .append("=\"")
                  .append(escapeXmlAttr(entry.getValue()))
                  .append("\"");
            }
        }

        sb.append(">").append(escapeXml(content)).append("</").append(tagName).append(">");
        return sb.toString();
    }

    /**
     * Create a self-closing XML tag with attributes.
     */
    public static String selfClosingTag(String tagName, Map<String, String> attributes) {
        StringBuilder sb = new StringBuilder();
        sb.append("<").append(tagName);

        if (attributes != null && !attributes.isEmpty()) {
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                sb.append(" ")
                  .append(entry.getKey())
                  .append("=\"")
                  .append(escapeXmlAttr(entry.getValue()))
                  .append("\"");
            }
        }

        sb.append("/>");
        return sb.toString();
    }
}