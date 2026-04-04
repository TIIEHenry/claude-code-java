/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/promptSuggestion
 */
package com.anthropic.claudecode.services.prompts;

import java.util.*;
import java.util.stream.*;

/**
 * Prompt suggestion service - Generate prompt suggestions.
 */
public final class PromptSuggestionService {
    private final List<PromptTemplate> templates = new ArrayList<>();
    private final List<String> recentPrompts = new ArrayList<>();

    /**
     * Prompt template record.
     */
    public record PromptTemplate(
        String id,
        String name,
        String template,
        String description,
        List<String> variables,
        List<String> tags,
        int usageCount
    ) {
        public String apply(Map<String, String> values) {
            String result = template;
            for (Map.Entry<String, String> entry : values.entrySet()) {
                result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
            return result;
        }
    }

    /**
     * Create prompt suggestion service.
     */
    public PromptSuggestionService() {
        loadDefaultTemplates();
    }

    /**
     * Load default templates.
     */
    private void loadDefaultTemplates() {
        templates.add(new PromptTemplate(
            "explain", "Explain Code",
            "Explain how this code works: {{code}}",
            "Explain a piece of code",
            List.of("code"),
            List.of("code", "explanation"),
            0
        ));

        templates.add(new PromptTemplate(
            "fix", "Fix Bug",
            "Fix the bug in this code: {{code}}\nError: {{error}}",
            "Fix a bug in code",
            List.of("code", "error"),
            List.of("bug", "fix"),
            0
        ));

        templates.add(new PromptTemplate(
            "refactor", "Refactor Code",
            "Refactor this code to improve {{aspect}}: {{code}}",
            "Refactor code for improvements",
            List.of("code", "aspect"),
            List.of("refactor", "improve"),
            0
        ));

        templates.add(new PromptTemplate(
            "test", "Write Tests",
            "Write tests for: {{code}}",
            "Generate tests for code",
            List.of("code"),
            List.of("test", "testing"),
            0
        ));

        templates.add(new PromptTemplate(
            "document", "Add Documentation",
            "Add documentation to: {{code}}",
            "Add documentation to code",
            List.of("code"),
            List.of("docs", "documentation"),
            0
        ));

        templates.add(new PromptTemplate(
            "optimize", "Optimize Performance",
            "Optimize this code for {{goal}}: {{code}}",
            "Optimize code performance",
            List.of("code", "goal"),
            List.of("performance", "optimize"),
            0
        ));
    }

    /**
     * Get suggestions based on context.
     */
    public List<PromptSuggestion> getSuggestions(String context, int limit) {
        List<PromptSuggestion> suggestions = new ArrayList<>();

        // Add template-based suggestions
        for (PromptTemplate template : templates) {
            double score = calculateRelevance(template, context);
            if (score > 0) {
                suggestions.add(new PromptSuggestion(
                    template.name(),
                    template.template(),
                    template.description(),
                    score,
                    SuggestionType.TEMPLATE
                ));
            }
        }

        // Add recent prompt suggestions
        for (String recent : recentPrompts) {
            if (recent.toLowerCase().contains(context.toLowerCase())) {
                suggestions.add(new PromptSuggestion(
                    recent,
                    recent,
                    "Recent prompt",
                    0.8,
                    SuggestionType.RECENT
                ));
            }
        }

        return suggestions.stream()
            .sorted((a, b) -> Double.compare(b.score(), a.score()))
            .limit(limit)
            .toList();
    }

    /**
     * Calculate relevance score.
     */
    private double calculateRelevance(PromptTemplate template, String context) {
        String lower = context.toLowerCase();

        for (String tag : template.tags()) {
            if (lower.contains(tag)) {
                return 0.9;
            }
        }

        if (template.name().toLowerCase().contains(lower)) {
            return 0.8;
        }

        if (template.description().toLowerCase().contains(lower)) {
            return 0.7;
        }

        return 0.5;
    }

    /**
     * Add recent prompt.
     */
    public void addRecentPrompt(String prompt) {
        recentPrompts.remove(prompt);
        recentPrompts.add(0, prompt);

        if (recentPrompts.size() > 100) {
            recentPrompts.remove(recentPrompts.size() - 1);
        }
    }

    /**
     * Add custom template.
     */
    public void addTemplate(PromptTemplate template) {
        templates.add(template);
    }

    /**
     * Get templates.
     */
    public List<PromptTemplate> getTemplates() {
        return Collections.unmodifiableList(templates);
    }

    /**
     * Get templates by tag.
     */
    public List<PromptTemplate> getTemplatesByTag(String tag) {
        return templates.stream()
            .filter(t -> t.tags().contains(tag))
            .toList();
    }

    /**
     * Prompt suggestion record.
     */
    public record PromptSuggestion(
        String title,
        String prompt,
        String description,
        double score,
        SuggestionType type
    ) {}

    /**
     * Suggestion type enum.
     */
    public enum SuggestionType {
        TEMPLATE,
        RECENT,
        CONTEXT_BASED,
        HISTORY_BASED
    }

    /**
     * Speculation result.
     */
    public record SpeculationResult(
        String predictedPrompt,
        double confidence,
        List<String> alternatives
    ) {
        public static SpeculationResult empty() {
            return new SpeculationResult("", 0.0, Collections.emptyList());
        }
    }

    /**
     * Speculate next prompt.
     */
    public SpeculationResult speculateNextPrompt(List<String> conversationHistory) {
        if (conversationHistory.isEmpty()) {
            return SpeculationResult.empty();
        }

        String lastMessage = conversationHistory.get(conversationHistory.size() - 1).toLowerCase();

        // Simple speculation based on last message
        if (lastMessage.contains("error") || lastMessage.contains("bug")) {
            return new SpeculationResult("Fix this error", 0.7, List.of("Debug this issue", "Help me fix this"));
        }
        if (lastMessage.contains("explain") || lastMessage.contains("how")) {
            return new SpeculationResult("Show me an example", 0.6, List.of("Can you elaborate?", "Give more details"));
        }
        if (lastMessage.contains("code") || lastMessage.contains("function")) {
            return new SpeculationResult("Write tests for this", 0.5, List.of("Add documentation", "Refactor this"));
        }

        return SpeculationResult.empty();
    }
}