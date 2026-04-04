/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.tools;

import com.anthropic.claudecode.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AskUserQuestionTool.
 */
class AskUserQuestionToolTest {

    @Test
    @DisplayName("AskUserQuestionTool has correct name")
    void nameWorks() {
        AskUserQuestionTool tool = new AskUserQuestionTool();
        assertEquals("AskUserQuestion", tool.name());
    }

    @Test
    @DisplayName("AskUserQuestionTool input schema is valid")
    void inputSchemaWorks() {
        AskUserQuestionTool tool = new AskUserQuestionTool();
        var schema = tool.inputSchema();
        assertNotNull(schema);
        assertEquals("object", schema.get("type"));
    }

    @Test
    @DisplayName("AskUserQuestionTool is read-only")
    void isReadOnlyWorks() {
        AskUserQuestionTool tool = new AskUserQuestionTool();
        AskUserQuestionTool.Input input = new AskUserQuestionTool.Input(
            List.of(),
            null
        );
        assertTrue(tool.isReadOnly(input));
    }

    @Test
    @DisplayName("AskUserQuestionTool matches name correctly")
    void matchesNameWorks() {
        AskUserQuestionTool tool = new AskUserQuestionTool();
        assertTrue(tool.matchesName("AskUserQuestion"));
        assertFalse(tool.matchesName("Bash"));
    }

    @Test
    @DisplayName("AskUserQuestionTool requiresUserInteraction returns true")
    void requiresUserInteractionWorks() {
        AskUserQuestionTool tool = new AskUserQuestionTool();
        assertTrue(tool.requiresUserInteraction());
    }

    @Test
    @DisplayName("AskUserQuestionTool Input record works")
    void inputRecordWorks() {
        AskUserQuestionTool.Input input = new AskUserQuestionTool.Input(
            List.of(),
            Map.of("key", "value")
        );

        assertNotNull(input.questions());
        assertEquals(1, input.metadata().size());
    }

    @Test
    @DisplayName("AskUserQuestionTool Question record works")
    void questionWorks() {
        var option = new AskUserQuestionTool.QuestionOption("Yes", "Confirm");
        var question = new AskUserQuestionTool.Question(
            "Header",
            "Which option?",
            false,
            List.of(option)
        );

        assertEquals("Header", question.header());
        assertEquals("Which option?", question.question());
        assertFalse(question.multiSelect());
        assertEquals(1, question.options().size());
    }

    @Test
    @DisplayName("AskUserQuestionTool QuestionOption record works")
    void questionOptionWorks() {
        var option = new AskUserQuestionTool.QuestionOption(
            "Yes",
            "Confirm the action",
            null
        );

        assertEquals("Yes", option.label());
        assertEquals("Confirm the action", option.description());
    }

    @Test
    @DisplayName("AskUserQuestionTool QuestionOption convenience constructor")
    void questionOptionConvenienceWorks() {
        var option = new AskUserQuestionTool.QuestionOption("No", "Cancel");

        assertEquals("No", option.label());
        assertEquals("Cancel", option.description());
        assertNull(option.preview());
    }
}