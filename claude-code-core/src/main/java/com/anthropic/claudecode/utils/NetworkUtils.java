/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code network utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;

/**
 * Network-related utilities.
 */
public final class NetworkUtils {
    private NetworkUtils() {}

    /**
     * Check if a host is reachable.
     */
    public static boolean isHostReachable(String host, int timeoutMs) {
        try {
            Process process = Runtime.getRuntime().exec(
                System.getProperty("os.name").toLowerCase().contains("win")
                    ? new String[]{"ping", "-n", "1", "-w", String.valueOf(timeoutMs), host}
                    : new String[]{"ping", "-c", "1", "-W", String.valueOf(timeoutMs / 1000), host}
            );
            return process.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if a port is available on localhost.
     */
    public static boolean isPortAvailable(int port) {
        try (java.net.ServerSocket socket = new java.net.ServerSocket(port)) {
            socket.setReuseAddress(true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Find an available port starting from the given port.
     */
    public static int findAvailablePort(int startPort) {
        for (int port = startPort; port < 65536; port++) {
            if (isPortAvailable(port)) {
                return port;
            }
        }
        return -1;
    }

    /**
     * Find multiple available ports.
     */
    public static List<Integer> findAvailablePorts(int count, int startPort) {
        List<Integer> ports = new ArrayList<>();
        int port = startPort;

        while (ports.size() < count && port < 65536) {
            if (isPortAvailable(port)) {
                ports.add(port);
            }
            port++;
        }

        return ports;
    }

    /**
     * Get localhost address.
     */
    public static String getLocalhostAddress() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    /**
     * Get localhost name.
     */
    public static String getLocalhostName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "localhost";
        }
    }

    /**
     * Get all local IP addresses.
     */
    public static List<String> getLocalIpAddresses() {
        List<String> addresses = new ArrayList<>();
        try {
            Enumeration<java.net.NetworkInterface> interfaces = java.net.NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                java.net.NetworkInterface networkInterface = interfaces.nextElement();
                Enumeration<java.net.InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    java.net.InetAddress address = inetAddresses.nextElement();
                    if (address instanceof java.net.Inet4Address && !address.isLoopbackAddress()) {
                        addresses.add(address.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return addresses;
    }

    /**
     * Check if the current machine is online.
     */
    public static boolean isOnline() {
        return isHostReachable("8.8.8.8", 3000) || isHostReachable("1.1.1.1", 3000);
    }

    /**
     * Resolve hostname to IP address.
     */
    public static Optional<String> resolveHostname(String hostname) {
        try {
            java.net.InetAddress address = java.net.InetAddress.getByName(hostname);
            return Optional.of(address.getHostAddress());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Reverse DNS lookup.
     */
    public static Optional<String> reverseDns(String ipAddress) {
        try {
            java.net.InetAddress address = java.net.InetAddress.getByName(ipAddress);
            return Optional.of(address.getHostName());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Check if an IP address is private/local.
     */
    public static boolean isPrivateIp(String ipAddress) {
        try {
            java.net.InetAddress address = java.net.InetAddress.getByName(ipAddress);
            return address.isSiteLocalAddress() || address.isLoopbackAddress();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if an IP address is localhost.
     */
    public static boolean isLocalhost(String ipAddress) {
        try {
            java.net.InetAddress address = java.net.InetAddress.getByName(ipAddress);
            return address.isLoopbackAddress();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Parse URL to get port number.
     */
    public static int getUrlPort(String url) {
        try {
            java.net.URL parsed = new java.net.URL(url);
            int port = parsed.getPort();
            if (port == -1) {
                String protocol = parsed.getProtocol();
                if ("https".equalsIgnoreCase(protocol)) return 443;
                if ("http".equalsIgnoreCase(protocol)) return 80;
                if ("ftp".equalsIgnoreCase(protocol)) return 21;
            }
            return port;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Build URL from components.
     */
    public static String buildUrl(String protocol, String host, int port, String path) {
        StringBuilder sb = new StringBuilder();
        sb.append(protocol).append("://").append(host);

        if (port != -1 && port != 80 && port != 443) {
            sb.append(":").append(port);
        }

        if (path != null && !path.isEmpty()) {
            if (!path.startsWith("/")) {
                sb.append("/");
            }
            sb.append(path);
        }

        return sb.toString();
    }

    /**
     * Wait for a host:port to become available.
     */
    public static boolean waitForHost(String host, int port, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            try (java.net.Socket socket = new java.net.Socket()) {
                socket.connect(new java.net.InetSocketAddress(host, port), 1000);
                return true;
            } catch (Exception e) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        return false;
    }

    /**
     * Wait for a URL to become available.
     */
    public static boolean waitForUrl(String url, long timeoutMs) {
        try {
            java.net.URL parsed = new java.net.URL(url);
            return waitForHost(parsed.getHost(), getUrlPort(url), timeoutMs);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get HTTP response code for a URL.
     */
    public static int getHttpResponseCode(String url, int timeoutMs) {
        try {
            java.net.URLConnection connection = new java.net.URL(url).openConnection();
            connection.setConnectTimeout(timeoutMs);
            connection.setReadTimeout(timeoutMs);

            if (connection instanceof java.net.HttpURLConnection) {
                return ((java.net.HttpURLConnection) connection).getResponseCode();
            }
            return -1;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Check if URL returns a successful response.
     */
    public static boolean isUrlAccessible(String url, int timeoutMs) {
        int code = getHttpResponseCode(url, timeoutMs);
        return code >= 200 && code < 400;
    }

    /**
     * Download content from URL as string.
     */
    public static String downloadString(String url, int timeoutMs) throws Exception {
        java.net.URLConnection connection = new java.net.URL(url).openConnection();
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs);

        try (java.io.InputStream is = connection.getInputStream();
             java.io.BufferedReader reader = new java.io.BufferedReader(
                 new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * Download content from URL as bytes.
     */
    public static byte[] downloadBytes(String url, int timeoutMs) throws Exception {
        java.net.URLConnection connection = new java.net.URL(url).openConnection();
        connection.setConnectTimeout(timeoutMs);
        connection.setReadTimeout(timeoutMs);

        try (java.io.InputStream is = connection.getInputStream();
             java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        }
    }

    /**
     * Download content asynchronously.
     */
    public static CompletableFuture<String> downloadStringAsync(String url, int timeoutMs) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return downloadString(url, timeoutMs);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    /**
     * Get content type for a URL.
     */
    public static String getContentType(String url) {
        try {
            java.net.URLConnection connection = new java.net.URL(url).openConnection();
            connection.connect();
            return connection.getContentType();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get content length for a URL.
     */
    public static long getContentLength(String url) {
        try {
            java.net.URLConnection connection = new java.net.URL(url).openConnection();
            connection.connect();
            return connection.getContentLengthLong();
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Parse query parameters from URL.
     */
    public static Map<String, String> parseQueryParams(String url) {
        try {
            java.net.URL parsed = new java.net.URL(url);
            String query = parsed.getQuery();
            if (query == null || query.isEmpty()) {
                return Map.of();
            }

            Map<String, String> params = new LinkedHashMap<>();
            for (String pair : query.split("&")) {
                String[] keyValue = pair.split("=", 2);
                String key = java.net.URLDecoder.decode(keyValue[0], "UTF-8");
                String value = keyValue.length > 1 ? java.net.URLDecoder.decode(keyValue[1], "UTF-8") : "";
                params.put(key, value);
            }
            return params;
        } catch (Exception e) {
            return Map.of();
        }
    }
}