/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ExpressionUtils.
 */
class ExpressionUtilsTest {

    @Test
    @DisplayName("ExpressionUtils evaluate simple addition")
    void evaluateAddition() {
        assertEquals(7.0, ExpressionUtils.evaluate("3 + 4"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate simple subtraction")
    void evaluateSubtraction() {
        assertEquals(1.0, ExpressionUtils.evaluate("5 - 4"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate simple multiplication")
    void evaluateMultiplication() {
        assertEquals(12.0, ExpressionUtils.evaluate("3 * 4"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate simple division")
    void evaluateDivision() {
        assertEquals(2.5, ExpressionUtils.evaluate("10 / 4"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate modulo")
    void evaluateModulo() {
        assertEquals(2.0, ExpressionUtils.evaluate("10 % 4"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate power")
    void evaluatePower() {
        assertEquals(8.0, ExpressionUtils.evaluate("2 ^ 3"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate complex expression")
    void evaluateComplex() {
        assertEquals(14.0, ExpressionUtils.evaluate("2 + 3 * 4"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate parentheses")
    void evaluateParentheses() {
        assertEquals(20.0, ExpressionUtils.evaluate("(2 + 3) * 4"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate nested parentheses")
    void evaluateNestedParentheses() {
        assertEquals(21.0, ExpressionUtils.evaluate("((2 + 3) * 4) + 1"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate unary minus")
    void evaluateUnaryMinus() {
        assertEquals(-5.0, ExpressionUtils.evaluate("-5"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate unary plus")
    void evaluateUnaryPlus() {
        assertEquals(5.0, ExpressionUtils.evaluate("+5"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate with spaces")
    void evaluateWithSpaces() {
        assertEquals(7.0, ExpressionUtils.evaluate(" 3  +  4 "), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate decimal numbers")
    void evaluateDecimals() {
        assertEquals(7.5, ExpressionUtils.evaluate("3.5 + 4.0"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate constant pi")
    void evaluatePi() {
        assertEquals(Math.PI, ExpressionUtils.evaluate("pi"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate constant e")
    void evaluateE() {
        assertEquals(Math.E, ExpressionUtils.evaluate("e"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate constant phi")
    void evaluatePhi() {
        assertEquals(1.618033988749895, ExpressionUtils.evaluate("phi"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate constant golden")
    void evaluateGolden() {
        assertEquals(1.618033988749895, ExpressionUtils.evaluate("golden"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate constant tau")
    void evaluateTau() {
        assertEquals(2 * Math.PI, ExpressionUtils.evaluate("tau"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate sin function")
    void evaluateSin() {
        assertEquals(0.0, ExpressionUtils.evaluate("sin(0)"), 0.001);
        assertEquals(1.0, ExpressionUtils.evaluate("sin(pi/2)"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate cos function")
    void evaluateCos() {
        assertEquals(1.0, ExpressionUtils.evaluate("cos(0)"), 0.001);
        assertEquals(0.0, ExpressionUtils.evaluate("cos(pi/2)"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate tan function")
    void evaluateTan() {
        assertEquals(0.0, ExpressionUtils.evaluate("tan(0)"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate sqrt function")
    void evaluateSqrt() {
        assertEquals(4.0, ExpressionUtils.evaluate("sqrt(16)"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate cbrt function")
    void evaluateCbrt() {
        assertEquals(3.0, ExpressionUtils.evaluate("cbrt(27)"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate abs function")
    void evaluateAbs() {
        assertEquals(5.0, ExpressionUtils.evaluate("abs(-5)"), 0.001);
        assertEquals(5.0, ExpressionUtils.evaluate("abs(5)"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate log function")
    void evaluateLog() {
        assertEquals(0.0, ExpressionUtils.evaluate("log(1)"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate ln function")
    void evaluateLn() {
        assertEquals(0.0, ExpressionUtils.evaluate("ln(1)"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate log10 function")
    void evaluateLog10() {
        assertEquals(1.0, ExpressionUtils.evaluate("log10(10)"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate log2 function")
    void evaluateLog2() {
        assertEquals(3.0, ExpressionUtils.evaluate("log2(8)"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate exp function")
    void evaluateExp() {
        assertEquals(1.0, ExpressionUtils.evaluate("exp(0)"), 0.001);
        assertEquals(Math.E, ExpressionUtils.evaluate("exp(1)"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate floor function")
    void evaluateFloor() {
        assertEquals(3.0, ExpressionUtils.evaluate("floor(3.7)"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate ceil function")
    void evaluateCeil() {
        assertEquals(4.0, ExpressionUtils.evaluate("ceil(3.2)"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate round function")
    void evaluateRound() {
        assertEquals(4.0, ExpressionUtils.evaluate("round(3.5)"), 0.001);
        assertEquals(3.0, ExpressionUtils.evaluate("round(3.4)"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate sign function")
    void evaluateSign() {
        assertEquals(-1.0, ExpressionUtils.evaluate("sign(-5)"), 0.001);
        assertEquals(1.0, ExpressionUtils.evaluate("sign(5)"), 0.001);
        assertEquals(0.0, ExpressionUtils.evaluate("sign(0)"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate min function")
    void evaluateMin() {
        assertEquals(3.0, ExpressionUtils.evaluate("min(3, 5)"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate max function")
    void evaluateMax() {
        assertEquals(5.0, ExpressionUtils.evaluate("max(3, 5)"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate pow function")
    void evaluatePow() {
        assertEquals(8.0, ExpressionUtils.evaluate("pow(2, 3)"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate atan2 function")
    void evaluateAtan2() {
        assertEquals(Math.atan2(1, 1), ExpressionUtils.evaluate("atan2(1, 1)"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate hypot function")
    void evaluateHypot() {
        assertEquals(5.0, ExpressionUtils.evaluate("hypot(3, 4)"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate sinh function")
    void evaluateSinh() {
        assertEquals(0.0, ExpressionUtils.evaluate("sinh(0)"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate cosh function")
    void evaluateCosh() {
        assertEquals(1.0, ExpressionUtils.evaluate("cosh(0)"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate tanh function")
    void evaluateTanh() {
        assertEquals(0.0, ExpressionUtils.evaluate("tanh(0)"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate asin function")
    void evaluateAsin() {
        assertEquals(0.0, ExpressionUtils.evaluate("asin(0)"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate acos function")
    void evaluateAcos() {
        assertEquals(Math.PI / 2, ExpressionUtils.evaluate("acos(0)"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate atan function")
    void evaluateAtan() {
        assertEquals(0.0, ExpressionUtils.evaluate("atan(0)"), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate random function")
    void evaluateRandom() {
        double result = ExpressionUtils.evaluate("random()");
        assertTrue(result >= 0.0 && result < 1.0);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate with variables")
    void evaluateWithVariables() {
        Map<String, Double> vars = Map.of("x", 5.0, "y", 3.0);
        assertEquals(8.0, ExpressionUtils.evaluate("x + y", vars), 0.001);
        assertEquals(15.0, ExpressionUtils.evaluate("x * y", vars), 0.001);
    }

    @Test
    @DisplayName("ExpressionUtils evaluate throws on unknown variable")
    void evaluateUnknownVariable() {
        assertThrows(RuntimeException.class, () ->
            ExpressionUtils.evaluate("unknownVar")
        );
    }

    @Test
    @DisplayName("ExpressionUtils evaluate throws on invalid expression")
    void evaluateInvalid() {
        // Expression with invalid character
        assertThrows(RuntimeException.class, () ->
            ExpressionUtils.evaluate("3 @ 4")
        );
    }

    @Test
    @DisplayName("ExpressionUtils evaluate throws on missing closing parenthesis")
    void evaluateMissingClosingParen() {
        assertThrows(RuntimeException.class, () ->
            ExpressionUtils.evaluate("(3 + 4")
        );
    }

    @Test
    @DisplayName("ExpressionUtils evaluate throws on unknown function")
    void evaluateUnknownFunction() {
        assertThrows(RuntimeException.class, () ->
            ExpressionUtils.evaluate("unknown(5)")
        );
    }

    @Test
    @DisplayName("ExpressionUtils isValidExpression true for valid")
    void isValidExpressionTrue() {
        assertTrue(ExpressionUtils.isValidExpression("3 + 4"));
        assertTrue(ExpressionUtils.isValidExpression("sin(pi)"));
        assertTrue(ExpressionUtils.isValidExpression("(2 + 3) * 4"));
    }

    @Test
    @DisplayName("ExpressionUtils isValidExpression false for invalid")
    void isValidExpressionFalse() {
        // Invalid character
        assertFalse(ExpressionUtils.isValidExpression("3 @ 4"));
        // Missing closing parenthesis
        assertFalse(ExpressionUtils.isValidExpression("(3 + 4"));
        // Unknown function throws when evaluated
        assertFalse(ExpressionUtils.isValidExpression("unknownFunc()"));
    }

    @Test
    @DisplayName("ExpressionUtils substituteVariables")
    void substituteVariables() {
        Map<String, String> vars = Map.of("x", "5", "y", "3");
        String result = ExpressionUtils.substituteVariables("x + y", vars);
        assertEquals("5 + 3", result);
    }

    @Test
    @DisplayName("ExpressionUtils substituteVariables preserves non-matches")
    void substituteVariablesPreservesNonMatches() {
        Map<String, String> vars = Map.of("x", "5");
        String result = ExpressionUtils.substituteVariables("x + z", vars);
        assertEquals("5 + z", result);
    }

    @Test
    @DisplayName("ExpressionUtils extractVariables")
    void extractVariables() {
        Set<String> vars = ExpressionUtils.extractVariables("x + y * z");
        assertTrue(vars.contains("x"));
        assertTrue(vars.contains("y"));
        assertTrue(vars.contains("z"));
    }

    @Test
    @DisplayName("ExpressionUtils extractVariables excludes functions")
    void extractVariablesExcludesFunctions() {
        Set<String> vars = ExpressionUtils.extractVariables("sin(x) + cos(y)");
        assertTrue(vars.contains("x"));
        assertTrue(vars.contains("y"));
        assertFalse(vars.contains("sin"));
        assertFalse(vars.contains("cos"));
    }

    @Test
    @DisplayName("ExpressionUtils extractVariables excludes constants")
    void extractVariablesExcludesConstants() {
        Set<String> vars = ExpressionUtils.extractVariables("x + pi + e");
        assertTrue(vars.contains("x"));
        assertFalse(vars.contains("pi"));
        assertFalse(vars.contains("e"));
    }

    @Test
    @DisplayName("ExpressionUtils evaluateBoolean true literal")
    void evaluateBooleanTrue() {
        assertTrue(ExpressionUtils.evaluateBoolean("true", Map.of()));
    }

    @Test
    @DisplayName("ExpressionUtils evaluateBoolean false literal")
    void evaluateBooleanFalse() {
        assertFalse(ExpressionUtils.evaluateBoolean("false", Map.of()));
    }

    @Test
    @DisplayName("ExpressionUtils evaluateBoolean or operator")
    void evaluateBooleanOr() {
        assertTrue(ExpressionUtils.evaluateBoolean("true || false", Map.of()));
        assertTrue(ExpressionUtils.evaluateBoolean("false || true", Map.of()));
        assertFalse(ExpressionUtils.evaluateBoolean("false || false", Map.of()));
    }

    @Test
    @DisplayName("ExpressionUtils evaluateBoolean and operator")
    void evaluateBooleanAnd() {
        assertTrue(ExpressionUtils.evaluateBoolean("true && true", Map.of()));
        assertFalse(ExpressionUtils.evaluateBoolean("true && false", Map.of()));
        assertFalse(ExpressionUtils.evaluateBoolean("false && true", Map.of()));
    }

    @Test
    @DisplayName("ExpressionUtils evaluateBoolean not operator")
    void evaluateBooleanNot() {
        assertFalse(ExpressionUtils.evaluateBoolean("!true", Map.of()));
        assertTrue(ExpressionUtils.evaluateBoolean("!false", Map.of()));
    }

    @Test
    @DisplayName("ExpressionUtils evaluateBoolean equality")
    void evaluateBooleanEquality() {
        assertTrue(ExpressionUtils.evaluateBoolean("5 == 5", Map.of()));
        assertFalse(ExpressionUtils.evaluateBoolean("5 == 3", Map.of()));
    }

    @Test
    @DisplayName("ExpressionUtils evaluateBoolean inequality")
    void evaluateBooleanInequality() {
        assertFalse(ExpressionUtils.evaluateBoolean("5 != 5", Map.of()));
        assertTrue(ExpressionUtils.evaluateBoolean("5 != 3", Map.of()));
    }

    @Test
    @DisplayName("ExpressionUtils evaluateBoolean greater than")
    void evaluateBooleanGreaterThan() {
        assertTrue(ExpressionUtils.evaluateBoolean("5 > 3", Map.of()));
        assertFalse(ExpressionUtils.evaluateBoolean("3 > 5", Map.of()));
    }

    @Test
    @DisplayName("ExpressionUtils evaluateBoolean less than")
    void evaluateBooleanLessThan() {
        assertTrue(ExpressionUtils.evaluateBoolean("3 < 5", Map.of()));
        assertFalse(ExpressionUtils.evaluateBoolean("5 < 3", Map.of()));
    }

    @Test
    @DisplayName("ExpressionUtils evaluateBoolean greater or equal")
    void evaluateBooleanGreaterOrEqual() {
        assertTrue(ExpressionUtils.evaluateBoolean("5 >= 5", Map.of()));
        assertTrue(ExpressionUtils.evaluateBoolean("5 >= 3", Map.of()));
        assertFalse(ExpressionUtils.evaluateBoolean("3 >= 5", Map.of()));
    }

    @Test
    @DisplayName("ExpressionUtils evaluateBoolean less or equal")
    void evaluateBooleanLessOrEqual() {
        assertTrue(ExpressionUtils.evaluateBoolean("3 <= 3", Map.of()));
        assertTrue(ExpressionUtils.evaluateBoolean("3 <= 5", Map.of()));
        assertFalse(ExpressionUtils.evaluateBoolean("5 <= 3", Map.of()));
    }

    @Test
    @DisplayName("ExpressionUtils evaluateBoolean with parentheses")
    void evaluateBooleanParentheses() {
        assertTrue(ExpressionUtils.evaluateBoolean("(true || false) && true", Map.of()));
        assertFalse(ExpressionUtils.evaluateBoolean("(false || false) && true", Map.of()));
    }

    @Test
    @DisplayName("ExpressionUtils evaluateBoolean with variables")
    void evaluateBooleanWithVariables() {
        Map<String, Object> vars = Map.of("x", true, "y", false);
        assertTrue(ExpressionUtils.evaluateBoolean("x || y", vars));
        assertFalse(ExpressionUtils.evaluateBoolean("x && y", vars));
    }

    @Test
    @DisplayName("ExpressionUtils evaluateBoolean null comparison")
    void evaluateBooleanNull() {
        assertTrue(ExpressionUtils.evaluateBoolean("null == null", Map.of()));
        assertFalse(ExpressionUtils.evaluateBoolean("null != null", Map.of()));
    }

    @Test
    @DisplayName("ExpressionUtils evaluateBoolean complex expression")
    void evaluateBooleanComplex() {
        Map<String, Object> vars = Map.of("a", 10, "b", 5);
        assertTrue(ExpressionUtils.evaluateBoolean("a > b && a < 20", vars));
        assertFalse(ExpressionUtils.evaluateBoolean("a < b || a > 100", vars));
    }

    @Test
    @DisplayName("ExpressionUtils evaluateBoolean decimal comparison")
    void evaluateBooleanDecimal() {
        assertTrue(ExpressionUtils.evaluateBoolean("3.5 > 2.5", Map.of()));
        assertFalse(ExpressionUtils.evaluateBoolean("3.5 < 2.5", Map.of()));
    }
}