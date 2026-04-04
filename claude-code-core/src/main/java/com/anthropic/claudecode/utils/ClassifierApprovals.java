/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code classifier approvals
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;

/**
 * Tracks which tool uses were auto-approved by classifiers.
 */
public final class ClassifierApprovals {
    private ClassifierApprovals() {}

    private static final ConcurrentHashMap<String, ClassifierApproval> classifierApprovals = new ConcurrentHashMap<>();
    private static final Set<String> classifierChecking = ConcurrentHashMap.newKeySet();

    /**
     * Classifier approval record.
     */
    public record ClassifierApproval(
            String classifier,
            String matchedRule,
            String reason
    ) {}

    /**
     * Set classifier approval for bash classifier.
     */
    public static void setClassifierApproval(String toolUseID, String matchedRule) {
        if (!isBashClassifierEnabled()) return;
        classifierApprovals.put(toolUseID, new ClassifierApproval("bash", matchedRule, null));
    }

    /**
     * Get classifier approval for bash.
     */
    public static String getClassifierApproval(String toolUseID) {
        if (!isBashClassifierEnabled()) return null;
        ClassifierApproval approval = classifierApprovals.get(toolUseID);
        if (approval == null || !"bash".equals(approval.classifier())) return null;
        return approval.matchedRule();
    }

    /**
     * Set yolo classifier approval.
     */
    public static void setYoloClassifierApproval(String toolUseID, String reason) {
        if (!isTranscriptClassifierEnabled()) return;
        classifierApprovals.put(toolUseID, new ClassifierApproval("auto-mode", null, reason));
    }

    /**
     * Get yolo classifier approval.
     */
    public static String getYoloClassifierApproval(String toolUseID) {
        if (!isTranscriptClassifierEnabled()) return null;
        ClassifierApproval approval = classifierApprovals.get(toolUseID);
        if (approval == null || !"auto-mode".equals(approval.classifier())) return null;
        return approval.reason();
    }

    /**
     * Set classifier checking status.
     */
    public static void setClassifierChecking(String toolUseID) {
        if (!isBashClassifierEnabled() && !isTranscriptClassifierEnabled()) return;
        classifierChecking.add(toolUseID);
    }

    /**
     * Clear classifier checking status.
     */
    public static void clearClassifierChecking(String toolUseID) {
        if (!isBashClassifierEnabled() && !isTranscriptClassifierEnabled()) return;
        classifierChecking.remove(toolUseID);
    }

    /**
     * Check if classifier is checking.
     */
    public static boolean isClassifierChecking(String toolUseID) {
        return classifierChecking.contains(toolUseID);
    }

    /**
     * Delete classifier approval.
     */
    public static void deleteClassifierApproval(String toolUseID) {
        classifierApprovals.remove(toolUseID);
    }

    /**
     * Clear all classifier approvals.
     */
    public static void clearClassifierApprovals() {
        classifierApprovals.clear();
        classifierChecking.clear();
    }

    // Feature flag placeholders
    private static boolean isBashClassifierEnabled() {
        return Boolean.parseBoolean(System.getenv("CLAUDE_CODE_BASH_CLASSIFIER"));
    }

    private static boolean isTranscriptClassifierEnabled() {
        return Boolean.parseBoolean(System.getenv("CLAUDE_CODE_TRANSCRIPT_CLASSIFIER"));
    }
}