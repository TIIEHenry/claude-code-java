/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RegexUtils.
 */
class RegexUtilsTest {

    @Test
    @DisplayName("RegexUtils compile creates pattern")
    void compileWorks() {
        Pattern p = RegexUtils.compile("test");
        assertNotNull(p);
        assertTrue(p.matcher("test").matches());
    }

    @Test
    @DisplayName("RegexUtils compileIgnoreCase creates case-insensitive pattern")
    void compileIgnoreCaseWorks() {
        Pattern p = RegexUtils.compileIgnoreCase("test");
        assertTrue(p.matcher("TEST").matches());
        assertTrue(p.matcher("Test").matches());
    }

    @Test
    @DisplayName("RegexUtils compileMultiline creates multiline pattern")
    void compileMultilineWorks() {
        Pattern p = RegexUtils.compileMultiline("^test");
        assertTrue(p.matcher("line1\ntest\nline3").find());
    }

    @Test
    @DisplayName("RegexUtils compileDotall creates dotall pattern")
    void compileDotallWorks() {
        Pattern p = RegexUtils.compileDotall("a.b");
        assertTrue(p.matcher("a\nb").matches());
    }

    @Test
    @DisplayName("RegexUtils matches string pattern")
    void matchesStringPattern() {
        assertTrue(RegexUtils.matches("test", "test"));
        assertFalse(RegexUtils.matches("test", "other"));
    }

    @Test
    @DisplayName("RegexUtils matches Pattern object")
    void matchesPatternObject() {
        Pattern p = Pattern.compile("\\d+");
        assertTrue(RegexUtils.matches("123", p));
        assertFalse(RegexUtils.matches("abc", p));
    }

    @Test
    @DisplayName("RegexUtils contains finds pattern")
    void containsWorks() {
        assertTrue(RegexUtils.contains("hello world", "world"));
        assertFalse(RegexUtils.contains("hello", "world"));
    }

    @Test
    @DisplayName("RegexUtils contains with Pattern")
    void containsPattern() {
        Pattern p = Pattern.compile("\\d+");
        assertTrue(RegexUtils.contains("abc123def", p));
        assertFalse(RegexUtils.contains("abcdef", p));
    }

    @Test
    @DisplayName("RegexUtils findFirst returns first match")
    void findFirstWorks() {
        Optional<String> result = RegexUtils.findFirst("abc123def456", "\\d+");
        assertTrue(result.isPresent());
        assertEquals("123", result.get());
    }

