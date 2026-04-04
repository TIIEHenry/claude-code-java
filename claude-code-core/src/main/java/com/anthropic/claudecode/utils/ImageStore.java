/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code image store utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Image store for caching pasted images to disk.
 */
public final class ImageStore {
    private ImageStore() {}

    private static final String IMAGE_STORE_DIR = "image-cache";
    private static final int MAX_STORED_IMAGE_PATHS = 200;

    // In-memory cache of stored image paths
    private static final Map<Integer, String> storedImagePaths = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
            return size() > MAX_STORED_IMAGE_PATHS;
        }
    };

    /**
     * Pasted content record.
     */
    public sealed interface PastedContent permits Image, Text {
        int id();
    }

    public static final class Image implements PastedContent {
        private final int id;
        private final String content; // base64
        private final String mediaType;

        public Image(int id, String content, String mediaType) {
            this.id = id;
            this.content = content;
            this.mediaType = mediaType != null ? mediaType : "image/png";
        }

        @Override public int id() { return id; }
        public String content() { return content; }
        public String mediaType() { return mediaType; }
    }

    public static final class Text implements PastedContent {
        private final int id;
        private final String content;

        public Text(int id, String content) {
            this.id = id;
            this.content = content;
        }

        @Override public int id() { return id; }
        public String content() { return content; }
    }

    /**
     * Get the image store directory for the current session.
     */
    public static Path getImageStoreDir() {
        String sessionId = getSessionId();
        return Paths.get(EnvUtils.getClaudeConfigHomeDir())
                .resolve(IMAGE_STORE_DIR)
                .resolve(sessionId);
    }

    /**
     * Ensure the image store directory exists.
     */
    public static void ensureImageStoreDir() throws IOException {
        Files.createDirectories(getImageStoreDir());
    }

    /**
     * Get the file path for an image by ID.
     */
    public static Path getImagePath(int imageId, String mediaType) {
        String extension = "png";
        if (mediaType != null && mediaType.contains("/")) {
            extension = mediaType.substring(mediaType.indexOf('/') + 1);
        }
        return getImageStoreDir().resolve(imageId + "." + extension);
    }

    /**
     * Cache the image path immediately.
     */
    public static String cacheImagePath(PastedContent content) {
        if (!(content instanceof Image)) {
            return null;
        }

        Image image = (Image) content;
        Path imagePath = getImagePath(image.id(), image.mediaType());

        synchronized (storedImagePaths) {
            storedImagePaths.put(image.id(), imagePath.toString());
        }

        return imagePath.toString();
    }

    /**
     * Store an image from pastedContents to disk.
     */
    public static CompletableFuture<String> storeImage(PastedContent content) {
        return CompletableFuture.supplyAsync(() -> {
            if (!(content instanceof Image)) {
                return null;
            }

            Image image = (Image) content;

            try {
                ensureImageStoreDir();
                Path imagePath = getImagePath(image.id(), image.mediaType());

                // Decode base64 and write to file
                byte[] imageBytes = Base64.getDecoder().decode(image.content());
                Files.write(imagePath, imageBytes,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);

                synchronized (storedImagePaths) {
                    storedImagePaths.put(image.id(), imagePath.toString());
                }

                Debug.logForDebugging("Stored image " + image.id() + " to " + imagePath);
                return imagePath.toString();

            } catch (Exception e) {
                Debug.logForDebugging("Failed to store image: " + e.getMessage());
                return null;
            }
        });
    }

    /**
     * Store all images from pastedContents to disk.
     */
    public static CompletableFuture<Map<Integer, String>> storeImages(Map<Integer, PastedContent> pastedContents) {
        Map<Integer, String> pathMap = new ConcurrentHashMap<>();

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (Map.Entry<Integer, PastedContent> entry : pastedContents.entrySet()) {
            if (entry.getValue() instanceof Image) {
                futures.add(storeImage(entry.getValue()).thenAccept(path -> {
                    if (path != null) {
                        pathMap.put(entry.getKey(), path);
                    }
                }));
            }
        }

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> pathMap);
    }

    /**
     * Get the file path for a stored image by ID.
     */
    public static String getStoredImagePath(int imageId) {
        synchronized (storedImagePaths) {
            return storedImagePaths.get(imageId);
        }
    }

    /**
     * Clear the in-memory cache of stored image paths.
     */
    public static void clearStoredImagePaths() {
        synchronized (storedImagePaths) {
            storedImagePaths.clear();
        }
    }

    /**
     * Clean up old image cache directories from previous sessions.
     */
    public static CompletableFuture<Void> cleanupOldImageCaches() {
        return CompletableFuture.runAsync(() -> {
            String currentSessionId = getSessionId();
            Path baseDir = Paths.get(EnvUtils.getClaudeConfigHomeDir()).resolve(IMAGE_STORE_DIR);

            try {
                if (!Files.exists(baseDir)) return;

                try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDir)) {
                    for (Path sessionDir : stream) {
                        String dirName = sessionDir.getFileName().toString();
                        if (!dirName.equals(currentSessionId)) {
                            try {
                                Files.walk(sessionDir)
                                        .sorted(Comparator.reverseOrder())
                                        .forEach(path -> {
                                            try {
                                                Files.delete(path);
                                            } catch (IOException e) {
                                                // Ignore
                                            }
                                        });
                                Debug.logForDebugging("Cleaned up old image cache: " + sessionDir);
                            } catch (Exception e) {
                                // Ignore errors for individual directories
                            }
                        }
                    }
                }

                // Remove base dir if empty
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(baseDir)) {
                    if (!stream.iterator().hasNext()) {
                        Files.delete(baseDir);
                    }
                }
            } catch (Exception e) {
                // Ignore errors reading base directory
            }
        });
    }

    /**
     * Get session ID.
     */
    private static String getSessionId() {
        String sessionId = System.getenv("CLAUDE_CODE_SESSION_ID");
        return sessionId != null ? sessionId : UUID.randomUUID().toString();
    }
}