/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Tree.
 */
class TreeTest {

    @Test
    @DisplayName("Tree creates with value")
    void createsWithValue() {
        Tree<String> tree = new Tree<>("root");

        assertEquals("root", tree.getValue());
        assertTrue(tree.isRoot());
        assertTrue(tree.isLeaf());
        assertEquals(0, tree.depth());
        assertEquals(0, tree.height());
        assertEquals(1, tree.size());
    }

    @Test
    @DisplayName("Tree addChild adds child")
    void addChild() {
        Tree<String> tree = new Tree<>("root");
        Tree<String> child = tree.addChild("child1");

        assertEquals("child1", child.getValue());
        assertEquals(tree, child.getParent().orElse(null));
        assertFalse(tree.isLeaf());
        assertEquals(1, tree.getChildren().size());
    }

    @Test
    @DisplayName("Tree addChild with tree adds subtree")
    void addChildTree() {
        Tree<String> tree = new Tree<>("root");
        Tree<String> subtree = new Tree<>("sub");
        subtree.addChild("subchild");

        tree.addChild(subtree);

        assertEquals(tree, subtree.getParent().orElse(null));
        assertEquals(1, tree.getChildren().size());
    }

    @Test
    @DisplayName("Tree removeChild removes child")
    void removeChild() {
        Tree<String> tree = new Tree<>("root");
        Tree<String> child = tree.addChild("child1");

        assertTrue(tree.removeChild(child));
        assertFalse(child.getParent().isPresent());
        assertTrue(tree.isLeaf());

        assertFalse(tree.removeChild(child)); // Already removed
    }

    @Test
    @DisplayName("Tree isLeaf checks children")
    void isLeaf() {
        Tree<String> leaf = new Tree<>("leaf");
        Tree<String> nonLeaf = new Tree<>("root");
        nonLeaf.addChild("child");

        assertTrue(leaf.isLeaf());
        assertFalse(nonLeaf.isLeaf());
    }

    @Test
    @DisplayName("Tree isRoot checks parent")
    void isRoot() {
        Tree<String> root = new Tree<>("root");
        Tree<String> child = root.addChild("child");

        assertTrue(root.isRoot());
        assertFalse(child.isRoot());
    }

    @Test
    @DisplayName("Tree depth returns distance to root")
    void depth() {
        Tree<String> root = new Tree<>("root");
        Tree<String> child1 = root.addChild("child1");
        Tree<String> child2 = child1.addChild("child2");

        assertEquals(0, root.depth());
        assertEquals(1, child1.depth());
        assertEquals(2, child2.depth());
    }

    @Test
    @DisplayName("Tree height returns max distance to leaf")
    void height() {
        Tree<String> root = new Tree<>("root");
        root.addChild("child1");
        Tree<String> child2 = root.addChild("child2");
        child2.addChild("grandchild");

        // root has children, child2 has grandchild (height 1), so root height = 2
        assertEquals(2, root.height());
        assertEquals(1, child2.height());
        assertEquals(0, child2.getChildren().get(0).height());
    }

    @Test
    @DisplayName("Tree size returns total nodes")
    void size() {
        Tree<String> root = new Tree<>("root");
        root.addChild("child1");
        root.addChild("child2");
        root.getChildren().get(0).addChild("grandchild");

        assertEquals(4, root.size());
    }

    @Test
    @DisplayName("Tree getRoot returns root")
    void getRoot() {
        Tree<String> root = new Tree<>("root");
        Tree<String> child = root.addChild("child");
        Tree<String> grandchild = child.addChild("grandchild");

        assertEquals(root, root.getRoot());
        assertEquals(root, child.getRoot());
        assertEquals(root, grandchild.getRoot());
    }

    @Test
    @DisplayName("Tree getPathToRoot returns path")
    void getPathToRoot() {
        Tree<String> root = new Tree<>("root");
        Tree<String> child = root.addChild("child");
        Tree<String> grandchild = child.addChild("grandchild");

        List<Tree<String>> path = grandchild.getPathToRoot();

        assertEquals(3, path.size());
        assertEquals(grandchild, path.get(0));
        assertEquals(child, path.get(1));
        assertEquals(root, path.get(2));
    }

    @Test
    @DisplayName("Tree traversePreOrder visits root first")
    void traversePreOrder() {
        Tree<String> root = new Tree<>("root");
        root.addChild("a");
        root.addChild("b");
        root.getChildren().get(0).addChild("a1");

        List<String> result = root.traversePreOrder();

        assertEquals(List.of("root", "a", "a1", "b"), result);
    }

    @Test
    @DisplayName("Tree traversePostOrder visits children first")
    void traversePostOrder() {
        Tree<String> root = new Tree<>("root");
        root.addChild("a");
        root.addChild("b");

        List<String> result = root.traversePostOrder();

        assertEquals(List.of("a", "b", "root"), result);
    }

    @Test
    @DisplayName("Tree traverseLevelOrder visits level by level")
    void traverseLevelOrder() {
        Tree<String> root = new Tree<>("root");
        root.addChild("a");
        root.addChild("b");
        root.getChildren().get(0).addChild("a1");

        List<String> result = root.traverseLevelOrder();

        assertEquals(List.of("root", "a", "b", "a1"), result);
    }

    @Test
    @DisplayName("Tree find returns matching node")
    void find() {
        Tree<String> root = new Tree<>("root");
        root.addChild("target");
        root.addChild("other");

        Optional<Tree<String>> found = root.find(v -> v.equals("target"));

        assertTrue(found.isPresent());
        assertEquals("target", found.get().getValue());
    }

