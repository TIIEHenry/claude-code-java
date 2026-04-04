/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code tools/PowerShellTool/PowerShellTool.tsx
 */
package com.anthropic.claudecode.tools;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.nio.file.*;
import com.anthropic.claudecode.*;
import com.anthropic.claudecode.permission.PermissionResult;
import com.anthropic.claudecode.services.analytics.AnalyticsMetadata;

/**
 * PowerShell Tool - execute PowerShell commands on Windows.
 * Similar to BashTool but for Windows PowerShell environment.
 */
public final class PowerShellTool extends AbstractTool<PowerShellTool.Input, PowerShellTool.Output, ToolProgressData> {
    public static final String TOOL_NAME = "PowerShell";

    // PowerShell search commands for collapsible display
    private static final Set<String> PS_SEARCH_COMMANDS = Set.of(
        "select-string", "get-childitem", "findstr", "where.exe"
    );

    // PowerShell read commands for collapsible display
    private static final Set<String> PS_READ_COMMANDS = Set.of(
        "get-content", "get-item", "test-path", "resolve-path",
        "get-process", "get-service", "get-location", "get-filehash",
        "get-acl", "format-hex"
    );

    // Commands that should not be auto-backgrounded
    private static final Set<String> DISALLOWED_AUTO_BACKGROUND = Set.of(
        "start-sleep", "sleep"
    );

    public PowerShellTool() {
        super(TOOL_NAME, "Execute PowerShell commands on Windows");
    }

    /**
     * Input schema.
     */
    public record Input(
        String command,
        String description,
        Boolean timeout,
        Boolean run_in_background,
        Boolean dangerously_disable_sandbox,
        Boolean show_raw_output,
        Boolean wait_after_exit,
        Boolean dangerous,
        Boolean interactive
    ) {}

    /**
     * Output schema.
     */
    public record Output(
        String stdout,
        String stderr,
        Integer exitCode,
        String message,
        String outputPath,
        boolean isImage,
        String imageId
    ) {}

    @Override
    public String description() {
        return "Execute PowerShell commands on Windows systems";
    }

    @Override
    public String searchHint() {
        return "execute PowerShell commands";
    }

    @Override
    public boolean shouldDefer() {
        return true;
    }

    @Override
    public boolean isDestructive(Input input) {
        return !isReadOnlyCommand(input.command());
    }

    @Override
    public Class<Input> inputType() {
        return Input.class;
    }

    @Override
    public Class<Output> outputType() {
        return Output.class;
    }

    @Override
    public CompletableFuture<ValidationResult> validateInput(Input input, ToolUseContext context) {
        if (input.command() == null || input.command().trim().isEmpty()) {
            return CompletableFuture.completedFuture(ValidationResult.failure("Command is required", 1));
        }

        // Check for blocked sleep patterns
        String blockedSleep = detectBlockedSleepPattern(input.command());
        if (blockedSleep != null) {
            return CompletableFuture.completedFuture(ValidationResult.failure(blockedSleep, 1));
        }

        return CompletableFuture.completedFuture(ValidationResult.success());
    }

    @Override
    public CompletableFuture<PermissionResult> checkPermissions(Input input, ToolUseContext context) {
        // Check if read-only
        if (isReadOnlyCommand(input.command())) {
            return CompletableFuture.completedFuture(PermissionResult.allow(input));
        }

        // Check for dangerous operations
        if (hasSecurityConcerns(input.command())) {
            return CompletableFuture.completedFuture(PermissionResult.ask(
                "PowerShell command may modify files: " + truncateCommand(input.command()), input
            ));
        }

        return CompletableFuture.completedFuture(PermissionResult.ask(
            "Run PowerShell command: " + truncateCommand(input.command()), input
        ));
    }

