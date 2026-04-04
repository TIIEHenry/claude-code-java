/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ValidationUtils.
 */
class ValidationUtilsTest {

    @Test
    @DisplayName("ValidationUtils valid creates valid result")
    void validCreates() {
        ValidationUtils.ValidationResult result = ValidationUtils.valid();

        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("ValidationUtils invalid creates invalid result")
    void invalidCreates() {
        ValidationUtils.ValidationResult result = ValidationUtils.invalid("error message");

        assertFalse(result.isValid());
        assertEquals(1, result.getErrors().size());
        assertEquals("error message", result.getErrors().get(0));
    }

    @Test
    @DisplayName("ValidationUtils invalid with multiple errors")
    void invalidMultipleErrors() {
        ValidationUtils.ValidationResult result = ValidationUtils.invalid(List.of("error1", "error2"));

        assertEquals(2, result.getErrors().size());
    }

    @Test
    @DisplayName("ValidationUtils and combines errors")
    void andCombines() {
        ValidationUtils.ValidationResult r1 = ValidationUtils.invalid("error1");
        ValidationUtils.ValidationResult r2 = ValidationUtils.invalid("error2");

        ValidationUtils.ValidationResult combined = r1.and(r2);

        assertFalse(combined.isValid());
        assertEquals(2, combined.getErrors().size());
    }

    @Test
    @DisplayName("ValidationUtils and with valid returns other")
    void andWithValid() {
        ValidationUtils.ValidationResult valid = ValidationUtils.valid();
        ValidationUtils.ValidationResult invalid = ValidationUtils.invalid("error");

        ValidationUtils.ValidationResult result = valid.and(invalid);

        assertFalse(result.isValid());
    }

    @Test
    @DisplayName("ValidationUtils or returns first if valid")
    void orReturnsFirst() {
        ValidationUtils.ValidationResult valid = ValidationUtils.valid();
        ValidationUtils.ValidationResult invalid = ValidationUtils.invalid("error");

        ValidationUtils.ValidationResult result = valid.or(invalid);

        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("ValidationUtils or returns second if first invalid")
    void orReturnsSecond() {
        ValidationUtils.ValidationResult invalid1 = ValidationUtils.invalid("error1");
        ValidationUtils.ValidationResult valid = ValidationUtils.valid();

        ValidationUtils.ValidationResult result = invalid1.or(valid);

        assertTrue(result.isValid());
    }

    @Test
    @DisplayName("ValidationUtils notNull validates null")
    void notNullValidates() {
        assertTrue(ValidationUtils.notNull("value", "field").isValid());
        assertFalse(ValidationUtils.notNull(null, "field").isValid());
    }

    @Test
    @DisplayName("ValidationUtils notEmpty validates empty string")
    void notEmptyValidates() {
        assertTrue(ValidationUtils.notEmpty("value", "field").isValid());
        assertFalse(ValidationUtils.notEmpty("", "field").isValid());
        assertFalse(ValidationUtils.notEmpty((String) null, "field").isValid());
    }

    @Test
    @DisplayName("ValidationUtils notBlank validates blank string")
    void notBlankValidates() {
        assertTrue(ValidationUtils.notBlank("value", "field").isValid());
        assertFalse(ValidationUtils.notBlank("", "field").isValid());
        assertFalse(ValidationUtils.notBlank("   ", "field").isValid());
        assertFalse(ValidationUtils.notBlank(null, "field").isValid());
    }

    @Test
    @DisplayName("ValidationUtils length validates string length")
    void lengthValidates() {
        assertTrue(ValidationUtils.length("abc", 1, 5, "field").isValid());
        assertFalse(ValidationUtils.length("abc", 5, 10, "field").isValid());
        assertFalse(ValidationUtils.length(null, 1, 5, "field").isValid());
    }

    @Test
    @DisplayName("ValidationUtils minLength validates minimum length")
    void minLengthValidates() {
        assertTrue(ValidationUtils.minLength("abc", 3, "field").isValid());
        assertFalse(ValidationUtils.minLength("ab", 3, "field").isValid());
    }

    @Test
    @DisplayName("ValidationUtils maxLength validates maximum length")
    void maxLengthValidates() {
        assertTrue(ValidationUtils.maxLength("abc", 5, "field").isValid());
        assertFalse(ValidationUtils.maxLength("abcdef", 5, "field").isValid());
    }

    @Test
    @DisplayName("ValidationUtils range validates numeric range")
    void rangeValidatesInt() {
        assertTrue(ValidationUtils.range(5, 1, 10, "field").isValid());
        assertFalse(ValidationUtils.range(0, 1, 10, "field").isValid());
        assertFalse(ValidationUtils.range(11, 1, 10, "field").isValid());
    }

    @Test
    @DisplayName("ValidationUtils range validates long range")
    void rangeValidatesLong() {
        assertTrue(ValidationUtils.range(5L, 1L, 10L, "field").isValid());
        assertFalse(ValidationUtils.range(0L, 1L, 10L, "field").isValid());
    }

    @Test
    @DisplayName("ValidationUtils positive validates positive")
    void positiveValidates() {
        assertTrue(ValidationUtils.positive(1, "field").isValid());
        assertFalse(ValidationUtils.positive(0, "field").isValid());
        assertFalse(ValidationUtils.positive(-1, "field").isValid());
    }

    @Test
    @DisplayName("ValidationUtils nonNegative validates non-negative")
    void nonNegativeValidates() {
        assertTrue(ValidationUtils.nonNegative(0, "field").isValid());
        assertTrue(ValidationUtils.nonNegative(1, "field").isValid());
        assertFalse(ValidationUtils.nonNegative(-1, "field").isValid());
    }

    @Test
    @DisplayName("ValidationUtils matches validates pattern")
    void matchesValidates() {
        assertTrue(ValidationUtils.matches("abc", "^[a-z]+$", "field").isValid());
        assertFalse(ValidationUtils.matches("ABC", "^[a-z]+$", "field").isValid());
    }

    @Test
    @DisplayName("ValidationUtils email validates email")
    void emailValidates() {
        assertTrue(ValidationUtils.email("test@example.com", "field").isValid());
        assertFalse(ValidationUtils.email("invalid", "field").isValid());
        assertFalse(ValidationUtils.email(null, "field").isValid());
    }

    @Test
    @DisplayName("ValidationUtils url validates URL")
    void urlValidates() {
        assertTrue(ValidationUtils.url("https://example.com", "field").isValid());
        assertFalse(ValidationUtils.url("invalid", "field").isValid());
    }

    @Test
    @DisplayName("ValidationUtils notEmpty validates collection")
    void notEmptyCollection() {
        assertTrue(ValidationUtils.notEmpty(List.of("a"), "field").isValid());
        assertFalse(ValidationUtils.notEmpty(List.of(), "field").isValid());
    }

    @Test
    @DisplayName("ValidationUtils size validates collection size")
    void sizeValidates() {
        assertTrue(ValidationUtils.size(List.of("a", "b"), 1, 3, "field").isValid());
        assertFalse(ValidationUtils.size(List.of(), 1, 3, "field").isValid());
    }

    @Test
    @DisplayName("ValidationUtils validator creates validator")
    void validatorCreates() {
        ValidationUtils.Validator<String> validator = ValidationUtils.<String>validator()
            .rule(s -> s != null, "cannot be null")
            .rule(s -> s != null && s.length() > 0, "cannot be empty");

        assertTrue(validator.isValid("test"));
        assertFalse(validator.isValid(null));
        assertFalse(validator.isValid(""));
    }

    @Test
    @DisplayName("ValidationUtils assertValid throws on invalid")
    void assertValidThrows() {
        ValidationUtils.Validator<String> validator = ValidationUtils.<String>validator()
            .rule(s -> s != null, "cannot be null");

        assertThrows(ValidationUtils.ValidationException.class,
            () -> validator.assertValid(null));
    }

    @Test
    @DisplayName("ValidationUtils ValidationException contains errors")
    void validationExceptionErrors() {
        ValidationUtils.ValidationException ex =
            new ValidationUtils.ValidationException(List.of("error1", "error2"));

        assertEquals(2, ex.getErrors().size());
        assertTrue(ex.getMessage().contains("error1"));
    }
}