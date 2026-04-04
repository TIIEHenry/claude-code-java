/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 *
 * Java port of Claude Code CLI version provider
 */
package com.anthropic.claudecode.cli;

import picocli.CommandLine.IVersionProvider;

/**
 * Version provider for CLI.
 */
public class VersionProvider implements IVersionProvider {

    @Override
    public String[] getVersion() {
        return new String[] {
            "Claude Code Java 1.0.0",
            "Based on Claude Code TypeScript",
            "Copyright 2024-2026 Anthropic"
        };
    }
}