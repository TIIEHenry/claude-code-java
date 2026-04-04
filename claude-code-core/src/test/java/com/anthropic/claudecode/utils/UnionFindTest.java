/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for UnionFind.
 */
class UnionFindTest {

    @Test
    @DisplayName("UnionFind creates empty")
    void createsEmpty() {
        UnionFind<String> uf = new UnionFind<>();

        assertEquals(0, uf.size());
        assertEquals(0, uf.componentCount());
    }

    @Test
    @DisplayName("UnionFind add adds element")
    void addElement() {
        UnionFind<String> uf = new UnionFind<>();

        uf.add("a");

        assertEquals(1, uf.size());
        assertEquals(1, uf.componentCount());
        assertTrue(uf.contains("a"));
    }

    @Test
    @DisplayName("UnionFind add doesn't duplicate")
    void addNoDuplicate() {
        UnionFind<String> uf = new UnionFind<>();

        uf.add("a");
        uf.add("a");

        assertEquals(1, uf.size());
    }

    @Test
    @DisplayName("UnionFind addAll adds all")
    void addAll() {
        UnionFind<String> uf = new UnionFind<>();

        uf.addAll(List.of("a", "b", "c"));

        assertEquals(3, uf.size());
        assertEquals(3, uf.componentCount());
    }

    @Test
    @DisplayName("UnionFind find returns root")
    void findRoot() {
        UnionFind<String> uf = new UnionFind<>();

        uf.add("a");

        assertEquals("a", uf.find("a"));
    }

    @Test
    @DisplayName("UnionFind find adds missing element")
    void findAddsMissing() {
        UnionFind<String> uf = new UnionFind<>();

        assertEquals("a", uf.find("a"));
        assertEquals(1, uf.size());
    }

    @Test
    @DisplayName("UnionFind union connects elements")
    void unionConnects() {
        UnionFind<String> uf = new UnionFind<>();

        uf.add("a");
        uf.add("b");
        uf.union("a", "b");

        assertTrue(uf.connected("a", "b"));
        assertEquals(1, uf.componentCount());
    }

    @Test
    @DisplayName("UnionFind union same element does nothing")
    void unionSame() {
        UnionFind<String> uf = new UnionFind<>();

        uf.add("a");
        uf.union("a", "a");

        assertEquals(1, uf.componentCount());
    }

    @Test
    @DisplayName("UnionFind union already connected does nothing")
    void unionAlreadyConnected() {
        UnionFind<String> uf = new UnionFind<>();

        uf.add("a");
        uf.add("b");
        uf.union("a", "b");
        int count = uf.componentCount();
        uf.union("a", "b");

        assertEquals(count, uf.componentCount());
    }

    @Test
    @DisplayName("UnionFind connected checks connection")
    void connectedWorks() {
        UnionFind<String> uf = new UnionFind<>();

        uf.add("a");
        uf.add("b");
        uf.add("c");

        uf.union("a", "b");

        assertTrue(uf.connected("a", "b"));
        assertFalse(uf.connected("a", "c"));
        assertFalse(uf.connected("b", "c"));
    }

    @Test
    @DisplayName("UnionFind connected works for missing elements")
    void connectedMissing() {
        UnionFind<String> uf = new UnionFind<>();

        // find adds missing elements, so they become connected
        assertTrue(uf.connected("a", "a"));
    }

    @Test
    @DisplayName("UnionFind contains checks existence")
    void containsWorks() {
        UnionFind<String> uf = new UnionFind<>();

        uf.add("a");

        assertTrue(uf.contains("a"));
        assertFalse(uf.contains("b"));
    }

    @Test
    @DisplayName("UnionFind getComponent returns component")
    void getComponent() {
        UnionFind<String> uf = new UnionFind<>();

        uf.add("a");
        uf.add("b");
        uf.add("c");
        uf.union("a", "b");

        Set<String> component = uf.getComponent("a");

        assertEquals(2, component.size());
        assertTrue(component.contains("a"));
        assertTrue(component.contains("b"));
        assertFalse(component.contains("c"));
    }

    @Test
    @DisplayName("UnionFind getComponents returns all")
    void getComponents() {
        UnionFind<String> uf = new UnionFind<>();

        uf.add("a");
        uf.add("b");
        uf.add("c");
        uf.union("a", "b");

        Collection<Set<String>> components = uf.getComponents();

        assertEquals(2, components.size());
    }

    @Test
    @DisplayName("UnionFind getComponentSize returns size")
    void getComponentSize() {
        UnionFind<String> uf = new UnionFind<>();

        uf.add("a");
        uf.add("b");
        uf.add("c");
        uf.union("a", "b");

        assertEquals(2, uf.getComponentSize("a"));
        assertEquals(1, uf.getComponentSize("c"));
    }

    @Test
    @DisplayName("UnionFind clear removes all")
    void clearWorks() {
        UnionFind<String> uf = new UnionFind<>();

        uf.add("a");
        uf.add("b");
        uf.clear();

        assertEquals(0, uf.size());
        assertEquals(0, uf.componentCount());
    }

    @Test
    @DisplayName("UnionFind toString shows info")
    void toStringWorks() {
        UnionFind<String> uf = new UnionFind<>();
        uf.add("a");

        String str = uf.toString();

        assertTrue(str.contains("elements=1"));
        assertTrue(str.contains("components=1"));
    }

    @Test
    @DisplayName("UnionFind UnionFindUtils fromCollection creates")
    void unionFindUtilsFromCollection() {
        UnionFind<String> uf = UnionFind.UnionFindUtils.fromCollection(List.of("a", "b", "c"));

        assertEquals(3, uf.size());
        assertEquals(3, uf.componentCount());
    }

    @Test
    @DisplayName("UnionFind UnionFindUtils fromPairs creates connected")
    void unionFindUtilsFromPairs() {
        UnionFind<String> uf = UnionFind.UnionFindUtils.fromPairs(
            List.of(Map.entry("a", "b"), Map.entry("b", "c"))
        );

        assertTrue(uf.connected("a", "b"));
        assertTrue(uf.connected("b", "c"));
        assertTrue(uf.connected("a", "c"));
    }

    @Test
    @DisplayName("UnionFind UnionFindUtils isFullyConnected checks")
    void unionFindUtilsIsFullyConnected() {
        UnionFind<String> uf = new UnionFind<>();
        uf.add("a");
        uf.add("b");
        uf.union("a", "b");

        assertTrue(UnionFind.UnionFindUtils.isFullyConnected(uf));
    }

    @Test
    @DisplayName("UnionFind UnionFindUtils connectionsNeeded returns count")
    void unionFindUtilsConnectionsNeeded() {
        UnionFind<String> uf = new UnionFind<>();
        uf.add("a");
        uf.add("b");
        uf.add("c");

        assertEquals(2, UnionFind.UnionFindUtils.connectionsNeeded(uf));
    }

    @Test
    @DisplayName("UnionFind path compression works")
    void pathCompression() {
        UnionFind<Integer> uf = new UnionFind<>();

        for (int i = 0; i < 10; i++) {
            uf.add(i);
        }

        // Create chain
        for (int i = 1; i < 10; i++) {
            uf.union(i - 1, i);
        }

        // All should be connected
        assertTrue(uf.connected(0, 9));
        assertEquals(1, uf.componentCount());
    }
}