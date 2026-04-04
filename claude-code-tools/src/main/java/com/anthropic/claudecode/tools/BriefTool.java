/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code tools/BriefTool/BriefTool.ts
 */
package com.anthropic.claudecode.tools;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import com.anthropic.claudecode.*;
import com.anthropic.claudecode.services.analytics.AnalyticsMetadata;

/**
 * Brief Tool - send messages to the user.
 * This is the primary visible output channel for the model.
 */
public final class BriefTool extends AbstractTool<BriefTool.Input, BriefTool.Output, ToolProgressData> {
    public static final String TOOL_NAME = "SendUserMessage";
    public static final String LEGACY_NAME = "Brief";

    public BriefTool() {
        super(TOOL_NAME, "Send a message to the user");
    }

    /**
     * Input schema.
     */
    public record Input(
        String message,
        List<String> attachments,
        String status
    ) {}

    /**
     * Output schema.
     */
    public record Output(
        String message,
        List<AttachmentInfo> attachments,
        String sentAt
    ) {}

    /**
     * Attachment info.
     */
    public record AttachmentInfo(
        String path,
        long size,
        boolean isImage,
        String file_uuid
    ) {}

    @Override
    public List<String> aliases() {
        return List.of(LEGACY_NAME);
    }

    @Override
    public String description() {
        return """
            Send a message to the user. Use this tool to communicate findings, status updates, and results.

            Use 'proactive' status when surfacing something the user hasn't asked for:
            - Task completion while they're away
            - A blocker you hit
            - An unsolicited status update

            Use 'normal' status when replying to something the user just said.""";
    }

    @Override
    public String searchHint() {
        return "send a message to the user — your primary visible output channel";
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
    public boolean isEnabled() {
        return isBriefEnabled();
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
        if (input.message() == null || input.message().isEmpty()) {
            return ValidationResult.failure("Message is required", 1);
        }
        if (input.attachments() != null && !input.attachments().isEmpty()) {
            // Validate attachment paths exist
            for (String path : input.attachments()) {
                if (!java.nio.file.Files.exists(java.nio.file.Paths.get(path))) {
                    return ValidationResult.failure("Attachment not found: " + path, 2);
                }
            }
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
            String sentAt = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .format(new Date());

            // Log analytics
            AnalyticsMetadata.logEvent("tengu_brief_send", Map.of(
                "proactive", "proactive".equals(input.status()) ? "true" : "false",
                "attachment_count", String.valueOf(input.attachments() != null ? input.attachments().size() : 0)
            ), true);

            if (input.attachments() == null || input.attachments().isEmpty()) {
                return ToolResult.of(new Output(input.message(), null, sentAt));
            }

            // Resolve attachments
            List<AttachmentInfo> resolved = new ArrayList<>();
            for (String path : input.attachments()) {
                try {
                    java.nio.file.Path p = java.nio.file.Paths.get(path);
                    long size = java.nio.file.Files.size(p);
                    String fileName = p.getFileName().toString().toLowerCase();
                    boolean isImage = fileName.endsWith(".png") || fileName.endsWith(".jpg") ||
                                     fileName.endsWith(".jpeg") || fileName.endsWith(".gif") ||
                                     fileName.endsWith(".webp");
                    resolved.add(new AttachmentInfo(
                        path,
                        size,
                        isImage,
                        UUID.randomUUID().toString()
                    ));
                } catch (Exception e) {
                    // Skip invalid attachments
                }
            }

            return ToolResult.of(new Output(input.message(), resolved, sentAt));
        });
    }

    @Override
    public String formatResult(Output output) {
        int n = output.attachments() != null ? output.attachments().size() : 0;
        String suffix = n == 0 ? "" : " (" + n + " attachment" + (n > 1 ? "s" : "") + " included)";
        return "Message delivered to user." + suffix;
    }

    /**
     * Check if Brief is enabled.
     */
    public static boolean isBriefEnabled() {
        String env = System.getenv("CLAUDE_CODE_BRIEF");
        return "true".equalsIgnoreCase(env) || "1".equals(env);
    }

    /**
     * Check if Brief is entitled.
     */
    public static boolean isBriefEntitled() {
        return isBriefEnabled();
    }
}