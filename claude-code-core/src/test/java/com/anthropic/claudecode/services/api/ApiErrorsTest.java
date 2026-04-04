/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.net.SocketTimeoutException;
import java.net.ConnectException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ApiErrors.
 */
class ApiErrorsTest {

    @Test
    @DisplayName("ApiErrors error message constants")
    void errorConstants() {
        assertEquals("API Error", ApiErrors.API_ERROR_MESSAGE_PREFIX);
        assertEquals("Prompt is too long", ApiErrors.PROMPT_TOO_LONG_ERROR_MESSAGE);
        assertEquals("Credit balance is too low", ApiErrors.CREDIT_BALANCE_TOO_LOW_ERROR_MESSAGE);
        assertNotNull(ApiErrors.INVALID_API_KEY_ERROR_MESSAGE);
        assertNotNull(ApiErrors.REPEATED_529_ERROR_MESSAGE);
        assertNotNull(ApiErrors.API_TIMEOUT_ERROR_MESSAGE);
    }

    @Test
    @DisplayName("ApiErrors startsWithApiErrorPrefix true for API error")
    void startsWithApiErrorPrefixTrue() {
        assertTrue(ApiErrors.startsWithApiErrorPrefix("API Error: something went wrong"));
    }

    @Test
    @DisplayName("ApiErrors startsWithApiErrorPrefix true for login prefix")
    void startsWithApiErrorPrefixLogin() {
        assertTrue(ApiErrors.startsWithApiErrorPrefix("Please run /login · API Error: auth failed"));
    }

    @Test
    @DisplayName("ApiErrors startsWithApiErrorPrefix false for other message")
    void startsWithApiErrorPrefixFalse() {
        assertFalse(ApiErrors.startsWithApiErrorPrefix("Some other error"));
    }

    @Test
    @DisplayName("ApiErrors startsWithApiErrorPrefix false for null")
    void startsWithApiErrorPrefixNull() {
        assertFalse(ApiErrors.startsWithApiErrorPrefix(null));
    }

    @Test
    @DisplayName("ApiErrors parsePromptTooLongTokenCounts extracts tokens")
    void parsePromptTooLongTokenCounts() {
        ApiErrors.TokenCounts counts = ApiErrors.parsePromptTooLongTokenCounts(
            "prompt is too long: 5000 tokens > 100000"
        );
        assertEquals(5000, counts.actualTokens());
        assertEquals(100000, counts.limitTokens());
    }

    @Test
    @DisplayName("ApiErrors parsePromptTooLongTokenCounts null message")
    void parsePromptTooLongTokenCountsNull() {
        ApiErrors.TokenCounts counts = ApiErrors.parsePromptTooLongTokenCounts(null);
        assertNull(counts.actualTokens());
        assertNull(counts.limitTokens());
    }

    @Test
    @DisplayName("ApiErrors parsePromptTooLongTokenCounts no match")
    void parsePromptTooLongTokenCountsNoMatch() {
        ApiErrors.TokenCounts counts = ApiErrors.parsePromptTooLongTokenCounts(
            "some other error message"
        );
        assertNull(counts.actualTokens());
        assertNull(counts.limitTokens());
    }

    @Test
    @DisplayName("ApiErrors TokenCounts record")
    void tokenCountsRecord() {
        ApiErrors.TokenCounts counts = new ApiErrors.TokenCounts(100, 200);
        assertEquals(100, counts.actualTokens());
        assertEquals(200, counts.limitTokens());
    }

    @Test
    @DisplayName("ApiErrors isMediaSizeError true for image exceeds")
    void isMediaSizeErrorImage() {
        assertTrue(ApiErrors.isMediaSizeError(
            "image exceeds 5MB maximum size"
        ));
    }

    @Test
    @DisplayName("ApiErrors isMediaSizeError true for PDF pages")
    void isMediaSizeErrorPdf() {
        assertTrue(ApiErrors.isMediaSizeError(
            "maximum of 100 PDF pages allowed"
        ));
    }

    @Test
    @DisplayName("ApiErrors isMediaSizeError true for image dimensions")
    void isMediaSizeErrorDimensions() {
        assertTrue(ApiErrors.isMediaSizeError(
            "image dimensions exceed many-image limits"
        ));
    }

