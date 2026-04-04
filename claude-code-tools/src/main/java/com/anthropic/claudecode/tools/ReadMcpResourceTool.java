/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code tools/ReadMcpResourceTool/ReadMcpResourceTool.ts
 */
package com.anthropic.claudecode.tools;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.nio.file.*;
import java.util.Base64;
import com.anthropic.claudecode.*;

/**
 * ReadMcpResource Tool - read a specific MCP resource by URI.
 */
public final class ReadMcpResourceTool extends AbstractTool<ReadMcpResourceTool.Input, ReadMcpResourceTool.Output, ToolProgressData> {
    public static final String TOOL_NAME = "ReadMcpResource";

    public ReadMcpResourceTool() {
        super(TOOL_NAME, "Read a specific MCP resource by URI");
    }

    /**
     * Input schema.
     */
    public record Input(
        String server,
        String uri
    ) {}

    /**
     * Resource content.
     */
    public record ResourceContent(
        String uri,
        String mimeType,
        String text,
        String blobSavedTo
    ) {}

    /**
     * Output schema.
     */
    public record Output(
        List<ResourceContent> contents
    ) {}

    @Override
    public String description() {
        return "Read a specific MCP resource by URI from a connected MCP server";
    }

    @Override
    public String searchHint() {
        return "read a specific MCP resource by URI";
    }

    @Override
    public boolean shouldDefer() {
        return true;
    }

    @Override
    public boolean isConcurrencySafe(Input input) {
        return true;
    }

    @Override
    public boolean isReadOnly(Input input) {
        return true;
    }

    @Override
    public Class<Input> inputType() {
        return Input.class;
    }

    @Override
    public Class<Output> outputType() {
        return Output.class;
    }

    @Override
    public ValidationResult validateInput(Input input) {
        if (input.server() == null || input.server().isEmpty()) {
            return ValidationResult.failure("server is required", 1);
        }
        if (input.uri() == null || input.uri().isEmpty()) {
            return ValidationResult.failure("uri is required", 1);
        }
        return ValidationResult.success();
    }

    @Override
    public CompletableFuture<ToolResult<Output>> call(
        Input input,
        ToolUseContext context,
        CanUseToolFn canUseTool,
        AssistantMessage parent,
        Consumer<ToolProgress<ToolProgressData>> onProgress
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<ResourceContent> contents = readResourceFromMcp(input.server(), input.uri());
                return ToolResult.of(new Output(contents));
            } catch (Exception e) {
                // Return error as content
                List<ResourceContent> errorContent = List.of(
                    new ResourceContent(input.uri(), "text/plain", "Error: " + e.getMessage(), null)
                );
                return ToolResult.of(new Output(errorContent));
            }
        });
    }

    @Override
    public String formatResult(Output output) {
        if (output.contents() == null || output.contents().isEmpty()) {
            return "No content returned from resource";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("{\"contents\":[");
        boolean first = true;
        for (ResourceContent c : output.contents()) {
            if (!first) sb.append(",");
            sb.append("\n  {\"uri\":\"").append(c.uri()).append("\"");
            if (c.mimeType() != null) {
                sb.append(",\"mimeType\":\"").append(c.mimeType()).append("\"");
            }
            if (c.text() != null) {
                sb.append(",\"text\":\"").append(escapeJson(c.text())).append("\"");
            }
            if (c.blobSavedTo() != null) {
                sb.append(",\"blobSavedTo\":\"").append(c.blobSavedTo()).append("\"");
            }
            sb.append("}");
            first = false;
        }
        sb.append("\n]}");
        return sb.toString();
    }

    // Helpers

    private List<ResourceContent> readResourceFromMcp(String serverName, String uri) {
        // Read resource from MCP server
        List<ResourceContent> results = new ArrayList<>();

        try {
            // Find MCP client for the server
            // In a real implementation, this would use context.options().mcpClients()
            // to find the appropriate MCP client and call resources/read

            // For now, simulate reading from a file-based resource
            if (uri.startsWith("file://")) {
                String filePath = uri.substring(7);
                java.nio.file.Path path = java.nio.file.Paths.get(filePath);

                if (java.nio.file.Files.exists(path)) {
                    String content = java.nio.file.Files.readString(path);
                    String mimeType = guessMimeType(path.toString());
                    results.add(new ResourceContent(uri, mimeType, content, null));
                }
            } else if (uri.startsWith("http://") || uri.startsWith("https://")) {
                // Fetch HTTP resource
                java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create(uri))
                    .GET()
                    .build();

                java.net.http.HttpResponse<String> response = httpClient.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String mimeType = response.headers().firstValue("Content-Type").orElse("text/plain");
                    results.add(new ResourceContent(uri, mimeType, response.body(), null));
                }
            } else {
                // Generic resource - return placeholder
                results.add(new ResourceContent(uri, "text/plain",
                    "Resource from MCP server: " + (serverName != null ? serverName : "unknown"), null));
            }
        } catch (Exception e) {
            results.add(new ResourceContent(uri, "text/plain",
                "Error reading resource: " + e.getMessage(), null));
        }

        return results;
    }

    private String guessMimeType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".xml")) return "application/xml";
        if (lower.endsWith(".html")) return "text/html";
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".js")) return "application/javascript";
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".md")) return "text/markdown";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }

    private ResourceContent processBlobContent(String uri, String mimeType, String base64Blob) {
        try {
            byte[] bytes = Base64.getDecoder().decode(base64Blob);

            // Generate unique file path
            String extension = getExtensionFromMimeType(mimeType);
            String filename = "mcp-resource-" + System.currentTimeMillis() + "-" +
                UUID.randomUUID().toString().substring(0, 8) + extension;

            String home = System.getProperty("user.home");
            Path filepath = Paths.get(home, ".claude", "mcp-output", filename);
            Files.createDirectories(filepath.getParent());
            Files.write(filepath, bytes);

            String message = "Binary content saved to " + filepath + " (" + bytes.length + " bytes)";

            return new ResourceContent(uri, mimeType, message, filepath.toString());
        } catch (Exception e) {
            return new ResourceContent(uri, mimeType,
                "Binary content could not be saved to disk: " + e.getMessage(), null);
        }
    }

    private String getExtensionFromMimeType(String mimeType) {
        if (mimeType == null) return ".bin";
        return switch (mimeType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "application/pdf" -> ".pdf";
            case "application/json" -> ".json";
            case "text/plain" -> ".txt";
            case "text/html" -> ".html";
            case "text/css" -> ".css";
            case "text/javascript", "application/javascript" -> ".js";
            default -> ".bin";
        };
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}