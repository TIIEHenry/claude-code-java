/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code components/StructuredDiff
 */
package com.anthropic.claudecode.components.diff;

import java.util.*;
import java.util.regex.*;

/**
 * Structured diff - Diff types and utilities.
 */
public final class StructuredDiffTypes {

    /**
     * Diff line type enum.
     */
    public enum DiffLineType {
        CONTEXT(" "),
        ADD("+"),
        REMOVE("-"),
        HEADER("@"),
        NO_NEWLINE("\\");

        private final String prefix;

        DiffLineType(String prefix) {
            this.prefix = prefix;
        }

        public String getPrefix() { return prefix; }

        public static DiffLineType fromPrefix(char c) {
            return switch (c) {
                case ' ' -> CONTEXT;
                case '+' -> ADD;
                case '-' -> REMOVE;
                case '@' -> HEADER;
                case '\\' -> NO_NEWLINE;
                default -> CONTEXT;
            };
        }
    }

    /**
     * Diff line record.
     */
    public record DiffLine(
        DiffLineType type,
        String content,
        int oldLineNumber,
        int newLineNumber,
        String annotation
    ) {
        public boolean isContext() { return type == DiffLineType.CONTEXT; }
        public boolean isAdd() { return type == DiffLineType.ADD; }
        public boolean isRemove() { return type == DiffLineType.REMOVE; }
        public boolean isHeader() { return type == DiffLineType.HEADER; }

        public String format() {
            StringBuilder sb = new StringBuilder();
            sb.append(type.getPrefix());
            sb.append(content);
            if (annotation != null) {
                sb.append(" [").append(annotation).append("]");
            }
            return sb.toString();
        }
    }

    /**
     * Diff hunk record.
     */
    public record DiffHunk(
        int oldStart,
        int oldCount,
        int newStart,
        int newCount,
        String header,
        List<DiffLine> lines
    ) {
        public int getOldEnd() { return oldStart + oldCount - 1; }
        public int getNewEnd() { return newStart + newCount - 1; }

        public List<DiffLine> getAdds() {
            return lines.stream().filter(DiffLine::isAdd).toList();
        }

        public List<DiffLine> getRemoves() {
            return lines.stream().filter(DiffLine::isRemove).toList();
        }

        public int getAddCount() { return getAdds().size(); }
        public int getRemoveCount() { return getRemoves().size(); }

        public String formatHeader() {
            return String.format("@@ -%d,%d +%d,%d @@%s",
                oldStart, oldCount, newStart, newCount, header);
        }
    }

    /**
     * Diff file record.
     */
    public record DiffFile(
        String oldPath,
        String newPath,
        List<DiffHunk> hunks,
        DiffFileType fileType
    ) {
        public boolean isNewFile() { return oldPath.equals("/dev/null"); }
        public boolean isDeletedFile() { return newPath.equals("/dev/null"); }
        public boolean isRenamed() { return !oldPath.equals(newPath) && !isNewFile() && !isDeletedFile(); }

        public int getTotalAddLines() {
            return hunks.stream().mapToInt(DiffHunk::getAddCount).sum();
        }

        public int getTotalRemoveLines() {
            return hunks.stream().mapToInt(DiffHunk::getRemoveCount).sum();
        }

        public String formatSummary() {
            String path = isDeletedFile() ? oldPath : newPath;
            return String.format("%s: +%d -%d", path, getTotalAddLines(), getTotalRemoveLines());
        }
    }

    /**
     * Diff file type enum.
     */
    public enum DiffFileType {
        ADDED,
        DELETED,
        MODIFIED,
        RENAMED,
        COPIED,
        TYPE_CHANGED
    }

    /**
     * Color config for diff.
     */
    public record DiffColorConfig(
        String addColor,
        String removeColor,
        String contextColor,
        String headerColor,
        boolean useAnsi
    ) {
        public static DiffColorConfig defaultConfig() {
            return new DiffColorConfig(
                "\033[32m",  // Green for adds
                "\033[31m",  // Red for removes
                "\033[0m",   // Default for context
                "\033[36m",  // Cyan for headers
                true
            );
        }
    }

    /**
     * Apply color to line.
     */
    public static String colorLine(DiffLine line, DiffColorConfig config) {
        if (!config.useAnsi()) return line.format();

        String color = switch (line.type()) {
            case ADD -> config.addColor();
            case REMOVE -> config.removeColor();
            case HEADER -> config.headerColor();
            default -> config.contextColor();
        };

        return color + line.format() + "\033[0m";
    }
}