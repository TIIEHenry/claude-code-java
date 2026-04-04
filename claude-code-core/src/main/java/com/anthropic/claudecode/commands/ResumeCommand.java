/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code commands/resume
 */
package com.anthropic.claudecode.commands;

import com.anthropic.claudecode.services.sessions.SessionService;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * Resume command - Resume a previous session.
 */
public final class ResumeCommand implements Command {
    private final SessionService sessionService;

    public ResumeCommand() {
        this.sessionService = new SessionService();
    }

    public ResumeCommand(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Override
    public String name() {
        return "resume";
    }

    @Override
    public String description() {
        return "Resume a previous session";
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public boolean supportsNonInteractive() {
        return true;
    }

    @Override
    public CompletableFuture<CommandResult> execute(String args, CommandContext context) {
        if (args == null || args.trim().isEmpty()) {
            return CompletableFuture.completedFuture(listSessions(context));
        }

        String arg = args.trim().toLowerCase();

        return switch (arg) {
            case "list", "ls" -> CompletableFuture.completedFuture(listSessions(context));
            case "latest", "last" -> CompletableFuture.completedFuture(resumeLatest(context));
            default -> CompletableFuture.completedFuture(resumeSession(context, args.trim()));
        };
    }

    private CommandResult listSessions(CommandContext context) {
        List<SessionService.SessionMeta> sessions = sessionService.listSavedSessions();

        StringBuilder sb = new StringBuilder();
        sb.append("Recent Sessions\n");
        sb.append("===============\n\n");

        if (sessions.isEmpty()) {
            sb.append("No previous sessions found.\n");
            sb.append("\nStart a new session and it will be saved automatically.\n");
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd HH:mm")
                .withZone(ZoneId.systemDefault());

            sb.append("Usage: resume <session-id>\n\n");

            for (int i = 0; i < Math.min(sessions.size(), 20); i++) {
                SessionService.SessionMeta meta = sessions.get(i);
                String time = formatter.format(meta.updatedAt());
                String title = truncate(meta.title(), 40);

                sb.append(String.format("  [%s] %s (%d messages) - %s\n",
                    meta.id().substring(0, 8),
                    title,
                    meta.messageCount(),
                    time
                ));
            }

            if (sessions.size() > 20) {
                sb.append("\n  ... and ").append(sessions.size() - 20).append(" more sessions\n");
            }

            sb.append("\nCommands:\n");
            sb.append("  resume <id>  - Resume specific session\n");
            sb.append("  resume last   - Resume most recent session\n");
        }

        return CommandResult.success(sb.toString());
    }

    private CommandResult resumeLatest(CommandContext context) {
        List<SessionService.SessionMeta> sessions = sessionService.listSavedSessions();

        if (sessions.isEmpty()) {
            return CommandResult.failure("No previous session found.\n\nStart a new conversation to create a session.");
        }

        SessionService.SessionMeta latest = sessions.get(0);
        return doResumeSession(latest.id(), latest.title());
    }

    private CommandResult resumeSession(CommandContext context, String sessionId) {
        // Try to find session by partial ID
        List<SessionService.SessionMeta> sessions = sessionService.listSavedSessions();

        SessionService.SessionMeta target = null;
        for (SessionService.SessionMeta meta : sessions) {
            if (meta.id().equals(sessionId) || meta.id().startsWith(sessionId)) {
                target = meta;
                break;
            }
        }

        if (target == null) {
            return CommandResult.failure("Session not found: " + sessionId + "\n\nUse 'resume' or 'resume list' to see available sessions.");
        }

        return doResumeSession(target.id(), target.title());
    }

    private CommandResult doResumeSession(String sessionId, String title) {
        SessionService.Session session = sessionService.loadSession(sessionId);

        if (session == null) {
            return CommandResult.failure("Failed to load session: " + sessionId);
        }

        sessionService.setActiveSession(sessionId);

        StringBuilder sb = new StringBuilder();
        sb.append("Resumed session: ").append(truncate(title, 50)).append("\n");
        sb.append("Session ID: ").append(sessionId).append("\n");
        sb.append("Messages: ").append(session.getMessageCount()).append("\n");

        if (session.projectPath() != null && !session.projectPath().isEmpty()) {
            sb.append("Project: ").append(session.projectPath()).append("\n");
        }

        sb.append("\n--- Last messages ---\n");

        List<SessionService.MessageEntry> messages = session.messages();
        int start = Math.max(0, messages.size() - 5);
        for (int i = start; i < messages.size(); i++) {
            SessionService.MessageEntry msg = messages.get(i);
            String role = msg.role().toString();
            String content = truncate(msg.content(), 200);
            sb.append("\n[").append(role).append("]\n");
            sb.append(content).append("\n");
        }

        return CommandResult.success(sb.toString());
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "..." : s;
    }
}