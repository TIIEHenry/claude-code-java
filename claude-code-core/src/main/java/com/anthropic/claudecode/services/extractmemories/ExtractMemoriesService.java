/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/extractMemories
 */
package com.anthropic.claudecode.services.extractmemories;

import java.util.*;
import java.nio.file.*;

/**
 * Extract memories service - Extracts durable memories from session transcripts.
 */
public final class ExtractMemoriesService {
    private final Path autoMemPath;
    private boolean enabled;
    private String lastExtractedUuid;

    public ExtractMemoriesService(Path projectPath) {
        this.autoMemPath = projectPath.resolve(".claude").resolve("memory");
        this.enabled = true;
        this.lastExtractedUuid = null;
    }

    /**
     * Check if auto memory is enabled.
     */
    public boolean isAutoMemoryEnabled() {
        return enabled;
    }

    /**
     * Enable or disable auto memory.
     */
    public void setAutoMemoryEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Get the auto memory path.
     */
    public Path getAutoMemPath() {
        return autoMemPath;
    }

    /**
     * Extract memories from session messages.
     */
    public ExtractionResult extractMemories(
        List<Object> messages,
        ExtractionOptions options
    ) {
        if (!enabled) {
            return new ExtractionResult(false, 0, Collections.emptyList());
        }

        // Count model-visible messages since last extraction
        int newMessageCount = countVisibleMessagesSince(messages, lastExtractedUuid);

        // Check if we have enough new messages
        if (newMessageCount < options.minMessages()) {
            return new ExtractionResult(false, 0, Collections.emptyList());
        }

        // Build extraction prompt
        String prompt = buildExtractionPrompt(messages, options);

        // Run extraction agent
        List<ExtractedMemory> memories = runExtractionAgent(prompt);

        // Write memories to disk
        int written = writeMemories(memories);

        // Update last extracted UUID
        if (!messages.isEmpty()) {
            lastExtractedUuid = getLastMessageUuid(messages);
        }

        return new ExtractionResult(true, written, memories);
    }

    /**
     * Build extraction prompt from messages.
     */
    private String buildExtractionPrompt(List<Object> messages, ExtractionOptions options) {
        StringBuilder sb = new StringBuilder();
        sb.append("Extract durable memories from the following conversation.\n\n");
        sb.append("Guidelines:\n");
        sb.append("- Extract facts, preferences, and context that should be remembered\n");
        sb.append("- Ignore temporary state and ephemeral details\n");
        sb.append("- Format each memory as a concise statement\n\n");

        sb.append("Messages:\n");
        for (Object message : messages) {
            // Implementation would format messages
        }

        return sb.toString();
    }

    /**
     * Run extraction agent.
     */
    private List<ExtractedMemory> runExtractionAgent(String prompt) {
        // Implementation would run a forked agent
        return Collections.emptyList();
    }

    /**
     * Write memories to disk.
     */
    private int writeMemories(List<ExtractedMemory> memories) {
        try {
            Files.createDirectories(autoMemPath);

            int written = 0;
            for (ExtractedMemory memory : memories) {
                Path memoryFile = autoMemPath.resolve(memory.id() + ".md");
                Files.writeString(memoryFile, memory.content());
                written++;
            }

            return written;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Count visible messages since a UUID.
     */
    private int countVisibleMessagesSince(List<Object> messages, String sinceUuid) {
        if (sinceUuid == null) {
            return messages.size();
        }

        boolean found = false;
        int count = 0;
        for (Object message : messages) {
            if (!found) {
                // Check if this is the target message
                found = true; // Simplified
                continue;
            }
            count++;
        }
        return count;
    }

    /**
     * Get last message UUID.
     */
    private String getLastMessageUuid(List<Object> messages) {
        if (messages.isEmpty()) {
            return null;
        }
        return UUID.randomUUID().toString(); // Simplified
    }

    /**
     * Scan existing memory files.
     */
    public List<MemoryFile> scanMemoryFiles() {
        List<MemoryFile> files = new ArrayList<>();
        if (!Files.exists(autoMemPath)) {
            return files;
        }

        try {
            Files.list(autoMemPath)
                .filter(p -> p.toString().endsWith(".md"))
                .forEach(p -> {
                    try {
                        String content = Files.readString(p);
                        files.add(new MemoryFile(
                            p.getFileName().toString(),
                            content,
                            Files.getLastModifiedTime(p).toMillis()
                        ));
                    } catch (Exception e) {
                        // Skip unreadable files
                    }
                });
        } catch (Exception e) {
            // Return empty list
        }

        return files;
    }

    /**
     * Clear all memories.
     */
    public void clearMemories() {
        try {
            if (Files.exists(autoMemPath)) {
                Files.list(autoMemPath)
                    .filter(p -> p.toString().endsWith(".md"))
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (Exception e) {
                            // Ignore deletion errors
                        }
                    });
            }
        } catch (Exception e) {
            // Ignore errors
        }
    }

    /**
     * Extraction options record.
     */
    public record ExtractionOptions(
        int minMessages,
        boolean includeSystemMessages,
        boolean includeToolResults
    ) {
        public static ExtractionOptions defaults() {
            return new ExtractionOptions(5, false, true);
        }
    }

    /**
     * Extraction result record.
     */
    public record ExtractionResult(
        boolean extracted,
        int memoriesWritten,
        List<ExtractedMemory> memories
    ) {}

    /**
     * Extracted memory record.
     */
    public record ExtractedMemory(
        String id,
        String content,
        String category,
        long timestamp
    ) {
        public ExtractedMemory(String content, String category) {
            this(UUID.randomUUID().toString(), content, category, System.currentTimeMillis());
        }
    }

    /**
     * Memory file record.
     */
    public record MemoryFile(
        String name,
        String content,
        long lastModified
    ) {}
}