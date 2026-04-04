/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code image resizer utilities
 */
package com.anthropic.claudecode.utils;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.imageio.*;

/**
 * Image resizing and compression utilities.
 */
public final class ImageResizer {
    private ImageResizer() {}

    // API limits
    public static final int API_IMAGE_MAX_BASE64_SIZE = 5 * 1024 * 1024; // 5MB
    public static final int IMAGE_MAX_WIDTH = 2000;
    public static final int IMAGE_MAX_HEIGHT = 2000;
    public static final int IMAGE_TARGET_RAW_SIZE = 3750000; // ~3.75MB

    /**
     * Image dimensions.
     */
    public record ImageDimensions(
            Integer originalWidth,
            Integer originalHeight,
            Integer displayWidth,
            Integer displayHeight
    ) {}

    /**
     * Resize result.
     */
    public record ResizeResult(byte[] buffer, String mediaType, ImageDimensions dimensions) {}

    /**
     * Error thrown when image resizing fails.
     */
    public static class ImageResizeError extends RuntimeException {
        public ImageResizeError(String message) {
            super(message);
        }
    }

    /**
     * Maybe resize and downsample an image buffer.
     */
    public static ResizeResult maybeResizeAndDownsample(byte[] imageBuffer, String ext) {
        if (imageBuffer.length == 0) {
            throw new ImageResizeError("Image file is empty (0 bytes)");
        }

        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(imageBuffer);
            BufferedImage image = ImageIO.read(bis);

            if (image == null) {
                // Can't read image - return as-is
                return new ResizeResult(imageBuffer, ext, null);
            }

            int originalWidth = image.getWidth();
            int originalHeight = image.getHeight();
            String mediaType = normalizeMediaType(ext);

            // Check if original works
            if (imageBuffer.length <= IMAGE_TARGET_RAW_SIZE &&
                originalWidth <= IMAGE_MAX_WIDTH &&
                originalHeight <= IMAGE_MAX_HEIGHT) {
                return new ResizeResult(imageBuffer, mediaType, new ImageDimensions(
                        originalWidth, originalHeight, originalWidth, originalHeight
                ));
            }

            // Calculate new dimensions
            int width = originalWidth;
            int height = originalHeight;

            if (width > IMAGE_MAX_WIDTH) {
                height = (int) Math.round((height * IMAGE_MAX_WIDTH) / (double) width);
                width = IMAGE_MAX_WIDTH;
            }

            if (height > IMAGE_MAX_HEIGHT) {
                width = (int) Math.round((width * IMAGE_MAX_HEIGHT) / (double) height);
                height = IMAGE_MAX_HEIGHT;
            }

            Debug.log("Resizing to " + width + "x" + height);

            // Resize image
            BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = resized.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(image, 0, 0, width, height, null);
            g2d.dispose();

            // Write to buffer
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            String outputFormat = "jpeg".equals(mediaType) || "jpg".equals(mediaType) ? "jpg" : "png";
            ImageIO.write(resized, outputFormat, bos);
            byte[] resultBuffer = bos.toByteArray();

            // If still too large, try more aggressive compression
            if (resultBuffer.length > IMAGE_TARGET_RAW_SIZE) {
                // Try smaller size
                int smallerWidth = Math.min(width, 1000);
                int smallerHeight = (int) Math.round((height * smallerWidth) / (double) width);

                BufferedImage smaller = new BufferedImage(smallerWidth, smallerHeight, BufferedImage.TYPE_INT_RGB);
                g2d = smaller.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(image, 0, 0, smallerWidth, smallerHeight, null);
                g2d.dispose();

                bos = new ByteArrayOutputStream();
                ImageIO.write(smaller, "jpg", bos);
                resultBuffer = bos.toByteArray();

                return new ResizeResult(resultBuffer, "jpeg", new ImageDimensions(
                        originalWidth, originalHeight, smallerWidth, smallerHeight
                ));
            }

            return new ResizeResult(resultBuffer, mediaType, new ImageDimensions(
                    originalWidth, originalHeight, width, height
            ));
        } catch (Exception e) {
            Debug.log("Image resize failed: " + e.getMessage());

            // Check if original is acceptable
            int base64Size = (int) Math.ceil((imageBuffer.length * 4) / 3.0);
            if (base64Size <= API_IMAGE_MAX_BASE64_SIZE) {
                return new ResizeResult(imageBuffer, ext, null);
            }

            throw new ImageResizeError(
                    "Unable to resize image (" + Format.formatFileSize(imageBuffer.length) + "). " +
                    "The image exceeds the 5MB API limit. Please resize manually."
            );
        }
    }

    /**
     * Convert image to base64.
     */
    public static String imageToBase64(Image image, String format) {
        try {
            BufferedImage bi = new BufferedImage(
                    image.getWidth(null),
                    image.getHeight(null),
                    BufferedImage.TYPE_INT_RGB
            );
            Graphics2D g2d = bi.createGraphics();
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(bi, format, bos);
            return Base64.getEncoder().encodeToString(bos.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Detect image format from buffer magic bytes.
     */
    public static String detectImageFormatFromBuffer(byte[] buffer) {
        if (buffer.length < 4) return "image/png";

        // PNG signature
        if (buffer[0] == (byte) 0x89 && buffer[1] == 0x50 && buffer[2] == 0x4e && buffer[3] == 0x47) {
            return "image/png";
        }

        // JPEG signature
        if ((buffer[0] & 0xFF) == 0xFF && (buffer[1] & 0xFF) == 0xD8 && (buffer[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }

        // GIF signature
        if (buffer[0] == 0x47 && buffer[1] == 0x49 && buffer[2] == 0x46) {
            return "image/gif";
        }

        // WebP signature
        if (buffer[0] == 0x52 && buffer[1] == 0x49 && buffer[2] == 0x46 && buffer[3] == 0x46) {
            if (buffer.length >= 12 && buffer[8] == 0x57 && buffer[9] == 0x45 && buffer[10] == 0x42 && buffer[11] == 0x50) {
                return "image/webp";
            }
        }

        return "image/png";
    }

    /**
     * Detect image format from base64.
     */
    public static String detectImageFormatFromBase64(String base64Data) {
        try {
            byte[] buffer = Base64.getDecoder().decode(base64Data);
            return detectImageFormatFromBuffer(buffer);
        } catch (Exception e) {
            return "image/png";
        }
    }

    /**
     * Normalize media type.
     */
    private static String normalizeMediaType(String ext) {
        if (ext == null) return "png";
        String lower = ext.toLowerCase();
        if ("jpg".equals(lower)) return "jpeg";
        return lower;
    }
}