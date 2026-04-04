/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/bash/ParsedCommand.ts
 */
package com.anthropic.claudecode.utils.bash;

/**
 * Output redirection record.
 */
public record OutputRedirection(
    String target,      // The file target
    String operator     // ">" or ">>"
) {
    public boolean isAppend() {
        return ">>".equals(operator);
    }

    public boolean isOverwrite() {
        return ">".equals(operator);
    }
}