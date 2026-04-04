/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/bash/shellCompletion
 */
package com.anthropic.claudecode.utils.bash;

import java.util.*;
import java.nio.file.*;

/**
 * Shell completion - Shell command completion utilities.
 */
public final class ShellCompletion {
    private final List<CompletionProvider> providers = new ArrayList<>();

    /**
     * Create shell completion.
     */
    public ShellCompletion() {
        // Add default providers
        providers.add(new FileCompletionProvider());
        providers.add(new CommandCompletionProvider());
        providers.add(new OptionCompletionProvider());
    }

    /**
     * Get completions.
     */
    public List<CompletionItem> getCompletions(String input, int cursorPosition) {
        List<CompletionItem> completions = new ArrayList<>();

        String partial = input.substring(0, cursorPosition);
        String wordToComplete = extractLastWord(partial);

        for (CompletionProvider provider : providers) {
            completions.addAll(provider.getCompletions(partial, wordToComplete));
        }

        return completions;
    }

    /**
     * Extract last word.
     */
    private String extractLastWord(String input) {
        if (input == null || input.isEmpty()) return "";

        int lastSpace = input.lastIndexOf(' ');
        if (lastSpace >= 0) {
            return input.substring(lastSpace + 1);
        }
        return input;
    }

    /**
     * Completion item record.
     */
    public record CompletionItem(
        String text,
        String display,
        String description,
        CompletionType type,
        int insertStart,
        int insertEnd
    ) {
        public String getInsertText() {
            return text;
        }
    }

    /**
     * Completion type enum.
     */
    public enum CompletionType {
        FILE,
        DIRECTORY,
        COMMAND,
        OPTION,
        ARGUMENT,
        VARIABLE,
        KEYWORD
    }

    /**
     * Completion provider interface.
     */
    public interface CompletionProvider {
        List<CompletionItem> getCompletions(String input, String wordToComplete);
    }

    /**
     * File completion provider.
     */
    private static class FileCompletionProvider implements CompletionProvider {
        @Override
        public List<CompletionItem> getCompletions(String input, String wordToComplete) {
            List<CompletionItem> completions = new ArrayList<>();

            try {
                Path basePath = Paths.get(".");
                if (wordToComplete.startsWith("/")) {
                    basePath = Paths.get("/");
                } else if (wordToComplete.startsWith("~")) {
                    basePath = Paths.get(System.getProperty("user.home"));
                } else if (wordToComplete.contains("/")) {
                    int lastSlash = wordToComplete.lastIndexOf('/');
                    basePath = Paths.get(wordToComplete.substring(0, lastSlash));
                }

                String prefixRaw = wordToComplete;
                if (wordToComplete.contains("/")) {
                    prefixRaw = wordToComplete.substring(wordToComplete.lastIndexOf('/') + 1);
                }
                final String prefix = prefixRaw;

                if (basePath.toFile().exists()) {
                    Files.list(basePath)
                        .filter(p -> p.getFileName().toString().startsWith(prefix))
                        .forEach(p -> {
                            String name = p.getFileName().toString();
                            CompletionType type = Files.isDirectory(p)
                                ? CompletionType.DIRECTORY
                                : CompletionType.FILE;
                            completions.add(new CompletionItem(
                                name,
                                name,
                                p.toString(),
                                type,
                                input.length() - wordToComplete.length(),
                                input.length()
                            ));
                        });
                }
            } catch (Exception e) {
                // Ignore errors
            }

            return completions;
        }
    }

    /**
     * Command completion provider.
     */
    private static class CommandCompletionProvider implements CompletionProvider {
        private static final Set<String> COMMON_COMMANDS = Set.of(
            "ls", "cd", "pwd", "cat", "echo", "mkdir", "rm", "cp", "mv",
            "grep", "find", "sed", "awk", "sort", "uniq", "head", "tail",
            "git", "npm", "node", "java", "python", "pip", "docker", "kubectl"
        );

        @Override
        public List<CompletionItem> getCompletions(String input, String wordToComplete) {
            List<CompletionItem> completions = new ArrayList<>();

            // Only suggest commands at start of input
            if (!input.trim().equals(wordToComplete)) {
                return completions;
            }

            for (String cmd : COMMON_COMMANDS) {
                if (cmd.startsWith(wordToComplete)) {
                    completions.add(new CompletionItem(
                        cmd,
                        cmd,
                        "Command: " + cmd,
                        CompletionType.COMMAND,
                        0,
                        wordToComplete.length()
                    ));
                }
            }

            return completions;
        }
    }

    /**
     * Option completion provider.
     */
    private static class OptionCompletionProvider implements CompletionProvider {
        private static final Map<String, Set<String>> COMMAND_OPTIONS = Map.of(
            "ls", Set.of("-l", "-a", "-h", "-R", "-t", "-S"),
            "git", Set.of("--help", "--version", "add", "commit", "push", "pull", "status", "log"),
            "grep", Set.of("-i", "-v", "-r", "-n", "-c", "-l"),
            "find", Set.of("-name", "-type", "-size", "-mtime", "-exec")
        );

        @Override
        public List<CompletionItem> getCompletions(String input, String wordToComplete) {
            List<CompletionItem> completions = new ArrayList<>();

            if (!wordToComplete.startsWith("-")) {
                return completions;
            }

            // Get command name
            String[] parts = input.trim().split("\\s+");
            if (parts.length < 1) return completions;

            String command = parts[0];
            Set<String> options = COMMAND_OPTIONS.getOrDefault(command, Collections.emptySet());

            for (String opt : options) {
                if (opt.startsWith(wordToComplete)) {
                    completions.add(new CompletionItem(
                        opt,
                        opt,
                        command + " option",
                        CompletionType.OPTION,
                        input.length() - wordToComplete.length(),
                        input.length()
                    ));
                }
            }

            return completions;
        }
    }
}