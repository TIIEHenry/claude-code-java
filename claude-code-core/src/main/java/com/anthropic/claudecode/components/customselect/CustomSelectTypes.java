/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code components/CustomSelect
 */
package com.anthropic.claudecode.components.customselect;

import java.util.*;
import java.util.function.*;

/**
 * Custom select - Custom select component types.
 */
public final class CustomSelectTypes {

    /**
     * Select option record.
     */
    public record SelectOption<T>(
        String id,
        String label,
        String description,
        T value,
        boolean isDisabled,
        boolean isSelected,
        String icon
    ) {
        public SelectOption(String id, String label, T value) {
            this(id, label, null, value, false, false, null);
        }

        public SelectOption withSelected(boolean selected) {
            return new SelectOption(id, label, description, value, isDisabled, selected, icon);
        }

        public SelectOption withDescription(String desc) {
            return new SelectOption(id, label, desc, value, isDisabled, isSelected, icon);
        }

        public SelectOption withIcon(String icon) {
            return new SelectOption(id, label, description, value, isDisabled, isSelected, icon);
        }

        public SelectOption disabled() {
            return new SelectOption(id, label, description, value, true, isSelected, icon);
        }
    }

    /**
     * Select state record.
     */
    public record SelectState<T>(
        List<SelectOption<T>> options,
        int selectedIndex,
        int focusedIndex,
        boolean isOpen,
        String searchText,
        SelectMode mode,
        Set<String> selectedIds
    ) {
        public static <T> SelectState<T> create(List<SelectOption<T>> options) {
            return new SelectState<T>(
                options,
                -1,
                0,
                false,
                "",
                SelectMode.SINGLE,
                Collections.emptySet()
            );
        }

        public SelectOption<T> getFocusedOption() {
            if (focusedIndex >= 0 && focusedIndex < options.size()) {
                return options.get(focusedIndex);
            }
            return null;
        }

        public SelectOption<T> getSelectedOption() {
            if (selectedIndex >= 0 && selectedIndex < options.size()) {
                return options.get(selectedIndex);
            }
            return null;
        }

        public List<SelectOption<T>> getSelectedOptions() {
            return options.stream()
                .filter(o -> selectedIds.contains(o.id()))
                .toList();
        }

        public List<SelectOption<T>> getFilteredOptions() {
            if (searchText.isEmpty()) {
                return options;
            }
            String lower = searchText.toLowerCase();
            return options.stream()
                .filter(o -> !o.isDisabled())
                .filter(o -> o.label().toLowerCase().contains(lower) ||
                            (o.description() != null && o.description().toLowerCase().contains(lower)))
                .toList();
        }

        public SelectState<T> withFocusedIndex(int index) {
            return new SelectState<T>(options, selectedIndex, index, isOpen, searchText, mode, selectedIds);
        }

        public SelectState<T> withOpen(boolean open) {
            return new SelectState<T>(options, selectedIndex, focusedIndex, open, searchText, mode, selectedIds);
        }

        public SelectState<T> withSearchText(String text) {
            return new SelectState<T>(options, selectedIndex, focusedIndex, isOpen, text, mode, selectedIds);
        }

        public SelectState<T> toggleSelection(String id) {
            Set<String> newSelected = new HashSet<>(selectedIds);
            if (newSelected.contains(id)) {
                newSelected.remove(id);
            } else {
                newSelected.add(id);
            }
            return new SelectState<T>(options, selectedIndex, focusedIndex, isOpen, searchText, mode, newSelected);
        }
    }

    /**
     * Select mode enum.
     */
    public enum SelectMode {
        SINGLE,
        MULTI,
        SEARCHABLE,
        SEARCHABLE_MULTI
    }

    /**
     * Select config record.
     */
    public record SelectConfig(
        String placeholder,
        boolean isSearchable,
        boolean clearable,
        boolean disabled,
        int maxHeight,
        String emptyMessage,
        Function<SelectOption<?>, String> formatter
    ) {
        public static SelectConfig defaultConfig() {
            return new SelectConfig(
                "Select an option...",
                false,
                false,
                false,
                10,
                "No options available",
                o -> o.label()
            );
        }

        public SelectConfig searchable() {
            return new SelectConfig(placeholder, true, clearable, disabled, maxHeight, emptyMessage, formatter);
        }

        public SelectConfig multi() {
            return new SelectConfig(placeholder, isSearchable, clearable, disabled, maxHeight, emptyMessage, formatter);
        }
    }

    /**
     * Select action enum.
     */
    public enum SelectAction {
        OPEN,
        CLOSE,
        UP,
        DOWN,
        SELECT,
        CLEAR,
        SEARCH,
        TOGGLE
    }
}