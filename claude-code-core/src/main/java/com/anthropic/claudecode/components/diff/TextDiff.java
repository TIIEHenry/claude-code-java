/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code diff utilities
 */
package com.anthropic.claudecode.components.diff;

import java.util.*;

/**
 * Text diff utility for comparing text content.
 */
public final class TextDiff {

    /**
     * Diff item representing a change.
     */
    public record DiffItem(
        DiffType type,
        String content,
        int oldLine,
        int newLine
    ) {}

    /**
     * Diff type enum.
     */
    public enum DiffType {
        ADD,
        REMOVE,
        CONTEXT
    }

    /**
     * Diff statistics.
     */
    public record DiffStats(
        int added,
        int removed,
        int unchanged,
        boolean hasChanges
    ) {}

    /**
     * Compute diff between two strings.
     */
    public List<DiffItem> diff(String oldText, String newText) {
        List<DiffItem> result = new ArrayList<>();

        String[] oldLines = oldText.split("\n", -1);
        String[] newLines = newText.split("\n", -1);

        // Simple line-by-line diff algorithm
        int oldIdx = 0;
        int newIdx = 0;

        while (oldIdx < oldLines.length || newIdx < newLines.length) {
            if (oldIdx >= oldLines.length) {
                // Remaining new lines are additions
                result.add(new DiffItem(DiffType.ADD, newLines[newIdx], -1, newIdx + 1));
                newIdx++;
            } else if (newIdx >= newLines.length) {
                // Remaining old lines are removals
                result.add(new DiffItem(DiffType.REMOVE, oldLines[oldIdx], oldIdx + 1, -1));
                oldIdx++;
            } else if (oldLines[oldIdx].equals(newLines[newIdx])) {
                // Lines match - context
                result.add(new DiffItem(DiffType.CONTEXT, oldLines[oldIdx], oldIdx + 1, newIdx + 1));
                oldIdx++;
                newIdx++;
            } else {
                // Lines differ - check if it's an add or remove
                // Look ahead to find matches
                boolean foundInNew = findLine(oldLines[oldIdx], newLines, newIdx + 1);
                boolean foundInOld = findLine(newLines[newIdx], oldLines, oldIdx + 1);

                if (foundInNew && !foundInOld) {
                    // Old line exists later in new - new line is added
                    result.add(new DiffItem(DiffType.ADD, newLines[newIdx], -1, newIdx + 1));
                    newIdx++;
                } else if (foundInOld && !foundInNew) {
                    // New line exists later in old - old line is removed
                    result.add(new DiffItem(DiffType.REMOVE, oldLines[oldIdx], oldIdx + 1, -1));
                    oldIdx++;
                } else {
                    // Both lines are different - treat as change
                    result.add(new DiffItem(DiffType.REMOVE, oldLines[oldIdx], oldIdx + 1, -1));
                    result.add(new DiffItem(DiffType.ADD, newLines[newIdx], -1, newIdx + 1));
                    oldIdx++;
                    newIdx++;
                }
            }
        }

        return result;
    }

    private boolean findLine(String line, String[] lines, int startIdx) {
        for (int i = startIdx; i < lines.length; i++) {
            if (lines[i].equals(line)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Format diff as unified diff output.
     */
    public String formatUnified(List<DiffItem> items) {
        StringBuilder sb = new StringBuilder();

        int oldStart = 1;
        int newStart = 1;
        int oldCount = 0;
        int newCount = 0;

        // Calculate ranges
        for (DiffItem item : items) {
            if (item.type() == DiffType.REMOVE) {
                oldCount++;
            } else if (item.type() == DiffType.ADD) {
                newCount++;
            } else {
                oldCount++;
                newCount++;
            }
        }

        sb.append("--- original\n");
        sb.append("+++ modified\n");
        sb.append("@@ -" + oldStart + "," + oldCount + " +" + newStart + "," + newCount + " @@\n");

        for (DiffItem item : items) {
            switch (item.type()) {
                case ADD -> sb.append("+").append(item.content()).append("\n");
                case REMOVE -> sb.append("-").append(item.content()).append("\n");
                case CONTEXT -> sb.append(" ").append(item.content()).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * Get statistics from diff items.
     */
    public DiffStats getStats(List<DiffItem> items) {
        int added = 0;
        int removed = 0;
        int unchanged = 0;

        for (DiffItem item : items) {
            switch (item.type()) {
                case ADD -> added++;
                case REMOVE -> removed++;
                case CONTEXT -> unchanged++;
            }
        }

        return new DiffStats(added, removed, unchanged, added > 0 || removed > 0);
    }
}