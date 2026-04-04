/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/lazySchema
 */
package com.anthropic.claudecode.utils;

import java.util.function.*;

/**
 * Lazy schema - Memoized factory that constructs value on first call.
 *
 * Used to defer schema construction from module init time to first access.
 */
public final class LazySchema {
    /**
     * Create a lazy schema factory.
     */
    public static <T> Supplier<T> lazySchema(Supplier<T> factory) {
        return new LazySchemaFactory<>(factory);
    }

    /**
     * Lazy schema factory implementation.
     */
    private static final class LazySchemaFactory<T> implements Supplier<T> {
        private final Supplier<T> factory;
        private volatile T cached;
        private volatile boolean initialized = false;
        private final Object lock = new Object();

        LazySchemaFactory(Supplier<T> factory) {
            this.factory = factory;
        }

        @Override
        public T get() {
            if (!initialized) {
                synchronized (lock) {
                    if (!initialized) {
                        cached = factory.get();
                        initialized = true;
                    }
                }
            }
            return cached;
        }
    }
}