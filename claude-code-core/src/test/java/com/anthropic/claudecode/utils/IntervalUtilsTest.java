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
 * Tests for IntervalUtils.
 */
class IntervalUtilsTest {

    @Test
    @DisplayName("IntervalUtils Interval constructor normalizes start > end")
    void intervalConstructorNormalizes() {
        IntervalUtils.Interval interval = new IntervalUtils.Interval(10, 5);

        assertEquals(5, interval.start());
        assertEquals(10, interval.end());
    }

    @Test
    @DisplayName("IntervalUtils Interval length calculates correctly")
    void intervalLength() {
        IntervalUtils.Interval interval = new IntervalUtils.Interval(5, 10);

        assertEquals(6, interval.length());
    }

    @Test
    @DisplayName("IntervalUtils Interval contains point")
    void intervalContainsPoint() {
        IntervalUtils.Interval interval = new IntervalUtils.Interval(5, 10);

        assertTrue(interval.contains(5));
        assertTrue(interval.contains(7));
        assertTrue(interval.contains(10));
        assertFalse(interval.contains(4));
        assertFalse(interval.contains(11));
    }

    @Test
    @DisplayName("IntervalUtils Interval overlaps")
    void intervalOverlaps() {
        IntervalUtils.Interval a = new IntervalUtils.Interval(5, 10);
        IntervalUtils.Interval b = new IntervalUtils.Interval(8, 15);
        IntervalUtils.Interval c = new IntervalUtils.Interval(11, 20);

        assertTrue(a.overlaps(b));
        assertTrue(b.overlaps(a));
        assertFalse(a.overlaps(c));
    }

    @Test
    @DisplayName("IntervalUtils Interval adjacent")
    void intervalAdjacent() {
        IntervalUtils.Interval a = new IntervalUtils.Interval(5, 10);
        IntervalUtils.Interval b = new IntervalUtils.Interval(11, 15);
        IntervalUtils.Interval c = new IntervalUtils.Interval(12, 15);

        assertTrue(a.adjacent(b));
        assertFalse(a.adjacent(c));
    }

    @Test
    @DisplayName("IntervalUtils Interval merge overlapping")
    void intervalMergeOverlapping() {
        IntervalUtils.Interval a = new IntervalUtils.Interval(5, 10);
        IntervalUtils.Interval b = new IntervalUtils.Interval(8, 15);

        IntervalUtils.Interval merged = a.merge(b);

        assertEquals(5, merged.start());
        assertEquals(15, merged.end());
    }

    @Test
    @DisplayName("IntervalUtils Interval merge adjacent")
    void intervalMergeAdjacent() {
        IntervalUtils.Interval a = new IntervalUtils.Interval(5, 10);
        IntervalUtils.Interval b = new IntervalUtils.Interval(11, 15);

        IntervalUtils.Interval merged = a.merge(b);

        assertEquals(5, merged.start());
        assertEquals(15, merged.end());
    }

    @Test
    @DisplayName("IntervalUtils Interval merge throws for non-overlapping")
    void intervalMergeThrows() {
        IntervalUtils.Interval a = new IntervalUtils.Interval(5, 10);
        IntervalUtils.Interval b = new IntervalUtils.Interval(12, 15);

        assertThrows(IllegalArgumentException.class, () -> a.merge(b));
    }

    @Test
    @DisplayName("IntervalUtils Interval intersection")
    void intervalIntersection() {
        IntervalUtils.Interval a = new IntervalUtils.Interval(5, 10);
        IntervalUtils.Interval b = new IntervalUtils.Interval(8, 15);

        Optional<IntervalUtils.Interval> result = a.intersection(b);

        assertTrue(result.isPresent());
        assertEquals(8, result.get().start());
        assertEquals(10, result.get().end());
    }

