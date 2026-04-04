/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/bash/heredoc.ts
 */
package com.anthropic.claudecode.utils.bash;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.security.SecureRandom;

/**
 * Heredoc extraction and restoration utilities.
 *
 * The shell-quote library parses << as two separate < redirect operators,
 * which breaks command splitting for heredoc syntax. This module provides
 * utilities to extract heredocs before parsing and restore them after.
 */
public final class HeredocUtils {
    private HeredocUtils() {}

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String HEREDOC_PLACEHOLDER_PREFIX = "__HEREDOC_";
    private static final String HEREDOC_PLACEHOLDER_SUFFIX = "__";

    // Heredoc start pattern regex
    // Matches: <<EOF, <<'EOF', <<"EOF", <<-EOF, <<-'EOF', <<\EOF
    private static final String HEREDOC_START_REGEX = "(?<!<)<<(?!<)(-)?[ \\t]*(?:(['\"])(\\\\?\\w+)\\2|\\\\?(\\w+))";

    /**
     * Heredoc information record.
     */
    public record HeredocInfo(
        String fullText,           // Full heredoc text including << operator, delimiter, content, closing
        String delimiter,          // The delimiter word (without quotes)
        int operatorStartIndex,    // Start position of << operator
        int operatorEndIndex,      // End position of << operator (exclusive)
        int contentStartIndex,     // Start position of heredoc content
        int contentEndIndex        // End position of heredoc content including closing delimiter
    ) {}

    /**
     * Heredoc extraction result.
     */
    public record HeredocExtractionResult(
        String processedCommand,   // Command with heredocs replaced by placeholders
        Map<String, HeredocInfo> heredocs  // Map of placeholder to heredoc info
    ) {}

