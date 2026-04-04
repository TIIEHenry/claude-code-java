/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.components.customselect;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Tests for CustomSelectTypes.
 */
@DisplayName("CustomSelectTypes Tests")
class CustomSelectTypesTest {

    @Test
    @DisplayName("SelectOption record works correctly")
    void selectOptionRecordWorksCorrectly() {
        CustomSelectTypes.SelectOption<String> option = new CustomSelectTypes.SelectOption<>(
            "opt1",
            "Option 1",
            "Description",
            "value1",
            false,
            true,
            "icon"
        );

        assertEquals("opt1", option.id());
        assertEquals("Option 1", option.label());
        assertEquals("Description", option.description());
        assertEquals("value1", option.value());
        assertFalse(option.isDisabled());
        assertTrue(option.isSelected());
        assertEquals("icon", option.icon());
    }

    @Test
    @DisplayName("SelectOption simple constructor works correctly")
    void selectOptionSimpleConstructorWorksCorrectly() {
        CustomSelectTypes.SelectOption<Integer> option = new CustomSelectTypes.SelectOption<>(
            "id1", "Label", 42
        );

        assertEquals("id1", option.id());
        assertEquals("Label", option.label());
        assertEquals(42, option.value());
        assertNull(option.description());
        assertFalse(option.isDisabled());
        assertFalse(option.isSelected());
        assertNull(option.icon());
    }

    @Test
    @DisplayName("SelectOption withSelected works correctly")
    void selectOptionWithSelectedWorksCorrectly() {
        CustomSelectTypes.SelectOption<String> option = new CustomSelectTypes.SelectOption<>(
            "id", "Label", "value"
        );

        CustomSelectTypes.SelectOption<String> selected = option.withSelected(true);

        assertTrue(selected.isSelected());
        assertFalse(option.isSelected());
    }

    @Test
    @DisplayName("SelectOption withDescription works correctly")
    void selectOptionWithDescriptionWorksCorrectly() {
        CustomSelectTypes.SelectOption<String> option = new CustomSelectTypes.SelectOption<>(
            "id", "Label", "value"
        );

        CustomSelectTypes.SelectOption<String> withDesc = option.withDescription("New desc");

        assertEquals("New desc", withDesc.description());
    }

    @Test
    @DisplayName("SelectOption withIcon works correctly")
    void selectOptionWithIconWorksCorrectly() {
        CustomSelectTypes.SelectOption<String> option = new CustomSelectTypes.SelectOption<>(
            "id", "Label", "value"
        );

        CustomSelectTypes.SelectOption<String> withIcon = option.withIcon("⭐");

        assertEquals("⭐", withIcon.icon());
    }

    @Test
    @DisplayName("SelectOption disabled works correctly")
    void selectOptionDisabledWorksCorrectly() {
        CustomSelectTypes.SelectOption<String> option = new CustomSelectTypes.SelectOption<>(
            "id", "Label", "value"
        );

        CustomSelectTypes.SelectOption<String> disabled = option.disabled();

        assertTrue(disabled.isDisabled());
    }

    @Test
    @DisplayName("SelectState create returns valid state")
    void selectStateCreateReturnsValidState() {
        List<CustomSelectTypes.SelectOption<String>> options = List.of(
            new CustomSelectTypes.SelectOption<>("1", "One", "v1"),
            new CustomSelectTypes.SelectOption<>("2", "Two", "v2")
        );

        CustomSelectTypes.SelectState<String> state = CustomSelectTypes.SelectState.create(options);

        assertNotNull(state);
        assertEquals(2, state.options().size());
        assertEquals(-1, state.selectedIndex());
        assertEquals(0, state.focusedIndex());
        assertFalse(state.isOpen());
        assertEquals("", state.searchText());
        assertEquals(CustomSelectTypes.SelectMode.SINGLE, state.mode());
        assertTrue(state.selectedIds().isEmpty());
    }

