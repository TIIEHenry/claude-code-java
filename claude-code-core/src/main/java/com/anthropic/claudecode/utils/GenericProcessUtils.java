/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code generic process utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 * Platform-agnostic implementations of common process utilities.
 */
public final class GenericProcessUtils {
    private GenericProcessUtils() {}

    private static final boolean IS_WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("win");

    /**
     * Check if a process with the given PID is running.
     */
    public static boolean isProcessRunning(long pid) {
        if (pid <= 1) return false;

        try {
            ProcessHandle handle = ProcessHandle.of(pid).orElse(null);
            return handle != null && handle.isAlive();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get ancestor process PIDs.
     */
    public static CompletableFuture<List<Long>> getAncestorPidsAsync(long pid, int maxDepth) {
        return CompletableFuture.supplyAsync(() -> {
            List<Long> ancestors = new ArrayList<>();
            long currentPid = pid;

            for (int i = 0; i < maxDepth; i++) {
                ProcessHandle handle = ProcessHandle.of(currentPid).orElse(null);
                if (handle == null) break;

                ProcessHandle.Info info = handle.info();
                Optional<ProcessHandle> parent = handle.parent();

                if (parent.isEmpty()) break;

                long parentPid = parent.get().pid();
                if (parentPid <= 1) break;

                ancestors.add(parentPid);
                currentPid = parentPid;
            }

            return ancestors;
        });
    }

    /**
     * Get command line for a process.
     */
    public static String getProcessCommand(long pid) {
        try {
            ProcessHandle handle = ProcessHandle.of(pid).orElse(null);
            if (handle == null) return null;

            ProcessHandle.Info info = handle.info();
            Optional<String[]> args = info.arguments();
            Optional<String> cmd = info.command();

            if (args.isPresent() && args.get().length > 0) {
                return String.join(" ", args.get());
            } else if (cmd.isPresent()) {
                return cmd.get();
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    /**
     * Get ancestor commands.
     */
    public static CompletableFuture<List<String>> getAncestorCommandsAsync(long pid, int maxDepth) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> commands = new ArrayList<>();
            long currentPid = pid;

            for (int i = 0; i < maxDepth; i++) {
                String cmd = getProcessCommand(currentPid);
                if (cmd != null && !cmd.isEmpty()) {
                    commands.add(cmd);
                }

                ProcessHandle handle = ProcessHandle.of(currentPid).orElse(null);
                if (handle == null) break;

                Optional<ProcessHandle> parent = handle.parent();
                if (parent.isEmpty()) break;

                long parentPid = parent.get().pid();
                if (parentPid <= 1) break;

                currentPid = parentPid;
            }

            return commands;
        });
    }

    /**
     * Get child process PIDs.
     */
    public static List<Long> getChildPids(long pid) {
        List<Long> children = new ArrayList<>();

        try {
            ProcessHandle.of(pid).ifPresent(handle -> {
                handle.children().forEach(child -> {
                    children.add(child.pid());
                });
            });
        } catch (Exception e) {
            // Ignore
        }

        return children;
    }

    /**
     * Kill a process tree (process and all children).
     */
    public static boolean killProcessTree(long pid) {
        try {
            ProcessHandle handle = ProcessHandle.of(pid).orElse(null);
            if (handle == null) return false;

            // Kill children first
            handle.children().forEach(child -> {
                killProcessTree(child.pid());
            });

            // Then kill the process itself
            return handle.destroyForcibly();
        } catch (Exception e) {
            return false;
        }
    }
}