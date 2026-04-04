/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code hooks/useCanUseTool.tsx
 */
package com.anthropic.claudecode.hooks;

import com.anthropic.claudecode.*;
import com.anthropic.claudecode.permission.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Hook for checking if a tool can be used.
 */
@FunctionalInterface
public interface CanUseToolFn {
    /**
     * Check if a tool can be used with the given input.
     *
     * @param toolName The tool name
     * @param input The tool input
     * @param context The tool use context
     * @return true if the tool can be used
     */
    boolean canUse(String toolName, Object input, ToolUseContext context);
}