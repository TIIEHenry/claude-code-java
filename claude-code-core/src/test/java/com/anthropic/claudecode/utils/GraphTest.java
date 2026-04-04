/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Graph.
 */
class GraphTest {

    @Test
    @DisplayName("Graph creates empty")
    void createsEmpty() {
        Graph<String> graph = new Graph<>();

        assertEquals(0, graph.vertexCount());
        assertEquals(0, graph.edgeCount());
    }

    @Test
    @DisplayName("Graph creates directed")
    void createsDirected() {
        Graph<String> graph = new Graph<>(true);

        graph.addEdge("a", "b");

        assertTrue(graph.hasEdge("a", "b"));
        assertFalse(graph.hasEdge("b", "a"));
    }

    @Test
    @DisplayName("Graph creates undirected")
    void createsUndirected() {
        Graph<String> graph = new Graph<>(false);

        graph.addEdge("a", "b");

        assertTrue(graph.hasEdge("a", "b"));
        assertTrue(graph.hasEdge("b", "a"));
    }

    @Test
    @DisplayName("Graph addVertex adds vertex")
    void addVertex() {
        Graph<String> graph = new Graph<>();

        graph.addVertex("a");

        assertTrue(graph.hasVertex("a"));
        assertEquals(1, graph.vertexCount());
    }

    @Test
    @DisplayName("Graph addEdge adds edge and vertices")
    void addEdge() {
        Graph<String> graph = new Graph<>();

        graph.addEdge("a", "b");

        assertTrue(graph.hasVertex("a"));
        assertTrue(graph.hasVertex("b"));
        assertTrue(graph.hasEdge("a", "b"));
    }

    @Test
    @DisplayName("Graph removeVertex removes vertex")
    void removeVertex() {
        Graph<String> graph = new Graph<>();

        graph.addEdge("a", "b");
        graph.removeVertex("a");

        assertFalse(graph.hasVertex("a"));
        assertFalse(graph.hasEdge("a", "b"));
    }

    @Test
    @DisplayName("Graph removeEdge removes edge")
    void removeEdge() {
        Graph<String> graph = new Graph<>();

        graph.addEdge("a", "b");
        graph.removeEdge("a", "b");

        assertFalse(graph.hasEdge("a", "b"));
    }

    @Test
    @DisplayName("Graph hasVertex checks vertex")
    void hasVertex() {
        Graph<String> graph = new Graph<>();

        graph.addVertex("a");

        assertTrue(graph.hasVertex("a"));
        assertFalse(graph.hasVertex("b"));
    }

    @Test
    @DisplayName("Graph hasEdge checks edge")
    void hasEdge() {
        Graph<String> graph = new Graph<>();

        graph.addEdge("a", "b");

        assertTrue(graph.hasEdge("a", "b"));
        assertFalse(graph.hasEdge("a", "c"));
    }

    @Test
    @DisplayName("Graph neighbors returns neighbors")
    void neighbors() {
        Graph<String> graph = new Graph<>();

        graph.addEdge("a", "b");
        graph.addEdge("a", "c");

        Set<String> neighbors = graph.neighbors("a");

        assertEquals(2, neighbors.size());
        assertTrue(neighbors.contains("b"));
        assertTrue(neighbors.contains("c"));
    }

    @Test
    @DisplayName("Graph vertices returns all vertices")
    void vertices() {
        Graph<String> graph = new Graph<>();

        graph.addVertex("a");
        graph.addVertex("b");

        Set<String> vertices = graph.vertices();

        assertEquals(2, vertices.size());
    }

    @Test
    @DisplayName("Graph edges returns all edges")
    void edges() {
        Graph<String> graph = new Graph<>();

        graph.addEdge("a", "b");
        graph.addEdge("b", "c");

        Set<Graph.Edge<String>> edges = graph.edges();

        assertEquals(2, edges.size());
    }

    @Test
    @DisplayName("Graph bfs traverses breadth first")
    void bfs() {
        Graph<String> graph = new Graph<>();

        graph.addEdge("a", "b");
        graph.addEdge("a", "c");
        graph.addEdge("b", "d");

        List<String> result = graph.bfs("a");

        assertEquals(4, result.size());
        assertEquals("a", result.get(0));
        // b and c can be in any order
        assertTrue(result.contains("b"));
        assertTrue(result.contains("c"));
        assertTrue(result.contains("d"));
    }

