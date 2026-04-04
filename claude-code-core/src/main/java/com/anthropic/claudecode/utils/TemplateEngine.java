/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code template engine
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.regex.*;

/**
 * Simple template engine.
 */
public final class TemplateEngine {
    private final String delimiterStart;
    private final String delimiterEnd;
    private final Pattern pattern;

    public TemplateEngine() {
        this("${", "}");
    }

    public TemplateEngine(String delimiterStart, String delimiterEnd) {
        this.delimiterStart = delimiterStart;
        this.delimiterEnd = delimiterEnd;
        this.pattern = Pattern.compile(
            Pattern.quote(delimiterStart) + "([^" + Pattern.quote(delimiterEnd) + "]*)" + Pattern.quote(delimiterEnd)
        );
    }

    /**
     * Render template with variable values.
     */
    public String render(String template, Map<String, Object> variables) {
        if (template == null) return "";
        if (variables == null) variables = Collections.emptyMap();

        StringBuffer result = new StringBuffer();
        Matcher matcher = pattern.matcher(template);

        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = variables.get(key);
            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Render with default value for missing keys.
     */
    public String render(String template, Map<String, Object> variables, String defaultValue) {
        if (template == null) return "";
        if (variables == null) variables = Collections.emptyMap();

        StringBuffer result = new StringBuffer();
        Matcher matcher = pattern.matcher(template);

        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = variables.get(key);
            String replacement = value != null ? value.toString() : defaultValue;
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Render with function for missing keys.
     */
    public String render(String template, Map<String, Object> variables,
            java.util.function.Function<String, String> defaultProvider) {
        if (template == null) return "";
        if (variables == null) variables = Collections.emptyMap();

        StringBuffer result = new StringBuffer();
        Matcher matcher = pattern.matcher(template);

        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = variables.get(key);
            String replacement = value != null ? value.toString() : defaultProvider.apply(key);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Extract all variable names from template.
     */
    public Set<String> extractVariables(String template) {
        if (template == null) return Collections.emptySet();

        Set<String> variables = new HashSet<>();
        Matcher matcher = pattern.matcher(template);

        while (matcher.find()) {
            variables.add(matcher.group(1));
        }

        return variables;
    }

    /**
     * Check if template contains variables.
     */
    public boolean hasVariables(String template) {
        if (template == null) return false;
        return pattern.matcher(template).find();
    }

    /**
     * Count variables in template.
     */
    public int countVariables(String template) {
        if (template == null) return 0;

        int count = 0;
        Matcher matcher = pattern.matcher(template);
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    /**
     * Validate template (check for unclosed delimiters).
     */
    public boolean validate(String template) {
        if (template == null) return true;

        int startCount = 0;
        int endCount = 0;
        int i = 0;

        while (i < template.length()) {
            if (template.startsWith(delimiterStart, i)) {
                startCount++;
                i += delimiterStart.length();
            } else if (template.startsWith(delimiterEnd, i)) {
                endCount++;
                i += delimiterEnd.length();
            } else {
                i++;
            }
        }

        return startCount == endCount;
    }

    /**
     * Static renderer with default engine.
     */
    public static String renderStatic(String template, Map<String, Object> variables) {
        return new TemplateEngine().render(template, variables);
    }

    /**
     * Render with single variable.
     */
    public static String render(String template, String key, Object value) {
        Map<String, Object> vars = new HashMap<>();
        vars.put(key, value);
        return renderStatic(template, vars);
    }

    /**
     * Builder for template.
     */
    public static TemplateBuilder builder() {
        return new TemplateBuilder();
    }

    /**
     * Template builder.
     */
    public static final class TemplateBuilder {
        private final StringBuilder template = new StringBuilder();
        private final TemplateEngine engine = new TemplateEngine();

        public TemplateBuilder append(String text) {
            template.append(text);
            return this;
        }

        public TemplateBuilder appendVariable(String name) {
            template.append("${").append(name).append("}");
            return this;
        }

        public TemplateBuilder append(String text, String variableName) {
            template.append(text).append("${").append(variableName).append("}");
            return this;
        }

        public TemplateBuilder newline() {
            template.append("\n");
            return this;
        }

        public TemplateBuilder clear() {
            template.setLength(0);
            return this;
        }

        public String build() {
            return template.toString();
        }

        public String build(Map<String, Object> variables) {
            return engine.render(build(), variables);
        }

        @Override
        public String toString() {
            return build();
        }
    }

    /**
     * Template utilities.
     */
    public static final class TemplateUtils {
        private TemplateUtils() {}

        /**
         * Create engine with custom delimiters.
         */
        public static TemplateEngine withDelimiters(String start, String end) {
            return new TemplateEngine(start, end);
        }

        /**
         * Create Mustache-style engine.
         */
        public static TemplateEngine mustache() {
            return new TemplateEngine("{{", "}}");
        }

        /**
         * Create percent-style engine.
         */
        public static TemplateEngine percent() {
            return new TemplateEngine("%{", "}");
        }

        /**
         * Create colon-style engine.
         */
        public static TemplateEngine colon() {
            return new TemplateEngine(":", "");
        }

        /**
         * Compile template for reuse.
         */
        public static CompiledTemplate compile(String template) {
            return new CompiledTemplate(template);
        }
    }

    /**
     * Compiled template for efficient reuse.
     */
    public static final class CompiledTemplate {
        private final String template;
        private final TemplateEngine engine;
        private final Set<String> requiredVariables;

        public CompiledTemplate(String template) {
            this.template = template;
            this.engine = new TemplateEngine();
            this.requiredVariables = engine.extractVariables(template);
        }

        public String render(Map<String, Object> variables) {
            return engine.render(template, variables);
        }

        public String render(Map<String, Object> variables, String defaultValue) {
            return engine.render(template, variables, defaultValue);
        }

        public Set<String> getRequiredVariables() {
            return requiredVariables;
        }

        public boolean hasAllVariables(Map<String, Object> variables) {
            return variables.keySet().containsAll(requiredVariables);
        }

        public Set<String> getMissingVariables(Map<String, Object> variables) {
            Set<String> missing = new HashSet<>(requiredVariables);
            missing.removeAll(variables.keySet());
            return missing;
        }

        @Override
        public String toString() {
            return template;
        }
    }
}