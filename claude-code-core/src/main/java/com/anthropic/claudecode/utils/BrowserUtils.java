/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/browser
 */
package com.anthropic.claudecode.utils;

import java.awt.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.*;

/**
 * Browser utilities - Open URLs and paths in the default browser.
 */
public final class BrowserUtils {

    /**
     * Open a URL in the default browser.
     */
    public static boolean openBrowser(String url) {
        try {
            // Validate URL
            validateUrl(url);

            String browserEnv = System.getenv("BROWSER");
            String os = System.getProperty("os.name").toLowerCase();

            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
                return true;
            }

            // Fallback to command line
            if (os.contains("win")) {
                if (browserEnv != null && !browserEnv.isEmpty()) {
                    return execCommand(browserEnv, "\"" + url + "\"");
                }
                return execCommand("rundll32", "url,OpenURL", url);
            } else if (os.contains("mac")) {
                String command = browserEnv != null ? browserEnv : "open";
                return execCommand(command, url);
            } else {
                String command = browserEnv != null ? browserEnv : "xdg-open";
                return execCommand(command, url);
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Open a file or folder path using the system's default handler.
     */
    public static boolean openPath(String path) {
        try {
            String os = System.getProperty("os.name").toLowerCase();

            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                Path p = Paths.get(path);

                if (Files.isDirectory(p) && desktop.isSupported(Desktop.Action.OPEN)) {
                    desktop.open(p.toFile());
                    return true;
                } else if (Files.isRegularFile(p) && desktop.isSupported(Desktop.Action.OPEN)) {
                    desktop.open(p.toFile());
                    return true;
                }
            }

            // Fallback to command line
            if (os.contains("win")) {
                return execCommand("explorer", path);
            } else if (os.contains("mac")) {
                return execCommand("open", path);
            } else {
                return execCommand("xdg-open", path);
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Open a file in the default editor.
     */
    public static boolean openInEditor(String filePath) {
        try {
            String editor = System.getenv("EDITOR");
            if (editor == null || editor.isEmpty()) {
                editor = System.getenv("VISUAL");
            }

            String os = System.getProperty("os.name").toLowerCase();

            if (editor != null && !editor.isEmpty()) {
                return execCommand(editor, filePath);
            }

            // Default editors by platform
            if (os.contains("mac")) {
                return execCommand("open", "-t", filePath);
            } else if (os.contains("win")) {
                return execCommand("notepad", filePath);
            } else {
                // Try common editors
                String[] editors = {"code", "nano", "vim", "vi"};
                for (String e : editors) {
                    if (execCommand(e, filePath)) {
                        return true;
                    }
                }
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Validate URL format and protocol.
     */
    public static void validateUrl(String url) {
        try {
            URI uri = new URI(url);
            String protocol = uri.getScheme();

            if (protocol == null || (!protocol.equals("http") && !protocol.equals("https"))) {
                throw new IllegalArgumentException("Invalid URL protocol: must use http:// or https://");
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL format: " + url, e);
        }
    }

    /**
     * Execute a command and return success status.
     */
    private static boolean execCommand(String... args) {
        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if a browser is available.
     */
    public static boolean isBrowserAvailable() {
        return Desktop.isDesktopSupported() &&
               Desktop.getDesktop().isSupported(Desktop.Action.BROWSE);
    }

    /**
     * Get the default browser name (if detectable).
     */
    public static String getDefaultBrowser() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("mac")) {
            return "Safari";
        } else if (os.contains("win")) {
            return "Edge";
        } else {
            return "Firefox";
        }
    }
}