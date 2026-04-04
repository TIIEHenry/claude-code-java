/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code builder pattern utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.function.*;

/**
 * Builder pattern utilities.
 */
public final class BuilderUtils {
    private BuilderUtils() {}

    /**
     * Simple builder interface.
     */
    public interface Builder<T> {
        T build();
    }

    /**
     * Fluent builder base class.
     */
    public static abstract class FluentBuilder<T, B extends FluentBuilder<T, B>> {
        protected abstract B self();
        public abstract T build();

        protected Consumer<T> postBuild = null;

        public B afterBuild(Consumer<T> action) {
            this.postBuild = action;
            return self();
        }

        protected void applyPostBuild(T result) {
            if (postBuild != null) {
                postBuild.accept(result);
            }
        }
    }

    /**
     * Map builder.
     */
    public static class MapBuilder<K, V> {
        private final Map<K, V> map = new LinkedHashMap<>();

        public MapBuilder<K, V> put(K key, V value) {
            map.put(key, value);
            return this;
        }

        public MapBuilder<K, V> putAll(Map<K, V> other) {
            map.putAll(other);
            return this;
        }

        public MapBuilder<K, V> putIfPresent(K key, V value) {
            if (value != null) {
                map.put(key, value);
            }
            return this;
        }

        public MapBuilder<K, V> putIf(boolean condition, K key, V value) {
            if (condition) {
                map.put(key, value);
            }
            return this;
        }

        public Map<K, V> build() {
            return new LinkedHashMap<>(map);
        }

        public Map<K, V> buildUnmodifiable() {
            return Collections.unmodifiableMap(build());
        }
    }

    /**
     * List builder.
     */
    public static class ListBuilder<T> {
        private final List<T> list = new ArrayList<>();

        public ListBuilder<T> add(T element) {
            list.add(element);
            return this;
        }

        public ListBuilder<T> addAll(Collection<T> elements) {
            list.addAll(elements);
            return this;
        }

        public ListBuilder<T> addIf(boolean condition, T element) {
            if (condition) {
                list.add(element);
            }
            return this;
        }

        public ListBuilder<T> addIfPresent(T element) {
            if (element != null) {
                list.add(element);
            }
            return this;
        }

        public ListBuilder<T> addNullable(T element) {
            list.add(element);
            return this;
        }

        public List<T> build() {
            return new ArrayList<>(list);
        }

        public List<T> buildUnmodifiable() {
            return Collections.unmodifiableList(build());
        }
    }

    /**
     * String builder with formatting.
     */
    public static class StringBuilder {
        private final java.lang.StringBuilder sb = new java.lang.StringBuilder();

        public StringBuilder append(String str) {
            sb.append(str);
            return this;
        }

        public StringBuilder append(String format, Object... args) {
            sb.append(String.format(format, args));
            return this;
        }

        public StringBuilder appendIf(boolean condition, String str) {
            if (condition) {
                sb.append(str);
            }
            return this;
        }

        public StringBuilder appendIf(boolean condition, String format, Object... args) {
            if (condition) {
                sb.append(String.format(format, args));
            }
            return this;
        }

        public StringBuilder appendLine(String str) {
            sb.append(str).append("\n");
            return this;
        }

        public StringBuilder appendLine() {
            sb.append("\n");
            return this;
        }

        public StringBuilder appendLine(String format, Object... args) {
            sb.append(String.format(format, args)).append("\n");
            return this;
        }

        public StringBuilder prepend(String str) {
            sb.insert(0, str);
            return this;
        }

        public StringBuilder indent(String indent) {
            String content = sb.toString();
            sb.setLength(0);
            for (String line : content.split("\n")) {
                sb.append(indent).append(line).append("\n");
            }
            return this;
        }

        public StringBuilder trimTrailingNewline() {
            int len = sb.length();
            if (len > 0 && sb.charAt(len - 1) == '\n') {
                sb.setLength(len - 1);
            }
            return this;
        }

