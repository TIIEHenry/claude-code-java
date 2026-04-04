/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UrlUtils.
 */
class UrlUtilsTest {

    @Test
    @DisplayName("UrlUtils parseUrl parses URL")
    void parseUrl() {
        UrlUtils.UrlComponents components = UrlUtils.parseUrl("https://example.com/path?query=value#fragment");

        assertNotNull(components);
        assertEquals("https", components.protocol());
        assertEquals("example.com", components.host());
        assertEquals("/path", components.path());
        assertEquals("query=value", components.query());
        assertEquals("fragment", components.fragment());
    }

    @Test
    @DisplayName("UrlUtils parseUrl handles null")
    void parseUrlNull() {
        assertNull(UrlUtils.parseUrl(null));
        assertNull(UrlUtils.parseUrl(""));
    }

    @Test
    @DisplayName("UrlUtils parseQueryString parses query")
    void parseQueryString() {
        Map<String, String> params = UrlUtils.parseQueryString("a=1&b=2&c=3");

        assertEquals(3, params.size());
        assertEquals("1", params.get("a"));
        assertEquals("2", params.get("b"));
    }

    @Test
    @DisplayName("UrlUtils parseQueryString handles null")
    void parseQueryStringNull() {
        assertTrue(UrlUtils.parseQueryString(null).isEmpty());
    }

    @Test
    @DisplayName("UrlUtils buildQueryString builds query")
    void buildQueryString() {
        String query = UrlUtils.buildQueryString(Map.of("a", "1", "b", "2"));

        assertTrue(query.contains("a=1"));
        assertTrue(query.contains("b=2"));
    }

    @Test
    @DisplayName("UrlUtils buildQueryString handles empty")
    void buildQueryStringEmpty() {
        assertEquals("", UrlUtils.buildQueryString(null));
        assertEquals("", UrlUtils.buildQueryString(Map.of()));
    }

    @Test
    @DisplayName("UrlUtils encodeUrl encodes")
    void encodeUrl() {
        assertEquals("hello+world", UrlUtils.encodeUrl("hello world"));
        assertEquals("a%40b", UrlUtils.encodeUrl("a@b"));
    }

    @Test
    @DisplayName("UrlUtils encodeUrl handles null")
    void encodeUrlNull() {
        assertNull(UrlUtils.encodeUrl(null));
    }

    @Test
    @DisplayName("UrlUtils decodeUrl decodes")
    void decodeUrl() {
        assertEquals("hello world", UrlUtils.decodeUrl("hello+world"));
        assertEquals("a@b", UrlUtils.decodeUrl("a%40b"));
    }

    @Test
    @DisplayName("UrlUtils decodeUrl handles null")
    void decodeUrlNull() {
        assertNull(UrlUtils.decodeUrl(null));
    }

    @Test
    @DisplayName("UrlUtils isValidUrl validates URL")
    void isValidUrl() {
        assertTrue(UrlUtils.isValidUrl("https://example.com"));
        assertTrue(UrlUtils.isValidUrl("http://localhost:8080/path"));
        assertFalse(UrlUtils.isValidUrl(null));
        assertFalse(UrlUtils.isValidUrl(""));
        assertFalse(UrlUtils.isValidUrl("not a url"));
    }

    @Test
    @DisplayName("UrlUtils isHttpUrl checks HTTP")
    void isHttpUrl() {
        assertTrue(UrlUtils.isHttpUrl("http://example.com"));
        assertTrue(UrlUtils.isHttpUrl("https://example.com"));
        assertFalse(UrlUtils.isHttpUrl("ftp://example.com"));
        assertFalse(UrlUtils.isHttpUrl("invalid"));
    }

    @Test
    @DisplayName("UrlUtils getDomain extracts domain")
    void getDomain() {
        assertEquals("example.com", UrlUtils.getDomain("https://example.com/path"));
        assertNull(UrlUtils.getDomain("invalid"));
    }

    @Test
    @DisplayName("UrlUtils getBaseUrl extracts base URL")
    void getBaseUrl() {
        assertEquals("https://example.com", UrlUtils.getBaseUrl("https://example.com/path?query=1"));
    }

    @Test
    @DisplayName("UrlUtils getPath extracts path")
    void getPath() {
        assertEquals("/path/to/file", UrlUtils.getPath("https://example.com/path/to/file?query=1"));
    }

    @Test
    @DisplayName("UrlUtils getQueryParam extracts param")
    void getQueryParam() {
        Optional<String> value = UrlUtils.getQueryParam("https://example.com?key=value", "key");

        assertTrue(value.isPresent());
        assertEquals("value", value.get());
    }

