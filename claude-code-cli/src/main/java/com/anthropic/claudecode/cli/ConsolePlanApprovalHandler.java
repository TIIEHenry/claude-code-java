/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 *
 * Java port of Claude Code CLI plan approval handler
 */
package com.anthropic.claudecode.cli;

import com.anthropic.claudecode.tools.*;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.Scanner;

/**
 * Console-based plan approval handler.
 *
 * <p>Handles plan approval via terminal.
 */
public class ConsolePlanApprovalHandler implements ExitPlanModeTool.PlanApprovalHandler {

    private final Scanner scanner = new Scanner(System.in);

    @Override
    public CompletableFuture<Boolean> requestApproval(Path planFile, String planContent) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println();
            System.out.println("=== Plan Review ===");
            System.out.println();
            System.out.println("Plan file: " + planFile);
            System.out.println();
            System.out.println(planContent);
            System.out.println();
            System.out.println("Approve this plan? [y/n]: ");
            System.out.flush();

            String input = scanner.nextLine().trim().toLowerCase();
            return "y".equals(input) || "yes".equals(input);
        });
    }
}