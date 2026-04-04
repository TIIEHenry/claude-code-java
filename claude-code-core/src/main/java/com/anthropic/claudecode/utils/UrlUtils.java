/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code URL utilities
 */
package com.anthropic.claudecode.utils;

import java.net.*;
import java.util.*;
import java.util.regex.*;

/**
 * URL parsing and manipulation utilities.
 */
public final class UrlUtils {
    private UrlUtils() {}

    private static final Pattern URL_PATTERN = Pattern.compile(
        "^(https?|ftp|file):\\/\\/(?:[^@\\/]+@)?([^:\\/]+)(?::(\\d+))?([\\/\\?].*)?$",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * URL components record.
     */
    public record UrlComponents(
        String protocol,
        String host,
        int port,
        String path,
        String query,
        String fragment,
        Map<String, String> queryParams
    ) {}

    /**
     * Parse a URL into components.
     */
    public static UrlComponents parseUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }

        try {
            URI uri = new URI(url);
            String query = uri.getQuery();
            Map<String, String> queryParams = parseQueryString(query);

            int port = uri.getPort();
            if (port == -1) {
                // Default ports
                String scheme = uri.getScheme();
                if ("http".equalsIgnoreCase(scheme)) port = 80;
                else if ("https".equalsIgnoreCase(scheme)) port = 443;
                else if ("ftp".equalsIgnoreCase(scheme)) port = 21;
            }

            return new UrlComponents(
                uri.getScheme(),
                uri.getHost(),
                port,
                uri.getPath(),
                query,
                uri.getFragment(),
                queryParams
            );
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * Parse query string into map.
     */
    public static Map<String, String> parseQueryString(String query) {
        if (query == null || query.isEmpty()) {
            return Map.of();
        }

        Map<String, String> params = new LinkedHashMap<>();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            String key = decodeUrl(keyValue[0]);
            String value = keyValue.length > 1 ? decodeUrl(keyValue[1]) : "";
            params.put(key, value);
        }
        return params;
    }

