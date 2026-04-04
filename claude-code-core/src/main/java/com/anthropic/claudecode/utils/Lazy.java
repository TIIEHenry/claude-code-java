/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code lazy evaluation
 */
package com.anthropic.claudecode.utils;

import java.util.function.Supplier;

/**
 * Lazy evaluation wrapper.
 */
public class Lazy<T> implements Supplier<T> {
    private final Supplier<T> supplier;
    private volatile T cachedValue;
    private volatile boolean evaluated = false;

    public Lazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T get() {
        if (!evaluated) {
            synchronized (this) {
                if (!evaluated) {
                    cachedValue = supplier.get();
                    evaluated = true;
                }
            }
        }
        return cachedValue;
    }

    public boolean isEvaluated() {
        return evaluated;
    }

    public void reset() {
        synchronized (this) {
            evaluated = false;
            cachedValue = null;
        }
    }

    public static <T> Lazy<T> of(Supplier<T> supplier) {
        return new Lazy<>(supplier);
    }

    public static <T> Lazy<T> ofValue(T value) {
        Lazy<T> lazy = new Lazy<>(() -> value);
        lazy.cachedValue = value;
        lazy.evaluated = true;
        return lazy;
    }
}