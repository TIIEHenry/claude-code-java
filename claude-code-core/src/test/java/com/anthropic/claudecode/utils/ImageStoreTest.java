/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ImageStore.
 */
class ImageStoreTest {

    @BeforeEach
    void setUp() {
        ImageStore.clearStoredImagePaths();
    }

    @Test
    @DisplayName("ImageStore Image record")
    void imageRecord() {
        ImageStore.Image image = new ImageStore.Image(1, "base64content", "image/png");

        assertEquals(1, image.id());
        assertEquals("base64content", image.content());
        assertEquals("image/png", image.mediaType());
    }

    @Test
    @DisplayName("ImageStore Image default media type")
    void imageDefaultMediaType() {
        ImageStore.Image image = new ImageStore.Image(1, "content", null);
        assertEquals("image/png", image.mediaType());
    }

    @Test
    @DisplayName("ImageStore Text record")
    void textRecord() {
        ImageStore.Text text = new ImageStore.Text(2, "text content");

        assertEquals(2, text.id());
        assertEquals("text content", text.content());
    }

    @Test
    @DisplayName("ImageStore getImageStoreDir returns path")
    void getImageStoreDir() {
        assertNotNull(ImageStore.getImageStoreDir());
        assertTrue(ImageStore.getImageStoreDir().toString().contains("image-cache"));
    }

    @Test
    @DisplayName("ImageStore getImagePath returns path")
    void getImagePath() {
        java.nio.file.Path path = ImageStore.getImagePath(123, "image/png");
        assertTrue(path.toString().contains("123.png"));
    }

    @Test
    @DisplayName("ImageStore getImagePath jpeg")
    void getImagePathJpeg() {
        java.nio.file.Path path = ImageStore.getImagePath(456, "image/jpeg");
        assertTrue(path.toString().contains("456.jpeg"));
    }

    @Test
    @DisplayName("ImageStore cacheImagePath for image")
    void cacheImagePathImage() {
        ImageStore.Image image = new ImageStore.Image(100, "content", "image/png");
        String path = ImageStore.cacheImagePath(image);

        assertNotNull(path);
        assertEquals(path, ImageStore.getStoredImagePath(100));
    }

    @Test
    @DisplayName("ImageStore cacheImagePath for text returns null")
    void cacheImagePathText() {
        ImageStore.Text text = new ImageStore.Text(101, "text");
        String path = ImageStore.cacheImagePath(text);

        assertNull(path);
    }

    @Test
    @DisplayName("ImageStore getStoredImagePath missing")
    void getStoredImagePathMissing() {
        assertNull(ImageStore.getStoredImagePath(999));
    }

    @Test
    @DisplayName("ImageStore clearStoredImagePaths")
    void clearStoredImagePaths() {
        ImageStore.Image image = new ImageStore.Image(200, "content", "image/png");
        ImageStore.cacheImagePath(image);

        assertNotNull(ImageStore.getStoredImagePath(200));

        ImageStore.clearStoredImagePaths();

        assertNull(ImageStore.getStoredImagePath(200));
    }

    @Test
    @DisplayName("ImageStore storeImage returns future")
    void storeImageReturnsFuture() {
        ImageStore.Image image = new ImageStore.Image(300, "dGVzdA==", "image/png"); // "test" in base64

        CompletableFuture<String> future = ImageStore.storeImage(image);
        assertNotNull(future);
    }

    @Test
    @DisplayName("ImageStore storeImage for text returns null")
    void storeImageText() throws Exception {
        ImageStore.Text text = new ImageStore.Text(301, "text");

        CompletableFuture<String> future = ImageStore.storeImage(text);
        String result = future.get(5, java.util.concurrent.TimeUnit.SECONDS);
        assertNull(result);
    }

    @Test
    @DisplayName("ImageStore storeImages returns future")
    void storeImagesReturnsFuture() {
        Map<Integer, ImageStore.PastedContent> contents = Map.of(
            400, new ImageStore.Image(400, "dGVzdA==", "image/png")
        );

        CompletableFuture<Map<Integer, String>> future = ImageStore.storeImages(contents);
        assertNotNull(future);
    }

    @Test
    @DisplayName("ImageStore cleanupOldImageCaches returns future")
    void cleanupOldImageCachesReturnsFuture() {
        CompletableFuture<Void> future = ImageStore.cleanupOldImageCaches();
        assertNotNull(future);
    }
}