    @Test
    @DisplayName("RegexUtils findFirst returns empty when no match")
    void findFirstNoMatch() {
        Optional<String> result = RegexUtils.findFirst("abcdef", "\\d+");
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("RegexUtils findFirstGroup returns specified group")
    void findFirstGroupWorks() {
        Optional<String> result = RegexUtils.findFirstGroup("user:john", "(\\w+):(\\w+)", 2);
        assertTrue(result.isPresent());
        assertEquals("john", result.get());
    }

    @Test
    @DisplayName("RegexUtils findAll returns all matches")
    void findAllWorks() {
        List<String> matches = RegexUtils.findAll("abc123def456ghi789", "\\d+");
        assertEquals(3, matches.size());
        assertEquals("123", matches.get(0));
        assertEquals("456", matches.get(1));
        assertEquals("789", matches.get(2));
    }

    @Test
    @DisplayName("RegexUtils findAllGroups returns all groups")
    void findAllGroupsWorks() {
        List<List<String>> groups = RegexUtils.findAllGroups("a1 b2 c3", "(\\w)(\\d)");
        assertEquals(3, groups.size());
        assertEquals("a1", groups.get(0).get(0));
        assertEquals("a", groups.get(0).get(1));
        assertEquals("1", groups.get(0).get(2));
    }

    @Test
    @DisplayName("RegexUtils findAllGroup returns specific group")
    void findAllGroupWorks() {
        List<String> digits = RegexUtils.findAllGroup("a1 b2 c3", "(\\w)(\\d)", 2);
        assertEquals(3, digits.size());
        assertEquals("1", digits.get(0));
        assertEquals("2", digits.get(1));
        assertEquals("3", digits.get(2));
    }

    @Test
    @DisplayName("RegexUtils replaceAll replaces all matches")
    void replaceAllWorks() {
        String result = RegexUtils.replaceAll("abc123def456", "\\d+", "X");
        assertEquals("abcXdefX", result);
    }

    @Test
    @DisplayName("RegexUtils replaceAll with Pattern")
    void replaceAllPattern() {
        Pattern p = Pattern.compile("\\d+");
        String result = RegexUtils.replaceAll("abc123def456", p, "NUM");
        assertEquals("abcNUMdefNUM", result);
    }

    @Test
    @DisplayName("RegexUtils replaceFirst replaces first match")
    void replaceFirstWorks() {
        String result = RegexUtils.replaceFirst("abc123def456", "\\d+", "X");
        assertEquals("abcXdef456", result);
    }

    @Test
    @DisplayName("RegexUtils replaceAll with function")
    void replaceAllFunction() {
        Function<Matcher, String> replacer = m -> "[" + m.group() + "]";
        String result = RegexUtils.replaceAll("abc123def456", "\\d+", replacer);
        assertEquals("abc[123]def[456]", result);
    }

    @Test
    @DisplayName("RegexUtils split splits string")
    void splitWorks() {
        String[] parts = RegexUtils.split("a,b,c", ",");
        assertEquals(3, parts.length);
        assertEquals("a", parts[0]);
        assertEquals("b", parts[1]);
        assertEquals("c", parts[2]);
    }

    @Test
    @DisplayName("RegexUtils split with limit")
    void splitWithLimit() {
        String[] parts = RegexUtils.split("a,b,c,d", ",", 2);
        assertEquals(2, parts.length);
        assertEquals("a", parts[0]);
        assertEquals("b,c,d", parts[1]);
    }

    @Test
    @DisplayName("RegexUtils splitAndTrim trims and filters empty")
    void splitAndTrimWorks() {
        List<String> parts = RegexUtils.splitAndTrim("a , b , , c", ",");
        assertEquals(3, parts.size());
        assertEquals("a", parts.get(0));
        assertEquals("b", parts.get(1));
        assertEquals("c", parts.get(2));
    }

    @Test
    @DisplayName("RegexUtils countMatches counts matches")
    void countMatchesWorks() {
        assertEquals(3, RegexUtils.countMatches("abc123def456ghi789", "\\d+"));
        assertEquals(0, RegexUtils.countMatches("abcdef", "\\d+"));
    }

    @Test
    @DisplayName("RegexUtils extractGroups extracts named groups")
    void extractGroupsWorks() {
        Map<String, String> groups = RegexUtils.extractGroups("user:john:age:25",
            "(\\w+):(\\w+):(\\w+):(\\d+)", "name", "value", "attr", "num");
        assertEquals("user", groups.get("name"));
        assertEquals("john", groups.get("value"));
        assertEquals("age", groups.get("attr"));
        assertEquals("25", groups.get("num"));
    }

    @Test
    @DisplayName("RegexUtils namedGroups extracts groups")
    void namedGroupsWorks() {
        Pattern p = Pattern.compile("(\\w+):(\\w+)");
        Map<String, String> groups = RegexUtils.namedGroups("user:john", p);
        assertEquals("user", groups.get("group1"));
        assertEquals("john", groups.get("group2"));
    }

    @Test
    @DisplayName("RegexUtils escape escapes special chars")
    void escapeWorks() {
        String escaped = RegexUtils.escape("a.b*c?");
        assertTrue(RegexUtils.matches("a.b*c?", escaped));
    }

    @Test
    @DisplayName("RegexUtils globToRegex converts glob to regex")
    void globToRegexWorks() {
        Pattern p = RegexUtils.globToRegex("*.java");
        assertTrue(p.matcher("Test.java").matches());
        assertFalse(p.matcher("Test.txt").matches());
    }

    @Test
    @DisplayName("RegexUtils globToRegex handles ? wildcard")
    void globToRegexQuestion() {
        Pattern p = RegexUtils.globToRegex("test?.txt");
        assertTrue(p.matcher("test1.txt").matches());
        assertTrue(p.matcher("testA.txt").matches());
        assertFalse(p.matcher("test12.txt").matches());
    }

    @Test
    @DisplayName("RegexUtils globMatches tests glob pattern")
    void globMatchesWorks() {
        assertTrue(RegexUtils.globMatches("Test.java", "*.java"));
        assertTrue(RegexUtils.globMatches("file.txt", "*.*"));
        assertFalse(RegexUtils.globMatches("file.doc", "*.txt"));
    }

    @Test
    @DisplayName("RegexUtils matchAndTransform transforms match")
    void matchAndTransformWorks() {
        Optional<Integer> result = RegexUtils.matchAndTransform("123", "\\d+",
            m -> Integer.parseInt(m.group()));
        assertTrue(result.isPresent());
        assertEquals(123, result.get());
    }

    @Test
    @DisplayName("RegexUtils forEachMatch processes each match")
    void forEachMatchWorks() {
        int[] count = {0};
        RegexUtils.forEachMatch("a1 b2 c3", "\\d+", m -> count[0]++);
        assertEquals(3, count[0]);
    }

    @Test
    @DisplayName("RegexUtils findMatchPositions returns positions")
    void findMatchPositionsWorks() {
        List<RegexUtils.MatchPosition> positions = RegexUtils.findMatchPositions("abc123def456", "\\d+");
        assertEquals(2, positions.size());
        assertEquals(3, positions.get(0).start());
        assertEquals(6, positions.get(0).end());
        assertEquals("123", positions.get(0).match());
        assertEquals(9, positions.get(1).start());
        assertEquals(12, positions.get(1).end());
        assertEquals("456", positions.get(1).match());
    }

    @Test
    @DisplayName("RegexUtils MatchPosition length calculates length")
    void matchPositionLength() {
        RegexUtils.MatchPosition pos = new RegexUtils.MatchPosition(0, 5, "test");
        assertEquals(5, pos.length());
    }

    @Test
    @DisplayName("RegexUtils isValidPattern validates patterns")
    void isValidPatternWorks() {
        assertTrue(RegexUtils.isValidPattern("\\d+"));
        assertTrue(RegexUtils.isValidPattern("[a-z]+"));
        assertFalse(RegexUtils.isValidPattern("["));
        assertFalse(RegexUtils.isValidPattern("*invalid"));
    }

    @Test
    @DisplayName("RegexUtils getPatternError returns error for invalid pattern")
    void getPatternErrorWorks() {
        assertNull(RegexUtils.getPatternError("\\d+"));
        assertNotNull(RegexUtils.getPatternError("["));
    }

    // Tests for Patterns class
    @Test
    @DisplayName("RegexUtils Patterns.EMAIL validates email")
    void emailPatternWorks() {
        assertTrue(RegexUtils.Patterns.EMAIL.matcher("test@example.com").matches());
        assertTrue(RegexUtils.Patterns.EMAIL.matcher("user.name@domain.org").matches());
        assertFalse(RegexUtils.Patterns.EMAIL.matcher("invalid").matches());
        assertFalse(RegexUtils.Patterns.EMAIL.matcher("no@domain").matches());
    }

    @Test
    @DisplayName("RegexUtils Patterns.URL validates URL")
    void urlPatternWorks() {
        assertTrue(RegexUtils.Patterns.URL.matcher("http://example.com").matches());
        assertTrue(RegexUtils.Patterns.URL.matcher("https://test.org/path").matches());
        assertFalse(RegexUtils.Patterns.URL.matcher("noturl").matches());
    }

    @Test
    @DisplayName("RegexUtils Patterns.UUID validates UUID")
    void uuidPatternWorks() {
        assertTrue(RegexUtils.Patterns.UUID.matcher("123e4567-e89b-12d3-a456-426614174000").matches());
        assertFalse(RegexUtils.Patterns.UUID.matcher("not-a-uuid").matches());
    }

    @Test
    @DisplayName("RegexUtils Patterns.IP_ADDRESS validates IP")
    void ipAddressPatternWorks() {
        assertTrue(RegexUtils.Patterns.IP_ADDRESS.matcher("192.168.1.1").matches());
        assertTrue(RegexUtils.Patterns.IP_ADDRESS.matcher("255.255.255.255").matches());
        assertFalse(RegexUtils.Patterns.IP_ADDRESS.matcher("256.1.1.1").matches());
    }

    @Test
    @DisplayName("RegexUtils Patterns.INTEGER validates integer")
    void integerPatternWorks() {
        assertTrue(RegexUtils.Patterns.INTEGER.matcher("123").matches());
        assertTrue(RegexUtils.Patterns.INTEGER.matcher("-456").matches());
        assertFalse(RegexUtils.Patterns.INTEGER.matcher("12.34").matches());
        assertFalse(RegexUtils.Patterns.INTEGER.matcher("abc").matches());
    }

    @Test
    @DisplayName("RegexUtils Patterns.DECIMAL validates decimal")
    void decimalPatternWorks() {
        assertTrue(RegexUtils.Patterns.DECIMAL.matcher("123").matches());
        assertTrue(RegexUtils.Patterns.DECIMAL.matcher("12.34").matches());
        assertTrue(RegexUtils.Patterns.DECIMAL.matcher("-45.67").matches());
        assertFalse(RegexUtils.Patterns.DECIMAL.matcher("abc").matches());
    }

    @Test
    @DisplayName("RegexUtils Patterns.ALPHANUMERIC validates alphanumeric")
    void alphanumericPatternWorks() {
        assertTrue(RegexUtils.Patterns.ALPHANUMERIC.matcher("abc123").matches());
        assertFalse(RegexUtils.Patterns.ALPHANUMERIC.matcher("abc-123").matches());
    }

    @Test
    @DisplayName("RegexUtils Patterns.HEX_COLOR validates hex color")
    void hexColorPatternWorks() {
        assertTrue(RegexUtils.Patterns.HEX_COLOR.matcher("#FF0000").matches());
        assertTrue(RegexUtils.Patterns.HEX_COLOR.matcher("#FFF").matches());
        assertFalse(RegexUtils.Patterns.HEX_COLOR.matcher("FF0000").matches());
    }

    @Test
    @DisplayName("RegexUtils Patterns.SEMVER validates semver")
    void semverPatternWorks() {
        assertTrue(RegexUtils.Patterns.SEMVER.matcher("1.0.0").matches());
        assertTrue(RegexUtils.Patterns.SEMVER.matcher("v2.1.3").matches());
        assertTrue(RegexUtils.Patterns.SEMVER.matcher("1.0.0-beta").matches());
        assertTrue(RegexUtils.Patterns.SEMVER.matcher("1.0.0+build").matches());
        assertFalse(RegexUtils.Patterns.SEMVER.matcher("1.0").matches());
    }

    // Tests for convenience methods
    @Test
    @DisplayName("RegexUtils isEmail validates email")
    void isEmailWorks() {
        assertTrue(RegexUtils.isEmail("test@example.com"));
        assertFalse(RegexUtils.isEmail("invalid"));
    }

    @Test
    @DisplayName("RegexUtils isUrl validates URL")
    void isUrlWorks() {
        assertTrue(RegexUtils.isUrl("http://example.com"));
        assertFalse(RegexUtils.isUrl("noturl"));
    }

    @Test
    @DisplayName("RegexUtils isUuid validates UUID")
    void isUuidWorks() {
        assertTrue(RegexUtils.isUuid("123e4567-e89b-12d3-a456-426614174000"));
        assertFalse(RegexUtils.isUuid("not-a-uuid"));
    }

    @Test
    @DisplayName("RegexUtils isInteger validates integer")
    void isIntegerWorks() {
        assertTrue(RegexUtils.isInteger("123"));
        assertTrue(RegexUtils.isInteger("-456"));
        assertFalse(RegexUtils.isInteger("12.34"));
    }

    @Test
    @DisplayName("RegexUtils isDecimal validates decimal")
    void isDecimalWorks() {
        assertTrue(RegexUtils.isDecimal("123"));
        assertTrue(RegexUtils.isDecimal("12.34"));
        assertFalse(RegexUtils.isDecimal("abc"));
    }
}