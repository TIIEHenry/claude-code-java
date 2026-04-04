/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/env.ts
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Environment service - comprehensive environment detection.
 *
 * Provides platform, terminal, runtime, and deployment environment detection.
 */
public final class Environment {
    private Environment() {}

    /**
     * Supported platforms.
     */
    public enum Platform {
        WIN32,
        DARWIN,
        LINUX
    }

    /**
     * Terminal types.
     */
    public enum Terminal {
        CURSOR,
        VSCODE,
        WINDSURF,
        ANTIGRAVITY,
        ITERM2,
        KITTY,
        GHOSTTY,
        APPLE_TERMINAL,
        TERMINAL_BELL,
        TMUX,
        SCREEN,
        KONSOLE,
        GNOME_TERMINAL,
        XTERM,
        TERMINATOR,
        ALACRITTY,
        TILIX,
        WINDOWS_TERMINAL,
        CYGWIN,
        CONEMU,
        SSH_SESSION,
        NON_INTERACTIVE,
        JETBRAINS,
        UNKNOWN
    }

    /**
     * Deployment environments.
     */
    public enum DeploymentEnvironment {
        CODESPACES,
        GITPOD,
        REPLIT,
        GLITCH,
        VERCEL,
        RAILWAY,
        RENDER,
        NETLIFY,
        HEROKU,
        FLY_IO,
        CLOUDFLARE_PAGES,
        DENO_DEPLOY,
        AWS_LAMBDA,
        AWS_FARGATE,
        AWS_ECS,
        AWS_EC2,
        GCP_CLOUD_RUN,
        GCP,
        AZURE_APP_SERVICE,
        AZURE_FUNCTIONS,
        DIGITALOCEAN_APP_PLATFORM,
        HUGGINGFACE_SPACES,
        GITHUB_ACTIONS,
        GITLAB_CI,
        CIRCLECI,
        BUILDKITE,
        CI,
        KUBERNETES,
        DOCKER,
        WSL,
        UNKNOWN_DARWIN,
        UNKNOWN_LINUX,
        UNKNOWN_WIN32,
        UNKNOWN
    }

    // Cached values
    private static volatile Platform cachedPlatform = null;
    private static volatile Terminal cachedTerminal = null;
    private static volatile DeploymentEnvironment cachedDeploymentEnv = null;
    private static volatile List<String> cachedPackageManagers = null;
    private static volatile List<String> cachedRuntimes = null;
    private static volatile Boolean cachedInternetAccess = null;
    private static volatile Boolean cachedWslEnvironment = null;
    private static volatile Boolean cachedNpmFromWindows = null;

    // JetBrains IDEs list
    public static final List<String> JETBRAINS_IDES = List.of(
        "pycharm", "intellij", "webstorm", "phpstorm", "rubymine",
        "clion", "goland", "rider", "datagrip", "appcode",
        "dataspell", "aqua", "gateway", "fleet", "jetbrains", "androidstudio"
    );

    /**
     * Get the current platform.
     */
    public static Platform getPlatform() {
        if (cachedPlatform != null) {
            return cachedPlatform;
        }

        String osName = System.getProperty("os.name", "").toLowerCase();

        if (osName.contains("win")) {
            cachedPlatform = Platform.WIN32;
        } else if (osName.contains("mac")) {
            cachedPlatform = Platform.DARWIN;
        } else {
            cachedPlatform = Platform.LINUX;
        }

        return cachedPlatform;
    }

