/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 *
 * Java port of Claude Code CLI module
 */
package com.anthropic.claudecode.cli;

import java.util.*;
import java.io.*;

/**
 * Output writer interface and implementations.
 */
public interface OutputWriter {

    /**
     * Write welcome message.
     */
    void writeWelcome();

    /**
     * Write result.
     */
    void writeResult(CommandResult result);

    /**
     * Write error.
     */
    void writeError(String error);

    /**
     * Write message.
     */
    void writeMessage(String message);

    /**
     * Write help.
     */
    void writeHelp();

    /**
     * Write table.
     */
    void writeTable(List<Map<String, Object>> rows, List<String> columns);

    /**
     * Close the writer.
     */
    void close();

    /**
     * Console output writer.
     */
    static class ConsoleOutputWriter implements OutputWriter {
        private final PrintWriter writer;
        private final boolean useColor;

        public ConsoleOutputWriter() {
            this(true);
        }

        public ConsoleOutputWriter(boolean useColor) {
            this.writer = new PrintWriter(new OutputStreamWriter(System.out), true);
            this.useColor = useColor;
        }

        @Override
        public void writeWelcome() {
            println("");
            println("  ╭─────────────────────────────────╮");
            println("  │     Claude Code Java CLI        │");
            println("  │     Version 1.0.0               │");
            println("  ╰─────────────────────────────────╯");
            println("");
            println("  Type 'help' for available commands.");
            println("");
        }

        @Override
        public void writeResult(CommandResult result) {
            if (result.hasError()) {
                writeError(result.error());
                return;
            }

            if (!result.isSuccess()) {
                writeError("Command failed");
                return;
            }

            switch (result.type()) {
                case EMPTY -> {}
                case SUCCESS -> {
                    if (result.hasMessage()) {
                        println(green(result.message()));
                    }
                }
                case INFO -> println(blue(result.message()));
                case WARNING -> println(yellow(result.message()));
                case DATA -> writeData(result.data());
                case TABLE -> writeTable((List<Map<String, Object>>) result.data(), null);
                default -> {
                    if (result.hasMessage()) {
                        println(result.message());
                    } else if (result.hasData()) {
                        writeData(result.data());
                    }
                }
            }
        }

        @Override
        public void writeError(String error) {
            println(red("Error: " + error));
        }

        @Override
        public void writeMessage(String message) {
            println(message);
        }

        @Override
        public void writeHelp() {
            println("Usage: claude-code [command] [options]");
            println("");
            println("Commands:");
            println("  help          Show this help message");
            println("  version       Show version information");
            println("  status        Show system status");
            println("  exit, quit    Exit the program");
            println("");
        }

        @Override
        public void writeTable(List<Map<String, Object>> rows, List<String> columns) {
            if (rows.isEmpty()) {
                println("(no data)");
                return;
            }

            // Determine columns
            List<String> cols = columns != null ? columns :
                new ArrayList<>(rows.get(0).keySet());

            // Calculate column widths
            Map<String, Integer> widths = new HashMap<>();
            for (String col : cols) {
                widths.put(col, col.length());
            }
            for (Map<String, Object> row : rows) {
                for (String col : cols) {
                    Object val = row.get(col);
                    int len = val != null ? val.toString().length() : 0;
                    widths.put(col, Math.max(widths.get(col), len));
                }
            }

            // Print header
            StringBuilder header = new StringBuilder("  ");
            for (String col : cols) {
                header.append(String.format("%-" + (widths.get(col) + 2) + "s", col));
            }
            println(bold(header.toString()));

            // Print separator
            StringBuilder sep = new StringBuilder("  ");
            for (String col : cols) {
                sep.append("-".repeat(widths.get(col) + 1)).append(" ");
            }
            println(sep.toString());

            // Print rows
            for (Map<String, Object> row : rows) {
                StringBuilder line = new StringBuilder("  ");
                for (String col : cols) {
                    Object val = row.get(col);
                    String str = val != null ? val.toString() : "";
                    line.append(String.format("%-" + (widths.get(col) + 2) + "s", str));
                }
                println(line.toString());
            }
        }

        @Override
        public void close() {
            writer.flush();
        }

        private void writeData(Object data) {
            if (data instanceof Map) {
                println("");
                ((Map<?, ?>) data).forEach((k, v) ->
                    println("  " + k + ": " + v));
                println("");
            } else if (data instanceof List) {
                println("");
                int i = 1;
                for (Object item : (List<?>) data) {
                    println("  " + i + ". " + item);
                    i++;
                }
                println("");
            } else {
                println(data.toString());
            }
        }

        private void println(String line) {
            writer.println(line);
        }

        private String red(String s) {
            return useColor ? "\u001B[31m" + s + "\u001B[0m" : s;
        }

        private String green(String s) {
            return useColor ? "\u001B[32m" + s + "\u001B[0m" : s;
        }

        private String yellow(String s) {
            return useColor ? "\u001B[33m" + s + "\u001B[0m" : s;
        }

        private String blue(String s) {
            return useColor ? "\u001B[34m" + s + "\u001B[0m" : s;
        }

        private String bold(String s) {
            return useColor ? "\u001B[1m" + s + "\u001B[0m" : s;
        }
    }

    /**
     * String output writer for testing.
     */
    static class StringOutputWriter implements OutputWriter {
        private final StringBuilder output = new StringBuilder();

        @Override
        public void writeWelcome() {
            output.append("Welcome to Claude Code CLI\n");
        }

        @Override
        public void writeResult(CommandResult result) {
            output.append(result.format()).append("\n");
        }

        @Override
        public void writeError(String error) {
            output.append("Error: ").append(error).append("\n");
        }

        @Override
        public void writeMessage(String message) {
            output.append(message).append("\n");
        }

        @Override
        public void writeHelp() {
            output.append("Help message\n");
        }

        @Override
        public void writeTable(List<Map<String, Object>> rows, List<String> columns) {
            output.append(rows.toString()).append("\n");
        }

        @Override
        public void close() {
            // Nothing
        }

        public String getOutput() {
            return output.toString();
        }

        public void clear() {
            output.setLength(0);
        }
    }
}