/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code thread utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Thread management and concurrency utilities.
 */
public final class ThreadUtils {
    private ThreadUtils() {}

    /**
     * Create a named thread factory.
     */
    public static ThreadFactory namedThreadFactory(String prefix) {
        return new NamedThreadFactory(prefix);
    }

    /**
     * Create a daemon thread factory.
     */
    public static ThreadFactory daemonThreadFactory(String prefix) {
        return new DaemonThreadFactory(prefix);
    }

    /**
     * Named thread factory implementation.
     */
    private static class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(0);
        private final String prefix;

        NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, prefix + "-" + counter.incrementAndGet());
            return thread;
        }
    }

    /**
     * Daemon thread factory implementation.
     */
    private static class DaemonThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger(0);
        private final String prefix;

        DaemonThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, prefix + "-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }

    /**
     * Create a fixed thread pool with named threads.
     */
    public static ExecutorService fixedThreadPool(int nThreads, String prefix) {
        return Executors.newFixedThreadPool(nThreads, namedThreadFactory(prefix));
    }

    /**
     * Create a cached thread pool with named threads.
     */
    public static ExecutorService cachedThreadPool(String prefix) {
        return Executors.newCachedThreadPool(namedThreadFactory(prefix));
    }

    /**
     * Create a single thread executor with named thread.
     */
    public static ExecutorService singleThreadExecutor(String name) {
        return Executors.newSingleThreadExecutor(namedThreadFactory(name));
    }

    /**
     * Create a scheduled executor with named threads.
     */
    public static ScheduledExecutorService scheduledThreadPool(int corePoolSize, String prefix) {
        return Executors.newScheduledThreadPool(corePoolSize, namedThreadFactory(prefix));
    }

    /**
     * Create a virtual thread executor (Java 21+).
     * Falls back to cached thread pool on Java 17.
     */
    public static ExecutorService virtualThreadExecutor(String prefix) {
        // Java 17 compatible: use cached thread pool with named threads
        return Executors.newCachedThreadPool(namedThreadFactory(prefix));
    }

    /**
     * Sleep silently (ignoring interruption).
     */
    public static void sleepSilently(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Sleep with interruption check.
     */
    public static boolean sleep(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Busy wait until condition is met.
     */
    public static void busyWait(BooleanSupplier condition, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            Thread.yield();
        }
    }

    /**
     * Wait for condition with polling.
     */
    public static boolean waitFor(BooleanSupplier condition, long timeoutMs, long pollIntervalMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            sleepSilently(pollIntervalMs);
        }
        return condition.getAsBoolean();
    }

    /**
     * Run with timeout.
     */
    public static <T> T runWithTimeout(Callable<T> task, long timeoutMs)
            throws TimeoutException, ExecutionException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<T> future = executor.submit(task);
            try {
                return future.get(timeoutMs, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ExecutionException(e);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Run with timeout (void version).
     */
    public static void runWithTimeout(Runnable task, long timeoutMs)
            throws TimeoutException, ExecutionException {
        runWithTimeout(() -> { task.run(); return null; }, timeoutMs);
    }

    /**
     * Execute async and return future.
     */
    public static <T> CompletableFuture<T> async(Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        });
    }

    /**
     * Execute async with executor.
     */
    public static <T> CompletableFuture<T> async(Callable<T> task, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, executor);
    }

    /**
     * Run tasks in parallel and collect results.
     */
    public static <T> List<T> parallel(Collection<Callable<T>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        try {
            List<Future<T>> futures = executor.invokeAll(tasks);
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Run tasks in parallel with timeout.
     */
    public static <T> List<T> parallel(Collection<Callable<T>> tasks, long timeoutMs) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        try {
            List<Future<T>> futures = executor.invokeAll(tasks, timeoutMs, TimeUnit.MILLISECONDS);
            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                if (!future.isCancelled()) {
                    results.add(future.get());
                }
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Race multiple tasks and return first successful result.
     */
    public static <T> T race(Collection<Callable<T>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        AtomicReference<T> result = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        List<Future<?>> futures = new ArrayList<>();
        for (Callable<T> task : tasks) {
            futures.add(executor.submit(() -> {
                try {
                    T value = task.call();
                    if (value != null) {
                        result.compareAndSet(null, value);
                        latch.countDown();
                    }
                } catch (Exception e) {
                    // Ignore individual failures
                }
            }));
        }

        latch.await();
        executor.shutdownNow();

        if (result.get() != null) {
            return result.get();
        }
        throw new ExecutionException("All tasks failed", null);
    }

    /**
     * Get current thread name.
     */
    public static String currentThreadName() {
        return Thread.currentThread().getName();
    }

    /**
     * Get current thread ID.
     */
    public static long currentThreadId() {
        return Thread.currentThread().getId();
    }

    /**
     * Check if current thread is interrupted.
     */
    public static boolean isInterrupted() {
        return Thread.currentThread().isInterrupted();
    }

    /**
     * Check if current thread is daemon.
     */
    public static boolean isDaemonThread() {
        return Thread.currentThread().isDaemon();
    }

    /**
     * Interrupt all threads with given name prefix.
     */
    public static void interruptThreads(String namePrefix) {
        Thread.getAllStackTraces().keySet().stream()
            .filter(t -> t.getName().startsWith(namePrefix))
            .forEach(Thread::interrupt);
    }

    /**
     * Graceful shutdown of executor.
     */
    public static void gracefulShutdown(ExecutorService executor, long timeoutMs) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(timeoutMs, TimeUnit.MILLISECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Run in background with daemon thread.
     */
    public static Thread runInBackground(Runnable task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /**
     * Run in background with name.
     */
    public static Thread runInBackground(Runnable task, String name) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /**
     * Boolean supplier interface.
     */
    @FunctionalInterface
    public interface BooleanSupplier {
        boolean getAsBoolean();
    }

    /**
     * Atomic reference wrapper.
     */
    private static class AtomicReference<T> extends java.util.concurrent.atomic.AtomicReference<T> {
        public AtomicReference() { super(); }
    }

    /**
     * CountDownLatch wrapper.
     */
    private static class CountDownLatch extends java.util.concurrent.CountDownLatch {
        public CountDownLatch(int count) { super(count); }
    }
}