/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code settings validation
 */
package com.anthropic.claudecode.utils.settings;

import java.util.*;
import java.util.regex.*;

/**
 * Settings validation utilities.
 */
public final class SettingsValidation {
    private SettingsValidation() {}

    /**
     * Validation error representation.
     */
    public record ValidationError(
            String file,
            String path,
            String message,
            String expected,
            Object invalidValue,
            String suggestion,
            String docLink,
            McpErrorMetadata mcpErrorMetadata
    ) {}

    /**
     * MCP-specific error metadata.
     */
    public record McpErrorMetadata(
            String scope,
            String serverName,
            String severity
    ) {}

    /**
     * Settings with errors result.
     */
    public record SettingsWithErrors(
            Map<String, Object> settings,
            List<ValidationError> errors
    ) {}

    /**
     * Validate settings file content.
     */
    public static ValidationResult validateSettingsFileContent(String content) {
        try {
            // Parse the JSON first
            Map<String, Object> jsonData = parseJson(content);

            // Validate against schema
            List<ValidationError> errors = validateAgainstSchema(jsonData);

            if (errors.isEmpty()) {
                return new ValidationResult(true, null, null);
            }

            String errorMessage = "Settings validation failed:\n" +
                    errors.stream()
                            .map(err -> "- " + err.path + ": " + err.message)
                            .reduce((a, b) -> a + "\n" + b)
                            .orElse("");

            return new ValidationResult(false, errorMessage, generateSchema());
        } catch (Exception parseError) {
            return new ValidationResult(
                    false,
                    "Invalid JSON: " + parseError.getMessage(),
                    generateSchema()
            );
        }
    }

    /**
     * Validation result.
     */
    public record ValidationResult(
            boolean isValid,
            String error,
            String fullSchema
    ) {}

    /**
     * Filter invalid permission rules from raw parsed JSON data.
     */
    public static List<ValidationError> filterInvalidPermissionRules(
            Map<String, Object> data,
            String filePath
    ) {
        if (data == null) return List.of();

        Object permissions = data.get("permissions");
        if (!(permissions instanceof Map)) return List.of();

        @SuppressWarnings("unchecked")
        Map<String, Object> perms = (Map<String, Object>) permissions;
        List<ValidationError> warnings = new ArrayList<>();

        for (String key : List.of("allow", "deny", "ask")) {
            Object rules = perms.get(key);
            if (!(rules instanceof List)) continue;

            @SuppressWarnings("unchecked")
            List<Object> rulesList = (List<Object>) rules;
            List<Object> filtered = new ArrayList<>();

            for (Object rule : rulesList) {
                if (!(rule instanceof String)) {
                    warnings.add(new ValidationError(
                            filePath,
                            "permissions." + key,
                            "Non-string value in " + key + " array was removed",
                            null,
                            rule,
                            null,
                            null,
                            null
                    ));
                    continue;
                }

                ValidationResult result = validatePermissionRule((String) rule);
                if (!result.isValid()) {
                    String message = "Invalid permission rule \"" + rule + "\" was skipped";
                    if (result.error() != null) message += ": " + result.error();

                    warnings.add(new ValidationError(
                            filePath,
                            "permissions." + key,
                            message,
                            null,
                            rule,
                            result.error(),
                            null,
                            null
                    ));
                    continue;
                }

                filtered.add(rule);
            }

            perms.put(key, filtered);
        }

        return warnings;
    }

    /**
     * Validate a single permission rule.
     */
    public static ValidationResult validatePermissionRule(String rule) {
        if (rule == null || rule.isEmpty()) {
            return new ValidationResult(false, "Rule cannot be empty", null);
        }

        // Check for valid format: tool_name(pattern) or just tool_name
        Pattern validPattern = Pattern.compile("^[a-zA-Z_]+(\\([^)]*\\))?$");
        if (!validPattern.matcher(rule).matches()) {
            // Check for more specific patterns
            if (rule.contains("(") && !rule.contains(")")) {
                return new ValidationResult(false, "Missing closing parenthesis", null);
            }

            // Allow some flexibility for complex patterns
            if (rule.startsWith("Bash(") || rule.startsWith("Read(") ||
                rule.startsWith("Edit(") || rule.startsWith("Write(")) {
                return new ValidationResult(true, null, null);
            }

            // Check for glob-style patterns
            if (rule.contains(":") || rule.contains("*")) {
                return new ValidationResult(true, null, null);
            }

            return new ValidationResult(false, "Invalid rule format", null);
        }

        return new ValidationResult(true, null, null);
    }

