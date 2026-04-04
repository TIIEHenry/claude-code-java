/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 *
 * Java port of Claude Code ReadImage tool
 */
package com.anthropic.claudecode.tools;

import com.anthropic.claudecode.*;
import com.anthropic.claudecode.message.*;
import com.anthropic.claudecode.utils.schema.SchemaUtils;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Read image tool for reading image files.
 */
public class ReadImageTool implements Tool<ReadImageTool.Input, Object, ToolProgressData> {

    public record Input(String filePath) {}

    @Override
    public String name() {
        return "ReadImage";
    }

    @Override
    public String description() {
        return "Read and display image files (PNG, JPG, etc.)";
    }

    @Override
    public Map<String, Object> inputSchema() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("filePath", SchemaUtils.stringSchema());
        return SchemaUtils.objectSchema(properties, List.of("filePath"));
    }

    @Override
    public CompletableFuture<ToolResult<Object>> call(
            Input input,
            ToolUseContext context,
            CanUseToolFn canUseTool,
            AssistantMessage parentMessage,
            Consumer<ToolProgress<ToolProgressData>> onProgress) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                Path path = Paths.get(input.filePath());

                if (!Files.exists(path)) {
                    return ToolResult.of(Map.of("error", "Image file not found: " + input.filePath()));
                }

                // Detect MIME type
                String mimeType = Files.probeContentType(path);
                if (mimeType == null || !mimeType.startsWith("image/")) {
                    // Try from extension
                    String extension = getExtension(path).toLowerCase();
                    mimeType = switch (extension) {
                        case "png" -> "image/png";
                        case "jpg", "jpeg" -> "image/jpeg";
                        case "gif" -> "image/gif";
                        case "webp" -> "image/webp";
                        case "svg" -> "image/svg+xml";
                        default -> "application/octet-stream";
                    };
                }

                // Read as base64
                byte[] bytes = Files.readAllBytes(path);
                String base64 = Base64.getEncoder().encodeToString(bytes);

                // Return as ContentBlock.Image compatible format
                return ToolResult.of(Map.of(
                        "type", "image",
                        "mediaType", mimeType,
                        "data", base64,
                        "path", input.filePath(),
                        "size", bytes.length
                ));

            } catch (Exception e) {
                return ToolResult.of(Map.of("error", e.getMessage()));
            }
        });
    }

    @Override
    public CompletableFuture<String> describe(Input input, ToolDescribeOptions options) {
        return CompletableFuture.completedFuture("ReadImage: " + input.filePath());
    }

    @Override
    public boolean isReadOnly(Input input) {
        return true;
    }

    private String getExtension(Path path) {
        String filename = path.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex > 0) {
            return filename.substring(dotIndex + 1);
        }
        return "";
    }
}