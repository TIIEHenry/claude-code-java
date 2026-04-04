/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code observable utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Observable pattern utilities.
 */
public final class ObservableUtils {
    private ObservableUtils() {}

    /**
     * Simple observable value.
     */
    public static class Observable<T> {
        private volatile T value;
        private final List<Consumer<T>> listeners = new CopyOnWriteArrayList<>();

        public Observable() {}

        public Observable(T initialValue) {
            this.value = initialValue;
        }

        public T get() {
            return value;
        }

        public void set(T newValue) {
            T oldValue = this.value;
            this.value = newValue;
            if (!Objects.equals(oldValue, newValue)) {
                notifyListeners(newValue);
            }
        }

        public void subscribe(Consumer<T> listener) {
            listeners.add(listener);
            // Immediately notify of current value
            listener.accept(value);
        }

        public void subscribeOnChange(Consumer<T> listener) {
            listeners.add(listener);
        }

        public void unsubscribe(Consumer<T> listener) {
            listeners.remove(listener);
        }

        public void clearListeners() {
            listeners.clear();
        }

        private void notifyListeners(T value) {
            for (Consumer<T> listener : listeners) {
                try {
                    listener.accept(value);
                } catch (Exception e) {
                    // Ignore listener errors
                }
            }
        }

        public Observable<T> map(UnaryOperator<T> mapper) {
            T newValue = mapper.apply(value);
            if (newValue != null) {
                set(newValue);
            }
            return this;
        }
    }

    /**
     * Subject that can emit values to subscribers.
     */
    public static class Subject<T> {
        private final List<Consumer<T>> subscribers = new CopyOnWriteArrayList<>();
        private volatile boolean completed = false;
        private Throwable error = null;

        public void next(T value) {
            if (completed) return;
            for (Consumer<T> subscriber : subscribers) {
                try {
                    subscriber.accept(value);
                } catch (Exception e) {
                    // Ignore
                }
            }
        }

        public void subscribe(Consumer<T> subscriber) {
            if (!completed) {
                subscribers.add(subscriber);
            }
        }

        public void unsubscribe(Consumer<T> subscriber) {
            subscribers.remove(subscriber);
        }

        public void complete() {
            completed = true;
            subscribers.clear();
        }

        public void error(Throwable error) {
            this.error = error;
            complete();
        }

        public boolean isCompleted() {
            return completed;
        }

        public Throwable getError() {
            return error;
        }
    }

    /**
     * Behavior subject (replays last value to new subscribers).
     */
    public static class BehaviorSubject<T> extends Subject<T> {
        private T currentValue;

        public BehaviorSubject(T initialValue) {
            this.currentValue = initialValue;
        }

        @Override
        public void next(T value) {
            this.currentValue = value;
            super.next(value);
        }

        @Override
        public void subscribe(Consumer<T> subscriber) {
            super.subscribe(subscriber);
            // Emit current value immediately
            if (currentValue != null) {
                subscriber.accept(currentValue);
            }
        }

        public T getValue() {
            return currentValue;
        }
    }

    /**
     * Replay subject (replays all values to new subscribers).
     */
    public static class ReplaySubject<T> extends Subject<T> {
        private final List<T> history = new CopyOnWriteArrayList<>();
        private final int bufferSize;

        public ReplaySubject() {
            this(Integer.MAX_VALUE);
        }

        public ReplaySubject(int bufferSize) {
            this.bufferSize = bufferSize;
        }

        @Override
        public void next(T value) {
            history.add(value);
            if (history.size() > bufferSize) {
                history.remove(0);
            }
            super.next(value);
        }

        @Override
        public void subscribe(Consumer<T> subscriber) {
            super.subscribe(subscriber);
            // Replay history
            for (T value : history) {
                subscriber.accept(value);
            }
        }

        public List<T> getHistory() {
            return new ArrayList<>(history);
        }
    }

    /**
     * Simple event emitter.
     */
    public static class EventEmitter<E> {
        private final Map<String, List<Consumer<Object>>> listeners = new ConcurrentHashMap<>();

