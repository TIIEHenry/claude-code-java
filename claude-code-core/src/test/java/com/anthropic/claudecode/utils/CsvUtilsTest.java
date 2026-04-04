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
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CsvUtils.
 */
class CsvUtilsTest {

    @Test
    @DisplayName("CsvUtils parseLine parses simple values")
    void parseLineSimple() {
        List<String> result = CsvUtils.parseLine("a,b,c");

        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    @DisplayName("CsvUtils parseLine handles quoted values")
    void parseLineQuoted() {
        List<String> result = CsvUtils.parseLine("\"a\",\"b\",\"c\"");

        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    @DisplayName("CsvUtils parseLine handles embedded separator in quotes")
    void parseLineEmbeddedSeparator() {
        List<String> result = CsvUtils.parseLine("\"a,b\",c");

        assertEquals(2, result.size());
        assertEquals("a,b", result.get(0));
        assertEquals("c", result.get(1));
    }

    @Test
    @DisplayName("CsvUtils parseLine handles escaped quotes")
    void parseLineEscapedQuotes() {
        List<String> result = CsvUtils.parseLine("\"a\"\"b\",c");

        assertEquals(2, result.size());
        assertEquals("a\"b", result.get(0));
        assertEquals("c", result.get(1));
    }

    @Test
    @DisplayName("CsvUtils parseLine handles empty values")
    void parseLineEmpty() {
        List<String> result = CsvUtils.parseLine("a,,c");

        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("", result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    @DisplayName("CsvUtils parseLine handles null input")
    void parseLineNull() {
        List<String> result = CsvUtils.parseLine(null);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("CsvUtils parseLine handles empty input")
    void parseLineEmptyString() {
        List<String> result = CsvUtils.parseLine("");

        // Implementation returns empty list for empty string
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("CsvUtils parse parses multi-line content")
    void parseMultiLine() {
        List<List<String>> result = CsvUtils.parse("a,b\nc,d");

        assertEquals(2, result.size());
        assertEquals(List.of("a", "b"), result.get(0));
        assertEquals(List.of("c", "d"), result.get(1));
    }

    @Test
    @DisplayName("CsvUtils parse with custom separator")
    void parseCustomSeparator() {
        List<List<String>> result = CsvUtils.parse("a|b|c", '|', '"');

        assertEquals(1, result.size());
        assertEquals(3, result.get(0).size());
    }

    @Test
    @DisplayName("CsvUtils formatLine formats simple values")
    void formatLineSimple() {
        String result = CsvUtils.formatLine(List.of("a", "b", "c"));

        assertEquals("a,b,c", result);
    }

    @Test
    @DisplayName("CsvUtils formatLine quotes values with separator")
    void formatLineWithSeparator() {
        String result = CsvUtils.formatLine(List.of("a,b", "c"));

        assertEquals("\"a,b\",c", result);
    }

    @Test
    @DisplayName("CsvUtils formatLine quotes values with newline")
    void formatLineWithNewline() {
        String result = CsvUtils.formatLine(List.of("a\nb", "c"));

        assertEquals("\"a\nb\",c", result);
    }

    @Test
    @DisplayName("CsvUtils formatLine handles null values")
    void formatLineNull() {
        String result = CsvUtils.formatLine(Arrays.asList(null, "b"));

        assertEquals(",b", result);
    }

    @Test
    @DisplayName("CsvUtils format formats rows")
    void formatRows() {
        String result = CsvUtils.format(List.of(
            List.of("a", "b"),
            List.of("c", "d")
        ));

        assertEquals("a,b\nc,d", result);
    }

    @Test
    @DisplayName("CsvUtils format with custom settings")
    void formatCustomSettings() {
        String result = CsvUtils.format(List.of(
            List.of("a", "b"),
            List.of("c", "d")
        ), '|', '"', "\r\n");

        assertEquals("a|b\r\nc|d", result);
    }

    @Test
    @DisplayName("CsvUtils write and read roundtrip")
    void writeReadRoundtrip(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("test.csv").toFile();
        List<List<String>> data = List.of(
            List.of("a", "b"),
            List.of("c", "d")
        );

        CsvUtils.write(file, data);
        List<List<String>> result = CsvUtils.read(file);

        assertEquals(data, result);
    }

    @Test
    @DisplayName("CsvUtils parse with mapper")
    void parseWithMapper() {
        String content = "name,age\nJohn,30\nJane,25";
        List<Person> result = CsvUtils.parse(content, row ->
            new Person(row.get(0), Integer.parseInt(row.get(1)))
        );

        assertEquals(2, result.size());
        assertEquals("John", result.get(0).name());
        assertEquals(30, result.get(0).age());
    }

    @Test
    @DisplayName("CsvUtils format with headers and mapper")
    void formatWithHeadersMapper() {
        List<Person> people = List.of(
            new Person("John", 30),
            new Person("Jane", 25)
        );

        String result = CsvUtils.format(
            List.of("name", "age"),
            people,
            p -> List.of(p.name(), String.valueOf(p.age()))
        );

        assertEquals("name,age\nJohn,30\nJane,25", result);
    }

    @Test
    @DisplayName("CsvUtils getColumn by index")
    void getColumnByIndex() {
        List<List<String>> csv = List.of(
            List.of("a", "b", "c"),
            List.of("d", "e", "f")
        );

        List<String> col = CsvUtils.getColumn(csv, 1);

        assertEquals(List.of("b", "e"), col);
    }

    @Test
    @DisplayName("CsvUtils getColumn by header name")
    void getColumnByHeader() {
        List<List<String>> csv = List.of(
            List.of("name", "age"),
            List.of("John", "30"),
            List.of("Jane", "25")
        );

        List<String> col = CsvUtils.getColumn(csv, "age");

        assertEquals(List.of("30", "25"), col);
    }

    @Test
    @DisplayName("CsvUtils getColumn missing header returns empty")
    void getColumnMissingHeader() {
        List<List<String>> csv = List.of(
            List.of("name", "age"),
            List.of("John", "30")
        );

        List<String> col = CsvUtils.getColumn(csv, "unknown");

        assertTrue(col.isEmpty());
    }

    @Test
    @DisplayName("CsvUtils transpose swaps rows and columns")
    void transpose() {
        List<List<String>> csv = List.of(
            List.of("a", "b"),
            List.of("c", "d")
        );

        List<List<String>> result = CsvUtils.transpose(csv);

        assertEquals(2, result.size());
        assertEquals(List.of("a", "c"), result.get(0));
        assertEquals(List.of("b", "d"), result.get(1));
    }

    @Test
    @DisplayName("CsvUtils transpose handles unequal row lengths")
    void transposeUnequal() {
        List<List<String>> csv = List.of(
            List.of("a", "b", "c"),
            List.of("d", "e")
        );

        List<List<String>> result = CsvUtils.transpose(csv);

        assertEquals(3, result.size());
        assertEquals(List.of("a", "d"), result.get(0));
        assertEquals(List.of("b", "e"), result.get(1));
        assertEquals(List.of("c", ""), result.get(2));
    }

    @Test
    @DisplayName("CsvUtils transpose empty returns empty")
    void transposeEmpty() {
        List<List<String>> result = CsvUtils.transpose(List.of());

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("CsvUtils reader builder works")
    void readerBuilder() {
        List<List<String>> result = CsvUtils.reader()
            .separator('|')
            .quote('"')
            .skipHeader(true)
            .parse("h1|h2\na|b\nc|d");

        assertEquals(2, result.size());
        assertEquals(List.of("a", "b"), result.get(0));
    }

    @Test
    @DisplayName("CsvUtils reader builder with comment marker")
    void readerBuilderCommentMarker() {
        List<List<String>> result = CsvUtils.reader()
            .commentMarker("#")
            .parse("# comment\na,b\nc,d");

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("CsvUtils writer builder works")
    void writerBuilder() {
        String result = CsvUtils.writer()
            .separator('|')
            .headers("h1", "h2")
            .format(List.of(
                List.of("a", "b"),
                List.of("c", "d")
            ));

        assertEquals("h1|h2\na|b\nc|d", result);
    }

    @Test
    @DisplayName("CsvUtils writer builder with custom line end")
    void writerBuilderLineEnd() {
        String result = CsvUtils.writer()
            .lineEnd("\r\n")
            .format(List.of(
                List.of("a", "b"),
                List.of("c", "d")
            ));

        // format joins rows with lineEnd, doesn't add trailing newline
        assertEquals("a,b\r\nc,d", result);
    }

    record Person(String name, int age) {}
}