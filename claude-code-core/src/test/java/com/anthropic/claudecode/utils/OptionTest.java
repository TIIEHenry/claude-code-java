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
 * Tests for Option.
 */
class OptionTest {

    @Test
    @DisplayName("Option Some isPresent true")
    void someIsPresent() {
        Option<Integer> option = Option.some(42);
        assertTrue(option.isPresent());
    }

    @Test
    @DisplayName("Option Some isEmpty false")
    void someIsEmpty() {
        Option<Integer> option = Option.some(42);
        assertFalse(option.isEmpty());
    }

    @Test
    @DisplayName("Option Some get returns value")
    void someGet() {
        Option<Integer> option = Option.some(42);
        assertEquals(42, option.get());
    }

    @Test
    @DisplayName("Option Some getOrElse returns value")
    void someGetOrElse() {
        Option<Integer> option = Option.some(42);
        assertEquals(42, option.getOrElse(0));
    }

    @Test
    @DisplayName("Option Some getOrElseGet returns value")
    void someGetOrElseGet() {
        Option<Integer> option = Option.some(42);
        assertEquals(42, option.getOrElseGet(() -> 0));
    }

    @Test
    @DisplayName("Option Some getOrThrow returns value")
    void someGetOrThrow() {
        Option<Integer> option = Option.some(42);
        assertEquals(42, option.getOrThrow(() -> new RuntimeException("error")));
    }

    @Test
    @DisplayName("Option Some toOptional")
    void someToOptional() {
        Option<Integer> option = Option.some(42);
        Optional<Integer> opt = option.toOptional();
        assertTrue(opt.isPresent());
        assertEquals(42, opt.get());
    }

    @Test
    @DisplayName("Option Some map transforms value")
    void someMap() {
        Option<Integer> option = Option.some(5);
        Option<Integer> mapped = option.map(x -> x * 2);
        assertTrue(mapped.isPresent());
        assertEquals(10, mapped.get());
    }

    @Test
    @DisplayName("Option Some flatMap transforms value")
    void someFlatMap() {
        Option<Integer> option = Option.some(5);
        Option<Integer> mapped = option.flatMap(x -> Option.some(x * 2));
        assertTrue(mapped.isPresent());
        assertEquals(10, mapped.get());
    }

    @Test
    @DisplayName("Option Some filter keeps matching")
    void someFilterKeeps() {
        Option<Integer> option = Option.some(5);
        Option<Integer> filtered = option.filter(x -> x > 3);
        assertTrue(filtered.isPresent());
        assertEquals(5, filtered.get());
    }

    @Test
    @DisplayName("Option Some filter removes non-matching")
    void someFilterRemoves() {
        Option<Integer> option = Option.some(5);
        Option<Integer> filtered = option.filter(x -> x > 10);
        assertTrue(filtered.isEmpty());
    }

    @Test
    @DisplayName("Option Some ifPresent executes action")
    void someIfPresent() {
        StringBuilder sb = new StringBuilder();
        Option<Integer> option = Option.some(42);
        option.ifPresent(sb::append);
        assertEquals("42", sb.toString());
    }

    @Test
    @DisplayName("Option Some ifEmpty does not execute")
    void someIfEmpty() {
        StringBuilder sb = new StringBuilder();
        Option<Integer> option = Option.some(42);
        option.ifEmpty(() -> sb.append("empty"));
        assertEquals("", sb.toString());
    }

    @Test
    @DisplayName("Option Some either executes ifPresent")
    void someEither() {
        StringBuilder sb = new StringBuilder();
        Option<Integer> option = Option.some(42);
        option.either(sb::append, () -> sb.append("empty"));
        assertEquals("42", sb.toString());
    }

    @Test
    @DisplayName("Option Some stream has one element")
    void someStream() {
        Option<Integer> option = Option.some(42);
        List<Integer> list = option.stream().toList();
        assertEquals(1, list.size());
        assertEquals(42, list.get(0));
    }

