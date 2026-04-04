/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/api/errors.ts
 */
package com.anthropic.claudecode.services.api;

import java.util.*;

/**
 * API Exception types.
 */
public class ApiException extends RuntimeException {
    private final String code;
    private final int statusCode;
    private final Map<String, String> headers;

    public ApiException(String message) {
        super(message);
        this.code = "unknown_error";
        this.statusCode = 0;
        this.headers = Map.of();
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.code = "unknown_error";
        this.statusCode = 0;
        this.headers = Map.of();
    }

    public ApiException(String code, String message, int statusCode) {
        super(message);
        this.code = code;
        this.statusCode = statusCode;
        this.headers = Map.of();
    }

    public ApiException(String code, String message, int statusCode, Map<String, String> headers) {
        super(message);
        this.code = code;
        this.statusCode = statusCode;
        this.headers = headers != null ? headers : Map.of();
    }

    public String getCode() {
        return code;
    }

    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Check if this error is retryable.
     */
    public boolean isRetryable() {
        return statusCode >= 500 || statusCode == 429;
    }

    /**
     * Check if this is an authentication error.
     */
    public boolean isAuthenticationError() {
        return statusCode == 401 || statusCode == 403;
    }

    /**
     * Check if this is a rate limit error.
     */
    public boolean isRateLimitError() {
        return statusCode == 429;
    }

    /**
     * Check if this is an overload error.
     */
    public boolean isOverloadError() {
        return statusCode == 529;
    }

    /**
     * Get a header value from the error response.
     */
    public String getHeader(String name) {
        if (headers == null) return null;
        // Case-insensitive lookup
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(name)) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Get all headers.
     */
    public Map<String, String> getHeaders() {
        return headers != null ? Collections.unmodifiableMap(headers) : Map.of();
    }
}