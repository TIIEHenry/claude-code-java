/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.components.wizard;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Tests for WizardTypes.
 */
@DisplayName("WizardTypes Tests")
class WizardTypesTest {

    @Test
    @DisplayName("WizardStep record works correctly")
    void wizardStepRecordWorksCorrectly() {
        WizardTypes.WizardStep<String> step = new WizardTypes.WizardStep<>(
            "step1",
            "First Step",
            "Description",
            WizardTypes.StepStatus.PENDING,
            "data"
        );

        assertEquals("step1", step.id());
        assertEquals("First Step", step.title());
        assertEquals("Description", step.description());
        assertEquals(WizardTypes.StepStatus.PENDING, step.status());
        assertEquals("data", step.data());
    }

    @Test
    @DisplayName("WizardStep simple constructor works correctly")
    void wizardStepSimpleConstructorWorksCorrectly() {
        WizardTypes.WizardStep<String> step = new WizardTypes.WizardStep<>("step1", "Title");

        assertEquals("step1", step.id());
        assertEquals("Title", step.title());
        assertNull(step.description());
        assertEquals(WizardTypes.StepStatus.PENDING, step.status());
        assertNull(step.data());
    }

    @Test
    @DisplayName("StepStatus enum has correct values")
    void stepStatusEnumHasCorrectValues() {
        WizardTypes.StepStatus[] statuses = WizardTypes.StepStatus.values();

        assertEquals(5, statuses.length);
        assertTrue(Arrays.asList(statuses).contains(WizardTypes.StepStatus.PENDING));
        assertTrue(Arrays.asList(statuses).contains(WizardTypes.StepStatus.CURRENT));
        assertTrue(Arrays.asList(statuses).contains(WizardTypes.StepStatus.COMPLETED));
        assertTrue(Arrays.asList(statuses).contains(WizardTypes.StepStatus.SKIPPED));
        assertTrue(Arrays.asList(statuses).contains(WizardTypes.StepStatus.ERROR));
    }

    @Test
    @DisplayName("WizardState getCurrentStep works correctly")
    void wizardStateGetCurrentStepWorksCorrectly() {
        List<WizardTypes.WizardStep<String>> steps = List.of(
            new WizardTypes.WizardStep<>("s1", "Step 1"),
            new WizardTypes.WizardStep<>("s2", "Step 2"),
            new WizardTypes.WizardStep<>("s3", "Step 3")
        );

        WizardTypes.WizardState<String> state = new WizardTypes.WizardState<>(
            steps, 1, false, false
        );

        WizardTypes.WizardStep<String> current = state.getCurrentStep();
        assertNotNull(current);
        assertEquals("s2", current.id());
    }

    @Test
    @DisplayName("WizardState getCurrentStep returns null for invalid index")
    void wizardStateGetCurrentStepReturnsNullForInvalidIndex() {
        List<WizardTypes.WizardStep<String>> steps = List.of();

        WizardTypes.WizardState<String> state = new WizardTypes.WizardState<>(
            steps, 0, false, false
        );

        assertNull(state.getCurrentStep());
    }

    @Test
    @DisplayName("WizardState hasNextStep works correctly")
    void wizardStateHasNextStepWorksCorrectly() {
        List<WizardTypes.WizardStep<String>> steps = List.of(
            new WizardTypes.WizardStep<>("s1", "Step 1"),
            new WizardTypes.WizardStep<>("s2", "Step 2")
        );

        WizardTypes.WizardState<String> state1 = new WizardTypes.WizardState<>(steps, 0, false, false);
        WizardTypes.WizardState<String> state2 = new WizardTypes.WizardState<>(steps, 1, false, false);

        assertTrue(state1.hasNextStep());
        assertFalse(state2.hasNextStep());
    }

    @Test
    @DisplayName("WizardState hasPreviousStep works correctly")
    void wizardStateHasPreviousStepWorksCorrectly() {
        List<WizardTypes.WizardStep<String>> steps = List.of(
            new WizardTypes.WizardStep<>("s1", "Step 1"),
            new WizardTypes.WizardStep<>("s2", "Step 2")
        );

        WizardTypes.WizardState<String> state1 = new WizardTypes.WizardState<>(steps, 0, false, false);
        WizardTypes.WizardState<String> state2 = new WizardTypes.WizardState<>(steps, 1, false, false);

        assertFalse(state1.hasPreviousStep());
        assertTrue(state2.hasPreviousStep());
    }

    @Test
    @DisplayName("WizardResult success factory works correctly")
    void wizardResultSuccessFactoryWorksCorrectly() {
        Map<String, String> data = new HashMap<>();
        data.put("step1", "result1");

        WizardTypes.WizardResult<String> result = WizardTypes.WizardResult.success(data);

        assertTrue(result.success());
        assertEquals(1, result.stepData().size());
        assertNull(result.error());
    }

    @Test
    @DisplayName("WizardResult failure factory works correctly")
    void wizardResultFailureFactoryWorksCorrectly() {
        WizardTypes.WizardResult<String> result = WizardTypes.WizardResult.failure("Something went wrong");

        assertFalse(result.success());
        assertNull(result.stepData());
        assertEquals("Something went wrong", result.error());
    }

    @Test
    @DisplayName("WizardConfig simple constructor works correctly")
    void wizardConfigSimpleConstructorWorksCorrectly() {
        WizardTypes.WizardConfig config = new WizardTypes.WizardConfig("My Wizard");

        assertEquals("My Wizard", config.title());
        assertTrue(config.allowSkip());
        assertTrue(config.allowBack());
        assertTrue(config.showProgress());
    }

    @Test
    @DisplayName("WizardConfig full constructor works correctly")
    void wizardConfigFullConstructorWorksCorrectly() {
        WizardTypes.WizardConfig config = new WizardTypes.WizardConfig(
            "Custom Wizard", false, false, false
        );

        assertEquals("Custom Wizard", config.title());
        assertFalse(config.allowSkip());
        assertFalse(config.allowBack());
        assertFalse(config.showProgress());
    }
}