    @Test
    @DisplayName("SelectState getFocusedOption works correctly")
    void selectStateGetFocusedOptionWorksCorrectly() {
        List<CustomSelectTypes.SelectOption<String>> options = List.of(
            new CustomSelectTypes.SelectOption<>("1", "One", "v1"),
            new CustomSelectTypes.SelectOption<>("2", "Two", "v2")
        );

        CustomSelectTypes.SelectState<String> state = new CustomSelectTypes.SelectState<>(
            options, -1, 1, false, "", CustomSelectTypes.SelectMode.SINGLE, Set.of()
        );

        CustomSelectTypes.SelectOption<String> focused = state.getFocusedOption();

        assertNotNull(focused);
        assertEquals("2", focused.id());
    }

    @Test
    @DisplayName("SelectState getFocusedOption returns null for invalid index")
    void selectStateGetFocusedOptionReturnsNullForInvalidIndex() {
        List<CustomSelectTypes.SelectOption<String>> options = List.of();

        CustomSelectTypes.SelectState<String> state = new CustomSelectTypes.SelectState<>(
            options, -1, 0, false, "", CustomSelectTypes.SelectMode.SINGLE, Set.of()
        );

        assertNull(state.getFocusedOption());
    }

    @Test
    @DisplayName("SelectState getSelectedOption works correctly")
    void selectStateGetSelectedOptionWorksCorrectly() {
        List<CustomSelectTypes.SelectOption<String>> options = List.of(
            new CustomSelectTypes.SelectOption<>("1", "One", "v1"),
            new CustomSelectTypes.SelectOption<>("2", "Two", "v2")
        );

        CustomSelectTypes.SelectState<String> state = new CustomSelectTypes.SelectState<>(
            options, 1, 0, false, "", CustomSelectTypes.SelectMode.SINGLE, Set.of()
        );

        CustomSelectTypes.SelectOption<String> selected = state.getSelectedOption();

        assertNotNull(selected);
        assertEquals("2", selected.id());
    }

    @Test
    @DisplayName("SelectState getSelectedOptions works correctly")
    void selectStateGetSelectedOptionsWorksCorrectly() {
        List<CustomSelectTypes.SelectOption<String>> options = List.of(
            new CustomSelectTypes.SelectOption<>("1", "One", "v1"),
            new CustomSelectTypes.SelectOption<>("2", "Two", "v2"),
            new CustomSelectTypes.SelectOption<>("3", "Three", "v3")
        );

        CustomSelectTypes.SelectState<String> state = new CustomSelectTypes.SelectState<>(
            options, -1, 0, false, "", CustomSelectTypes.SelectMode.MULTI, Set.of("1", "3")
        );

        List<CustomSelectTypes.SelectOption<String>> selected = state.getSelectedOptions();

        assertEquals(2, selected.size());
    }

    @Test
    @DisplayName("SelectState getFilteredOptions filters by search text")
    void selectStateGetFilteredOptionsFiltersBySearchText() {
        List<CustomSelectTypes.SelectOption<String>> options = List.of(
            new CustomSelectTypes.SelectOption<>("1", "Apple", "v1"),
            new CustomSelectTypes.SelectOption<>("2", "Banana", "v2"),
            new CustomSelectTypes.SelectOption<>("3", "Apricot", "v3")
        );

        CustomSelectTypes.SelectState<String> state = new CustomSelectTypes.SelectState<>(
            options, -1, 0, false, "ap", CustomSelectTypes.SelectMode.SEARCHABLE, Set.of()
        );

        List<CustomSelectTypes.SelectOption<String>> filtered = state.getFilteredOptions();

        assertEquals(2, filtered.size());
    }

    @Test
    @DisplayName("SelectState withFocusedIndex works correctly")
    void selectStateWithFocusedIndexWorksCorrectly() {
        List<CustomSelectTypes.SelectOption<String>> options = List.of(
            new CustomSelectTypes.SelectOption<>("1", "One", "v1")
        );

        CustomSelectTypes.SelectState<String> state = CustomSelectTypes.SelectState.create(options);

        CustomSelectTypes.SelectState<String> newState = state.withFocusedIndex(5);

        assertEquals(5, newState.focusedIndex());
        assertEquals(0, state.focusedIndex());
    }

