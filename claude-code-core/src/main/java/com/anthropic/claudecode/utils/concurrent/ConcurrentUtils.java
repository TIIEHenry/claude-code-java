/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code concurrency utilities
 */
package com.anthropic.claudecode.utils.concurrent;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

/**
 * Concurrency utilities for thread-safe operations.
 */
public final class ConcurrentUtils {
    private ConcurrentUtils() {}

    /**
     * Create a thread-safe LRU cache.
     */
    public static <K, V> ConcurrentLRUCache<K, V> lruCache(int maxSize) {
        return new ConcurrentLRUCache<>(maxSize);
    }

    /**
     * Concurrent LRU cache implementation.
     */
    public static class ConcurrentLRUCache<K, V> {
        private final int maxSize;
        private final ConcurrentHashMap<K, V> cache;
        private final ConcurrentLinkedDeque<K> accessOrder;
        private final ReentrantLock evictionLock = new ReentrantLock();

        public ConcurrentLRUCache(int maxSize) {
            this.maxSize = maxSize;
            this.cache = new ConcurrentHashMap<>();
            this.accessOrder = new ConcurrentLinkedDeque<>();
        }

        public V get(K key) {
            V value = cache.get(key);
            if (value != null) {
                // Update access order
                accessOrder.remove(key);
                accessOrder.addLast(key);
            }
            return value;
        }

        public V put(K key, V value) {
            V existing = cache.put(key, value);

            if (existing == null) {
                accessOrder.addLast(key);
                evictIfNeeded();
            } else {
                // Update access order for existing key
                accessOrder.remove(key);
                accessOrder.addLast(key);
            }

            return existing;
        }

        public V computeIfAbsent(K key, java.util.function.Function<K, V> function) {
            V value = cache.get(key);
            if (value != null) {
                accessOrder.remove(key);
                accessOrder.addLast(key);
                return value;
            }

            value = function.apply(key);
            if (value != null) {
                V existing = cache.putIfAbsent(key, value);
                if (existing == null) {
                    accessOrder.addLast(key);
                    evictIfNeeded();
                } else {
                    value = existing;
                }
            }

            return value;
        }

        public V remove(K key) {
            V value = cache.remove(key);
            if (value != null) {
                accessOrder.remove(key);
            }
            return value;
        }

        public boolean containsKey(K key) {
            return cache.containsKey(key);
        }

        public int size() {
            return cache.size();
        }

        public void clear() {
            cache.clear();
            accessOrder.clear();
        }

        public Set<K> keySet() {
            return new HashSet<>(cache.keySet());
        }

        public Collection<V> values() {
            return new ArrayList<>(cache.values());
        }

        private void evictIfNeeded() {
            if (cache.size() > maxSize) {
                evictionLock.lock();
                try {
                    while (cache.size() > maxSize && !accessOrder.isEmpty()) {
                        K oldest = accessOrder.pollFirst();
                        if (oldest != null) {
                            cache.remove(oldest);
                        }
                    }
                } finally {
                    evictionLock.unlock();
                }
            }
        }
    }

    /**
     * Create a bounded executor.
     */
    public static ThreadPoolExecutor boundedExecutor(int corePoolSize, int maxPoolSize, int queueCapacity) {
        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * Create a single-threaded executor with uncaught exception handler.
     */
    public static ExecutorService singleThreadExecutor(Thread.UncaughtExceptionHandler handler) {
        ThreadFactory factory = r -> {
            Thread t = new Thread(r);
            t.setUncaughtExceptionHandler(handler);
            return t;
        };
        return Executors.newSingleThreadExecutor(factory);
    }

    /**
     * Atomic reference with version for optimistic locking.
     */
    public static class VersionedReference<T> {
        private final AtomicReference<T> value;
        private final AtomicInteger version = new AtomicInteger(0);

        public VersionedReference(T initialValue) {
            this.value = new AtomicReference<>(initialValue);
        }

        public T get() {
            return value.get();
        }

        public int getVersion() {
            return version.get();
        }

        public void set(T newValue) {
            value.set(newValue);
            version.incrementAndGet();
        }

        public boolean compareAndSet(T expectedValue, T newValue, int expectedVersion) {
            // Check version first
            if (version.get() != expectedVersion) {
                return false;
            }

            // Try to update value
            if (value.compareAndSet(expectedValue, newValue)) {
                version.incrementAndGet();
                return true;
            }

            return false;
        }
    }

    /**
     * Countdown with timeout support.
     */
    public static class TimeoutCountdown {
        private final AtomicInteger count;
        private final CountDownLatch latch;
        private final long timeoutMs;
        private final long startTime;

        public TimeoutCountdown(int count, long timeoutMs) {
            this.count = new AtomicInteger(count);
            this.latch = new CountDownLatch(count);
            this.timeoutMs = timeoutMs;
            this.startTime = System.currentTimeMillis();
        }

        public void countDown() {
            if (count.decrementAndGet() >= 0) {
                latch.countDown();
            }
        }

        public boolean await() throws InterruptedException {
            long remaining = timeoutMs - (System.currentTimeMillis() - startTime);
            if (remaining <= 0) {
                return false;
            }
            return latch.await(remaining, TimeUnit.MILLISECONDS);
        }

        public int getCount() {
            return count.get();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - startTime >= timeoutMs;
        }
    }

    /**
     * Run with lock helper.
     */
    public static <T> T withLock(Lock lock, java.util.function.Supplier<T> supplier) {
        lock.lock();
        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Run with lock helper (void).
     */
    public static void withLock(Lock lock, Runnable runnable) {
        lock.lock();
        try {
            runnable.run();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Retry with backoff.
     */
    public static <T> T retryWithBackoff(
            java.util.function.Supplier<T> operation,
            int maxRetries,
            long initialDelayMs,
            long maxDelayMs
    ) throws Exception {
        Exception lastException = null;
        long delay = initialDelayMs;

        for (int i = 0; i <= maxRetries; i++) {
            try {
                return operation.get();
            } catch (Exception e) {
                lastException = e;
                if (i < maxRetries) {
                    Thread.sleep(delay);
                    delay = Math.min(delay * 2, maxDelayMs);
                }
            }
        }

        throw lastException != null ? lastException : new RuntimeException("Retry failed");
    }

    /**
     * Debouncer for rate-limiting function calls.
     */
    public static class Debouncer {
        private final ScheduledExecutorService scheduler;
        private final long delayMs;
        private final AtomicInteger pendingCount = new AtomicInteger(0);
        private volatile ScheduledFuture<?> pendingTask;

        public Debouncer(ScheduledExecutorService scheduler, long delayMs) {
            this.scheduler = scheduler;
            this.delayMs = delayMs;
        }

        public void debounce(Runnable action) {
            pendingCount.incrementAndGet();

            if (pendingTask != null) {
                pendingTask.cancel(false);
            }

            pendingTask = scheduler.schedule(() -> {
                if (pendingCount.get() > 0) {
                    action.run();
                    pendingCount.set(0);
                }
            }, delayMs, TimeUnit.MILLISECONDS);
        }
    }
}