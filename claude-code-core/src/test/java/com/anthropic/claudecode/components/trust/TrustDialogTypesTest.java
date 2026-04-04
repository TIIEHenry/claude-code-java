/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.components.trust;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.time.*;

/**
 * Tests for TrustDialogTypes.
 */
@DisplayName("TrustDialogTypes Tests")
class TrustDialogTypesTest {

    @Test
    @DisplayName("TrustLevel enum has correct values")
    void trustLevelEnumHasCorrectValues() {
        TrustDialogTypes.TrustLevel[] levels = TrustDialogTypes.TrustLevel.values();

        assertEquals(5, levels.length);
        assertTrue(Arrays.asList(levels).contains(TrustDialogTypes.TrustLevel.UNTRUSTED));
        assertTrue(Arrays.asList(levels).contains(TrustDialogTypes.TrustLevel.LOW));
        assertTrue(Arrays.asList(levels).contains(TrustDialogTypes.TrustLevel.MEDIUM));
        assertTrue(Arrays.asList(levels).contains(TrustDialogTypes.TrustLevel.HIGH));
        assertTrue(Arrays.asList(levels).contains(TrustDialogTypes.TrustLevel.FULL));
    }

    @Test
    @DisplayName("TrustLevel getLabel and getValue work correctly")
    void trustLevelGetLabelAndGetValueWorkCorrectly() {
        assertEquals("Untrusted", TrustDialogTypes.TrustLevel.UNTRUSTED.getLabel());
        assertEquals(0, TrustDialogTypes.TrustLevel.UNTRUSTED.getValue());
        assertEquals("Full Trust", TrustDialogTypes.TrustLevel.FULL.getLabel());
        assertEquals(4, TrustDialogTypes.TrustLevel.FULL.getValue());
    }

    @Test
    @DisplayName("TrustAction enum has correct values")
    void trustActionEnumHasCorrectValues() {
        TrustDialogTypes.TrustAction[] actions = TrustDialogTypes.TrustAction.values();

        assertEquals(6, actions.length);
        assertTrue(Arrays.asList(actions).contains(TrustDialogTypes.TrustAction.TRUST_ALWAYS));
        assertTrue(Arrays.asList(actions).contains(TrustDialogTypes.TrustAction.TRUST_ONCE));
        assertTrue(Arrays.asList(actions).contains(TrustDialogTypes.TrustAction.TRUST_SESSION));
        assertTrue(Arrays.asList(actions).contains(TrustDialogTypes.TrustAction.DO_NOT_TRUST));
        assertTrue(Arrays.asList(actions).contains(TrustDialogTypes.TrustAction.VIEW_DETAILS));
        assertTrue(Arrays.asList(actions).contains(TrustDialogTypes.TrustAction.CANCEL));
    }

    @Test
    @DisplayName("TrustDialogConfig forFile factory works correctly")
    void trustDialogConfigForFileFactoryWorksCorrectly() {
        TrustDialogTypes.TrustDialogConfig config = TrustDialogTypes.TrustDialogConfig.forFile("/path/to/file.txt");

        assertEquals("Trust File?", config.title());
        assertEquals(TrustDialogTypes.TrustLevel.MEDIUM, config.recommendedLevel());
        assertTrue(config.showDetails());
    }

    @Test
    @DisplayName("TrustDialogConfig forScript factory works correctly")
    void trustDialogConfigForScriptFactoryWorksCorrectly() {
        TrustDialogTypes.TrustDialogConfig config = TrustDialogTypes.TrustDialogConfig.forScript("/path/to/script.sh");

        assertEquals("Trust Script?", config.title());
        assertEquals(TrustDialogTypes.TrustLevel.LOW, config.recommendedLevel());
    }

    @Test
    @DisplayName("RiskAssessment factory methods work correctly")
    void riskAssessmentFactoryMethodsWorkCorrectly() {
        TrustDialogTypes.RiskAssessment low = TrustDialogTypes.RiskAssessment.low();
        TrustDialogTypes.RiskAssessment medium = TrustDialogTypes.RiskAssessment.medium();
        TrustDialogTypes.RiskAssessment high = TrustDialogTypes.RiskAssessment.high();

        assertEquals(TrustDialogTypes.RiskLevel.LOW, low.level());
        assertEquals(10, low.riskScore());

        assertEquals(TrustDialogTypes.RiskLevel.MEDIUM, medium.level());
        assertEquals(50, medium.riskScore());

        assertEquals(TrustDialogTypes.RiskLevel.HIGH, high.level());
        assertEquals(80, high.riskScore());
    }

    @Test
    @DisplayName("RiskLevel enum has correct values")
    void riskLevelEnumHasCorrectValues() {
        TrustDialogTypes.RiskLevel[] levels = TrustDialogTypes.RiskLevel.values();

        assertEquals(4, levels.length);
        assertTrue(Arrays.asList(levels).contains(TrustDialogTypes.RiskLevel.LOW));
        assertTrue(Arrays.asList(levels).contains(TrustDialogTypes.RiskLevel.MEDIUM));
        assertTrue(Arrays.asList(levels).contains(TrustDialogTypes.RiskLevel.HIGH));
        assertTrue(Arrays.asList(levels).contains(TrustDialogTypes.RiskLevel.CRITICAL));
    }

    @Test
    @DisplayName("RiskLevel getLabel and getColor work correctly")
    void riskLevelGetLabelAndGetColorWorkCorrectly() {
        assertNotNull(TrustDialogTypes.RiskLevel.LOW.getLabel());
        assertNotNull(TrustDialogTypes.RiskLevel.LOW.getColor());
    }

