/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.components.feedback;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.time.*;

/**
 * Tests for FeedbackTypes.
 */
@DisplayName("FeedbackTypes Tests")
class FeedbackTypesTest {

    @Test
    @DisplayName("FeedbackCategory enum has correct values")
    void feedbackCategoryEnumHasCorrectValues() {
        FeedbackTypes.FeedbackCategory[] categories = FeedbackTypes.FeedbackCategory.values();

        assertEquals(7, categories.length);
        assertTrue(Arrays.asList(categories).contains(FeedbackTypes.FeedbackCategory.GENERAL));
        assertTrue(Arrays.asList(categories).contains(FeedbackTypes.FeedbackCategory.BUG));
        assertTrue(Arrays.asList(categories).contains(FeedbackTypes.FeedbackCategory.FEATURE));
        assertTrue(Arrays.asList(categories).contains(FeedbackTypes.FeedbackCategory.PERFORMANCE));
        assertTrue(Arrays.asList(categories).contains(FeedbackTypes.FeedbackCategory.UI));
        assertTrue(Arrays.asList(categories).contains(FeedbackTypes.FeedbackCategory.DOCUMENTATION));
        assertTrue(Arrays.asList(categories).contains(FeedbackTypes.FeedbackCategory.OTHER));
    }

    @Test
    @DisplayName("FeedbackCategory getLabel returns correct label")
    void feedbackCategoryGetLabelReturnsCorrectLabel() {
        assertEquals("General feedback", FeedbackTypes.FeedbackCategory.GENERAL.getLabel());
        assertEquals("Bug report", FeedbackTypes.FeedbackCategory.BUG.getLabel());
        assertEquals("Feature request", FeedbackTypes.FeedbackCategory.FEATURE.getLabel());
    }

    @Test
    @DisplayName("FeedbackSeverity enum has correct values")
    void feedbackSeverityEnumHasCorrectValues() {
        FeedbackTypes.FeedbackSeverity[] severities = FeedbackTypes.FeedbackSeverity.values();

        assertEquals(4, severities.length);
        assertTrue(Arrays.asList(severities).contains(FeedbackTypes.FeedbackSeverity.LOW));
        assertTrue(Arrays.asList(severities).contains(FeedbackTypes.FeedbackSeverity.MEDIUM));
        assertTrue(Arrays.asList(severities).contains(FeedbackTypes.FeedbackSeverity.HIGH));
        assertTrue(Arrays.asList(severities).contains(FeedbackTypes.FeedbackSeverity.CRITICAL));
    }

    @Test
    @DisplayName("FeedbackSeverity getValue and getLabel work correctly")
    void feedbackSeverityGetValueAndGetLabelWorkCorrectly() {
        assertEquals(1, FeedbackTypes.FeedbackSeverity.LOW.getValue());
        assertEquals(2, FeedbackTypes.FeedbackSeverity.MEDIUM.getValue());
        assertEquals(3, FeedbackTypes.FeedbackSeverity.HIGH.getValue());
        assertEquals(4, FeedbackTypes.FeedbackSeverity.CRITICAL.getValue());

        assertEquals("Low", FeedbackTypes.FeedbackSeverity.LOW.getLabel());
        assertEquals("Critical", FeedbackTypes.FeedbackSeverity.CRITICAL.getLabel());
    }

    @Test
    @DisplayName("FeedbackEntry create factory method works correctly")
    void feedbackEntryCreateFactoryMethodWorksCorrectly() {
        FeedbackTypes.FeedbackEntry entry = FeedbackTypes.FeedbackEntry.create(
            FeedbackTypes.FeedbackCategory.BUG,
            "Test Bug",
            "Bug description"
        );

        assertNotNull(entry);
        assertNotNull(entry.id());
        assertEquals(FeedbackTypes.FeedbackCategory.BUG, entry.category());
        assertEquals(FeedbackTypes.FeedbackSeverity.MEDIUM, entry.severity());
        assertEquals("Test Bug", entry.title());
        assertEquals("Bug description", entry.description());
        assertEquals(FeedbackTypes.FeedbackStatus.NEW, entry.status());
    }

