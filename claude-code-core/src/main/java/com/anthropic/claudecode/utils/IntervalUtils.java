/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code interval utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;

/**
 * Interval utilities.
 */
public final class IntervalUtils {
    private IntervalUtils() {}

    /**
     * Interval record.
     */
    public record Interval(int start, int end) implements Comparable<Interval> {
        public Interval {
            if (start > end) {
                int temp = start;
                start = end;
                end = temp;
            }
        }

        public int length() {
            return end - start + 1;
        }

        public boolean contains(int point) {
            return point >= start && point <= end;
        }

        public boolean overlaps(Interval other) {
            return start <= other.end && other.start <= end;
        }

        public boolean adjacent(Interval other) {
            return end + 1 == other.start || other.end + 1 == start;
        }

        public Interval merge(Interval other) {
            if (!overlaps(other) && !adjacent(other)) {
                throw new IllegalArgumentException("Intervals don't overlap or touch");
            }
            return new Interval(Math.min(start, other.start), Math.max(end, other.end));
        }

        public Optional<Interval> intersection(Interval other) {
            if (!overlaps(other)) return Optional.empty();
            return Optional.of(new Interval(Math.max(start, other.start), Math.min(end, other.end)));
        }

        public List<Interval> subtract(Interval other) {
            if (!overlaps(other)) return List.of(this);

            List<Interval> result = new ArrayList<>();
            if (start < other.start) {
                result.add(new Interval(start, other.start - 1));
            }
            if (end > other.end) {
                result.add(new Interval(other.end + 1, end));
            }
            return result;
        }

        @Override
        public int compareTo(Interval other) {
            int cmp = Integer.compare(start, other.start);
            return cmp != 0 ? cmp : Integer.compare(end, other.end);
        }
    }

    /**
     * Merge overlapping intervals.
     */
    public static List<Interval> merge(List<Interval> intervals) {
        if (intervals.isEmpty()) return List.of();

        List<Interval> sorted = new ArrayList<>(intervals);
        sorted.sort(Comparator.comparingInt(Interval::start));

        List<Interval> result = new ArrayList<>();
        Interval current = sorted.get(0);

        for (int i = 1; i < sorted.size(); i++) {
            Interval next = sorted.get(i);
            if (current.overlaps(next) || current.adjacent(next)) {
                current = current.merge(next);
            } else {
                result.add(current);
                current = next;
            }
        }
        result.add(current);

        return result;
    }

    /**
     * Find intersection of all intervals.
     */
    public static Optional<Interval> intersection(List<Interval> intervals) {
        if (intervals.isEmpty()) return Optional.empty();

        int maxStart = intervals.stream().mapToInt(Interval::start).max().orElse(0);
        int minEnd = intervals.stream().mapToInt(Interval::end).min().orElse(0);

        if (maxStart > minEnd) return Optional.empty();
        return Optional.of(new Interval(maxStart, minEnd));
    }

    /**
     * Find point coverage.
     */
    public static int coverage(List<Interval> intervals) {
        return merge(intervals).stream().mapToInt(Interval::length).sum();
    }

    /**
     * Check if point is covered by any interval.
     */
    public static boolean isCovered(List<Interval> intervals, int point) {
        return intervals.stream().anyMatch(i -> i.contains(point));
    }

    /**
     * Find gaps between intervals.
     */
    public static List<Interval> gaps(List<Interval> intervals, int min, int max) {
        List<Interval> merged = merge(intervals);
        List<Interval> gaps = new ArrayList<>();

        int current = min;
        for (Interval interval : merged) {
            if (interval.start > current) {
                gaps.add(new Interval(current, interval.start - 1));
            }
            current = Math.max(current, interval.end + 1);
        }

        if (current <= max) {
            gaps.add(new Interval(current, max));
        }

        return gaps;
    }

    /**
     * Subtract intervals.
     */
    public static List<Interval> subtract(Interval from, List<Interval> toRemove) {
        List<Interval> result = List.of(from);
        for (Interval remove : merge(toRemove)) {
            List<Interval> newResult = new ArrayList<>();
            for (Interval current : result) {
                newResult.addAll(current.subtract(remove));
            }
            result = newResult;
        }
        return result;
    }

    /**
     * Find all points in range covered by intervals.
     */
    public static Set<Integer> coveredPoints(List<Interval> intervals, int min, int max) {
        Set<Integer> points = new HashSet<>();
        for (Interval interval : intervals) {
            for (int i = Math.max(min, interval.start); i <= Math.min(max, interval.end); i++) {
                points.add(i);
            }
        }
        return points;
    }

    /**
     * Find overlapping pairs.
     */
    public static List<Interval[]> findOverlaps(List<Interval> intervals) {
        List<Interval[]> overlaps = new ArrayList<>();
        for (int i = 0; i < intervals.size(); i++) {
            for (int j = i + 1; j < intervals.size(); j++) {
                if (intervals.get(i).overlaps(intervals.get(j))) {
                    overlaps.add(new Interval[]{intervals.get(i), intervals.get(j)});
                }
            }
        }
        return overlaps;
    }

    /**
     * Split interval at point.
     */
    public static List<Interval> split(Interval interval, int point) {
        if (!interval.contains(point) || point == interval.start || point == interval.end) {
            return List.of(interval);
        }
        return List.of(
            new Interval(interval.start, point - 1),
            new Interval(point, interval.end)
        );
    }
}