    /**
     * Build query string from map.
     */
    public static String buildQueryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) sb.append("&");
            sb.append(encodeUrl(entry.getKey()));
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                sb.append("=").append(encodeUrl(entry.getValue()));
            }
        }
        return sb.toString();
    }

    /**
     * URL encode a string.
     */
    public static String encodeUrl(String value) {
        if (value == null) return null;
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * URL decode a string.
     */
    public static String decodeUrl(String value) {
        if (value == null) return null;
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (Exception e) {
            return value;
        }
    }

    /**
     * Check if a string is a valid URL.
     */
    public static boolean isValidUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        try {
            new URI(url);
            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    /**
     * Check if URL uses HTTP/HTTPS protocol.
     */
    public static boolean isHttpUrl(String url) {
        if (!isValidUrl(url)) return false;
        String scheme = url.toLowerCase();
        return scheme.startsWith("http://") || scheme.startsWith("https://");
    }

    /**
     * Get domain from URL.
     */
    public static String getDomain(String url) {
        UrlComponents components = parseUrl(url);
        return components != null ? components.host() : null;
    }

    /**
     * Get base URL (without path and query).
     */
    public static String getBaseUrl(String url) {
        UrlComponents components = parseUrl(url);
        if (components == null) return null;

        StringBuilder sb = new StringBuilder();
        sb.append(components.protocol()).append("://").append(components.host());
        if (components.port() != 80 && components.port() != 443 && components.port() != -1) {
            sb.append(":").append(components.port());
        }
        return sb.toString();
    }

    /**
     * Get path from URL.
     */
    public static String getPath(String url) {
        UrlComponents components = parseUrl(url);
        return components != null ? components.path() : null;
    }

    /**
     * Get query parameter value.
     */
    public static Optional<String> getQueryParam(String url, String param) {
        UrlComponents components = parseUrl(url);
        if (components == null || components.queryParams() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(components.queryParams().get(param));
    }

    /**
     * Add query parameter to URL.
     */
    public static String addQueryParam(String url, String key, String value) {
        UrlComponents components = parseUrl(url);
        if (components == null) return url;

        Map<String, String> params = new LinkedHashMap<>(components.queryParams());
        params.put(key, value);

        StringBuilder sb = new StringBuilder();
        sb.append(components.protocol()).append("://").append(components.host());
        if (components.port() != -1 && components.port() != 80 && components.port() != 443) {
            sb.append(":").append(components.port());
        }
        sb.append(components.path());
        String query = buildQueryString(params);
        if (!query.isEmpty()) {
            sb.append("?").append(query);
        }
        if (components.fragment() != null) {
            sb.append("#").append(components.fragment());
        }
        return sb.toString();
    }

    /**
     * Remove query parameter from URL.
     */
    public static String removeQueryParam(String url, String key) {
        UrlComponents components = parseUrl(url);
        if (components == null || components.queryParams() == null) {
            return url;
        }

        Map<String, String> params = new LinkedHashMap<>(components.queryParams());
        params.remove(key);

        StringBuilder sb = new StringBuilder();
        sb.append(components.protocol()).append("://").append(components.host());
        if (components.port() != -1 && components.port() != 80 && components.port() != 443) {
            sb.append(":").append(components.port());
        }
        sb.append(components.path());
        String query = buildQueryString(params);
        if (!query.isEmpty()) {
            sb.append("?").append(query);
        }
        if (components.fragment() != null) {
            sb.append("#").append(components.fragment());
        }
        return sb.toString();
    }

    /**
     * Join URL path segments.
     */
    public static String joinPath(String... segments) {
        if (segments == null || segments.length == 0) return "";

        StringBuilder sb = new StringBuilder();
        for (String segment : segments) {
            if (segment == null || segment.isEmpty()) continue;

            if (segment.startsWith("/")) {
                segment = segment.substring(1);
            }

            if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '/') {
                sb.append("/");
            }
            sb.append(segment);
        }
        return sb.toString();
    }

    /**
     * Normalize URL path.
     */
    public static String normalizePath(String path) {
        if (path == null || path.isEmpty()) return "/";

        // Remove duplicate slashes
        String normalized = path.replaceAll("/+", "/");

        // Ensure starts with slash
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        return normalized;
    }

    /**
     * Resolve relative URL against base.
     */
    public static String resolveUrl(String baseUrl, String relativeUrl) {
        try {
            URI base = new URI(baseUrl);
            URI resolved = base.resolve(relativeUrl);
            return resolved.toString();
        } catch (URISyntaxException e) {
            return relativeUrl;
        }
    }

    /**
     * Check if URL is absolute.
     */
    public static boolean isAbsoluteUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        return url.contains(":/") || url.startsWith("/");
    }

    /**
     * Check if URL is relative.
     */
    public static boolean isRelativeUrl(String url) {
        return !isAbsoluteUrl(url);
    }

    /**
     * Get URL extension from path.
     */
    public static String getExtension(String url) {
        String path = getPath(url);
        if (path == null || path.isEmpty()) return null;

        int lastDot = path.lastIndexOf('.');
        int lastSlash = path.lastIndexOf('/');

        if (lastDot > lastSlash && lastDot < path.length() - 1) {
            return path.substring(lastDot + 1);
        }
        return null;
    }

    /**
     * Check if URL is a GitHub URL.
     */
    public static boolean isGitHubUrl(String url) {
        String domain = getDomain(url);
        return domain != null && (domain.equals("github.com") || domain.endsWith(".github.com"));
    }

    /**
     * Parse GitHub URL into components.
     */
    public static Optional<GitHubUrlComponents> parseGitHubUrl(String url) {
        if (!isGitHubUrl(url)) return Optional.empty();

        String path = getPath(url);
        if (path == null) return Optional.empty();

        String[] parts = path.split("/");
        if (parts.length < 3) return Optional.empty();

        String owner = parts[1];
        String repo = parts[2];

        String type = null;
        String number = null;

        if (parts.length >= 4) {
            type = parts[3];
            if (parts.length >= 5) {
                number = parts[4];
            }
        }

        return Optional.of(new GitHubUrlComponents(owner, repo, type, number));
    }

    /**
     * GitHub URL components.
     */
    public record GitHubUrlComponents(String owner, String repo, String type, String number) {
        public boolean isIssue() {
            return "issues".equals(type);
        }

        public boolean isPullRequest() {
            return "pull".equals(type);
        }

        public String toShortFormat() {
            return owner + "/" + repo + "#" + number;
        }
    }

    /**
     * Check if URL matches a pattern.
     */
    public static boolean matchesPattern(String url, String pattern) {
        if (url == null || pattern == null) return false;
        String regex = pattern.replace(".", "\\.").replace("*", ".*");
        return Pattern.matches(regex, url);
    }

    /**
     * Sanitize URL for display (remove sensitive query params).
     */
    public static String sanitizeUrl(String url) {
        UrlComponents components = parseUrl(url);
        if (components == null) return url;

        Set<String> sensitiveParams = Set.of("token", "key", "secret", "password", "api_key", "apikey", "auth");

        Map<String, String> sanitizedParams = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : components.queryParams().entrySet()) {
            String key = entry.getKey().toLowerCase();
            if (!sensitiveParams.contains(key) && !key.contains("token") && !key.contains("key")) {
                sanitizedParams.put(entry.getKey(), entry.getValue());
            } else {
                sanitizedParams.put(entry.getKey(), "[REDACTED]");
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(components.protocol()).append("://").append(components.host());
        if (components.port() != -1 && components.port() != 80 && components.port() != 443) {
            sb.append(":").append(components.port());
        }
        sb.append(components.path());
        String query = buildQueryString(sanitizedParams);
        if (!query.isEmpty()) {
            sb.append("?").append(query);
        }
        return sb.toString();
    }
}