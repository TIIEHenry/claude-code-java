/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/bash/ParsedCommand.ts
 */
package com.anthropic.claudecode.utils.bash;

import java.util.*;
import java.nio.charset.StandardCharsets;

/**
 * Interface for parsed command implementations.
 */
public interface IParsedCommand {
    /**
     * Get the original command string.
     */
    String originalCommand();

    /**
     * Convert to string representation.
     */
    String toString();

    /**
     * Get pipe segments of the command.
     */
    List<String> getPipeSegments();

    /**
     * Get command without output redirections.
     */
    String withoutOutputRedirections();

    /**
     * Get output redirections.
     */
    List<OutputRedirection> getOutputRedirections();

    /**
     * Get tree-sitter analysis data (null for regex fallback).
     */
    TreeSitterAnalysis getTreeSitterAnalysis();
}