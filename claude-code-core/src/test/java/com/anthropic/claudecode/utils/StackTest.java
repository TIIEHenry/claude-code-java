/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.EmptyStackException;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Stack.
 */
class StackTest {

    @Test
    @DisplayName("Stack creates empty")
    void createsEmpty() {
        Stack<String> stack = new Stack<>();

        assertTrue(stack.isEmpty());
        assertEquals(0, stack.size());
    }

    @Test
    @DisplayName("Stack creates with capacity")
    void createsWithCapacity() {
        Stack<String> stack = new Stack<>(5);

        assertTrue(stack.isEmpty());
        assertEquals(0, stack.size());
    }

    @Test
    @DisplayName("Stack push and pop works")
    void pushPop() {
        Stack<String> stack = new Stack<>();

        stack.push("a");
        stack.push("b");
        stack.push("c");

        assertEquals(3, stack.size());
        assertEquals("c", stack.pop());
        assertEquals("b", stack.pop());
        assertEquals("a", stack.pop());
        assertTrue(stack.isEmpty());
    }

    @Test
    @DisplayName("Stack pop throws when empty")
    void popThrowsWhenEmpty() {
        Stack<String> stack = new Stack<>();

        assertThrows(EmptyStackException.class, stack::pop);
    }

    @Test
    @DisplayName("Stack popOrNull returns null when empty")
    void popOrNullReturnsNull() {
        Stack<String> stack = new Stack<>();

        assertNull(stack.popOrNull());
    }

    @Test
    @DisplayName("Stack peek returns top element")
    void peekWorks() {
        Stack<String> stack = new Stack<>();

        stack.push("a");
        stack.push("b");

        assertEquals("b", stack.peek());
        assertEquals(2, stack.size()); // Not removed
    }

    @Test
    @DisplayName("Stack peek throws when empty")
    void peekThrowsWhenEmpty() {
        Stack<String> stack = new Stack<>();

        assertThrows(EmptyStackException.class, stack::peek);
    }

    @Test
    @DisplayName("Stack peekOrNull returns null when empty")
    void peekOrNullReturnsNull() {
        Stack<String> stack = new Stack<>();

        assertNull(stack.peekOrNull());
    }

    @Test
    @DisplayName("Stack peek at depth works")
    void peekAtDepth() {
        Stack<String> stack = new Stack<>();

        stack.push("a");
        stack.push("b");
        stack.push("c");

        assertEquals("c", stack.peek(0)); // Top
        assertEquals("b", stack.peek(1));
        assertEquals("a", stack.peek(2)); // Bottom
    }

    @Test
    @DisplayName("Stack peek at depth throws on invalid")
    void peekAtDepthThrows() {
        Stack<String> stack = new Stack<>();
        stack.push("a");

        assertThrows(IndexOutOfBoundsException.class, () -> stack.peek(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> stack.peek(1));
    }

    @Test
    @DisplayName("Stack contains finds element")
    void containsWorks() {
        Stack<String> stack = new Stack<>();

        stack.push("a");
        stack.push("b");

        assertTrue(stack.contains("a"));
        assertTrue(stack.contains("b"));
        assertFalse(stack.contains("c"));
    }

    @Test
    @DisplayName("Stack search finds element")
    void searchWorks() {
        Stack<String> stack = new Stack<>();

        stack.push("a");
        stack.push("b");
        stack.push("c");

        assertEquals(1, stack.search("c")); // Top, 1-based
        assertEquals(2, stack.search("b"));
        assertEquals(3, stack.search("a"));
        assertEquals(-1, stack.search("missing"));
    }

    @Test
    @DisplayName("Stack toList returns bottom to top")
    void toListWorks() {
        Stack<String> stack = new Stack<>();

        stack.push("a");
        stack.push("b");
        stack.push("c");

        List<String> list = stack.toList();

        assertEquals(List.of("a", "b", "c"), list);
    }

    @Test
    @DisplayName("Stack toListReversed returns top to bottom")
    void toListReversedWorks() {
        Stack<String> stack = new Stack<>();

        stack.push("a");
        stack.push("b");
        stack.push("c");

        List<String> list = stack.toListReversed();

        assertEquals(List.of("c", "b", "a"), list);
    }

    @Test
    @DisplayName("Stack clear removes all")
    void clearWorks() {
        Stack<String> stack = new Stack<>();

        stack.push("a");
        stack.push("b");
        stack.clear();

        assertTrue(stack.isEmpty());
        assertEquals(0, stack.size());
    }

    @Test
    @DisplayName("Stack iterator works")
    void iteratorWorks() {
        Stack<String> stack = new Stack<>();

        stack.push("a");
        stack.push("b");

        StringBuilder sb = new StringBuilder();
        for (String s : stack) {
            sb.append(s);
        }

        assertEquals("ab", sb.toString());
    }

    @Test
    @DisplayName("Stack iterator throws when empty")
    void iteratorThrows() {
        Stack<String> stack = new Stack<>();
        var iterator = stack.iterator();

        assertFalse(iterator.hasNext());
        assertThrows(NoSuchElementException.class, iterator::next);
    }

    @Test
    @DisplayName("Stack expands capacity")
    void expandsCapacity() {
        Stack<Integer> stack = new Stack<>(2);

        for (int i = 0; i < 100; i++) {
            stack.push(i);
        }

        assertEquals(100, stack.size());
        assertEquals(99, stack.peek());
    }

    @Test
    @DisplayName("Stack withPush supplier executes")
    void withPushSupplier() {
        Stack<String> stack = new Stack<>();
        stack.push("base");

        String result = stack.withPush("temp", () -> {
            assertEquals(2, stack.size());
            assertEquals("temp", stack.peek());
            return "result";
        });

        assertEquals("result", result);
        assertEquals(1, stack.size());
        assertEquals("base", stack.peek());
    }

    @Test
    @DisplayName("Stack withPush runnable executes")
    void withPushRunnable() {
        Stack<String> stack = new Stack<>();
        stack.push("base");

        stack.withPush("temp", () -> {
            assertEquals(2, stack.size());
        });

        assertEquals(1, stack.size());
        assertEquals("base", stack.peek());
    }

    @Test
    @DisplayName("Stack toString returns list representation")
    void toStringWorks() {
        Stack<String> stack = new Stack<>();

        stack.push("a");
        stack.push("b");

        String str = stack.toString();

        assertEquals("[a, b]", str);
    }

    @Test
    @DisplayName("Stack StackUtils of creates stack")
    void stackUtilsOf() {
        Stack<String> stack = Stack.StackUtils.of("a", "b", "c");

        assertEquals(3, stack.size());
        assertEquals("c", stack.peek());
    }

    @Test
    @DisplayName("Stack StackUtils fromCollection creates stack")
    void stackUtilsFromCollection() {
        Stack<String> stack = Stack.StackUtils.fromCollection(List.of("a", "b"));

        assertEquals(2, stack.size());
        assertEquals("b", stack.peek());
    }

    @Test
    @DisplayName("Stack handles null elements")
    void handlesNull() {
        Stack<String> stack = new Stack<>();

        stack.push(null);
        stack.push("value");

        assertTrue(stack.contains(null));
        assertEquals("value", stack.pop());
        assertNull(stack.pop());
    }

    @Test
    @DisplayName("Stack maintains order after multiple operations")
    void maintainsOrder() {
        Stack<Integer> stack = new Stack<>();

        stack.push(1);
        stack.push(2);
        stack.pop();
        stack.push(3);
        stack.push(4);
        stack.pop();
        stack.push(5);

        assertEquals(3, stack.size());
        assertEquals(List.of(1, 3, 5), stack.toList());
    }
}