    /**
     * Get architecture.
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
     * Detect terminal type.
     */
    public static Terminal detectTerminal() {
        if (cachedTerminal != null) {
            return cachedTerminal;
        }

        Map<String, String> env = System.getenv();

        // Cursor detection
        if (env.containsKey("CURSOR_TRACE_ID")) {
            cachedTerminal = Terminal.CURSOR;
            return cachedTerminal;
        }

        // VS Code Git Askpass detection
        String vscodeGit = env.get("VSCODE_GIT_ASKPASS_MAIN");
        if (vscodeGit != null) {
            if (vscodeGit.contains("cursor")) {
                cachedTerminal = Terminal.CURSOR;
                return cachedTerminal;
            }
            if (vscodeGit.contains("windsurf")) {
                cachedTerminal = Terminal.WINDSURF;
                return cachedTerminal;
            }
            if (vscodeGit.contains("antigravity")) {
                cachedTerminal = Terminal.ANTIGRAVITY;
                return cachedTerminal;
            }
        }

        // Bundle ID detection (macOS)
        String bundleId = env.get("__CFBundleIdentifier");
        if (bundleId != null) {
            String lower = bundleId.toLowerCase();
            if (lower.contains("vscodium")) {
                cachedTerminal = Terminal.VSCODE; // Treat as VS Code variant
                return cachedTerminal;
            }
            if (lower.contains("windsurf")) {
                cachedTerminal = Terminal.WINDSURF;
                return cachedTerminal;
            }
            if (lower.contains("com.google.android.studio")) {
                cachedTerminal = Terminal.JETBRAINS; // Android Studio
                return cachedTerminal;
            }
            for (String ide : JETBRAINS_IDES) {
                if (lower.contains(ide)) {
                    cachedTerminal = Terminal.JETBRAINS;
                    return cachedTerminal;
                }
            }
        }

        // Visual Studio detection
        if (env.containsKey("VisualStudioVersion")) {
            cachedTerminal = Terminal.UNKNOWN;
            return cachedTerminal;
        }

        // JetBrains terminal on Linux/Windows
        String terminalEmulator = env.get("TERMINAL_EMULATOR");
        if ("JetBrains-JediTerm".equals(terminalEmulator)) {
            cachedTerminal = Terminal.JETBRAINS;
            return cachedTerminal;
        }

        // TERM-based detection
        String term = env.get("TERM");
        if (term != null) {
            if ("xterm-ghostty".equals(term)) {
                cachedTerminal = Terminal.GHOSTTY;
                return cachedTerminal;
            }
            if (term.contains("kitty")) {
                cachedTerminal = Terminal.KITTY;
                return cachedTerminal;
            }
        }

        // TERM_PROGRAM detection
        String termProgram = env.get("TERM_PROGRAM");
        if (termProgram != null) {
            switch (termProgram.toLowerCase()) {
                case "iterm.app":
                    cachedTerminal = Terminal.ITERM2;
                    return cachedTerminal;
                case "apple_terminal":
                    cachedTerminal = Terminal.APPLE_TERMINAL;
                    return cachedTerminal;
                case "vscode":
                    cachedTerminal = Terminal.VSCODE;
                    return cachedTerminal;
                default:
                    // Try to match known terminals
                    cachedTerminal = Terminal.UNKNOWN;
                    return cachedTerminal;
            }
        }

        // TMUX/Screen detection
        if (env.containsKey("TMUX")) {
            cachedTerminal = Terminal.TMUX;
            return cachedTerminal;
        }
        if (env.containsKey("STY")) {
            cachedTerminal = Terminal.SCREEN;
            return cachedTerminal;
        }

        // Linux terminal-specific env vars
        if (env.containsKey("KONSOLE_VERSION")) {
            cachedTerminal = Terminal.KONSOLE;
            return cachedTerminal;
        }
        if (env.containsKey("GNOME_TERMINAL_SERVICE")) {
            cachedTerminal = Terminal.GNOME_TERMINAL;
            return cachedTerminal;
        }
        if (env.containsKey("XTERM_VERSION")) {
            cachedTerminal = Terminal.XTERM;
            return cachedTerminal;
        }
        if (env.containsKey("TERMINATOR_UUID")) {
            cachedTerminal = Terminal.TERMINATOR;
            return cachedTerminal;
        }
        if (env.containsKey("KITTY_WINDOW_ID")) {
            cachedTerminal = Terminal.KITTY;
            return cachedTerminal;
        }
        if (env.containsKey("ALACRITTY_LOG")) {
            cachedTerminal = Terminal.ALACRITTY;
            return cachedTerminal;
        }
        if (env.containsKey("TILIX_ID")) {
            cachedTerminal = Terminal.TILIX;
            return cachedTerminal;
        }

        // Windows-specific detection
        if (env.containsKey("WT_SESSION")) {
            cachedTerminal = Terminal.WINDOWS_TERMINAL;
            return cachedTerminal;
        }
        if (env.containsKey("SESSIONNAME") && "cygwin".equals(term)) {
            cachedTerminal = Terminal.CYGWIN;
            return cachedTerminal;
        }
        if (env.containsKey("MSYSTEM")) {
            cachedTerminal = Terminal.CYGWIN; // MINGW64, MSYS2 variants
            return cachedTerminal;
        }
        if (env.containsKey("ConEmuANSI") || env.containsKey("ConEmuPID") || env.containsKey("ConEmuTask")) {
            cachedTerminal = Terminal.CONEMU;
            return cachedTerminal;
        }

        // WSL detection
        if (env.containsKey("WSL_DISTRO_NAME")) {
            cachedTerminal = Terminal.UNKNOWN; // WSL variant
            return cachedTerminal;
        }

        // SSH session detection
        if (isSSHSession()) {
            cachedTerminal = Terminal.SSH_SESSION;
            return cachedTerminal;
        }

        // Fall back to TERM
        if (term != null) {
            if (term.contains("alacritty")) {
                cachedTerminal = Terminal.ALACRITTY;
                return cachedTerminal;
            }
            if (term.contains("rxvt")) {
                cachedTerminal = Terminal.XTERM; // RXVT variant
                return cachedTerminal;
            }
            if (term.contains("termite")) {
                cachedTerminal = Terminal.UNKNOWN;
                return cachedTerminal;
            }
            cachedTerminal = Terminal.UNKNOWN;
            return cachedTerminal;
        }

        // Non-interactive check
        if (!isTTY()) {
            cachedTerminal = Terminal.NON_INTERACTIVE;
            return cachedTerminal;
        }

        cachedTerminal = Terminal.UNKNOWN;
        return cachedTerminal;
    }

