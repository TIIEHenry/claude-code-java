/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 *
 * Java port of Claude Code Tool.ts
 */
package com.anthropic.claudecode;

import com.anthropic.claudecode.permission.PermissionResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Tool interface - Core abstraction for Claude Code tools.
 *
 * <p>This interface corresponds to the Tool type in Tool.ts.
 *
 * @param <I> Input type
 * @param <O> Output type
 * @param <P> Progress data type
 */
public interface Tool<I, O, P extends ToolProgressData> {

    /**
     * Get the tool name.
     */
    String name();

    /**
     * Get tool description for prompts and UI.
     */
    default String description() {
        return "Tool: " + name();
    }

    /**
     * Get tool aliases for backwards compatibility.
     */
    default List<String> aliases() {
        return List.of();
    }

    /**
     * Get a short capability phrase for tool search.
     */
    default String searchHint() {
        return null;
    }

    /**
     * Get the input JSON schema.
     */
    Map<String, Object> inputSchema();

    /**
     * Get an InputSchema wrapper that provides parse() method.
     */
    default InputSchema getInputSchema() {
        return new InputSchema(inputSchema());
    }

    /**
     * Input schema wrapper with parse capability.
     */
    record InputSchema(Map<String, Object> schema) {
        /**
         * Parse input according to schema.
         * Returns the parsed input or null if invalid.
         */
        public Object parse(Map<String, Object> input) {
            // Simple validation - in real implementation would validate against schema
            if (input == null) return null;
            return input;
        }
    }

    /**
     * Get the output schema (optional).
     */
    default Map<String, Object> outputSchema() {
        return null;
    }

    /**
     * Execute the tool.
     *
     * @param input           Tool input
     * @param context         Tool use context
     * @param canUseTool      Permission check function
     * @param parentMessage   The assistant message containing this tool call
     * @param onProgress      Progress callback (optional)
     * @return Tool result
     */
    CompletableFuture<ToolResult<O>> call(
            I input,
            ToolUseContext context,
            CanUseToolFn canUseTool,
            AssistantMessage parentMessage,
            Consumer<ToolProgress<P>> onProgress);

    /**
     * Generate a description for this tool use.
     */
    CompletableFuture<String> describe(I input, ToolDescribeOptions options);

    /**
     * Generate the tool prompt.
     */
    default CompletableFuture<String> prompt(ToolPromptOptions options) {
        return CompletableFuture.completedFuture("");
    }

    /**
     * Get user-facing name for this tool.
     */
    default String userFacingName(I input) {
        return name();
    }

    // ==================== Permission Methods ====================

    /**
     * Check permissions for this tool use.
     */
    default CompletableFuture<PermissionResult> checkPermissions(I input, ToolUseContext context) {
        return CompletableFuture.completedFuture(PermissionResult.allow(input));
    }

    /**
     * Validate input before execution.
     */
    default CompletableFuture<ValidationResult> validateInput(I input, ToolUseContext context) {
        return CompletableFuture.completedFuture(ValidationResult.success());
    }

    // ==================== State Query Methods ====================

    /**
     * Whether this tool is currently enabled.
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Whether this tool can run concurrently with other tools.
     */
    default boolean isConcurrencySafe(I input) {
        return false;
    }

    /**
     * Whether this tool only reads data (no side effects).
     */
    default boolean isReadOnly(I input) {
        return false;
    }

    /**
     * Whether this tool performs irreversible operations.
     */
    default boolean isDestructive(I input) {
        return false;
    }

    /**
     * Whether this tool requires user interaction.
     */
    default boolean requiresUserInteraction() {
        return false;
    }

    // Untyped versions for use with Object input
    @SuppressWarnings("unchecked")
    default boolean isReadOnlyUntyped(Object input) {
        return isReadOnly((I) input);
    }

    @SuppressWarnings("unchecked")
    default boolean isConcurrencySafeUntyped(Object input) {
        return isConcurrencySafe((I) input);
    }

    @SuppressWarnings("unchecked")
    default boolean isDestructiveUntyped(Object input) {
        return isDestructive((I) input);
    }

    /**
     * What should happen when user submits a new message while this tool is running.
     * @return 'cancel' to stop and discard, 'block' to keep running
     */
    default String interruptBehavior() {
        return "block";
    }

    // ==================== Search/Read Detection ====================

    /**
     * Whether this tool use is a search or read operation.
     */
    default SearchOrReadCommand isSearchOrReadCommand(I input) {
        return new SearchOrReadCommand(false, false, false);
    }

    /**
     * Whether this tool accesses external/unknown resources.
     */
    default boolean isOpenWorld(I input) {
        return false;
    }

    // ==================== Tool Metadata ====================

    /**
     * Maximum size in characters for tool result before persisting to disk.
     */
    default long maxResultSizeChars() {
        return Long.MAX_VALUE;
    }

    /**
     * Whether to enable strict mode for this tool.
     */
    default boolean strict() {
        return false;
    }

    /**
     * Whether this tool is deferred (requires ToolSearch to load).
     */
    default boolean shouldDefer() {
        return false;
    }

    /**
     * Whether this tool should always be loaded (never deferred).
     */
    default boolean alwaysLoad() {
        return false;
    }

    /**
     * MCP info for MCP tools.
     */
    default McpInfo mcpInfo() {
        return null;
    }

    // ==================== Helper Methods ====================

    /**
     * Check if this tool matches the given name (primary name or alias).
     */
    default boolean matchesName(String name) {
        return name().equals(name) || aliases().contains(name);
    }

    /**
     * Get compact representation for auto-mode security classifier.
     */
    default Object toAutoClassifierInput(I input) {
        return "";
    }

    /**
     * Get a summary string for compact views.
     */
    default String getToolUseSummary(I input) {
        return null;
    }

    /**
     * Get activity description for spinner display.
     */
    default String getActivityDescription(I input) {
        return null;
    }

    // ==================== Record Classes ====================

    record SearchOrReadCommand(boolean isSearch, boolean isRead, boolean isList) {}

    record McpInfo(String serverName, String toolName) {}
}