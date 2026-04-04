/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 *
 * CLI configuration options
 */
package com.anthropic.claudecode.cli;

import java.util.Map;

/**
 * CLI configuration loaded from environment and config files.
 *
 * <p>Corresponds to CLI config in the TypeScript version.
 */
public class CliConfig {

    private final String apiKey;
    private final String model;
    private final String cwd;
    private final boolean verbose;
    private final Map<String, Object> settings;

    public CliConfig() {
        this.apiKey = loadApiKey();
        this.model = System.getenv("CLAUDE_MODEL");
        this.cwd = System.getProperty("user.dir");
        this.verbose = Boolean.parseBoolean(System.getenv("CLAUDE_VERBOSE"));
        this.settings = Map.of();
    }

    /**
     * Load API key from environment.
     */
    private String loadApiKey() {
        String key = System.getenv("ANTHROPIC_API_KEY");
        if (key == null) {
            key = System.getenv("CLAUDE_API_KEY");
        }
        return key;
    }

    public String apiKey() {
        return apiKey;
    }

    public String model() {
        return model;
    }

    public String cwd() {
        return cwd;
    }

    public boolean verbose() {
        return verbose;
    }

    public Map<String, Object> settings() {
        return settings;
    }

    /**
     * Check if API key is available.
     */
    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isEmpty();
    }
}