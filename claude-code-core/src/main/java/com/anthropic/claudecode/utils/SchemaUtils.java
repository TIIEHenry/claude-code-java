/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code schema utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.regex.*;

/**
 * Schema definition and validation utilities.
 */
public final class SchemaUtils {
    private SchemaUtils() {}

    /**
     * Schema type enumeration.
     */
    public enum SchemaType {
        STRING,
        NUMBER,
        INTEGER,
        BOOLEAN,
        OBJECT,
        ARRAY,
        NULL,
        ANY
    }

    /**
     * Schema definition record.
     */
    public record Schema(
        SchemaType type,
        String description,
        List<Schema> items,
        Map<String, Schema> properties,
        List<String> required,
        Schema additionalProperties,
        Object defaultValue,
        Object example,
        List<Object> enumValues,
        Number minimum,
        Number maximum,
        Integer minLength,
        Integer maxLength,
        Pattern pattern,
        String format
    ) {
        public static Schema string() {
            return new Schema(SchemaType.STRING, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        }

        public static Schema string(String description) {
            return new Schema(SchemaType.STRING, description, null, null, null, null, null, null, null, null, null, null, null, null, null);
        }

        public static Schema number() {
            return new Schema(SchemaType.NUMBER, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        }

        public static Schema integer() {
            return new Schema(SchemaType.INTEGER, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        }

        public static Schema boolean_() {
            return new Schema(SchemaType.BOOLEAN, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        }

        public static Schema object(Map<String, Schema> properties) {
            return new Schema(SchemaType.OBJECT, null, null, properties, null, null, null, null, null, null, null, null, null, null, null);
        }

        public static Schema array(Schema items) {
            return new Schema(SchemaType.ARRAY, null, List.of(items), null, null, null, null, null, null, null, null, null, null, null, null);
        }

        public static Schema any() {
            return new Schema(SchemaType.ANY, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
        }

        public Schema withDescription(String desc) {
            return new Schema(type, desc, items, properties, required, additionalProperties, defaultValue, example, enumValues, minimum, maximum, minLength, maxLength, pattern, format);
        }

        public Schema withRequired(List<String> req) {
            return new Schema(type, description, items, properties, req, additionalProperties, defaultValue, example, enumValues, minimum, maximum, minLength, maxLength, pattern, format);
        }

        public Schema withDefault(Object def) {
            return new Schema(type, description, items, properties, required, additionalProperties, def, example, enumValues, minimum, maximum, minLength, maxLength, pattern, format);
        }

        public Schema withEnum(List<Object> enumVals) {
            return new Schema(type, description, items, properties, required, additionalProperties, defaultValue, example, enumVals, minimum, maximum, minLength, maxLength, pattern, format);
        }

        public Schema withMinLength(Integer min) {
            return new Schema(type, description, items, properties, required, additionalProperties, defaultValue, example, enumValues, minimum, maximum, min, maxLength, pattern, format);
        }

        public Schema withMaxLength(Integer max) {
            return new Schema(type, description, items, properties, required, additionalProperties, defaultValue, example, enumValues, minimum, maximum, minLength, max, pattern, format);
        }

        public Schema withPattern(String regex) {
            return new Schema(type, description, items, properties, required, additionalProperties, defaultValue, example, enumValues, minimum, maximum, minLength, maxLength, Pattern.compile(regex), format);
        }

        public Schema withFormat(String fmt) {
            return new Schema(type, description, items, properties, required, additionalProperties, defaultValue, example, enumValues, minimum, maximum, minLength, maxLength, pattern, fmt);
        }
    }

    /**
     * ValidationResult record.
     */
    public record ValidationResult(boolean valid, List<String> errors) {
        public static ValidationResult success() {
            return new ValidationResult(true, List.of());
        }

        public static ValidationResult failure(String error) {
            return new ValidationResult(false, List.of(error));
        }

        public static ValidationResult failure(List<String> errors) {
            return new ValidationResult(false, errors);
        }

        public ValidationResult merge(ValidationResult other) {
            List<String> combined = new ArrayList<>(errors);
            combined.addAll(other.errors);
            return new ValidationResult(valid && other.valid, combined);
        }
    }

    /**
     * Validate value against schema.
     */
    public static ValidationResult validate(Object value, Schema schema) {
        if (value == null) {
            if (schema.type() == SchemaType.NULL || schema.type() == SchemaType.ANY) {
                return ValidationResult.success();
            }
            return ValidationResult.failure("Value is null but schema type is " + schema.type());
        }

        switch (schema.type()) {
            case STRING:
                return validateString(value, schema);
            case NUMBER:
                return validateNumber(value, schema);
            case INTEGER:
                return validateInteger(value, schema);
            case BOOLEAN:
                return validateBoolean(value, schema);
            case OBJECT:
                return validateObject(value, schema);
            case ARRAY:
                return validateArray(value, schema);
            case ANY:
                return ValidationResult.success();
            default:
                return ValidationResult.failure("Unknown schema type: " + schema.type());
        }
    }

    private static ValidationResult validateString(Object value, Schema schema) {
        if (!(value instanceof String)) {
            return ValidationResult.failure("Expected string, got " + value.getClass().getSimpleName());
        }

        String str = (String) value;
        List<String> errors = new ArrayList<>();

        if (schema.minLength() != null && str.length() < schema.minLength()) {
            errors.add("String length " + str.length() + " is less than minimum " + schema.minLength());
        }

        if (schema.maxLength() != null && str.length() > schema.maxLength()) {
            errors.add("String length " + str.length() + " exceeds maximum " + schema.maxLength());
        }

        if (schema.pattern() != null && !schema.pattern().matcher(str).matches()) {
            errors.add("String does not match pattern " + schema.pattern().pattern());
        }

        if (schema.enumValues() != null && !schema.enumValues().contains(str)) {
            errors.add("String '" + str + "' is not one of allowed values: " + schema.enumValues());
        }

        if (schema.format() != null) {
            ValidationResult formatResult = validateFormat(str, schema.format());
            if (!formatResult.valid()) {
                errors.addAll(formatResult.errors());
            }
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    private static ValidationResult validateNumber(Object value, Schema schema) {
        if (!(value instanceof Number)) {
            return ValidationResult.failure("Expected number, got " + value.getClass().getSimpleName());
        }

        Number num = (Number) value;
        List<String> errors = new ArrayList<>();

        if (schema.minimum() != null && num.doubleValue() < schema.minimum().doubleValue()) {
            errors.add("Number " + num + " is less than minimum " + schema.minimum());
        }

        if (schema.maximum() != null && num.doubleValue() > schema.maximum().doubleValue()) {
            errors.add("Number " + num + " exceeds maximum " + schema.maximum());
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    private static ValidationResult validateInteger(Object value, Schema schema) {
        if (!(value instanceof Number)) {
            return ValidationResult.failure("Expected integer, got " + value.getClass().getSimpleName());
        }

        Number num = (Number) value;
        if (num.doubleValue() != num.longValue()) {
            return ValidationResult.failure("Expected integer, got fractional number " + num);
        }

        List<String> errors = new ArrayList<>();

        if (schema.minimum() != null && num.longValue() < schema.minimum().longValue()) {
            errors.add("Integer " + num + " is less than minimum " + schema.minimum());
        }

        if (schema.maximum() != null && num.longValue() > schema.maximum().longValue()) {
            errors.add("Integer " + num + " exceeds maximum " + schema.maximum());
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    private static ValidationResult validateBoolean(Object value, Schema schema) {
        if (!(value instanceof Boolean)) {
            return ValidationResult.failure("Expected boolean, got " + value.getClass().getSimpleName());
        }
        return ValidationResult.success();
    }

    private static ValidationResult validateObject(Object value, Schema schema) {
        if (!(value instanceof Map)) {
            return ValidationResult.failure("Expected object, got " + value.getClass().getSimpleName());
        }

        Map<String, Object> map = (Map<String, Object>) value;
        List<String> errors = new ArrayList<>();

        // Check required properties
        if (schema.required() != null) {
            for (String req : schema.required()) {
                if (!map.containsKey(req)) {
                    errors.add("Missing required property: " + req);
                }
            }
        }

        // Validate each property
        if (schema.properties() != null) {
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String key = entry.getKey();
                Schema propSchema = schema.properties().get(key);
                if (propSchema != null) {
                    ValidationResult result = validate(entry.getValue(), propSchema);
                    if (!result.valid()) {
                        for (String error : result.errors()) {
                            errors.add("Property '" + key + "': " + error);
                        }
                    }
                } else if (schema.additionalProperties() != null) {
                    ValidationResult result = validate(entry.getValue(), schema.additionalProperties());
                    if (!result.valid()) {
                        for (String error : result.errors()) {
                            errors.add("Additional property '" + key + "': " + error);
                        }
                    }
                }
            }
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    private static ValidationResult validateArray(Object value, Schema schema) {
        if (!(value instanceof List)) {
            return ValidationResult.failure("Expected array, got " + value.getClass().getSimpleName());
        }

        List<?> list = (List<?>) value;
        List<String> errors = new ArrayList<>();

        if (schema.items() != null && !schema.items().isEmpty()) {
            Schema itemSchema = schema.items().get(0);
            for (int i = 0; i < list.size(); i++) {
                ValidationResult result = validate(list.get(i), itemSchema);
                if (!result.valid()) {
                    for (String error : result.errors()) {
                        errors.add("Array item[" + i + "]: " + error);
                    }
                }
            }
        }

        return errors.isEmpty() ? ValidationResult.success() : ValidationResult.failure(errors);
    }

    private static ValidationResult validateFormat(String value, String format) {
        switch (format) {
            case "email":
                if (!value.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                    return ValidationResult.failure("Invalid email format");
                }
                break;
            case "uri":
                try {
                    new java.net.URI(value);
                } catch (Exception e) {
                    return ValidationResult.failure("Invalid URI format");
                }
                break;
            case "date":
                if (!value.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
                    return ValidationResult.failure("Invalid date format (expected YYYY-MM-DD)");
                }
                break;
            case "date-time":
                if (!value.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")) {
                    return ValidationResult.failure("Invalid date-time format");
                }
                break;
            case "uuid":
                if (!value.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")) {
                    return ValidationResult.failure("Invalid UUID format");
                }
                break;
        }
        return ValidationResult.success();
    }

    /**
     * Convert schema to JSON Schema format.
     */
    public static Map<String, Object> toJsonSchema(Schema schema) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (schema.type() != SchemaType.ANY) {
            result.put("type", schema.type().name().toLowerCase());
        }

        if (schema.description() != null) {
            result.put("description", schema.description());
        }

        if (schema.properties() != null) {
            Map<String, Object> props = new LinkedHashMap<>();
            for (Map.Entry<String, Schema> entry : schema.properties().entrySet()) {
                props.put(entry.getKey(), toJsonSchema(entry.getValue()));
            }
            result.put("properties", props);
        }

        if (schema.required() != null && !schema.required().isEmpty()) {
            result.put("required", schema.required());
        }

        if (schema.items() != null && !schema.items().isEmpty()) {
            result.put("items", toJsonSchema(schema.items().get(0)));
        }

        if (schema.defaultValue() != null) {
            result.put("default", schema.defaultValue());
        }

        if (schema.enumValues() != null && !schema.enumValues().isEmpty()) {
            result.put("enum", schema.enumValues());
        }

        if (schema.minimum() != null) {
            result.put("minimum", schema.minimum());
        }

        if (schema.maximum() != null) {
            result.put("maximum", schema.maximum());
        }

        if (schema.minLength() != null) {
            result.put("minLength", schema.minLength());
        }

        if (schema.maxLength() != null) {
            result.put("maxLength", schema.maxLength());
        }

        if (schema.pattern() != null) {
            result.put("pattern", schema.pattern().pattern());
        }

        if (schema.format() != null) {
            result.put("format", schema.format());
        }

        return result;
    }

    /**
     * Create schema builder.
     */
    public static SchemaBuilder builder() {
        return new SchemaBuilder();
    }

    /**
     * Schema builder class.
     */
    public static class SchemaBuilder {
        private SchemaType type = SchemaType.ANY;
        private String description;
        private List<Schema> items;
        private Map<String, Schema> properties = new LinkedHashMap<>();
        private List<String> required = new ArrayList<>();
        private Schema additionalProperties;
        private Object defaultValue;
        private Object example;
        private List<Object> enumValues;
        private Number minimum;
        private Number maximum;
        private Integer minLength;
        private Integer maxLength;
        private Pattern pattern;
        private String format;

        public SchemaBuilder type(SchemaType type) {
            this.type = type;
            return this;
        }

        public SchemaBuilder description(String description) {
            this.description = description;
            return this;
        }

        public SchemaBuilder property(String name, Schema schema) {
            this.properties.put(name, schema);
            return this;
        }

        public SchemaBuilder property(String name, SchemaType type) {
            this.properties.put(name, new Schema(type, null, null, null, null, null, null, null, null, null, null, null, null, null, null));
            return this;
        }

        public SchemaBuilder required(String... names) {
            this.required.addAll(Arrays.asList(names));
            return this;
        }

        public SchemaBuilder items(Schema schema) {
            this.items = List.of(schema);
            return this;
        }

        public SchemaBuilder defaultValue(Object value) {
            this.defaultValue = value;
            return this;
        }

        public SchemaBuilder enumValues(Object... values) {
            this.enumValues = Arrays.asList(values);
            return this;
        }

        public SchemaBuilder minimum(Number min) {
            this.minimum = min;
            return this;
        }

        public SchemaBuilder maximum(Number max) {
            this.maximum = max;
            return this;
        }

        public SchemaBuilder minLength(Integer min) {
            this.minLength = min;
            return this;
        }

        public SchemaBuilder maxLength(Integer max) {
            this.maxLength = max;
            return this;
        }

        public SchemaBuilder pattern(String regex) {
            this.pattern = Pattern.compile(regex);
            return this;
        }

        public SchemaBuilder format(String format) {
            this.format = format;
            return this;
        }

        public Schema build() {
            return new Schema(type, description, items, properties, required, additionalProperties, defaultValue, example, enumValues, minimum, maximum, minLength, maxLength, pattern, format);
        }
    }
}