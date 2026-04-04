/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code spawn utilities for teammates
 */
package com.anthropic.claudecode.utils.swarm;

import com.anthropic.claudecode.utils.bash.ShellQuote;
import java.util.*;

/**
 * Shared utilities for spawning teammates across different backends.
 */
public final class SpawnUtils {
    private SpawnUtils() {}

    /**
     * Environment variables that must be forwarded to tmux-spawned teammates.
     */
    private static final List<String> TEAMMATE_ENV_VARS = List.of(
            // API provider selection
            "CLAUDE_CODE_USE_BEDROCK",
            "CLAUDE_CODE_USE_VERTEX",
            "CLAUDE_CODE_USE_FOUNDRY",
            // Custom API endpoint
            "ANTHROPIC_BASE_URL",
            // Config directory override
            "CLAUDE_CONFIG_DIR",
            // CCR marker
            "CLAUDE_CODE_REMOTE",
            // Memory directory
            "CLAUDE_CODE_REMOTE_MEMORY_DIR",
            // Upstream proxy
            "HTTPS_PROXY",
            "https_proxy",
            "HTTP_PROXY",
            "http_proxy",
            "NO_PROXY",
            "no_proxy",
            // SSL certificates
            "SSL_CERT_FILE",
            "NODE_EXTRA_CA_CERTS",
            "REQUESTS_CA_BUNDLE",
            "CURL_CA_BUNDLE"
    );

    /**
     * Gets the command to use for spawning teammate processes.
     */
    public static String getTeammateCommand() {
        String envCommand = System.getenv(SwarmConstants.TEAMMATE_COMMAND_ENV_VAR);
        if (envCommand != null && !envCommand.isEmpty()) {
            return envCommand;
        }
        // Would check bundled mode in real implementation
        // return isInBundledMode() ? ProcessHandle.current().info().command() : getMainScript();
        return "claude";
    }

    /**
     * Builds CLI flags to propagate from current session to spawned teammates.
     */
    public static String buildInheritedCliFlags(SpawnOptions options) {
        List<String> flags = new ArrayList<>();

        if (options != null) {
            // Propagate permission mode
            if (!options.planModeRequired) {
                if (options.permissionMode == "bypassPermissions" ||
                    isSessionBypassPermissionsMode()) {
                    flags.add("--dangerously-skip-permissions");
                } else if (options.permissionMode == "acceptEdits") {
                    flags.add("--permission-mode acceptEdits");
                }
            }

            // Propagate model if explicitly set
            String modelOverride = getMainLoopModelOverride();
            if (modelOverride != null) {
                flags.add("--model " + ShellQuote.quote(modelOverride));
            }

            // Propagate settings path if set
            String settingsPath = getFlagSettingsPath();
            if (settingsPath != null) {
                flags.add("--settings " + ShellQuote.quote(settingsPath));
            }

            // Propagate plugin directories
            List<String> inlinePlugins = getInlinePlugins();
            for (String pluginDir : inlinePlugins) {
                flags.add("--plugin-dir " + ShellQuote.quote(pluginDir));
            }

            // Propagate teammate mode
            String sessionMode = getTeammateModeFromSnapshot();
            flags.add("--teammate-mode " + sessionMode);

            // Propagate chrome flag
            Boolean chromeOverride = getChromeFlagOverride();
            if (chromeOverride != null) {
                if (chromeOverride) {
                    flags.add("--chrome");
                } else {
                    flags.add("--no-chrome");
                }
            }
        }

        return String.join(" ", flags);
    }

    /**
     * Builds the env KEY=VALUE string for teammate spawn commands.
     */
    public static String buildInheritedEnvVars() {
        List<String> envVars = new ArrayList<>();
        envVars.add("CLAUDECODE=1");
        envVars.add("CLAUDE_CODE_EXPERIMENTAL_AGENT_TEAMS=1");

        for (String key : TEAMMATE_ENV_VARS) {
            String value = System.getenv(key);
            if (value != null && !value.isEmpty()) {
                envVars.add(key + "=" + ShellQuote.quote(value));
            }
        }

        return String.join(" ", envVars);
    }

    // Placeholder methods - would integrate with bootstrap state in real impl

    private static boolean isSessionBypassPermissionsMode() {
        return Boolean.parseBoolean(System.getenv("CLAUDE_CODE_BYPASS_PERMISSIONS"));
    }

    private static String getMainLoopModelOverride() {
        return System.getenv("CLAUDE_CODE_MODEL_OVERRIDE");
    }

    private static String getFlagSettingsPath() {
        return System.getenv("CLAUDE_CODE_SETTINGS_PATH");
    }

    private static List<String> getInlinePlugins() {
        String plugins = System.getenv("CLAUDE_CODE_INLINE_PLUGINS");
        if (plugins == null || plugins.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(plugins.split(","));
    }

    private static String getTeammateModeFromSnapshot() {
        String mode = System.getenv("CLAUDE_CODE_TEAMMATE_MODE");
        return mode != null ? mode : "default";
    }

    private static Boolean getChromeFlagOverride() {
        String chrome = System.getenv("CLAUDE_CODE_CHROME_FLAG");
        if (chrome == null) return null;
        return Boolean.parseBoolean(chrome);
    }

    /**
     * Spawn options record.
     */
    public record SpawnOptions(
            boolean planModeRequired,
            String permissionMode
    ) {}
}