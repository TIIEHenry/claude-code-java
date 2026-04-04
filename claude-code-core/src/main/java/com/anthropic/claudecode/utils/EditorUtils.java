/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/editor.ts
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * External editor utilities.
 */
public final class EditorUtils {
    private EditorUtils() {}

    // GUI editors that open in separate windows
    private static final Set<String> GUI_EDITORS = Set.of(
        "code", "cursor", "windsurf", "codium", "subl", "atom", "gedit", "notepad++", "notepad"
    );

    // Editors that accept +N as a goto-line argument
    private static final Set<String> PLUS_N_EDITORS = Set.of(
        "vi", "vim", "nvim", "nano", "emacs", "pico", "micro", "helix", "hx"
    );

    // VS Code family
    private static final Set<String> VSCODE_FAMILY = Set.of("code", "cursor", "windsurf", "codium");

    // Cached editor
    private static volatile String cachedEditor = null;

    /**
     * Get the external editor.
     */
    public static String getExternalEditor() {
        if (cachedEditor != null) {
            return cachedEditor;
        }

        // Prioritize environment variables
        String visual = System.getenv("VISUAL");
        if (visual != null && !visual.trim().isEmpty()) {
            cachedEditor = visual.trim();
            return cachedEditor;
        }

        String editor = System.getenv("EDITOR");
        if (editor != null && !editor.trim().isEmpty()) {
            cachedEditor = editor.trim();
            return cachedEditor;
        }

        // Platform-specific defaults
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            cachedEditor = "notepad";
            return cachedEditor;
        }

        // Search for available editors
        String[] editors = {"code", "vim", "nano", "vi"};
        for (String e : editors) {
            if (isCommandAvailable(e)) {
                cachedEditor = e;
                return cachedEditor;
            }
        }

        return null;
    }

    /**
     * Classify the editor as GUI or not.
     */
    public static String classifyGuiEditor(String editor) {
        if (editor == null || editor.isEmpty()) return null;

        String base = getBaseCommand(editor);
        for (String gui : GUI_EDITORS) {
            if (base.contains(gui)) {
                return gui;
            }
        }
        return null;
    }

    /**
     * Check if editor is a GUI editor.
     */
    public static boolean isGuiEditor(String editor) {
        return classifyGuiEditor(editor) != null;
    }

    /**
     * Check if editor supports +N goto-line.
     */
    public static boolean supportsGotoLine(String editor) {
        if (editor == null) return false;
        String base = getBaseCommand(editor);
        return PLUS_N_EDITORS.contains(base);
    }

    /**
     * Open a file in the external editor.
     */
    public static boolean openFileInExternalEditor(String filePath, Integer line) {
        String editor = getExternalEditor();
        if (editor == null) return false;

        String guiFamily = classifyGuiEditor(editor);
        if (guiFamily != null) {
            return openInGuiEditor(editor, guiFamily, filePath, line);
        } else {
            return openInTerminalEditor(editor, filePath, line);
        }
    }

    /**
     * Open a file in the external editor (no line number).
     */
    public static boolean openFileInExternalEditor(String filePath) {
        return openFileInExternalEditor(filePath, null);
    }

    /**
     * Open file in GUI editor (non-blocking).
     */
    private static boolean openInGuiEditor(String editor, String guiFamily, String filePath, Integer line) {
        try {
            List<String> command = new ArrayList<>();
            String[] parts = editor.split(" ");
            command.add(parts[0]);

            // Add goto arguments
            if (line != null) {
                if (VSCODE_FAMILY.contains(guiFamily)) {
                    command.add("-g");
                    command.add(filePath + ":" + line);
                } else if ("subl".equals(guiFamily)) {
                    command.add(filePath + ":" + line);
                } else {
                    command.add(filePath);
                }
            } else {
                command.add(filePath);
            }

            // Add remaining editor args
            for (int i = 1; i < parts.length; i++) {
                command.add(parts[i]);
            }

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(Cwd.getCwd().toString()));
            Process process = pb.start();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Open file in terminal editor (blocking).
     */
    private static boolean openInTerminalEditor(String editor, String filePath, Integer line) {
        try {
            List<String> command = new ArrayList<>();
            String[] parts = editor.split(" ");
            command.add(parts[0]);

            for (int i = 1; i < parts.length; i++) {
                command.add(parts[i]);
            }

            // Add +N for editors that support it
            if (line != null && supportsGotoLine(editor)) {
                command.add("+" + line);
            }
            command.add(filePath);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(Cwd.getCwd().toString()));
            pb.inheritIO();
            Process process = pb.start();
            process.waitFor();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get base command from editor string.
     */
    private static String getBaseCommand(String editor) {
        if (editor == null) return "";
        String[] parts = editor.split(" ");
        String first = parts[0];
        int lastSlash = Math.max(first.lastIndexOf('/'), first.lastIndexOf('\\'));
        if (lastSlash >= 0 && lastSlash < first.length() - 1) {
            return first.substring(lastSlash + 1);
        }
        return first;
    }

    /**
     * Check if a command is available.
     */
    private static boolean isCommandAvailable(String command) {
        try {
            ProcessBuilder pb;
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                pb = new ProcessBuilder("where", command);
            } else {
                pb = new ProcessBuilder("which", command);
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Clear the cached editor.
     */
    public static void clearCache() {
        cachedEditor = null;
    }
}