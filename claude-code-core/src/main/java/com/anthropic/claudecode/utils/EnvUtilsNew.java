/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code environment utilities
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;

/**
 * Environment detection utilities.
 */
public final class EnvUtilsNew {
    private EnvUtilsNew() {}

    /**
     * Platform types.
     */
    public enum Platform {
        WIN32, DARWIN, LINUX
    }

    /**
     * Deployment environment types.
     */
    public enum DeploymentEnvironment {
        CODESPACES, GITPOD, REPLIT, GLITCH, VERCEL, RAILWAY, RENDER, NETLIFY,
        HEROKU, FLY_IO, CLOUDFLARE_PAGES, DENO_DEPLOY, AWS_LAMBDA, AWS_FARGATE,
        AWS_ECS, AWS_EC2, GCP_CLOUD_RUN, GCP, AZURE_APP_SERVICE, AZURE_FUNCTIONS,
        DIGITALOCEAN_APP_PLATFORM, HUGGINGFACE_SPACES, GITHUB_ACTIONS, GITLAB_CI,
        CIRCLECI, BUILDKITE, CI, KUBERNETES, DOCKER, UNKNOWN
    }

    // JetBrains IDEs
    public static final Set<String> JETBRAINS_IDES = Set.of(
            "pycharm", "intellij", "webstorm", "phpstorm", "rubymine", "clion",
            "goland", "rider", "datagrip", "appcode", "dataspell", "aqua",
            "gateway", "fleet", "jetbrains", "androidstudio"
    );

