/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code editor utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Editor utilities for launching external editors.
 */
public final class Editor {
    private Editor() {}

    // GUI editors that open in a separate window
    private static final Set<String> GUI_EDITORS = Set.of(
            "code", "cursor", "windsurf", "codium", "subl", "atom", "gedit", "notepad++", "notepad"
    );

    // VS Code family
    private static final Set<String> VSCODE_FAMILY = Set.of("code", "cursor", "windsurf", "codium");

    // Editors that accept +N as goto-line argument
    private static final java.util.regex.Pattern PLUS_N_EDITORS = java.util.regex.Pattern.compile(
            "\\b(vi|vim|nvim|nano|emacs|pico|micro|helix|hx)\\b"
    );

    // Cached editor
    private static volatile String cachedEditor = null;

    /**
     * Get the external editor command.
     */
    public static String getExternalEditor() {
        if (cachedEditor != null) {
            return cachedEditor;
        }

        // Check environment variables
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
        if (Platform.isWindows()) {
            cachedEditor = "notepad";
            return cachedEditor;
        }

        // Search for available editors
        String[] editors = {"code", "vim", "nano", "vi"};
        for (String cmd : editors) {
            if (Which.which(cmd) != null) {
                cachedEditor = cmd;
                return cachedEditor;
            }
        }

        return null;
    }

    /**
     * Classify the editor as GUI or not.
     * Returns the matched GUI family name for goto-line argv selection.
     */
    public static String classifyGuiEditor(String editor) {
        if (editor == null || editor.isEmpty()) {
            return null;
        }
        String base = new File(editor.split(" ")[0]).getName();
        for (String gui : GUI_EDITORS) {
            if (base.contains(gui)) {
                return gui;
            }
        }
        return null;
    }

    /**
     * Check if editor supports goto-line with +N.
     */
    public static boolean supportsGotoLine(String editor) {
        if (editor == null) return false;
        String base = new File(editor.split(" ")[0]).getName();
        return PLUS_N_EDITORS.matcher(base).find();
    }

    /**
     * Open a file in the external editor.
     */
    public static boolean openFileInExternalEditor(String filePath) {
        return openFileInExternalEditor(filePath, null);
    }

    /**
     * Open a file in the external editor at a specific line.
     */
    public static boolean openFileInExternalEditor(String filePath, Integer line) {
        String editor = getExternalEditor();
        if (editor == null) {
            return false;
        }

        String[] parts = editor.split(" ");
        String base = parts[0];
        List<String> editorArgs = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            editorArgs.add(parts[i]);
        }

        String guiFamily = classifyGuiEditor(editor);

        if (guiFamily != null) {
            // GUI editor - spawn detached
            return launchGuiEditor(guiFamily, base, editorArgs, filePath, line);
        } else {
            // Terminal editor - spawn and wait
            return launchTerminalEditor(base, editorArgs, filePath, line);
        }
    }

    /**
     * Launch a GUI editor.
     */
    private static boolean launchGuiEditor(String guiFamily, String base, List<String> editorArgs, String filePath, Integer line) {
        try {
            List<String> gotoArgs = buildGuiGotoArgs(guiFamily, filePath, line);
            List<String> cmd = new ArrayList<>();
            cmd.add(base);
            cmd.addAll(editorArgs);
            cmd.addAll(gotoArgs);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            if (Platform.isWindows()) {
                pb.command("cmd", "/c", String.join(" ", cmd));
            }
            pb.redirectInput(ProcessBuilder.Redirect.PIPE)
              .redirectOutput(ProcessBuilder.Redirect.PIPE)
              .redirectError(ProcessBuilder.Redirect.PIPE);

            Process p = pb.start();
            // Don't wait for GUI editors
            return true;
        } catch (IOException e) {
            Debug.log("Editor launch failed: " + e.getMessage(), "error");
            return false;
        }
    }

    /**
     * Build goto-line args for GUI editor.
     */
    private static List<String> buildGuiGotoArgs(String guiFamily, String filePath, Integer line) {
        List<String> args = new ArrayList<>();
        if (line == null) {
            args.add(filePath);
            return args;
        }

        if (VSCODE_FAMILY.contains(guiFamily)) {
            args.add("-g");
            args.add(filePath + ":" + line);
        } else if ("subl".equals(guiFamily)) {
            args.add(filePath + ":" + line);
        } else {
            args.add(filePath);
        }
        return args;
    }

    /**
     * Launch a terminal editor.
     */
    private static boolean launchTerminalEditor(String base, List<String> editorArgs, String filePath, Integer line) {
        try {
            List<String> cmd = new ArrayList<>();
            cmd.add(base);
            cmd.addAll(editorArgs);

            boolean useGotoLine = line != null && supportsGotoLine(base);
            if (useGotoLine) {
                cmd.add("+" + line);
            }
            cmd.add(filePath);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.inheritIO();

            Process p = pb.start();
            int exitCode = p.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            Debug.log("Editor launch failed: " + e.getMessage(), "error");
            return false;
        }
    }

    /**
     * Open a file in the default system editor.
     */
    public static boolean openWithDefaultApp(String filePath) {
        try {
            ProcessBuilder pb;
            if (Platform.isWindows()) {
                pb = new ProcessBuilder("cmd", "/c", "start", "", filePath);
            } else if (Platform.isMac()) {
                pb = new ProcessBuilder("open", filePath);
            } else {
                pb = new ProcessBuilder("xdg-open", filePath);
            }
            pb.start();
            return true;
        } catch (IOException e) {
            Debug.log("Failed to open with default app: " + e.getMessage(), "error");
            return false;
        }
    }

    /**
     * Clear the cached editor (for testing).
     */
    public static void clearCache() {
        cachedEditor = null;
    }
}