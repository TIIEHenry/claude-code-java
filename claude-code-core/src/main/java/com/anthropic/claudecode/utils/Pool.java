/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code pool utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Object pool for resource management.
 */
public final class Pool<T> {
    private final Supplier<T> factory;
    private final Consumer<T> resetter;
    private final Consumer<T> destroyer;
    private final int maxSize;
    private final BlockingQueue<T> pool;
    private final AtomicInteger created = new AtomicInteger(0);
    private final AtomicInteger borrowed = new AtomicInteger(0);

    public Pool(Supplier<T> factory, int maxSize) {
        this(factory, null, null, maxSize);
    }

    public Pool(Supplier<T> factory, Consumer<T> resetter, Consumer<T> destroyer, int maxSize) {
        this.factory = factory;
        this.resetter = resetter;
        this.destroyer = destroyer;
        this.maxSize = maxSize;
        this.pool = new LinkedBlockingQueue<>(maxSize);
    }

    /**
     * Borrow an object from pool.
     */
    public T borrow() throws InterruptedException {
        T obj = pool.poll();
        if (obj != null) {
            borrowed.incrementAndGet();
            return obj;
        }

        if (created.get() < maxSize) {
            synchronized (this) {
                if (created.get() < maxSize) {
                    T newObj = factory.get();
                    created.incrementAndGet();
                    borrowed.incrementAndGet();
                    return newObj;
                }
            }
        }

        obj = pool.take();
        borrowed.incrementAndGet();
        return obj;
    }

    /**
     * Borrow with timeout.
     */
    public T borrow(long timeout, TimeUnit unit) throws InterruptedException {
        T obj = pool.poll();
        if (obj != null) {
            borrowed.incrementAndGet();
            return obj;
        }

        if (created.get() < maxSize) {
            synchronized (this) {
                if (created.get() < maxSize) {
                    T newObj = factory.get();
                    created.incrementAndGet();
                    borrowed.incrementAndGet();
                    return newObj;
                }
            }
        }

        obj = pool.poll(timeout, unit);
        if (obj != null) {
            borrowed.incrementAndGet();
        }
        return obj;
    }

    /**
     * Try to borrow without waiting.
     */
    public Optional<T> tryBorrow() {
        T obj = pool.poll();
        if (obj != null) {
            borrowed.incrementAndGet();
            return Optional.of(obj);
        }

        if (created.get() < maxSize) {
            synchronized (this) {
                if (created.get() < maxSize) {
                    T newObj = factory.get();
                    created.incrementAndGet();
                    borrowed.incrementAndGet();
                    return Optional.of(newObj);
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Return object to pool.
     */
    public void release(T obj) {
        if (obj == null) return;

        if (resetter != null) {
            try {
                resetter.accept(obj);
            } catch (Exception e) {
                // If reset fails, destroy the object
                destroy(obj);
                return;
            }
        }

        borrowed.decrementAndGet();
        if (!pool.offer(obj)) {
            // Pool is full, destroy
            destroy(obj);
        }
    }

    /**
     * Execute with borrowed object.
     */
    public <R> R withBorrow(Function<T, R> function) throws InterruptedException {
        T obj = borrow();
        try {
            return function.apply(obj);
        } finally {
            release(obj);
        }
    }

    /**
     * Execute with borrowed object and exception handling.
     */
    public <R> R withBorrow(Function<T, R> function, Consumer<Exception> errorHandler) {
        try {
            T obj = borrow();
            try {
                return function.apply(obj);
            } catch (Exception e) {
                errorHandler.accept(e);
                throw e;
            } finally {
                release(obj);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while borrowing from pool", e);
        }
    }

    /**
     * Destroy object.
     */
    private void destroy(T obj) {
        if (destroyer != null) {
            try {
                destroyer.accept(obj);
            } catch (Exception ignored) {}
        }
        created.decrementAndGet();
    }

    /**
     * Get pool statistics.
     */
    public PoolStats getStats() {
        return new PoolStats(
            maxSize,
            created.get(),
            borrowed.get(),
            pool.size(),
            maxSize - created.get()
        );
    }

    /**
     * Clear pool.
     */
    public void clear() {
        T obj;
        while ((obj = pool.poll()) != null) {
            destroy(obj);
        }
    }

    /**
     * Pre-warm pool.
     */
    public void warmUp(int count) {
        int toCreate = Math.min(count, maxSize - created.get());
        for (int i = 0; i < toCreate; i++) {
            T obj = factory.get();
            created.incrementAndGet();
            pool.offer(obj);
        }
    }

    /**
     * Pool statistics.
     */
    public record PoolStats(
        int maxSize,
        int created,
        int borrowed,
        int available,
        int remainingCapacity
    ) {
        public double utilization() {
            return maxSize > 0 ? (double) borrowed / maxSize : 0;
        }
    }

    /**
     * Pool builder.
     */
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    /**
     * Builder class.
     */
    public static final class Builder<T> {
        private Supplier<T> factory;
        private Consumer<T> resetter;
        private Consumer<T> destroyer;
        private int maxSize = 10;

        public Builder<T> factory(Supplier<T> factory) {
            this.factory = factory;
            return this;
        }

        public Builder<T> resetter(Consumer<T> resetter) {
            this.resetter = resetter;
            return this;
        }

        public Builder<T> destroyer(Consumer<T> destroyer) {
            this.destroyer = destroyer;
            return this;
        }

        public Builder<T> maxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Pool<T> build() {
            if (factory == null) {
                throw new IllegalStateException("Factory is required");
            }
            return new Pool<>(factory, resetter, destroyer, maxSize);
        }
    }

    // Helper
    private static final class AtomicInteger extends java.util.concurrent.atomic.AtomicInteger {
        public AtomicInteger(int value) { super(value); }
    }
}