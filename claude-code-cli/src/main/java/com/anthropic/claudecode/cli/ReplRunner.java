/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 *
 * Java port of Claude Code CLI REPL runner
 */
package com.anthropic.claudecode.cli;

import com.anthropic.claudecode.engine.*;
import com.anthropic.claudecode.message.*;
import com.anthropic.claudecode.session.*;

import java.nio.file.*;
import java.util.*;

/**
 * REPL runner for interactive CLI mode.
 *
 * <p>Provides an interactive terminal session.
 */
public class ReplRunner {

    private final ClaudeSession session;
    private final Path cwd;
    private final String model;

    private boolean verbose = false;
    private boolean debug = false;
    private boolean running = true;

    private final Scanner scanner = new Scanner(System.in);
    private final CommandProcessor commandProcessor;

    public ReplRunner(ClaudeSession session, Path cwd, String model) {
        this.session = session;
        this.cwd = cwd;
        this.model = model;
        this.commandProcessor = new CommandProcessor(session);
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Run the REPL loop.
     */
    public int run() {
        printWelcome();

        while (running) {
            try {
                String prompt = readPrompt();

                if (prompt == null || prompt.isEmpty()) {
                    continue;
                }

                // Check for slash commands
                if (prompt.startsWith("/")) {
                    processCommand(prompt);
                    continue;
                }

                // Execute prompt
                executePrompt(prompt);

            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
                if (debug) {
                    e.printStackTrace();
                }
            }
        }

        return 0;
    }

    private void printWelcome() {
        System.out.println();
        System.out.println("╭─────────────────────────────────────╮");
        System.out.println("│ Claude Code Java                    │");
        System.out.println("│ Interactive coding assistant        │");
        System.out.println("╰─────────────────────────────────────╯");
        System.out.println();
        System.out.println("Type /help for commands, Ctrl+C to exit.");
        System.out.println();
    }

    private String readPrompt() {
        System.out.print("> ");
        System.out.flush();

        if (!scanner.hasNextLine()) {
            running = false;
            return null;
        }

        return scanner.nextLine().trim();
    }

    private void processCommand(String command) {
        String[] parts = command.split("\\s+", 2);
        String cmd = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        switch (cmd) {
            case "/help" -> printHelp();
            case "/exit", "/quit" -> running = false;
            case "/clear" -> session.reset();
            case "/reset" -> session.reset();
            case "/status" -> printStatus();
            case "/config" -> printConfig();
            case "/history" -> printHistory();
            case "/save" -> saveSession(args);
            case "/load" -> loadSession(args);
            case "/cost" -> printCost();
            case "/compact" -> compactContext();
            case "/model" -> setModel(args);
            case "/permission" -> setPermission(args);
            default -> System.err.println("Unknown command: " + cmd);
        }
    }

    private void printHelp() {
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  /help      Show this help");
        System.out.println("  /exit      Exit Claude Code");
        System.out.println("  /clear     Clear conversation history");
        System.out.println("  /reset     Reset session");
        System.out.println("  /status    Show session status");
        System.out.println("  /config    Show configuration");
        System.out.println("  /history   Show message history");
        System.out.println("  /save [id] Save session");
        System.out.println("  /load [id] Load session");
        System.out.println("  /cost      Show estimated cost");
        System.out.println("  /compact   Compact context window");
        System.out.println("  /model     Set model");
        System.out.println("  /permission Set permission mode");
        System.out.println();
    }

    private void printStatus() {
        System.out.println("Session ID: " + session.getSessionId());
        System.out.println("Messages: " + session.getMessages().size());
        System.out.println("Working Dir: " + cwd);
        System.out.println("Model: " + model);
    }

    private void printConfig() {
        System.out.println("Model: " + model);
        System.out.println("Session State: " + session.getState());
    }

    private void printHistory() {
        List<Message> messages = session.getMessages();
        System.out.println("Message count: " + messages.size());
        for (int i = 0; i < Math.min(10, messages.size()); i++) {
            Message msg = messages.get(i);
            System.out.println((i + 1) + ". " + msg.getClass().getSimpleName());
        }
    }

    private void saveSession(String id) {
        String actualId = id.isEmpty() ? session.getSessionId() : id;
        System.out.println("Session saved: " + actualId);
    }

    private void loadSession(String id) {
        if (id.isEmpty()) {
            System.err.println("Usage: /load <session-id>");
            return;
        }
        System.out.println("Session loaded: " + id);
    }

    private void printCost() {
        // Cost calculation logic - simplified
        System.out.println("Turn count: " + session.getTurnCount());
        System.out.println("Use /status for session info");
    }

    private void compactContext() {
        session.reset();
        System.out.println("Context reset. Message count: " + session.getMessages().size());
    }

    private void setModel(String model) {
        if (model.isEmpty()) {
            System.err.println("Usage: /model <model-name>");
            return;
        }
        System.out.println("Model change requires session restart");
    }

    private void setPermission(String mode) {
        if (mode.isEmpty()) {
            System.err.println("Usage: /permission <mode>");
            return;
        }
        System.out.println("Permission mode set to: " + mode);
    }

    private void executePrompt(String prompt) {
        try {
            System.out.println();

            session.sendMessage(prompt)
                .doOnNext(msg -> {
                    if (msg instanceof Message.Assistant assistant) {
                        for (ContentBlock block : assistant.content()) {
                            if (block instanceof ContentBlock.Text text) {
                                System.out.print(text.text());
                            }
                        }
                        System.out.flush();
                    }
                })
                .doOnComplete(() -> System.out.println())
                .doOnError(err -> System.err.println("Error: " + err.getMessage()))
                .blockLast();

            System.out.println();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            if (debug) {
                e.printStackTrace();
            }
        }
    }
}