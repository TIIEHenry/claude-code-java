/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code dependency injection utilities
 */
package com.anthropic.claudecode.utils;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * Simple dependency injection container.
 */
public final class ServiceLocator {
    private static final ServiceLocator INSTANCE = new ServiceLocator();

    private final ConcurrentHashMap<Class<?>, Supplier<?>> factories = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Class<?>, Object> singletons = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> namedServices = new ConcurrentHashMap<>();

    private ServiceLocator() {}

    /**
     * Get the global service locator instance.
     */
    public static ServiceLocator getInstance() {
        return INSTANCE;
    }

    /**
     * Register a factory for a type.
     */
    public <T> void register(Class<T> type, Supplier<T> factory) {
        factories.put(type, factory);
    }

    /**
     * Register a singleton instance.
     */
    public <T> void registerSingleton(Class<T> type, T instance) {
        singletons.put(type, instance);
    }

    /**
     * Register a singleton with lazy initialization.
     */
    public <T> void registerSingleton(Class<T> type, Supplier<T> factory) {
        factories.put(type, () -> {
            return singletons.computeIfAbsent(type, k -> factory.get());
        });
    }

    /**
     * Register a named service.
     */
    public <T> void registerNamed(String name, T instance) {
        namedServices.put(name, instance);
    }

    /**
     * Get a service by type.
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {
        // Check singletons first
        Object singleton = singletons.get(type);
        if (singleton != null) {
            return (T) singleton;
        }

        // Check factories
        Supplier<?> factory = factories.get(type);
        if (factory != null) {
            return (T) factory.get();
        }

        // Try to create instance
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Cannot create instance of " + type.getName(), e);
        }
    }

    /**
     * Get a named service.
     */
    @SuppressWarnings("unchecked")
    public <T> T getNamed(String name, Class<T> type) {
        Object service = namedServices.get(name);
        if (service != null && type.isInstance(service)) {
            return (T) service;
        }
        return null;
    }

    /**
     * Get a named service.
     */
    public Object getNamed(String name) {
        return namedServices.get(name);
    }

    /**
     * Check if a type is registered.
     */
    public boolean isRegistered(Class<?> type) {
        return singletons.containsKey(type) || factories.containsKey(type);
    }

    /**
     * Check if a named service is registered.
     */
    public boolean isNamedRegistered(String name) {
        return namedServices.containsKey(name);
    }

    /**
     * Unregister a type.
     */
    public <T> void unregister(Class<T> type) {
        singletons.remove(type);
        factories.remove(type);
    }

    /**
     * Unregister a named service.
     */
    public void unregisterNamed(String name) {
        namedServices.remove(name);
    }

    /**
     * Clear all registrations.
     */
    public void clear() {
        singletons.clear();
        factories.clear();
        namedServices.clear();
    }

    /**
     * Get all registered types.
     */
    public Set<Class<?>> getRegisteredTypes() {
        Set<Class<?>> types = new HashSet<>();
        types.addAll(singletons.keySet());
        types.addAll(factories.keySet());
        return types;
    }

    /**
     * Get all registered names.
     */
    public Set<String> getRegisteredNames() {
        return new HashSet<>(namedServices.keySet());
    }

    /**
     * Execute an action with a service.
     */
    public <T> void withService(Class<T> type, Consumer<T> action) {
        T service = get(type);
        action.accept(service);
    }

    /**
     * Execute a function with a service.
     */
    public <T, R> R withService(Class<T> type, Function<T, R> function) {
        T service = get(type);
        return function.apply(service);
    }

    /**
     * Get multiple services.
     */
    public List<Object> getAll(Collection<Class<?>> types) {
        List<Object> services = new ArrayList<>();
        for (Class<?> type : types) {
            services.add(get(type));
        }
        return services;
    }

    /**
     * Create a child locator with inherited services.
     */
    public ServiceLocator createChild() {
        ServiceLocator child = new ServiceLocator();
        // Copy factories and singletons
        child.factories.putAll(this.factories);
        child.singletons.putAll(this.singletons);
        child.namedServices.putAll(this.namedServices);
        return child;
    }

    /**
     * Builder for service locator configuration.
     */
    public static class Builder {
        private final ServiceLocator locator = new ServiceLocator();

        public <T> Builder singleton(Class<T> type, T instance) {
            locator.registerSingleton(type, instance);
            return this;
        }

        public <T> Builder singleton(Class<T> type, Supplier<T> factory) {
            locator.registerSingleton(type, factory);
            return this;
        }

        public <T> Builder factory(Class<T> type, Supplier<T> factory) {
            locator.register(type, factory);
            return this;
        }

        public <T> Builder named(String name, T instance) {
            locator.registerNamed(name, instance);
            return this;
        }

        public ServiceLocator build() {
            return locator;
        }
    }

    /**
     * Create a builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    // Static convenience methods

    /**
     * Static get service.
     */
    public static <T> T getService(Class<T> type) {
        return INSTANCE.get(type);
    }

    /**
     * Static get named service.
     */
    public static <T> T getNamedService(String name, Class<T> type) {
        return INSTANCE.getNamed(name, type);
    }

    /**
     * Static register singleton.
     */
    public static <T> void registerService(Class<T> type, T instance) {
        INSTANCE.registerSingleton(type, instance);
    }

    /**
     * Static register factory.
     */
    public static <T> void registerServiceFactory(Class<T> type, Supplier<T> factory) {
        INSTANCE.register(type, factory);
    }
}