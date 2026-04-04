/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code filesystem operations - default implementation
 */
package com.anthropic.claudecode.utils.fs;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

/**
 * Default Node-style filesystem operations implementation using Java NIO.
 */
public final class NodeFsOperations implements FsOperations {
    private static volatile FsOperations activeFs = new NodeFsOperations();

    private NodeFsOperations() {}

    /**
     * Get the currently active filesystem implementation.
     */
    public static FsOperations getFsImplementation() {
        return activeFs;
    }

    /**
     * Set the filesystem implementation.
     */
    public static void setFsImplementation(FsOperations impl) {
        activeFs = impl;
    }

    /**
     * Reset to default implementation.
     */
    public static void setOriginalFsImplementation() {
        activeFs = new NodeFsOperations();
    }

    @Override
    public String cwd() {
        return System.getProperty("user.dir");
    }

    @Override
    public boolean existsSync(String path) {
        return Files.exists(Paths.get(path));
    }

    @Override
    public CompletableFuture<BasicFileAttributes> stat(String path) {
        return CompletableFuture.supplyAsync(() -> statSync(path));
    }

    @Override
    public CompletableFuture<List<Dirent>> readdir(String path) {
        return CompletableFuture.supplyAsync(() -> readdirSync(path));
    }

    @Override
    public CompletableFuture<Void> unlink(String path) {
        return CompletableFuture.runAsync(() -> unlinkSync(path));
    }

    @Override
    public CompletableFuture<Void> rmdir(String path) {
        return CompletableFuture.runAsync(() -> rmdirSync(path));
    }

    @Override
    public CompletableFuture<Void> rm(String path, RmOptions options) {
        return CompletableFuture.runAsync(() -> rmSync(path, options));
    }

    @Override
    public CompletableFuture<Void> mkdir(String path, MkdirOptions options) {
        return CompletableFuture.runAsync(() -> mkdirSync(path, options));
    }

    @Override
    public CompletableFuture<String> readFile(String path, Charset encoding) {
        return CompletableFuture.supplyAsync(() -> readFileSync(path, encoding));
    }

    @Override
    public CompletableFuture<Void> rename(String oldPath, String newPath) {
        return CompletableFuture.runAsync(() -> renameSync(oldPath, newPath));
    }

    @Override
    public BasicFileAttributes statSync(String path) {
        try {
            return Files.readAttributes(Paths.get(path), BasicFileAttributes.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to stat: " + path, e);
        }
    }

    @Override
    public BasicFileAttributes lstatSync(String path) {
        try {
            return Files.readAttributes(Paths.get(path), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            throw new RuntimeException("Failed to lstat: " + path, e);
        }
    }

    @Override
    public String readFileSync(String path, Charset encoding) {
        try {
            return Files.readString(Paths.get(path), encoding);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + path, e);
        }
    }

    @Override
    public byte[] readFileBytesSync(String path) {
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file bytes: " + path, e);
        }
    }

    @Override
    public ReadResult readSync(String path, int length) {
        try (RandomAccessFile raf = new RandomAccessFile(path, "r")) {
            byte[] buffer = new byte[length];
            int bytesRead = raf.read(buffer, 0, length);
            return new ReadResult(buffer, bytesRead >= 0 ? bytesRead : 0);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read: " + path, e);
        }
    }

