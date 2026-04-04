/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/analytics/firstPartyEventLogger
 */
package com.anthropic.claudecode.services.analytics;

import java.util.*;
import java.util.concurrent.*;

/**
 * First party event logger - Internal analytics event logging.
 *
 * Events are batched and exported to internal analytics endpoints.
 */
public final class FirstPartyEventLogger {
    private volatile boolean enabled = false;
    private final AnalyticsConfig config;
    private final GrowthBookService growthBook;
    private final SinkKillswitch sinkKillswitch;

    /**
     * Event sampling config.
     */
    public record EventSamplingConfig(Map<String, EventSamplingEntry> events) {
        public static EventSamplingConfig empty() {
            return new EventSamplingConfig(Collections.emptyMap());
        }
    }

    /**
     * Event sampling entry.
     */
    public record EventSamplingEntry(double sampleRate) {}

    /**
     * Batch config.
     */
    public record BatchConfig(
        Long scheduledDelayMillis,
        Integer maxExportBatchSize,
        Integer maxQueueSize,
        Boolean skipAuth,
        Integer maxAttempts,
        String path,
        String baseUrl
    ) {
        public static BatchConfig defaults() {
            return new BatchConfig(10000L, 200, 8192, false, 3, null, null);
        }
    }

    /**
     * GrowthBook experiment data.
     */
    public record GrowthBookExperimentData(
        String experimentId,
        int variationId,
        GrowthBookUserAttributes userAttributes,
        Map<String, Object> experimentMetadata
    ) {}

    /**
     * GrowthBook user attributes.
     */
    public record GrowthBookUserAttributes(String sessionId, String deviceId) {}

    /**
     * Create first party event logger.
     */
    public FirstPartyEventLogger(
        AnalyticsConfig config,
        GrowthBookService growthBook,
        SinkKillswitch sinkKillswitch
    ) {
        this.config = config;
        this.growthBook = growthBook;
        this.sinkKillswitch = sinkKillswitch;
    }

    /**
     * Get event sampling config from GrowthBook.
     */
    public EventSamplingConfig getEventSamplingConfig() {
        return EventSamplingConfig.empty();
    }

    /**
     * Determine if an event should be sampled.
     */
    public Double shouldSampleEvent(String eventName) {
        return 1.0; // Log everything by default
    }

    /**
     * Get batch config from GrowthBook.
     */
    public BatchConfig getBatchConfig() {
        return BatchConfig.defaults();
    }

    /**
     * Enable or disable the logger.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Check if logger is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Log an event.
     */
    public void logEvent(String eventName, Map<String, Object> data) {
        if (!enabled) return;
        // Convert Map<String, Object> to Map<String, String> for logging
        Map<String, String> stringData = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            stringData.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        AnalyticsMetadata.logEvent(eventName, stringData);
    }

    /**
     * Flush pending events.
     */
    public void flush() {
        // No-op for now
    }
}