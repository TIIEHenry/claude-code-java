/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code components/TrustDialog
 */
package com.anthropic.claudecode.components.trust;

import java.time.Instant;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Trust dialog - Trust confirmation dialogs.
 */
public final class TrustDialogTypes {

    /**
     * Trust level enum.
     */
    public enum TrustLevel {
        UNTRUSTED("Untrusted", 0),
        LOW("Low Trust", 1),
        MEDIUM("Medium Trust", 2),
        HIGH("High Trust", 3),
        FULL("Full Trust", 4);

        private final String label;
        private final int value;

        TrustLevel(String label, int value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() { return label; }
        public int getValue() { return value; }
    }

    /**
     * Trust action enum.
     */
    public enum TrustAction {
        TRUST_ALWAYS,
        TRUST_ONCE,
        TRUST_SESSION,
        DO_NOT_TRUST,
        VIEW_DETAILS,
        CANCEL
    }

    /**
     * Trust dialog config record.
     */
    public record TrustDialogConfig(
        String title,
        String message,
        TrustLevel recommendedLevel,
        List<TrustAction> actions,
        boolean showDetails,
        String detailsContent,
        RiskAssessment riskAssessment
    ) {
        public static TrustDialogConfig forFile(String path) {
            return new TrustDialogConfig(
                "Trust File?",
                "Do you want to trust this file?",
                TrustLevel.MEDIUM,
                List.of(TrustAction.TRUST_ALWAYS, TrustAction.TRUST_ONCE, TrustAction.DO_NOT_TRUST),
                true,
                "File: " + path,
                RiskAssessment.medium()
            );
        }

        public static TrustDialogConfig forScript(String path) {
            return new TrustDialogConfig(
                "Trust Script?",
                "This script may modify your system. Do you want to trust it?",
                TrustLevel.LOW,
                List.of(TrustAction.TRUST_ONCE, TrustAction.VIEW_DETAILS, TrustAction.DO_NOT_TRUST),
                true,
                "Script: " + path,
                RiskAssessment.high()
            );
        }
    }

    /**
     * Risk assessment record.
     */
    public record RiskAssessment(
        RiskLevel level,
        List<String> warnings,
        List<String> recommendations,
        int riskScore
    ) {
        public static RiskAssessment low() {
            return new RiskAssessment(RiskLevel.LOW, Collections.emptyList(), Collections.emptyList(), 10);
        }

        public static RiskAssessment medium() {
            return new RiskAssessment(RiskLevel.MEDIUM,
                List.of("This file has not been reviewed"),
                List.of("Review the file contents before proceeding"),
                50);
        }

        public static RiskAssessment high() {
            return new RiskAssessment(RiskLevel.HIGH,
                List.of("This operation may modify system files"),
                List.of("Backup important data before proceeding"),
                80);
        }
    }

    /**
     * Risk level enum.
     */
    public enum RiskLevel {
        LOW("Low Risk", "\033[32m"),
        MEDIUM("Medium Risk", "\033[33m"),
        HIGH("High Risk", "\033[31m"),
        CRITICAL("Critical Risk", "\033[35m");

        private final String label;
        private final String color;

        RiskLevel(String label, String color) {
            this.label = label;
            this.color = color;
        }

        public String getLabel() { return label; }
        public String getColor() { return color; }
    }

    /**
     * Trust result record.
     */
    public record TrustResult(
        TrustAction action,
        TrustLevel grantedLevel,
        String reason,
        Instant timestamp
    ) {
        public boolean isTrusted() {
            return action != TrustAction.DO_NOT_TRUST && action != TrustAction.CANCEL;
        }
    }

    /**
     * Trust entry record.
     */
    public record TrustEntry(
        String id,
        String path,
        TrustLevel level,
        TrustScope scope,
        String checksum,
        Instant grantedAt,
        Instant expiresAt
    ) {
        public static TrustEntry forPath(String path, TrustLevel level, TrustScope scope) {
            return new TrustEntry(
                UUID.randomUUID().toString(),
                path,
                level,
                scope,
                null,
                Instant.now(),
                null
            );
        }

        public boolean isExpired() {
            return expiresAt != null && Instant.now().isAfter(expiresAt);
        }
    }

    /**
     * Trust scope enum.
     */
    public enum TrustScope {
        ONCE,
        SESSION,
        PERMANENT
    }

    /**
     * Trust store interface.
     */
    public interface TrustStore {
        Optional<TrustEntry> getTrust(String path);
        void addTrust(TrustEntry entry);
        void removeTrust(String path);
        List<TrustEntry> getAllTrusts();
        void clearSession();
        void clearAll();
    }

    /**
     * In-memory trust store.
     */
    public static final class MemoryTrustStore implements TrustStore {
        private final Map<String, TrustEntry> trusts = new ConcurrentHashMap<>();

        @Override
        public Optional<TrustEntry> getTrust(String path) {
            return Optional.ofNullable(trusts.get(path));
        }

        @Override
        public void addTrust(TrustEntry entry) {
            trusts.put(entry.path(), entry);
        }

        @Override
        public void removeTrust(String path) {
            trusts.remove(path);
        }

        @Override
        public List<TrustEntry> getAllTrusts() {
            return new ArrayList<>(trusts.values());
        }

        @Override
        public void clearSession() {
            trusts.entrySet().removeIf(e -> e.getValue().scope() == TrustScope.SESSION);
        }

        @Override
        public void clearAll() {
            trusts.clear();
        }
    }
}