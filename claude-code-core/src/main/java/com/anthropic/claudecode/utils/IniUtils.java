/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code INI utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.util.*;

/**
 * INI file utilities.
 */
public final class IniUtils {
    private IniUtils() {}

    /**
     * Parse INI content.
     */
    public static Map<String, Map<String, String>> parse(String content) {
        Map<String, Map<String, String>> result = new LinkedHashMap<>();
        String currentSection = "";

        for (String line : content.lines().toList()) {
            line = line.trim();

            // Skip empty lines and comments
            if (line.isEmpty() || line.startsWith(";") || line.startsWith("#")) {
                continue;
            }

            // Section header
            if (line.startsWith("[") && line.endsWith("]")) {
                currentSection = line.substring(1, line.length() - 1).trim();
                result.putIfAbsent(currentSection, new LinkedHashMap<>());
                continue;
            }

            // Key-value pair
            int eqIndex = line.indexOf('=');
            if (eqIndex > 0) {
                String key = line.substring(0, eqIndex).trim();
                String value = line.substring(eqIndex + 1).trim();

                // Remove quotes
                if (value.startsWith("\"") && value.endsWith("\"")) {
                    value = value.substring(1, value.length() - 1);
                } else if (value.startsWith("'") && value.endsWith("'")) {
                    value = value.substring(1, value.length() - 1);
                }

                result.computeIfAbsent(currentSection, k -> new LinkedHashMap<>())
                    .put(key, value);
            }
        }

        return result;
    }

    /**
     * Parse INI file.
     */
    public static Map<String, Map<String, String>> parseFile(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return parse(content.toString());
    }

    /**
     * Format INI content.
     */
    public static String format(Map<String, Map<String, String>> data) {
        StringBuilder sb = new StringBuilder();

        // Write default section first
        Map<String, String> defaultSection = data.get("");
        if (defaultSection != null && !defaultSection.isEmpty()) {
            writeSection(sb, "", defaultSection);
        }

        // Write other sections
        for (Map.Entry<String, Map<String, String>> entry : data.entrySet()) {
            if (!entry.getKey().isEmpty()) {
                writeSection(sb, entry.getKey(), entry.getValue());
            }
        }

        return sb.toString();
    }

    private static void writeSection(StringBuilder sb, String section, Map<String, String> values) {
        if (!section.isEmpty()) {
            sb.append("[").append(section).append("]\n");
        }
        for (Map.Entry<String, String> entry : values.entrySet()) {
            String value = entry.getValue();
            if (value.contains(" ") || value.contains(";") || value.contains("#")) {
                value = "\"" + value + "\"";
            }
            sb.append(entry.getKey()).append("=").append(value).append("\n");
        }
        sb.append("\n");
    }

    /**
     * Write INI file.
     */
    public static void write(File file, Map<String, Map<String, String>> data) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(format(data));
        }
    }

    /**
     * Get value from section.
     */
    public static Optional<String> get(Map<String, Map<String, String>> ini, String section, String key) {
        return Optional.ofNullable(ini.get(section))
            .map(s -> s.get(key));
    }

    /**
     * Get value with default.
     */
    public static String get(Map<String, Map<String, String>> ini, String section, String key, String defaultValue) {
        return get(ini, section, key).orElse(defaultValue);
    }

    /**
     * Get integer value.
     */
    public static int getInt(Map<String, Map<String, String>> ini, String section, String key, int defaultValue) {
        return get(ini, section, key)
            .map(v -> {
                try {
                    return Integer.parseInt(v);
                } catch (NumberFormatException e) {
                    return defaultValue;
                }
            })
            .orElse(defaultValue);
    }

    /**
     * Get boolean value.
     */
    public static boolean getBoolean(Map<String, Map<String, String>> ini, String section, String key, boolean defaultValue) {
        return get(ini, section, key)
            .map(Boolean::parseBoolean)
            .orElse(defaultValue);
    }

    /**
     * Get all keys in section.
     */
    public static Set<String> keys(Map<String, Map<String, String>> ini, String section) {
        return ini.getOrDefault(section, Map.of()).keySet();
    }

    /**
     * Get all sections.
     */
    public static Set<String> sections(Map<String, Map<String, String>> ini) {
        Set<String> result = new LinkedHashSet<>(ini.keySet());
        result.remove(""); // Remove default section
        return result;
    }

    /**
     * Set value.
     */
    public static void set(Map<String, Map<String, String>> ini, String section, String key, String value) {
        ini.computeIfAbsent(section, k -> new LinkedHashMap<>()).put(key, value);
    }

    /**
     * Remove key.
     */
    public static void remove(Map<String, Map<String, String>> ini, String section, String key) {
        Map<String, String> sec = ini.get(section);
        if (sec != null) {
            sec.remove(key);
            if (sec.isEmpty() && !section.isEmpty()) {
                ini.remove(section);
            }
        }
    }

    /**
     * INI builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class.
     */
    public static final class Builder {
        private final Map<String, Map<String, String>> data = new LinkedHashMap<>();

        public Builder section(String name) {
            data.computeIfAbsent(name, k -> new LinkedHashMap<>());
            return this;
        }

        public Builder put(String section, String key, String value) {
            data.computeIfAbsent(section, k -> new LinkedHashMap<>()).put(key, value);
            return this;
        }

        public Builder put(String key, String value) {
            return put("", key, value);
        }

        public Map<String, Map<String, String>> build() {
            return data;
        }

        public String format() {
            return IniUtils.format(data);
        }

        public void write(File file) throws IOException {
            IniUtils.write(file, data);
        }
    }
}