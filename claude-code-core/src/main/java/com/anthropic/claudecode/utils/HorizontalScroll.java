/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code horizontal scroll utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Calculate the visible window of items that fit within available width,
 * ensuring the selected item is always visible. Uses edge-based scrolling.
 */
public final class HorizontalScroll {
    private HorizontalScroll() {}

    /**
     * Horizontal scroll window result.
     */
    public record HorizontalScrollWindow(
            int startIndex,
            int endIndex,
            boolean showLeftArrow,
            boolean showRightArrow
    ) {}

    /**
     * Calculate the visible window of items that fit within available width.
     *
     * @param itemWidths Array of item widths (each width should include separator)
     * @param availableWidth Total available width for items
     * @param arrowWidth Width of scroll indicator arrow (including space)
     * @param selectedIdx Index of selected item (must stay visible)
     * @param firstItemHasSeparator Whether first item's width includes a separator
     * @return Visible window bounds and whether to show scroll arrows
     */
    public static HorizontalScrollWindow calculateHorizontalScrollWindow(
            List<Integer> itemWidths,
            int availableWidth,
            int arrowWidth,
            int selectedIdx,
            boolean firstItemHasSeparator
    ) {
        int totalItems = itemWidths.size();

        if (totalItems == 0) {
            return new HorizontalScrollWindow(0, 0, false, false);
        }

        // Clamp selectedIdx to valid range
        int clampedSelected = Math.max(0, Math.min(selectedIdx, totalItems - 1));

        // If all items fit, show them all
        int totalWidth = itemWidths.stream().mapToInt(Integer::intValue).sum();
        if (totalWidth <= availableWidth) {
            return new HorizontalScrollWindow(0, totalItems, false, false);
        }

        // Calculate cumulative widths for efficient range calculations
        List<Integer> cumulativeWidths = new ArrayList<>();
        cumulativeWidths.add(0);
        for (int i = 0; i < totalItems; i++) {
            cumulativeWidths.add(cumulativeWidths.get(i) + itemWidths.get(i));
        }

        // Helper to get width of range [start, end)
        // When starting after index 0 and first item has separator baked in,
        // subtract 1 because we don't render leading separator on first visible item
        int[] cumulative = cumulativeWidths.stream().mapToInt(Integer::intValue).toArray();

        // Edge-based scrolling: Start from the beginning and only scroll when necessary
        int startIndex = 0;
        int endIndex = 1;

        // Expand from start as much as possible
        while (endIndex < totalItems &&
               rangeWidth(cumulative, startIndex, endIndex + 1, firstItemHasSeparator) <=
               getEffectiveWidth(startIndex, endIndex + 1, totalItems, availableWidth, arrowWidth)) {
            endIndex++;
        }

        // If selected is within visible range, we're done
        if (clampedSelected >= startIndex && clampedSelected < endIndex) {
            return new HorizontalScrollWindow(
                    startIndex, endIndex,
                    startIndex > 0, endIndex < totalItems
            );
        }

        // Selected is outside visible range - need to scroll
        if (clampedSelected >= endIndex) {
            // Selected is to the right - scroll so selected is at the right edge
            endIndex = clampedSelected + 1;
            startIndex = clampedSelected;

            // Expand left as much as possible (selected stays at right edge)
            while (startIndex > 0 &&
                   rangeWidth(cumulative, startIndex - 1, endIndex, firstItemHasSeparator) <=
                   getEffectiveWidth(startIndex - 1, endIndex, totalItems, availableWidth, arrowWidth)) {
                startIndex--;
            }
        } else {
            // Selected is to the left - scroll so selected is at the left edge
            startIndex = clampedSelected;
            endIndex = clampedSelected + 1;

            // Expand right as much as possible (selected stays at left edge)
            while (endIndex < totalItems &&
                   rangeWidth(cumulative, startIndex, endIndex + 1, firstItemHasSeparator) <=
                   getEffectiveWidth(startIndex, endIndex + 1, totalItems, availableWidth, arrowWidth)) {
                endIndex++;
            }
        }

        return new HorizontalScrollWindow(
                startIndex, endIndex,
                startIndex > 0, endIndex < totalItems
        );
    }

    /**
     * Get width of range [start, end) from cumulative widths.
     */
    private static int rangeWidth(int[] cumulative, int start, int end, boolean firstItemHasSeparator) {
        int baseWidth = cumulative[end] - cumulative[start];
        if (firstItemHasSeparator && start > 0) {
            return baseWidth - 1;
        }
        return baseWidth;
    }

    /**
     * Calculate effective available width based on whether we'll show arrows.
     */
    private static int getEffectiveWidth(int start, int end, int totalItems,
                                          int availableWidth, int arrowWidth) {
        int width = availableWidth;
        if (start > 0) width -= arrowWidth; // left arrow
        if (end < totalItems) width -= arrowWidth; // right arrow
        return width;
    }
}