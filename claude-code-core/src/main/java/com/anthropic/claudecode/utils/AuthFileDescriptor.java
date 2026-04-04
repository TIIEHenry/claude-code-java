/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code auth file descriptor utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Auth file descriptor utilities for CCR (Claude Code Remote).
 */
public final class AuthFileDescriptor {
    private AuthFileDescriptor() {}

    // Well-known token file locations in CCR
    private static final String CCR_TOKEN_DIR = "/home/claude/.claude/remote";
    public static final String CCR_OAUTH_TOKEN_PATH = CCR_TOKEN_DIR + "/.oauth_token";
    public static final String CCR_API_KEY_PATH = CCR_TOKEN_DIR + "/.api_key";
    public static final String CCR_SESSION_INGRESS_TOKEN_PATH = CCR_TOKEN_DIR + "/.session_ingress_token";

    // Cached credentials
    private static String cachedOAuthToken = null;
    private static String cachedApiKey = null;
    private static boolean oauthTokenCached = false;
    private static boolean apiKeyCached = false;

    /**
     * Best-effort write of the token to a well-known location for subprocess access.
     */
    public static void maybePersistTokenForSubprocesses(String path, String token, String tokenName) {
        String remoteEnv = System.getenv("CLAUDE_CODE_REMOTE");
        if (!EnvUtils.isEnvTruthy(remoteEnv)) {
            return;
        }

        try {
            Files.createDirectories(Paths.get(CCR_TOKEN_DIR));
            Files.writeString(Paths.get(path), token);
            Debug.log("Persisted " + tokenName + " to " + path + " for subprocess access");
        } catch (Exception e) {
            Debug.log("Failed to persist " + tokenName + " to disk (non-fatal): " + e.getMessage());
        }
    }

    /**
     * Fallback read from a well-known file.
     */
    public static String readTokenFromWellKnownFile(String path, String tokenName) {
        try {
            Path filePath = Paths.get(path);
            if (!Files.exists(filePath)) {
                return null;
            }

            String token = Files.readString(filePath).trim();
            if (token.isEmpty()) {
                return null;
            }

            Debug.log("Read " + tokenName + " from well-known file " + path);
            return token;
        } catch (FileNotFoundException | NoSuchFileException e) {
            // Expected outside CCR
            return null;
        } catch (Exception e) {
            Debug.log("Failed to read " + tokenName + " from " + path + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Get the CCR-injected OAuth token.
     */
    public static String getOAuthTokenFromFileDescriptor() {
        if (oauthTokenCached) {
            return cachedOAuthToken;
        }

        String result = getCredentialFromFd(
                "CLAUDE_CODE_OAUTH_TOKEN_FILE_DESCRIPTOR",
                CCR_OAUTH_TOKEN_PATH,
                "OAuth token"
        );

        cachedOAuthToken = result;
        oauthTokenCached = true;
        return result;
    }

    /**
     * Get the CCR-injected API key.
     */
    public static String getApiKeyFromFileDescriptor() {
        if (apiKeyCached) {
            return cachedApiKey;
        }

        String result = getCredentialFromFd(
                "CLAUDE_CODE_API_KEY_FILE_DESCRIPTOR",
                CCR_API_KEY_PATH,
                "API key"
        );

        cachedApiKey = result;
        apiKeyCached = true;
        return result;
    }

    /**
     * Shared FD-or-well-known-file credential reader.
     */
    private static String getCredentialFromFd(String envVar, String wellKnownPath, String label) {
        String fdEnv = System.getenv(envVar);
        if (fdEnv == null || fdEnv.isEmpty()) {
            // No FD env var — try the well-known file
            return readTokenFromWellKnownFile(wellKnownPath, label);
        }

        try {
            int fd = Integer.parseInt(fdEnv);

            // Use /dev/fd on macOS/BSD, /proc/self/fd on Linux
            String osName = System.getProperty("os.name").toLowerCase();
            String fdPath = osName.contains("mac") || osName.contains("freebsd")
                    ? "/dev/fd/" + fd
                    : "/proc/self/fd/" + fd;

            String token = Files.readString(Paths.get(fdPath)).trim();
            if (token.isEmpty()) {
                Debug.log("File descriptor contained empty " + label);
                return readTokenFromWellKnownFile(wellKnownPath, label);
            }

            Debug.log("Successfully read " + label + " from file descriptor " + fd);
            maybePersistTokenForSubprocesses(wellKnownPath, token, label);
            return token;
        } catch (NumberFormatException e) {
            Debug.log(envVar + " must be a valid file descriptor number, got: " + fdEnv);
            return readTokenFromWellKnownFile(wellKnownPath, label);
        } catch (Exception e) {
            Debug.log("Failed to read " + label + " from file descriptor: " + e.getMessage());
            return readTokenFromWellKnownFile(wellKnownPath, label);
        }
    }

    /**
     * Reset cached credentials.
     */
    public static void reset() {
        cachedOAuthToken = null;
        cachedApiKey = null;
        oauthTokenCached = false;
        apiKeyCached = false;
    }
}