    @Override
    public void appendFileSync(String path, String data, AppendOptions options) {
        try {
            Path p = Paths.get(path);
            if (options != null && options.mode() != null && !Files.exists(p)) {
                // Create with mode for new files
                Set<OpenOption> openOptions = new HashSet<>();
                openOptions.add(StandardOpenOption.CREATE);
                openOptions.add(StandardOpenOption.APPEND);
                Files.write(p, data.getBytes(StandardCharsets.UTF_8), openOptions.toArray(new OpenOption[0]));
            } else {
                Files.write(p, data.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to append to file: " + path, e);
        }
    }

    @Override
    public void copyFileSync(String src, String dest) {
        try {
            Files.copy(Paths.get(src), Paths.get(dest), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy: " + src + " -> " + dest, e);
        }
    }

    @Override
    public void unlinkSync(String path) {
        try {
            Files.delete(Paths.get(path));
        } catch (IOException e) {
            throw new RuntimeException("Failed to unlink: " + path, e);
        }
    }

    @Override
    public void renameSync(String oldPath, String newPath) {
        try {
            Files.move(Paths.get(oldPath), Paths.get(newPath), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to rename: " + oldPath + " -> " + newPath, e);
        }
    }

    @Override
    public void linkSync(String target, String path) {
        try {
            Files.createLink(Paths.get(path), Paths.get(target));
        } catch (IOException e) {
            throw new RuntimeException("Failed to link: " + target + " -> " + path, e);
        }
    }

    @Override
    public void symlinkSync(String target, String path, String type) {
        try {
            Files.createSymbolicLink(Paths.get(path), Paths.get(target));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create symlink: " + target + " -> " + path, e);
        }
    }

    @Override
    public String readlinkSync(String path) {
        try {
            return Files.readSymbolicLink(Paths.get(path)).toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to readlink: " + path, e);
        }
    }

    @Override
    public String realpathSync(String path) {
        try {
            return Paths.get(path).toRealPath().toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to realpath: " + path, e);
        }
    }

    @Override
    public void mkdirSync(String path, MkdirOptions options) {
        try {
            Files.createDirectories(Paths.get(path));
        } catch (IOException e) {
            throw new RuntimeException("Failed to mkdir: " + path, e);
        }
    }

    @Override
    public List<Dirent> readdirSync(String path) {
        try (Stream<Path> stream = Files.list(Paths.get(path))) {
            return stream.map(p -> {
                try {
                    BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
                    return Dirent.fromPath(p, attrs);
                } catch (IOException e) {
                    return null;
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to readdir: " + path, e);
        }
    }

    @Override
    public List<String> readdirStringSync(String path) {
        try (Stream<Path> stream = Files.list(Paths.get(path))) {
            return stream.map(p -> p.getFileName().toString()).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Failed to readdir: " + path, e);
        }
    }

    @Override
    public boolean isDirEmptySync(String path) {
        try (Stream<Path> stream = Files.list(Paths.get(path))) {
            return stream.findFirst().isEmpty();
        } catch (IOException e) {
            return true;
        }
    }

    @Override
    public void rmdirSync(String path) {
        try {
            Files.delete(Paths.get(path));
        } catch (IOException e) {
            throw new RuntimeException("Failed to rmdir: " + path, e);
        }
    }

    @Override
    public void rmSync(String path, RmOptions options) {
        try {
            Path p = Paths.get(path);
            if (options != null && options.recursive()) {
                Files.walkFileTree(p, new SimpleFileVisitor<Path>() {
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
                Files.delete(p);
            }
        } catch (IOException e) {
            if (options == null || !options.force()) {
                throw new RuntimeException("Failed to rm: " + path, e);
            }
        }
    }

    @Override
    public OutputStream createWriteStream(String path) {
        try {
            return Files.newOutputStream(Paths.get(path), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create write stream: " + path, e);
        }
    }

    @Override
    public CompletableFuture<byte[]> readFileBytes(String path, Integer maxBytes) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (maxBytes == null) {
                    return Files.readAllBytes(Paths.get(path));
                }
                long size = Files.size(Paths.get(path));
                int readSize = (int) Math.min(size, maxBytes);
                byte[] buffer = new byte[readSize];
                try (InputStream is = Files.newInputStream(Paths.get(path))) {
                    int bytesRead = is.read(buffer, 0, readSize);
                    return bytesRead < readSize ? Arrays.copyOf(buffer, bytesRead) : buffer;
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to read file bytes: " + path, e);
            }
        });
    }
}