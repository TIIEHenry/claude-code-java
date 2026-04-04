/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code graph utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Graph data structure and utilities.
 */
public final class Graph<T> {
    private final Map<T, Set<T>> adjacency = new HashMap<>();
    private final boolean directed;

    public Graph() {
        this(false);
    }

    public Graph(boolean directed) {
        this.directed = directed;
    }

    /**
     * Add vertex.
     */
    public void addVertex(T vertex) {
        adjacency.computeIfAbsent(vertex, k -> new HashSet<>());
    }

    /**
     * Add edge.
     */
    public void addEdge(T from, T to) {
        addVertex(from);
        addVertex(to);
        adjacency.get(from).add(to);
        if (!directed) {
            adjacency.get(to).add(from);
        }
    }

    /**
     * Remove vertex.
     */
    public void removeVertex(T vertex) {
        adjacency.remove(vertex);
        adjacency.values().forEach(neighbors -> neighbors.remove(vertex));
    }

    /**
     * Remove edge.
     */
    public void removeEdge(T from, T to) {
        Set<T> fromNeighbors = adjacency.get(from);
        if (fromNeighbors != null) {
            fromNeighbors.remove(to);
        }
        if (!directed) {
            Set<T> toNeighbors = adjacency.get(to);
            if (toNeighbors != null) {
                toNeighbors.remove(from);
            }
        }
    }

    /**
     * Check if vertex exists.
     */
    public boolean hasVertex(T vertex) {
        return adjacency.containsKey(vertex);
    }

    /**
     * Check if edge exists.
     */
    public boolean hasEdge(T from, T to) {
        Set<T> neighbors = adjacency.get(from);
        return neighbors != null && neighbors.contains(to);
    }

    /**
     * Get neighbors.
     */
    public Set<T> neighbors(T vertex) {
        return adjacency.getOrDefault(vertex, Set.of());
    }

    /**
     * Get all vertices.
     */
    public Set<T> vertices() {
        return adjacency.keySet();
    }

    /**
     * Get all edges.
     */
    public Set<Edge<T>> edges() {
        Set<Edge<T>> result = new HashSet<>();
        for (Map.Entry<T, Set<T>> entry : adjacency.entrySet()) {
            for (T neighbor : entry.getValue()) {
                if (directed || entry.getKey().toString().compareTo(neighbor.toString()) <= 0) {
                    result.add(new Edge<>(entry.getKey(), neighbor));
                }
            }
        }
        return result;
    }

    /**
     * Get vertex count.
     */
    public int vertexCount() {
        return adjacency.size();
    }

    /**
     * Get edge count.
     */
    public int edgeCount() {
        return adjacency.values().stream().mapToInt(Set::size).sum() / (directed ? 1 : 2);
    }

