/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code expression utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.regex.*;

/**
 * Expression parsing and evaluation utilities.
 */
public final class ExpressionUtils {
    private ExpressionUtils() {}

    /**
     * Simple expression evaluator.
     */
    public static double evaluate(String expression) {
        return new ExpressionEvaluator(expression).evaluate();
    }

    /**
     * Evaluate expression with variables.
     */
    public static double evaluate(String expression, Map<String, Double> variables) {
        return new ExpressionEvaluator(expression, variables).evaluate();
    }

    /**
     * Expression evaluator class.
     */
    private static class ExpressionEvaluator {
        private final String expression;
        private final Map<String, Double> variables;
        private int pos;

        ExpressionEvaluator(String expression) {
            this(expression, Map.of());
        }

        ExpressionEvaluator(String expression, Map<String, Double> variables) {
            this.expression = expression.replaceAll("\\s+", "");
            this.variables = variables;
            this.pos = 0;
        }

        double evaluate() {
            double result = parseExpression();
            if (pos < expression.length()) {
                throw new RuntimeException("Unexpected character: " + expression.charAt(pos));
            }
            return result;
        }

        private double parseExpression() {
            double result = parseTerm();
            while (pos < expression.length()) {
                char op = expression.charAt(pos);
                if (op == '+') {
                    pos++;
                    result += parseTerm();
                } else if (op == '-') {
                    pos++;
                    result -= parseTerm();
                } else {
                    break;
                }
            }
            return result;
        }

        private double parseTerm() {
            double result = parseFactor();
            while (pos < expression.length()) {
                char op = expression.charAt(pos);
                if (op == '*') {
                    pos++;
                    result *= parseFactor();
                } else if (op == '/') {
                    pos++;
                    result /= parseFactor();
                } else if (op == '%') {
                    pos++;
                    result %= parseFactor();
                } else {
                    break;
                }
            }
            return result;
        }

        private double parseFactor() {
            double result = parsePower();
            while (pos < expression.length()) {
                char op = expression.charAt(pos);
                if (op == '^') {
                    pos++;
                    result = Math.pow(result, parsePower());
                } else {
                    break;
                }
            }
            return result;
        }

        private double parsePower() {
            if (pos >= expression.length()) return 0;

            char ch = expression.charAt(pos);

            // Unary operators
            if (ch == '-') {
                pos++;
                return -parsePower();
            }
            if (ch == '+') {
                pos++;
                return parsePower();
            }

            // Parentheses
            if (ch == '(') {
                pos++;
                double result = parseExpression();
                if (pos >= expression.length() || expression.charAt(pos) != ')') {
                    throw new RuntimeException("Missing closing parenthesis");
                }
                pos++;
                return result;
            }

            // Number
            if (Character.isDigit(ch) || ch == '.') {
                return parseNumber();
            }

            // Function or variable
            if (Character.isLetter(ch)) {
                return parseFunctionOrVariable();
            }

            throw new RuntimeException("Unexpected character: " + ch);
        }

        private double parseNumber() {
            int start = pos;
            while (pos < expression.length() &&
                   (Character.isDigit(expression.charAt(pos)) || expression.charAt(pos) == '.')) {
                pos++;
            }
            return Double.parseDouble(expression.substring(start, pos));
        }

        private double parseFunctionOrVariable() {
            int start = pos;
            while (pos < expression.length() && Character.isLetterOrDigit(expression.charAt(pos))) {
                pos++;
            }
            String name = expression.substring(start, pos);

            // Check if it's a function
            if (pos < expression.length() && expression.charAt(pos) == '(') {
                pos++;
                List<Double> args = new ArrayList<>();
                if (expression.charAt(pos) != ')') {
                    args.add(parseExpression());
                    while (pos < expression.length() && expression.charAt(pos) == ',') {
                        pos++;
                        args.add(parseExpression());
                    }
                }
                if (pos >= expression.length() || expression.charAt(pos) != ')') {
                    throw new RuntimeException("Missing closing parenthesis for function: " + name);
                }
                pos++;
                return evaluateFunction(name, args);
            }

            // It's a variable
            if (variables.containsKey(name)) {
                return variables.get(name);
            }

            // Built-in constants
            return switch (name.toLowerCase()) {
                case "pi" -> Math.PI;
                case "e" -> Math.E;
                case "phi", "golden" -> 1.618033988749895;
                case "tau" -> 2 * Math.PI;
                default -> throw new RuntimeException("Unknown variable: " + name);
            };
        }

