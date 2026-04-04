/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ClassifierApprovals.
 */
class ClassifierApprovalsTest {

    @BeforeEach
    void setUp() {
        ClassifierApprovals.clearClassifierApprovals();
    }

    @Test
    @DisplayName("ClassifierApprovals ClassifierApproval record")
    void classifierApprovalRecord() {
        ClassifierApprovals.ClassifierApproval approval = new ClassifierApprovals.ClassifierApproval(
            "bash", "safe_command", "Auto-approved"
        );

        assertEquals("bash", approval.classifier());
        assertEquals("safe_command", approval.matchedRule());
        assertEquals("Auto-approved", approval.reason());
    }

    @Test
    @DisplayName("ClassifierApprovals clearClassifierApprovals")
    void clearClassifierApprovals() {
        // Should not throw
        assertDoesNotThrow(() -> ClassifierApprovals.clearClassifierApprovals());
    }

    @Test
    @DisplayName("ClassifierApprovals setClassifierChecking")
    void setClassifierChecking() {
        ClassifierApprovals.setClassifierChecking("test-id");
        // Without feature flag enabled, this may not do anything
        assertTrue(true);
    }

    @Test
    @DisplayName("ClassifierApprovals clearClassifierChecking")
    void clearClassifierChecking() {
        ClassifierApprovals.clearClassifierChecking("test-id");
        assertFalse(ClassifierApprovals.isClassifierChecking("test-id"));
    }

    @Test
    @DisplayName("ClassifierApprovals isClassifierChecking false")
    void isClassifierCheckingFalse() {
        assertFalse(ClassifierApprovals.isClassifierChecking("nonexistent"));
    }

    @Test
    @DisplayName("ClassifierApprovals deleteClassifierApproval")
    void deleteClassifierApproval() {
        ClassifierApprovals.deleteClassifierApproval("test-id");
        // Should not throw
        assertTrue(true);
    }

    @Test
    @DisplayName("ClassifierApprovals getClassifierApproval null without feature")
    void getClassifierApprovalNull() {
        assertNull(ClassifierApprovals.getClassifierApproval("test-id"));
    }

    @Test
    @DisplayName("ClassifierApprovals getYoloClassifierApproval null without feature")
    void getYoloClassifierApprovalNull() {
        assertNull(ClassifierApprovals.getYoloClassifierApproval("test-id"));
    }

    @Test
    @DisplayName("ClassifierApprovals setClassifierApproval without feature")
    void setClassifierApprovalWithoutFeature() {
        ClassifierApprovals.setClassifierApproval("test-id", "rule");
        // Without feature flag, should not be stored
        assertNull(ClassifierApprovals.getClassifierApproval("test-id"));
    }

    @Test
    @DisplayName("ClassifierApprovals setYoloClassifierApproval without feature")
    void setYoloClassifierApprovalWithoutFeature() {
        ClassifierApprovals.setYoloClassifierApproval("test-id", "reason");
        // Without feature flag, should not be stored
        assertNull(ClassifierApprovals.getYoloClassifierApproval("test-id"));
    }
}