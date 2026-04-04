/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/advisor
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Advisor - Advisory message system.
 */
public final class Advisor {
    private final List<AdvisoryMessage> messages = new ArrayList<>();
    private final AdvisoryLevel threshold = AdvisoryLevel.INFO;

    /**
     * Add advisory message.
     */
    public void addAdvisory(AdvisoryLevel level, String message) {
        if (level.ordinal() >= threshold.ordinal()) {
            messages.add(new AdvisoryMessage(level, message, System.currentTimeMillis()));
        }
    }

    /**
     * Add info advisory.
     */
    public void info(String message) {
        addAdvisory(AdvisoryLevel.INFO, message);
    }

    /**
     * Add warning advisory.
     */
    public void warning(String message) {
        addAdvisory(AdvisoryLevel.WARNING, message);
    }

    /**
     * Add error advisory.
     */
    public void error(String message) {
        addAdvisory(AdvisoryLevel.ERROR, message);
    }

    /**
     * Get all messages.
     */
    public List<AdvisoryMessage> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    /**
     * Get messages by level.
     */
    public List<AdvisoryMessage> getMessagesByLevel(AdvisoryLevel level) {
        return messages.stream()
            .filter(m -> m.level() == level)
            .toList();
    }

    /**
     * Clear messages.
     */
    public void clear() {
        messages.clear();
    }

    /**
     * Check if has errors.
     */
    public boolean hasErrors() {
        return messages.stream().anyMatch(m -> m.level() == AdvisoryLevel.ERROR);
    }

    /**
     * Check if has warnings.
     */
    public boolean hasWarnings() {
        return messages.stream().anyMatch(m -> m.level() == AdvisoryLevel.WARNING);
    }

    /**
     * Get summary string.
     */
    public String getSummary() {
        int errors = (int) messages.stream().filter(m -> m.level() == AdvisoryLevel.ERROR).count();
        int warnings = (int) messages.stream().filter(m -> m.level() == AdvisoryLevel.WARNING).count();
        int infos = (int) messages.stream().filter(m -> m.level() == AdvisoryLevel.INFO).count();

        return String.format("Errors: %d, Warnings: %d, Info: %d", errors, warnings, infos);
    }

    /**
     * Advisory level enum.
     */
    public enum AdvisoryLevel {
        DEBUG,
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }

    /**
     * Advisory message record.
     */
    public record AdvisoryMessage(
        AdvisoryLevel level,
        String message,
        long timestamp
    ) {
        public String format() {
            return String.format("[%s] %s", level.name(), message);
        }
    }
}