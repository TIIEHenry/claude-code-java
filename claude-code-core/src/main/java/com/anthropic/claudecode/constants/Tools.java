/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code constants/tools.ts
 */
package com.anthropic.claudecode.constants;

/**
 * Tool constants.
 */
public final class Tools {
    private Tools() {}

    // Tool names
    public static final String BASH = "Bash";
    public static final String READ = "Read";
    public static final String WRITE = "Write";
    public static final String EDIT = "Edit";
    public static final String GLOB = "Glob";
    public static final String GREP = "Grep";
    public static final String ASK = "Ask";
    public static final String AGENT = "Agent";
    public static final String WEB_FETCH = "WebFetch";
    public static final String WEB_SEARCH = "WebSearch";
    public static final String NOTEBOOK_EDIT = "NotebookEdit";
    public static final String TASK_CREATE = "TaskCreate";
    public static final String TASK_OUTPUT = "TaskOutput";
    public static final String TASK_UPDATE = "TaskUpdate";
    public static final String SEND_MESSAGE = "SendMessage";
    public static final String ENTER_PLAN_MODE = "EnterPlanMode";
    public static final String EXIT_PLAN_MODE = "ExitPlanMode";

    // Tool descriptions
    public static final String BASH_DESCRIPTION = "Execute a bash command in a persistent shell session";
    public static final String READ_DESCRIPTION = "Reads a file from the local filesystem";
    public static final String WRITE_DESCRIPTION = "Writes a file to the local filesystem";
    public static final String EDIT_DESCRIPTION = "Performs string replacements in a file";
    public static final String GLOB_DESCRIPTION = "Fast file pattern matching tool";
    public static final String GREP_DESCRIPTION = "Search file contents using regular expressions";

    // Read-only tools
    public static final String[] READ_ONLY_TOOLS = {
        READ, GLOB, GREP, WEB_FETCH, WEB_SEARCH
    };

    // Concurrency-safe tools
    public static final String[] CONCURRENCY_SAFE_TOOLS = {
        READ, GLOB, GREP, WEB_FETCH, WEB_SEARCH
    };

    // Destructive tools
    public static final String[] DESTRUCTIVE_TOOLS = {
        BASH, WRITE, EDIT
    };

    /**
     * Check if tool is read-only.
     */
    public static boolean isReadOnly(String toolName) {
        for (String name : READ_ONLY_TOOLS) {
            if (name.equals(toolName)) return true;
        }
        return false;
    }

    /**
     * Check if tool is concurrency-safe.
     */
    public static boolean isConcurrencySafe(String toolName) {
        for (String name : CONCURRENCY_SAFE_TOOLS) {
            if (name.equals(toolName)) return true;
        }
        return false;
    }

    /**
     * Check if tool is destructive.
     */
    public static boolean isDestructive(String toolName) {
        for (String name : DESTRUCTIVE_TOOLS) {
            if (name.equals(toolName)) return true;
        }
        return false;
    }
}