    @Test
    @DisplayName("Graph dfs traverses depth first")
    void dfs() {
        Graph<String> graph = new Graph<>();

        graph.addEdge("a", "b");
        graph.addEdge("b", "c");

        List<String> result = graph.dfs("a");

        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
    }

    @Test
    @DisplayName("Graph isConnected checks connectivity")
    void isConnected() {
        Graph<String> graph = new Graph<>();

        graph.addEdge("a", "b");
        graph.addEdge("b", "c");

        assertTrue(graph.isConnected());

        graph.addVertex("d"); // Disconnected

        assertFalse(graph.isConnected());
    }

    @Test
    @DisplayName("Graph shortestPath finds path")
    void shortestPath() {
        Graph<String> graph = new Graph<>();

        graph.addEdge("a", "b");
        graph.addEdge("b", "c");
        graph.addEdge("a", "c");

        Optional<List<String>> path = graph.shortestPath("a", "c");

        assertTrue(path.isPresent());
        assertEquals(List.of("a", "c"), path.get());
    }

    @Test
    @DisplayName("Graph shortestPath returns empty when no path")
    void shortestPathNoPath() {
        Graph<String> graph = new Graph<>();

        graph.addVertex("a");
        graph.addVertex("b");

        Optional<List<String>> path = graph.shortestPath("a", "b");

        assertFalse(path.isPresent());
    }

    @Test
    @DisplayName("Graph shortestPath returns self for same node")
    void shortestPathSameNode() {
        Graph<String> graph = new Graph<>();

        graph.addVertex("a");

        Optional<List<String>> path = graph.shortestPath("a", "a");

        assertTrue(path.isPresent());
        assertEquals(List.of("a"), path.get());
    }

    @Test
    @DisplayName("Graph topologicalSort sorts directed")
    void topologicalSort() {
        Graph<String> graph = new Graph<>(true);

        graph.addEdge("a", "b");
        graph.addEdge("b", "c");

        Optional<List<String>> sorted = graph.topologicalSort();

        assertTrue(sorted.isPresent());
        assertTrue(sorted.get().indexOf("a") < sorted.get().indexOf("b"));
        assertTrue(sorted.get().indexOf("b") < sorted.get().indexOf("c"));
    }

    @Test
    @DisplayName("Graph topologicalSort fails on undirected")
    void topologicalSortUndirected() {
        Graph<String> graph = new Graph<>(false);

        assertThrows(IllegalStateException.class, graph::topologicalSort);
    }

    @Test
    @DisplayName("Graph topologicalSort returns empty on cycle")
    void topologicalSortCycle() {
        Graph<String> graph = new Graph<>(true);

        graph.addEdge("a", "b");
        graph.addEdge("b", "a");

        Optional<List<String>> sorted = graph.topologicalSort();

        assertFalse(sorted.isPresent());
    }

    @Test
    @DisplayName("Graph connectedComponents finds components")
    void connectedComponents() {
        Graph<String> graph = new Graph<>();

        graph.addEdge("a", "b");
        graph.addEdge("c", "d");

        List<Set<String>> components = graph.connectedComponents();

        assertEquals(2, components.size());
    }

    @Test
    @DisplayName("Graph clear removes all")
    void clearWorks() {
        Graph<String> graph = new Graph<>();

        graph.addEdge("a", "b");
        graph.clear();

        assertEquals(0, graph.vertexCount());
    }

    @Test
    @DisplayName("Graph reverse reverses edges")
    void reverseWorks() {
        Graph<String> graph = new Graph<>(true);

        graph.addEdge("a", "b");
        Graph<String> reversed = graph.reverse();

        assertTrue(reversed.hasEdge("b", "a"));
        assertFalse(reversed.hasEdge("a", "b"));
    }

    @Test
    @DisplayName("Graph toString shows info")
    void toStringWorks() {
        Graph<String> graph = new Graph<>();

        graph.addEdge("a", "b");

        String str = graph.toString();

        assertTrue(str.contains("vertices=2"));
        assertTrue(str.contains("edges=1"));
    }

    @Test
    @DisplayName("Graph Edge record works")
    void edgeRecord() {
        Graph.Edge<String> edge = new Graph.Edge<>("a", "b");

        assertEquals("a", edge.from());
        assertEquals("b", edge.to());
        assertTrue(edge.toString().contains("a"));
        assertTrue(edge.toString().contains("b"));
    }
}