    /**
     * Check if running in SSH session.
     */
    public static boolean isSSHSession() {
        Map<String, String> env = System.getenv();
        return env.containsKey("SSH_CONNECTION") ||
               env.containsKey("SSH_CLIENT") ||
               env.containsKey("SSH_TTY");
    }

    /**
     * Check if stdout is TTY.
     */
    public static boolean isTTY() {
        // In Java, we can check if console is available
        return System.console() != null;
    }

    /**
     * Detect deployment environment.
     */
    public static DeploymentEnvironment detectDeploymentEnvironment() {
        if (cachedDeploymentEnv != null) {
            return cachedDeploymentEnv;
        }

        Map<String, String> env = System.getenv();

        // Cloud development environments
        if (EnvUtils.isEnvTruthy("CODESPACES")) {
            cachedDeploymentEnv = DeploymentEnvironment.CODESPACES;
            return cachedDeploymentEnv;
        }
        if (env.containsKey("GITPOD_WORKSPACE_ID")) {
            cachedDeploymentEnv = DeploymentEnvironment.GITPOD;
            return cachedDeploymentEnv;
        }
        if (env.containsKey("REPL_ID") || env.containsKey("REPL_SLUG")) {
            cachedDeploymentEnv = DeploymentEnvironment.REPLIT;
            return cachedDeploymentEnv;
        }
        if (env.containsKey("PROJECT_DOMAIN")) {
            cachedDeploymentEnv = DeploymentEnvironment.GLITCH;
            return cachedDeploymentEnv;
        }

        // Cloud platforms
        if (EnvUtils.isEnvTruthy("VERCEL")) {
            cachedDeploymentEnv = DeploymentEnvironment.VERCEL;
            return cachedDeploymentEnv;
        }
        if (env.containsKey("RAILWAY_ENVIRONMENT_NAME") || env.containsKey("RAILWAY_SERVICE_NAME")) {
            cachedDeploymentEnv = DeploymentEnvironment.RAILWAY;
            return cachedDeploymentEnv;
        }
        if (EnvUtils.isEnvTruthy("RENDER")) {
            cachedDeploymentEnv = DeploymentEnvironment.RENDER;
            return cachedDeploymentEnv;
        }
        if (EnvUtils.isEnvTruthy("NETLIFY")) {
            cachedDeploymentEnv = DeploymentEnvironment.NETLIFY;
            return cachedDeploymentEnv;
        }
        if (env.containsKey("DYNO")) {
            cachedDeploymentEnv = DeploymentEnvironment.HEROKU;
            return cachedDeploymentEnv;
        }
        if (env.containsKey("FLY_APP_NAME") || env.containsKey("FLY_MACHINE_ID")) {
            cachedDeploymentEnv = DeploymentEnvironment.FLY_IO;
            return cachedDeploymentEnv;
        }
        if (EnvUtils.isEnvTruthy("CF_PAGES")) {
            cachedDeploymentEnv = DeploymentEnvironment.CLOUDFLARE_PAGES;
            return cachedDeploymentEnv;
        }
        if (env.containsKey("DENO_DEPLOYMENT_ID")) {
            cachedDeploymentEnv = DeploymentEnvironment.DENO_DEPLOY;
            return cachedDeploymentEnv;
        }
        if (env.containsKey("AWS_LAMBDA_FUNCTION_NAME")) {
            cachedDeploymentEnv = DeploymentEnvironment.AWS_LAMBDA;
            return cachedDeploymentEnv;
        }
        if ("AWS_ECS_FARGATE".equals(env.get("AWS_EXECUTION_ENV"))) {
            cachedDeploymentEnv = DeploymentEnvironment.AWS_FARGATE;
            return cachedDeploymentEnv;
        }
        if ("AWS_ECS_EC2".equals(env.get("AWS_EXECUTION_ENV"))) {
            cachedDeploymentEnv = DeploymentEnvironment.AWS_ECS;
            return cachedDeploymentEnv;
        }

        // Check for EC2 via hypervisor UUID
        try {
            Path uuidPath = Paths.get("/sys/hypervisor/uuid");
            if (Files.exists(uuidPath)) {
                String uuid = Files.readString(uuidPath).trim().toLowerCase();
                if (uuid.startsWith("ec2")) {
                    cachedDeploymentEnv = DeploymentEnvironment.AWS_EC2;
                    return cachedDeploymentEnv;
                }
            }
        } catch (Exception e) {
            // Ignore
        }

        if (env.containsKey("K_SERVICE")) {
            cachedDeploymentEnv = DeploymentEnvironment.GCP_CLOUD_RUN;
            return cachedDeploymentEnv;
        }
        if (env.containsKey("GOOGLE_CLOUD_PROJECT")) {
            cachedDeploymentEnv = DeploymentEnvironment.GCP;
            return cachedDeploymentEnv;
        }
        if (env.containsKey("WEBSITE_SITE_NAME") || env.containsKey("WEBSITE_SKU")) {
            cachedDeploymentEnv = DeploymentEnvironment.AZURE_APP_SERVICE;
            return cachedDeploymentEnv;
        }
        if (env.containsKey("AZURE_FUNCTIONS_ENVIRONMENT")) {
            cachedDeploymentEnv = DeploymentEnvironment.AZURE_FUNCTIONS;
            return cachedDeploymentEnv;
        }
        String appUrl = env.get("APP_URL");
        if (appUrl != null && appUrl.contains("ondigitalocean.app")) {
            cachedDeploymentEnv = DeploymentEnvironment.DIGITALOCEAN_APP_PLATFORM;
            return cachedDeploymentEnv;
        }
        if (env.containsKey("SPACE_CREATOR_USER_ID")) {
            cachedDeploymentEnv = DeploymentEnvironment.HUGGINGFACE_SPACES;
            return cachedDeploymentEnv;
        }

        // CI/CD platforms
        if (EnvUtils.isEnvTruthy("GITHUB_ACTIONS")) {
            cachedDeploymentEnv = DeploymentEnvironment.GITHUB_ACTIONS;
            return cachedDeploymentEnv;
        }
        if (EnvUtils.isEnvTruthy("GITLAB_CI")) {
            cachedDeploymentEnv = DeploymentEnvironment.GITLAB_CI;
            return cachedDeploymentEnv;
        }
        if (env.containsKey("CIRCLECI")) {
            cachedDeploymentEnv = DeploymentEnvironment.CIRCLECI;
            return cachedDeploymentEnv;
        }
        if (env.containsKey("BUILDKITE")) {
            cachedDeploymentEnv = DeploymentEnvironment.BUILDKITE;
            return cachedDeploymentEnv;
        }
        if (EnvUtils.isEnvTruthy("CI")) {
            cachedDeploymentEnv = DeploymentEnvironment.CI;
            return cachedDeploymentEnv;
        }

        // Container orchestration
        if (env.containsKey("KUBERNETES_SERVICE_HOST")) {
            cachedDeploymentEnv = DeploymentEnvironment.KUBERNETES;
            return cachedDeploymentEnv;
        }
        try {
            if (Files.exists(Paths.get("/.dockerenv"))) {
                cachedDeploymentEnv = DeploymentEnvironment.DOCKER;
                return cachedDeploymentEnv;
            }
        } catch (Exception e) {
            // Ignore
        }

        // Platform-specific fallback
        switch (getPlatform()) {
            case DARWIN:
                cachedDeploymentEnv = DeploymentEnvironment.UNKNOWN_DARWIN;
                break;
            case LINUX:
                cachedDeploymentEnv = DeploymentEnvironment.UNKNOWN_LINUX;
                break;
            case WIN32:
                cachedDeploymentEnv = DeploymentEnvironment.UNKNOWN_WIN32;
                break;
            default:
                cachedDeploymentEnv = DeploymentEnvironment.UNKNOWN;
        }

        return cachedDeploymentEnv;
    }

