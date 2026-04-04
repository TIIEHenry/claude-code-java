/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/stream.ts
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A simple asynchronous stream implementation.
 * Allows enqueueing values that can be consumed asynchronously.
 */
public class Stream<T> implements Iterator<T> {
    private final java.util.Queue<T> queue = new ConcurrentLinkedQueue<>();
    private volatile boolean done = false;
    private volatile Throwable error = null;
    private final Runnable onReturn;

    public Stream() {
        this(null);
    }

    public Stream(Runnable onReturn) {
        this.onReturn = onReturn;
    }

    /**
     * Check if there are more elements.
     */
    @Override
    public boolean hasNext() {
        return !queue.isEmpty() || !done;
    }

    /**
     * Get the next element.
     * Blocks if no element is available but stream is not done.
     */
    @Override
    public T next() {
        // Return queued item if available
        T item = queue.poll();
        if (item != null) {
            return item;
        }

        // Check if done
        if (done) {
            throw new NoSuchElementException("Stream is done");
        }

        // Check for error
        if (error != null) {
            throw new RuntimeException("Stream error", error);
        }

        // Wait for element (simple spin-wait for now)
        while (!done && queue.isEmpty()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted", e);
            }
        }

        item = queue.poll();
        if (item != null) {
            return item;
        }

        if (error != null) {
            throw new RuntimeException("Stream error", error);
        }

        throw new NoSuchElementException("Stream is done");
    }

    /**
     * Enqueue a value.
     */
    public void enqueue(T value) {
        queue.add(value);
    }

    /**
     * Mark the stream as done.
     */
    public void done() {
        this.done = true;
    }

    /**
     * Set an error on the stream.
     */
    public void error(Throwable error) {
        this.error = error;
        this.done = true;
    }

    /**
     * Get all remaining elements as a list.
     */
    public List<T> toList() {
        List<T> result = new ArrayList<>();
        while (hasNext()) {
            T item = queue.poll();
            if (item != null) {
                result.add(item);
            } else if (done) {
                break;
            }
        }
        return result;
    }

    /**
     * Process all elements with a consumer.
     */
    public void forEach(java.util.function.Consumer<T> consumer) {
        while (hasNext()) {
            T item = queue.poll();
            if (item != null) {
                consumer.accept(item);
            } else if (done) {
                break;
            } else {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * Create an async iterable from this stream.
     */
    public Iterable<T> asIterable() {
        return () -> new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return !queue.isEmpty() || !done;
            }

            @Override
            public T next() {
                T item = queue.poll();
                if (item != null) {
                    return item;
                }
                if (done) {
                    throw new NoSuchElementException();
                }
                throw new IllegalStateException("No element available");
            }
        };
    }
}