    /**
     * Generate a random hex string for placeholder uniqueness.
     */
    private static String generatePlaceholderSalt() {
        byte[] bytes = new byte[8];
        RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(16);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Extract heredocs from a command string and replace with placeholders.
     */
    public static HeredocExtractionResult extractHeredocs(String command) {
        return extractHeredocs(command, false);
    }

    /**
     * Extract heredocs from a command string.
     * @param command The shell command potentially containing heredocs
     * @param quotedOnly If true, only extract quoted/escaped heredocs
     */
    public static HeredocExtractionResult extractHeredocs(String command, boolean quotedOnly) {
        Map<String, HeredocInfo> heredocs = new LinkedHashMap<>();

        if (command == null || !command.contains("<<")) {
            return new HeredocExtractionResult(command != null ? command : "", heredocs);
        }

        // Security pre-validation: bail on constructs that could desync quote tracking
        if (command.contains("$'") || command.contains("$\"")) {
            return new HeredocExtractionResult(command, heredocs);
        }

        // Check for backticks before first <<
        int firstHeredocPos = command.indexOf("<<");
        if (firstHeredocPos > 0 && command.substring(0, firstHeredocPos).contains("`")) {
            return new HeredocExtractionResult(command, heredocs);
        }

        // Check for arithmetic context
        if (firstHeredocPos > 0) {
            String beforeHeredoc = command.substring(0, firstHeredocPos);
            int openArith = countOccurrences(beforeHeredoc, "((");
            int closeArith = countOccurrences(beforeHeredoc, "))");
            if (openArith > closeArith) {
                return new HeredocExtractionResult(command, heredocs);
            }
        }

        List<HeredocInfo> heredocMatches = new ArrayList<>();
        List<int[]> skippedHeredocRanges = new ArrayList<>(); // [contentStart, contentEnd]

        // Quote/comment scanner state
        int scanPos = 0;
        boolean scanInSingleQuote = false;
        boolean scanInDoubleQuote = false;
        boolean scanInComment = false;
        boolean scanDqEscapeNext = false;
        int scanPendingBackslashes = 0;

        // Parse with regex
        Pattern pattern = Pattern.compile(HEREDOC_START_REGEX);
        Matcher matcher = pattern.matcher(command);

        while (matcher.find()) {
            int startIndex = matcher.start();

            // Advance scanner to this position
            int[] scanState = advanceScan(command, scanPos, startIndex,
                scanInSingleQuote, scanInDoubleQuote, scanInComment,
                scanDqEscapeNext, scanPendingBackslashes);
            scanInSingleQuote = scanState[0] == 1;
            scanInDoubleQuote = scanState[1] == 1;
            scanInComment = scanState[2] == 1;
            scanDqEscapeNext = scanState[3] == 1;
            scanPendingBackslashes = scanState[4];
            scanPos = startIndex;

            // Skip if inside quoted string
            if (scanInSingleQuote || scanInDoubleQuote) {
                continue;
            }

            // Skip if inside comment
            if (scanInComment) {
                continue;
            }

            // Skip if escaped
            if (scanPendingBackslashes % 2 == 1) {
                continue;
            }

            // Check if inside skipped heredoc range
            boolean insideSkipped = false;
            for (int[] range : skippedHeredocRanges) {
                if (startIndex > range[0] && startIndex < range[1]) {
                    insideSkipped = true;
                    break;
                }
            }
            if (insideSkipped) {
                continue;
            }

            String fullMatch = matcher.group();
            boolean isDash = "-".equals(matcher.group(1));
            // Group 3 = quoted delimiter, group 4 = unquoted
            String delimiter = matcher.group(3) != null ? matcher.group(3) : matcher.group(4);
            if (delimiter == null) continue;

            int operatorEndIndex = startIndex + fullMatch.length();

            // Check for quote closure
            String quoteChar = matcher.group(2);
            if (quoteChar != null && command.charAt(operatorEndIndex - 1) != quoteChar.charAt(0)) {
                continue;
            }

            // Determine if delimiter is quoted/escaped
            boolean isEscapedDelimiter = fullMatch.contains("\\");
            boolean isQuotedOrEscaped = quoteChar != null || isEscapedDelimiter;

            // Check for bash word terminator
            if (operatorEndIndex < command.length()) {
                char nextChar = command.charAt(operatorEndIndex);
                if (!isBashMetachar(nextChar)) {
                    continue;
                }
            }

            // Find first unquoted newline after operator
            int firstNewlineOffset = findFirstUnquotedNewline(command, operatorEndIndex);
            if (firstNewlineOffset == -1) {
                continue;
            }

            // Check for line continuation
            String sameLineContent = command.substring(operatorEndIndex, operatorEndIndex + firstNewlineOffset);
            int trailingBackslashes = countTrailingBackslashes(sameLineContent);
            if (trailingBackslashes % 2 == 1) {
                continue;
            }

            int contentStartIndex = operatorEndIndex + firstNewlineOffset;
            String afterNewline = command.substring(contentStartIndex + 1);
            String[] contentLines = afterNewline.split("\n", -1);

            // Find closing delimiter
            int closingLineIndex = -1;
            for (int i = 0; i < contentLines.length; i++) {
                String line = contentLines[i];

                if (isDash) {
                    String stripped = line.replaceFirst("^\\t*", "");
                    if (stripped.equals(delimiter)) {
                        closingLineIndex = i;
                        break;
                    }
                } else {
                    if (line.equals(delimiter)) {
                        closingLineIndex = i;
                        break;
                    }
                }

                // Check for early closure pattern
                String eofCheckLine = isDash ? line.replaceFirst("^\\t*", "") : line;
                if (eofCheckLine.length() > delimiter.length() && eofCheckLine.startsWith(delimiter)) {
                    char charAfter = eofCheckLine.charAt(delimiter.length());
                    if (isEarlyCloserChar(charAfter)) {
                        closingLineIndex = -1;
                        break;
                    }
                }
            }

            // Handle quotedOnly mode
            if (quotedOnly && !isQuotedOrEscaped) {
                int skipContentEndIndex;
                if (closingLineIndex == -1) {
                    skipContentEndIndex = command.length();
                } else {
                    String[] skipLinesUpToClosing = Arrays.copyOfRange(contentLines, 0, closingLineIndex + 1);
                    int skipContentLength = String.join("\n", skipLinesUpToClosing).length();
                    skipContentEndIndex = contentStartIndex + 1 + skipContentLength;
                }
                skippedHeredocRanges.add(new int[]{contentStartIndex, skipContentEndIndex});
                continue;
            }

            if (closingLineIndex == -1) {
                continue;
            }

            // Calculate content end index
            String[] linesUpToClosing = Arrays.copyOfRange(contentLines, 0, closingLineIndex + 1);
            int contentLength = String.join("\n", linesUpToClosing).length();
            int contentEndIndex = contentStartIndex + 1 + contentLength;

            // Check overlap with skipped ranges
            boolean overlapsSkipped = false;
            for (int[] range : skippedHeredocRanges) {
                if (contentStartIndex < range[1] && range[0] < contentEndIndex) {
                    overlapsSkipped = true;
                    break;
                }
            }
            if (overlapsSkipped) {
                continue;
            }

            // Build fullText
            String operatorText = command.substring(startIndex, operatorEndIndex);
            String contentText = command.substring(contentStartIndex, contentEndIndex);
            String fullText = operatorText + contentText;

            heredocMatches.add(new HeredocInfo(
                fullText, delimiter, startIndex, operatorEndIndex,
                contentStartIndex, contentEndIndex
            ));
        }

        if (heredocMatches.isEmpty()) {
            return new HeredocExtractionResult(command, heredocs);
        }

        // Filter nested heredocs
        List<HeredocInfo> topLevelHeredocs = filterNestedHeredocs(heredocMatches);

        if (topLevelHeredocs.isEmpty()) {
            return new HeredocExtractionResult(command, heredocs);
        }

        // Check for shared content start positions
        Set<Integer> contentStartPositions = new HashSet<>();
        for (HeredocInfo info : topLevelHeredocs) {
            contentStartPositions.add(info.contentStartIndex());
        }
        if (contentStartPositions.size() < topLevelHeredocs.size()) {
            return new HeredocExtractionResult(command, heredocs);
        }

        // Sort by content end position descending
        topLevelHeredocs.sort((a, b) -> Integer.compare(b.contentEndIndex(), a.contentEndIndex()));

        // Generate salt and process
        String salt = generatePlaceholderSalt();
        String processedCommand = command;

        for (int i = 0; i < topLevelHeredocs.size(); i++) {
            HeredocInfo info = topLevelHeredocs.get(i);
            int placeholderIndex = topLevelHeredocs.size() - 1 - i;
            String placeholder = HEREDOC_PLACEHOLDER_PREFIX + placeholderIndex + "_" + salt + HEREDOC_PLACEHOLDER_SUFFIX;

            heredocs.put(placeholder, info);

            // Replace heredoc with placeholder
            processedCommand = processedCommand.substring(0, info.operatorStartIndex()) +
                placeholder +
                processedCommand.substring(info.operatorEndIndex(), info.contentStartIndex()) +
                processedCommand.substring(info.contentEndIndex());
        }

        return new HeredocExtractionResult(processedCommand, heredocs);
    }

    /**
     * Restore heredoc placeholders in a string array.
     */
    public static List<String> restoreHeredocs(List<String> parts, Map<String, HeredocInfo> heredocs) {
        if (heredocs == null || heredocs.isEmpty() || parts == null) {
            return parts != null ? parts : Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String restored = part;
            for (Map.Entry<String, HeredocInfo> entry : heredocs.entrySet()) {
                restored = restored.replace(entry.getKey(), entry.getValue().fullText());
            }
            result.add(restored);
        }
        return result;
    }

    /**
     * Restore heredoc placeholders in a single string.
     */
    public static String restoreHeredocsInString(String text, Map<String, HeredocInfo> heredocs) {
        if (heredocs == null || heredocs.isEmpty() || text == null) {
            return text != null ? text : "";
        }

        String result = text;
        for (Map.Entry<String, HeredocInfo> entry : heredocs.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue().fullText());
        }
        return result;
    }

