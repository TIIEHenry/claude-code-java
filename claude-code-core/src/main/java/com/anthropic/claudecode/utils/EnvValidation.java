/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code environment validation utilities
 */
package com.anthropic.claudecode.utils;

/**
 * Validate environment variables with bounds checking.
 */
public final class EnvValidation {
    private EnvValidation() {}

    /**
     * Validation result record.
     */
    public record EnvVarValidationResult(
            int effective,
            String status,
            String message
    ) {
        public static EnvVarValidationResult valid(int value) {
            return new EnvVarValidationResult(value, "valid", null);
        }

        public static EnvVarValidationResult invalid(int defaultValue, String message) {
            return new EnvVarValidationResult(defaultValue, "invalid", message);
        }

        public static EnvVarValidationResult capped(int cappedValue, String message) {
            return new EnvVarValidationResult(cappedValue, "capped", message);
        }
    }

    /**
     * Validate a bounded integer environment variable.
     *
     * @param name The environment variable name
     * @param value The string value to parse
     * @param defaultValue The default value if invalid or missing
     * @param upperLimit The maximum allowed value
     * @return Validation result
     */
    public static EnvVarValidationResult validateBoundedIntEnvVar(
            String name,
            String value,
            int defaultValue,
            int upperLimit) {

        if (value == null || value.isEmpty()) {
            return EnvVarValidationResult.valid(defaultValue);
        }

        try {
            int parsed = Integer.parseInt(value);

            if (parsed <= 0) {
                String message = String.format(
                        "Invalid value \"%s\" (using default: %d)",
                        value, defaultValue
                );
                Debug.logForDebugging(name + " " + message);
                return EnvVarValidationResult.invalid(defaultValue, message);
            }

            if (parsed > upperLimit) {
                String message = String.format(
                        "Capped from %d to %d",
                        parsed, upperLimit
                );
                Debug.logForDebugging(name + " " + message);
                return EnvVarValidationResult.capped(upperLimit, message);
            }

            return EnvVarValidationResult.valid(parsed);

        } catch (NumberFormatException e) {
            String message = String.format(
                    "Invalid value \"%s\" (using default: %d)",
                    value, defaultValue
            );
            Debug.logForDebugging(name + " " + message);
            return EnvVarValidationResult.invalid(defaultValue, message);
        }
    }

    /**
     * Validate and get a bounded integer environment variable.
     *
     * @param name The environment variable name
     * @param defaultValue The default value if invalid or missing
     * @param upperLimit The maximum allowed value
     * @return The effective value after validation
     */
    public static int getBoundedIntEnvVar(String name, int defaultValue, int upperLimit) {
        String value = System.getenv(name);
        EnvVarValidationResult result = validateBoundedIntEnvVar(name, value, defaultValue, upperLimit);
        return result.effective();
    }
}