        public boolean isEmpty() {
            return sb.length() == 0;
        }

        public int length() {
            return sb.length();
        }

        @Override
        public String toString() {
            return sb.toString();
        }
    }

    /**
     * Create map builder.
     */
    public static <K, V> MapBuilder<K, V> mapBuilder() {
        return new MapBuilder<>();
    }

    /**
     * Create list builder.
     */
    public static <T> ListBuilder<T> listBuilder() {
        return new ListBuilder<>();
    }

    /**
     * Create string builder.
     */
    public static StringBuilder stringBuilder() {
        return new StringBuilder();
    }

    /**
     * Build with consumer.
     */
    public static <T> T build(Supplier<T> supplier, Consumer<T> configurator) {
        T instance = supplier.get();
        configurator.accept(instance);
        return instance;
    }

    /**
     * Build with fluent API.
     */
    public static <T, B extends Builder<T>> T buildWithBuilder(Supplier<B> builderSupplier, Consumer<B> configurator) {
        B builder = builderSupplier.get();
        configurator.accept(builder);
        return builder.build();
    }

    /**
     * Create configurable builder.
     */
    public static <T> ConfigurableBuilder<T> configurableBuilder(Supplier<T> supplier) {
        return new ConfigurableBuilder<>(supplier);
    }

    /**
     * Configurable builder.
     */
    public static class ConfigurableBuilder<T> implements Builder<T> {
        private final Supplier<T> supplier;
        private final List<Consumer<T>> configurators = new ArrayList<>();

        ConfigurableBuilder(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        public ConfigurableBuilder<T> configure(Consumer<T> configurator) {
            configurators.add(configurator);
            return this;
        }

        public ConfigurableBuilder<T> configureIf(boolean condition, Consumer<T> configurator) {
            if (condition) {
                configurators.add(configurator);
            }
            return this;
        }

        @Override
        public T build() {
            T instance = supplier.get();
            for (Consumer<T> configurator : configurators) {
                configurator.accept(instance);
            }
            return instance;
        }
    }

    /**
     * Prototype builder for cloning.
     */
    public static <T extends Cloneable> T clone(T prototype) {
        try {
            @SuppressWarnings("unchecked")
            T clone = (T) prototype.getClass().getMethod("clone").invoke(prototype);
            return clone;
        } catch (Exception e) {
            throw new RuntimeException("Failed to clone", e);
        }
    }

    /**
     * Prototype builder with modifier.
     */
    public static <T extends Cloneable> T cloneAndModify(T prototype, Consumer<T> modifier) {
        T clone = clone(prototype);
        modifier.accept(clone);
        return clone;
    }

    /**
     * Recursive builder for self-referencing structures.
     */
    public static class RecursiveBuilder<T> {
        private final List<Consumer<T>> configurators = new ArrayList<>();
        private final List<RecursiveBuilder<T>> children = new ArrayList<>();
        private final Function<List<T>, T> finalizer;

        RecursiveBuilder(Function<List<T>, T> finalizer) {
            this.finalizer = finalizer;
        }

        public RecursiveBuilder<T> configure(Consumer<T> configurator) {
            configurators.add(configurator);
            return this;
        }

        public RecursiveBuilder<T> addChild() {
            RecursiveBuilder<T> child = new RecursiveBuilder<>(finalizer);
            children.add(child);
            return child;
        }

        public T build(Supplier<T> supplier) {
            T instance = supplier.get();
            for (Consumer<T> configurator : configurators) {
                configurator.accept(instance);
            }
            List<T> childResults = children.stream()
                .map(child -> child.build(supplier))
                .toList();
            if (finalizer != null && !childResults.isEmpty()) {
                return finalizer.apply(childResults);
            }
            return instance;
        }
    }

    /**
     * Create recursive builder.
     */
    public static <T> RecursiveBuilder<T> recursiveBuilder(Function<List<T>, T> finalizer) {
        return new RecursiveBuilder<>(finalizer);
    }
}