    /**
     * Get the current platform.
     */
    public static Platform getPlatform() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) return Platform.WIN32;
        if (os.contains("mac")) return Platform.DARWIN;
        return Platform.LINUX;
    }

    /**
     * Get the system architecture.
     */
    public static String getArch() {
        return System.getProperty("os.arch", "unknown");
    }

    /**
     * Get Java version.
     */
    public static String getJavaVersion() {
        return System.getProperty("java.version", "unknown");
    }

    /**
     * Detect the current terminal.
     */
    public static String detectTerminal() {
        // Check for Cursor
        if (System.getenv("CURSOR_TRACE_ID") != null) {
            return "cursor";
        }

        // Check for VS Code family via git askpass
        String vscodeGitAskpass = System.getenv("VSCODE_GIT_ASKPASS_MAIN");
        if (vscodeGitAskpass != null) {
            if (vscodeGitAskpass.contains("cursor")) return "cursor";
            if (vscodeGitAskpass.contains("windsurf")) return "windsurf";
            if (vscodeGitAskpass.contains("antigravity")) return "antigravity";
        }

        // Check for macOS bundle identifier
        String bundleId = System.getenv("__CFBundleIdentifier");
        if (bundleId != null) {
            String lower = bundleId.toLowerCase();
            if (lower.contains("vscodium")) return "codium";
            if (lower.contains("windsurf")) return "windsurf";
            if (lower.contains("com.google.android.studio")) return "androidstudio";

            for (String ide : JETBRAINS_IDES) {
                if (lower.contains(ide)) return ide;
            }
        }

        // Check for Visual Studio
        if (System.getenv("VisualStudioVersion") != null) {
            return "visualstudio";
        }

        // Check for JetBrains terminal
        if ("JetBrains-JediTerm".equals(System.getenv("TERMINAL_EMULATOR"))) {
            return "pycharm";
        }

        // Check for specific terminals by TERM
        String term = System.getenv("TERM");
        if (term != null) {
            if ("xterm-ghostty".equals(term)) return "ghostty";
            if (term.contains("kitty")) return "kitty";
        }

        // Check TERM_PROGRAM
        String termProgram = System.getenv("TERM_PROGRAM");
        if (termProgram != null) {
            return termProgram;
        }

        // Check for tmux
        if (System.getenv("TMUX") != null) return "tmux";
        if (System.getenv("STY") != null) return "screen";

        // Check for terminal-specific environment variables
        if (System.getenv("KONSOLE_VERSION") != null) return "konsole";
        if (System.getenv("GNOME_TERMINAL_SERVICE") != null) return "gnome-terminal";
        if (System.getenv("XTERM_VERSION") != null) return "xterm";
        if (System.getenv("VTE_VERSION") != null) return "vte-based";
        if (System.getenv("TERMINATOR_UUID") != null) return "terminator";
        if (System.getenv("KITTY_WINDOW_ID") != null) return "kitty";
        if (System.getenv("ALACRITTY_LOG") != null) return "alacritty";
        if (System.getenv("TILIX_ID") != null) return "tilix";

        // Windows-specific
        if (System.getenv("WT_SESSION") != null) return "windows-terminal";
        if (System.getenv("SESSIONNAME") != null && "cygwin".equals(term)) return "cygwin";
        if (System.getenv("MSYSTEM") != null) return System.getenv("MSYSTEM").toLowerCase();
        if (System.getenv("ConEmuANSI") != null ||
            System.getenv("ConEmuPID") != null ||
            System.getenv("ConEmuTask") != null) {
            return "conemu";
        }

        // WSL detection
        if (System.getenv("WSL_DISTRO_NAME") != null) {
            return "wsl-" + System.getenv("WSL_DISTRO_NAME");
        }

        // SSH session
        if (isSSHSession()) {
            return "ssh-session";
        }

        // Fall back to TERM
        if (term != null) {
            if (term.contains("alacritty")) return "alacritty";
            if (term.contains("rxvt")) return "rxvt";
            if (term.contains("termite")) return "termite";
            return term;
        }

        // Check for non-interactive
        if (System.console() == null) return "non-interactive";

        return null;
    }

    /**
     * Check if running in an SSH session.
     */
    public static boolean isSSHSession() {
        return System.getenv("SSH_CONNECTION") != null ||
               System.getenv("SSH_CLIENT") != null ||
               System.getenv("SSH_TTY") != null;
    }

    /**
     * Detect the deployment environment.
     */
    public static DeploymentEnvironment detectDeploymentEnvironment() {
        // Cloud development environments
        if (isEnvTruthy("CODESPACES")) return DeploymentEnvironment.CODESPACES;
        if (System.getenv("GITPOD_WORKSPACE_ID") != null) return DeploymentEnvironment.GITPOD;
        if (System.getenv("REPL_ID") != null || System.getenv("REPL_SLUG") != null) return DeploymentEnvironment.REPLIT;
        if (System.getenv("PROJECT_DOMAIN") != null) return DeploymentEnvironment.GLITCH;

        // Cloud platforms
        if (isEnvTruthy("VERCEL")) return DeploymentEnvironment.VERCEL;
        if (System.getenv("RAILWAY_ENVIRONMENT_NAME") != null ||
            System.getenv("RAILWAY_SERVICE_NAME") != null) return DeploymentEnvironment.RAILWAY;
        if (isEnvTruthy("RENDER")) return DeploymentEnvironment.RENDER;
        if (isEnvTruthy("NETLIFY")) return DeploymentEnvironment.NETLIFY;
        if (System.getenv("DYNO") != null) return DeploymentEnvironment.HEROKU;
        if (System.getenv("FLY_APP_NAME") != null || System.getenv("FLY_MACHINE_ID") != null) return DeploymentEnvironment.FLY_IO;
        if (isEnvTruthy("CF_PAGES")) return DeploymentEnvironment.CLOUDFLARE_PAGES;
        if (System.getenv("DENO_DEPLOYMENT_ID") != null) return DeploymentEnvironment.DENO_DEPLOY;

        // AWS
        if (System.getenv("AWS_LAMBDA_FUNCTION_NAME") != null) return DeploymentEnvironment.AWS_LAMBDA;
        if ("AWS_ECS_FARGATE".equals(System.getenv("AWS_EXECUTION_ENV"))) return DeploymentEnvironment.AWS_FARGATE;
        if ("AWS_ECS_EC2".equals(System.getenv("AWS_EXECUTION_ENV"))) return DeploymentEnvironment.AWS_ECS;

        // GCP
        if (System.getenv("K_SERVICE") != null) return DeploymentEnvironment.GCP_CLOUD_RUN;
        if (System.getenv("GOOGLE_CLOUD_PROJECT") != null) return DeploymentEnvironment.GCP;

        // Azure
        if (System.getenv("WEBSITE_SITE_NAME") != null || System.getenv("WEBSITE_SKU") != null) return DeploymentEnvironment.AZURE_APP_SERVICE;
        if (System.getenv("AZURE_FUNCTIONS_ENVIRONMENT") != null) return DeploymentEnvironment.AZURE_FUNCTIONS;

        // Other platforms
        if (System.getenv("APP_URL") != null && System.getenv("APP_URL").contains("ondigitalocean.app")) return DeploymentEnvironment.DIGITALOCEAN_APP_PLATFORM;
        if (System.getenv("SPACE_CREATOR_USER_ID") != null) return DeploymentEnvironment.HUGGINGFACE_SPACES;

        // CI/CD platforms
        if (isEnvTruthy("GITHUB_ACTIONS")) return DeploymentEnvironment.GITHUB_ACTIONS;
        if (isEnvTruthy("GITLAB_CI")) return DeploymentEnvironment.GITLAB_CI;
        if (System.getenv("CIRCLECI") != null) return DeploymentEnvironment.CIRCLECI;
        if (System.getenv("BUILDKITE") != null) return DeploymentEnvironment.BUILDKITE;
        if (isEnvTruthy("CI")) return DeploymentEnvironment.CI;

        // Container orchestration
        if (System.getenv("KUBERNETES_SERVICE_HOST") != null) return DeploymentEnvironment.KUBERNETES;
        if (Files.exists(Paths.get("/.dockerenv"))) return DeploymentEnvironment.DOCKER;

        return DeploymentEnvironment.UNKNOWN;
    }

    /**
     * Check if running in WSL.
     */
    public static boolean isWslEnvironment() {
        try {
            return Files.exists(Paths.get("/proc/sys/fs/binfmt_misc/WSLInterop"));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check if running via Conductor.
     */
    public static boolean isConductor() {
        return "com.conductor.app".equals(System.getenv("__CFBundleIdentifier"));
    }

    /**
     * Check if running in CI environment.
     */
    public static boolean isCI() {
        return isEnvTruthy("CI");
    }

    /**
     * Get the user's home directory.
     */
    public static String getHomeDir() {
        return System.getProperty("user.home", "/");
    }

    /**
     * Get Claude config directory.
     */
    public static String getClaudeConfigDir() {
        String configDir = System.getenv("CLAUDE_CONFIG_DIR");
        return configDir != null ? configDir : getHomeDir();
    }

    /**
     * Check if an environment variable is truthy.
     */
    public static boolean isEnvTruthy(String name) {
        String value = System.getenv(name);
        if (value == null) return false;
        return "true".equalsIgnoreCase(value) ||
               "1".equals(value) ||
               "yes".equalsIgnoreCase(value);
    }

    /**
     * Get environment variable with default.
     */
    public static String getEnv(String name, String defaultValue) {
        String value = System.getenv(name);
        return value != null ? value : defaultValue;
    }

    /**
     * Get environment variable as integer.
     */
    public static int getEnvAsInt(String name, int defaultValue) {
        String value = System.getenv(name);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Get environment variable as boolean.
     */
    public static boolean getEnvAsBool(String name, boolean defaultValue) {
        String value = System.getenv(name);
        if (value == null) return defaultValue;
        return "true".equalsIgnoreCase(value) ||
               "1".equals(value) ||
               "yes".equalsIgnoreCase(value);
    }

    /**
     * Get the Claude config home directory.
     */
    public static String getClaudeConfigHomeDir() {
        String home = System.getProperty("user.home");
        return home + "/.claude";
    }
}