    @Test
    @DisplayName("FeedbackEntry record works correctly")
    void feedbackEntryRecordWorksCorrectly() {
        Instant now = Instant.now();
        FeedbackTypes.FeedbackEntry entry = new FeedbackTypes.FeedbackEntry(
            "fb-123",
            FeedbackTypes.FeedbackCategory.FEATURE,
            FeedbackTypes.FeedbackSeverity.HIGH,
            "Feature Request",
            "Description",
            "user@example.com",
            Map.of("browser", "chrome"),
            List.of("/path/to/screenshot.png"),
            now,
            FeedbackTypes.FeedbackStatus.IN_PROGRESS,
            "session-456",
            "1.0.0"
        );

        assertEquals("fb-123", entry.id());
        assertEquals(FeedbackTypes.FeedbackCategory.FEATURE, entry.category());
        assertEquals(FeedbackTypes.FeedbackSeverity.HIGH, entry.severity());
        assertEquals("Feature Request", entry.title());
        assertEquals("user@example.com", entry.userEmail());
        assertEquals(1, entry.metadata().size());
        assertEquals(1, entry.attachments().size());
        assertEquals(now, entry.submittedAt());
        assertEquals(FeedbackTypes.FeedbackStatus.IN_PROGRESS, entry.status());
    }

    @Test
    @DisplayName("FeedbackEntry withMetadata works correctly")
    void feedbackEntryWithMetadataWorksCorrectly() {
        FeedbackTypes.FeedbackEntry entry = FeedbackTypes.FeedbackEntry.create(
            FeedbackTypes.FeedbackCategory.GENERAL,
            "Title",
            "Description"
        );

        FeedbackTypes.FeedbackEntry withMeta = entry.withMetadata("os", "macOS");

        assertEquals("macOS", withMeta.metadata().get("os"));
        assertFalse(entry.metadata().containsKey("os"));
    }

    @Test
    @DisplayName("FeedbackEntry withAttachment works correctly")
    void feedbackEntryWithAttachmentWorksCorrectly() {
        FeedbackTypes.FeedbackEntry entry = FeedbackTypes.FeedbackEntry.create(
            FeedbackTypes.FeedbackCategory.BUG,
            "Title",
            "Description"
        );

        FeedbackTypes.FeedbackEntry withAttachment = entry.withAttachment("/path/to/file.png");

        assertEquals(1, withAttachment.attachments().size());
        assertEquals("/path/to/file.png", withAttachment.attachments().get(0));
    }

    @Test
    @DisplayName("FeedbackStatus enum has correct values")
    void feedbackStatusEnumHasCorrectValues() {
        FeedbackTypes.FeedbackStatus[] statuses = FeedbackTypes.FeedbackStatus.values();

        assertEquals(6, statuses.length);
        assertTrue(Arrays.asList(statuses).contains(FeedbackTypes.FeedbackStatus.NEW));
        assertTrue(Arrays.asList(statuses).contains(FeedbackTypes.FeedbackStatus.TRIAGED));
        assertTrue(Arrays.asList(statuses).contains(FeedbackTypes.FeedbackStatus.IN_PROGRESS));
        assertTrue(Arrays.asList(statuses).contains(FeedbackTypes.FeedbackStatus.RESOLVED));
        assertTrue(Arrays.asList(statuses).contains(FeedbackTypes.FeedbackStatus.CLOSED));
        assertTrue(Arrays.asList(statuses).contains(FeedbackTypes.FeedbackStatus.WONT_FIX));
    }

    @Test
    @DisplayName("SurveyConfig defaultConfig returns valid config")
    void surveyConfigDefaultConfigReturnsValidConfig() {
        FeedbackTypes.SurveyConfig config = FeedbackTypes.SurveyConfig.defaultConfig();

        assertNotNull(config);
        assertEquals("Claude Code Feedback", config.title());
        assertEquals("Help us improve Claude Code", config.description());
        assertFalse(config.anonymous());
        assertFalse(config.requireEmail());
        assertEquals(5000, config.maxLength());
        assertTrue(config.allowAttachments());
    }

    @Test
    @DisplayName("SurveyConfig record works correctly")
    void surveyConfigRecordWorksCorrectly() {
        List<FeedbackTypes.SurveyQuestion> questions = List.of(
            FeedbackTypes.SurveyQuestion.text("q1", "Question 1")
        );

        FeedbackTypes.SurveyConfig config = new FeedbackTypes.SurveyConfig(
            "Custom Survey",
            "Description",
            questions,
            true,
            true,
            1000,
            false
        );

        assertEquals("Custom Survey", config.title());
        assertEquals(1, config.questions().size());
        assertTrue(config.anonymous());
        assertTrue(config.requireEmail());
        assertFalse(config.allowAttachments());
    }