    /**
     * Check if running in WSL environment.
     */
    public static boolean isWslEnvironment() {
        if (cachedWslEnvironment != null) {
            return cachedWslEnvironment;
        }

        try {
            Path wslInterop = Paths.get("/proc/sys/fs/binfmt_misc/WSLInterop");
            cachedWslEnvironment = Files.exists(wslInterop);
        } catch (Exception e) {
            cachedWslEnvironment = false;
        }

        return cachedWslEnvironment;
    }

    /**
     * Check if running via Conductor.
     */
    public static boolean isConductor() {
        return "com.conductor.app".equals(System.getenv("__CFBundleIdentifier"));
    }

    /**
     * Get package managers available.
     */
    public static CompletableFuture<List<String>> getPackageManagers() {
        if (cachedPackageManagers != null) {
            return CompletableFuture.completedFuture(cachedPackageManagers);
        }

        return CompletableFuture.supplyAsync(() -> {
            List<String> managers = new ArrayList<>();

            if (isCommandAvailable("npm")) managers.add("npm");
            if (isCommandAvailable("yarn")) managers.add("yarn");
            if (isCommandAvailable("pnpm")) managers.add("pnpm");

            cachedPackageManagers = managers;
            return managers;
        });
    }

    /**
     * Get runtimes available.
     */
    public static CompletableFuture<List<String>> getRuntimes() {
        if (cachedRuntimes != null) {
            return CompletableFuture.completedFuture(cachedRuntimes);
        }

        return CompletableFuture.supplyAsync(() -> {
            List<String> runtimes = new ArrayList<>();

            if (isCommandAvailable("bun")) runtimes.add("bun");
            if (isCommandAvailable("deno")) runtimes.add("deno");
            if (isCommandAvailable("node")) runtimes.add("node");
            if (isCommandAvailable("java")) runtimes.add("java");

            cachedRuntimes = runtimes;
            return runtimes;
        });
    }

