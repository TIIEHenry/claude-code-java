/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 *
 * REPL-style interactive session handler
 */
package com.anthropic.claudecode.cli;

import com.anthropic.claudecode.*;
import com.anthropic.claudecode.engine.*;
import com.anthropic.claudecode.message.*;
import com.anthropic.claudecode.tools.*;

import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Interactive REPL session for Claude Code.
 *
 * <p>Handles the interactive conversation loop.
 */
public class InteractiveSession {

    private final QueryEngine engine;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Scanner scanner;

    public InteractiveSession(QueryEngine engine) {
        this.engine = engine;
        this.scanner = new Scanner(System.in);
    }

    /**
     * Start the interactive session.
     */
    public void start() {
        printWelcome();

        while (running.get()) {
            String input = readInput();
            if (input == null) {
                break;
            }

            processInput(input);
        }

        printGoodbye();
    }

    /**
     * Stop the session.
     */
    public void stop() {
        running.set(false);
    }

    /**
     * Read input from user.
     */
    private String readInput() {
        System.out.print("\n> ");
        System.out.flush();

        if (!scanner.hasNextLine()) {
            return null;
        }

        return scanner.nextLine().trim();
    }

    /**
     * Process user input.
     */
    private void processInput(String input) {
        if (input.isEmpty()) {
            return;
        }

        // Handle special commands
        if (handleCommand(input)) {
            return;
        }

        // Send to Claude
        sendToClaude(input);
    }

    /**
     * Handle special commands (exit, clear, etc).
     */
    private boolean handleCommand(String input) {
        String lower = input.toLowerCase();

        if (lower.equals("exit") || lower.equals("quit") || lower.equals("q")) {
            stop();
            return true;
        }

        if (lower.equals("clear") || lower.equals("cls")) {
            clearScreen();
            return true;
        }

        if (lower.equals("help") || lower.equals("?")) {
            printHelp();
            return true;
        }

        if (lower.equals("history")) {
            printHistory();
            return true;
        }

        if (lower.startsWith("/")) {
            // Slash command - would route to SkillTool in full implementation
            System.out.println("Slash commands not yet implemented: " + input);
            return true;
        }

        return false;
    }

    /**
     * Send message to Claude and handle response.
     */
    private void sendToClaude(String prompt) {
        System.out.println("\n[Claude is thinking...]");
        System.out.flush();

        try {
            QueryEngine.QueryResult result = engine.submitMessage(
                    prompt,
                    QueryEngine.SubmitOptions.empty()
            ).get();

            // Handle the result
            if (result.status() == QueryEngine.QueryStatus.COMPLETED) {
                System.out.println("\n[Done]");
            } else if (result.status() == QueryEngine.QueryStatus.FAILED) {
                System.err.println("\nError: Query failed");
            }
        } catch (Exception e) {
            handleError(e);
        }
    }

    /**
     * Handle SDK message.
     */
    private void handleMessage(Object message) {
        if (message instanceof Message.Assistant a) {
            printAssistantMessage(a);
        } else if (message instanceof Message.User u) {
            if (verbose) {
                System.err.println("[User message received]");
            }
        }
        // Skip other message types in interactive mode
    }

    private void printAssistantMessage(Message.Assistant a) {
        System.out.println();
        for (ContentBlock block : a.content()) {
            if (block instanceof ContentBlock.Text text) {
                System.out.println(text.text());
            }
        }
    }

    private boolean verbose = false;

    /**
     * Handle error.
     */
    private void handleError(Throwable error) {
        System.err.println("\nError: " + error.getMessage());
        if (error.getCause() != null) {
            System.err.println("Cause: " + error.getCause().getMessage());
        }
    }

    /**
     * Handle completion.
     */
    private void handleComplete() {
        System.out.println("\n[Done]");
        System.out.flush();
    }

    /**
     * Print welcome message.
     */
    private void printWelcome() {
        System.out.println();
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║        Claude Code Java - Interactive      ║");
        System.out.println("║                                            ║");
        System.out.println("║  Anthropic's official CLI port for Claude  ║");
        System.out.println("║                                            ║");
        System.out.println("║  Commands:                                 ║");
        System.out.println("║    exit/quit  - Exit session               ║");
        System.out.println("║    clear      - Clear screen               ║");
        System.out.println("║    help       - Show help                  ║");
        System.out.println("║    history    - Show conversation history  ║");
        System.out.println("╚════════════════════════════════════════════╝");
        System.out.println();
    }

    /**
     * Print goodbye message.
     */
    private void printGoodbye() {
        System.out.println();
        System.out.println("Thanks for using Claude Code Java!");
        System.out.println("Goodbye!");
    }

    /**
     * Print help message.
     */
    private void printHelp() {
        System.out.println();
        System.out.println("Available commands:");
        System.out.println("  exit, quit, q  - Exit the session");
        System.out.println("  clear, cls     - Clear the screen");
        System.out.println("  help, ?        - Show this help message");
        System.out.println("  history        - Show conversation history");
        System.out.println("  /<command>     - Execute a slash command");
        System.out.println();
        System.out.println("For more information, visit:");
        System.out.println("  https://github.com/anthropics/claude-code");
        System.out.println();
    }

    /**
     * Clear screen (platform-dependent).
     */
    private void clearScreen() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // Fallback - just print some blank lines
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }

    /**
     * Print conversation history.
     */
    private void printHistory() {
        List<Object> messages = engine.getMessages();
        System.out.println();
        System.out.println("Conversation history (" + messages.size() + " messages):");
        System.out.println("----------------------------------------");

        for (int i = 0; i < messages.size(); i++) {
            Object msg = messages.get(i);
            System.out.println((i + 1) + ". [" + msg.getClass().getSimpleName() + "]");
        }

        System.out.println("----------------------------------------");
        System.out.println();
    }
}