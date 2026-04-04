/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode;

/**
 * Validation result for tool input.
 */
public sealed interface ValidationResult permits
        ValidationResult.Success,
        ValidationResult.Failure {

    boolean isSuccess();
    String message();
    int errorCode();

    record Success() implements ValidationResult {
        @Override
        public boolean isSuccess() { return true; }
        @Override
        public String message() { return null; }
        @Override
        public int errorCode() { return 0; }
    }

    record Failure(String message, int errorCode) implements ValidationResult {
        @Override
        public boolean isSuccess() { return false; }

        public static Failure of(String message) {
            return new Failure(message, 1);
        }

        public static Failure of(String message, int errorCode) {
            return new Failure(message, errorCode);
        }
    }

    static ValidationResult success() {
        return new Success();
    }

    static ValidationResult failure(String message) {
        return new Failure(message, 1);
    }

    static ValidationResult failure(String message, int errorCode) {
        return new Failure(message, errorCode);
    }
}