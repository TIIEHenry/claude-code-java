/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/api/errorUtils.ts
 */
package com.anthropic.claudecode.services.api;

import java.util.*;
import java.util.regex.*;

/**
 * API error utilities for extracting and formatting error details.
 */
public final class ApiErrorUtils {
    private ApiErrorUtils() {}

    /**
     * SSL/TLS error codes from OpenSSL.
     */
    public static final Set<String> SSL_ERROR_CODES = Set.of(
        // Certificate verification errors
        "UNABLE_TO_VERIFY_LEAF_SIGNATURE",
        "UNABLE_TO_GET_ISSUER_CERT",
        "UNABLE_TO_GET_ISSUER_CERT_LOCALLY",
        "CERT_SIGNATURE_FAILURE",
        "CERT_NOT_YET_VALID",
        "CERT_HAS_EXPIRED",
        "CERT_REVOKED",
        "CERT_REJECTED",
        "CERT_UNTRUSTED",
        // Self-signed certificate errors
        "DEPTH_ZERO_SELF_SIGNED_CERT",
        "SELF_SIGNED_CERT_IN_CHAIN",
        // Chain errors
        "CERT_CHAIN_TOO_LONG",
        "PATH_LENGTH_EXCEEDED",
        // Hostname/altname errors
        "ERR_TLS_CERT_ALTNAME_INVALID",
        "HOSTNAME_MISMATCH",
        // TLS handshake errors
        "ERR_TLS_HANDSHAKE_TIMEOUT",
        "ERR_SSL_WRONG_VERSION_NUMBER",
        "ERR_SSL_DECRYPTION_FAILED_OR_BAD_RECORD_MAC"
    );

    /**
     * Connection error details extracted from error cause chain.
     */
    public record ConnectionErrorDetails(
        String code,
        String message,
        boolean isSSLError
    ) {}

    /**
     * Extracts connection error details from the error cause chain.
     */
    public static ConnectionErrorDetails extractConnectionErrorDetails(Throwable error) {
        if (error == null) {
            return null;
        }

        // Walk the cause chain to find the root error with a code
        Throwable current = error;
        int maxDepth = 5;
        int depth = 0;

        while (current != null && depth < maxDepth) {
            String code = getErrorCode(current);
            if (code != null) {
                boolean isSSLError = SSL_ERROR_CODES.contains(code);
                return new ConnectionErrorDetails(
                    code,
                    current.getMessage(),
                    isSSLError
                );
            }

            current = current.getCause();
            depth++;
        }

        return null;
    }

    /**
     * Get error code from throwable if available.
     */
    private static String getErrorCode(Throwable error) {
        // Check for common error code patterns
        if (error instanceof java.net.SocketTimeoutException) {
            return "ETIMEDOUT";
        }
        if (error instanceof java.net.ConnectException) {
            return "ECONNREFUSED";
        }
        if (error instanceof javax.net.ssl.SSLException) {
            return "SSL_ERROR";
        }

        // Try to extract code from message
        String message = error.getMessage();
        if (message != null) {
            Pattern codePattern = Pattern.compile("code[:\\s]+(['\"]?([A-Z_]+)['\"]?)");
            Matcher matcher = codePattern.matcher(message);
            if (matcher.find()) {
                return matcher.group(2);
            }
        }

        return null;
    }

    /**
     * Returns an actionable hint for SSL/TLS errors.
     */
    public static String getSSLErrorHint(Throwable error) {
        ConnectionErrorDetails details = extractConnectionErrorDetails(error);
        if (details == null || !details.isSSLError()) {
            return null;
        }
        return String.format(
            "SSL certificate error (%s). If you are behind a corporate proxy or " +
            "TLS-intercepting firewall, set NODE_EXTRA_CA_CERTS to your CA bundle path, " +
            "or ask IT to allowlist *.anthropic.com.",
            details.code()
        );
    }

    /**
     * Strips HTML content from a message string.
     */
    public static String sanitizeMessageHTML(String message) {
        if (message == null) {
            return "";
        }
        if (message.contains("<!DOCTYPE html") || message.contains("<html")) {
            Pattern titlePattern = Pattern.compile("<title>([^<]+)</title>");
            Matcher matcher = titlePattern.matcher(message);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
            return "";
        }
        return message;
    }

    /**
     * Formats an API error for user display.
     */
    public static String formatAPIError(ApiException error) {
        ConnectionErrorDetails connectionDetails = extractConnectionErrorDetails(error);

        if (connectionDetails != null) {
            String code = connectionDetails.code();
            boolean isSSLError = connectionDetails.isSSLError();

            // Handle timeout errors
            if ("ETIMEDOUT".equals(code)) {
                return "Request timed out. Check your internet connection and proxy settings";
            }

            // Handle SSL/TLS errors with specific messages
            if (isSSLError) {
                return switch (code) {
                    case "UNABLE_TO_VERIFY_LEAF_SIGNATURE",
                         "UNABLE_TO_GET_ISSUER_CERT",
                         "UNABLE_TO_GET_ISSUER_CERT_LOCALLY" ->
                        "Unable to connect to API: SSL certificate verification failed. " +
                        "Check your proxy or corporate SSL certificates";
                    case "CERT_HAS_EXPIRED" ->
                        "Unable to connect to API: SSL certificate has expired";
                    case "CERT_REVOKED" ->
                        "Unable to connect to API: SSL certificate has been revoked";
                    case "DEPTH_ZERO_SELF_SIGNED_CERT",
                         "SELF_SIGNED_CERT_IN_CHAIN" ->
                        "Unable to connect to API: Self-signed certificate detected. " +
                        "Check your proxy or corporate SSL certificates";
                    case "ERR_TLS_CERT_ALTNAME_INVALID",
                         "HOSTNAME_MISMATCH" ->
                        "Unable to connect to API: SSL certificate hostname mismatch";
                    case "CERT_NOT_YET_VALID" ->
                        "Unable to connect to API: SSL certificate is not yet valid";
                    default ->
                        String.format("Unable to connect to API: SSL error (%s)", code);
                };
            }
        }

        String message = error.getMessage();
        if ("Connection error.".equals(message)) {
            if (connectionDetails != null && connectionDetails.code() != null) {
                return String.format(
                    "Unable to connect to API (%s)",
                    connectionDetails.code()
                );
            }
            return "Unable to connect to API. Check your internet connection";
        }

        if (message == null || message.isEmpty()) {
            return String.format("API error (status %d)", error.getStatusCode());
        }

        String sanitized = sanitizeMessageHTML(message);
        if (!sanitized.equals(message) && !sanitized.isEmpty()) {
            return sanitized;
        }

        return message;
    }
}