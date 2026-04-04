/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/argumentSubstitution.ts
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.regex.*;

/**
 * Utility for substituting $ARGUMENTS placeholders in skill/command prompts.
 *
 * Supports:
 * - $ARGUMENTS - replaced with the full arguments string
 * - $ARGUMENTS[0], $ARGUMENTS[1], etc. - replaced with individual indexed arguments
 * - $0, $1, etc. - shorthand for $ARGUMENTS[0], $ARGUMENTS[1]
 * - Named arguments (e.g., $foo, $bar) - when argument names are defined in frontmatter
 */
public final class ArgumentSubstitution {
    private ArgumentSubstitution() {}

    /**
     * Parse an arguments string into an array of individual arguments.
     * Uses shell-quote for proper shell argument parsing including quoted strings.
     */
    public static List<String> parseArguments(String args) {
        if (args == null || args.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // Simple shell-like parsing - handles basic quoted strings
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean escape = false;

        for (int i = 0; i < args.length(); i++) {
            char c = args.charAt(i);

            if (escape) {
                current.append(c);
                escape = false;
                continue;
            }

            if (c == '\\' && !inSingleQuote) {
                escape = true;
                continue;
            }

            if (c == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                continue;
            }

            if (c == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                continue;
            }

            if (Character.isWhitespace(c) && !inSingleQuote && !inDoubleQuote) {
                if (current.length() > 0) {
                    result.add(current.toString());
                    current = new StringBuilder();
                }
                continue;
            }

            current.append(c);
        }

        if (current.length() > 0) {
            result.add(current.toString());
        }

        return result;
    }

    /**
     * Parse argument names from the frontmatter 'arguments' field.
     * Accepts either a space-separated string or an array of strings.
     */
    public static List<String> parseArgumentNames(Object argumentNames) {
        if (argumentNames == null) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();

        if (argumentNames instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof String name && isValidName(name)) {
                    result.add(name.trim());
                }
            }
        } else if (argumentNames instanceof String str) {
            for (String name : str.split("\\s+")) {
                if (isValidName(name)) {
                    result.add(name.trim());
                }
            }
        }

        return result;
    }

    private static boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty() && !name.matches("^\\d+$");
    }

    /**
     * Generate argument hint showing remaining unfilled args.
     */
    public static String generateProgressiveArgumentHint(
            List<String> argNames,
            List<String> typedArgs) {

        if (argNames.size() <= typedArgs.size()) {
            return null;
        }

        List<String> remaining = argNames.subList(typedArgs.size(), argNames.size());
        StringBuilder sb = new StringBuilder();
        for (String name : remaining) {
            if (sb.length() > 0) sb.append(" ");
            sb.append("[").append(name).append("]");
        }
        return sb.toString();
    }

    /**
     * Substitute $ARGUMENTS placeholders in content with actual argument values.
     */
    public static String substituteArguments(
            String content,
            String args,
            boolean appendIfNoPlaceholder,
            List<String> argumentNames) {

        // null means no args provided - return content unchanged
        if (args == null) {
            return content;
        }

        List<String> parsedArgs = parseArguments(args);
        String originalContent = content;

        // Replace named arguments (e.g., $foo, $bar) with their values
        if (argumentNames != null) {
            for (int i = 0; i < argumentNames.size(); i++) {
                String name = argumentNames.get(i);
                if (name == null || name.isEmpty()) continue;

                // Match $name but not $name[...] or $nameXxx
                String value = i < parsedArgs.size() ? parsedArgs.get(i) : "";
                content = content.replaceAll("\\$" + Pattern.quote(name) + "(?![\\[\\w])", value);
            }
        }

        // Replace indexed arguments ($ARGUMENTS[0], $ARGUMENTS[1], etc.)
        Pattern indexedPattern = Pattern.compile("\\$ARGUMENTS\\[(\\d+)\\]");
        Matcher matcher = indexedPattern.matcher(content);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            String value = index < parsedArgs.size() ? parsedArgs.get(index) : "";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        content = sb.toString();

        // Replace shorthand indexed arguments ($0, $1, etc.)
        Pattern shorthandPattern = Pattern.compile("\\$(\\d+)(?!\\w)");
        matcher = shorthandPattern.matcher(content);
        sb = new StringBuffer();
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            String value = index < parsedArgs.size() ? parsedArgs.get(index) : "";
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        content = sb.toString();

        // Replace $ARGUMENTS with the full arguments string
        content = content.replace("$ARGUMENTS", args);

        // If no placeholders were found and appendIfNoPlaceholder is true, append
        if (content.equals(originalContent) && appendIfNoPlaceholder && !args.isEmpty()) {
            content = content + "\n\nARGUMENTS: " + args;
        }

        return content;
    }

    /**
     * Substitute arguments with defaults.
     */
    public static String substituteArguments(String content, String args) {
        return substituteArguments(content, args, true, new ArrayList<>());
    }
}