    @Test
    @DisplayName("SurveyQuestion text factory method works correctly")
    void surveyQuestionTextFactoryMethodWorksCorrectly() {
        FeedbackTypes.SurveyQuestion question = FeedbackTypes.SurveyQuestion.text("q1", "Your name?");

        assertEquals("q1", question.id());
        assertEquals("Your name?", question.question());
        assertEquals(FeedbackTypes.QuestionType.TEXT, question.type());
        assertFalse(question.required());
    }

    @Test
    @DisplayName("SurveyQuestion rating factory method works correctly")
    void surveyQuestionRatingFactoryMethodWorksCorrectly() {
        FeedbackTypes.SurveyQuestion question = FeedbackTypes.SurveyQuestion.rating("rating", "Rate us");

        assertEquals("rating", question.id());
        assertEquals(FeedbackTypes.QuestionType.RATING, question.type());
        assertTrue(question.required());
    }

    @Test
    @DisplayName("SurveyQuestion multipleChoice factory method works correctly")
    void surveyQuestionMultipleChoiceFactoryMethodWorksCorrectly() {
        List<String> options = List.of("Option A", "Option B", "Option C");

        FeedbackTypes.SurveyQuestion question = FeedbackTypes.SurveyQuestion.multipleChoice("choice", "Pick one", options);

        assertEquals("choice", question.id());
        assertEquals(FeedbackTypes.QuestionType.MULTIPLE_CHOICE, question.type());
        assertEquals(3, question.options().size());
        assertTrue(question.required());
    }

    @Test
    @DisplayName("QuestionType enum has correct values")
    void questionTypeEnumHasCorrectValues() {
        FeedbackTypes.QuestionType[] types = FeedbackTypes.QuestionType.values();

        assertEquals(6, types.length);
        assertTrue(Arrays.asList(types).contains(FeedbackTypes.QuestionType.TEXT));
        assertTrue(Arrays.asList(types).contains(FeedbackTypes.QuestionType.TEXTAREA));
        assertTrue(Arrays.asList(types).contains(FeedbackTypes.QuestionType.RATING));
        assertTrue(Arrays.asList(types).contains(FeedbackTypes.QuestionType.MULTIPLE_CHOICE));
        assertTrue(Arrays.asList(types).contains(FeedbackTypes.QuestionType.CHECKBOX));
        assertTrue(Arrays.asList(types).contains(FeedbackTypes.QuestionType.BOOLEAN));
    }

    @Test
    @DisplayName("SurveyResponse record works correctly")
    void surveyResponseRecordWorksCorrectly() {
        Map<String, Object> answers = new HashMap<>();
        answers.put("q1", "Answer 1");
        answers.put("rating", 5);

        FeedbackTypes.SurveyResponse response = new FeedbackTypes.SurveyResponse(
            "survey-123",
            answers,
            Instant.now()
        );

        assertEquals("survey-123", response.surveyId());
        assertEquals("Answer 1", response.getAnswer("q1"));
        assertEquals("Answer 1", response.getAnswerText("q1"));
        assertEquals(5, response.getRating("rating"));
    }

    @Test
    @DisplayName("SurveyResponse getAnswer returns null for missing key")
    void surveyResponseGetAnswerReturnsNullForMissingKey() {
        FeedbackTypes.SurveyResponse response = new FeedbackTypes.SurveyResponse(
            "survey-1",
            Map.of(),
            Instant.now()
        );

        assertNull(response.getAnswer("nonexistent"));
    }

    @Test
    @DisplayName("SurveyResponse getRating returns 0 for non-number")
    void surveyResponseGetRatingReturnsZeroForNonNumber() {
        Map<String, Object> answers = new HashMap<>();
        answers.put("text", "not a number");

        FeedbackTypes.SurveyResponse response = new FeedbackTypes.SurveyResponse(
            "survey-1",
            answers,
            Instant.now()
        );

        assertEquals(0, response.getRating("text"));
        assertEquals(0, response.getRating("missing"));
    }
}