    /**
     * Validate against schema.
     */
    private static List<ValidationError> validateAgainstSchema(Map<String, Object> data) {
        List<ValidationError> errors = new ArrayList<>();

        // Validate permissions section
        Object permissions = data.get("permissions");
        if (permissions != null && !(permissions instanceof Map)) {
            errors.add(new ValidationError(
                    null,
                    "permissions",
                    "Expected object, but received " + getTypeName(permissions),
                    "object",
                    permissions,
                    null,
                    null,
                    null
            ));
        }

        // Validate hooks section
        Object hooks = data.get("hooks");
        if (hooks != null && !(hooks instanceof Map)) {
            errors.add(new ValidationError(
                    null,
                    "hooks",
                    "Expected object, but received " + getTypeName(hooks),
                    "object",
                    hooks,
                    null,
                    null,
                    null
            ));
        }

        // Validate env section
        Object env = data.get("env");
        if (env != null && !(env instanceof Map)) {
            errors.add(new ValidationError(
                    null,
                    "env",
                    "Expected object, but received " + getTypeName(env),
                    "object",
                    env,
                    null,
                    null,
                    null
            ));
        }

        // Validate model
        Object model = data.get("model");
        if (model != null && !(model instanceof String)) {
            errors.add(new ValidationError(
                    null,
                    "model",
                    "Expected string, but received " + getTypeName(model),
                    "string",
                    model,
                    null,
                    null,
                    null
            ));
        }

        // Validate cleanupPeriodDays
        Object cleanupPeriodDays = data.get("cleanupPeriodDays");
        if (cleanupPeriodDays != null) {
            if (!(cleanupPeriodDays instanceof Number)) {
                errors.add(new ValidationError(
                        null,
                        "cleanupPeriodDays",
                        "Expected number, but received " + getTypeName(cleanupPeriodDays),
                        "number",
                        cleanupPeriodDays,
                        null,
                        null,
                        null
                ));
            } else {
                int value = ((Number) cleanupPeriodDays).intValue();
                if (value < 0) {
                    errors.add(new ValidationError(
                            null,
                            "cleanupPeriodDays",
                            "Number must be greater than or equal to 0",
                            "0",
                            value,
                            null,
                            null,
                            null
                    ));
                }
            }
        }

        return errors;
    }

    /**
     * Get type name for an object.
     */
    private static String getTypeName(Object value) {
        if (value == null) return "null";
        if (value instanceof Map) return "object";
        if (value instanceof List) return "array";
        if (value instanceof String) return "string";
        if (value instanceof Number) return "number";
        if (value instanceof Boolean) return "boolean";
        return value.getClass().getSimpleName();
    }

    /**
     * Simple JSON parser (placeholder - would use Jackson or similar).
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseJson(String content) {
        // Simplified JSON parsing - would use proper JSON library
        Map<String, Object> result = new LinkedHashMap<>();

        content = content.trim();
        if (!content.startsWith("{") || !content.endsWith("}")) {
            throw new RuntimeException("Invalid JSON object");
        }

        // Basic parsing - real implementation would use Jackson/Gson
        content = content.substring(1, content.length() - 1).trim();
        if (content.isEmpty()) {
            return result;
        }

        // Very simple key-value parsing for basic validation
        String[] parts = content.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String part : parts) {
            String[] kv = part.split(":", 2);
            if (kv.length == 2) {
                String key = kv[0].trim().replace("\"", "");
                String value = kv[1].trim();
                result.put(key, parseValue(value));
            }
        }

        return result;
    }

    /**
     * Parse a JSON value.
     */
    private static Object parseValue(String value) {
        value = value.trim();

        if (value.equals("null")) return null;
        if (value.equals("true")) return true;
        if (value.equals("false")) return false;

        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }

        if (value.startsWith("{")) {
            return parseJson(value);
        }

        if (value.startsWith("[")) {
            // Simplified array parsing
            return new ArrayList<>();
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e1) {
            try {
                return Double.parseDouble(value);
            } catch (NumberFormatException e2) {
                return value;
            }
        }
    }

    /**
     * Generate schema string.
     */
    private static String generateSchema() {
        return "{\n" +
                "  \"$schema\": \"https://claude.ai/schema/settings.json\",\n" +
                "  \"type\": \"object\",\n" +
                "  \"properties\": {\n" +
                "    \"model\": { \"type\": \"string\" },\n" +
                "    \"permissions\": { \"$ref\": \"#/definitions/permissions\" },\n" +
                "    \"hooks\": { \"$ref\": \"#/definitions/hooks\" },\n" +
                "    \"env\": { \"type\": \"object\", \"additionalProperties\": { \"type\": \"string\" } }\n" +
                "  }\n" +
                "}";
    }

    /**
     * Get validation tip for an error.
     */
    public static ValidationTip getValidationTip(String path, String code, String expected, Object received) {
        // Tips for common validation errors
        if ("permissions.defaultMode".equals(path)) {
            return new ValidationTip(
                    "Valid modes: default, acceptEdits, bypassPermissions, dontAsk, plan, auto",
                    "https://docs.anthropic.com/claude-code/settings#permissions"
            );
        }

        if (path != null && path.startsWith("permissions.")) {
            return new ValidationTip(
                    "Permission rules should be in format: ToolName(pattern) or ToolName",
                    "https://docs.anthropic.com/claude-code/permissions"
            );
        }

        if ("hooks".equals(path)) {
            return new ValidationTip(
                    "Hooks must be configured as pre_tool_use, post_tool_use, etc.",
                    "https://docs.anthropic.com/claude-code/hooks"
            );
        }

        return null;
    }

    /**
     * Validation tip record.
     */
    public record ValidationTip(String suggestion, String docLink) {}
}