        private double evaluateFunction(String name, List<Double> args) {
            return switch (name.toLowerCase()) {
                case "sin" -> Math.sin(args.get(0));
                case "cos" -> Math.cos(args.get(0));
                case "tan" -> Math.tan(args.get(0));
                case "asin" -> Math.asin(args.get(0));
                case "acos" -> Math.acos(args.get(0));
                case "atan" -> Math.atan(args.get(0));
                case "sinh" -> Math.sinh(args.get(0));
                case "cosh" -> Math.cosh(args.get(0));
                case "tanh" -> Math.tanh(args.get(0));
                case "sqrt" -> Math.sqrt(args.get(0));
                case "cbrt" -> Math.cbrt(args.get(0));
                case "abs" -> Math.abs(args.get(0));
                case "log", "ln" -> Math.log(args.get(0));
                case "log10" -> Math.log10(args.get(0));
                case "log2" -> Math.log(args.get(0)) / Math.log(2);
                case "exp" -> Math.exp(args.get(0));
                case "floor" -> Math.floor(args.get(0));
                case "ceil" -> Math.ceil(args.get(0));
                case "round" -> Math.round(args.get(0));
                case "sign" -> Math.signum(args.get(0));
                case "min" -> Math.min(args.get(0), args.get(1));
                case "max" -> Math.max(args.get(0), args.get(1));
                case "pow" -> Math.pow(args.get(0), args.get(1));
                case "random" -> Math.random();
                case "atan2" -> Math.atan2(args.get(0), args.get(1));
                case "hypot" -> Math.hypot(args.get(0), args.get(1));
                default -> throw new RuntimeException("Unknown function: " + name);
            };
        }
    }

    /**
     * Check if expression is valid.
     */
    public static boolean isValidExpression(String expression) {
        try {
            evaluate(expression);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Substitute variables in expression.
     */
    public static String substituteVariables(String expression, Map<String, String> variables) {
        String result = expression;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replaceAll("\\b" + Pattern.quote(entry.getKey()) + "\\b", entry.getValue());
        }
        return result;
    }

    /**
     * Extract variable names from expression.
     */
    public static Set<String> extractVariables(String expression) {
        Set<String> variables = new HashSet<>();
        Pattern pattern = Pattern.compile("\\b[a-zA-Z][a-zA-Z0-9_]*\\b");
        Matcher matcher = pattern.matcher(expression);

        Set<String> reservedWords = Set.of("sin", "cos", "tan", "asin", "acos", "atan",
            "sinh", "cosh", "tanh", "sqrt", "cbrt", "abs", "log", "ln", "log10", "log2",
            "exp", "floor", "ceil", "round", "sign", "min", "max", "pow", "random", "atan2",
            "hypot", "pi", "e", "phi", "golden", "tau");

        while (matcher.find()) {
            String name = matcher.group();
            if (!reservedWords.contains(name.toLowerCase())) {
                variables.add(name);
            }
        }
        return variables;
    }

    /**
     * Evaluate boolean expression.
     */
    public static boolean evaluateBoolean(String expression, Map<String, Object> variables) {
        return new BooleanEvaluator(expression, variables).evaluate();
    }

    /**
     * Boolean expression evaluator.
     */
    private static class BooleanEvaluator {
        private final String expression;
        private final Map<String, Object> variables;
        private int pos;

        BooleanEvaluator(String expression, Map<String, Object> variables) {
            this.expression = expression.replaceAll("\\s+", "");
            this.variables = variables;
            this.pos = 0;
        }

        boolean evaluate() {
            return parseOr();
        }

        private boolean parseOr() {
            boolean result = parseAnd();
            while (match("||")) {
                result = result || parseAnd();
            }
            return result;
        }

        private boolean parseAnd() {
            boolean result = parseNot();
            while (match("&&")) {
                result = result && parseNot();
            }
            return result;
        }

        private boolean parseNot() {
            if (match("!")) {
                return !parseNot();
            }
            return parseComparison();
        }

        private boolean parseComparison() {
            Object left = parseValue();

            if (match("==")) return Objects.equals(left, parseValue());
            if (match("!=")) return !Objects.equals(left, parseValue());
            if (match(">=")) return compareNumbers(left, parseValue()) >= 0;
            if (match("<=")) return compareNumbers(left, parseValue()) <= 0;
            if (match(">")) return compareNumbers(left, parseValue()) > 0;
            if (match("<")) return compareNumbers(left, parseValue()) < 0;

            if (left instanceof Boolean) return (Boolean) left;
            return left != null;
        }

        private Object parseValue() {
            if (match("(")) {
                Object result = parseOr();
                match(")");
                return result;
            }

            if (match("true")) return true;
            if (match("false")) return false;
            if (match("null")) return null;

            // Number
            int start = pos;
            while (pos < expression.length() &&
                   (Character.isDigit(expression.charAt(pos)) || expression.charAt(pos) == '.')) {
                pos++;
            }
            if (pos > start) {
                String numStr = expression.substring(start, pos);
                return numStr.contains(".") ? Double.parseDouble(numStr) : Long.parseLong(numStr);
            }

            // Variable
            start = pos;
            while (pos < expression.length() && Character.isLetterOrDigit(expression.charAt(pos))) {
                pos++;
            }
            if (pos > start) {
                String name = expression.substring(start, pos);
                return variables.getOrDefault(name, name);
            }

            return null;
        }

        private boolean match(String s) {
            if (expression.startsWith(s, pos)) {
                pos += s.length();
                return true;
            }
            return false;
        }

        @SuppressWarnings("unchecked")
        private int compareNumbers(Object a, Object b) {
            if (a instanceof Number && b instanceof Number) {
                return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
            }
            if (a instanceof Comparable && b instanceof Comparable) {
                return ((Comparable<Object>) a).compareTo(b);
            }
            return 0;
        }
    }
}