        @SuppressWarnings("unchecked")
        public void on(String event, Consumer<? super E> listener) {
            listeners.computeIfAbsent(event, k -> new CopyOnWriteArrayList<>())
                    .add((Consumer<Object>) listener);
        }

        @SuppressWarnings("unchecked")
        public void once(String event, Consumer<? super E> listener) {
            Consumer<E> wrapper = new Consumer<E>() {
                @Override
                public void accept(E e) {
                    ((Consumer<Object>) listener).accept(e);
                    off(event, this);
                }
            };
            on(event, wrapper);
        }

        public void off(String event, Consumer<?> listener) {
            List<Consumer<Object>> eventListeners = listeners.get(event);
            if (eventListeners != null) {
                eventListeners.remove(listener);
            }
        }

        @SuppressWarnings("unchecked")
        public void emit(String event, E data) {
            List<Consumer<Object>> eventListeners = listeners.get(event);
            if (eventListeners != null) {
                for (Consumer<Object> listener : eventListeners) {
                    try {
                        listener.accept(data);
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        }

        public void removeAllListeners(String event) {
            listeners.remove(event);
        }

        public void removeAllListeners() {
            listeners.clear();
        }

        public int listenerCount(String event) {
            List<Consumer<Object>> eventListeners = listeners.get(event);
            return eventListeners != null ? eventListeners.size() : 0;
        }
    }

    /**
     * Computed value that recalculates when dependencies change.
     */
    public static class Computed<T> {
        private final Supplier<T> computation;
        private final List<Observable<?>> dependencies;
        private volatile T cachedValue;
        private volatile boolean dirty = true;

        @SafeVarargs
        public Computed(Supplier<T> computation, Observable<?>... dependencies) {
            this.computation = computation;
            this.dependencies = Arrays.asList(dependencies);

            for (Observable<?> dep : this.dependencies) {
                dep.subscribeOnChange(v -> dirty = true);
            }
        }

        public T get() {
            if (dirty) {
                cachedValue = computation.get();
                dirty = false;
            }
            return cachedValue;
        }

        public void invalidate() {
            dirty = true;
        }
    }

    /**
     * Create an observable.
     */
    public static <T> Observable<T> observable(T initialValue) {
        return new Observable<>(initialValue);
    }

    /**
     * Create a subject.
     */
    public static <T> Subject<T> subject() {
        return new Subject<>();
    }

    /**
     * Create a behavior subject.
     */
    public static <T> BehaviorSubject<T> behaviorSubject(T initialValue) {
        return new BehaviorSubject<>(initialValue);
    }

    /**
     * Create a replay subject.
     */
    public static <T> ReplaySubject<T> replaySubject() {
        return new ReplaySubject<>();
    }

    /**
     * Create a replay subject with buffer.
     */
    public static <T> ReplaySubject<T> replaySubject(int bufferSize) {
        return new ReplaySubject<>(bufferSize);
    }

    /**
     * Create an event emitter.
     */
    public static <E> EventEmitter<E> eventEmitter() {
        return new EventEmitter<>();
    }

    /**
     * Create a computed value.
     */
    @SafeVarargs
    public static <T> Computed<T> computed(Supplier<T> computation, Observable<?>... dependencies) {
        return new Computed<>(computation, dependencies);
    }

    /**
     * Combine multiple observables.
     */
    @SafeVarargs
    public static <T> Observable<List<T>> combine(Observable<T>... observables) {
        Observable<List<T>> combined = new Observable<>(collectValues(observables));

        for (Observable<T> obs : observables) {
            obs.subscribeOnChange(v -> combined.set(collectValues(observables)));
        }

        return combined;
    }

    @SafeVarargs
    private static <T> List<T> collectValues(Observable<T>... observables) {
        List<T> values = new ArrayList<>();
        for (Observable<T> obs : observables) {
            values.add(obs.get());
        }
        return values;
    }
}