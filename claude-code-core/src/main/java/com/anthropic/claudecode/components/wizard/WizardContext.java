/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code components/wizard/useWizard
 */
package com.anthropic.claudecode.components.wizard;

import java.util.*;

/**
 * Wizard context - Context manager for wizard state.
 */
public final class WizardContext<T> {
    private final List<WizardTypes.WizardStep<T>> steps;
    private int currentStepIndex = 0;
    private boolean isComplete = false;
    private boolean isCancelled = false;
    private final Map<String, T> stepData = new HashMap<>();
    private final List<WizardListener<T>> listeners = new ArrayList<>();

    /**
     * Create wizard with steps.
     */
    public WizardContext(List<WizardTypes.WizardStep<T>> steps) {
        this.steps = new ArrayList<>(steps);
        updateStepStatuses();
    }

    /**
     * Get current step.
     */
    public WizardTypes.WizardStep<T> getCurrentStep() {
        if (currentStepIndex >= 0 && currentStepIndex < steps.size()) {
            return steps.get(currentStepIndex);
        }
        return null;
    }

    /**
     * Get current step index.
     */
    public int getCurrentStepIndex() {
        return currentStepIndex;
    }

    /**
     * Go to next step.
     */
    public boolean nextStep() {
        if (currentStepIndex < steps.size() - 1) {
            currentStepIndex++;
            updateStepStatuses();
            notifyListeners();
            return true;
        } else {
            isComplete = true;
            notifyListeners();
            return false;
        }
    }

    /**
     * Go to previous step.
     */
    public boolean previousStep() {
        if (currentStepIndex > 0) {
            currentStepIndex--;
            updateStepStatuses();
            notifyListeners();
            return true;
        }
        return false;
    }

    /**
     * Go to specific step.
     */
    public boolean goToStep(int index) {
        if (index >= 0 && index < steps.size()) {
            currentStepIndex = index;
            updateStepStatuses();
            notifyListeners();
            return true;
        }
        return false;
    }

    /**
     * Set step data.
     */
    public void setStepData(String stepId, T data) {
        stepData.put(stepId, data);
    }

    /**
     * Get step data.
     */
    public T getStepData(String stepId) {
        return stepData.get(stepId);
    }

    /**
     * Get all step data.
     */
    public Map<String, T> getAllStepData() {
        return Collections.unmodifiableMap(stepData);
    }

    /**
     * Skip current step.
     */
    public void skipStep() {
        WizardTypes.WizardStep<T> current = getCurrentStep();
        if (current != null) {
            steps.set(currentStepIndex, new WizardTypes.WizardStep<>(
                current.id(),
                current.title(),
                current.description(),
                WizardTypes.StepStatus.SKIPPED,
                null
            ));
            nextStep();
        }
    }

    /**
     * Mark current step as error.
     */
    public void errorStep(String error) {
        WizardTypes.WizardStep<T> current = getCurrentStep();
        if (current != null) {
            steps.set(currentStepIndex, new WizardTypes.WizardStep<>(
                current.id(),
                current.title(),
                current.description(),
                WizardTypes.StepStatus.ERROR,
                null
            ));
            notifyListeners();
        }
    }

    /**
     * Cancel wizard.
     */
    public void cancel() {
        isCancelled = true;
        notifyListeners();
    }

    /**
     * Check if complete.
     */
    public boolean isComplete() {
        return isComplete;
    }

    /**
     * Check if cancelled.
     */
    public boolean isCancelled() {
        return isCancelled;
    }

    /**
     * Get total steps.
     */
    public int getTotalSteps() {
        return steps.size();
    }

    /**
     * Get completed steps count.
     */
    public int getCompletedSteps() {
        return (int) steps.stream()
            .filter(s -> s.status() == WizardTypes.StepStatus.COMPLETED ||
                        s.status() == WizardTypes.StepStatus.SKIPPED)
            .count();
    }

    /**
     * Get progress percentage.
     */
    public double getProgress() {
        return (double) getCompletedSteps() / steps.size() * 100;
    }

    /**
     * Get wizard state.
     */
    public WizardTypes.WizardState<T> getState() {
        return new WizardTypes.WizardState<>(
            Collections.unmodifiableList(steps),
            currentStepIndex,
            isComplete,
            isCancelled
        );
    }

    /**
     * Add listener.
     */
    public void addListener(WizardListener<T> listener) {
        listeners.add(listener);
    }

    /**
     * Remove listener.
     */
    public void removeListener(WizardListener<T> listener) {
        listeners.remove(listener);
    }

    private void updateStepStatuses() {
        for (int i = 0; i < steps.size(); i++) {
            WizardTypes.WizardStep<T> step = steps.get(i);
            WizardTypes.StepStatus status;

            if (i < currentStepIndex) {
                status = WizardTypes.StepStatus.COMPLETED;
            } else if (i == currentStepIndex) {
                status = WizardTypes.StepStatus.CURRENT;
            } else {
                status = WizardTypes.StepStatus.PENDING;
            }

            steps.set(i, new WizardTypes.WizardStep<>(
                step.id(),
                step.title(),
                step.description(),
                status,
                step.data()
            ));
        }
    }

    private void notifyListeners() {
        for (WizardListener<T> listener : listeners) {
            listener.onWizardChanged(getState());
        }
    }

    /**
     * Wizard listener interface.
     */
    public interface WizardListener<T> {
        void onWizardChanged(WizardTypes.WizardState<T> state);
    }
}