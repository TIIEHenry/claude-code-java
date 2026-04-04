/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/validation
 */
package com.anthropic.claudecode.utils.validation;

import java.util.*;
import java.util.regex.*;

/**
 * Validation utils - Input validation utilities.
 */
public final class ValidationUtils {

    /**
     * Email pattern.
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    /**
     * URL pattern.
     */
    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * IPv4 pattern.
     */
    private static final Pattern IPV4_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    );

    /**
     * UUID pattern.
     */
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Validate email.
     */
    public static ValidationResult validateEmail(String email) {
        if (email == null || email.isEmpty()) {
            return ValidationResult.invalid("Email is required");
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return ValidationResult.invalid("Invalid email format");
        }

        return ValidationResult.valid();
    }

    /**
     * Validate URL.
     */
    public static ValidationResult validateUrl(String url) {
        if (url == null || url.isEmpty()) {
            return ValidationResult.invalid("URL is required");
        }

        if (!URL_PATTERN.matcher(url).matches()) {
            return ValidationResult.invalid("Invalid URL format");
        }

        return ValidationResult.valid();
    }

    /**
     * Validate IPv4.
     */
    public static ValidationResult validateIPv4(String ip) {
        if (ip == null || ip.isEmpty()) {
            return ValidationResult.invalid("IP address is required");
        }

        if (!IPV4_PATTERN.matcher(ip).matches()) {
            return ValidationResult.invalid("Invalid IPv4 format");
        }

        return ValidationResult.valid();
    }

    /**
     * Validate UUID.
     */
    public static ValidationResult validateUuid(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return ValidationResult.invalid("UUID is required");
        }

        if (!UUID_PATTERN.matcher(uuid).matches()) {
            return ValidationResult.invalid("Invalid UUID format");
        }

        return ValidationResult.valid();
    }

    /**
     * Validate string length.
     */
    public static ValidationResult validateLength(String value, int min, int max, String fieldName) {
        if (value == null) {
            return ValidationResult.invalid(fieldName + " is required");
        }

        int length = value.length();
        if (length < min) {
            return ValidationResult.invalid(fieldName + " must be at least " + min + " characters");
        }

        if (length > max) {
            return ValidationResult.invalid(fieldName + " must be at most " + max + " characters");
        }

        return ValidationResult.valid();
    }

    /**
     * Validate range.
     */
    public static ValidationResult validateRange(int value, int min, int max, String fieldName) {
        if (value < min) {
            return ValidationResult.invalid(fieldName + " must be at least " + min);
        }

        if (value > max) {
            return ValidationResult.invalid(fieldName + " must be at most " + max);
        }

        return ValidationResult.valid();
    }

    /**
     * Validate range.
     */
    public static ValidationResult validateRange(long value, long min, long max, String fieldName) {
        if (value < min) {
            return ValidationResult.invalid(fieldName + " must be at least " + min);
        }

        if (value > max) {
            return ValidationResult.invalid(fieldName + " must be at most " + max);
        }

        return ValidationResult.valid();
    }

    /**
     * Validate pattern.
     */
    public static ValidationResult validatePattern(String value, Pattern pattern, String fieldName, String message) {
        if (value == null || value.isEmpty()) {
            return ValidationResult.invalid(fieldName + " is required");
        }

        if (!pattern.matcher(value).matches()) {
            return ValidationResult.invalid(message);
        }

        return ValidationResult.valid();
    }

    /**
     * Validate required.
     */
    public static ValidationResult validateRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return ValidationResult.invalid(fieldName + " is required");
        }
        return ValidationResult.valid();
    }

    /**
     * Validate required.
     */
    public static ValidationResult validateRequired(Object value, String fieldName) {
        if (value == null) {
            return ValidationResult.invalid(fieldName + " is required");
        }
        return ValidationResult.valid();
    }

    /**
     * Validate one of.
     */
    public static <T> ValidationResult validateOneOf(T value, Set<T> allowed, String fieldName) {
        if (value == null) {
            return ValidationResult.invalid(fieldName + " is required");
        }

        if (!allowed.contains(value)) {
            return ValidationResult.invalid(fieldName + " must be one of: " + allowed);
        }

        return ValidationResult.valid();
    }

    /**
     * Validate alphanumeric.
     */
    public static ValidationResult validateAlphanumeric(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            return ValidationResult.invalid(fieldName + " is required");
        }

        if (!value.matches("^[a-zA-Z0-9]+$")) {
            return ValidationResult.invalid(fieldName + " must be alphanumeric");
        }

        return ValidationResult.valid();
    }

    /**
     * Validate no special characters.
     */
    public static ValidationResult validateNoSpecialChars(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            return ValidationResult.invalid(fieldName + " is required");
        }

        if (!value.matches("^[a-zA-Z0-9_-]+$")) {
            return ValidationResult.invalid(fieldName + " can only contain letters, numbers, underscore, and hyphen");
        }

        return ValidationResult.valid();
    }

    /**
     * Validation result record.
     */
    public record ValidationResult(
        boolean isValid,
        String errorMessage
    ) {
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String message) {
            return new ValidationResult(false, message);
        }
    }

    /**
     * Validation builder.
     */
    public static final class ValidationBuilder {
        private final List<ValidationResult> results = new ArrayList<>();

        public ValidationBuilder add(ValidationResult result) {
            results.add(result);
            return this;
        }

        public ValidationBuilder required(String value, String fieldName) {
            results.add(validateRequired(value, fieldName));
            return this;
        }

        public ValidationBuilder email(String value) {
            results.add(validateEmail(value));
            return this;
        }

        public ValidationBuilder url(String value) {
            results.add(validateUrl(value));
            return this;
        }

        public ValidationBuilder length(String value, int min, int max, String fieldName) {
            results.add(validateLength(value, min, max, fieldName));
            return this;
        }

        public ValidationBuilder range(int value, int min, int max, String fieldName) {
            results.add(validateRange(value, min, max, fieldName));
            return this;
        }

        public boolean isValid() {
            return results.stream().allMatch(ValidationResult::isValid);
        }

        public List<String> getErrors() {
            return results.stream()
                .filter(r -> !r.isValid())
                .map(ValidationResult::errorMessage)
                .toList();
        }

        public String getFirstError() {
            return results.stream()
                .filter(r -> !r.isValid())
                .map(ValidationResult::errorMessage)
                .findFirst()
                .orElse(null);
        }
    }

    /**
     * Create validation builder.
     */
    public static ValidationBuilder builder() {
        return new ValidationBuilder();
    }
}