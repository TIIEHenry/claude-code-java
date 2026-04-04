/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/api/errors.ts
 */
package com.anthropic.claudecode.services.api;

import java.util.*;
import java.util.regex.*;

/**
 * API error message constants and handling utilities.
 */
public final class ApiErrors {
    private ApiErrors() {}

    // Error message constants
    public static final String API_ERROR_MESSAGE_PREFIX = "API Error";
    public static final String PROMPT_TOO_LONG_ERROR_MESSAGE = "Prompt is too long";
    public static final String CREDIT_BALANCE_TOO_LOW_ERROR_MESSAGE = "Credit balance is too low";
    public static final String INVALID_API_KEY_ERROR_MESSAGE = "Not logged in · Please run /login";
    public static final String INVALID_API_KEY_ERROR_MESSAGE_EXTERNAL =
        "Invalid API key · Fix external API key";
    public static final String ORG_DISABLED_ERROR_MESSAGE_ENV_KEY_WITH_OAUTH =
        "Your ANTHROPIC_API_KEY belongs to a disabled organization · " +
        "Unset the environment variable to use your subscription instead";
    public static final String ORG_DISABLED_ERROR_MESSAGE_ENV_KEY =
        "Your ANTHROPIC_API_KEY belongs to a disabled organization · " +
        "Update or unset the environment variable";
    public static final String TOKEN_REVOKED_ERROR_MESSAGE =
        "OAuth token revoked · Please run /login";
    public static final String CCR_AUTH_ERROR_MESSAGE =
        "Authentication error · This may be a temporary network issue, please try again";
    public static final String REPEATED_529_ERROR_MESSAGE = "Repeated 529 Overloaded errors";
    public static final String CUSTOM_OFF_SWITCH_MESSAGE =
        "Opus is experiencing high load, please use /model to switch to Sonnet";
    public static final String API_TIMEOUT_ERROR_MESSAGE = "Request timed out";
    public static final String OAUTH_ORG_NOT_ALLOWED_ERROR_MESSAGE =
        "Your account does not have access to Claude Code. Please run /login.";

    /**
     * Check if text starts with API error prefix.
     */
    public static boolean startsWithApiErrorPrefix(String text) {
        if (text == null) return false;
        return text.startsWith(API_ERROR_MESSAGE_PREFIX) ||
               text.startsWith("Please run /login · " + API_ERROR_MESSAGE_PREFIX);
    }

