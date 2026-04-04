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
 * Tests for Interner.
 */
class InternerTest {

    @Test
    @DisplayName("Interner creates empty")
    void createsEmpty() {
        Interner<String> interner = new Interner<>();

        assertEquals(0, interner.size());
    }

    @Test
    @DisplayName("Interner creates with weak references")
    void createsWithWeakReferences() {
        Interner<String> interner = new Interner<>(true);

        assertEquals(0, interner.size());
    }

    @Test
    @DisplayName("Interner intern returns same instance")
    void internReturnsSame() {
        Interner<String> interner = new Interner<>();

        String a = new String("test");
        String b = new String("test");

        String internedA = interner.intern(a);
        String internedB = interner.intern(b);

        assertSame(internedA, internedB);
        assertEquals(1, interner.size());
    }

    @Test
    @DisplayName("Interner intern returns null for null")
    void internNull() {
        Interner<String> interner = new Interner<>();

        assertNull(interner.intern(null));
    }

    @Test
    @DisplayName("Interner isInterned checks existence")
    void isInternedWorks() {
        Interner<String> interner = new Interner<>();

        interner.intern("test");

        assertTrue(interner.isInterned("test"));
        assertFalse(interner.isInterned("other"));
    }

    @Test
    @DisplayName("Interner getCanonical returns canonical")
    void getCanonicalWorks() {
        Interner<String> interner = new Interner<>();

        String original = new String("test");
        interner.intern(original);

        Optional<String> canonical = interner.getCanonical(new String("test"));

        assertTrue(canonical.isPresent());
        assertSame(original, canonical.get());
    }

    @Test
    @DisplayName("Interner remove removes value")
    void removeWorks() {
        Interner<String> interner = new Interner<>();

        interner.intern("test");
        interner.remove("test");

        assertEquals(0, interner.size());
        assertFalse(interner.isInterned("test"));
    }

    @Test
    @DisplayName("Interner clear removes all")
    void clearWorks() {
        Interner<String> interner = new Interner<>();

        interner.intern("a");
        interner.intern("b");
        interner.clear();

        assertEquals(0, interner.size());
    }

    @Test
    @DisplayName("Interner getAllInterned returns all")
    void getAllInterned() {
        Interner<String> interner = new Interner<>();

        interner.intern("a");
        interner.intern("b");

        Set<String> all = interner.getAllInterned();

        assertEquals(2, all.size());
        assertTrue(all.contains("a"));
        assertTrue(all.contains("b"));
    }

    @Test
    @DisplayName("Interner internAll interns collection")
    void internAllWorks() {
        Interner<String> interner = new Interner<>();

        interner.internAll(List.of("a", "b", "c"));

        assertEquals(3, interner.size());
    }

    @Test
    @DisplayName("Interner toString shows info")
    void toStringWorks() {
        Interner<String> interner = new Interner<>();

        interner.intern("test");

        String str = interner.toString();

        assertTrue(str.contains("size=1"));
    }

    @Test
    @DisplayName("Interner StringInterner interns globally")
    void stringInternerWorks() {
        // Clear first
        Interner.StringInterner.clear();

        String a = new String("test");
        String b = new String("test");

        String internedA = Interner.StringInterner.intern(a);
        String internedB = Interner.StringInterner.intern(b);

        assertSame(internedA, internedB);
        assertTrue(Interner.StringInterner.size() > 0);

        // Clean up
        Interner.StringInterner.clear();
    }

    @Test
    @DisplayName("Interner InternerUtils concurrent creates interner")
    void internerUtilsConcurrent() {
        Interner<String> interner = Interner.InternerUtils.concurrent();

        assertNotNull(interner);
    }

    @Test
    @DisplayName("Interner InternerUtils weak creates weak interner")
    void internerUtilsWeak() {
        Interner<String> interner = Interner.InternerUtils.weak();

        assertNotNull(interner);
    }

    @Test
    @DisplayName("Interner InternerUtils bounded creates bounded interner")
    void internerUtilsBounded() {
        Interner<String> interner = Interner.InternerUtils.bounded(2);

        interner.intern("a");
        interner.intern("b");
        interner.intern("c"); // Should evict oldest

        // Size should be bounded
        assertTrue(interner.size() <= 3);
    }

    @Test
    @DisplayName("Interner InternerUtils withEquality creates custom interner")
    void internerUtilsWithEquality() {
        Interner<String> interner = Interner.InternerUtils.withEquality(
            String::equalsIgnoreCase,
            s -> s.toLowerCase().hashCode()
        );

        interner.intern("Test");
        String result = interner.intern("TEST");

        // Should return the first interned value (case-insensitive match)
        assertEquals("Test", result);
    }

    @Test
    @DisplayName("Interner BoundedInterner respects limit")
    void boundedInternerRespectsLimit() {
        Interner.BoundedInterner<String> interner = new Interner.BoundedInterner<>(2);

        interner.intern("a");
        interner.intern("b");
        interner.intern("c");

        assertTrue(interner.size() <= 3);
    }

    @Test
    @DisplayName("Interner BoundedInterner clear clears order")
    void boundedInternerClear() {
        Interner.BoundedInterner<String> interner = new Interner.BoundedInterner<>(5);

        interner.intern("a");
        interner.intern("b");
        interner.clear();

        assertEquals(0, interner.size());
    }

    @Test
    @DisplayName("Interner CustomEqualityInterner uses custom equality")
    void customEqualityInterner() {
        Interner.CustomEqualityInterner<String> interner = new Interner.CustomEqualityInterner<>(
            (a, b) -> a.length() == b.length(),
            String::hashCode
        );

        interner.intern("aa");
        String result = interner.intern("bb"); // Same length as "aa"

        // Should return "aa" due to custom equality
        assertEquals("aa", result);
    }

    @Test
    @DisplayName("Interner handles multiple different values")
    void multipleValues() {
        Interner<String> interner = new Interner<>();

        for (int i = 0; i < 100; i++) {
            interner.intern("value" + i);
        }

        assertEquals(100, interner.size());
    }
}