/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code image validation utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Image validation utilities for API limits.
 */
public final class ImageValidation {
    private ImageValidation() {}

    // API limits
    public static final int API_IMAGE_MAX_BASE64_SIZE = 5 * 1024 * 1024; // 5MB

    /**
     * Oversized image info.
     */
    public record OversizedImage(int index, int size) {}

    /**
     * Exception thrown when images exceed size limits.
     */
    public static class ImageSizeError extends RuntimeException {
        private final List<OversizedImage> oversizedImages;
        private final int maxSize;

        public ImageSizeError(List<OversizedImage> oversizedImages, int maxSize) {
            super(buildMessage(oversizedImages, maxSize));
            this.oversizedImages = oversizedImages;
            this.maxSize = maxSize;
        }

        public List<OversizedImage> getOversizedImages() {
            return oversizedImages;
        }

        public int getMaxSize() {
            return maxSize;
        }

        private static String buildMessage(List<OversizedImage> images, int maxSize) {
            if (images.size() == 1) {
                OversizedImage img = images.get(0);
                return String.format(
                        "Image base64 size (%s) exceeds API limit (%s). Please resize the image before sending.",
                        Format.formatFileSize(img.size()),
                        Format.formatFileSize(maxSize)
                );
            }

            StringBuilder sb = new StringBuilder();
            sb.append(images.size()).append(" images exceed the API limit (")
                    .append(Format.formatFileSize(maxSize)).append("): ");

            for (int i = 0; i < images.size(); i++) {
                if (i > 0) sb.append(", ");
                OversizedImage img = images.get(i);
                sb.append("Image ").append(img.index()).append(": ")
                        .append(Format.formatFileSize(img.size()));
            }

            sb.append(". Please resize these images before sending.");
            return sb.toString();
        }
    }

    /**
     * Validate images for API size limits.
     */
    public static void validateImagesForAPI(List<Map<String, Object>> messages) {
        List<OversizedImage> oversizedImages = new ArrayList<>();
        int imageIndex = 0;

        for (Map<String, Object> msg : messages) {
            String type = (String) msg.get("type");
            if (!"user".equals(type)) continue;

            Object messageObj = msg.get("message");
            if (!(messageObj instanceof Map)) continue;

            @SuppressWarnings("unchecked")
            Map<String, Object> innerMessage = (Map<String, Object>) messageObj;
            Object content = innerMessage.get("content");

            if (content instanceof String || !(content instanceof List)) continue;

            @SuppressWarnings("unchecked")
            List<Object> contentList = (List<Object>) content;

            for (Object block : contentList) {
                if (isBase64ImageBlock(block)) {
                    imageIndex++;
                    String base64Data = extractBase64Data(block);
                    if (base64Data != null && base64Data.length() > API_IMAGE_MAX_BASE64_SIZE) {
                        Analytics.logEvent("tengu_image_api_validation_failed", Map.of(
                                "base64_size_bytes", base64Data.length(),
                                "max_bytes", API_IMAGE_MAX_BASE64_SIZE
                        ));
                        oversizedImages.add(new OversizedImage(imageIndex, base64Data.length()));
                    }
                }
            }
        }

        if (!oversizedImages.isEmpty()) {
            throw new ImageSizeError(oversizedImages, API_IMAGE_MAX_BASE64_SIZE);
        }
    }

    /**
     * Check if a block is a base64 image block.
     */
    private static boolean isBase64ImageBlock(Object block) {
        if (!(block instanceof Map)) return false;

        @SuppressWarnings("unchecked")
        Map<String, Object> b = (Map<String, Object>) block;

        if (!"image".equals(b.get("type"))) return false;

        Object source = b.get("source");
        if (!(source instanceof Map)) return false;

        @SuppressWarnings("unchecked")
        Map<String, Object> sourceMap = (Map<String, Object>) source;

        return "base64".equals(sourceMap.get("type")) && sourceMap.get("data") instanceof String;
    }

    /**
     * Extract base64 data from an image block.
     */
    private static String extractBase64Data(Object block) {
        if (!(block instanceof Map)) return null;

        @SuppressWarnings("unchecked")
        Map<String, Object> b = (Map<String, Object>) block;
        Object source = b.get("source");

        if (!(source instanceof Map)) return null;

        @SuppressWarnings("unchecked")
        Map<String, Object> sourceMap = (Map<String, Object>) source;
        Object data = sourceMap.get("data");

        return data instanceof String ? (String) data : null;
    }
}