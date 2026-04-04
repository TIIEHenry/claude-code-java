/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code tree utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Tree data structure.
 */
public final class Tree<T> {
    private final T value;
    private final List<Tree<T>> children;
    private Tree<T> parent;

    public Tree(T value) {
        this.value = value;
        this.children = new ArrayList<>();
        this.parent = null;
    }

    /**
     * Get value.
     */
    public T getValue() {
        return value;
    }

    /**
     * Get parent.
     */
    public Optional<Tree<T>> getParent() {
        return Optional.ofNullable(parent);
    }

    /**
     * Get children.
     */
    public List<Tree<T>> getChildren() {
        return Collections.unmodifiableList(children);
    }

    /**
     * Add child.
     */
    public Tree<T> addChild(T childValue) {
        Tree<T> child = new Tree<>(childValue);
        child.parent = this;
        children.add(child);
        return child;
    }

    /**
     * Add child tree.
     */
    public Tree<T> addChild(Tree<T> child) {
        child.parent = this;
        children.add(child);
        return child;
    }

    /**
     * Remove child.
     */
    public boolean removeChild(Tree<T> child) {
        if (children.remove(child)) {
            child.parent = null;
            return true;
        }
        return false;
    }

    /**
     * Check if leaf.
     */
    public boolean isLeaf() {
        return children.isEmpty();
    }

    /**
     * Check if root.
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * Get depth.
     */
    public int depth() {
        if (isRoot()) return 0;
        return parent.depth() + 1;
    }

    /**
     * Get height.
     */
    public int height() {
        if (isLeaf()) return 0;
        return children.stream().mapToInt(Tree::height).max().orElse(0) + 1;
    }

    /**
     * Get size (total nodes).
     */
    public int size() {
        return 1 + children.stream().mapToInt(Tree::size).sum();
    }

    /**
     * Get root.
     */
    public Tree<T> getRoot() {
        return isRoot() ? this : parent.getRoot();
    }

    /**
     * Get path to root.
     */
    public List<Tree<T>> getPathToRoot() {
        List<Tree<T>> path = new ArrayList<>();
        Tree<T> current = this;
        while (current != null) {
            path.add(current);
            current = current.parent;
        }
        return path;
    }

    /**
     * Traverse pre-order.
     */
    public List<T> traversePreOrder() {
        List<T> result = new ArrayList<>();
        traversePreOrderHelper(result);
        return result;
    }

    private void traversePreOrderHelper(List<T> result) {
        result.add(value);
        for (Tree<T> child : children) {
            child.traversePreOrderHelper(result);
        }
    }

    /**
     * Traverse post-order.
     */
    public List<T> traversePostOrder() {
        List<T> result = new ArrayList<>();
        traversePostOrderHelper(result);
        return result;
    }

    private void traversePostOrderHelper(List<T> result) {
        for (Tree<T> child : children) {
            child.traversePostOrderHelper(result);
        }
        result.add(value);
    }

    /**
     * Traverse level-order (BFS).
     */
    public List<T> traverseLevelOrder() {
        List<T> result = new ArrayList<>();
        java.util.Queue<Tree<T>> queue = new java.util.LinkedList<>();
        queue.add(this);

        while (!queue.isEmpty()) {
            Tree<T> current = queue.poll();
            result.add(current.value);
            queue.addAll(current.children);
        }

        return result;
    }

