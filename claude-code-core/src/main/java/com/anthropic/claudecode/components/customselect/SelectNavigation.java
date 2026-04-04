/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code components/CustomSelect/use-select-navigation
 */
package com.anthropic.claudecode.components.customselect;

import java.util.*;

/**
 * Select navigation - Keyboard navigation for select.
 */
public final class SelectNavigation {
    private static final Set<String> NAVIGATION_KEYS = Set.of(
        "up", "down", "enter", "escape", "tab", "home", "end", "pageup", "pagedown"
    );

    /**
     * Handle key press.
     */
    public static CustomSelectTypes.SelectAction handleKey(String key, boolean isOpen) {
        return switch (key.toLowerCase()) {
            case "up" -> isOpen ? CustomSelectTypes.SelectAction.UP : CustomSelectTypes.SelectAction.OPEN;
            case "down" -> isOpen ? CustomSelectTypes.SelectAction.DOWN : CustomSelectTypes.SelectAction.OPEN;
            case "enter", "space" -> isOpen ? CustomSelectTypes.SelectAction.SELECT : CustomSelectTypes.SelectAction.OPEN;
            case "escape" -> isOpen ? CustomSelectTypes.SelectAction.CLOSE : CustomSelectTypes.SelectAction.CLEAR;
            case "tab" -> CustomSelectTypes.SelectAction.CLOSE;
            case "home" -> isOpen ? CustomSelectTypes.SelectAction.UP : null; // Would move to first
            case "end" -> isOpen ? CustomSelectTypes.SelectAction.DOWN : null; // Would move to last
            default -> null;
        };
    }

    /**
     * Check if is navigation key.
     */
    public static boolean isNavigationKey(String key) {
        return NAVIGATION_KEYS.contains(key.toLowerCase());
    }

    /**
     * Navigation result record.
     */
    public record NavigationResult(
        CustomSelectTypes.SelectAction action,
        int newFocusIndex,
        boolean shouldClose
    ) {
        public static NavigationResult none() {
            return new NavigationResult(null, -1, false);
        }
    }

    /**
     * Calculate new focus index.
     */
    public static int calculateNewIndex(
        int currentIndex,
        int totalOptions,
        CustomSelectTypes.SelectAction action
    ) {
        if (totalOptions == 0) return -1;

        return switch (action) {
            case UP -> Math.max(0, currentIndex - 1);
            case DOWN -> Math.min(totalOptions - 1, currentIndex + 1);
            case SELECT, CLOSE, CLEAR -> currentIndex;
            default -> currentIndex;
        };
    }

    /**
     * Calculate page navigation.
     */
    public static int calculatePageMove(
        int currentIndex,
        int totalOptions,
        int pageSize,
        boolean up
    ) {
        if (totalOptions == 0) return -1;

        int delta = pageSize;
        int newIndex = currentIndex + (up ? -delta : delta);

        return Math.max(0, Math.min(newIndex, totalOptions - 1));
    }

    /**
     * Find next enabled option.
     */
    public static int findNextEnabled(
        List<CustomSelectTypes.SelectOption<?>> options,
        int startIndex,
        boolean forward
    ) {
        if (options.isEmpty()) return -1;

        int index = startIndex;
        int delta = forward ? 1 : -1;
        int attempts = 0;

        while (attempts < options.size()) {
            index = (index + delta + options.size()) % options.size();
            if (!options.get(index).isDisabled()) {
                return index;
            }
            attempts++;
        }

        return startIndex;
    }

    /**
     * Key binding config record.
     */
    public record KeyBindingConfig(
        String upKey,
        String downKey,
        String selectKey,
        String closeKey,
        String clearKey,
        String searchKey
    ) {
        public static KeyBindingConfig defaultConfig() {
            return new KeyBindingConfig(
                "up", "down", "enter", "escape", "delete", "/"
            );
        }
    }
}