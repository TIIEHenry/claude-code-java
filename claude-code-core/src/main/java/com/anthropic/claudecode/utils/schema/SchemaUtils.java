/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code schema utilities
 */
package com.anthropic.claudecode.utils.schema;

import java.util.*;
import java.util.regex.*;

/**
 * JSON Schema utilities for validation and parsing.
 */
public final class SchemaUtils {
    private SchemaUtils() {}

    /**
     * Schema type enum.
     */
    public enum SchemaType {
        STRING("string"),
        NUMBER("number"),
        INTEGER("integer"),
        BOOLEAN("boolean"),
        OBJECT("object"),
        ARRAY("array"),
        NULL("null");

        private final String id;

        SchemaType(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public static SchemaType fromId(String id) {
            for (SchemaType type : values()) {
                if (type.id.equals(id)) {
                    return type;
                }
            }
            return STRING;
        }
    }

    /**
     * Validate a value against a schema.
     */
    public static boolean validate(Object value, Map<String, Object> schema) {
        if (schema == null) {
            return true;
        }

        String typeStr = (String) schema.get("type");
        SchemaType type = SchemaType.fromId(typeStr);

        return validateType(value, type, schema);
    }

    /**
     * Validate value type.
     */
    private static boolean validateType(Object value, SchemaType type, Map<String, Object> schema) {
        if (value == null) {
            return type == SchemaType.NULL || !isRequired(schema);
        }

        return switch (type) {
            case STRING -> value instanceof String && validateString((String) value, schema);
            case NUMBER -> value instanceof Number;
            case INTEGER -> value instanceof Number && isInteger((Number) value);
            case BOOLEAN -> value instanceof Boolean;
            case OBJECT -> value instanceof Map && validateObject((Map<?, ?>) value, schema);
            case ARRAY -> value instanceof List && validateArray((List<?>) value, schema);
            case NULL -> value == null;
        };
    }