    @Override
    public CompletableFuture<ToolResult<Output>> call(
        Input input,
        ToolUseContext context,
        CanUseToolFn canUseTool,
        AssistantMessage parent,
        Consumer<ToolProgress<ToolProgressData>> onProgress
    ) {
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            String cwd = System.getProperty("user.dir");

            try {
                // Build PowerShell command
                List<String> command = buildPowerShellCommand(input.command());

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(new java.io.File(cwd));
                pb.environment().putAll(System.getenv());

                // Redirect error stream
                pb.redirectErrorStream(false);

                Process process = pb.start();

                // Read output
                String stdout = new String(process.getInputStream().readAllBytes());
                String stderr = new String(process.getErrorStream().readAllBytes());

                int exitCode = process.waitFor();
                long duration = System.currentTimeMillis() - startTime;

                // Log analytics
                AnalyticsMetadata.logEvent("tengu_powershell_run", Map.of(
                    "exit_code", String.valueOf(exitCode),
                    "duration_ms", String.valueOf(duration),
                    "background", "false"
                ), true);

                return ToolResult.of(new Output(
                    stdout,
                    stderr,
                    exitCode,
                    null,
                    null,
                    false,
                    null
                ));

            } catch (Exception e) {
                throw new RuntimeException("PowerShell command failed: " + e.getMessage(), e);
            }
        });
    }

    @Override
    public String formatResult(Output output) {
        StringBuilder sb = new StringBuilder();

        if (output.stdout() != null && !output.stdout().isEmpty()) {
            sb.append(output.stdout());
        }

        if (output.stderr() != null && !output.stderr().isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("STDERR:\n").append(output.stderr());
        }

        if (output.exitCode() != null && output.exitCode() != 0) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("Exit code: ").append(output.exitCode());
        }

        return sb.length() > 0 ? sb.toString() : "Command completed successfully";
    }

    // Helpers

    private List<String> buildPowerShellCommand(String command) {
        List<String> cmd = new ArrayList<>();
        cmd.add(getPowerShellPath());
        cmd.add("-NoLogo");
        cmd.add("-NonInteractive");
        cmd.add("-Command");
        cmd.add(command);
        return cmd;
    }

    private String getPowerShellPath() {
        // Try to find PowerShell
        String psPath = System.getenv("PS_MODULE_PATH");
        if (psPath != null) {
            return "pwsh"; // PowerShell Core
        }
        return "powershell"; // Windows PowerShell
    }

    private boolean isReadOnlyCommand(String command) {
        if (command == null) return true;
        String trimmed = command.trim();
        if (trimmed.isEmpty()) return true;

        // Check first command
        String[] parts = trimmed.split("[;|]");
        if (parts.length == 0) return true;

        String firstPart = parts[0].trim().split("\\s+")[0];
        if (firstPart.isEmpty()) return true;

        String canonical = resolveToCanonical(firstPart);

        return PS_SEARCH_COMMANDS.contains(canonical) || PS_READ_COMMANDS.contains(canonical);
    }

    private boolean hasSecurityConcerns(String command) {
        if (command == null) return false;

        String lower = command.toLowerCase();
        // Check for dangerous operations
        return lower.contains("remove-item") ||
               lower.contains("del ") ||
               lower.contains("rm ") ||
               lower.contains("move-item") ||
               lower.contains("copy-item") ||
               lower.contains("set-content") ||
               lower.contains("out-file") ||
               lower.contains("invoke-webrequest") ||
               lower.contains("iwr ") ||
               lower.contains("wget") ||
               lower.contains("curl");
    }

    private String resolveToCanonical(String cmd) {
        // Map aliases to canonical names
        return switch (cmd.toLowerCase()) {
            case "cat", "type" -> "get-content";
            case "ls", "dir" -> "get-childitem";
            case "rm", "del", "erase" -> "remove-item";
            case "mv", "move" -> "move-item";
            case "cp", "copy" -> "copy-item";
            case "echo" -> "write-output";
            case "pwd" -> "get-location";
            case "where" -> "where.exe";
            case "sleep" -> "start-sleep";
            case "sls" -> "select-string";
            default -> cmd.toLowerCase();
        };
    }

    private String detectBlockedSleepPattern(String command) {
        if (command == null) return null;

        String first = command.trim().split("[;|&\r\n]")[0].trim();
        // Match: Start-Sleep N, Start-Sleep -Seconds N, sleep N
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "^(?:start-sleep|sleep)(?:\\s+-s(?:econds)?)?\\s+(\\d+)\\s*$",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher m = pattern.matcher(first);
        if (m.matches()) {
            int secs = Integer.parseInt(m.group(1));
            return "Blocking sleep detected: Start-Sleep -Seconds " + secs + ". " +
                   "Use /wait instead for sleeping during non-interactive sessions.";
        }
        return null;
    }

    private String truncateCommand(String command) {
        if (command == null) return "";
        return command.length() > 50 ? command.substring(0, 50) + "..." : command;
    }
}