    @Test
    @DisplayName("SelectState withOpen works correctly")
    void selectStateWithOpenWorksCorrectly() {
        List<CustomSelectTypes.SelectOption<String>> options = List.of();

        CustomSelectTypes.SelectState<String> state = CustomSelectTypes.SelectState.create(options);

        CustomSelectTypes.SelectState<String> openState = state.withOpen(true);

        assertTrue(openState.isOpen());
    }

    @Test
    @DisplayName("SelectState withSearchText works correctly")
    void selectStateWithSearchTextWorksCorrectly() {
        List<CustomSelectTypes.SelectOption<String>> options = List.of();

        CustomSelectTypes.SelectState<String> state = CustomSelectTypes.SelectState.create(options);

        CustomSelectTypes.SelectState<String> searchState = state.withSearchText("test");

        assertEquals("test", searchState.searchText());
    }

    @Test
    @DisplayName("SelectState toggleSelection works correctly")
    void selectStateToggleSelectionWorksCorrectly() {
        List<CustomSelectTypes.SelectOption<String>> options = List.of(
            new CustomSelectTypes.SelectOption<>("1", "One", "v1")
        );

        CustomSelectTypes.SelectState<String> state = CustomSelectTypes.SelectState.create(options);

        CustomSelectTypes.SelectState<String> selected = state.toggleSelection("1");
        assertTrue(selected.selectedIds().contains("1"));

        CustomSelectTypes.SelectState<String> unselected = selected.toggleSelection("1");
        assertFalse(unselected.selectedIds().contains("1"));
    }

    @Test
    @DisplayName("SelectMode enum has correct values")
    void selectModeEnumHasCorrectValues() {
        CustomSelectTypes.SelectMode[] modes = CustomSelectTypes.SelectMode.values();

        assertEquals(4, modes.length);
        assertTrue(Arrays.asList(modes).contains(CustomSelectTypes.SelectMode.SINGLE));
        assertTrue(Arrays.asList(modes).contains(CustomSelectTypes.SelectMode.MULTI));
        assertTrue(Arrays.asList(modes).contains(CustomSelectTypes.SelectMode.SEARCHABLE));
        assertTrue(Arrays.asList(modes).contains(CustomSelectTypes.SelectMode.SEARCHABLE_MULTI));
    }

    @Test
    @DisplayName("SelectConfig defaultConfig returns valid config")
    void selectConfigDefaultConfigReturnsValidConfig() {
        CustomSelectTypes.SelectConfig config = CustomSelectTypes.SelectConfig.defaultConfig();

        assertNotNull(config);
        assertEquals("Select an option...", config.placeholder());
        assertFalse(config.isSearchable());
        assertFalse(config.clearable());
        assertFalse(config.disabled());
        assertEquals(10, config.maxHeight());
        assertEquals("No options available", config.emptyMessage());
    }

    @Test
    @DisplayName("SelectConfig searchable returns searchable config")
    void selectConfigSearchableReturnsSearchableConfig() {
        CustomSelectTypes.SelectConfig config = CustomSelectTypes.SelectConfig.defaultConfig().searchable();

        assertTrue(config.isSearchable());
    }

    @Test
    @DisplayName("SelectAction enum has correct values")
    void selectActionEnumHasCorrectValues() {
        CustomSelectTypes.SelectAction[] actions = CustomSelectTypes.SelectAction.values();

        assertEquals(8, actions.length);
        assertTrue(Arrays.asList(actions).contains(CustomSelectTypes.SelectAction.OPEN));
        assertTrue(Arrays.asList(actions).contains(CustomSelectTypes.SelectAction.CLOSE));
        assertTrue(Arrays.asList(actions).contains(CustomSelectTypes.SelectAction.UP));
        assertTrue(Arrays.asList(actions).contains(CustomSelectTypes.SelectAction.DOWN));
        assertTrue(Arrays.asList(actions).contains(CustomSelectTypes.SelectAction.SELECT));
        assertTrue(Arrays.asList(actions).contains(CustomSelectTypes.SelectAction.CLEAR));
        assertTrue(Arrays.asList(actions).contains(CustomSelectTypes.SelectAction.SEARCH));
        assertTrue(Arrays.asList(actions).contains(CustomSelectTypes.SelectAction.TOGGLE));
    }
}