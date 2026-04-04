/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/hooks/execHttpHook.ts
 */
package com.anthropic.claudecode.utils.hooks;

import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 * Execute an HTTP hook by POSTing the hook input JSON to the configured URL.
 */
public final class ExecHttpHook {
    private ExecHttpHook() {}

    private static final int DEFAULT_HTTP_HOOK_TIMEOUT_MS = 10 * 60 * 1000; // 10 minutes

    /**
     * HTTP hook configuration.
     */
    public record HttpHook(
        String type,
        String url,
        Integer timeout,
        Map<String, String> headers,
        List<String> allowedEnvVars
    ) {}

    /**
     * HTTP hook result.
     */
    public record HttpHookResult(
        boolean ok,
        Integer statusCode,
        String body,
        String error,
        boolean aborted
    ) {}

    // Policy settings (can be set externally)
    private static volatile List<String> allowedUrls = null;
    private static volatile List<String> allowedEnvVars = null;
    private static volatile String proxyUrl = null;

    /**
     * Set HTTP hook policy settings.
     */
    public static void setPolicy(List<String> urls, List<String> envVars) {
        allowedUrls = urls;
        allowedEnvVars = envVars;
    }

    /**
     * Set proxy URL.
     */
    public static void setProxyUrl(String url) {
        proxyUrl = url;
    }

    /**
     * Execute an HTTP hook.
     */
    public static CompletableFuture<HttpHookResult> execHttpHook(
            HttpHook hook,
            String hookEvent,
            String jsonInput,
            CompletableFuture<Void> signal) {

        return CompletableFuture.supplyAsync(() -> {
            // Enforce URL allowlist
            if (allowedUrls != null) {
                boolean matched = allowedUrls.stream()
                    .anyMatch(pattern -> urlMatchesPattern(hook.url(), pattern));
                if (!matched) {
                    String msg = "HTTP hook blocked: " + hook.url() +
                        " does not match any pattern in allowedHttpHookUrls";
                    logForDebugging(msg);
                    return new HttpHookResult(false, null, "", msg, false);
                }
            }

            int timeoutMs = hook.timeout() != null
                ? hook.timeout() * 1000
                : DEFAULT_HTTP_HOOK_TIMEOUT_MS;

            try {
                // Build headers with env var interpolation
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");

                if (hook.headers() != null) {
                    List<String> hookVars = hook.allowedEnvVars() != null
                        ? hook.allowedEnvVars()
                        : new ArrayList<>();

                    List<String> effectiveVars = allowedEnvVars != null
                        ? hookVars.stream().filter(v -> allowedEnvVars.contains(v)).toList()
                        : hookVars;

                    Set<String> allowedVarSet = new HashSet<>(effectiveVars);

                    for (Map.Entry<String, String> entry : hook.headers().entrySet()) {
                        String interpolated = interpolateEnvVars(entry.getValue(), allowedVarSet);
                        headers.put(entry.getKey(), interpolated);
                    }
                }

                // Check proxy configuration
                boolean useProxy = proxyUrl != null;

                if (useProxy) {
                    logForDebugging("Hooks: HTTP hook POST to " + hook.url() +
                        " (via env-var proxy)");
                } else {
                    logForDebugging("Hooks: HTTP hook POST to " + hook.url());
                }

                // Build HTTP client
                HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(timeoutMs));

                if (useProxy) {
                    // Parse proxy URL
                    try {
                        URI proxyUri = URI.create(proxyUrl);
                        clientBuilder.proxy(ProxySelector.of(
                            new InetSocketAddress(proxyUri.getHost(), proxyUri.getPort())));
                    } catch (Exception e) {
                        // Ignore invalid proxy
                    }
                }

                HttpClient client = clientBuilder.build();

                // Build request
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(hook.url()))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonInput));

                for (Map.Entry<String, String> header : headers.entrySet()) {
                    requestBuilder.header(header.getKey(), header.getValue());
                }

                HttpRequest request = requestBuilder.build();

                // Execute request (with SSRF guard if no proxy)
                if (!useProxy) {
                    try {
                        SsrfGuard.guardedLookup(hook.url()).get();
                    } catch (SsrfGuard.SsrfException e) {
                        return new HttpHookResult(false, null, "", e.getMessage(), false);
                    }
                }

                HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

                String body = response.body() != null ? response.body() : "";
                int statusCode = response.statusCode();

                logForDebugging("Hooks: HTTP hook response status " + statusCode +
                    ", body length " + body.length());

                return new HttpHookResult(
                    statusCode >= 200 && statusCode < 300,
                    statusCode,
                    body,
                    null,
                    false);

            } catch (Exception e) {
                if (signal != null && signal.isDone()) {
                    return new HttpHookResult(false, null, "", null, true);
                }

                String errorMsg = e.getMessage();
                logForDebugging("Hooks: HTTP hook error: " + errorMsg);
                return new HttpHookResult(false, null, "", errorMsg, false);
            }
        });
    }

    /**
     * Match a URL against a pattern with * as wildcard.
     */
    private static boolean urlMatchesPattern(String url, String pattern) {
        String escaped = pattern.replaceAll("[.+?^${}()|\\[\\]\\\\]", "\\\\$0");
        String regexStr = escaped.replace("*", ".*");
        return Pattern.compile("^" + regexStr + "$").matcher(url).matches();
    }

    /**
     * Strip CR, LF, NUL bytes to prevent header injection.
     */
    private static String sanitizeHeaderValue(String value) {
        return value.replaceAll("[\\r\\n\\x00]", "");
    }

    /**
     * Interpolate environment variables in a string.
     */
    private static String interpolateEnvVars(String value, Set<String> allowedEnvVars) {
        Pattern pattern = Pattern.compile("\\$\\{([A-Z_][A-Z0-9_]*)\\}|\\$([A-Z_][A-Z0-9_]*)");

        String interpolated = pattern.matcher(value).replaceAll(result -> {
            String varName = result.group(1) != null ? result.group(1) : result.group(2);
            if (!allowedEnvVars.contains(varName)) {
                logForDebugging("Hooks: env var $" + varName +
                    " not in allowedEnvVars, skipping interpolation");
                return "";
            }
            String envValue = System.getenv(varName);
            return envValue != null ? envValue : "";
        });

        return sanitizeHeaderValue(interpolated);
    }

    private static void logForDebugging(String message) {
        if (System.getenv("CLAUDE_CODE_DEBUG") != null) {
            System.err.println("[http-hook] " + message);
        }
    }
}