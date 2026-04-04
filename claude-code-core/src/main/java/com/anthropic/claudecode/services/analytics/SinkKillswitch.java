/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/analytics/sinkKillswitch
 */
package com.anthropic.claudecode.services.analytics;

import java.util.*;

/**
 * Sink killswitch - Per-sink analytics killswitch from GrowthBook.
 *
 * GrowthBook JSON config that disables individual analytics sinks.
 * Shape: { datadog?: boolean, firstParty?: boolean }
 */
public final class SinkKillswitch {
    private static final String CONFIG_NAME = "tengu_frond_boric";

    private final GrowthBookService growthBook;

    /**
     * Create sink killswitch.
     */
    public SinkKillswitch(GrowthBookService growthBook) {
        this.growthBook = growthBook;
    }

    /**
     * Sink name enum.
     */
    public enum SinkName {
        DATADOG,
        FIRST_PARTY
    }

    /**
     * Check if sink is killed.
     *
     * NOTE: Must NOT be called from inside is1PEventLoggingEnabled() -
     * growthbook.ts:isGrowthBookEnabled() calls that, so a lookup would recurse.
     * Call at per-event dispatch sites instead.
     */
    public boolean isSinkKilled(String sink) {
        Map<String, Boolean> config = GrowthBookService.getDynamicConfigCached(
            CONFIG_NAME,
            Collections.emptyMap()
        );

        // getFeatureValue guards on !== undefined, so a cached JSON null
        // leaks through instead of falling back to empty map.
        Boolean killed = config.get(sink);
        return killed != null && killed;
    }

    /**
     * Check if sink is killed by enum.
     */
    public boolean isSinkKilled(SinkName sink) {
        return isSinkKilled(sink.name().toLowerCase().replace("_", ""));
    }

    /**
     * Get all killed sinks.
     */
    public Set<String> getKilledSinks() {
        Map<String, Boolean> config = GrowthBookService.getDynamicConfigCached(
            CONFIG_NAME,
            Collections.emptyMap()
        );

        Set<String> killed = new HashSet<>();
        for (Map.Entry<String, Boolean> entry : config.entrySet()) {
            if (entry.getValue() != null && entry.getValue()) {
                killed.add(entry.getKey());
            }
        }
        return killed;
    }
}