    /**
     * Check if a command contains heredoc syntax.
     */
    public static boolean containsHeredocPattern(String command) {
        if (command == null || !command.contains("<<")) {
            return false;
        }
        return Pattern.compile(HEREDOC_START_REGEX).matcher(command).find();
    }

    // Private helper methods

    private static int[] advanceScan(String command, int fromPos, int targetPos,
            boolean inSingle, boolean inDouble, boolean inComment,
            boolean dqEscapeNext, int pendingBackslashes) {
        for (int i = fromPos; i < targetPos; i++) {
            char ch = command.charAt(i);

            if (ch == '\n') inComment = false;

            if (inSingle) {
                if (ch == '\'') inSingle = false;
                continue;
            }

            if (inDouble) {
                if (dqEscapeNext) {
                    dqEscapeNext = false;
                    continue;
                }
                if (ch == '\\') {
                    dqEscapeNext = true;
                    continue;
                }
                if (ch == '"') inDouble = false;
                continue;
            }

            if (ch == '\\') {
                pendingBackslashes++;
                continue;
            }
            boolean escaped = pendingBackslashes % 2 == 1;
            pendingBackslashes = 0;
            if (escaped) continue;

            if (ch == '\'') inSingle = true;
            else if (ch == '"') inDouble = true;
            else if (!inComment && ch == '#') inComment = true;
        }

        return new int[]{
            inSingle ? 1 : 0,
            inDouble ? 1 : 0,
            inComment ? 1 : 0,
            dqEscapeNext ? 1 : 0,
            pendingBackslashes
        };
    }

