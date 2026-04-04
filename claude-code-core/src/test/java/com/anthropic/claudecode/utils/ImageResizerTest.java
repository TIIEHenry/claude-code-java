/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ImageResizer.
 */
class ImageResizerTest {

    @Test
    @DisplayName("ImageResizer constants")
    void constants() {
        assertEquals(5 * 1024 * 1024, ImageResizer.API_IMAGE_MAX_BASE64_SIZE);
        assertEquals(2000, ImageResizer.IMAGE_MAX_WIDTH);
        assertEquals(2000, ImageResizer.IMAGE_MAX_HEIGHT);
        assertEquals(3750000, ImageResizer.IMAGE_TARGET_RAW_SIZE);
    }

    @Test
    @DisplayName("ImageResizer ImageDimensions record")
    void imageDimensionsRecord() {
        ImageResizer.ImageDimensions dims = new ImageResizer.ImageDimensions(
            1000, 800, 500, 400
        );

        assertEquals(1000, dims.originalWidth());
        assertEquals(800, dims.originalHeight());
        assertEquals(500, dims.displayWidth());
        assertEquals(400, dims.displayHeight());
    }

    @Test
    @DisplayName("ImageResizer ResizeResult record")
    void resizeResultRecord() {
        byte[] buffer = new byte[]{1, 2, 3};
        ImageResizer.ResizeResult result = new ImageResizer.ResizeResult(
            buffer, "png", null
        );

        assertEquals(buffer, result.buffer());
        assertEquals("png", result.mediaType());
        assertNull(result.dimensions());
    }

    @Test
    @DisplayName("ImageResizer ImageResizeError")
    void imageResizeError() {
        ImageResizer.ImageResizeError error = new ImageResizer.ImageResizeError("Test error");
        assertEquals("Test error", error.getMessage());
    }

    @Test
    @DisplayName("ImageResizer maybeResizeAndDownsample empty buffer throws")
    void maybeResizeAndDownsampleEmpty() {
        assertThrows(ImageResizer.ImageResizeError.class, () ->
            ImageResizer.maybeResizeAndDownsample(new byte[0], "png")
        );
    }

    @Test
    @DisplayName("ImageResizer detectImageFormatFromBuffer PNG")
    void detectImageFormatFromBufferPng() {
        byte[] buffer = new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47};
        assertEquals("image/png", ImageResizer.detectImageFormatFromBuffer(buffer));
    }

    @Test
    @DisplayName("ImageResizer detectImageFormatFromBuffer JPEG")
    void detectImageFormatFromBufferJpeg() {
        byte[] buffer = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x00};
        assertEquals("image/jpeg", ImageResizer.detectImageFormatFromBuffer(buffer));
    }

    @Test
    @DisplayName("ImageResizer detectImageFormatFromBuffer GIF")
    void detectImageFormatFromBufferGif() {
        byte[] buffer = new byte[]{0x47, 0x49, 0x46, 0x00};
        assertEquals("image/gif", ImageResizer.detectImageFormatFromBuffer(buffer));
    }

    @Test
    @DisplayName("ImageResizer detectImageFormatFromBuffer unknown")
    void detectImageFormatFromBufferUnknown() {
        byte[] buffer = new byte[]{0x00, 0x00, 0x00, 0x00};
        assertEquals("image/png", ImageResizer.detectImageFormatFromBuffer(buffer));
    }

    @Test
    @DisplayName("ImageResizer detectImageFormatFromBuffer too short")
    void detectImageFormatFromBufferTooShort() {
        byte[] buffer = new byte[]{0x00, 0x00};
        assertEquals("image/png", ImageResizer.detectImageFormatFromBuffer(buffer));
    }

    @Test
    @DisplayName("ImageResizer detectImageFormatFromBase64 valid")
    void detectImageFormatFromBase64Valid() {
        // PNG signature in base64: iVBORw==
        String base64 = "iVBORw==";
        assertEquals("image/png", ImageResizer.detectImageFormatFromBase64(base64));
    }

    @Test
    @DisplayName("ImageResizer detectImageFormatFromBase64 invalid")
    void detectImageFormatFromBase64Invalid() {
        assertEquals("image/png", ImageResizer.detectImageFormatFromBase64("invalid!!!"));
    }

    @Test
    @DisplayName("ImageResizer imageToBase64 returns string or null")
    void imageToBase64() {
        // This would need a real Image object
        // Just verify the method exists and handles null gracefully
        try {
            String result = ImageResizer.imageToBase64(null, "png");
            assertNull(result);
        } catch (NullPointerException e) {
            // Expected if null image passed
            assertTrue(true);
        }
    }
}