/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code filesystem operations interface
 */
package com.anthropic.claudecode.utils.fs;

import java.util.concurrent.CompletableFuture;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;

/**
 * Simplified filesystem operations interface.
 * Provides a subset of commonly used operations with type safety.
 * Allows abstraction for alternative implementations (e.g., mock, virtual).
 */
public interface FsOperations {

    // === File access and information operations ===

    /**
     * Gets the current working directory.
     */
    String cwd();

    /**
     * Checks if a file or directory exists.
     */
    boolean existsSync(String path);

    /**
     * Gets file stats asynchronously.
     */
    CompletableFuture<BasicFileAttributes> stat(String path);

    /**
     * Lists directory contents with file type information asynchronously.
     */
    CompletableFuture<List<Dirent>> readdir(String path);

    /**
     * Deletes file asynchronously.
     */
    CompletableFuture<Void> unlink(String path);

    /**
     * Removes an empty directory asynchronously.
     */
    CompletableFuture<Void> rmdir(String path);

    /**
     * Removes files and directories asynchronously (with recursive option).
     */
    CompletableFuture<Void> rm(String path, RmOptions options);

    /**
     * Creates directory recursively asynchronously.
     */
    CompletableFuture<Void> mkdir(String path, MkdirOptions options);

    /**
     * Reads file content as string asynchronously.
     */
    CompletableFuture<String> readFile(String path, Charset encoding);

    /**
     * Renames/moves file asynchronously.
     */
    CompletableFuture<Void> rename(String oldPath, String newPath);

    /**
     * Gets file stats.
     */
    BasicFileAttributes statSync(String path);

    /**
     * Gets file stats without following symlinks.
     */
    BasicFileAttributes lstatSync(String path);

    // === File content operations ===

    /**
     * Reads file content as string with specified encoding.
     */
    String readFileSync(String path, Charset encoding);

    /**
     * Reads raw file bytes.
     */
    byte[] readFileBytesSync(String path);

    /**
     * Reads specified number of bytes from file start.
     */
    ReadResult readSync(String path, int length);

    /**
     * Appends string to file.
     */
    void appendFileSync(String path, String data, AppendOptions options);

    /**
     * Copies file from source to destination.
     */
    void copyFileSync(String src, String dest);

    /**
     * Deletes file.
     */
    void unlinkSync(String path);

    /**
     * Renames/moves file.
     */
    void renameSync(String oldPath, String newPath);

    /**
     * Creates hard link.
     */
    void linkSync(String target, String path);

    /**
     * Creates symbolic link.
     */
    void symlinkSync(String target, String path, String type);

    /**
     * Reads symbolic link.
     */
    String readlinkSync(String path);

    /**
     * Resolves symbolic links and returns the canonical pathname.
     */
    String realpathSync(String path);

    // === Directory operations ===

    /**
     * Creates directory recursively.
     */
    void mkdirSync(String path, MkdirOptions options);

    /**
     * Lists directory contents with file type information.
     */
    List<Dirent> readdirSync(String path);

    /**
     * Lists directory contents as strings.
     */
    List<String> readdirStringSync(String path);

    /**
     * Checks if the directory is empty.
     */
    boolean isDirEmptySync(String path);

    /**
     * Removes an empty directory.
     */
    void rmdirSync(String path);

    /**
     * Removes files and directories (with recursive option).
     */
    void rmSync(String path, RmOptions options);

    /**
     * Create a writable stream for writing data to a file.
     */
    OutputStream createWriteStream(String path);

    /**
     * Reads raw file bytes asynchronously.
     */
    CompletableFuture<byte[]> readFileBytes(String path, Integer maxBytes);

    // === Supporting types ===

    /**
     * Directory entry with name and type.
     */
    record Dirent(
            String name,
            boolean isFile,
            boolean isDirectory,
            boolean isSymbolicLink,
            boolean isOther
    ) {
        public static Dirent fromPath(Path path, BasicFileAttributes attrs) {
            return new Dirent(
                    path.getFileName().toString(),
                    attrs.isRegularFile(),
                    attrs.isDirectory(),
                    attrs.isSymbolicLink(),
                    attrs.isOther()
            );
        }
    }

    /**
     * Read result with buffer and bytes read.
     */
    record ReadResult(byte[] buffer, int bytesRead) {}

    /**
     * Remove options.
     */
    record RmOptions(boolean recursive, boolean force) {
        public static RmOptions DEFAULT = new RmOptions(false, false);
        public static RmOptions RECURSIVE = new RmOptions(true, false);
        public static RmOptions FORCE = new RmOptions(false, true);
        public static RmOptions RECURSIVE_FORCE = new RmOptions(true, true);
    }

    /**
     * Mkdir options.
     */
    record MkdirOptions(Integer mode) {
        public static MkdirOptions DEFAULT = new MkdirOptions(null);
    }

    /**
     * Append options.
     */
    record AppendOptions(Integer mode) {
        public static AppendOptions DEFAULT = new AppendOptions(null);
    }
}