    @Test
    @DisplayName("ApiErrors isMediaSizeError false for other")
    void isMediaSizeErrorFalse() {
        assertFalse(ApiErrors.isMediaSizeError("some other error"));
    }

    @Test
    @DisplayName("ApiErrors isMediaSizeError false for null")
    void isMediaSizeErrorNull() {
        assertFalse(ApiErrors.isMediaSizeError(null));
    }

    @Test
    @DisplayName("ApiErrors getPdfTooLargeErrorMessage non-interactive")
    void getPdfTooLargeErrorMessageNonInteractive() {
        String msg = ApiErrors.getPdfTooLargeErrorMessage(true);
        assertTrue(msg.contains("PDF too large"));
        assertTrue(msg.contains("100 pages"));
    }

    @Test
    @DisplayName("ApiErrors getPdfTooLargeErrorMessage interactive")
    void getPdfTooLargeErrorMessageInteractive() {
        String msg = ApiErrors.getPdfTooLargeErrorMessage(false);
        assertTrue(msg.contains("PDF too large"));
        assertTrue(msg.contains("esc"));
    }

    @Test
    @DisplayName("ApiErrors getPdfPasswordProtectedErrorMessage")
    void getPdfPasswordProtectedErrorMessage() {
        String msg = ApiErrors.getPdfPasswordProtectedErrorMessage(true);
        assertTrue(msg.contains("password protected"));
    }

    @Test
    @DisplayName("ApiErrors getPdfInvalidErrorMessage")
    void getPdfInvalidErrorMessage() {
        String msg = ApiErrors.getPdfInvalidErrorMessage(true);
        assertTrue(msg.contains("not valid"));
    }

    @Test
    @DisplayName("ApiErrors getImageTooLargeErrorMessage")
    void getImageTooLargeErrorMessage() {
        String msg = ApiErrors.getImageTooLargeErrorMessage(true);
        assertTrue(msg.contains("too large"));
    }

    @Test
    @DisplayName("ApiErrors getRequestTooLargeErrorMessage")
    void getRequestTooLargeErrorMessage() {
        String msg = ApiErrors.getRequestTooLargeErrorMessage(true);
        assertTrue(msg.contains("too large"));
        assertTrue(msg.contains("32MB"));
    }

    @Test
    @DisplayName("ApiErrors ErrorType enum values")
    void errorTypeEnum() {
        ApiErrors.ErrorType[] types = ApiErrors.ErrorType.values();
        assertTrue(types.length > 10);
        assertEquals(ApiErrors.ErrorType.ABORTED, ApiErrors.ErrorType.valueOf("ABORTED"));
        assertEquals(ApiErrors.ErrorType.API_TIMEOUT, ApiErrors.ErrorType.valueOf("API_TIMEOUT"));
        assertEquals(ApiErrors.ErrorType.UNKNOWN, ApiErrors.ErrorType.valueOf("UNKNOWN"));
    }

    @Test
    @DisplayName("ApiErrors classifyAPIError aborted")
    void classifyApiErrorAborted() {
        Exception error = new RuntimeException("Request was aborted.");
        ApiErrors.ErrorType type = ApiErrors.classifyAPIError(error, 0);
        assertEquals(ApiErrors.ErrorType.ABORTED, type);
    }

    @Test
    @DisplayName("ApiErrors classifyAPIError timeout exception")
    void classifyApiErrorTimeoutException() {
        Exception error = new SocketTimeoutException("connection timed out");
        ApiErrors.ErrorType type = ApiErrors.classifyAPIError(error, 0);
        assertEquals(ApiErrors.ErrorType.API_TIMEOUT, type);
    }

    @Test
    @DisplayName("ApiErrors classifyAPIError timeout message")
    void classifyApiErrorTimeoutMessage() {
        Exception error = new RuntimeException("Request timeout occurred");
        ApiErrors.ErrorType type = ApiErrors.classifyAPIError(error, 0);
        assertEquals(ApiErrors.ErrorType.API_TIMEOUT, type);
    }

    @Test
    @DisplayName("ApiErrors classifyAPIError rate limit 429")
    void classifyApiErrorRateLimit() {
        Exception error = new RuntimeException("Rate limited");
        ApiErrors.ErrorType type = ApiErrors.classifyAPIError(error, 429);
        assertEquals(ApiErrors.ErrorType.RATE_LIMIT, type);
    }

