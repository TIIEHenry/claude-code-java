/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code components/CustomSelect/use-multi-select-state
 */
package com.anthropic.claudecode.components.customselect;

import java.util.*;

/**
 * Multi select state - State for multi-select component.
 */
public final class MultiSelectState<T> {
    private final List<CustomSelectTypes.SelectOption<T>> options;
    private final Set<String> selectedIds;
    private int focusedIndex;
    private String searchText;
    private boolean isOpen;

    /**
     * Create multi select state.
     */
    public MultiSelectState(List<CustomSelectTypes.SelectOption<T>> options) {
        this.options = new ArrayList<>(options);
        this.selectedIds = new HashSet<>();
        this.focusedIndex = 0;
        this.searchText = "";
        this.isOpen = false;
    }

    /**
     * Get options.
     */
    public List<CustomSelectTypes.SelectOption<T>> getOptions() {
        return Collections.unmodifiableList(options);
    }

    /**
     * Get filtered options.
     */
    public List<CustomSelectTypes.SelectOption<T>> getFilteredOptions() {
        if (searchText.isEmpty()) {
            return options;
        }
        String lower = searchText.toLowerCase();
        return options.stream()
            .filter(o -> !o.isDisabled())
            .filter(o -> o.label().toLowerCase().contains(lower))
            .toList();
    }

    /**
     * Get selected options.
     */
    public List<CustomSelectTypes.SelectOption<T>> getSelectedOptions() {
        return options.stream()
            .filter(o -> selectedIds.contains(o.id()))
            .toList();
    }

    /**
     * Get selected values.
     */
    public List<T> getSelectedValues() {
        return getSelectedOptions()
            .stream()
            .map(CustomSelectTypes.SelectOption::value)
            .toList();
    }

    /**
     * Get selected count.
     */
    public int getSelectedCount() {
        return selectedIds.size();
    }

    /**
     * Is selected.
     */
    public boolean isSelected(String id) {
        return selectedIds.contains(id);
    }

    /**
     * Toggle selection.
     */
    public void toggleSelection(String id) {
        if (selectedIds.contains(id)) {
            selectedIds.remove(id);
        } else {
            selectedIds.add(id);
        }
    }

    /**
     * Select all.
     */
    public void selectAll() {
        for (CustomSelectTypes.SelectOption<T> option : options) {
            if (!option.isDisabled()) {
                selectedIds.add(option.id());
            }
        }
    }

    /**
     * Clear all.
     */
    public void clearAll() {
        selectedIds.clear();
    }

    /**
     * Get focused index.
     */
    public int getFocusedIndex() {
        return focusedIndex;
    }

    /**
     * Set focused index.
     */
    public void setFocusedIndex(int index) {
        List<CustomSelectTypes.SelectOption<T>> filtered = getFilteredOptions();
        this.focusedIndex = Math.max(0, Math.min(index, filtered.size() - 1));
    }

    /**
     * Move focus.
     */
    public void moveFocus(int delta) {
        List<CustomSelectTypes.SelectOption<T>> filtered = getFilteredOptions();
        if (filtered.isEmpty()) return;

        int newIndex = focusedIndex + delta;
        focusedIndex = Math.max(0, Math.min(newIndex, filtered.size() - 1));
    }

    /**
     * Get focused option.
     */
    public CustomSelectTypes.SelectOption<T> getFocusedOption() {
        List<CustomSelectTypes.SelectOption<T>> filtered = getFilteredOptions();
        if (focusedIndex >= 0 && focusedIndex < filtered.size()) {
            return filtered.get(focusedIndex);
        }
        return null;
    }

    /**
     * Toggle focused option.
     */
    public void toggleFocused() {
        CustomSelectTypes.SelectOption<T> focused = getFocusedOption();
        if (focused != null && !focused.isDisabled()) {
            toggleSelection(focused.id());
        }
    }

    /**
     * Get search text.
     */
    public String getSearchText() {
        return searchText;
    }

    /**
     * Set search text.
     */
    public void setSearchText(String text) {
        this.searchText = text;
        this.focusedIndex = 0;
    }

    /**
     * Is open.
     */
    public boolean isOpen() {
        return isOpen;
    }

    /**
     * Set open.
     */
    public void setOpen(boolean open) {
        this.isOpen = open;
        if (!open) {
            this.searchText = "";
        }
    }

    /**
     * Toggle open.
     */
    public void toggleOpen() {
        setOpen(!isOpen);
    }

    /**
     * Multi select config record.
     */
    public record MultiSelectConfig(
        String placeholder,
        boolean searchable,
        int maxVisible,
        boolean showSelectAll,
        boolean showCount,
        String allSelectedText,
        String someSelectedText
    ) {
        public static MultiSelectConfig defaultConfig() {
            return new MultiSelectConfig(
                "Select options...",
                false,
                10,
                true,
                true,
                "All selected",
                "%d selected"
            );
        }
    }

    /**
     * Get display text.
     */
    public String getDisplayText(MultiSelectConfig config) {
        if (selectedIds.isEmpty()) {
            return config.placeholder();
        }
        if (selectedIds.size() == options.size()) {
            return config.allSelectedText();
        }
        return String.format(config.someSelectedText(), selectedIds.size());
    }
}