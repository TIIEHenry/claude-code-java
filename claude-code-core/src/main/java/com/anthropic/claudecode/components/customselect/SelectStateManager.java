/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code components/CustomSelect/use-select-state
 */
package com.anthropic.claudecode.components.customselect;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.*;

/**
 * Select state manager - Manages select component state.
 */
public final class SelectStateManager<T> {
    private CustomSelectTypes.SelectState<T> state;
    private final List<SelectListener<T>> listeners = new CopyOnWriteArrayList<>();

    /**
     * Create state manager.
     */
    public SelectStateManager(List<CustomSelectTypes.SelectOption<T>> options) {
        this.state = CustomSelectTypes.SelectState.create(options);
    }

    /**
     * Create with config.
     */
    public SelectStateManager(
        List<CustomSelectTypes.SelectOption<T>> options,
        CustomSelectTypes.SelectConfig config
    ) {
        CustomSelectTypes.SelectMode mode = CustomSelectTypes.SelectMode.SINGLE;
        if (config.isSearchable()) {
            mode = CustomSelectTypes.SelectMode.SEARCHABLE;
        }

        this.state = new CustomSelectTypes.SelectState<T>(
            options,
            -1,
            0,
            false,
            "",
            mode,
            Collections.emptySet()
        );
    }

    /**
     * Get state.
     */
    public CustomSelectTypes.SelectState<T> getState() {
        return state;
    }

    /**
     * Handle action.
     */
    public void handleAction(CustomSelectTypes.SelectAction action) {
        state = switch (action) {
            case OPEN -> state.withOpen(true);
            case CLOSE -> state.withOpen(false).withSearchText("");
            case UP -> moveFocus(-1);
            case DOWN -> moveFocus(1);
            case SELECT -> selectFocused();
            case CLEAR -> clearSelection();
            case TOGGLE -> state.withOpen(!state.isOpen());
            default -> state;
        };

        notifyListeners();
    }

    /**
     * Move focus.
     */
    private CustomSelectTypes.SelectState<T> moveFocus(int delta) {
        List<CustomSelectTypes.SelectOption<T>> filtered = state.getFilteredOptions();
        if (filtered.isEmpty()) return state;

        int newIndex = state.focusedIndex() + delta;
        newIndex = Math.max(0, Math.min(newIndex, filtered.size() - 1));

        return state.withFocusedIndex(newIndex);
    }

    /**
     * Select focused option.
     */
    private CustomSelectTypes.SelectState<T> selectFocused() {
        CustomSelectTypes.SelectOption<T> focused = state.getFocusedOption();
        if (focused == null || focused.isDisabled()) return state;

        if (state.mode() == CustomSelectTypes.SelectMode.SINGLE ||
            state.mode() == CustomSelectTypes.SelectMode.SEARCHABLE) {
            return new CustomSelectTypes.SelectState<T>(
                state.options(),
                findOptionIndex(focused.id()),
                state.focusedIndex(),
                false,
                "",
                state.mode(),
                Set.of(focused.id())
            );
        }

        return state.toggleSelection(focused.id());
    }

    /**
     * Find option index.
     */
    private int findOptionIndex(String id) {
        for (int i = 0; i < state.options().size(); i++) {
            if (state.options().get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Clear selection.
     */
    private CustomSelectTypes.SelectState<T> clearSelection() {
        return new CustomSelectTypes.SelectState<T>(
            state.options(),
            -1,
            0,
            false,
            "",
            state.mode(),
            Collections.emptySet()
        );
    }

    /**
     * Set search text.
     */
    public void setSearchText(String text) {
        state = state.withSearchText(text);
        // Reset focus to first filtered option
        List<CustomSelectTypes.SelectOption<T>> filtered = state.getFilteredOptions();
        if (!filtered.isEmpty()) {
            state = state.withFocusedIndex(0);
        }
        notifyListeners();
    }

    /**
     * Set options.
     */
    public void setOptions(List<CustomSelectTypes.SelectOption<T>> options) {
        state = new CustomSelectTypes.SelectState<T>(
            options,
            state.selectedIndex(),
            state.focusedIndex(),
            state.isOpen(),
            state.searchText(),
            state.mode(),
            state.selectedIds()
        );
        notifyListeners();
    }

    /**
     * Add listener.
     */
    public void addListener(SelectListener<T> listener) {
        listeners.add(listener);
    }

    /**
     * Remove listener.
     */
    public void removeListener(SelectListener<T> listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (SelectListener<T> listener : listeners) {
            listener.onStateChange(state);
        }
    }

    /**
     * Select listener interface.
     */
    public interface SelectListener<T> {
        void onStateChange(CustomSelectTypes.SelectState<T> state);
    }

    /**
     * Get selected values.
     */
    public List<T> getSelectedValues() {
        return state.getSelectedOptions()
            .stream()
            .map(CustomSelectTypes.SelectOption::value)
            .toList();
    }

    /**
     * Get first selected value.
     */
    public Optional<T> getSelectedValue() {
        CustomSelectTypes.SelectOption<T> selected = state.getSelectedOption();
        return selected != null ? Optional.of(selected.value()) : Optional.empty();
    }
}