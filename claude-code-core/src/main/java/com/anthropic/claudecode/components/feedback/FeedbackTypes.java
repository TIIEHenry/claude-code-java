/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code components/FeedbackSurvey
 */
package com.anthropic.claudecode.components.feedback;

import java.util.*;
import java.time.*;

/**
 * Feedback survey - Feedback collection types.
 */
public final class FeedbackTypes {

    /**
     * Feedback type enum.
     */
    public enum FeedbackCategory {
        GENERAL("General feedback"),
        BUG("Bug report"),
        FEATURE("Feature request"),
        PERFORMANCE("Performance issue"),
        UI("UI/UX feedback"),
        DOCUMENTATION("Documentation"),
        OTHER("Other");

        private final String label;

        FeedbackCategory(String label) {
            this.label = label;
        }

        public String getLabel() { return label; }
    }

    /**
     * Feedback severity enum.
     */
    public enum FeedbackSeverity {
        LOW(1, "Low"),
        MEDIUM(2, "Medium"),
        HIGH(3, "High"),
        CRITICAL(4, "Critical");

        private final int value;
        private final String label;

        FeedbackSeverity(int value, String label) {
            this.value = value;
            this.label = label;
        }

        public int getValue() { return value; }
        public String getLabel() { return label; }
    }

    /**
     * Feedback entry record.
     */
    public record FeedbackEntry(
        String id,
        FeedbackCategory category,
        FeedbackSeverity severity,
        String title,
        String description,
        String userEmail,
        Map<String, Object> metadata,
        List<String> attachments,
        Instant submittedAt,
        FeedbackStatus status,
        String sessionId,
        String version
    ) {
        public static FeedbackEntry create(
            FeedbackCategory category,
            String title,
            String description
        ) {
            return new FeedbackEntry(
                UUID.randomUUID().toString(),
                category,
                FeedbackSeverity.MEDIUM,
                title,
                description,
                null,
                new HashMap<>(),
                new ArrayList<>(),
                Instant.now(),
                FeedbackStatus.NEW,
                null,
                "1.0.0"
            );
        }

        public FeedbackEntry withMetadata(String key, Object value) {
            Map<String, Object> newMeta = new HashMap<>(metadata);
            newMeta.put(key, value);
            return new FeedbackEntry(
                id, category, severity, title, description,
                userEmail, newMeta, attachments, submittedAt,
                status, sessionId, version
            );
        }

        public FeedbackEntry withAttachment(String path) {
            List<String> newAttachments = new ArrayList<>(attachments);
            newAttachments.add(path);
            return new FeedbackEntry(
                id, category, severity, title, description,
                userEmail, metadata, newAttachments, submittedAt,
                status, sessionId, version
            );
        }
    }

    /**
     * Feedback status enum.
     */
    public enum FeedbackStatus {
        NEW,
        TRIAGED,
        IN_PROGRESS,
        RESOLVED,
        CLOSED,
        WONT_FIX
    }

    /**
     * Feedback survey config record.
     */
    public record SurveyConfig(
        String title,
        String description,
        List<SurveyQuestion> questions,
        boolean anonymous,
        boolean requireEmail,
        int maxLength,
        boolean allowAttachments
    ) {
        public static SurveyConfig defaultConfig() {
            return new SurveyConfig(
                "Claude Code Feedback",
                "Help us improve Claude Code",
                new ArrayList<>(),
                false,
                false,
                5000,
                true
            );
        }
    }

    /**
     * Survey question record.
     */
    public record SurveyQuestion(
        String id,
        String question,
        QuestionType type,
        List<String> options,
        boolean required,
        String placeholder
    ) {
        public static SurveyQuestion text(String id, String question) {
            return new SurveyQuestion(id, question, QuestionType.TEXT, Collections.emptyList(), false, "");
        }

        public static SurveyQuestion rating(String id, String question) {
            return new SurveyQuestion(id, question, QuestionType.RATING, Collections.emptyList(), true, "");
        }

        public static SurveyQuestion multipleChoice(String id, String question, List<String> options) {
            return new SurveyQuestion(id, question, QuestionType.MULTIPLE_CHOICE, options, true, "");
        }
    }

    /**
     * Question type enum.
     */
    public enum QuestionType {
        TEXT,
        TEXTAREA,
        RATING,
        MULTIPLE_CHOICE,
        CHECKBOX,
        BOOLEAN
    }

    /**
     * Survey response record.
     */
    public record SurveyResponse(
        String surveyId,
        Map<String, Object> answers,
        Instant submittedAt
    ) {
        public Object getAnswer(String questionId) {
            return answers.get(questionId);
        }

        public String getAnswerText(String questionId) {
            Object answer = getAnswer(questionId);
            return answer != null ? answer.toString() : null;
        }

        public int getRating(String questionId) {
            Object answer = getAnswer(questionId);
            if (answer instanceof Number) {
                return ((Number) answer).intValue();
            }
            return 0;
        }
    }
}