/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 *
 * Java port of Claude Code CLI input handler
 */
package com.anthropic.claudecode.cli;

import java.io.*;
import java.util.*;
import java.util.function.*;

/**
 * Input handler for CLI interactions.
 *
 * <p>Handles user input with history and autocomplete.
 */
public class InputHandler {

    private final BufferedReader reader;
    private final List<String> history;
    private final int maxHistory;
    private final List<String> completions;
    private int historyIndex;

    public InputHandler() {
        this(new BufferedReader(new InputStreamReader(System.in)), 1000);
    }

    public InputHandler(BufferedReader reader, int maxHistory) {
        this.reader = reader;
        this.maxHistory = maxHistory;
        this.history = new ArrayList<>();
        this.completions = new ArrayList<>();
        this.historyIndex = -1;
    }

    /**
     * Read a line of input.
     */
    public String readLine(String prompt) throws IOException {
        System.out.print(prompt);
        System.out.flush();

        String line = reader.readLine();
        if (line != null && !line.trim().isEmpty()) {
            addToHistory(line);
        }

        return line;
    }

    /**
     * Read line with default value.
     */
    public String readLineWithDefault(String prompt, String defaultValue) throws IOException {
        System.out.print(prompt);
        if (defaultValue != null && !defaultValue.isEmpty()) {
            System.out.print(" [" + defaultValue + "]");
        }
        System.out.print(": ");
        System.out.flush();

        String line = reader.readLine();
        if (line == null || line.trim().isEmpty()) {
            return defaultValue;
        }
        return line.trim();
    }

    /**
     * Read password (masked input).
     */
    public String readPassword(String prompt) throws IOException {
        Console console = System.console();
        if (console != null) {
            char[] password = console.readPassword(prompt);
            return new String(password);
        }
        // Fallback for environments without console
        return readLine(prompt);
    }

    /**
     * Read yes/no confirmation.
     */
    public boolean readYesNo(String prompt) throws IOException {
        while (true) {
            String input = readLine(prompt + " (y/n): ");
            if (input == null) return false;

            input = input.trim().toLowerCase();
            if (input.equals("y") || input.equals("yes")) {
                return true;
            } else if (input.equals("n") || input.equals("no")) {
                return false;
            }

            System.out.println("Please enter 'y' or 'n'");
        }
    }

    /**
     * Read choice from options.
     */
    public int readChoice(String prompt, String... options) throws IOException {
        System.out.println(prompt);
        for (int i = 0; i < options.length; i++) {
            System.out.println("  " + (i + 1) + ". " + options[i]);
        }

        while (true) {
            String input = readLine("Select (1-" + options.length + "): ");
            try {
                int choice = Integer.parseInt(input.trim());
                if (choice >= 1 && choice <= options.length) {
                    return choice - 1;
                }
            } catch (NumberFormatException ignored) {}

            System.out.println("Invalid choice. Please enter a number between 1 and " + options.length);
        }
    }

    /**
     * Read multiline input until terminator.
     */
    public String readMultiline(String prompt, String terminator) throws IOException {
        System.out.println(prompt);
        System.out.println("End input with '" + terminator + "' on a new line");

        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.equals(terminator)) {
                break;
            }
            sb.append(line).append("\n");
        }

        return sb.toString();
    }

    /**
     * Get previous history item.
     */
    public String getPreviousHistory() {
        if (history.isEmpty() || historyIndex <= 0) {
            return null;
        }
        historyIndex--;
        return history.get(historyIndex);
    }

    /**
     * Get next history item.
     */
    public String getNextHistory() {
        if (history.isEmpty() || historyIndex >= history.size() - 1) {
            return null;
        }
        historyIndex++;
        return history.get(historyIndex);
    }

    /**
     * Get history.
     */
    public List<String> getHistory() {
        return new ArrayList<>(history);
    }

    /**
     * Clear history.
     */
    public void clearHistory() {
        history.clear();
        historyIndex = -1;
    }

    /**
     * Add completion option.
     */
    public void addCompletion(String completion) {
        completions.add(completion);
    }

    /**
     * Set completions.
     */
    public void setCompletions(List<String> completions) {
        this.completions.clear();
        this.completions.addAll(completions);
    }

    /**
     * Get completions for prefix.
     */
    public List<String> getCompletions(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return new ArrayList<>(completions);
        }

        List<String> matches = new ArrayList<>();
        for (String completion : completions) {
            if (completion.startsWith(prefix)) {
                matches.add(completion);
            }
        }
        return matches;
    }

    /**
     * Close the handler.
     */
    public void close() throws IOException {
        reader.close();
    }

    private void addToHistory(String line) {
        // Don't add duplicates consecutively
        if (!history.isEmpty() && history.get(history.size() - 1).equals(line)) {
            return;
        }

        history.add(line);
        if (history.size() > maxHistory) {
            history.remove(0);
        }
        historyIndex = history.size();
    }
}