    @Test
    @DisplayName("IntervalUtils Interval intersection empty for non-overlapping")
    void intervalIntersectionEmpty() {
        IntervalUtils.Interval a = new IntervalUtils.Interval(5, 10);
        IntervalUtils.Interval b = new IntervalUtils.Interval(11, 15);

        Optional<IntervalUtils.Interval> result = a.intersection(b);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("IntervalUtils Interval subtract returns original for non-overlapping")
    void intervalSubtractNonOverlapping() {
        IntervalUtils.Interval a = new IntervalUtils.Interval(5, 10);
        IntervalUtils.Interval b = new IntervalUtils.Interval(15, 20);

        List<IntervalUtils.Interval> result = a.subtract(b);

        assertEquals(1, result.size());
        assertEquals(a, result.get(0));
    }

    @Test
    @DisplayName("IntervalUtils Interval subtract splits interval")
    void intervalSubtractSplits() {
        IntervalUtils.Interval a = new IntervalUtils.Interval(5, 15);
        IntervalUtils.Interval b = new IntervalUtils.Interval(8, 10);

        List<IntervalUtils.Interval> result = a.subtract(b);

        assertEquals(2, result.size());
        assertEquals(new IntervalUtils.Interval(5, 7), result.get(0));
        assertEquals(new IntervalUtils.Interval(11, 15), result.get(1));
    }

    @Test
    @DisplayName("IntervalUtils Interval subtract removes middle")
    void intervalSubtractMiddle() {
        IntervalUtils.Interval a = new IntervalUtils.Interval(5, 10);
        IntervalUtils.Interval b = new IntervalUtils.Interval(6, 9);

        List<IntervalUtils.Interval> result = a.subtract(b);

        assertEquals(2, result.size());
        assertEquals(new IntervalUtils.Interval(5, 5), result.get(0));
        assertEquals(new IntervalUtils.Interval(10, 10), result.get(1));
    }

    @Test
    @DisplayName("IntervalUtils Interval compareTo")
    void intervalCompareTo() {
        IntervalUtils.Interval a = new IntervalUtils.Interval(5, 10);
        IntervalUtils.Interval b = new IntervalUtils.Interval(8, 15);
        IntervalUtils.Interval c = new IntervalUtils.Interval(5, 8);

        assertTrue(a.compareTo(b) < 0);
        assertTrue(b.compareTo(a) > 0);
        assertTrue(a.compareTo(c) > 0);
        assertEquals(0, a.compareTo(a));
    }

    @Test
    @DisplayName("IntervalUtils merge list of intervals")
    void mergeList() {
        List<IntervalUtils.Interval> intervals = List.of(
            new IntervalUtils.Interval(5, 10),
            new IntervalUtils.Interval(8, 15),
            new IntervalUtils.Interval(20, 25)
        );

        List<IntervalUtils.Interval> result = IntervalUtils.merge(intervals);

        assertEquals(2, result.size());
        assertEquals(new IntervalUtils.Interval(5, 15), result.get(0));
        assertEquals(new IntervalUtils.Interval(20, 25), result.get(1));
    }

    @Test
    @DisplayName("IntervalUtils merge empty list")
    void mergeEmpty() {
        List<IntervalUtils.Interval> result = IntervalUtils.merge(List.of());

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("IntervalUtils merge adjacent intervals")
    void mergeAdjacentList() {
        List<IntervalUtils.Interval> intervals = List.of(
            new IntervalUtils.Interval(5, 10),
            new IntervalUtils.Interval(11, 15)
        );

        List<IntervalUtils.Interval> result = IntervalUtils.merge(intervals);

        assertEquals(1, result.size());
        assertEquals(new IntervalUtils.Interval(5, 15), result.get(0));
    }

    @Test
    @DisplayName("IntervalUtils intersection of list")
    void intersectionList() {
        List<IntervalUtils.Interval> intervals = List.of(
            new IntervalUtils.Interval(5, 20),
            new IntervalUtils.Interval(10, 25),
            new IntervalUtils.Interval(8, 15)
        );

        Optional<IntervalUtils.Interval> result = IntervalUtils.intersection(intervals);

        assertTrue(result.isPresent());
        assertEquals(10, result.get().start());
        assertEquals(15, result.get().end());
    }

    @Test
    @DisplayName("IntervalUtils intersection empty list")
    void intersectionEmptyList() {
        Optional<IntervalUtils.Interval> result = IntervalUtils.intersection(List.of());

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("IntervalUtils intersection no common")
    void intersectionNoCommon() {
        List<IntervalUtils.Interval> intervals = List.of(
            new IntervalUtils.Interval(5, 10),
            new IntervalUtils.Interval(15, 20)
        );

        Optional<IntervalUtils.Interval> result = IntervalUtils.intersection(intervals);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("IntervalUtils coverage calculates total length")
    void coverage() {
        List<IntervalUtils.Interval> intervals = List.of(
            new IntervalUtils.Interval(5, 10),
            new IntervalUtils.Interval(15, 20)
        );

        int result = IntervalUtils.coverage(intervals);

        assertEquals(12, result);
    }

    @Test
    @DisplayName("IntervalUtils coverage merges overlapping")
    void coverageOverlapping() {
        List<IntervalUtils.Interval> intervals = List.of(
            new IntervalUtils.Interval(5, 10),
            new IntervalUtils.Interval(8, 15)
        );

        int result = IntervalUtils.coverage(intervals);

        assertEquals(11, result);
    }

    @Test
    @DisplayName("IntervalUtils isCovered checks point coverage")
    void isCovered() {
        List<IntervalUtils.Interval> intervals = List.of(
            new IntervalUtils.Interval(5, 10),
            new IntervalUtils.Interval(15, 20)
        );

        assertTrue(IntervalUtils.isCovered(intervals, 7));
        assertTrue(IntervalUtils.isCovered(intervals, 17));
        assertFalse(IntervalUtils.isCovered(intervals, 12));
    }

    @Test
    @DisplayName("IntervalUtils gaps finds gaps between intervals")
    void gaps() {
        List<IntervalUtils.Interval> intervals = List.of(
            new IntervalUtils.Interval(5, 10),
            new IntervalUtils.Interval(15, 20)
        );

        List<IntervalUtils.Interval> result = IntervalUtils.gaps(intervals, 0, 25);

        assertEquals(3, result.size());
        assertEquals(new IntervalUtils.Interval(0, 4), result.get(0));
        assertEquals(new IntervalUtils.Interval(11, 14), result.get(1));
        assertEquals(new IntervalUtils.Interval(21, 25), result.get(2));
    }

    @Test
    @DisplayName("IntervalUtils gaps empty intervals")
    void gapsEmpty() {
        List<IntervalUtils.Interval> result = IntervalUtils.gaps(List.of(), 0, 10);

        assertEquals(1, result.size());
        assertEquals(new IntervalUtils.Interval(0, 10), result.get(0));
    }

    @Test
    @DisplayName("IntervalUtils subtract intervals from interval")
    void subtractFromInterval() {
        IntervalUtils.Interval from = new IntervalUtils.Interval(0, 20);
        List<IntervalUtils.Interval> toRemove = List.of(
            new IntervalUtils.Interval(5, 10),
            new IntervalUtils.Interval(15, 18)
        );

        List<IntervalUtils.Interval> result = IntervalUtils.subtract(from, toRemove);

        assertEquals(3, result.size());
        assertEquals(new IntervalUtils.Interval(0, 4), result.get(0));
        assertEquals(new IntervalUtils.Interval(11, 14), result.get(1));
        assertEquals(new IntervalUtils.Interval(19, 20), result.get(2));
    }

    @Test
    @DisplayName("IntervalUtils coveredPoints returns set of points")
    void coveredPoints() {
        List<IntervalUtils.Interval> intervals = List.of(
            new IntervalUtils.Interval(5, 7),
            new IntervalUtils.Interval(10, 12)
        );

        Set<Integer> result = IntervalUtils.coveredPoints(intervals, 0, 15);

        assertTrue(result.contains(5));
        assertTrue(result.contains(6));
        assertTrue(result.contains(7));
        assertTrue(result.contains(10));
        assertTrue(result.contains(11));
        assertTrue(result.contains(12));
        assertFalse(result.contains(8));
        assertFalse(result.contains(13));
    }

    @Test
    @DisplayName("IntervalUtils coveredPoints respects bounds")
    void coveredPointsBounds() {
        List<IntervalUtils.Interval> intervals = List.of(
            new IntervalUtils.Interval(0, 10)
        );

        Set<Integer> result = IntervalUtils.coveredPoints(intervals, 5, 8);

        assertFalse(result.contains(4));
        assertFalse(result.contains(9));
        assertTrue(result.contains(5));
        assertTrue(result.contains(8));
    }

    @Test
    @DisplayName("IntervalUtils findOverlaps returns overlapping pairs")
    void findOverlaps() {
        List<IntervalUtils.Interval> intervals = List.of(
            new IntervalUtils.Interval(5, 10),
            new IntervalUtils.Interval(8, 15),
            new IntervalUtils.Interval(20, 25)
        );

        List<IntervalUtils.Interval[]> result = IntervalUtils.findOverlaps(intervals);

        assertEquals(1, result.size());
        assertEquals(new IntervalUtils.Interval(5, 10), result.get(0)[0]);
        assertEquals(new IntervalUtils.Interval(8, 15), result.get(0)[1]);
    }

    @Test
    @DisplayName("IntervalUtils split interval at point")
    void splitInterval() {
        IntervalUtils.Interval interval = new IntervalUtils.Interval(5, 10);

        List<IntervalUtils.Interval> result = IntervalUtils.split(interval, 7);

        assertEquals(2, result.size());
        assertEquals(new IntervalUtils.Interval(5, 6), result.get(0));
        assertEquals(new IntervalUtils.Interval(7, 10), result.get(1));
    }

    @Test
    @DisplayName("IntervalUtils split at start returns original")
    void splitAtStart() {
        IntervalUtils.Interval interval = new IntervalUtils.Interval(5, 10);

        List<IntervalUtils.Interval> result = IntervalUtils.split(interval, 5);

        assertEquals(1, result.size());
        assertEquals(interval, result.get(0));
    }

    @Test
    @DisplayName("IntervalUtils split at end returns original")
    void splitAtEnd() {
        IntervalUtils.Interval interval = new IntervalUtils.Interval(5, 10);

        List<IntervalUtils.Interval> result = IntervalUtils.split(interval, 10);

        assertEquals(1, result.size());
        assertEquals(interval, result.get(0));
    }

    @Test
    @DisplayName("IntervalUtils split outside interval returns original")
    void splitOutside() {
        IntervalUtils.Interval interval = new IntervalUtils.Interval(5, 10);

        List<IntervalUtils.Interval> result = IntervalUtils.split(interval, 15);

        assertEquals(1, result.size());
        assertEquals(interval, result.get(0));
    }
}