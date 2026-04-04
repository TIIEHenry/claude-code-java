/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/detectRepository.ts
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 * Repository detection utilities.
 */
public final class DetectRepository {
    private DetectRepository() {}

    /**
     * Parsed repository record.
     */
    public record ParsedRepository(String host, String owner, String name) {}

    private static final ConcurrentHashMap<String, ParsedRepository> repositoryCache = new ConcurrentHashMap<>();

    /**
     * Clear repository caches.
     */
    public static void clearRepositoryCaches() {
        repositoryCache.clear();
    }

    /**
     * Detect the current repository (GitHub only).
     */
    public static String detectCurrentRepository() {
        ParsedRepository result = detectCurrentRepositoryWithHost();
        if (result == null) return null;
        // Only return results for github.com
        if (!"github.com".equals(result.host())) return null;
        return result.owner() + "/" + result.name();
    }

    /**
     * Detect the current repository with host info.
     */
    public static ParsedRepository detectCurrentRepositoryWithHost() {
        String cwd = Cwd.getCwd().toString();

        if (repositoryCache.containsKey(cwd)) {
            return repositoryCache.get(cwd);
        }

        try {
            String remoteUrl = getRemoteUrl();
            if (remoteUrl == null || remoteUrl.isEmpty()) {
                repositoryCache.put(cwd, null);
                return null;
            }

            ParsedRepository parsed = parseGitRemote(remoteUrl);
            if (parsed != null) {
                repositoryCache.put(cwd, parsed);
            }
            return parsed;
        } catch (Exception e) {
            repositoryCache.put(cwd, null);
            return null;
        }
    }

    /**
     * Get cached repository (GitHub only).
     */
    public static String getCachedRepository() {
        ParsedRepository parsed = repositoryCache.get(Cwd.getCwd());
        if (parsed == null || !"github.com".equals(parsed.host())) return null;
        return parsed.owner() + "/" + parsed.name();
    }

    /**
     * Parse a git remote URL into host, owner, and name components.
     * Supports:
     *   https://host/owner/repo.git
     *   git@host:owner/repo.git
     *   ssh://git@host/owner/repo.git
     *   git://host/owner/repo.git
     */
    public static ParsedRepository parseGitRemote(String input) {
        if (input == null || input.isEmpty()) return null;

        String trimmed = input.trim();

        // SSH format: git@host:owner/repo.git
        Pattern sshPattern = Pattern.compile("^git@([^:]+):([^/]+)/([^/]+?)(?:\\.git)?$");
        Matcher sshMatch = sshPattern.matcher(trimmed);
        if (sshMatch.matches()) {
            String host = sshMatch.group(1);
            if (!looksLikeRealHostname(host)) return null;
            return new ParsedRepository(host, sshMatch.group(2), sshMatch.group(3));
        }

        // URL format: https://host/owner/repo.git, ssh://git@host/owner/repo, etc.
        Pattern urlPattern = Pattern.compile(
            "^(https?|ssh|git)://(?:[^@]+@)?([^/:]+(?::\\d+)?)/([^/]+)/([^/]+?)(?:\\.git)?$"
        );
        Matcher urlMatch = urlPattern.matcher(trimmed);
        if (urlMatch.matches()) {
            String protocol = urlMatch.group(1);
            String hostWithPort = urlMatch.group(2);
            String hostWithoutPort = hostWithPort.split(":")[0];

            if (!looksLikeRealHostname(hostWithoutPort)) return null;

            // Only preserve port for HTTPS
            String host = "https".equals(protocol) || "http".equals(protocol)
                ? hostWithPort
                : hostWithoutPort;

            return new ParsedRepository(host, urlMatch.group(3), urlMatch.group(4));
        }

        return null;
    }

    /**
     * Parse a git remote URL or "owner/repo" string and returns "owner/repo".
     * Only returns results for github.com hosts.
     */
    public static String parseGitHubRepository(String input) {
        if (input == null || input.isEmpty()) return null;

        String trimmed = input.trim();

        // Try parsing as a full remote URL first
        ParsedRepository parsed = parseGitRemote(trimmed);
        if (parsed != null) {
            if (!"github.com".equals(parsed.host())) return null;
            return parsed.owner() + "/" + parsed.name();
        }

        // Check if it's already in owner/repo format
        if (!trimmed.contains("://") && !trimmed.contains("@") && trimmed.contains("/")) {
            String[] parts = trimmed.split("/");
            if (parts.length == 2 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
                String repo = parts[1].replaceFirst("\\.git$", "");
                return parts[0] + "/" + repo;
            }
        }

        return null;
    }

    /**
     * Check whether a hostname looks like a real domain name.
     */
    private static boolean looksLikeRealHostname(String host) {
        if (host == null || !host.contains(".")) return false;

        int lastDot = host.lastIndexOf('.');
        if (lastDot < 0 || lastDot >= host.length() - 1) return false;

        String lastSegment = host.substring(lastDot + 1);
        // Real TLDs are purely alphabetic
        return lastSegment.matches("^[a-zA-Z]+$");
    }

    /**
     * Get git remote URL by executing git command.
     */
    private static String getRemoteUrl() {
        try {
            ProcessBuilder pb = new ProcessBuilder("git", "remote", "get-url", "origin");
            pb.directory(new java.io.File(Cwd.getCwd().toString()));
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            process.waitFor(5, TimeUnit.SECONDS);

            if (process.exitValue() == 0 && !output.isEmpty()) {
                return output;
            }
        } catch (Exception e) {
            // Git not available or not a git repository
        }
        return null;
    }
}