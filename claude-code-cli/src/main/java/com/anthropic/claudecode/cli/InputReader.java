/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 *
 * Java port of Claude Code CLI module
 */
package com.anthropic.claudecode.cli;

import java.util.*;
import java.io.*;

/**
 * Input reader interface and implementations.
 */
public interface InputReader {

    /**
     * Read a line of input.
     */
    String readLine(String prompt);

    /**
     * Read a line without prompt.
     */
    String readLine();

    /**
     * Read multiple lines until delimiter.
     */
    List<String> readLines(String delimiter);

    /**
     * Read password (hidden input).
     */
    String readPassword(String prompt);

    /**
     * Check if input available.
     */
    boolean hasInput();

    /**
     * Close the reader.
     */
    void close();

    /**
     * Console input reader.
     */
    static class ConsoleInputReader implements InputReader {
        private final BufferedReader reader;
        private volatile boolean closed = false;

        public ConsoleInputReader() {
            this.reader = new BufferedReader(new InputStreamReader(System.in));
        }

        @Override
        public String readLine(String prompt) {
            if (closed) return null;

            System.out.print(prompt);
            System.out.flush();

            try {
                return reader.readLine();
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        public String readLine() {
            return readLine("");
        }

        @Override
        public List<String> readLines(String delimiter) {
            List<String> lines = new ArrayList<>();
            while (true) {
                String line = readLine();
                if (line == null || line.equals(delimiter)) {
                    break;
                }
                lines.add(line);
            }
            return lines;
        }

        @Override
        public String readPassword(String prompt) {
            if (closed) return null;

            System.out.print(prompt);
            System.out.flush();

            try {
                // Simple implementation - not hiding input properly
                // In production, use Console.readPassword() or similar
                return reader.readLine();
            } catch (IOException e) {
                return null;
            }
        }

        @Override
        public boolean hasInput() {
            try {
                return System.in.available() > 0;
            } catch (IOException e) {
                return false;
            }
        }

        @Override
        public void close() {
            closed = true;
            try {
                reader.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    /**
     * String input reader for testing.
     */
    static class StringInputReader implements InputReader {
        private final Iterator<String> lines;
        private volatile boolean closed = false;

        public StringInputReader(List<String> lines) {
            this.lines = lines.iterator();
        }

        public StringInputReader(String... lines) {
            this.lines = Arrays.asList(lines).iterator();
        }

        @Override
        public String readLine(String prompt) {
            if (closed || !lines.hasNext()) return null;
            return lines.next();
        }

        @Override
        public String readLine() {
            return readLine("");
        }

        @Override
        public List<String> readLines(String delimiter) {
            List<String> result = new ArrayList<>();
            while (lines.hasNext()) {
                String line = lines.next();
                if (line.equals(delimiter)) break;
                result.add(line);
            }
            return result;
        }

        @Override
        public String readPassword(String prompt) {
            return readLine(prompt);
        }

        @Override
        public boolean hasInput() {
            return !closed && lines.hasNext();
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}