    @Test
    @DisplayName("Tree find returns empty when not found")
    void findNotFound() {
        Tree<String> root = new Tree<>("root");
        root.addChild("child");

        Optional<Tree<String>> found = root.find(v -> v.equals("missing"));

        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Tree findAll returns all matches")
    void findAll() {
        Tree<String> root = new Tree<>("root");
        root.addChild("a");
        root.addChild("a");
        root.addChild("b");

        List<Tree<String>> found = root.findAll(v -> v.equals("a"));

        assertEquals(2, found.size());
    }

    @Test
    @DisplayName("Tree map transforms values")
    void map() {
        Tree<Integer> root = new Tree<>(1);
        root.addChild(2);
        root.addChild(3);

        Tree<String> mapped = root.map(v -> "v" + v);

        assertEquals("v1", mapped.getValue());
        assertEquals("v2", mapped.getChildren().get(0).getValue());
        assertEquals("v3", mapped.getChildren().get(1).getValue());
    }

    @Test
    @DisplayName("Tree filter keeps matching nodes")
    void filter() {
        Tree<Integer> root = new Tree<>(1);
        root.addChild(2);
        root.addChild(3);
        root.getChildren().get(0).addChild(4);

        // Filter keeps nodes where value matches OR has matching descendants
        Tree<Integer> filtered = root.filter(v -> v % 2 == 0);

        // Root (1) doesn't match, but has child 2 which matches
        assertEquals(1, filtered.getValue()); // Root value preserved
        assertFalse(filtered.getChildren().isEmpty()); // Has child 2
    }

    @Test
    @DisplayName("Tree fold reduces tree")
    void fold() {
        Tree<Integer> root = new Tree<>(1);
        root.addChild(2);
        root.addChild(3);

        Integer sum = root.fold((value, childResults) ->
            value + childResults.stream().mapToInt(i -> i).sum());

        assertEquals(6, sum);
    }

    @Test
    @DisplayName("Tree getLeaves returns leaf values")
    void getLeaves() {
        Tree<String> root = new Tree<>("root");
        root.addChild("a");
        root.addChild("b");
        root.getChildren().get(0).addChild("a1");

        List<String> leaves = root.getLeaves();

        assertEquals(List.of("a1", "b"), leaves);
    }

    @Test
    @DisplayName("Tree getAllValues returns all values")
    void getAllValues() {
        Tree<String> root = new Tree<>("root");
        root.addChild("a");
        root.addChild("b");

        List<String> values = root.getAllValues();

        assertEquals(3, values.size());
        assertTrue(values.contains("root"));
        assertTrue(values.contains("a"));
        assertTrue(values.contains("b"));
    }

    @Test
    @DisplayName("Tree prettyPrint formats tree")
    void prettyPrint() {
        Tree<String> root = new Tree<>("root");
        root.addChild("a");
        root.addChild("b");

        String printed = root.prettyPrint();

        assertTrue(printed.contains("root"));
        assertTrue(printed.contains("a"));
        assertTrue(printed.contains("b"));
    }

    @Test
    @DisplayName("Tree toString shows value and children count")
    void toStringWorks() {
        Tree<String> tree = new Tree<>("root");
        tree.addChild("child");

        String str = tree.toString();

        assertTrue(str.contains("root"));
        assertTrue(str.contains("children=1"));
    }

    @Test
    @DisplayName("Tree TreeUtils of creates tree")
    void treeUtilsOf() {
        Tree<String> tree = Tree.TreeUtils.of("root");

        assertEquals("root", tree.getValue());
        assertTrue(tree.isRoot());
    }

    @Test
    @DisplayName("Tree TreeUtils of with children")
    void treeUtilsOfWithChildren() {
        Tree<String> child1 = new Tree<>("child1");
        Tree<String> child2 = new Tree<>("child2");
        Tree<String> tree = Tree.TreeUtils.of("root", child1, child2);

        assertEquals(2, tree.getChildren().size());
    }

    @Test
    @DisplayName("Tree TreeUtils fromPairs creates tree")
    void treeUtilsFromPairs() {
        Map<String, List<String>> children = Map.of(
            "root", List.of("a", "b"),
            "a", List.of("a1")
        );

        Tree<String> tree = Tree.TreeUtils.fromPairs("root", children);

        assertEquals("root", tree.getValue());
        assertEquals(2, tree.getChildren().size());
    }

    @Test
    @DisplayName("Tree TreeUtils balancedTree creates balanced")
    void treeUtilsBalancedTree() {
        List<Integer> values = List.of(1, 2, 3, 4, 5, 6, 7);

        Tree<Integer> tree = Tree.TreeUtils.balancedTree(values);

        assertEquals(4, tree.getValue()); // Middle value
        assertEquals(2, tree.height());
    }

    @Test
    @DisplayName("Tree TreeUtils balancedTree empty list")
    void treeUtilsBalancedTreeEmpty() {
        Tree<Integer> tree = Tree.TreeUtils.balancedTree(List.of());

        assertNull(tree);
    }

    @Test
    @DisplayName("Tree getChildren is unmodifiable")
    void getChildrenUnmodifiable() {
        Tree<String> tree = new Tree<>("root");
        tree.addChild("child");

        List<Tree<String>> children = tree.getChildren();

        assertThrows(UnsupportedOperationException.class, () -> children.add(new Tree<>("new")));
    }

    @Test
    @DisplayName("Tree handles null value")
    void handlesNull() {
        Tree<String> tree = new Tree<>(null);

        assertNull(tree.getValue());
        assertTrue(tree.isRoot());
    }
}