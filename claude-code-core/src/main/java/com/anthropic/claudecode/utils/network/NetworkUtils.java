/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/network
 */
package com.anthropic.claudecode.utils.network;

import java.util.*;
import java.net.*;
import java.io.*;

/**
 * Network utils - Network utilities.
 */
public final class NetworkUtils {

    /**
     * Check if host is reachable.
     */
    public static boolean isReachable(String host, int timeout) {
        try {
            InetAddress address = InetAddress.getByName(host);
            return address.isReachable(timeout);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if port is open.
     */
    public static boolean isPortOpen(String host, int port, int timeout) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeout);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get local IP address.
     */
    public static String getLocalIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    /**
     * Get available port.
     */
    public static int getAvailablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Parse URL.
     */
    public static UrlInfo parseUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            return new UrlInfo(
                url.getProtocol(),
                url.getHost(),
                url.getPort() > 0 ? url.getPort() : url.getDefaultPort(),
                url.getPath(),
                url.getQuery(),
                url.getRef()
            );
        } catch (Exception e) {
            return new UrlInfo("", "", 0, "", "", "");
        }
    }

    /**
     * URL info record.
     */
    public record UrlInfo(
        String protocol,
        String host,
        int port,
        String path,
        String query,
        String fragment
    ) {
        public String getBaseUrl() {
            return protocol + "://" + host + (port != 80 && port != 443 ? ":" + port : "");
        }

        public String getFullPath() {
            StringBuilder sb = new StringBuilder(path);
            if (query != null && !query.isEmpty()) {
                sb.append("?").append(query);
            }
            if (fragment != null && !fragment.isEmpty()) {
                sb.append("#").append(fragment);
            }
            return sb.toString();
        }
    }

    /**
     * Build URL with params.
     */
    public static String buildUrl(String baseUrl, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return baseUrl;
        }

        StringBuilder sb = new StringBuilder(baseUrl);
        if (!baseUrl.contains("?")) {
            sb.append("?");
        } else if (!baseUrl.endsWith("&") && !baseUrl.endsWith("?")) {
            sb.append("&");
        }

        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) sb.append("&");
            try {
                sb.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
                sb.append("=");
                sb.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                // Ignore
            }
            first = false;
        }

        return sb.toString();
    }

    /**
     * Parse query string.
     */
    public static Map<String, String> parseQueryString(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isEmpty()) {
            return params;
        }

        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int eq = pair.indexOf('=');
            if (eq > 0) {
                try {
                    String key = URLDecoder.decode(pair.substring(0, eq), "UTF-8");
                    String value = URLDecoder.decode(pair.substring(eq + 1), "UTF-8");
                    params.put(key, value);
                } catch (Exception e) {
                    // Ignore
                }
            }
        }

        return params;
    }

    /**
     * Get MIME type for file.
     */
    public static String getMimeType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html";
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".js")) return "application/javascript";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".xml")) return "application/xml";
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        return "application/octet-stream";
    }

    /**
     * HTTP method enum.
     */
    public enum HttpMethod {
        GET,
        POST,
        PUT,
        DELETE,
        PATCH,
        HEAD,
        OPTIONS
    }

    /**
     * HTTP header record.
     */
    public record HttpHeader(String name, String value) {
        public static HttpHeader of(String name, String value) {
            return new HttpHeader(name, value);
        }
    }

    /**
     * Network connection info record.
     */
    public record ConnectionInfo(
        String localAddress,
        String remoteAddress,
        boolean isConnected,
        long bytesSent,
        long bytesReceived
    ) {}
}