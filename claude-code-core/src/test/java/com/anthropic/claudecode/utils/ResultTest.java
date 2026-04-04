/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;
import java.util.function.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Result.
 */
class ResultTest {

    @Test
    @DisplayName("Result Ok isOk true")
    void okIsOk() {
        Result<Integer> result = Result.ok(42);
        assertTrue(result.isOk());
    }

    @Test
    @DisplayName("Result Ok isErr false")
    void okIsErr() {
        Result<Integer> result = Result.ok(42);
        assertFalse(result.isErr());
    }

    @Test
    @DisplayName("Result Ok getValue returns value")
    void okGetValue() {
        Result<Integer> result = Result.ok(42);
        Optional<Integer> value = result.getValue();
        assertTrue(value.isPresent());
        assertEquals(42, value.get());
    }

    @Test
    @DisplayName("Result Ok getValue with null")
    void okGetValueNull() {
        Result<String> result = Result.ok(null);
        Optional<String> value = result.getValue();
        assertFalse(value.isPresent());
    }

    @Test
    @DisplayName("Result Ok getError empty")
    void okGetError() {
        Result<Integer> result = Result.ok(42);
        assertFalse(result.getError().isPresent());
    }

    @Test
    @DisplayName("Result Ok getOrElse returns value")
    void okGetOrElse() {
        Result<Integer> result = Result.ok(42);
        assertEquals(42, result.getOrElse(0));
    }

    @Test
    @DisplayName("Result Ok getOrThrow returns value")
    void okGetOrThrow() {
        Result<Integer> result = Result.ok(42);
        assertEquals(42, result.getOrThrow());
    }

    @Test
    @DisplayName("Result Ok map transforms value")
    void okMap() {
        Result<Integer> result = Result.ok(5);
        Result<Integer> mapped = result.map(x -> x * 2);
        assertTrue(mapped.isOk());
        assertEquals(10, mapped.getValue().orElse(null));
    }

    @Test
    @DisplayName("Result Ok flatMap transforms value")
    void okFlatMap() {
        Result<Integer> result = Result.ok(5);
        Result<Integer> mapped = result.flatMap(x -> Result.ok(x * 2));
        assertTrue(mapped.isOk());
        assertEquals(10, mapped.getValue().orElse(null));
    }

    @Test
    @DisplayName("Result Ok mapError unchanged")
    void okMapError() {
        Result<Integer> result = Result.ok(42);
        Result<Integer> mapped = result.mapError(e -> "new error");
        assertTrue(mapped.isOk());
        assertEquals(42, mapped.getValue().orElse(null));
    }

    @Test
    @DisplayName("Result Ok onSuccess executes action")
    void okOnSuccess() {
        StringBuilder sb = new StringBuilder();
        Result<Integer> result = Result.ok(42);
        result.onSuccess(sb::append);
        assertEquals("42", sb.toString());
    }

    @Test
    @DisplayName("Result Ok onFailure does not execute")
    void okOnFailure() {
        StringBuilder sb = new StringBuilder();
        Result<Integer> result = Result.ok(42);
        result.onFailure(sb::append);
        assertEquals("", sb.toString());
    }

    @Test
    @DisplayName("Result Ok recover unchanged")
    void okRecover() {
        Result<Integer> result = Result.ok(42);
        Result<Integer> recovered = result.recover(e -> 0);
        assertTrue(recovered.isOk());
        assertEquals(42, recovered.getValue().orElse(null));
    }

    @Test
    @DisplayName("Result Err isOk false")
    void errIsOk() {
        Result<Integer> result = Result.err("error");
        assertFalse(result.isOk());
    }

    @Test
    @DisplayName("Result Err isErr true")
    void errIsErr() {
        Result<Integer> result = Result.err("error");
        assertTrue(result.isErr());
    }

    @Test
    @DisplayName("Result Err getValue empty")
    void errGetValue() {
        Result<Integer> result = Result.err("error");
        assertFalse(result.getValue().isPresent());
    }

    @Test
    @DisplayName("Result Err getError returns error")
    void errGetError() {
        Result<Integer> result = Result.err("error message");
        Optional<String> error = result.getError();
        assertTrue(error.isPresent());
        assertEquals("error message", error.get());
    }

    @Test
    @DisplayName("Result Err getOrElse returns default")
    void errGetOrElse() {
        Result<Integer> result = Result.err("error");
        assertEquals(0, result.getOrElse(0));
    }

    @Test
    @DisplayName("Result Err getOrThrow throws")
    void errGetOrThrow() {
        Result<Integer> result = Result.err("error message");
        RuntimeException ex = assertThrows(RuntimeException.class, result::getOrThrow);
        assertEquals("error message", ex.getMessage());
    }

    @Test
    @DisplayName("Result Err map propagates error")
    void errMap() {
        Result<Integer> result = Result.err("error");
        Result<Integer> mapped = result.map(x -> x * 2);
        assertTrue(mapped.isErr());
        assertEquals("error", mapped.getError().orElse(null));
    }

    @Test
    @DisplayName("Result Err flatMap propagates error")
    void errFlatMap() {
        Result<Integer> result = Result.err("error");
        Result<Integer> mapped = result.flatMap(x -> Result.ok(x * 2));
        assertTrue(mapped.isErr());
    }

    @Test
    @DisplayName("Result Err mapError transforms error")
    void errMapError() {
        Result<Integer> result = Result.err("error");
        Result<Integer> mapped = result.mapError(e -> "new " + e);
        assertTrue(mapped.isErr());
        assertEquals("new error", mapped.getError().orElse(null));
    }

