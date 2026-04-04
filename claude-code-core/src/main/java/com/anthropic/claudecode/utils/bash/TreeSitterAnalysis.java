/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/bash/treeSitterAnalysis.ts
 */
package com.anthropic.claudecode.utils.bash;

import java.util.*;

/**
 * Tree-sitter analysis result for shell commands.
 * Contains parsed AST information about the command structure.
 */
public record TreeSitterAnalysis(
    String commandName,          // The primary command name
    List<String> arguments,      // Command arguments
    List<String> operators,      // Shell operators used (|, &&, ||, etc.)
    boolean hasCommandSubstitution,  // Contains $() or backticks
    boolean hasProcessSubstitution,  // Contains <() or >()
    boolean hasVariableExpansion,    // Contains $VAR or ${VAR}
    boolean hasGlobPattern,          // Contains glob patterns (*, ?, [])
    boolean hasBraceExpansion,       // Contains brace expansion {a,b}
    boolean hasHeredoc,              // Contains heredoc syntax <<
    boolean hasSubshell,             // Contains (command) subshell
    int depth,                       // Nesting depth of commands
    List<String> subcommands,        // Extracted subcommands
    Map<String, Object> metadata     // Additional metadata
) {
    /**
     * Create empty/default analysis.
     */
    public static TreeSitterAnalysis empty() {
        return new TreeSitterAnalysis(
            null,
            Collections.emptyList(),
            Collections.emptyList(),
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            0,
            Collections.emptyList(),
            Collections.emptyMap()
        );
    }

    /**
     * Create simple command analysis.
     */
    public static TreeSitterAnalysis simpleCommand(String name, List<String> args) {
        return new TreeSitterAnalysis(
            name,
            args,
            Collections.emptyList(),
            false,
            false,
            false,
            false,
            false,
            false,
            false,
            0,
            Collections.emptyList(),
            Collections.emptyMap()
        );
    }

    /**
     * Check if this is a simple command (no special features).
     */
    public boolean isSimple() {
        return !hasCommandSubstitution &&
               !hasProcessSubstitution &&
               !hasVariableExpansion &&
               !hasGlobPattern &&
               !hasBraceExpansion &&
               !hasHeredoc &&
               !hasSubshell &&
               depth == 0;
    }

    /**
     * Check if the command is potentially dangerous.
     */
    public boolean isPotentiallyDangerous() {
        return hasCommandSubstitution || hasSubshell;
    }

    /**
     * Get all environment variables referenced.
     */
    public List<String> getReferencedVariables() {
        // This would be populated from actual tree-sitter analysis
        // Placeholder for now
        return Collections.emptyList();
    }

    /**
     * Get all commands that could be executed (including nested).
     */
    public List<String> getAllExecutableCommands() {
        List<String> all = new ArrayList<>();
        if (commandName != null) {
            all.add(commandName);
        }
        all.addAll(subcommands);
        return all;
    }
}