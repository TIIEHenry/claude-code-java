/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code components/wizard
 */
package com.anthropic.claudecode.components.wizard;

import java.util.*;

/**
 * Wizard types - Type definitions for wizard system.
 */
public final class WizardTypes {

    /**
     * Wizard step record.
     */
    public record WizardStep<T>(
        String id,
        String title,
        String description,
        StepStatus status,
        T data
    ) {
        public WizardStep(String id, String title) {
            this(id, title, null, StepStatus.PENDING, null);
        }
    }

    /**
     * Step status enum.
     */
    public enum StepStatus {
        PENDING,
        CURRENT,
        COMPLETED,
        SKIPPED,
        ERROR
    }

    /**
     * Wizard state record.
     */
    public record WizardState<T>(
        List<WizardStep<T>> steps,
        int currentStepIndex,
        boolean isComplete,
        boolean isCancelled
    ) {
        public WizardStep<T> getCurrentStep() {
            if (currentStepIndex >= 0 && currentStepIndex < steps.size()) {
                return steps.get(currentStepIndex);
            }
            return null;
        }

        public boolean hasNextStep() {
            return currentStepIndex < steps.size() - 1;
        }

        public boolean hasPreviousStep() {
            return currentStepIndex > 0;
        }
    }

    /**
     * Wizard result record.
     */
    public record WizardResult<T>(
        boolean success,
        Map<String, T> stepData,
        String error
    ) {
        public static <T> WizardResult<T> success(Map<String, T> data) {
            return new WizardResult<>(true, data, null);
        }

        public static <T> WizardResult<T> failure(String error) {
            return new WizardResult<>(false, null, error);
        }
    }

    /**
     * Wizard config record.
     */
    public record WizardConfig(
        String title,
        boolean allowSkip,
        boolean allowBack,
        boolean showProgress
    ) {
        public WizardConfig(String title) {
            this(title, true, true, true);
        }
    }
}