    @Test
    @DisplayName("ApiErrors classifyAPIError server overload 529")
    void classifyApiErrorServerOverload() {
        Exception error = new RuntimeException("Server overloaded");
        ApiErrors.ErrorType type = ApiErrors.classifyAPIError(error, 529);
        assertEquals(ApiErrors.ErrorType.SERVER_OVERLOAD, type);
    }

    @Test
    @DisplayName("ApiErrors classifyAPIError prompt too long")
    void classifyApiErrorPromptTooLong() {
        Exception error = new RuntimeException("Prompt is too long: exceeded limit");
        ApiErrors.ErrorType type = ApiErrors.classifyAPIError(error, 400);
        assertEquals(ApiErrors.ErrorType.PROMPT_TOO_LONG, type);
    }

    @Test
    @DisplayName("ApiErrors classifyAPIError invalid API key")
    void classifyApiErrorInvalidApiKey() {
        Exception error = new RuntimeException("missing x-api-key header");
        ApiErrors.ErrorType type = ApiErrors.classifyAPIError(error, 401);
        assertEquals(ApiErrors.ErrorType.INVALID_API_KEY, type);
    }

    @Test
    @DisplayName("ApiErrors classifyAPIError credit balance low")
    void classifyApiErrorCreditBalance() {
        Exception error = new RuntimeException("Credit balance is too low for this request");
        ApiErrors.ErrorType type = ApiErrors.classifyAPIError(error, 400);
        assertEquals(ApiErrors.ErrorType.CREDIT_BALANCE_LOW, type);
    }

    @Test
    @DisplayName("ApiErrors classifyAPIError server error 500")
    void classifyApiErrorServerError() {
        Exception error = new RuntimeException("Internal server error");
        ApiErrors.ErrorType type = ApiErrors.classifyAPIError(error, 500);
        assertEquals(ApiErrors.ErrorType.SERVER_ERROR, type);
    }

    @Test
    @DisplayName("ApiErrors classifyAPIError client error 400")
    void classifyApiErrorClientError() {
        Exception error = new RuntimeException("Bad request");
        ApiErrors.ErrorType type = ApiErrors.classifyAPIError(error, 400);
        assertEquals(ApiErrors.ErrorType.CLIENT_ERROR, type);
    }

    @Test
    @DisplayName("ApiErrors classifyAPIError null error")
    void classifyApiErrorNull() {
        ApiErrors.ErrorType type = ApiErrors.classifyAPIError(null, 0);
        assertEquals(ApiErrors.ErrorType.UNKNOWN, type);
    }

    @Test
    @DisplayName("ApiErrors classifyAPIError connection error")
    void classifyApiErrorConnection() {
        Exception error = new ConnectException("Connection refused");
        ApiErrors.ErrorType type = ApiErrors.classifyAPIError(error, 0);
        assertEquals(ApiErrors.ErrorType.CONNECTION_ERROR, type);
    }

    @Test
    @DisplayName("ApiErrors categorizeRetryableAPIError rate limit 529")
    void categorizeRetryableApiError529() {
        String category = ApiErrors.categorizeRetryableAPIError(529, null);
        assertEquals("rate_limit", category);
    }

    @Test
    @DisplayName("ApiErrors categorizeRetryableAPIError rate limit 429")
    void categorizeRetryableApiError429() {
        String category = ApiErrors.categorizeRetryableAPIError(429, null);
        assertEquals("rate_limit", category);
    }

    @Test
    @DisplayName("ApiErrors categorizeRetryableAPIError authentication")
    void categorizeRetryableApiErrorAuth() {
        String category = ApiErrors.categorizeRetryableAPIError(401, null);
        assertEquals("authentication_failed", category);
    }

    @Test
    @DisplayName("ApiErrors categorizeRetryableAPIError server error")
    void categorizeRetryableApiErrorServer() {
        String category = ApiErrors.categorizeRetryableAPIError(503, null);
        assertEquals("server_error", category);
    }

    @Test
    @DisplayName("ApiErrors categorizeRetryableAPIError unknown")
    void categorizeRetryableApiErrorUnknown() {
        String category = ApiErrors.categorizeRetryableAPIError(200, null);
        assertEquals("unknown", category);
    }
}