    private static boolean isBashMetachar(char c) {
        return c == ' ' || c == '\t' || c == '\n' ||
               c == '|' || c == '&' || c == ';' ||
               c == '(' || c == ')' || c == '<' || c == '>';
    }

    private static int findFirstUnquotedNewline(String command, int startPos) {
        boolean inSingle = false;
        boolean inDouble = false;

        for (int k = startPos; k < command.length(); k++) {
            char ch = command.charAt(k);

            if (inSingle) {
                if (ch == '\'') inSingle = false;
                continue;
            }

            if (inDouble) {
                if (ch == '\\') {
                    k++;
                    continue;
                }
                if (ch == '"') inDouble = false;
                continue;
            }

            // Unquoted context
            int backslashCount = 0;
            for (int j = k - 1; j >= startPos && command.charAt(j) == '\\'; j--) {
                backslashCount++;
            }
            if (backslashCount % 2 == 1) continue;

            if (ch == '\n') {
                return k - startPos;
            }

            if (ch == '\'') inSingle = true;
            else if (ch == '"') inDouble = true;
        }

        return -1;
    }

    private static int countTrailingBackslashes(String s) {
        int count = 0;
        for (int j = s.length() - 1; j >= 0; j--) {
            if (s.charAt(j) == '\\') {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    private static boolean isEarlyCloserChar(char c) {
        return c == ')' || c == '}' || c == '`' ||
               c == '|' || c == '&' || c == ';' ||
               c == '(' || c == '<' || c == '>';
    }

    private static List<HeredocInfo> filterNestedHeredocs(List<HeredocInfo> matches) {
        List<HeredocInfo> topLevel = new ArrayList<>();
        for (HeredocInfo candidate : matches) {
            boolean isNested = false;
            for (HeredocInfo other : matches) {
                if (candidate == other) continue;
                if (candidate.operatorStartIndex() > other.contentStartIndex() &&
                    candidate.operatorStartIndex() < other.contentEndIndex()) {
                    isNested = true;
                    break;
                }
            }
            if (!isNested) {
                topLevel.add(candidate);
            }
        }
        return topLevel;
    }

    private static int countOccurrences(String s, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = s.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}