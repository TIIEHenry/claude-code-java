/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code HTTP utilities
 */
package com.anthropic.claudecode.utils.http;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * HTTP client utilities with retry support.
 */
public final class HttpUtils {
    private HttpUtils() {}

    private static final HttpClient sharedClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    /**
     * HTTP method enum.
     */
    public enum Method {
        GET, POST, PUT, DELETE, PATCH
    }

    /**
     * HTTP response record.
     */
    public record HttpResponse(
            int statusCode,
            String body,
            Map<String, String> headers
    ) {
        public boolean isSuccessful() {
            return statusCode >= 200 && statusCode < 300;
        }

        public boolean isClientError() {
            return statusCode >= 400 && statusCode < 500;
        }

        public boolean isServerError() {
            return statusCode >= 500;
        }
    }

    /**
     * Make a GET request.
     */
    public static HttpResponse get(String url) throws IOException {
        return get(url, Map.of());
    }

    /**
     * Make a GET request with headers.
     */
    public static HttpResponse get(String url, Map<String, String> headers) throws IOException {
        return request(Method.GET, url, headers, null);
    }

    /**
     * Make a POST request.
     */
    public static HttpResponse post(String url, String body) throws IOException {
        return post(url, Map.of(), body);
    }

    /**
     * Make a POST request with headers.
     */
    public static HttpResponse post(String url, Map<String, String> headers, String body) throws IOException {
        return request(Method.POST, url, headers, body);
    }

    /**
     * Make a PUT request.
     */
    public static HttpResponse put(String url, Map<String, String> headers, String body) throws IOException {
        return request(Method.PUT, url, headers, body);
    }

    /**
     * Make a DELETE request.
     */
    public static HttpResponse delete(String url) throws IOException {
        return request(Method.DELETE, url, Map.of(), null);
    }

    /**
     * Make an HTTP request with retry support.
     */
    public static HttpResponse request(Method method, String url, Map<String, String> headers, String body) throws IOException {
        return requestWithRetry(method, url, headers, body, MAX_RETRIES);
    }

    /**
     * Make an HTTP request with specified retries.
     */
    public static HttpResponse requestWithRetry(Method method, String url, Map<String, String> headers, String body, int maxRetries) throws IOException {
        IOException lastError = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return doRequest(method, url, headers, body);
            } catch (java.net.http.HttpTimeoutException e) {
                lastError = new IOException("Request timed out", e);
            } catch (java.net.ConnectException e) {
                lastError = new IOException("Connection failed", e);
            } catch (java.net.SocketTimeoutException e) {
                lastError = new IOException("Socket timed out", e);
            } catch (IOException e) {
                // Don't retry client errors (4xx)
                if (e.getMessage() != null && e.getMessage().contains("4")) {
                    throw e;
                }
                lastError = e;
            }

            // Wait before retry
            if (attempt < maxRetries) {
                try {
                    Thread.sleep(RETRY_DELAY_MS * (attempt + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", ie);
                }
            }
        }

        throw lastError != null ? lastError : new IOException("Request failed");
    }

    /**
     * Execute HTTP request.
     */
    private static HttpResponse doRequest(Method method, String url, Map<String, String> headers, String body) throws IOException {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url));

            // Add headers
            for (Map.Entry<String, String> header : headers.entrySet()) {
                builder.header(header.getKey(), header.getValue());
            }

            // Set method and body
            HttpRequest.BodyPublisher bodyPublisher = body != null
                    ? HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)
                    : HttpRequest.BodyPublishers.noBody();

            builder.method(method.name(), bodyPublisher);

            HttpRequest request = builder.build();
            java.net.http.HttpResponse<String> response = sharedClient.send(request, java.net.http.HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            // Extract headers
            Map<String, String> responseHeaders = new HashMap<>();
            response.headers().map().forEach((key, values) -> {
                if (!values.isEmpty()) {
                    responseHeaders.put(key, values.get(0));
                }
            });

            return new HttpResponse(response.statusCode(), response.body(), responseHeaders);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Request interrupted", e);
        }
    }

    /**
     * Make an async GET request.
     */
    public static CompletableFuture<HttpResponse> getAsync(String url) {
        return getAsync(url, Map.of());
    }

    /**
     * Make an async GET request with headers.
     */
    public static CompletableFuture<HttpResponse> getAsync(String url, Map<String, String> headers) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return get(url, headers);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

    /**
     * Make an async POST request.
     */
    public static CompletableFuture<HttpResponse> postAsync(String url, Map<String, String> headers, String body) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return post(url, headers, body);
            } catch (IOException e) {
                throw new CompletionException(e);
            }
        });
    }

    /**
     * URL encode a string.
     */
    public static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * URL decode a string.
     */
    public static String urlDecode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * Build a query string from a map.
     */
    public static String buildQueryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) sb.append("&");
            first = false;
            sb.append(urlEncode(entry.getKey()));
            sb.append("=");
            sb.append(urlEncode(entry.getValue()));
        }

        return sb.toString();
    }

    /**
     * Parse a query string into a map.
     */
    public static Map<String, String> parseQueryString(String query) {
        Map<String, String> result = new HashMap<>();

        if (query == null || query.isEmpty()) {
            return result;
        }

        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                String key = urlDecode(pair.substring(0, eq));
                String value = urlDecode(pair.substring(eq + 1));
                result.put(key, value);
            } else if (!pair.isEmpty()) {
                result.put(urlDecode(pair), "");
            }
        }

        return result;
    }

    /**
     * Check if URL is valid.
     */
    public static boolean isValidUrl(String url) {
        try {
            URI uri = URI.create(url);
            return uri.getScheme() != null && uri.getHost() != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the shared HTTP client.
     */
    public static HttpClient getSharedClient() {
        return sharedClient;
    }
}