    @Test
    @DisplayName("TrustResult isTrusted works correctly")
    void trustResultIsTrustedWorksCorrectly() {
        TrustDialogTypes.TrustResult trusted = new TrustDialogTypes.TrustResult(
            TrustDialogTypes.TrustAction.TRUST_ONCE,
            TrustDialogTypes.TrustLevel.MEDIUM,
            "User approved",
            Instant.now()
        );

        TrustDialogTypes.TrustResult notTrusted = new TrustDialogTypes.TrustResult(
            TrustDialogTypes.TrustAction.DO_NOT_TRUST,
            TrustDialogTypes.TrustLevel.UNTRUSTED,
            "User denied",
            Instant.now()
        );

        assertTrue(trusted.isTrusted());
        assertFalse(notTrusted.isTrusted());
    }

    @Test
    @DisplayName("TrustEntry forPath factory works correctly")
    void trustEntryForPathFactoryWorksCorrectly() {
        TrustDialogTypes.TrustEntry entry = TrustDialogTypes.TrustEntry.forPath(
            "/path/to/file.txt",
            TrustDialogTypes.TrustLevel.HIGH,
            TrustDialogTypes.TrustScope.PERMANENT
        );

        assertNotNull(entry.id());
        assertEquals("/path/to/file.txt", entry.path());
        assertEquals(TrustDialogTypes.TrustLevel.HIGH, entry.level());
        assertEquals(TrustDialogTypes.TrustScope.PERMANENT, entry.scope());
        assertNotNull(entry.grantedAt());
    }

    @Test
    @DisplayName("TrustEntry isExpired works correctly")
    void trustEntryIsExpiredWorksCorrectly() {
        TrustDialogTypes.TrustEntry notExpired = new TrustDialogTypes.TrustEntry(
            "id", "path", TrustDialogTypes.TrustLevel.MEDIUM, TrustDialogTypes.TrustScope.SESSION,
            null, Instant.now(), Instant.now().plusSeconds(3600)
        );

        TrustDialogTypes.TrustEntry expired = new TrustDialogTypes.TrustEntry(
            "id", "path", TrustDialogTypes.TrustLevel.MEDIUM, TrustDialogTypes.TrustScope.SESSION,
            null, Instant.now(), Instant.now().minusSeconds(3600)
        );

        TrustDialogTypes.TrustEntry noExpiry = new TrustDialogTypes.TrustEntry(
            "id", "path", TrustDialogTypes.TrustLevel.MEDIUM, TrustDialogTypes.TrustScope.PERMANENT,
            null, Instant.now(), null
        );

        assertFalse(notExpired.isExpired());
        assertTrue(expired.isExpired());
        assertFalse(noExpiry.isExpired());
    }

    @Test
    @DisplayName("TrustScope enum has correct values")
    void trustScopeEnumHasCorrectValues() {
        TrustDialogTypes.TrustScope[] scopes = TrustDialogTypes.TrustScope.values();

        assertEquals(3, scopes.length);
        assertTrue(Arrays.asList(scopes).contains(TrustDialogTypes.TrustScope.ONCE));
        assertTrue(Arrays.asList(scopes).contains(TrustDialogTypes.TrustScope.SESSION));
        assertTrue(Arrays.asList(scopes).contains(TrustDialogTypes.TrustScope.PERMANENT));
    }

    @Test
    @DisplayName("MemoryTrustStore works correctly")
    void memoryTrustStoreWorksCorrectly() {
        TrustDialogTypes.MemoryTrustStore store = new TrustDialogTypes.MemoryTrustStore();

        TrustDialogTypes.TrustEntry entry = TrustDialogTypes.TrustEntry.forPath(
            "/test/path", TrustDialogTypes.TrustLevel.MEDIUM, TrustDialogTypes.TrustScope.SESSION
        );

        // Add trust
        store.addTrust(entry);
        assertTrue(store.getTrust("/test/path").isPresent());

        // Get all trusts
        assertEquals(1, store.getAllTrusts().size());

        // Remove trust
        store.removeTrust("/test/path");
        assertFalse(store.getTrust("/test/path").isPresent());
    }

    @Test
    @DisplayName("MemoryTrustStore clearSession works correctly")
    void memoryTrustStoreClearSessionWorksCorrectly() {
        TrustDialogTypes.MemoryTrustStore store = new TrustDialogTypes.MemoryTrustStore();

        TrustDialogTypes.TrustEntry sessionEntry = TrustDialogTypes.TrustEntry.forPath(
            "/session", TrustDialogTypes.TrustLevel.MEDIUM, TrustDialogTypes.TrustScope.SESSION
        );
        TrustDialogTypes.TrustEntry permanentEntry = TrustDialogTypes.TrustEntry.forPath(
            "/permanent", TrustDialogTypes.TrustLevel.HIGH, TrustDialogTypes.TrustScope.PERMANENT
        );

        store.addTrust(sessionEntry);
        store.addTrust(permanentEntry);

        store.clearSession();

        assertFalse(store.getTrust("/session").isPresent());
        assertTrue(store.getTrust("/permanent").isPresent());
    }

    @Test
    @DisplayName("MemoryTrustStore clearAll works correctly")
    void memoryTrustStoreClearAllWorksCorrectly() {
        TrustDialogTypes.MemoryTrustStore store = new TrustDialogTypes.MemoryTrustStore();

        store.addTrust(TrustDialogTypes.TrustEntry.forPath("/path1", TrustDialogTypes.TrustLevel.MEDIUM, TrustDialogTypes.TrustScope.SESSION));
        store.addTrust(TrustDialogTypes.TrustEntry.forPath("/path2", TrustDialogTypes.TrustLevel.HIGH, TrustDialogTypes.TrustScope.PERMANENT));

        store.clearAll();

        assertTrue(store.getAllTrusts().isEmpty());
    }
}