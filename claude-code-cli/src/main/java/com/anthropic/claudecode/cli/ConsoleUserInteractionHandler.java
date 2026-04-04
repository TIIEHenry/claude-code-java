/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 *
 * Java port of Claude Code CLI console interaction handler
 */
package com.anthropic.claudecode.cli;

import com.anthropic.claudecode.tools.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.Scanner;

/**
 * Console-based user interaction handler.
 *
 * <p>Handles user questions via terminal.
 */
public class ConsoleUserInteractionHandler implements AskUserQuestionTool.UserInteractionHandler {

    private final Scanner scanner = new Scanner(System.in);

    @Override
    public CompletableFuture<Map<String, String>> askQuestions(List<AskUserQuestionTool.Question> questions) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, String> answers = new LinkedHashMap<>();

            for (AskUserQuestionTool.Question q : questions) {
                String answer = askSingleQuestion(q);
                answers.put(q.header(), answer);
            }

            return answers;
        });
    }

    private String askSingleQuestion(AskUserQuestionTool.Question question) {
        System.out.println();
        System.out.println(question.question());
        System.out.println();

        List<AskUserQuestionTool.QuestionOption> options = question.options();
        for (int i = 0; i < options.size(); i++) {
            AskUserQuestionTool.QuestionOption opt = options.get(i);
            System.out.println("  [" + (i + 1) + "] " + opt.label());
            if (opt.description() != null) {
                System.out.println("      " + opt.description());
            }
        }

        if (question.multiSelect()) {
            System.out.println();
            System.out.print("Select multiple (comma-separated numbers): ");
        } else {
            System.out.println();
            System.out.print("Select option (1-" + options.size() + "): ");
        }
        System.out.flush();

        String input = scanner.nextLine().trim();

        if (question.multiSelect()) {
            return parseMultiSelect(input, options);
        } else {
            return parseSingleSelect(input, options);
        }
    }

    private String parseSingleSelect(String input, List<AskUserQuestionTool.QuestionOption> options) {
        try {
            int index = Integer.parseInt(input) - 1;
            if (index >= 0 && index < options.size()) {
                return options.get(index).label();
            }
        } catch (NumberFormatException e) {
            // Try matching by label
            for (AskUserQuestionTool.QuestionOption opt : options) {
                if (opt.label().equalsIgnoreCase(input)) {
                    return opt.label();
                }
            }
        }

        // "Other" option - return the input itself
        return input;
    }

    private String parseMultiSelect(String input, List<AskUserQuestionTool.QuestionOption> options) {
        List<String> selected = new ArrayList<>();

        String[] parts = input.split(",");
        for (String part : parts) {
            part = part.trim();
            try {
                int index = Integer.parseInt(part) - 1;
                if (index >= 0 && index < options.size()) {
                    selected.add(options.get(index).label());
                }
            } catch (NumberFormatException e) {
                // Try matching by label
                for (AskUserQuestionTool.QuestionOption opt : options) {
                    if (opt.label().equalsIgnoreCase(part)) {
                        selected.add(opt.label());
                    }
                }
            }
        }

        return String.join(",", selected);
    }
}