    @Test
    @DisplayName("Result Err onSuccess does not execute")
    void errOnSuccess() {
        StringBuilder sb = new StringBuilder();
        Result<Integer> result = Result.err("error");
        result.onSuccess(sb::append);
        assertEquals("", sb.toString());
    }

    @Test
    @DisplayName("Result Err onFailure executes action")
    void errOnFailure() {
        StringBuilder sb = new StringBuilder();
        Result<Integer> result = Result.err("error");
        result.onFailure(sb::append);
        assertEquals("error", sb.toString());
    }

    @Test
    @DisplayName("Result Err recover returns new value")
    void errRecover() {
        Result<Integer> result = Result.err("error");
        Result<Integer> recovered = result.recover(e -> 0);
        assertTrue(recovered.isOk());
        assertEquals(0, recovered.getValue().orElse(null));
    }

    @Test
    @DisplayName("Result ok without value")
    void okWithoutValue() {
        Result<Void> result = Result.ok();
        assertTrue(result.isOk());
        assertFalse(result.getValue().isPresent());
    }

    @Test
    @DisplayName("Result err with format")
    void errWithFormat() {
        Result<Integer> result = Result.err("Error: %s", "test");
        assertTrue(result.isErr());
        assertEquals("Error: test", result.getError().orElse(null));
    }

    @Test
    @DisplayName("Result err with list of errors")
    void errWithList() {
        Result<Integer> result = Result.err(List.of("error1", "error2"));
        assertTrue(result.isErr());
        assertEquals("error1; error2", result.getError().orElse(null));
    }

    @Test
    @DisplayName("Result from supplier success")
    void fromSuccess() {
        Result<Integer> result = Result.from(() -> 42);
        assertTrue(result.isOk());
        assertEquals(42, result.getValue().orElse(null));
    }

    @Test
    @DisplayName("Result from supplier failure")
    void fromFailure() {
        Result<Integer> result = Result.from(() -> {
            throw new RuntimeException("fail");
        });
        assertTrue(result.isErr());
        assertEquals("fail", result.getError().orElse(null));
    }

    @Test
    @DisplayName("Result fromCallable success")
    void fromCallableSuccess() {
        Result<Integer> result = Result.fromCallable(() -> 42);
        assertTrue(result.isOk());
        assertEquals(42, result.getValue().orElse(null));
    }

    @Test
    @DisplayName("Result fromCallable failure")
    void fromCallableFailure() {
        Result<Integer> result = Result.fromCallable(() -> {
            throw new Exception("fail");
        });
        assertTrue(result.isErr());
        assertEquals("fail", result.getError().orElse(null));
    }

    @Test
    @DisplayName("Result all with all ok")
    void allAllOk() {
        Result<List<Integer>> result = Result.all(
            Result.ok(1),
            Result.ok(2),
            Result.ok(3)
        );
        assertTrue(result.isOk());
        assertEquals(List.of(1, 2, 3), result.getValue().orElse(null));
    }

    @Test
    @DisplayName("Result all with one err")
    void allOneErr() {
        Result<List<Integer>> result = Result.all(
            Result.ok(1),
            Result.err("error"),
            Result.ok(3)
        );
        assertTrue(result.isErr());
        assertEquals("error", result.getError().orElse(null));
    }

    @Test
    @DisplayName("Result all with list")
    void allList() {
        List<Result<Integer>> results = List.of(Result.ok(1), Result.ok(2));
        Result<List<Integer>> result = Result.all(results);
        assertTrue(result.isOk());
        assertEquals(2, result.getValue().orElse(null).size());
    }

    @Test
    @DisplayName("Result any with one ok")
    void anyOneOk() {
        Result<Integer> result = Result.any(
            Result.err("error1"),
            Result.ok(42),
            Result.err("error2")
        );
        assertTrue(result.isOk());
        assertEquals(42, result.getValue().orElse(null));
    }

    @Test
    @DisplayName("Result any with all err")
    void anyAllErr() {
        Result<Integer> result = Result.any(
            Result.err("error1"),
            Result.err("error2")
        );
        assertTrue(result.isErr());
        assertTrue(result.getError().orElse(null).contains("error1"));
        assertTrue(result.getError().orElse(null).contains("error2"));
    }

    @Test
    @DisplayName("Result any with list")
    void anyList() {
        List<Result<Integer>> results = List.of(Result.err("error"), Result.ok(42));
        Result<Integer> result = Result.any(results);
        assertTrue(result.isOk());
    }

    @Test
    @DisplayName("Result partition separates successes and failures")
    void partition() {
        List<Result<Integer>> results = List.of(
            Result.ok(1),
            Result.err("error1"),
            Result.ok(2),
            Result.err("error2")
        );

        Result.Partitioned<Integer> partitioned = Result.partition(results);
        assertEquals(2, partitioned.successes().size());
        assertEquals(2, partitioned.failures().size());
        assertTrue(partitioned.successes().contains(1));
        assertTrue(partitioned.successes().contains(2));
        assertTrue(partitioned.failures().contains("error1"));
        assertTrue(partitioned.failures().contains("error2"));
    }

    @Test
    @DisplayName("Result Ok record fields")
    void okRecordFields() {
        Result.Ok<String> ok = new Result.Ok<>("value");
        assertEquals("value", ok.value());
    }

    @Test
    @DisplayName("Result Err record fields")
    void errRecordFields() {
        Result.Err<String> err = new Result.Err<>("error message");
        assertEquals("error message", err.error());
    }
}
