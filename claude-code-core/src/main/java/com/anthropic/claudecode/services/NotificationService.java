/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/notifier.ts
 */
package com.anthropic.claudecode.services;

import java.util.*;
import java.util.concurrent.*;

/**
 * Notification service.
 *
 * Handles sending notifications through various channels.
 */
public final class NotificationService {
    private NotificationService() {}

    private static volatile String preferredChannel = "auto";
    private static volatile TerminalType detectedTerminal = TerminalType.UNKNOWN;

    /**
     * Terminal type enum.
     */
    public enum TerminalType {
        APPLE_TERMINAL("Apple_Terminal"),
        ITERM2("iTerm.app"),
        KITTY("kitty"),
        GHOSTTY("ghostty"),
        VS_CODE("vscode"),
        JET_BRAINS("jetbrains"),
        UNKNOWN("unknown");

        private final String id;

        TerminalType(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public static TerminalType fromEnv(String termProgram) {
            if (termProgram == null) return UNKNOWN;

            return switch (termProgram.toLowerCase()) {
                case "apple_terminal" -> APPLE_TERMINAL;
                case "iterm.app" -> ITERM2;
                case "kitty" -> KITTY;
                case "ghostty" -> GHOSTTY;
                case "vscode" -> VS_CODE;
                case "jetbrains" -> JET_BRAINS;
                default -> UNKNOWN;
            };
        }
    }

    /**
     * Notification channel enum.
     */
    public enum NotificationChannel {
        AUTO("auto"),
        ITERM2("iterm2"),
        ITERM2_WITH_BELL("iterm2_with_bell"),
        KITTY("kitty"),
        GHOSTTY("ghostty"),
        TERMINAL_BELL("terminal_bell"),
        DISABLED("notifications_disabled");

        private final String id;

        NotificationChannel(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public static NotificationChannel fromId(String id) {
            for (NotificationChannel channel : values()) {
                if (channel.id.equals(id)) {
                    return channel;
                }
            }
            return AUTO;
        }
    }

    /**
     * Notification options.
     */
    public record NotificationOptions(
        String message,
        String title,
        String notificationType
    ) {
        public static NotificationOptions of(String message) {
            return new NotificationOptions(message, "Claude Code", "default");
        }

        public static NotificationOptions withTitle(String message, String title) {
            return new NotificationOptions(message, title, "default");
        }
    }

    /**
     * Set preferred notification channel.
     */
    public static void setPreferredChannel(String channel) {
        preferredChannel = channel != null ? channel : "auto";
    }

    /**
     * Get preferred notification channel.
     */
    public static String getPreferredChannel() {
        return preferredChannel;
    }

    /**
     * Set detected terminal.
     */
    public static void setDetectedTerminal(TerminalType terminal) {
        detectedTerminal = terminal;
    }

    /**
     * Get detected terminal.
     */
    public static TerminalType getDetectedTerminal() {
        return detectedTerminal;
    }

    /**
     * Detect terminal from environment.
     */
    public static TerminalType detectTerminal() {
        String termProgram = System.getenv("TERM_PROGRAM");
        if (termProgram != null) {
            detectedTerminal = TerminalType.fromEnv(termProgram);
            return detectedTerminal;
        }

        // Check other indicators
        String term = System.getenv("TERM");
        if (term != null) {
            if (term.contains("xterm")) {
                // Could be various terminals
            }
        }

        detectedTerminal = TerminalType.UNKNOWN;
        return detectedTerminal;
    }

    /**
     * Send a notification.
     */
    public static CompletableFuture<String> sendNotification(NotificationOptions options) {
        NotificationChannel channel = NotificationChannel.fromId(preferredChannel);
        return sendToChannel(channel, options);
    }

    /**
     * Send notification to a specific channel.
     */
    private static CompletableFuture<String> sendToChannel(
        NotificationChannel channel,
        NotificationOptions options
    ) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return switch (channel) {
                    case AUTO -> sendAuto(options);
                    case ITERM2 -> {
                        // iTerm2 escape sequence for notification
                        System.out.print("\033]9;" + options.message() + "\033\\");
                        yield "iterm2";
                    }
                    case ITERM2_WITH_BELL -> {
                        System.out.print("\033]9;" + options.message() + "\033\\");
                        System.out.print("\007");
                        yield "iterm2_with_bell";
                    }
                    case KITTY -> {
                        // Kitty notification
                        int id = generateKittyId();
                        System.out.print("\033]99;i=" + id + ";p=" + options.title() + ";" + options.message() + "\033\\");
                        yield "kitty";
                    }
                    case GHOSTTY -> {
                        // Ghostty notification
                        System.out.print("\033]99;p=" + options.title() + ";" + options.message() + "\033\\");
                        yield "ghostty";
                    }
                    case TERMINAL_BELL -> {
                        System.out.print("\007");
                        yield "terminal_bell";
                    }
                    case DISABLED -> "disabled";
                };
            } catch (Exception e) {
                return "error";
            }
        });
    }

    /**
     * Auto-detect best notification method.
     */
    private static String sendAuto(NotificationOptions options) {
        TerminalType terminal = detectedTerminal != TerminalType.UNKNOWN
            ? detectedTerminal
            : detectTerminal();

        return switch (terminal) {
            case ITERM2 -> {
                System.out.print("\033]9;" + options.message() + "\033\\");
                yield "iterm2";
            }
            case KITTY -> {
                int id = generateKittyId();
                System.out.print("\033]99;i=" + id + ";p=" + options.title() + ";" + options.message() + "\033\\");
                yield "kitty";
            }
            case GHOSTTY -> {
                System.out.print("\033]99;p=" + options.title() + ";" + options.message() + "\033\\");
                yield "ghostty";
            }
            case APPLE_TERMINAL -> {
                // Apple Terminal - just use bell
                System.out.print("\007");
                yield "terminal_bell";
            }
            default -> "no_method_available";
        };
    }

    /**
     * Generate random Kitty notification ID.
     */
    private static int generateKittyId() {
        return (int) (Math.random() * 10000);
    }

    /**
     * Ring terminal bell.
     */
    public static void ringBell() {
        System.out.print("\007");
        System.out.flush();
    }
}