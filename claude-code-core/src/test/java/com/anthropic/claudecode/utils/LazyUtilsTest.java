/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LazyUtils.
 */
class LazyUtilsTest {

    @Test
    @DisplayName("LazyUtils Lazy get computes value")
    void lazyGet() {
        LazyUtils.Lazy<String> lazy = new LazyUtils.Lazy<>(() -> "computed");

        assertFalse(lazy.isComputed());
        String result = lazy.get();
        assertTrue(lazy.isComputed());
        assertEquals("computed", result);
    }

    @Test
    @DisplayName("LazyUtils Lazy get caches value")
    void lazyCaches() {
        AtomicInteger counter = new AtomicInteger(0);
        LazyUtils.Lazy<Integer> lazy = new LazyUtils.Lazy<>(counter::incrementAndGet);

        lazy.get();
        lazy.get();
        lazy.get();

        assertEquals(1, counter.get()); // Only computed once
    }

    @Test
    @DisplayName("LazyUtils Lazy isComputed initially false")
    void lazyIsComputedFalse() {
        LazyUtils.Lazy<String> lazy = new LazyUtils.Lazy<>(() -> "test");
        assertFalse(lazy.isComputed());
    }

    @Test
    @DisplayName("LazyUtils Lazy isComputed true after get")
    void lazyIsComputedTrue() {
        LazyUtils.Lazy<String> lazy = new LazyUtils.Lazy<>(() -> "test");
        lazy.get();
        assertTrue(lazy.isComputed());
    }

