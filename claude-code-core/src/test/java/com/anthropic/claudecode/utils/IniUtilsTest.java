/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for IniUtils.
 */
class IniUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("IniUtils parse parses simple INI content")
    void parseSimple() {
        String content = "key1=value1\nkey2=value2";
        Map<String, Map<String, String>> result = IniUtils.parse(content);

        assertEquals(1, result.size());
        assertEquals("value1", result.get("").get("key1"));
        assertEquals("value2", result.get("").get("key2"));
    }

    @Test
    @DisplayName("IniUtils parse parses sections")
    void parseSections() {
        String content = "[section1]\nkey1=value1\n[section2]\nkey2=value2";
        Map<String, Map<String, String>> result = IniUtils.parse(content);

        assertEquals(2, result.size());
        assertTrue(result.containsKey("section1"));
        assertTrue(result.containsKey("section2"));
        assertEquals("value1", result.get("section1").get("key1"));
        assertEquals("value2", result.get("section2").get("key2"));
    }

    @Test
    @DisplayName("IniUtils parse handles comments")
    void parseComments() {
        String content = "; comment\nkey1=value1\n# another comment\nkey2=value2";
        Map<String, Map<String, String>> result = IniUtils.parse(content);

        assertEquals(2, result.get("").size());
        assertFalse(result.get("").containsKey("comment"));
    }

    @Test
    @DisplayName("IniUtils parse handles quoted values")
    void parseQuotedValues() {
        String content = "key1=\"value with spaces\"\nkey2='single quoted'";
        Map<String, Map<String, String>> result = IniUtils.parse(content);

        assertEquals("value with spaces", result.get("").get("key1"));
        assertEquals("single quoted", result.get("").get("key2"));
    }

    @Test
    @DisplayName("IniUtils parse handles empty lines")
    void parseEmptyLines() {
        String content = "key1=value1\n\n\nkey2=value2";
        Map<String, Map<String, String>> result = IniUtils.parse(content);

        assertEquals(2, result.get("").size());
    }

    @Test
    @DisplayName("IniUtils parse handles whitespace")
    void parseWhitespace() {
        String content = "  key1  =  value1  \n  [  section  ]  \n  key2  =  value2  ";
        Map<String, Map<String, String>> result = IniUtils.parse(content);

        assertEquals("value1", result.get("").get("key1"));
        assertTrue(result.containsKey("section"));
        assertEquals("value2", result.get("section").get("key2"));
    }

    @Test
    @DisplayName("IniUtils parseFile parses INI file")
    void parseFileWorks() throws IOException {
        File file = tempDir.resolve("test.ini").toFile();
        IniUtils.write(file, Map.of(
            "", Map.of("key1", "value1"),
            "section", Map.of("key2", "value2")
        ));

        Map<String, Map<String, String>> result = IniUtils.parseFile(file);

        assertEquals("value1", result.get("").get("key1"));
        assertEquals("value2", result.get("section").get("key2"));
    }

    @Test
    @DisplayName("IniUtils format formats INI data")
    void formatWorks() {
        Map<String, Map<String, String>> data = Map.of(
            "", Map.of("key1", "value1"),
            "section", Map.of("key2", "value2")
        );

        String result = IniUtils.format(data);

        assertTrue(result.contains("key1=value1"));
        assertTrue(result.contains("[section]"));
        assertTrue(result.contains("key2=value2"));
    }

    @Test
    @DisplayName("IniUtils format quotes values with special chars")
    void formatQuotesSpecialChars() {
        Map<String, Map<String, String>> data = Map.of(
            "", Map.of("key", "value with spaces")
        );

        String result = IniUtils.format(data);

        assertTrue(result.contains("\"value with spaces\""));
    }

    @Test
    @DisplayName("IniUtils write writes INI file")
    void writeWorks() throws IOException {
        File file = tempDir.resolve("output.ini").toFile();
        Map<String, Map<String, String>> data = Map.of(
            "section", Map.of("key", "value")
        );

        IniUtils.write(file, data);

        assertTrue(file.exists());
        String content = java.nio.file.Files.readString(file.toPath());
        assertTrue(content.contains("[section]"));
        assertTrue(content.contains("key=value"));
    }

    @Test
    @DisplayName("IniUtils get retrieves value")
    void getWorks() {
        Map<String, Map<String, String>> ini = Map.of(
            "section", Map.of("key", "value")
        );

        Optional<String> result = IniUtils.get(ini, "section", "key");

        assertTrue(result.isPresent());
        assertEquals("value", result.get());
    }

    @Test
    @DisplayName("IniUtils get returns empty for missing key")
    void getMissing() {
        Map<String, Map<String, String>> ini = Map.of(
            "section", Map.of("key", "value")
        );

        Optional<String> result = IniUtils.get(ini, "section", "missing");

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("IniUtils get with default returns value")
    void getWithDefaultWorks() {
        Map<String, Map<String, String>> ini = Map.of(
            "section", Map.of("key", "value")
        );

        String result = IniUtils.get(ini, "section", "key", "default");

        assertEquals("value", result);
    }

    @Test
    @DisplayName("IniUtils get with default returns default for missing")
    void getWithDefaultMissing() {
        Map<String, Map<String, String>> ini = Map.of(
            "section", Map.of("key", "value")
        );

        String result = IniUtils.get(ini, "section", "missing", "default");

        assertEquals("default", result);
    }

    @Test
    @DisplayName("IniUtils getInt parses integer")
    void getIntWorks() {
        Map<String, Map<String, String>> ini = Map.of(
            "section", Map.of("num", "42")
        );

        int result = IniUtils.getInt(ini, "section", "num", 0);

        assertEquals(42, result);
    }

    @Test
    @DisplayName("IniUtils getInt returns default for invalid")
    void getIntInvalid() {
        Map<String, Map<String, String>> ini = Map.of(
            "section", Map.of("num", "notanumber")
        );

        int result = IniUtils.getInt(ini, "section", "num", 10);

        assertEquals(10, result);
    }

    @Test
    @DisplayName("IniUtils getInt returns default for missing")
    void getIntMissing() {
        Map<String, Map<String, String>> ini = Map.of(
            "section", Map.of("key", "value")
        );

        int result = IniUtils.getInt(ini, "section", "missing", 10);

        assertEquals(10, result);
    }

    @Test
    @DisplayName("IniUtils getBoolean parses boolean")
    void getBooleanWorks() {
        Map<String, Map<String, String>> ini = Map.of(
            "section", Map.of("flag", "true")
        );

        boolean result = IniUtils.getBoolean(ini, "section", "flag", false);

        assertTrue(result);
    }

    @Test
    @DisplayName("IniUtils getBoolean returns default for missing")
    void getBooleanMissing() {
        Map<String, Map<String, String>> ini = Map.of(
            "section", Map.of("key", "value")
        );

        boolean result = IniUtils.getBoolean(ini, "section", "missing", true);

        assertTrue(result);
    }

    @Test
    @DisplayName("IniUtils keys returns keys in section")
    void keysWorks() {
        Map<String, Map<String, String>> ini = Map.of(
            "section", Map.of("key1", "v1", "key2", "v2")
        );

        Set<String> keys = IniUtils.keys(ini, "section");

        assertEquals(2, keys.size());
        assertTrue(keys.contains("key1"));
        assertTrue(keys.contains("key2"));
    }

    @Test
    @DisplayName("IniUtils keys returns empty for missing section")
    void keysMissingSection() {
        Map<String, Map<String, String>> ini = Map.of(
            "section", Map.of("key", "value")
        );

        Set<String> keys = IniUtils.keys(ini, "missing");

        assertTrue(keys.isEmpty());
    }

    @Test
    @DisplayName("IniUtils sections returns all sections")
    void sectionsWorks() {
        Map<String, Map<String, String>> ini = Map.of(
            "", Map.of("key1", "v1"),
            "section1", Map.of("key2", "v2"),
            "section2", Map.of("key3", "v3")
        );

        Set<String> sections = IniUtils.sections(ini);

        assertEquals(2, sections.size());
        assertTrue(sections.contains("section1"));
        assertTrue(sections.contains("section2"));
        assertFalse(sections.contains(""));
    }

    @Test
    @DisplayName("IniUtils set sets value")
    void setWorks() {
        Map<String, Map<String, String>> ini = new java.util.LinkedHashMap<>();
        java.util.Map<String, String> section = new java.util.LinkedHashMap<>();
        section.put("key", "value");
        ini.put("section", section);

        IniUtils.set(ini, "section", "newkey", "newvalue");

        assertEquals("newvalue", ini.get("section").get("newkey"));
    }

    @Test
    @DisplayName("IniUtils set creates section if missing")
    void setCreatesSection() {
        Map<String, Map<String, String>> ini = new java.util.LinkedHashMap<>();

        IniUtils.set(ini, "newsection", "key", "value");

        assertTrue(ini.containsKey("newsection"));
        assertEquals("value", ini.get("newsection").get("key"));
    }

    @Test
    @DisplayName("IniUtils remove removes key")
    void removeWorks() {
        Map<String, Map<String, String>> ini = new java.util.LinkedHashMap<>();
        ini.put("section", new java.util.LinkedHashMap<>());
        ini.get("section").put("key1", "value1");
        ini.get("section").put("key2", "value2");

        IniUtils.remove(ini, "section", "key1");

        assertFalse(ini.get("section").containsKey("key1"));
        assertTrue(ini.get("section").containsKey("key2"));
    }

    @Test
    @DisplayName("IniUtils builder creates INI")
    void builderWorks() {
        Map<String, Map<String, String>> ini = IniUtils.builder()
            .put("key1", "value1")
            .section("section1")
            .put("section1", "key2", "value2")
            .build();

        assertEquals("value1", ini.get("").get("key1"));
        assertEquals("value2", ini.get("section1").get("key2"));
    }

    @Test
    @DisplayName("IniUtils builder format formats INI")
    void builderFormat() {
        String result = IniUtils.builder()
            .put("key", "value")
            .section("section")
            .put("section", "key2", "value2")
            .format();

        assertTrue(result.contains("key=value"));
        assertTrue(result.contains("[section]"));
        assertTrue(result.contains("key2=value2"));
    }

    @Test
    @DisplayName("IniUtils builder write writes file")
    void builderWrite() throws IOException {
        File file = tempDir.resolve("builder.ini").toFile();

        IniUtils.builder()
            .section("config")
            .put("config", "setting", "enabled")
            .write(file);

        assertTrue(file.exists());
        Map<String, Map<String, String>> result = IniUtils.parseFile(file);
        assertEquals("enabled", result.get("config").get("setting"));
    }
}