    /**
     * Check if a command is available.
     */
    public static boolean isCommandAvailable(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                EnvUtils.isWindows() ? "where" : "which",
                command
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            process.waitFor(5, TimeUnit.SECONDS);
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Check internet access (async).
     */
    public static CompletableFuture<Boolean> hasInternetAccess() {
        if (cachedInternetAccess != null) {
            return CompletableFuture.completedFuture(cachedInternetAccess);
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Simple connectivity check
                ProcessBuilder pb = new ProcessBuilder("ping", "-c", "1", "1.1.1.1");
                pb.redirectErrorStream(true);
                Process process = pb.start();
                process.waitFor(3, TimeUnit.SECONDS);
                cachedInternetAccess = process.exitValue() == 0;
                return cachedInternetAccess;
            } catch (Exception e) {
                cachedInternetAccess = false;
                return false;
            }
        });
    }

    /**
     * Get host platform for analytics.
     */
    public static Platform getHostPlatformForAnalytics() {
        String override = System.getenv("CLAUDE_CODE_HOST_PLATFORM");
        if (override != null) {
            switch (override.toLowerCase()) {
                case "win32":
                    return Platform.WIN32;
                case "darwin":
                    return Platform.DARWIN;
                case "linux":
                    return Platform.LINUX;
            }
        }
        return getPlatform();
    }

    /**
     * Get terminal name string.
     */
    public static String getTerminalName() {
        Terminal terminal = detectTerminal();
        return terminal.name().toLowerCase().replace("_", "-");
    }

    /**
     * Environment info record.
     */
    public record EnvironmentInfo(
        Platform platform,
        String arch,
        String javaVersion,
        Terminal terminal,
        DeploymentEnvironment deployment,
        boolean isSSH,
        boolean isCI,
        boolean isWsl,
        boolean isConductor,
        List<String> packageManagers,
        List<String> runtimes
    ) {}

    /**
     * Get full environment info.
     */
    public static CompletableFuture<EnvironmentInfo> getEnvironmentInfo() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<String> pkgManagers = getPackageManagers().get(5, TimeUnit.SECONDS);
                List<String> runtimes = getRuntimes().get(5, TimeUnit.SECONDS);

                return new EnvironmentInfo(
                    getPlatform(),
                    getArch(),
                    getJavaVersion(),
                    detectTerminal(),
                    detectDeploymentEnvironment(),
                    isSSHSession(),
                    EnvUtils.isCI(),
                    isWslEnvironment(),
                    isConductor(),
                    pkgManagers,
                    runtimes
                );
            } catch (Exception e) {
                return new EnvironmentInfo(
                    getPlatform(),
                    getArch(),
                    getJavaVersion(),
                    detectTerminal(),
                    detectDeploymentEnvironment(),
                    isSSHSession(),
                    EnvUtils.isCI(),
                    isWslEnvironment(),
                    isConductor(),
                    List.of(),
                    List.of()
                );
            }
        });
    }
}