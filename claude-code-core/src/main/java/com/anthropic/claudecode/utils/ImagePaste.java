/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code image paste utilities
 */
package com.anthropic.claudecode.utils;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Image paste utilities for clipboard operations.
 */
public final class ImagePaste {
    private ImagePaste() {}

    public static final int PASTE_THRESHOLD = 800;

    /**
     * Image extension regex.
     */
    public static final Pattern IMAGE_EXTENSION_REGEX = Pattern.compile("\\.(png|jpe?g|gif|webp)$", Pattern.CASE_INSENSITIVE);

    /**
     * Image with dimensions.
     */
    public record ImageWithDimensions(String base64, String mediaType, ImageDimensions dimensions) {}

    /**
     * Image dimensions.
     */
    public record ImageDimensions(int originalWidth, int originalHeight, int displayWidth, int displayHeight) {}

    /**
     * Check if clipboard contains an image.
     */
    public static boolean hasImageInClipboard() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            return clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get image from clipboard.
     */
    public static ImageWithDimensions getImageFromClipboard() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            Image image = (Image) clipboard.getData(DataFlavor.imageFlavor);

            if (image == null) {
                return null;
            }

            // Get dimensions
            int width = image.getWidth(null);
            int height = image.getHeight(null);

            // Convert to base64
            // Note: This is a simplified implementation
            // A full implementation would use ImageIO to write to PNG
            String base64 = ImageResizer.imageToBase64(image, "png");

            return new ImageWithDimensions(
                    base64,
                    "image/png",
                    new ImageDimensions(width, height, width, height)
            );
        } catch (Exception e) {
            Debug.log("Failed to get image from clipboard: " + e.getMessage());
            return null;
        }
    }

    /**
     * Check if text is an image file path.
     */
    public static boolean isImageFilePath(String text) {
        String cleaned = removeOuterQuotes(text.trim());
        String unescaped = stripBackslashEscapes(cleaned);
        return IMAGE_EXTENSION_REGEX.matcher(unescaped).find();
    }

    /**
     * Clean and normalize a text string that might be an image file path.
     */
    public static String asImageFilePath(String text) {
        String cleaned = removeOuterQuotes(text.trim());
        String unescaped = stripBackslashEscapes(cleaned);

        if (IMAGE_EXTENSION_REGEX.matcher(unescaped).find()) {
            return unescaped;
        }

        return null;
    }

    /**
     * Try to read an image from a file path.
     */
    public static ImageWithDimensions tryReadImageFromPath(String text) {
        String cleanedPath = asImageFilePath(text);

        if (cleanedPath == null) {
            return null;
        }

        try {
            Path imagePath = Paths.get(cleanedPath);
            if (!Files.exists(imagePath)) {
                return null;
            }

            byte[] imageBytes = Files.readAllBytes(imagePath);

            if (imageBytes.length == 0) {
                Debug.log("Image file is empty: " + cleanedPath);
                return null;
            }

            // Get extension for format hint
            String ext = cleanedPath.substring(cleanedPath.lastIndexOf('.') + 1).toLowerCase();
            if (ext.isEmpty()) ext = "png";

            // Resize if needed
            ImageResizer.ResizeResult resized = ImageResizer.maybeResizeAndDownsample(imageBytes, ext);

            String base64 = Base64.getEncoder().encodeToString(resized.buffer());
            String mediaType = "image/" + resized.mediaType();

            // Convert ImageResizer.ImageDimensions to ImagePaste.ImageDimensions
            ImageResizer.ImageDimensions resizerDims = resized.dimensions();
            ImageDimensions pasteDims = resizerDims != null
                ? new ImageDimensions(
                    resizerDims.originalWidth() != null ? resizerDims.originalWidth() : 0,
                    resizerDims.originalHeight() != null ? resizerDims.originalHeight() : 0,
                    resizerDims.displayWidth() != null ? resizerDims.displayWidth() : 0,
                    resizerDims.displayHeight() != null ? resizerDims.displayHeight() : 0)
                : null;

            return new ImageWithDimensions(base64, mediaType, pasteDims);
        } catch (Exception e) {
            Debug.log("Failed to read image from path: " + e.getMessage());
            return null;
        }
    }

    /**
     * Remove outer single or double quotes from a string.
     */
    private static String removeOuterQuotes(String text) {
        if ((text.startsWith("\"") && text.endsWith("\"")) ||
            (text.startsWith("'") && text.endsWith("'"))) {
            return text.substring(1, text.length() - 1);
        }
        return text;
    }

    /**
     * Remove shell escape backslashes from a path.
     */
    private static String stripBackslashEscapes(String path) {
        String osName = System.getProperty("os.name").toLowerCase();

        // On Windows, don't remove backslashes
        if (osName.contains("win")) {
            return path;
        }

        // Handle shell-escaped paths
        String salt = UUID.randomUUID().toString().substring(0, 8);
        String placeholder = "__DOUBLE_BACKSLASH_" + salt + "__";
        String withPlaceholder = path.replace("\\\\", placeholder);
        String withoutEscapes = withPlaceholder.replace("\\", "");
        return withoutEscapes.replace(placeholder, "\\");
    }
}