    /**
     * BFS traversal.
     */
    public List<T> bfs(T start) {
        List<T> result = new ArrayList<>();
        Set<T> visited = new HashSet<>();
        java.util.Queue<T> queue = new ArrayDeque<T>();

        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            T current = queue.poll();
            result.add(current);

            for (T neighbor : neighbors(current)) {
                if (visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        return result;
    }

    /**
     * DFS traversal.
     */
    public List<T> dfs(T start) {
        List<T> result = new ArrayList<>();
        Set<T> visited = new HashSet<>();
        dfsHelper(start, visited, result);
        return result;
    }

    private void dfsHelper(T vertex, Set<T> visited, List<T> result) {
        if (!visited.add(vertex)) return;
        result.add(vertex);
        for (T neighbor : neighbors(vertex)) {
            dfsHelper(neighbor, visited, result);
        }
    }

    /**
     * Check if connected (undirected).
     */
    public boolean isConnected() {
        if (directed) throw new IllegalStateException("Use isStronglyConnected for directed graphs");
        if (adjacency.isEmpty()) return true;
        return bfs(adjacency.keySet().iterator().next()).size() == adjacency.size();
    }

    /**
     * Check if strongly connected (directed).
     */
    public boolean isStronglyConnected() {
        if (adjacency.isEmpty()) return true;
        T start = adjacency.keySet().iterator().next();
        if (bfs(start).size() != adjacency.size()) return false;

        Graph<T> reversed = reverse();
        return reversed.bfs(start).size() == adjacency.size();
    }

    /**
     * Reverse graph.
     */
    public Graph<T> reverse() {
        Graph<T> reversed = new Graph<>(directed);
        for (T vertex : vertices()) {
            reversed.addVertex(vertex);
        }
        for (Edge<T> edge : edges()) {
            reversed.addEdge(edge.to, edge.from);
        }
        return reversed;
    }

    /**
     * Shortest path (BFS for unweighted).
     */
    public Optional<List<T>> shortestPath(T start, T end) {
        if (!hasVertex(start) || !hasVertex(end)) return Optional.empty();
        if (start.equals(end)) return Optional.of(List.of(start));

        Map<T, T> parent = new HashMap<>();
        java.util.Queue<T> queue = new ArrayDeque<T>();
        Set<T> visited = new HashSet<>();

        queue.add(start);
        visited.add(start);
        parent.put(start, null);

        while (!queue.isEmpty()) {
            T current = queue.poll();

            if (current.equals(end)) {
                return Optional.of(reconstructPath(parent, end));
            }

            for (T neighbor : neighbors(current)) {
                if (visited.add(neighbor)) {
                    parent.put(neighbor, current);
                    queue.add(neighbor);
                }
            }
        }

        return Optional.empty();
    }

    private List<T> reconstructPath(Map<T, T> parent, T end) {
        List<T> path = new ArrayList<>();
        T current = end;
        while (current != null) {
            path.add(current);
            current = parent.get(current);
        }
        Collections.reverse(path);
        return path;
    }

    /**
     * Topological sort (directed).
     */
    public Optional<List<T>> topologicalSort() {
        if (!directed) throw new IllegalStateException("Topological sort requires directed graph");

        Map<T, Integer> inDegree = new HashMap<>();
        for (T vertex : vertices()) {
            inDegree.put(vertex, 0);
        }
        for (Edge<T> edge : edges()) {
            inDegree.merge(edge.to, 1, Integer::sum);
        }

        java.util.Queue<T> queue = new ArrayDeque<T>();
        for (Map.Entry<T, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.add(entry.getKey());
            }
        }

        List<T> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            T current = queue.poll();
            result.add(current);

            for (T neighbor : neighbors(current)) {
                int newDegree = inDegree.merge(neighbor, -1, Integer::sum);
                if (newDegree == 0) {
                    queue.add(neighbor);
                }
            }
        }

        return result.size() == adjacency.size() ? Optional.of(result) : Optional.empty();
    }

    /**
     * Find connected components.
     */
    public List<Set<T>> connectedComponents() {
        List<Set<T>> components = new ArrayList<>();
        Set<T> visited = new HashSet<>();

        for (T vertex : vertices()) {
            if (!visited.contains(vertex)) {
                Set<T> component = new HashSet<>();
                componentBfs(vertex, component);
                visited.addAll(component);
                components.add(component);
            }
        }

        return components;
    }

    private void componentBfs(T start, Set<T> component) {
        java.util.Queue<T> queue = new ArrayDeque<T>();
        queue.add(start);
        component.add(start);

        while (!queue.isEmpty()) {
            T current = queue.poll();
            for (T neighbor : neighbors(current)) {
                if (component.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
    }

    /**
     * Clear graph.
     */
    public void clear() {
        adjacency.clear();
    }

    /**
     * Edge record.
     */
    public record Edge<T>(T from, T to) {
        @Override
        public String toString() {
            return from + " -> " + to;
        }
    }

    @Override
    public String toString() {
        return String.format("Graph[vertices=%d, edges=%d, directed=%b]",
            vertexCount(), edgeCount(), directed);
    }
}