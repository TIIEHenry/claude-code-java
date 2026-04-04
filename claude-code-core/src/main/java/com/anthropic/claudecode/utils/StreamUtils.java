/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code stream utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * Reactive stream utilities.
 */
public final class StreamUtils {
    private StreamUtils() {}

    /**
     * Simple reactive stream publisher interface.
     */
    public interface Publisher<T> {
        void subscribe(Subscriber<T> subscriber);
        void publish(T item);
        void complete();
        void error(Throwable error);
    }

    /**
     * Default publisher implementation.
     */
    public static class DefaultPublisher<T> implements Publisher<T> {
        private final List<Subscriber<T>> subscribers = new CopyOnWriteArrayList<>();

        @Override
        public void subscribe(Subscriber<T> subscriber) {
            subscribers.add(subscriber);
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    // Simple implementation - no backpressure
                }

                @Override
                public void cancel() {
                    subscribers.remove(subscriber);
                }
            });
        }

        @Override
        public void publish(T item) {
            for (Subscriber<T> sub : subscribers) {
                sub.onNext(item);
            }
        }

        @Override
        public void complete() {
            for (Subscriber<T> sub : subscribers) {
                sub.onComplete();
            }
            subscribers.clear();
        }

        @Override
        public void error(Throwable error) {
            for (Subscriber<T> sub : subscribers) {
                sub.onError(error);
            }
            subscribers.clear();
        }
    }

    /**
     * Subscriber interface.
     */
    public interface Subscriber<T> {
        void onSubscribe(Subscription subscription);
        void onNext(T item);
        void onError(Throwable error);
        void onComplete();
    }

    /**
     * Subscription interface.
     */
    public interface Subscription {
        void request(long n);
        void cancel();
    }

    /**
     * Processor - both publisher and subscriber.
     */
    public static class Processor<T, R> implements Subscriber<T>, Publisher<R> {
        private final Function<T, R> transform;
        private final List<Subscriber<R>> subscribers = new CopyOnWriteArrayList<>();
        private Subscription upstream;

        public Processor(Function<T, R> transform) {
            this.transform = transform;
        }

        @Override
        public void subscribe(Subscriber<R> subscriber) {
            subscribers.add(subscriber);
            subscriber.onSubscribe(new Subscription() {
                @Override
                public void request(long n) {
                    if (upstream != null) upstream.request(n);
                }

                @Override
                public void cancel() {
                    subscribers.remove(subscriber);
                }
            });
        }

        @Override
        public void onSubscribe(Subscription subscription) {
            this.upstream = subscription;
        }

        @Override
        public void onNext(T item) {
            R result = transform.apply(item);
            for (Subscriber<R> sub : subscribers) {
                sub.onNext(result);
            }
        }

        @Override
        public void onError(Throwable error) {
            for (Subscriber<R> sub : subscribers) {
                sub.onError(error);
            }
            subscribers.clear();
        }

        @Override
        public void onComplete() {
            for (Subscriber<R> sub : subscribers) {
                sub.onComplete();
            }
            subscribers.clear();
        }

        @Override
        public void publish(R item) {
            for (Subscriber<R> sub : subscribers) {
                sub.onNext(item);
            }
        }

        @Override
        public void complete() {
            onComplete();
        }

        @Override
        public void error(Throwable error) {
            onError(error);
        }
    }

    /**
     * Reactive sink for building streams.
     */
    public static class Sink<T> {
        private final Publisher<T> publisher = new DefaultPublisher<>();
        private volatile boolean completed = false;

        public void next(T item) {
            if (!completed) {
                publisher.publish(item);
            }
        }

        public void complete() {
            completed = true;
            publisher.complete();
        }

        public void error(Throwable error) {
            completed = true;
            publisher.error(error);
        }

        public Publisher<T> asPublisher() {
            return publisher;
        }

        public boolean isCompleted() {
            return completed;
        }
    }

    /**
     * Create a sink.
     */
    public static <T> Sink<T> sink() {
        return new Sink<>();
    }

    /**
     * Create a publisher from iterable.
     */
    public static <T> Publisher<T> fromIterable(Iterable<T> iterable) {
        DefaultPublisher<T> publisher = new DefaultPublisher<>();
        CompletableFuture.runAsync(() -> {
            try {
                for (T item : iterable) {
                    publisher.publish(item);
                }
                publisher.complete();
            } catch (Exception e) {
                publisher.error(e);
            }
        });
        return publisher;
    }

    /**
     * Create a publisher from stream.
     */
    public static <T> Publisher<T> fromStream(Stream<T> stream) {
        DefaultPublisher<T> publisher = new DefaultPublisher<>();
        CompletableFuture.runAsync(() -> {
            try {
                stream.forEach(publisher::publish);
                publisher.complete();
            } catch (Exception e) {
                publisher.error(e);
            }
        });
        return publisher;
    }

    /**
     * Create a publisher that emits a single value.
     */
    public static <T> Publisher<T> just(T value) {
        DefaultPublisher<T> publisher = new DefaultPublisher<>();
        CompletableFuture.runAsync(() -> {
            publisher.publish(value);
            publisher.complete();
        });
        return publisher;
    }

    /**
     * Create a publisher that emits multiple values.
     */
    @SafeVarargs
    public static <T> Publisher<T> just(T... values) {
        return fromIterable(Arrays.asList(values));
    }

    /**
     * Create an empty publisher.
     */
    public static <T> Publisher<T> empty() {
        DefaultPublisher<T> publisher = new DefaultPublisher<>();
        publisher.complete();
        return publisher;
    }

    /**
     * Create an error publisher.
     */
    public static <T> Publisher<T> error(Throwable error) {
        DefaultPublisher<T> publisher = new DefaultPublisher<>();
        publisher.error(error);
        return publisher;
    }

    /**
     * Create a publisher that never completes.
     */
    public static <T> Publisher<T> never() {
        return new DefaultPublisher<>();
    }

    /**
     * Interval publisher.
     */
    public static Publisher<Long> interval(long periodMs) {
        DefaultPublisher<Long> publisher = new DefaultPublisher<>();
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        AtomicLong counter = new AtomicLong(0);

        executor.scheduleAtFixedRate(() -> {
            publisher.publish(counter.getAndIncrement());
        }, 0, periodMs, TimeUnit.MILLISECONDS);

        return publisher;
    }

    /**
     * Range publisher.
     */
    public static Publisher<Integer> range(int start, int end) {
        DefaultPublisher<Integer> publisher = new DefaultPublisher<>();
        CompletableFuture.runAsync(() -> {
            for (int i = start; i < end; i++) {
                publisher.publish(i);
            }
            publisher.complete();
        });
        return publisher;
    }

    /**
     * Collect publisher items to list.
     */
    public static <T> CompletableFuture<List<T>> collectList(Publisher<T> publisher) {
        List<T> items = Collections.synchronizedList(new ArrayList<>());
        CompletableFuture<List<T>> future = new CompletableFuture<>();

        publisher.subscribe(new Subscriber<T>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(T item) {
                items.add(item);
            }

            @Override
            public void onError(Throwable error) {
                future.completeExceptionally(error);
            }

            @Override
            public void onComplete() {
                future.complete(new ArrayList<>(items));
            }
        });

        return future;
    }

    /**
     * Take first N items.
     */
    public static <T> Publisher<T> take(Publisher<T> source, long n) {
        Publisher<T> result = new DefaultPublisher<>();
        AtomicLong counter = new AtomicLong(0);

        source.subscribe(new Subscriber<T>() {
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription s) {
                subscription = s;
                s.request(n);
            }

            @Override
            public void onNext(T item) {
                if (counter.getAndIncrement() < n) {
                    result.publish(item);
                    if (counter.get() >= n) {
                        subscription.cancel();
                        result.complete();
                    }
                }
            }

            @Override
            public void onError(Throwable error) {
                result.error(error);
            }

            @Override
            public void onComplete() {
                result.complete();
            }
        });

        return result;
    }

    /**
     * Filter items.
     */
    public static <T> Publisher<T> filter(Publisher<T> source, Predicate<T> predicate) {
        Processor<T, T> processor = new Processor<>(t -> predicate.test(t) ? t : null);
        source.subscribe(new Subscriber<T>() {
            @Override
            public void onSubscribe(Subscription s) {
                processor.upstream = s;
            }

            @Override
            public void onNext(T item) {
                if (predicate.test(item)) {
                    for (Subscriber<T> sub : processor.subscribers) {
                        sub.onNext(item);
                    }
                }
            }

            @Override
            public void onError(Throwable error) {
                processor.error(error);
            }

            @Override
            public void onComplete() {
                processor.complete();
            }
        });
        return processor;
    }

    /**
     * Map items.
     */
    public static <T, R> Publisher<R> map(Publisher<T> source, Function<T, R> mapper) {
        return new Processor<T, R>(mapper) {{
            source.subscribe(this);
        }};
    }

    /**
     * Flat map items.
     */
    public static <T, R> Publisher<R> flatMap(Publisher<T> source, Function<T, Publisher<R>> mapper) {
        DefaultPublisher<R> result = new DefaultPublisher<>();
        AtomicInteger active = new AtomicInteger(1);

        source.subscribe(new Subscriber<T>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(T item) {
                active.incrementAndGet();
                Publisher<R> inner = mapper.apply(item);
                inner.subscribe(new Subscriber<R>() {
                    @Override
                    public void onSubscribe(Subscription s) {
                        s.request(Long.MAX_VALUE);
                    }

                    @Override
                    public void onNext(R r) {
                        result.publish(r);
                    }

                    @Override
                    public void onError(Throwable error) {
                        result.error(error);
                    }

                    @Override
                    public void onComplete() {
                        if (active.decrementAndGet() == 0) {
                            result.complete();
                        }
                    }
                });
            }

            @Override
            public void onError(Throwable error) {
                result.error(error);
            }

            @Override
            public void onComplete() {
                if (active.decrementAndGet() == 0) {
                    result.complete();
                }
            }
        });

        return result;
    }

    /**
     * Debounce items.
     */
    public static <T> Publisher<T> debounce(Publisher<T> source, long timeoutMs) {
        Publisher<T> result = new DefaultPublisher<>();
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        AtomicReference<ScheduledFuture<?>> lastTask = new AtomicReference<>();

        source.subscribe(new Subscriber<T>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(T item) {
                ScheduledFuture<?> prev = lastTask.getAndSet(
                    executor.schedule(() -> result.publish(item), timeoutMs, TimeUnit.MILLISECONDS)
                );
                if (prev != null) {
                    prev.cancel(false);
                }
            }

            @Override
            public void onError(Throwable error) {
                result.error(error);
                executor.shutdown();
            }

            @Override
            public void onComplete() {
                executor.shutdown();
                try {
                    executor.awaitTermination(timeoutMs + 100, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                result.complete();
            }
        });

        return result;
    }

    /**
     * Throttle items.
     */
    public static <T> Publisher<T> throttle(Publisher<T> source, long windowMs) {
        Publisher<T> result = new DefaultPublisher<>();
        AtomicLong lastEmit = new AtomicLong(0);

        source.subscribe(new Subscriber<T>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(T item) {
                long now = System.currentTimeMillis();
                long last = lastEmit.get();
                if (now - last >= windowMs) {
                    if (lastEmit.compareAndSet(last, now)) {
                        result.publish(item);
                    }
                }
            }

            @Override
            public void onError(Throwable error) {
                result.error(error);
            }

            @Override
            public void onComplete() {
                result.complete();
            }
        });

        return result;
    }

    /**
     * Buffer items.
     */
    public static <T> Publisher<List<T>> buffer(Publisher<T> source, int size) {
        DefaultPublisher<List<T>> result = new DefaultPublisher<>();
        List<T> buffer = Collections.synchronizedList(new ArrayList<>());

        source.subscribe(new Subscriber<T>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(T item) {
                buffer.add(item);
                if (buffer.size() >= size) {
                    result.publish(new ArrayList<>(buffer));
                    buffer.clear();
                }
            }

            @Override
            public void onError(Throwable error) {
                result.error(error);
            }

            @Override
            public void onComplete() {
                if (!buffer.isEmpty()) {
                    result.publish(new ArrayList<>(buffer));
                }
                result.complete();
            }
        });

        return result;
    }

    // Helper classes
    private static class AtomicLong extends java.util.concurrent.atomic.AtomicLong {
        public AtomicLong(long initialValue) { super(initialValue); }
    }

    private static class AtomicInteger extends java.util.concurrent.atomic.AtomicInteger {
        public AtomicInteger(int initialValue) { super(initialValue); }
    }

    private static class AtomicReference<T> extends java.util.concurrent.atomic.AtomicReference<T> {
        public AtomicReference() { super(); }
    }
}