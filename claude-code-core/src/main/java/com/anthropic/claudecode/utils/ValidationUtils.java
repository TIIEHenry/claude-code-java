/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code validation utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.function.*;
import java.util.regex.*;

/**
 * Validation utilities for input checking.
 */
public final class ValidationUtils {
    private ValidationUtils() {}

    /**
     * Validation result.
     */
    public sealed interface ValidationResult permits ValidationResult.Valid, ValidationResult.Invalid {
        boolean isValid();
        List<String> getErrors();
        ValidationResult and(ValidationResult other);
        ValidationResult or(ValidationResult other);

        record Valid() implements ValidationResult {
            @Override
            public boolean isValid() { return true; }

            @Override
            public List<String> getErrors() { return List.of(); }

            @Override
            public ValidationResult and(ValidationResult other) {
                return other;
            }

            @Override
            public ValidationResult or(ValidationResult other) {
                return this;
            }
        }

        record Invalid(List<String> errors) implements ValidationResult {
            @Override
            public boolean isValid() { return false; }

            @Override
            public List<String> getErrors() { return errors; }

            @Override
            public ValidationResult and(ValidationResult other) {
                if (other.isValid()) return this;
                List<String> combined = new ArrayList<>(errors);
                combined.addAll(other.getErrors());
                return new Invalid(combined);
            }

            @Override
            public ValidationResult or(ValidationResult other) {
                if (other.isValid()) return other;
                return this;
            }
        }
    }

    /**
     * Create a valid result.
     */
    public static ValidationResult valid() {
        return new ValidationResult.Valid();
    }

    /**
     * Create an invalid result.
     */
    public static ValidationResult invalid(String error) {
        return new ValidationResult.Invalid(List.of(error));
    }

    /**
     * Create an invalid result with multiple errors.
     */
    public static ValidationResult invalid(List<String> errors) {
        return new ValidationResult.Invalid(errors);
    }

    /**
     * Validator builder.
     */
    public static class Validator<T> {
        private final List<ValidationRule<T>> rules = new ArrayList<>();

        public Validator<T> rule(Predicate<T> predicate, String errorMessage) {
            rules.add(new ValidationRule<>(predicate, errorMessage));
            return this;
        }

        public Validator<T> rule(ValidationRule<T> rule) {
            rules.add(rule);
            return this;
        }

        public ValidationResult validate(T value) {
            List<String> errors = new ArrayList<>();
            for (ValidationRule<T> rule : rules) {
                if (!rule.predicate().test(value)) {
                    errors.add(rule.errorMessage());
                }
            }
            return errors.isEmpty() ? valid() : invalid(errors);
        }

        public boolean isValid(T value) {
            return validate(value).isValid();
        }

        public void assertValid(T value) {
            ValidationResult result = validate(value);
            if (!result.isValid()) {
                throw new ValidationException(result.getErrors());
            }
        }
    }

    /**
     * Validation rule record.
     */
    public record ValidationRule<T>(Predicate<T> predicate, String errorMessage) {}

    /**
     * Validation exception.
     */
    public static class ValidationException extends RuntimeException {
        private final List<String> errors;

        public ValidationException(List<String> errors) {
            super(String.join(", ", errors));
            this.errors = errors;
        }

        public List<String> getErrors() {
            return errors;
        }
    }

    /**
     * Create a validator.
     */
    public static <T> Validator<T> validator() {
        return new Validator<>();
    }

    // Common validators

    /**
     * Validate not null.
     */
    public static <T> ValidationResult notNull(T value, String fieldName) {
        return value != null ? valid() : invalid(fieldName + " is required");
    }

