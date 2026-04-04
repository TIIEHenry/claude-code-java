/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/voiceStreamSTT.ts
 */
package com.anthropic.claudecode.services.voice;

import java.net.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Voice stream speech-to-text client for push-to-talk.
 *
 * Connects to Anthropic's voice_stream WebSocket endpoint using OAuth.
 * Designed for hold-to-talk: hold the keybinding to record, release to stop.
 */
public final class VoiceStreamSTT {
    private VoiceStreamSTT() {}

    // Constants
    private static final String VOICE_STREAM_PATH = "/api/ws/speech_to_text/voice_stream";
    private static final int KEEPALIVE_INTERVAL_MS = 8000;
    private static final String KEEPALIVE_MSG = "{\"type\":\"KeepAlive\"}";
    private static final String CLOSE_STREAM_MSG = "{\"type\":\"CloseStream\"}";

    // Finalize timeouts
    public static final int FINALIZE_SAFETY_TIMEOUT_MS = 5000;
    public static final int FINALIZE_NO_DATA_TIMEOUT_MS = 1500;

    /**
     * Check if voice stream is available.
     */
    public static boolean isVoiceStreamAvailable() {
        // Check if OAuth tokens are available
        try {
            // Check for OAuth token in environment or token file
            String authToken = System.getenv("ANTHROPIC_AUTH_TOKEN");
            if (authToken != null && !authToken.isEmpty()) {
                return true;
            }

            // Check for token file
            java.nio.file.Path tokenPath = java.nio.file.Paths.get(
                System.getProperty("user.home"), ".claude", "oauth-token.json"
            );
            if (java.nio.file.Files.exists(tokenPath)) {
                String content = java.nio.file.Files.readString(tokenPath);
                if (content.contains("\"access_token\"")) {
                    return true;
                }
            }

            // Check for API key as fallback
            String apiKey = System.getenv("ANTHROPIC_API_KEY");
            return apiKey != null && !apiKey.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Connect to voice stream WebSocket.
     */
    public static CompletableFuture<VoiceStreamConnection> connectVoiceStream(
        VoiceStreamCallbacks callbacks,
        VoiceStreamOptions options
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get OAuth token or API key
                String authToken = getAuthToken();
                if (authToken == null) {
                    callbacks.onError("No authentication token available", new ErrorOptions(true));
                    return null;
                }

                // Build WebSocket URL
                String baseUrl = System.getenv("ANTHROPIC_WS_URL");
                if (baseUrl == null) {
                    baseUrl = "wss://ws.anthropic.com";
                }
                String wsUrl = buildWebSocketUrl(baseUrl, options);

                // Create connection
                InternalConnection connection = new InternalConnection(callbacks, options);

                // Note: Full WebSocket implementation requires a WebSocket client library
                // For now, return the connection object which can be used with external WS client
                // In production, this would use Java-WebSocket, Tyrus, or similar

                // Simulate connection ready
                connection.handleOpen();

                return connection;
            } catch (Exception e) {
                callbacks.onError("Failed to connect: " + e.getMessage(), new ErrorOptions(true));
                return null;
            }
        });
    }

    private static String getAuthToken() {
        // Try OAuth token first
        String authToken = System.getenv("ANTHROPIC_AUTH_TOKEN");
        if (authToken != null && !authToken.isEmpty()) {
            return authToken;
        }

        // Try reading from token file
        try {
            java.nio.file.Path tokenPath = java.nio.file.Paths.get(
                System.getProperty("user.home"), ".claude", "oauth-token.json"
            );
            if (java.nio.file.Files.exists(tokenPath)) {
                String content = java.nio.file.Files.readString(tokenPath);
                int idx = content.indexOf("\"access_token\"");
                if (idx >= 0) {
                    int valStart = content.indexOf("\"", idx + 14) + 1;
                    int valEnd = content.indexOf("\"", valStart);
                    if (valStart > 0 && valEnd > valStart) {
                        return content.substring(valStart, valEnd);
                    }
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        // Fallback to API key
        return System.getenv("ANTHROPIC_API_KEY");
    }

    // ─── Types ────────────────────────────────────────────────────────────

    public interface VoiceStreamCallbacks {
        void onTranscript(String text, boolean isFinal);
        void onError(String error, ErrorOptions options);
        void onClose();
        void onReady(VoiceStreamConnection connection);
    }

    public record ErrorOptions(boolean fatal) {
        public ErrorOptions() {
            this(false);
        }
    }

    public record VoiceStreamOptions(
        String language,
        List<String> keyterms
    ) {
        public VoiceStreamOptions() {
            this("en", Collections.emptyList());
        }
    }

    public enum FinalizeSource {
        POST_CLOSESTREAM_ENDPOINT,
        NO_DATA_TIMEOUT,
        SAFETY_TIMEOUT,
        WS_CLOSE,
        WS_ALREADY_CLOSED
    }

    public interface VoiceStreamConnection {
        void send(byte[] audioChunk);
        CompletableFuture<FinalizeSource> finalizeStream();
        void close();
        boolean isConnected();
    }

    // ─── WebSocket message types ──────────────────────────────────────────

    public record TranscriptText(String type, String data) {
        public TranscriptText(String data) {
            this("TranscriptText", data);
        }
    }

    public record TranscriptEndpoint(String type) {
        public TranscriptEndpoint() {
            this("TranscriptEndpoint");
        }
    }

    public record TranscriptError(
        String type,
        String errorCode,
        String description
    ) {
        public TranscriptError() {
            this("TranscriptError", null, null);
        }
    }

    // ─── Internal implementation class ─────────────────────────────────────

    /**
     * Internal WebSocket connection implementation.
     *
     * Note: This is a placeholder structure. Full implementation requires
     * a WebSocket client library like Java-WebSocket or Tyrus.
     */
    static class InternalConnection implements VoiceStreamConnection {
        private volatile boolean connected = false;
        private volatile boolean finalized = false;
        private volatile boolean finalizing = false;
        private volatile String lastTranscriptText = "";
        private ScheduledFuture<?> keepaliveTimer = null;
        private CompletableFuture<FinalizeSource> finalizePromise = null;

        private final VoiceStreamCallbacks callbacks;
        private final VoiceStreamOptions options;

        InternalConnection(VoiceStreamCallbacks callbacks, VoiceStreamOptions options) {
            this.callbacks = callbacks;
            this.options = options;
        }

        @Override
        public void send(byte[] audioChunk) {
            if (!connected || finalized) {
                return;
            }
            // Would send to WebSocket
        }

        @Override
        public CompletableFuture<FinalizeSource> finalizeStream() {
            if (finalizing || finalized) {
                return CompletableFuture.completedFuture(FinalizeSource.WS_ALREADY_CLOSED);
            }
            finalizing = true;

            finalizePromise = new CompletableFuture<>();

            // Set up timers
            ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

            // Safety timer
            scheduler.schedule(() -> {
                if (!finalizePromise.isDone()) {
                    promoteLastTranscript();
                    finalizePromise.complete(FinalizeSource.SAFETY_TIMEOUT);
                }
            }, FINALIZE_SAFETY_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            // No-data timer
            scheduler.schedule(() -> {
                if (!finalizePromise.isDone() && finalized) {
                    promoteLastTranscript();
                    finalizePromise.complete(FinalizeSource.NO_DATA_TIMEOUT);
                }
            }, FINALIZE_NO_DATA_TIMEOUT_MS, TimeUnit.MILLISECONDS);

            // Deferred CloseStream send
            scheduler.schedule(() -> {
                finalized = true;
                // Would send CLOSE_STREAM_MSG to WebSocket
            }, 0, TimeUnit.MILLISECONDS);

            return finalizePromise;
        }

        @Override
        public void close() {
            finalized = true;
            if (keepaliveTimer != null) {
                keepaliveTimer.cancel(false);
                keepaliveTimer = null;
            }
            connected = false;
        }

        @Override
        public boolean isConnected() {
            return connected && !finalized;
        }

        private void promoteLastTranscript() {
            if (!lastTranscriptText.isEmpty()) {
                String text = lastTranscriptText;
                lastTranscriptText = "";
                callbacks.onTranscript(text, true);
            }
        }

        // Message handlers
        void handleMessage(String message) {
            // Parse JSON and handle
            if (message.contains("TranscriptText")) {
                // Extract data field
                callbacks.onTranscript(lastTranscriptText, false);
            } else if (message.contains("TranscriptEndpoint")) {
                promoteLastTranscript();
                if (finalized && finalizePromise != null && !finalizePromise.isDone()) {
                    finalizePromise.complete(FinalizeSource.POST_CLOSESTREAM_ENDPOINT);
                }
            } else if (message.contains("TranscriptError")) {
                callbacks.onError("Transcription error", new ErrorOptions(false));
            }
        }

        void handleOpen() {
            connected = true;
            // Send initial keepalive
            // Start keepalive timer
            callbacks.onReady(this);
        }

        void handleClose(int code, String reason) {
            connected = false;
            if (keepaliveTimer != null) {
                keepaliveTimer.cancel(false);
                keepaliveTimer = null;
            }
            promoteLastTranscript();
            if (finalizePromise != null && !finalizePromise.isDone()) {
                finalizePromise.complete(FinalizeSource.WS_CLOSE);
            }
            if (!finalizing && code != 1000 && code != 1005) {
                callbacks.onError("Connection closed: code " + code, new ErrorOptions(false));
            }
            callbacks.onClose();
        }

        void handleError(Exception error) {
            if (!finalizing) {
                callbacks.onError("Voice stream error: " + error.getMessage(), new ErrorOptions(false));
            }
        }
    }

    /**
     * Build WebSocket URL with parameters.
     */
    public static String buildWebSocketUrl(String baseUrl, VoiceStreamOptions options) {
        StringBuilder url = new StringBuilder(baseUrl);
        url.append(VOICE_STREAM_PATH);
        url.append("?encoding=linear16");
        url.append("&sample_rate=16000");
        url.append("&channels=1");
        url.append("&endpointing_ms=300");
        url.append("&utterance_end_ms=1000");
        url.append("&language=").append(options.language() != null ? options.language() : "en");

        // Add keyterms
        if (options.keyterms() != null) {
            for (String term : options.keyterms()) {
                try {
                    url.append("&keyterms=").append(URLEncoder.encode(term, "UTF-8"));
                } catch (Exception e) {
                    // Skip invalid term
                }
            }
        }

        return url.toString();
    }
}