    /**
     * Validate a string value.
     */
    private static boolean validateString(String value, Map<String, Object> schema) {
        // Min length
        Number minLength = (Number) schema.get("minLength");
        if (minLength != null && value.length() < minLength.intValue()) {
            return false;
        }

        // Max length
        Number maxLength = (Number) schema.get("maxLength");
        if (maxLength != null && value.length() > maxLength.intValue()) {
            return false;
        }

        // Pattern
        String pattern = (String) schema.get("pattern");
        if (pattern != null) {
            try {
                if (!Pattern.matches(pattern, value)) {
                    return false;
                }
            } catch (PatternSyntaxException e) {
                // Invalid pattern, skip validation
            }
        }

        // Enum
        Object enumValue = schema.get("enum");
        if (enumValue instanceof List) {
            List<?> enumList = (List<?>) enumValue;
            if (!enumList.contains(value)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Validate an object value.
     */
    @SuppressWarnings("unchecked")
    private static boolean validateObject(Map<?, ?> value, Map<String, Object> schema) {
        // Required properties
        Object required = schema.get("required");
        if (required instanceof List) {
            List<?> requiredList = (List<?>) required;
            for (Object prop : requiredList) {
                if (!value.containsKey(prop)) {
                    return false;
                }
            }
        }

        // Properties
        Object properties = schema.get("properties");
        if (properties instanceof Map) {
            Map<String, Object> props = (Map<String, Object>) properties;
            for (Map.Entry<String, Object> entry : props.entrySet()) {
                if (value.containsKey(entry.getKey())) {
                    Object propSchema = entry.getValue();
                    if (propSchema instanceof Map) {
                        if (!validate(value.get(entry.getKey()), (Map<String, Object>) propSchema)) {
                            return false;
                        }
                    }
                }
            }
        }

        // Additional properties
        Object additionalProps = schema.get("additionalProperties");
        if (Boolean.FALSE.equals(additionalProps)) {
            Set<String> allowedKeys = new HashSet<>();
            if (properties instanceof Map) {
                allowedKeys.addAll(((Map<String, Object>) properties).keySet());
            }
            for (Object key : value.keySet()) {
                if (!allowedKeys.contains(key.toString())) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Validate an array value.
     */
    @SuppressWarnings("unchecked")
    private static boolean validateArray(List<?> value, Map<String, Object> schema) {
        // Min items
        Number minItems = (Number) schema.get("minItems");
        if (minItems != null && value.size() < minItems.intValue()) {
            return false;
        }

        // Max items
        Number maxItems = (Number) schema.get("maxItems");
        if (maxItems != null && value.size() > maxItems.intValue()) {
            return false;
        }

        // Items
        Object items = schema.get("items");
        if (items instanceof Map) {
            Map<String, Object> itemSchema = (Map<String, Object>) items;
            for (Object item : value) {
                if (!validate(item, itemSchema)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Check if a number is an integer.
     */
    private static boolean isInteger(Number value) {
        double d = value.doubleValue();
        return d == Math.floor(d) && !Double.isInfinite(d);
    }

    /**
     * Check if schema has required constraint.
     */
    private static boolean isRequired(Map<String, Object> schema) {
        // By default, all properties can be null unless required
        return false;
    }

    /**
     * Create a simple string schema.
     */
    public static Map<String, Object> stringSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "string");
        return schema;
    }

    /**
     * Create a string schema with constraints.
     */
    public static Map<String, Object> stringSchema(Integer minLength, Integer maxLength, String pattern) {
        Map<String, Object> schema = stringSchema();
        if (minLength != null) schema.put("minLength", minLength);
        if (maxLength != null) schema.put("maxLength", maxLength);
        if (pattern != null) schema.put("pattern", pattern);
        return schema;
    }

    /**
     * Create an enum schema.
     */
    public static Map<String, Object> enumSchema(List<String> values) {
        Map<String, Object> schema = stringSchema();
        schema.put("enum", values);
        return schema;
    }

    /**
     * Create an integer schema.
     */
    public static Map<String, Object> integerSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "integer");
        return schema;
    }

    /**
     * Create a number schema.
     */
    public static Map<String, Object> numberSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "number");
        return schema;
    }

    /**
     * Create a boolean schema.
     */
    public static Map<String, Object> booleanSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "boolean");
        return schema;
    }

    /**
     * Create an object schema.
     */
    public static Map<String, Object> objectSchema(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        if (properties != null) schema.put("properties", properties);
        if (required != null) schema.put("required", required);
        return schema;
    }

    /**
     * Create an array schema.
     */
    public static Map<String, Object> arraySchema(Map<String, Object> itemSchema) {
        return arraySchema(itemSchema, null, null);
    }

    /**
     * Create an array schema with constraints.
     */
    public static Map<String, Object> arraySchema(Map<String, Object> itemSchema, Integer minItems, Integer maxItems) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "array");
        if (itemSchema != null) schema.put("items", itemSchema);
        if (minItems != null) schema.put("minItems", minItems);
        if (maxItems != null) schema.put("maxItems", maxItems);
        return schema;
    }

    /**
     * Merge two schemas.
     */
    public static Map<String, Object> mergeSchemas(Map<String, Object> base, Map<String, Object> override) {
        if (base == null) return override;
        if (override == null) return base;

        Map<String, Object> result = new LinkedHashMap<>(base);
        result.putAll(override);
        return result;
    }

    /**
     * Get a human-readable description of a schema.
     */
    public static String describeSchema(Map<String, Object> schema) {
        if (schema == null) return "any";

        StringBuilder sb = new StringBuilder();
        String type = (String) schema.get("type");
        if (type != null) {
            sb.append(type);
        }

        String description = (String) schema.get("description");
        if (description != null) {
            if (sb.length() > 0) sb.append(" - ");
            sb.append(description);
        }

        Object enumVal = schema.get("enum");
        if (enumVal instanceof List) {
            sb.append(" (").append(String.join(", ", ((List<?>) enumVal).stream().map(Object::toString).toList())).append(")");
        }

        return sb.toString();
    }
}