/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code tools/SyntheticOutputTool/SyntheticOutputTool.ts
 */
package com.anthropic.claudecode.tools;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import com.anthropic.claudecode.*;
import com.anthropic.claudecode.permission.PermissionResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

/**
 * SyntheticOutput Tool - return the final response as structured JSON.
 * Used for non-interactive SDK/CLI use to validate and return structured output.
 */
public final class SyntheticOutputTool extends AbstractTool<SyntheticOutputTool.Input, SyntheticOutputTool.Output, ToolProgressData> {
    public static final String TOOL_NAME = "StructuredOutput";

    private final JsonSchema jsonSchema;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SyntheticOutputTool() {
        this(null);
    }

    public SyntheticOutputTool(Map<String, Object> jsonSchema) {
        super(TOOL_NAME, "Return structured output in the requested format");
        this.jsonSchema = jsonSchema != null ? createJsonSchema(jsonSchema) : null;
    }

    /**
     * Input schema - allows any fields.
     */
    public record Input(
        Map<String, Object> fields
    ) {
        public Object get(String key) {
            return fields != null ? fields.get(key) : null;
        }
    }

    /**
     * Output schema.
     */
    public record Output(
        String message,
        Map<String, Object> structured_output
    ) {}

    /**
     * Create result.
     */
    public sealed interface CreateResult permits Success, Error {}
    public record Success(SyntheticOutputTool tool) implements CreateResult {}
    public record Error(String error) implements CreateResult {}

    // Cache for schema validation
    private static final Map<Integer, SyntheticOutputTool> toolCache = new ConcurrentHashMap<>();

    @Override
    public boolean isOpenWorld(Input input) {
        return false;
    }

    @Override
    public String description() {
        return "Return structured output in the requested format";
    }

    @Override
    public String searchHint() {
        return "return the final response as structured JSON";
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
    public CompletableFuture<PermissionResult> checkPermissions(Input input, ToolUseContext context) {
        return CompletableFuture.completedFuture(PermissionResult.allow(input));
    }

    @Override
    public CompletableFuture<ValidationResult> validateInput(Input input, ToolUseContext context) {
        if (jsonSchema != null && input.fields() != null) {
            try {
                JsonNode jsonNode = objectMapper.valueToTree(input.fields());
                Set<ValidationMessage> errors = jsonSchema.validate(jsonNode);

                if (!errors.isEmpty()) {
                    String errorMessages = errors.stream()
                        .map(ValidationMessage::getMessage)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("Unknown validation error");

                    return CompletableFuture.completedFuture(ValidationResult.failure(
                        "Output does not match required schema: " + errorMessages, 1
                    ));
                }
            } catch (Exception e) {
                return CompletableFuture.completedFuture(ValidationResult.failure(
                    "Schema validation error: " + e.getMessage(), 1
                ));
            }
        }
        return CompletableFuture.completedFuture(ValidationResult.success());
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
            // Validate against schema if provided
            if (jsonSchema != null && input.fields() != null) {
                try {
                    JsonNode jsonNode = objectMapper.valueToTree(input.fields());
                    Set<ValidationMessage> errors = jsonSchema.validate(jsonNode);

                    if (!errors.isEmpty()) {
                        String errorMessages = errors.stream()
                            .map(ValidationMessage::getMessage)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("Unknown validation error");

                        throw new RuntimeException(
                            "Output does not match required schema: " + errorMessages);
                    }
                } catch (Exception e) {
                    if (e instanceof RuntimeException) throw (RuntimeException) e;
                    throw new RuntimeException("Schema validation error: " + e.getMessage(), e);
                }
            }

            return ToolResult.of(new Output(
                "Structured output provided successfully",
                input.fields()
            ));
        });
    }

    @Override
    public String formatResult(Output output) {
        return output.message();
    }

    // Factory method

    public static CreateResult createSyntheticOutputTool(Map<String, Object> jsonSchema) {
        int cacheKey = jsonSchema.hashCode();
        SyntheticOutputTool cached = toolCache.get(cacheKey);
        if (cached != null) {
            return new Success(cached);
        }

        try {
            SyntheticOutputTool tool = new SyntheticOutputTool(jsonSchema);
            toolCache.put(cacheKey, tool);
            return new Success(tool);
        } catch (Exception e) {
            return new Error(e.getMessage());
        }
    }

    private JsonSchema createJsonSchema(Map<String, Object> schema) {
        try {
            JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
            JsonNode schemaNode = objectMapper.valueToTree(schema);
            return factory.getSchema(schemaNode);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create JSON schema: " + e.getMessage(), e);
        }
    }

    // Check if enabled for non-interactive sessions
    public static boolean isSyntheticOutputToolEnabled(boolean isNonInteractiveSession) {
        return isNonInteractiveSession;
    }
}