    @Test
    @DisplayName("Option Some iterator")
    void someIterator() {
        Option<Integer> option = Option.some(42);
        Iterator<Integer> it = option.iterator();
        assertTrue(it.hasNext());
        assertEquals(42, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    @DisplayName("Option None isPresent false")
    void noneIsPresent() {
        Option<Integer> option = Option.none();
        assertFalse(option.isPresent());
    }

    @Test
    @DisplayName("Option None isEmpty true")
    void noneIsEmpty() {
        Option<Integer> option = Option.none();
        assertTrue(option.isEmpty());
    }

    @Test
    @DisplayName("Option None get throws NoSuchElementException")
    void noneGet() {
        Option<Integer> option = Option.none();
        assertThrows(NoSuchElementException.class, option::get);
    }

    @Test
    @DisplayName("Option None getOrElse returns default")
    void noneGetOrElse() {
        Option<Integer> option = Option.none();
        assertEquals(0, option.getOrElse(0));
    }

    @Test
    @DisplayName("Option None getOrElseGet uses supplier")
    void noneGetOrElseGet() {
        Option<Integer> option = Option.none();
        assertEquals(42, option.getOrElseGet(() -> 42));
    }

    @Test
    @DisplayName("Option None getOrThrow throws supplied exception")
    void noneGetOrThrow() {
        Option<Integer> option = Option.none();
        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> option.getOrThrow(() -> new RuntimeException("custom error")));
        assertEquals("custom error", ex.getMessage());
    }

    @Test
    @DisplayName("Option None toOptional empty")
    void noneToOptional() {
        Option<Integer> option = Option.none();
        assertFalse(option.toOptional().isPresent());
    }

    @Test
    @DisplayName("Option None map stays empty")
    void noneMap() {
        Option<Integer> option = Option.none();
        Option<Integer> mapped = option.map(x -> x * 2);
        assertTrue(mapped.isEmpty());
    }

    @Test
    @DisplayName("Option None flatMap stays empty")
    void noneFlatMap() {
        Option<Integer> option = Option.none();
        Option<Integer> mapped = option.flatMap(x -> Option.some(x * 2));
        assertTrue(mapped.isEmpty());
    }

    @Test
    @DisplayName("Option None filter stays empty")
    void noneFilter() {
        Option<Integer> option = Option.none();
        Option<Integer> filtered = option.filter(x -> true);
        assertTrue(filtered.isEmpty());
    }

    @Test
    @DisplayName("Option None ifPresent does not execute")
    void noneIfPresent() {
        StringBuilder sb = new StringBuilder();
        Option<Integer> option = Option.none();
        option.ifPresent(sb::append);
        assertEquals("", sb.toString());
    }

    @Test
    @DisplayName("Option None ifEmpty executes action")
    void noneIfEmpty() {
        StringBuilder sb = new StringBuilder();
        Option<Integer> option = Option.none();
        option.ifEmpty(() -> sb.append("empty"));
        assertEquals("empty", sb.toString());
    }

    @Test
    @DisplayName("Option None either executes ifEmpty")
    void noneEither() {
        StringBuilder sb = new StringBuilder();
        Option<Integer> option = Option.none();
        option.either(sb::append, () -> sb.append("empty"));
        assertEquals("empty", sb.toString());
    }

    @Test
    @DisplayName("Option None stream empty")
    void noneStream() {
        Option<Integer> option = Option.none();
        assertEquals(0, option.stream().count());
    }

    @Test
    @DisplayName("Option None iterator empty")
    void noneIterator() {
        Option<Integer> option = Option.none();
        assertFalse(option.iterator().hasNext());
    }

    @Test
    @DisplayName("Option None equals")
    void noneEquals() {
        Option<Integer> none1 = Option.none();
        Option<Integer> none2 = Option.none();
        Option<String> none3 = Option.none();

        assertEquals(none1, none2);
        assertEquals(none1, none3);
    }

    @Test
    @DisplayName("Option None hashCode")
    void noneHashCode() {
        Option<Integer> none1 = Option.none();
        Option<Integer> none2 = Option.none();
        assertEquals(none1.hashCode(), none2.hashCode());
    }

    @Test
    @DisplayName("Option None toString")
    void noneToString() {
        Option<Integer> option = Option.none();
        assertEquals("None", option.toString());
    }

    @Test
    @DisplayName("Option of with non-null")
    void ofNonNull() {
        Option<Integer> option = Option.of(42);
        assertTrue(option.isPresent());
        assertEquals(42, option.get());
    }

    @Test
    @DisplayName("Option of with null")
    void ofNull() {
        Option<Integer> option = Option.of(null);
        assertTrue(option.isEmpty());
    }

    @Test
    @DisplayName("Option fromOptional with present")
    void fromOptionalPresent() {
        Optional<Integer> opt = Optional.of(42);
        Option<Integer> option = Option.fromOptional(opt);
        assertTrue(option.isPresent());
        assertEquals(42, option.get());
    }

    @Test
    @DisplayName("Option fromOptional with empty")
    void fromOptionalEmpty() {
        Optional<Integer> opt = Optional.empty();
        Option<Integer> option = Option.fromOptional(opt);
        assertTrue(option.isEmpty());
    }

    @Test
    @DisplayName("Option combine both present")
    void combineBothPresent() {
        Option<Integer> opt1 = Option.some(5);
        Option<Integer> opt2 = Option.some(3);
        Option<Integer> combined = Option.combine(opt1, opt2, (a, b) -> a + b);
        assertTrue(combined.isPresent());
        assertEquals(8, combined.get());
    }

    @Test
    @DisplayName("Option combine one empty")
    void combineOneEmpty() {
        Option<Integer> opt1 = Option.some(5);
        Option<Integer> opt2 = Option.none();
        Option<Integer> combined = Option.combine(opt1, opt2, (a, b) -> a + b);
        assertTrue(combined.isEmpty());
    }

    @Test
    @DisplayName("Option combine both empty")
    void combineBothEmpty() {
        Option<Integer> opt1 = Option.none();
        Option<Integer> opt2 = Option.none();
        Option<Integer> combined = Option.combine(opt1, opt2, (a, b) -> a + b);
        assertTrue(combined.isEmpty());
    }

    @Test
    @DisplayName("Option firstOf finds first present")
    void firstOfFirstPresent() {
        Option<Integer> first = Option.firstOf(
            Option.none(),
            Option.some(42),
            Option.some(10)
        );
        assertTrue(first.isPresent());
        assertEquals(42, first.get());
    }

    @Test
    @DisplayName("Option firstOf all empty")
    void firstOfAllEmpty() {
        Option<Integer> first = Option.firstOf(
            Option.none(),
            Option.none()
        );
        assertTrue(first.isEmpty());
    }

    @Test
    @DisplayName("Option valuesOf extracts values")
    void valuesOf() {
        List<Integer> values = Option.valuesOf(
            Option.some(1),
            Option.none(),
            Option.some(2),
            Option.some(3)
        );
        assertEquals(3, values.size());
        assertTrue(values.contains(1));
        assertTrue(values.contains(2));
        assertTrue(values.contains(3));
    }

    @Test
    @DisplayName("Option Some record fields")
    void someRecordFields() {
        Option.Some<String> some = new Option.Some<>("value");
        assertEquals("value", some.value());
    }
}