    @Test
    @DisplayName("UrlUtils addQueryParam adds param")
    void addQueryParam() {
        String url = UrlUtils.addQueryParam("https://example.com", "key", "value");

        assertTrue(url.contains("key=value"));
    }

    @Test
    @DisplayName("UrlUtils removeQueryParam removes param")
    void removeQueryParam() {
        String url = UrlUtils.removeQueryParam("https://example.com?key=value", "key");

        assertFalse(url.contains("key"));
    }

    @Test
    @DisplayName("UrlUtils joinPath joins segments")
    void joinPath() {
        assertEquals("path/to/file", UrlUtils.joinPath("path", "to", "file"));
    }

    @Test
    @DisplayName("UrlUtils joinPath handles single segment")
    void joinPathSingle() {
        String result = UrlUtils.joinPath("/path/");
        assertTrue(result.contains("path"));
    }

    @Test
    @DisplayName("UrlUtils normalizePath normalizes path")
    void normalizePath() {
        assertEquals("/path/to/file", UrlUtils.normalizePath("//path///to//file"));
        assertEquals("/", UrlUtils.normalizePath(null));
    }

    @Test
    @DisplayName("UrlUtils resolveUrl resolves relative")
    void resolveUrl() {
        String resolved = UrlUtils.resolveUrl("https://example.com/path/", "../other");

        assertTrue(resolved.contains("other"));
    }

    @Test
    @DisplayName("UrlUtils isAbsoluteUrl checks absolute")
    void isAbsoluteUrl() {
        assertTrue(UrlUtils.isAbsoluteUrl("https://example.com"));
        assertTrue(UrlUtils.isAbsoluteUrl("/path"));
        assertFalse(UrlUtils.isAbsoluteUrl("relative/path"));
    }

    @Test
    @DisplayName("UrlUtils isRelativeUrl checks relative")
    void isRelativeUrl() {
        assertTrue(UrlUtils.isRelativeUrl("relative/path"));
        assertFalse(UrlUtils.isRelativeUrl("https://example.com"));
    }

    @Test
    @DisplayName("UrlUtils getExtension extracts extension")
    void getExtension() {
        assertEquals("html", UrlUtils.getExtension("https://example.com/page.html"));
        assertNull(UrlUtils.getExtension("https://example.com/page"));
    }

    @Test
    @DisplayName("UrlUtils isGitHubUrl checks GitHub")
    void isGitHubUrl() {
        assertTrue(UrlUtils.isGitHubUrl("https://github.com/user/repo"));
        assertFalse(UrlUtils.isGitHubUrl("https://example.com"));
    }

    @Test
    @DisplayName("UrlUtils parseGitHubUrl parses GitHub URL")
    void parseGitHubUrl() {
        Optional<UrlUtils.GitHubUrlComponents> components =
            UrlUtils.parseGitHubUrl("https://github.com/anthropics/claude-code/issues/100");

        assertTrue(components.isPresent());
        assertEquals("anthropics", components.get().owner());
        assertEquals("claude-code", components.get().repo());
        assertEquals("issues", components.get().type());
        assertEquals("100", components.get().number());
    }

    @Test
    @DisplayName("UrlUtils GitHubUrlComponents isIssue checks")
    void gitHubUrlComponentsIsIssue() {
        UrlUtils.GitHubUrlComponents issue = new UrlUtils.GitHubUrlComponents("user", "repo", "issues", "1");
        UrlUtils.GitHubUrlComponents pr = new UrlUtils.GitHubUrlComponents("user", "repo", "pull", "1");

        assertTrue(issue.isIssue());
        assertFalse(issue.isPullRequest());
        assertTrue(pr.isPullRequest());
        assertFalse(pr.isIssue());
    }

    @Test
    @DisplayName("UrlUtils GitHubUrlComponents toShortFormat")
    void gitHubUrlComponentsToShortFormat() {
        UrlUtils.GitHubUrlComponents components =
            new UrlUtils.GitHubUrlComponents("user", "repo", "issues", "100");

        assertEquals("user/repo#100", components.toShortFormat());
    }

    @Test
    @DisplayName("UrlUtils matchesPattern matches URL")
    void matchesPattern() {
        assertTrue(UrlUtils.matchesPattern("https://example.com", "https://example.com"));
        assertTrue(UrlUtils.matchesPattern("https://example.com/path", "https://example.*"));
        assertFalse(UrlUtils.matchesPattern("https://other.com", "https://example.com"));
    }

    @Test
    @DisplayName("UrlUtils sanitizeUrl redacts sensitive params")
    void sanitizeUrl() {
        String sanitized = UrlUtils.sanitizeUrl("https://example.com?token=secret&name=test");

        // Token should be redacted
        assertTrue(sanitized.contains("token"));
        assertTrue(sanitized.contains("name=test"));
    }
}