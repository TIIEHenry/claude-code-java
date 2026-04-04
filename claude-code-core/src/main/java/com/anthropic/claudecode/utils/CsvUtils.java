/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code CSV utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.*;

/**
 * CSV parsing and writing utilities.
 */
public final class CsvUtils {
    private CsvUtils() {}

    private static final char DEFAULT_SEPARATOR = ',';
    private static final char DEFAULT_QUOTE = '"';
    private static final String DEFAULT_LINE_END = "\n";

    /**
     * Parse CSV line.
     */
    public static List<String> parseLine(String line) {
        return parseLine(line, DEFAULT_SEPARATOR, DEFAULT_QUOTE);
    }

    /**
     * Parse CSV line with custom separator.
     */
    public static List<String> parseLine(String line, char separator, char quote) {
        List<String> result = new ArrayList<>();
        if (line == null || line.isEmpty()) {
            return result;
        }

        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        boolean wasQuote = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == quote) {
                    if (i + 1 < line.length() && line.charAt(i + 1) == quote) {
                        current.append(quote);
                        i++;
                    } else {
                        inQuotes = false;
                        wasQuote = true;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == quote && !wasQuote) {
                    inQuotes = true;
                } else if (c == separator) {
                    result.add(current.toString());
                    current = new StringBuilder();
                    wasQuote = false;
                } else {
                    current.append(c);
                    wasQuote = false;
                }
            }
        }

        result.add(current.toString());
        return result;
    }

    /**
     * Parse CSV content.
     */
    public static List<List<String>> parse(String content) {
        return parse(content, DEFAULT_SEPARATOR, DEFAULT_QUOTE);
    }

    /**
     * Parse CSV content with custom separator.
     */
    public static List<List<String>> parse(String content, char separator, char quote) {
        return content.lines()
            .map(line -> parseLine(line, separator, quote))
            .toList();
    }

    /**
     * Format CSV line.
     */
    public static String formatLine(List<String> values) {
        return formatLine(values, DEFAULT_SEPARATOR, DEFAULT_QUOTE);
    }

    /**
     * Format CSV line with custom separator.
     */
    public static String formatLine(List<String> values, char separator, char quote) {
        return values.stream()
            .map(value -> formatValue(value, separator, quote))
            .collect(Collectors.joining(String.valueOf(separator)));
    }

    /**
     * Format single CSV value.
     */
    private static String formatValue(String value, char separator, char quote) {
        if (value == null) return "";

        boolean needsQuote = value.indexOf(separator) >= 0 ||
            value.indexOf(quote) >= 0 ||
            value.indexOf('\n') >= 0 ||
            value.indexOf('\r') >= 0;

        if (needsQuote) {
            String escaped = value.replace(String.valueOf(quote), String.valueOf(quote) + quote);
            return quote + escaped + quote;
        }
        return value;
    }

    /**
     * Format CSV content.
     */
    public static String format(List<List<String>> rows) {
        return format(rows, DEFAULT_SEPARATOR, DEFAULT_QUOTE, DEFAULT_LINE_END);
    }

    /**
     * Format CSV content with custom settings.
     */
    public static String format(List<List<String>> rows, char separator, char quote, String lineEnd) {
        return rows.stream()
            .map(row -> formatLine(row, separator, quote))
            .collect(Collectors.joining(lineEnd));
    }

    /**
     * Read CSV from file.
     */
    public static List<List<String>> read(File file) throws IOException {
        return read(file, DEFAULT_SEPARATOR, DEFAULT_QUOTE);
    }

    /**
     * Read CSV from file with custom settings.
     */
    public static List<List<String>> read(File file, char separator, char quote) throws IOException {
        List<List<String>> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                result.add(parseLine(line, separator, quote));
            }
        }
        return result;
    }

    /**
     * Write CSV to file.
     */
    public static void write(File file, List<List<String>> rows) throws IOException {
        write(file, rows, DEFAULT_SEPARATOR, DEFAULT_QUOTE, DEFAULT_LINE_END);
    }

    /**
     * Write CSV to file with custom settings.
     */
    public static void write(File file, List<List<String>> rows, char separator, char quote, String lineEnd) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (List<String> row : rows) {
                writer.write(formatLine(row, separator, quote));
                writer.write(lineEnd);
            }
        }
    }

    /**
     * Convert CSV to objects using mapper.
     */
    public static <T> List<T> parse(String content, Function<List<String>, T> mapper) {
        return parse(content).stream()
            .skip(1) // Skip header
            .map(mapper)
            .toList();
    }

    /**
     * Convert objects to CSV.
     */
    public static <T> String format(List<String> headers, List<T> objects, Function<T, List<String>> mapper) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(headers);
        for (T obj : objects) {
            rows.add(mapper.apply(obj));
        }
        return format(rows);
    }

    /**
     * Get column from CSV.
     */
    public static List<String> getColumn(List<List<String>> csv, int columnIndex) {
        return csv.stream()
            .filter(row -> row.size() > columnIndex)
            .map(row -> row.get(columnIndex))
            .toList();
    }

    /**
     * Get column by header name.
     */
    public static List<String> getColumn(List<List<String>> csv, String headerName) {
        if (csv.isEmpty()) return List.of();

        List<String> headers = csv.get(0);
        int index = headers.indexOf(headerName);
        if (index < 0) return List.of();

        return csv.stream()
            .skip(1)
            .filter(row -> row.size() > index)
            .map(row -> row.get(index))
            .toList();
    }

    /**
     * Transpose CSV (swap rows and columns).
     */
    public static List<List<String>> transpose(List<List<String>> csv) {
        if (csv.isEmpty()) return List.of();

        int maxCols = csv.stream().mapToInt(List::size).max().orElse(0);
        List<List<String>> result = new ArrayList<>();

        for (int col = 0; col < maxCols; col++) {
            List<String> newRow = new ArrayList<>();
            for (List<String> row : csv) {
                newRow.add(col < row.size() ? row.get(col) : "");
            }
            result.add(newRow);
        }

        return result;
    }

    /**
     * CSV reader builder.
     */
    public static ReaderBuilder reader() {
        return new ReaderBuilder();
    }

    /**
     * CSV writer builder.
     */
    public static WriterBuilder writer() {
        return new WriterBuilder();
    }

    /**
     * Reader builder class.
     */
    public static final class ReaderBuilder {
        private char separator = DEFAULT_SEPARATOR;
        private char quote = DEFAULT_QUOTE;
        private boolean skipHeader = false;
        private String commentMarker = null;

        public ReaderBuilder separator(char separator) {
            this.separator = separator;
            return this;
        }

        public ReaderBuilder quote(char quote) {
            this.quote = quote;
            return this;
        }

        public ReaderBuilder skipHeader(boolean skip) {
            this.skipHeader = skip;
            return this;
        }

        public ReaderBuilder commentMarker(String marker) {
            this.commentMarker = marker;
            return this;
        }

        public List<List<String>> parse(String content) {
            java.util.stream.Stream<String> lines = content.lines();
            if (skipHeader) {
                lines = lines.skip(1);
            }
            if (commentMarker != null) {
                lines = lines.filter(line -> !line.startsWith(commentMarker));
            }
            return lines.map(line -> CsvUtils.parseLine(line, separator, quote)).toList();
        }

        public List<List<String>> read(File file) throws IOException {
            return CsvUtils.read(file, separator, quote);
        }
    }

    /**
     * Writer builder class.
     */
    public static final class WriterBuilder {
        private char separator = DEFAULT_SEPARATOR;
        private char quote = DEFAULT_QUOTE;
        private String lineEnd = DEFAULT_LINE_END;
        private List<String> headers = null;

        public WriterBuilder separator(char separator) {
            this.separator = separator;
            return this;
        }

        public WriterBuilder quote(char quote) {
            this.quote = quote;
            return this;
        }

        public WriterBuilder lineEnd(String lineEnd) {
            this.lineEnd = lineEnd;
            return this;
        }

        public WriterBuilder headers(String... headers) {
            this.headers = Arrays.asList(headers);
            return this;
        }

        public String format(List<List<String>> rows) {
            List<List<String>> allRows = new ArrayList<>();
            if (headers != null) {
                allRows.add(headers);
            }
            allRows.addAll(rows);
            return CsvUtils.format(allRows, separator, quote, lineEnd);
        }

        public void write(File file, List<List<String>> rows) throws IOException {
            CsvUtils.write(file, rows, separator, quote, lineEnd);
        }
    }
}