    /**
     * Parse token counts from prompt-too-long error message.
     */
    public static TokenCounts parsePromptTooLongTokenCounts(String rawMessage) {
        if (rawMessage == null) {
            return new TokenCounts(null, null);
        }
        Pattern pattern = Pattern.compile(
            "prompt is too long[^0-9]*(\\d+)\\s*tokens?\\s*>\\s*(\\d+)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(rawMessage);
        if (matcher.find()) {
            return new TokenCounts(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2))
            );
        }
        return new TokenCounts(null, null);
    }

    /**
     * Token counts from error message.
     */
    public record TokenCounts(Integer actualTokens, Integer limitTokens) {}

    /**
     * Check if raw error text is a media-size error.
     */
    public static boolean isMediaSizeError(String raw) {
        if (raw == null) return false;
        return (raw.contains("image exceeds") && raw.contains("maximum")) ||
               (raw.contains("image dimensions exceed") && raw.contains("many-image")) ||
               Pattern.compile("maximum of \\d+ PDF pages").matcher(raw).find();
    }

    /**
     * Get PDF too large error message.
     */
    public static String getPdfTooLargeErrorMessage(boolean nonInteractive) {
        String limits = "max 100 pages, 32MB";
        return nonInteractive
            ? "PDF too large (" + limits + "). Try reading the file a different way."
            : "PDF too large (" + limits + "). Double press esc to go back and try again.";
    }

    /**
     * Get PDF password protected error message.
     */
    public static String getPdfPasswordProtectedErrorMessage(boolean nonInteractive) {
        return nonInteractive
            ? "PDF is password protected. Try using a CLI tool to extract or convert the PDF."
            : "PDF is password protected. Please double press esc to edit your message.";
    }

    /**
     * Get PDF invalid error message.
     */
    public static String getPdfInvalidErrorMessage(boolean nonInteractive) {
        return nonInteractive
            ? "The PDF file was not valid. Try converting it to text first."
            : "The PDF file was not valid. Double press esc to go back and try again.";
    }

    /**
     * Get image too large error message.
     */
    public static String getImageTooLargeErrorMessage(boolean nonInteractive) {
        return nonInteractive
            ? "Image was too large. Try resizing the image or using a different approach."
            : "Image was too large. Double press esc to go back and try again.";
    }

    /**
     * Get request too large error message.
     */
    public static String getRequestTooLargeErrorMessage(boolean nonInteractive) {
        String limits = "max 32MB";
        return nonInteractive
            ? "Request too large (" + limits + "). Try with a smaller file."
            : "Request too large (" + limits + "). Double press esc to go back and try again.";
    }

    /**
     * Get token revoked error message.
     */
    public static String getTokenRevokedErrorMessage(boolean nonInteractive) {
        return nonInteractive
            ? "Your account does not have access to Claude. Please login again."
            : TOKEN_REVOKED_ERROR_MESSAGE;
    }

    /**
     * Get OAuth org not allowed error message.
     */
    public static String getOauthOrgNotAllowedErrorMessage(boolean nonInteractive) {
        return nonInteractive
            ? "Your organization does not have access to Claude. Please login again."
            : OAUTH_ORG_NOT_ALLOWED_ERROR_MESSAGE;
    }

    /**
     * Error type classification.
     */
    public enum ErrorType {
        ABORTED,
        API_TIMEOUT,
        REPEATED_529,
        CAPACITY_OFF_SWITCH,
        RATE_LIMIT,
        SERVER_OVERLOAD,
        PROMPT_TOO_LONG,
        PDF_TOO_LARGE,
        PDF_PASSWORD_PROTECTED,
        IMAGE_TOO_LARGE,
        TOOL_USE_MISMATCH,
        UNEXPECTED_TOOL_RESULT,
        DUPLICATE_TOOL_USE_ID,
        INVALID_MODEL,
        CREDIT_BALANCE_LOW,
        INVALID_API_KEY,
        TOKEN_REVOKED,
        OAUTH_ORG_NOT_ALLOWED,
        AUTH_ERROR,
        BEDROCK_MODEL_ACCESS,
        SSL_CERT_ERROR,
        CONNECTION_ERROR,
        SERVER_ERROR,
        CLIENT_ERROR,
        UNKNOWN
    }

    /**
     * Classify an API error into a specific type.
     */
    public static ErrorType classifyAPIError(Throwable error, int statusCode) {
        if (error == null) {
            return ErrorType.UNKNOWN;
        }

        String message = error.getMessage();
        if (message == null) {
            message = "";
        }

        // Aborted requests
        if ("Request was aborted.".equals(message)) {
            return ErrorType.ABORTED;
        }

        // Timeout errors
        if (error instanceof java.net.SocketTimeoutException ||
            message.toLowerCase().contains("timeout")) {
            return ErrorType.API_TIMEOUT;
        }

        // Repeated 529 errors
        if (message.contains(REPEATED_529_ERROR_MESSAGE)) {
            return ErrorType.REPEATED_529;
        }

        // Capacity off switch
        if (message.contains(CUSTOM_OFF_SWITCH_MESSAGE)) {
            return ErrorType.CAPACITY_OFF_SWITCH;
        }

        // Rate limiting
        if (statusCode == 429) {
            return ErrorType.RATE_LIMIT;
        }

        // Server overload
        if (statusCode == 529 || message.contains("overloaded_error")) {
            return ErrorType.SERVER_OVERLOAD;
        }

        // Prompt too long
        if (message.toLowerCase().contains(PROMPT_TOO_LONG_ERROR_MESSAGE.toLowerCase())) {
            return ErrorType.PROMPT_TOO_LONG;
        }

        // PDF errors
        if (Pattern.compile("maximum of \\d+ PDF pages").matcher(message).find()) {
            return ErrorType.PDF_TOO_LARGE;
        }
        if (message.contains("The PDF specified is password protected")) {
            return ErrorType.PDF_PASSWORD_PROTECTED;
        }

        // Image errors
        if (statusCode == 400 && message.contains("image exceeds") && message.contains("maximum")) {
            return ErrorType.IMAGE_TOO_LARGE;
        }
        if (statusCode == 400 && message.contains("image dimensions exceed") && message.contains("many-image")) {
            return ErrorType.IMAGE_TOO_LARGE;
        }

        // Tool use errors
        if (statusCode == 400 && message.contains("tool_use ids were found without tool_result")) {
            return ErrorType.TOOL_USE_MISMATCH;
        }
        if (statusCode == 400 && message.contains("unexpected tool_use_id found in tool_result")) {
            return ErrorType.UNEXPECTED_TOOL_RESULT;
        }
        if (statusCode == 400 && message.contains("tool_use ids must be unique")) {
            return ErrorType.DUPLICATE_TOOL_USE_ID;
        }

        // Invalid model
        if (statusCode == 400 && message.toLowerCase().contains("invalid model name")) {
            return ErrorType.INVALID_MODEL;
        }

        // Credit balance
        if (message.toLowerCase().contains(CREDIT_BALANCE_TOO_LOW_ERROR_MESSAGE.toLowerCase())) {
            return ErrorType.CREDIT_BALANCE_LOW;
        }

        // Authentication errors
        if (message.toLowerCase().contains("x-api-key")) {
            return ErrorType.INVALID_API_KEY;
        }
        if (statusCode == 403 && message.contains("OAuth token has been revoked")) {
            return ErrorType.TOKEN_REVOKED;
        }
        if ((statusCode == 401 || statusCode == 403) &&
            message.contains("OAuth authentication is currently not allowed")) {
            return ErrorType.OAUTH_ORG_NOT_ALLOWED;
        }
        if (statusCode == 401 || statusCode == 403) {
            return ErrorType.AUTH_ERROR;
        }

        // Bedrock errors
        if (message.toLowerCase().contains("model id")) {
            return ErrorType.BEDROCK_MODEL_ACCESS;
        }

        // Status code fallbacks
        if (statusCode >= 500) return ErrorType.SERVER_ERROR;
        if (statusCode >= 400) return ErrorType.CLIENT_ERROR;

        // Connection errors
        if (error instanceof java.net.ConnectException) {
            ApiErrorUtils.ConnectionErrorDetails details =
                ApiErrorUtils.extractConnectionErrorDetails(error);
            if (details != null && details.isSSLError()) {
                return ErrorType.SSL_CERT_ERROR;
            }
            return ErrorType.CONNECTION_ERROR;
        }

        return ErrorType.UNKNOWN;
    }

    /**
     * Categorize a retryable API error.
     */
    public static String categorizeRetryableAPIError(int statusCode, String message) {
        if (statusCode == 529 || (message != null && message.contains("overloaded_error"))) {
            return "rate_limit";
        }
        if (statusCode == 429) {
            return "rate_limit";
        }
        if (statusCode == 401 || statusCode == 403) {
            return "authentication_failed";
        }
        if (statusCode >= 408) {
            return "server_error";
        }
        return "unknown";
    }
}