/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code JSON Schema utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSON Schema utilities for converting internal schemas to JSON Schema format.
 *
 * In TypeScript, this uses Zod's toJSONSchema. In Java, we provide similar
 * functionality for converting Java types to JSON Schema.
 */
public final class JsonSchemaUtils {
    private JsonSchemaUtils() {}

    // Cache for converted schemas
    private static final ConcurrentHashMap<String, Map<String, Object>> schemaCache = new ConcurrentHashMap<>();

    /**
     * JSON Schema type enumeration.
     */
    public enum SchemaType {
        STRING, NUMBER, INTEGER, BOOLEAN, ARRAY, OBJECT, NULL, ANY
    }

    /**
     * Convert a Java class to JSON Schema type.
     */
    public static Map<String, Object> classToSchema(Class<?> clazz) {
        String cacheKey = clazz.getName();
        Map<String, Object> cached = schemaCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        Map<String, Object> schema = new LinkedHashMap<>();

        if (clazz == String.class) {
            schema.put("type", "string");
        } else if (clazz == Integer.class || clazz == int.class ||
                   clazz == Long.class || clazz == long.class) {
            schema.put("type", "integer");
        } else if (clazz == Double.class || clazz == double.class ||
                   clazz == Float.class || clazz == float.class ||
                   clazz == Number.class) {
            schema.put("type", "number");
        } else if (clazz == Boolean.class || clazz == boolean.class) {
            schema.put("type", "boolean");
        } else if (clazz.isArray() || List.class.isAssignableFrom(clazz)) {
            schema.put("type", "array");
            schema.put("items", Map.of("type", "any"));
        } else if (Map.class.isAssignableFrom(clazz)) {
            schema.put("type", "object");
        } else if (clazz == Void.class || clazz == void.class) {
            schema.put("type", "null");
        } else {
            schema.put("type", "object");
        }

        schemaCache.put(cacheKey, schema);
        return schema;
    }

    /**
     * Create a string schema.
     */
    public static Map<String, Object> stringSchema() {
        return Map.of("type", "string");
    }

    /**
     * Create a string schema with constraints.
     */
    public static Map<String, Object> stringSchema(Integer minLength, Integer maxLength, String pattern, String format) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "string");
        if (minLength != null) schema.put("minLength", minLength);
        if (maxLength != null) schema.put("maxLength", maxLength);
        if (pattern != null) schema.put("pattern", pattern);
        if (format != null) schema.put("format", format);
        return schema;
    }

    /**
     * Create a number schema.
     */
    public static Map<String, Object> numberSchema() {
        return Map.of("type", "number");
    }

    /**
     * Create a number schema with constraints.
     */
    public static Map<String, Object> numberSchema(Double minimum, Double maximum, Boolean exclusiveMinimum, Boolean exclusiveMaximum) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "number");
        if (minimum != null) schema.put("minimum", minimum);
        if (maximum != null) schema.put("maximum", maximum);
        if (exclusiveMinimum != null) schema.put("exclusiveMinimum", exclusiveMinimum);
        if (exclusiveMaximum != null) schema.put("exclusiveMaximum", exclusiveMaximum);
        return schema;
    }

    /**
     * Create an integer schema.
     */
    public static Map<String, Object> integerSchema() {
        return Map.of("type", "integer");
    }

    /**
     * Create an integer schema with constraints.
     */
    public static Map<String, Object> integerSchema(Long minimum, Long maximum, Boolean exclusiveMinimum, Boolean exclusiveMaximum) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "integer");
        if (minimum != null) schema.put("minimum", minimum);
        if (maximum != null) schema.put("maximum", maximum);
        if (exclusiveMinimum != null) schema.put("exclusiveMinimum", exclusiveMinimum);
        if (exclusiveMaximum != null) schema.put("exclusiveMaximum", exclusiveMaximum);
        return schema;
    }

    /**
     * Create a boolean schema.
     */
    public static Map<String, Object> booleanSchema() {
        return Map.of("type", "boolean");
    }

    /**
     * Create an array schema.
     */
    public static Map<String, Object> arraySchema(Map<String, Object> itemsSchema) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "array");
        schema.put("items", itemsSchema);
        return schema;
    }

    /**
     * Create an array schema with constraints.
     */
    public static Map<String, Object> arraySchema(Map<String, Object> itemsSchema, Integer minItems, Integer maxItems, Boolean uniqueItems) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "array");
        schema.put("items", itemsSchema);
        if (minItems != null) schema.put("minItems", minItems);
        if (maxItems != null) schema.put("maxItems", maxItems);
        if (uniqueItems != null) schema.put("uniqueItems", uniqueItems);
        return schema;
    }

    /**
     * Create an object schema.
     */
    public static Map<String, Object> objectSchema(Map<String, Map<String, Object>> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        if (properties != null && !properties.isEmpty()) {
            schema.put("properties", properties);
        }
        if (required != null && !required.isEmpty()) {
            schema.put("required", required);
        }
        return schema;
    }

    /**
     * Create an object schema with additional properties setting.
     */
    public static Map<String, Object> objectSchema(
            Map<String, Map<String, Object>> properties,
            List<String> required,
            Boolean additionalProperties
    ) {
        Map<String, Object> schema = objectSchema(properties, required);
        if (additionalProperties != null) {
            schema.put("additionalProperties", additionalProperties);
        }
        return schema;
    }

    /**
     * Create an enum schema.
     */
    public static Map<String, Object> enumSchema(List<Object> values) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("enum", values);
        return schema;
    }

    /**
     * Create a oneOf schema.
     */
    public static Map<String, Object> oneOfSchema(List<Map<String, Object>> schemas) {
        return Map.of("oneOf", schemas);
    }

    /**
     * Create an anyOf schema.
     */
    public static Map<String, Object> anyOfSchema(List<Map<String, Object>> schemas) {
        return Map.of("anyOf", schemas);
    }

    /**
     * Create an allOf schema.
     */
    public static Map<String, Object> allOfSchema(List<Map<String, Object>> schemas) {
        return Map.of("allOf", schemas);
    }

    /**
     * Create a null schema.
     */
    public static Map<String, Object> nullSchema() {
        return Map.of("type", "null");
    }

    /**
     * Create a reference schema.
     */
    public static Map<String, Object> refSchema(String ref) {
        return Map.of("$ref", ref);
    }

    /**
     * Add description to schema.
     */
    public static Map<String, Object> withDescription(Map<String, Object> schema, String description) {
        Map<String, Object> newSchema = new LinkedHashMap<>(schema);
        newSchema.put("description", description);
        return newSchema;
    }

    /**
     * Add default value to schema.
     */
    public static Map<String, Object> withDefault(Map<String, Object> schema, Object defaultValue) {
        Map<String, Object> newSchema = new LinkedHashMap<>(schema);
        newSchema.put("default", defaultValue);
        return newSchema;
    }

    /**
     * Clear the schema cache.
     */
    public static void clearCache() {
        schemaCache.clear();
    }
}