    /**
     * Find first matching value.
     */
    public Optional<Tree<T>> find(Predicate<T> predicate) {
        if (predicate.test(value)) {
            return Optional.of(this);
        }
        for (Tree<T> child : children) {
            Optional<Tree<T>> found = child.find(predicate);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    /**
     * Find all matching values.
     */
    public List<Tree<T>> findAll(Predicate<T> predicate) {
        List<Tree<T>> result = new ArrayList<>();
        findAllHelper(predicate, result);
        return result;
    }

    private void findAllHelper(Predicate<T> predicate, List<Tree<T>> result) {
        if (predicate.test(value)) {
            result.add(this);
        }
        for (Tree<T> child : children) {
            child.findAllHelper(predicate, result);
        }
    }

    /**
     * Map tree values.
     */
    public <R> Tree<R> map(Function<T, R> mapper) {
        Tree<R> result = new Tree<>(mapper.apply(value));
        for (Tree<T> child : children) {
            result.addChild(child.map(mapper));
        }
        return result;
    }

    /**
     * Filter tree (keeps structure where matches exist).
     */
    public Tree<T> filter(Predicate<T> predicate) {
        Tree<T> result = new Tree<>(value);
        for (Tree<T> child : children) {
            Tree<T> filtered = child.filter(predicate);
            if (predicate.test(filtered.value) || !filtered.children.isEmpty()) {
                result.addChild(filtered);
            }
        }
        return result;
    }

    /**
     * Fold/reduce tree.
     */
    public <R> R fold(BiFunction<T, List<R>, R> reducer) {
        List<R> childResults = children.stream()
            .map(child -> child.fold(reducer))
            .toList();
        return reducer.apply(value, childResults);
    }

    /**
     * Get all leaf values.
     */
    public List<T> getLeaves() {
        List<T> result = new ArrayList<>();
        collectLeaves(result);
        return result;
    }

    private void collectLeaves(List<T> result) {
        if (isLeaf()) {
            result.add(value);
        } else {
            for (Tree<T> child : children) {
                child.collectLeaves(result);
            }
        }
    }

    /**
     * Get all node values.
     */
    public List<T> getAllValues() {
        return traversePreOrder();
    }

    /**
     * Pretty print tree.
     */
    public String prettyPrint() {
        StringBuilder sb = new StringBuilder();
        prettyPrintHelper(sb, "", true);
        return sb.toString();
    }

    private void prettyPrintHelper(StringBuilder sb, String prefix, boolean isTail) {
        sb.append(prefix).append(isTail ? "└── " : "├── ").append(value).append("\n");
        for (int i = 0; i < children.size(); i++) {
            children.get(i).prettyPrintHelper(sb,
                prefix + (isTail ? "    " : "│   "),
                i == children.size() - 1);
        }
    }

    @Override
    public String toString() {
        return String.format("Tree[%s, children=%d]", value, children.size());
    }

    /**
     * Tree utilities.
     */
    public static final class TreeUtils {
        private TreeUtils() {}

        /**
         * Create tree from root value.
         */
        public static <T> Tree<T> of(T value) {
            return new Tree<>(value);
        }

        /**
         * Create tree from root and children.
         */
        @SafeVarargs
        public static <T> Tree<T> of(T value, Tree<T>... children) {
            Tree<T> tree = new Tree<>(value);
            for (Tree<T> child : children) {
                tree.addChild(child);
            }
            return tree;
        }

        /**
         * Create from parent-child pairs.
         */
        public static <T> Tree<T> fromPairs(T root, Map<T, List<T>> childrenMap) {
            Tree<T> tree = new Tree<>(root);
            buildTree(tree, childrenMap);
            return tree;
        }

        private static <T> void buildTree(Tree<T> tree, Map<T, List<T>> childrenMap) {
            List<T> childValues = childrenMap.get(tree.getValue());
            if (childValues != null) {
                for (T childValue : childValues) {
                    Tree<T> child = new Tree<>(childValue);
                    tree.addChild(child);
                    buildTree(child, childrenMap);
                }
            }
        }

        /**
         * Create balanced binary tree from sorted list.
         */
        public static <T> Tree<T> balancedTree(List<T> sortedValues) {
            if (sortedValues.isEmpty()) return null;
            return buildBalanced(sortedValues, 0, sortedValues.size() - 1);
        }

        private static <T> Tree<T> buildBalanced(List<T> values, int start, int end) {
            if (start > end) return null;
            int mid = (start + end) / 2;
            Tree<T> root = new Tree<>(values.get(mid));
            Tree<T> left = buildBalanced(values, start, mid - 1);
            Tree<T> right = buildBalanced(values, mid + 1, end);
            if (left != null) root.addChild(left);
            if (right != null) root.addChild(right);
            return root;
        }
    }
}