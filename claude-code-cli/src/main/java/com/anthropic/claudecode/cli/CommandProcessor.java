/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 *
 * Java port of Claude Code CLI command processor
 */
package com.anthropic.claudecode.cli;

import com.anthropic.claudecode.session.*;

/**
 * Processes slash commands in CLI.
 */
public class CommandProcessor {

    private final ClaudeSession session;

    public CommandProcessor(ClaudeSession session) {
        this.session = session;
    }

    /**
     * Process a command string.
     */
    public void process(String command) {
        String[] parts = command.split("\\s+", 2);
        String cmd = parts[0];
        String args = parts.length > 1 ? parts[1] : "";

        switch (cmd.toLowerCase()) {
            case "/help" -> showHelp();
            case "/clear" -> clearHistory();
            case "/compact" -> clearHistory();
            case "/cost" -> showCost();
            case "/status" -> showStatus();
            default -> System.err.println("Unknown command: " + cmd);
        }
    }

    private void showHelp() {
        System.out.println("Available commands:");
        System.out.println("  /help    - Show this help");
        System.out.println("  /clear   - Clear conversation");
        System.out.println("  /compact - Compact context");
        System.out.println("  /cost    - Show usage cost");
        System.out.println("  /status  - Show session status");
    }

    private void clearHistory() {
        session.reset();
        System.out.println("Conversation cleared.");
    }

    private void showCost() {
        System.out.println("Turn count: " + session.getTurnCount());
        System.out.println("Messages: " + session.getMessages().size());
    }

    private void showStatus() {
        System.out.println("Session: " + session.getSessionId());
        System.out.println("Messages: " + session.getMessages().size());
        System.out.println("State: " + session.getState());
        System.out.println("Turns: " + session.getTurnCount());
    }
}