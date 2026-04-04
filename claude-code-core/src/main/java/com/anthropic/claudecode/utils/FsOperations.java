/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code filesystem operations
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.*;

/**
 * Filesystem operations interface and implementation.
 */
public final class FsOperations {
    private FsOperations() {}

    private static FsOperationsImpl activeFs = new DefaultFsOperations();

    /**
     * Get the active filesystem implementation.
     */
    public static FsOperationsImpl getFsImplementation() {
        return activeFs;
    }

    /**
     * Set the filesystem implementation.
     */
    public static void setFsImplementation(FsOperationsImpl impl) {
        activeFs = impl;
    }

    /**
     * Reset to default implementation.
     */
    public static void setOriginalFsImplementation() {
        activeFs = new DefaultFsOperations();
    }

    /**
     * Safely resolve a file path, handling symlinks.
     */
    public static ResolvedPath safeResolvePath(FsOperationsImpl fs, String filePath) {
        // Block UNC paths on Windows
        if (filePath.startsWith("//") || filePath.startsWith("\\\\")) {
            return new ResolvedPath(filePath, false, false);
        }

        try {
            Path path = Paths.get(filePath);
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

            // Skip special file types
            if (attrs.isOther()) {
                return new ResolvedPath(filePath, false, false);
            }

            Path resolved = path.toRealPath();
            return new ResolvedPath(
                    resolved.toString(),
                    !resolved.equals(path),
                    true
            );
        } catch (IOException e) {
            return new ResolvedPath(filePath, false, false);
        }
    }

    /**
     * Check if path is duplicate.
     */
    public static boolean isDuplicatePath(FsOperationsImpl fs, String filePath, Set<String> loadedPaths) {
        ResolvedPath resolved = safeResolvePath(fs, filePath);
        if (loadedPaths.contains(resolved.resolvedPath())) {
            return true;
        }
        loadedPaths.add(resolved.resolvedPath());
        return false;
    }

    /**
     * Get paths for permission check.
     */
    public static List<String> getPathsForPermissionCheck(String inputPath) {
        Set<String> pathSet = new LinkedHashSet<>();

        // Expand tilde
        String path = inputPath;
        if (path.equals("~")) {
            path = System.getProperty("user.home");
        } else if (path.startsWith("~/")) {
            path = Paths.get(System.getProperty("user.home"), path.substring(2)).toString();
        }

        pathSet.add(path);

        // Block UNC paths
        if (path.startsWith("//") || path.startsWith("\\\\")) {
            return new ArrayList<>(pathSet);
        }

        try {
            Path currentPath = Paths.get(path).toAbsolutePath();
            Set<Path> visited = new HashSet<>();
            int maxDepth = 40;

            for (int depth = 0; depth < maxDepth; depth++) {
                if (visited.contains(currentPath)) break;
                visited.add(currentPath);

                if (!Files.exists(currentPath)) break;

                if (!Files.isSymbolicLink(currentPath)) break;

                Path target = Files.readSymbolicLink(currentPath);
                if (!target.isAbsolute()) {
                    target = currentPath.getParent().resolve(target);
                }

                pathSet.add(target.toString());
                currentPath = target;
            }
        } catch (IOException e) {
            // Continue with what we have
        }

        // Add final resolved path
        ResolvedPath resolved = safeResolvePath(activeFs, path);
        if (resolved.isSymlink() && !resolved.resolvedPath().equals(path)) {
            pathSet.add(resolved.resolvedPath());
        }

        return new ArrayList<>(pathSet);
    }

    /**
     * Read file range.
     */
    public static ReadRangeResult readFileRange(String path, long offset, int maxBytes) throws IOException {
        try (SeekableByteChannel channel = Files.newByteChannel(Paths.get(path), StandardOpenOption.READ)) {
            long size = channel.size();
            if (size <= offset) {
                return null;
            }

            int bytesToRead = (int) Math.min(size - offset, maxBytes);
            ByteBuffer buffer = ByteBuffer.allocate(bytesToRead);
            channel.position(offset);

            int totalRead = 0;
            while (totalRead < bytesToRead) {
                int bytesRead = channel.read(buffer);
                if (bytesRead <= 0) break;
                totalRead += bytesRead;
            }

            return new ReadRangeResult(
                    new String(buffer.array(), 0, totalRead, java.nio.charset.StandardCharsets.UTF_8),
                    totalRead,
                    size
            );
        }
    }

    /**
     * Tail file (read last bytes).
     */
    public static ReadRangeResult tailFile(String path, int maxBytes) throws IOException {
        Path filePath = Paths.get(path);
        long size = Files.size(filePath);

        if (size == 0) {
            return new ReadRangeResult("", 0, 0);
        }

        long offset = Math.max(0, size - maxBytes);
        return readFileRange(path, offset, maxBytes);
    }

    /**
     * Resolved path result.
     */
    public record ResolvedPath(String resolvedPath, boolean isSymlink, boolean isCanonical) {}

    /**
     * Read range result.
     */
    public record ReadRangeResult(String content, int bytesRead, long bytesTotal) {}

    /**
     * Filesystem operations interface.
     */
    public interface FsOperationsImpl {
        String cwd();
        boolean existsSync(String path);
        long size(String path) throws IOException;
        String readFileSync(String path, String encoding) throws IOException;
        void writeFileSync(String path, String content) throws IOException;
        void mkdirSync(String path) throws IOException;
        void rmSync(String path, boolean recursive) throws IOException;
        List<String> readdirSync(String path) throws IOException;
    }

    /**
     * Default filesystem operations implementation.
     */
    public static class DefaultFsOperations implements FsOperationsImpl {
        @Override
        public String cwd() {
            return System.getProperty("user.dir");
        }

        @Override
        public boolean existsSync(String path) {
            return Files.exists(Paths.get(path));
        }

        @Override
        public long size(String path) throws IOException {
            return Files.size(Paths.get(path));
        }

        @Override
        public String readFileSync(String path, String encoding) throws IOException {
            return Files.readString(Paths.get(path), java.nio.charset.Charset.forName(encoding));
        }

        @Override
        public void writeFileSync(String path, String content) throws IOException {
            Files.writeString(Paths.get(path), content);
        }

        @Override
        public void mkdirSync(String path) throws IOException {
            Files.createDirectories(Paths.get(path));
        }

        @Override
        public void rmSync(String path, boolean recursive) throws IOException {
            if (recursive) {
                Files.walkFileTree(Paths.get(path), new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                Files.delete(Paths.get(path));
            }
        }

        @Override
        public List<String> readdirSync(String path) throws IOException {
            try (java.util.stream.Stream<Path> stream = Files.list(Paths.get(path))) {
                return stream.map(p -> p.getFileName().toString())
                        .collect(Collectors.toList());
            }
        }
    }
}