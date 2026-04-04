/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code properties utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.function.*;
import java.util.regex.Pattern;

/**
 * Properties utilities with type conversion.
 */
public final class PropertyUtils {
    private PropertyUtils() {}

    /**
     * Get string property.
     */
    public static String getString(Properties props, String key) {
        return props.getProperty(key);
    }

    /**
     * Get string property with default.
     */
    public static String getString(Properties props, String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    /**
     * Get integer property.
     */
    public static int getInt(Properties props, String key, int defaultValue) {
        String value = props.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Get long property.
     */
    public static long getLong(Properties props, String key, long defaultValue) {
        String value = props.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Get double property.
     */
    public static double getDouble(Properties props, String key, double defaultValue) {
        String value = props.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Get boolean property.
     */
    public static boolean getBoolean(Properties props, String key, boolean defaultValue) {
        String value = props.getProperty(key);
        if (value == null) return defaultValue;
        return Boolean.parseBoolean(value.trim());
    }

    /**
     * Get enum property.
     */
    public static <E extends Enum<E>> E getEnum(Properties props, String key, Class<E> enumClass, E defaultValue) {
        String value = props.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return Enum.valueOf(enumClass, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    /**
     * Get duration property (parses strings like "5s", "10m", "2h").
     */
    public static Duration getDuration(Properties props, String key, Duration defaultValue) {
        String value = props.getProperty(key);
        if (value == null) return defaultValue;
        try {
            return DurationUtils.parse(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Get list property (comma-separated).
     */
    public static List<String> getList(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }

    /**
     * Get list property with custom separator.
     */
    public static List<String> getList(Properties props, String key, String separator) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) return List.of();
        return Arrays.stream(value.split(Pattern.quote(separator)))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    }

    /**
     * Get map property (key=value pairs separated by commas).
     */
    public static Map<String, String> getMap(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) return Map.of();

        Map<String, String> result = new HashMap<>();
        for (String pair : value.split(",")) {
            String[] kv = pair.trim().split("=", 2);
            if (kv.length == 2) {
                result.put(kv[0].trim(), kv[1].trim());
            }
        }
        return result;
    }

    /**
     * Set property.
     */
    public static void set(Properties props, String key, Object value) {
        if (value == null) {
            props.remove(key);
        } else {
            props.setProperty(key, String.valueOf(value));
        }
    }

    /**
     * Set list property.
     */
    public static void setList(Properties props, String key, List<String> values) {
        if (values == null || values.isEmpty()) {
            props.remove(key);
        } else {
            props.setProperty(key, String.join(",", values));
        }
    }

    /**
     * Set map property.
     */
    public static void setMap(Properties props, String key, Map<String, String> map) {
        if (map == null || map.isEmpty()) {
            props.remove(key);
        } else {
            String value = map.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(java.util.stream.Collectors.joining(","));
            props.setProperty(key, value);
        }
    }

    /**
     * Load properties from file.
     */
    public static Properties load(File file) throws IOException {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(file)) {
            props.load(fis);
        }
        return props;
    }

    /**
     * Load properties from classpath resource.
     */
    public static Properties loadResource(String name) throws IOException {
        Properties props = new Properties();
        try (InputStream is = PropertyUtils.class.getClassLoader().getResourceAsStream(name)) {
            if (is == null) {
                throw new FileNotFoundException("Resource not found: " + name);
            }
            props.load(is);
        }
        return props;
    }

    /**
     * Save properties to file.
     */
    public static void save(Properties props, File file, String comments) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            props.store(fos, comments);
        }
    }

    /**
     * Convert properties to map.
     */
    public static Map<String, String> toMap(Properties props) {
        Map<String, String> map = new HashMap<>();
        for (String key : props.stringPropertyNames()) {
            map.put(key, props.getProperty(key));
        }
        return map;
    }

    /**
     * Convert map to properties.
     */
    public static Properties fromMap(Map<String, String> map) {
        Properties props = new Properties();
        props.putAll(map);
        return props;
    }

    /**
     * Merge properties.
     */
    public static Properties merge(Properties base, Properties override) {
        Properties result = new Properties();
        result.putAll(base);
        result.putAll(override);
        return result;
    }

    /**
     * Filter properties by prefix.
     */
    public static Properties filterByPrefix(Properties props, String prefix) {
        Properties result = new Properties();
        for (String key : props.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                result.setProperty(key.substring(prefix.length()), props.getProperty(key));
            }
        }
        return result;
    }

    /**
     * Get required property (throws if missing).
     */
    public static String getRequired(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("Required property missing: " + key);
        }
        return value;
    }

    /**
     * Check if property exists.
     */
    public static boolean hasProperty(Properties props, String key) {
        return props.containsKey(key);
    }

    /**
     * Interpolate properties (replace ${key} references).
     */
    public static Properties interpolate(Properties props) {
        Properties result = new Properties();
        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);
            String interpolated = interpolateValue(value, props);
            result.setProperty(key, interpolated);
        }
        return result;
    }

    private static String interpolateValue(String value, Properties props) {
        if (value == null || !value.contains("${")) {
            return value;
        }

        StringBuilder result = new StringBuilder();
        int i = 0;
        while (i < value.length()) {
            int start = value.indexOf("${", i);
            if (start < 0) {
                result.append(value.substring(i));
                break;
            }
            result.append(value.substring(i, start));
            int end = value.indexOf("}", start);
            if (end < 0) {
                result.append(value.substring(start));
                break;
            }
            String refKey = value.substring(start + 2, end);
            String refValue = props.getProperty(refKey, "");
            result.append(refValue);
            i = end + 1;
        }
        return result.toString();
    }

    /**
     * Properties builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder class.
     */
    public static final class Builder {
        private final Properties props = new Properties();

        public Builder put(String key, Object value) {
            set(props, key, value);
            return this;
        }

        public Builder putAll(Map<String, ?> map) {
            map.forEach((k, v) -> set(props, k, v));
            return this;
        }

        public Builder putAll(Properties other) {
            props.putAll(other);
            return this;
        }

        public Properties build() {
            return props;
        }
    }
}