    /**
     * Validate not empty string.
     */
    public static ValidationResult notEmpty(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            return invalid(fieldName + " cannot be empty");
        }
        return valid();
    }

    /**
     * Validate not blank string.
     */
    public static ValidationResult notBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return invalid(fieldName + " cannot be blank");
        }
        return valid();
    }

    /**
     * Validate string length.
     */
    public static ValidationResult length(String value, int min, int max, String fieldName) {
        if (value == null) return invalid(fieldName + " is required");
        int len = value.length();
        if (len < min || len > max) {
            return invalid(fieldName + " must be between " + min + " and " + max + " characters");
        }
        return valid();
    }

    /**
     * Validate minimum length.
     */
    public static ValidationResult minLength(String value, int min, String fieldName) {
        if (value == null || value.length() < min) {
            return invalid(fieldName + " must be at least " + min + " characters");
        }
        return valid();
    }

    /**
     * Validate maximum length.
     */
    public static ValidationResult maxLength(String value, int max, String fieldName) {
        if (value != null && value.length() > max) {
            return invalid(fieldName + " must be at most " + max + " characters");
        }
        return valid();
    }

    /**
     * Validate numeric range.
     */
    public static ValidationResult range(int value, int min, int max, String fieldName) {
        if (value < min || value > max) {
            return invalid(fieldName + " must be between " + min + " and " + max);
        }
        return valid();
    }

    /**
     * Validate numeric range.
     */
    public static ValidationResult range(long value, long min, long max, String fieldName) {
        if (value < min || value > max) {
            return invalid(fieldName + " must be between " + min + " and " + max);
        }
        return valid();
    }

    /**
     * Validate positive number.
     */
    public static ValidationResult positive(long value, String fieldName) {
        return value > 0 ? valid() : invalid(fieldName + " must be positive");
    }

    /**
     * Validate non-negative number.
     */
    public static ValidationResult nonNegative(long value, String fieldName) {
        return value >= 0 ? valid() : invalid(fieldName + " must be non-negative");
    }

    /**
     * Validate regex pattern.
     */
    public static ValidationResult matches(String value, String pattern, String fieldName) {
        if (value == null || !value.matches(pattern)) {
            return invalid(fieldName + " has invalid format");
        }
        return valid();
    }

    /**
     * Validate email format.
     */
    public static ValidationResult email(String value, String fieldName) {
        String emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        if (value == null || !value.matches(emailPattern)) {
            return invalid(fieldName + " is not a valid email address");
        }
        return valid();
    }

    /**
     * Validate URL format.
     */
    public static ValidationResult url(String value, String fieldName) {
        String urlPattern = "^(https?|ftp)://[^\\s/$.?#].[^\\s]*$";
        if (value == null || !value.matches(urlPattern)) {
            return invalid(fieldName + " is not a valid URL");
        }
        return valid();
    }

    /**
     * Validate enum value.
     */
    public static <E extends Enum<E>> ValidationResult enumValue(String value, Class<E> enumClass, String fieldName) {
        if (value == null) return invalid(fieldName + " is required");
        try {
            Enum.valueOf(enumClass, value.toUpperCase());
            return valid();
        } catch (IllegalArgumentException e) {
            String validValues = Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
            return invalid(fieldName + " must be one of: " + validValues);
        }
    }

    /**
     * Validate collection not empty.
     */
    public static <C extends Collection<?>> ValidationResult notEmpty(C collection, String fieldName) {
        if (collection == null || collection.isEmpty()) {
            return invalid(fieldName + " cannot be empty");
        }
        return valid();
    }

    /**
     * Validate collection size.
     */
    public static <C extends Collection<?>> ValidationResult size(C collection, int min, int max, String fieldName) {
        if (collection == null) return invalid(fieldName + " is required");
        int size = collection.size();
        if (size < min || size > max) {
            return invalid(fieldName + " must have between " + min + " and " + max + " items");
        }
        return valid();
    }

    /**
     * Validate date format.
     */
    public static ValidationResult dateFormat(String value, String pattern, String fieldName) {
        try {
            java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(pattern);
            java.time.LocalDate.parse(value, formatter);
            return valid();
        } catch (Exception e) {
            return invalid(fieldName + " must be in format " + pattern);
        }
    }

    /**
     * Validate past date.
     */
    public static ValidationResult pastDate(java.time.LocalDate date, String fieldName) {
        if (date == null || !date.isBefore(java.time.LocalDate.now())) {
            return invalid(fieldName + " must be in the past");
        }
        return valid();
    }

    /**
     * Validate future date.
     */
    public static ValidationResult futureDate(java.time.LocalDate date, String fieldName) {
        if (date == null || !date.isAfter(java.time.LocalDate.now())) {
            return invalid(fieldName + " must be in the future");
        }
        return valid();
    }

    /**
     * Combine validators.
     */
    @SafeVarargs
    public static <T> ValidationResult combine(T value, Validator<T>... validators) {
        ValidationResult result = valid();
        for (Validator<T> validator : validators) {
            result = result.and(validator.validate(value));
        }
        return result;
    }
}