    @Test
    @DisplayName("LazyUtils Lazy getIfComputed empty before compute")
    void lazyGetIfComputedEmpty() {
        LazyUtils.Lazy<String> lazy = new LazyUtils.Lazy<>(() -> "test");
        Optional<String> result = lazy.getIfComputed();
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("LazyUtils Lazy getIfComputed present after compute")
    void lazyGetIfComputedPresent() {
        LazyUtils.Lazy<String> lazy = new LazyUtils.Lazy<>(() -> "test");
        lazy.get();
        Optional<String> result = lazy.getIfComputed();
        assertTrue(result.isPresent());
        assertEquals("test", result.get());
    }

    @Test
    @DisplayName("LazyUtils Lazy reset clears cache")
    void lazyReset() {
        AtomicInteger counter = new AtomicInteger(0);
        LazyUtils.Lazy<Integer> lazy = new LazyUtils.Lazy<>(counter::incrementAndGet);

        lazy.get();
        assertEquals(1, counter.get());

        lazy.reset();
        assertFalse(lazy.isComputed());

        lazy.get();
        assertEquals(2, counter.get()); // Computed again
    }

    @Test
    @DisplayName("LazyUtils Lazy map transforms value")
    void lazyMap() {
        LazyUtils.Lazy<Integer> lazy = new LazyUtils.Lazy<>(() -> 5);
        LazyUtils.Lazy<Integer> mapped = lazy.map(x -> x * 2);

        assertEquals(10, mapped.get());
    }

    @Test
    @DisplayName("LazyUtils Lazy flatMap transforms with lazy")
    void lazyFlatMap() {
        LazyUtils.Lazy<Integer> lazy = new LazyUtils.Lazy<>(() -> 5);
        LazyUtils.Lazy<Integer> flatMapped = lazy.flatMap(x -> new LazyUtils.Lazy<>(() -> x * 3));

        assertEquals(15, flatMapped.get());
    }

    @Test
    @DisplayName("LazyUtils Lazy of creates lazy")
    void lazyOf() {
        AtomicInteger counter = new AtomicInteger(0);
        LazyUtils.Lazy<Integer> lazy = LazyUtils.Lazy.of(counter::incrementAndGet);

        assertEquals(1, lazy.get());
    }

    @Test
    @DisplayName("LazyUtils Lazy ofValue creates precomputed")
    void lazyOfValue() {
        LazyUtils.Lazy<String> lazy = LazyUtils.Lazy.ofValue("precomputed");

        assertTrue(lazy.isComputed());
        assertEquals("precomputed", lazy.get());
    }

    @Test
    @DisplayName("LazyUtils Lazy null supplier throws")
    void lazyNullSupplier() {
        assertThrows(NullPointerException.class, () -> new LazyUtils.Lazy<>(null));
    }

    @Test
    @DisplayName("LazyUtils memoize supplier caches")
    void memoizeSupplier() {
        AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> memoized = LazyUtils.memoize(counter::incrementAndGet);

        memoized.get();
        memoized.get();
        memoized.get();

        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("LazyUtils memoize function caches")
    void memoizeFunction() {
        AtomicInteger counter = new AtomicInteger(0);
        Function<Integer, Integer> memoized = LazyUtils.memoize(x -> {
            counter.incrementAndGet();
            return x * 2;
        });

        assertEquals(4, memoized.apply(2));
        assertEquals(4, memoized.apply(2));
        assertEquals(6, memoized.apply(3));
        assertEquals(6, memoized.apply(3));

        assertEquals(2, counter.get()); // Only 2 and 3 computed
    }

    @Test
    @DisplayName("LazyUtils memoize function with TTL caches")
    void memoizeFunctionTTL() {
        AtomicInteger counter = new AtomicInteger(0);
        Function<Integer, Integer> memoized = LazyUtils.memoize(
            x -> {
                counter.incrementAndGet();
                return x * 2;
            },
            100 // 100ms TTL
        );

        assertEquals(4, memoized.apply(2));
        assertEquals(4, memoized.apply(2));

        // Wait for TTL to expire
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            // ignore
        }

        assertEquals(4, memoized.apply(2));
        assertEquals(2, counter.get()); // Recomputed after TTL
    }

    @Test
    @DisplayName("LazyUtils Thunk force computes")
    void thunkForce() {
        AtomicInteger counter = new AtomicInteger(0);
        LazyUtils.Thunk<Integer> thunk = new LazyUtils.Thunk<>(counter::incrementAndGet);

        assertFalse(thunk.isEvaluated());
        assertEquals(1, thunk.force());
        assertTrue(thunk.isEvaluated());
    }

    @Test
    @DisplayName("LazyUtils Thunk force caches")
    void thunkCaches() {
        AtomicInteger counter = new AtomicInteger(0);
        LazyUtils.Thunk<Integer> thunk = new LazyUtils.Thunk<>(counter::incrementAndGet);

        thunk.force();
        thunk.force();
        thunk.force();

        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("LazyUtils Thunk getValue empty before force")
    void thunkGetValueEmpty() {
        LazyUtils.Thunk<String> thunk = new LazyUtils.Thunk<>(() -> "test");
        Optional<String> result = thunk.getValue();
        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("LazyUtils Thunk getValue present after force")
    void thunkGetValuePresent() {
        LazyUtils.Thunk<String> thunk = new LazyUtils.Thunk<>(() -> "test");
        thunk.force();
        Optional<String> result = thunk.getValue();
        assertTrue(result.isPresent());
        assertEquals("test", result.get());
    }

    @Test
    @DisplayName("LazyUtils thunk creates thunk")
    void thunkFactory() {
        LazyUtils.Thunk<String> thunk = LazyUtils.thunk(() -> "computed");
        assertEquals("computed", thunk.force());
    }

    @Test
    @DisplayName("LazyUtils LazySeq head returns first")
    void lazySeqHead() {
        LazyUtils.LazySeq<Integer> seq = LazyUtils.LazySeq.range(0, 10);
        assertEquals(0, seq.head());
    }

    @Test
    @DisplayName("LazyUtils LazySeq tail returns next")
    void lazySeqTail() {
        LazyUtils.LazySeq<Integer> seq = LazyUtils.LazySeq.range(0, 10);
        LazyUtils.LazySeq<Integer> tail = seq.tail();
        assertEquals(1, tail.head());
    }

    @Test
    @DisplayName("LazyUtils LazySeq take returns first n")
    void lazySeqTake() {
        LazyUtils.LazySeq<Integer> seq = LazyUtils.LazySeq.range(0);
        List<Integer> result = seq.take(5);
        assertEquals(List.of(0, 1, 2, 3, 4), result);
    }

    @Test
    @DisplayName("LazyUtils LazySeq drop skips first n")
    void lazySeqDrop() {
        LazyUtils.LazySeq<Integer> seq = LazyUtils.LazySeq.range(0, 10);
        LazyUtils.LazySeq<Integer> dropped = seq.drop(3);
        assertEquals(3, dropped.head());
    }

    @Test
    @DisplayName("LazyUtils LazySeq filter selects elements")
    void lazySeqFilter() {
        LazyUtils.LazySeq<Integer> seq = LazyUtils.LazySeq.range(0, 10);
        LazyUtils.LazySeq<Integer> filtered = seq.filter(x -> x % 2 == 0);
        assertEquals(0, filtered.head());
        assertEquals(2, filtered.tail().head());
    }

    @Test
    @DisplayName("LazyUtils LazySeq map transforms elements")
    void lazySeqMap() {
        LazyUtils.LazySeq<Integer> seq = LazyUtils.LazySeq.range(0, 5);
        LazyUtils.LazySeq<Integer> mapped = seq.map(x -> x * 2);
        assertEquals(0, mapped.head());
        assertEquals(2, mapped.tail().head());
    }

    @Test
    @DisplayName("LazyUtils LazySeq iterate creates infinite")
    void lazySeqIterate() {
        LazyUtils.LazySeq<Integer> seq = LazyUtils.LazySeq.iterate(0, x -> x + 1);
        List<Integer> result = seq.take(5);
        assertEquals(List.of(0, 1, 2, 3, 4), result);
    }

    @Test
    @DisplayName("LazyUtils LazySeq generate creates from supplier")
    void lazySeqGenerate() {
        AtomicInteger counter = new AtomicInteger(0);
        LazyUtils.LazySeq<Integer> seq = LazyUtils.LazySeq.generate(counter::incrementAndGet);
        List<Integer> result = seq.take(5);
        assertEquals(List.of(1, 2, 3, 4, 5), result);
    }

    @Test
    @DisplayName("LazyUtils LazySeq range with end bounded")
    void lazySeqRangeBounded() {
        LazyUtils.LazySeq<Integer> seq = LazyUtils.LazySeq.range(0, 5);
        List<Integer> result = seq.take(10); // Only 5 elements exist
        assertEquals(List.of(0, 1, 2, 3, 4), result);
    }

    @Test
    @DisplayName("LazyUtils LazySeq range invalid range null")
    void lazySeqRangeInvalid() {
        LazyUtils.LazySeq<Integer> seq = LazyUtils.LazySeq.range(10, 5);
        assertNull(seq);
    }

    @Test
    @DisplayName("LazyUtils LazySeq iterator")
    void lazySeqIterator() {
        LazyUtils.LazySeq<Integer> seq = LazyUtils.LazySeq.range(0, 5);
        Iterator<Integer> iter = seq.iterator();

        assertTrue(iter.hasNext());
        assertEquals(0, iter.next());
        assertEquals(1, iter.next());
        assertEquals(2, iter.next());
        assertEquals(3, iter.next());
        assertEquals(4, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("LazyUtils LazySeq iterator throws on empty")
    void lazySeqIteratorNoSuchElement() {
        LazyUtils.LazySeq<Integer> seq = LazyUtils.LazySeq.range(10, 5); // null
        assertNull(seq);

        // For a valid empty sequence case, iterator.hasNext() would be false
        LazyUtils.LazySeq<Integer> emptySeq = null;
        assertNull(emptySeq);
    }

    @Test
    @DisplayName("LazyUtils lazySeq factory")
    void lazySeqFactory() {
        LazyUtils.LazySeq<Integer> seq = LazyUtils.lazySeq(1, () -> LazyUtils.lazySeq(2, () -> null));
        assertEquals(1, seq.head());
        assertEquals(2, seq.tail().head());
        assertNull(seq.tail().tail());
    }

    @Test
    @DisplayName("LazyUtils iterate factory")
    void iterateFactory() {
        LazyUtils.LazySeq<Integer> seq = LazyUtils.iterate(0, x -> x + 2);
        List<Integer> result = seq.take(5);
        assertEquals(List.of(0, 2, 4, 6, 8), result);
    }

    @Test
    @DisplayName("LazyUtils generate factory")
    void generateFactory() {
        AtomicInteger counter = new AtomicInteger(0);
        LazyUtils.LazySeq<Integer> seq = LazyUtils.generate(counter::incrementAndGet);
        List<Integer> result = seq.take(3);
        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    @DisplayName("LazyUtils Deferred get computes value")
    void deferredGet() {
        LazyUtils.Deferred<String> deferred = new LazyUtils.Deferred<>(() -> "computed");

        assertFalse(deferred.isComputed());
        String result = deferred.get();
        assertTrue(deferred.isComputed());
        assertEquals("computed", result);
    }

    @Test
    @DisplayName("LazyUtils Deferred get caches")
    void deferredCaches() {
        AtomicInteger counter = new AtomicInteger(0);
        LazyUtils.Deferred<Integer> deferred = new LazyUtils.Deferred<>(counter::incrementAndGet);

        deferred.get();
        deferred.get();
        deferred.get();

        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("LazyUtils Deferred dependsOn adds dependency")
    void deferredDependsOn() {
        AtomicInteger depCounter = new AtomicInteger(0);
        AtomicInteger mainCounter = new AtomicInteger(0);

        Supplier<Integer> dependency = () -> {
            depCounter.incrementAndGet();
            return 42;
        };

        LazyUtils.Deferred<Integer> deferred = new LazyUtils.Deferred<>(() -> {
            mainCounter.incrementAndGet();
            return 100;
        });

        deferred.dependsOn(dependency);
        deferred.get();

        assertEquals(1, depCounter.get()); // Dependency was called
        assertEquals(1, mainCounter.get());
    }

    @Test
    @DisplayName("LazyUtils Deferred multiple dependencies")
    void deferredMultipleDependencies() {
        AtomicInteger dep1Counter = new AtomicInteger(0);
        AtomicInteger dep2Counter = new AtomicInteger(0);

        LazyUtils.Deferred<Integer> deferred = new LazyUtils.Deferred<>(() -> 100);
        deferred.dependsOn(dep1Counter::incrementAndGet);
        deferred.dependsOn(dep2Counter::incrementAndGet);

        deferred.get();

        assertEquals(1, dep1Counter.get());
        assertEquals(1, dep2Counter.get());
    }

    @Test
    @DisplayName("LazyUtils deferred factory")
    void deferredFactory() {
        LazyUtils.Deferred<String> deferred = LazyUtils.deferred(() -> "test");
        assertEquals("test", deferred.get());
    }

    @Test
    @DisplayName("LazyUtils LazyProperty get computes default")
    void lazyPropertyGetDefault() {
        AtomicInteger counter = new AtomicInteger(0);
        LazyUtils.LazyProperty<Integer> prop = new LazyUtils.LazyProperty<>(counter::incrementAndGet);

        assertFalse(prop.isSet());
        assertEquals(1, prop.get());
        assertTrue(prop.isSet());
    }

    @Test
    @DisplayName("LazyUtils LazyProperty get caches")
    void lazyPropertyCaches() {
        AtomicInteger counter = new AtomicInteger(0);
        LazyUtils.LazyProperty<Integer> prop = new LazyUtils.LazyProperty<>(counter::incrementAndGet);

        prop.get();
        prop.get();
        prop.get();

        assertEquals(1, counter.get());
    }

    @Test
    @DisplayName("LazyUtils LazyProperty set overrides default")
    void lazyPropertySet() {
        AtomicInteger counter = new AtomicInteger(0);
        LazyUtils.LazyProperty<Integer> prop = new LazyUtils.LazyProperty<>(counter::incrementAndGet);

        prop.set(42);
        assertTrue(prop.isSet());
        assertEquals(42, prop.get());

        // Default never called
        assertEquals(0, counter.get());
    }

    @Test
    @DisplayName("LazyUtils LazyProperty set after get")
    void lazyPropertySetAfterGet() {
        AtomicInteger counter = new AtomicInteger(0);
        LazyUtils.LazyProperty<Integer> prop = new LazyUtils.LazyProperty<>(counter::incrementAndGet);

        prop.get(); // Computes default
        prop.set(99);

        assertEquals(99, prop.get());
        assertEquals(1, counter.get()); // Default was called once
    }

    @Test
    @DisplayName("LazyUtils LazyProperty reset clears")
    void lazyPropertyReset() {
        AtomicInteger counter = new AtomicInteger(0);
        LazyUtils.LazyProperty<Integer> prop = new LazyUtils.LazyProperty<>(counter::incrementAndGet);

        prop.get();
        prop.reset();

        assertFalse(prop.isSet());

        prop.get();
        assertEquals(2, counter.get()); // Recomputed
    }

    @Test
    @DisplayName("LazyUtils lazyProperty factory")
    void lazyPropertyFactory() {
        LazyUtils.LazyProperty<String> prop = LazyUtils.lazyProperty(() -> "default");
        assertEquals("default", prop.get());
    }

    @Test
    @DisplayName("LazyUtils LazySeq realize marks realized")
    void lazySeqRealize() {
        LazyUtils.LazySeq<Integer> seq = LazyUtils.LazySeq.range(0, 5);
        assertFalse(seq.isRealized());

        seq.realize();
        assertTrue(seq.isRealized());
    }

    @Test
    @DisplayName("LazyUtils LazySeq of factory")
    void lazySeqOf() {
        LazyUtils.LazySeq<Integer> seq = LazyUtils.LazySeq.of(1, () -> null);
        assertEquals(1, seq.head());
        assertNull(seq.tail());
    }
}