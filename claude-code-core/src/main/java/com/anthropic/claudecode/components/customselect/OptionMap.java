/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code components/CustomSelect/option-map
 */
package com.anthropic.claudecode.components.customselect;

import java.util.*;
import java.util.function.*;

/**
 * Option map - Mapping utilities for select options.
 */
public final class OptionMap {
    private final Map<String, CustomSelectTypes.SelectOption<?>> idToOption = new HashMap<>();
    private final Map<Object, CustomSelectTypes.SelectOption<?>> valueToOption = new HashMap<>();

    /**
     * Create option map.
     */
    public OptionMap(List<CustomSelectTypes.SelectOption<?>> options) {
        for (CustomSelectTypes.SelectOption<?> option : options) {
            idToOption.put(option.id(), option);
            valueToOption.put(option.value(), option);
        }
    }

    /**
     * Get by ID.
     */
    public Optional<CustomSelectTypes.SelectOption<?>> getById(String id) {
        return Optional.ofNullable(idToOption.get(id));
    }

    /**
     * Get by value.
     */
    public Optional<CustomSelectTypes.SelectOption<?>> getByValue(Object value) {
        return Optional.ofNullable(valueToOption.get(value));
    }

    /**
     * Get all IDs.
     */
    public Set<String> getAllIds() {
        return Collections.unmodifiableSet(idToOption.keySet());
    }

    /**
     * Get all values.
     */
    public Set<Object> getAllValues() {
        return Collections.unmodifiableSet(valueToOption.keySet());
    }

    /**
     * Contains ID.
     */
    public boolean containsId(String id) {
        return idToOption.containsKey(id);
    }

    /**
     * Contains value.
     */
    public boolean containsValue(Object value) {
        return valueToOption.containsKey(value);
    }

    /**
     * Get size.
     */
    public int size() {
        return idToOption.size();
    }

    /**
     * Filter options.
     */
    public List<CustomSelectTypes.SelectOption<?>> filter(Predicate<CustomSelectTypes.SelectOption<?>> predicate) {
        return idToOption.values()
            .stream()
            .filter(predicate)
            .toList();
    }

    /**
     * Filter by label.
     */
    public List<CustomSelectTypes.SelectOption<?>> filterByLabel(String searchText) {
        if (searchText.isEmpty()) {
            return new ArrayList<>(idToOption.values());
        }

        String lower = searchText.toLowerCase();
        return filter(o ->
            o.label().toLowerCase().contains(lower) ||
            (o.description() != null && o.description().toLowerCase().contains(lower))
        );
    }

    /**
     * Transform options.
     */
    public <T, U> List<CustomSelectTypes.SelectOption<U>> transform(
        List<CustomSelectTypes.SelectOption<T>> options,
        Function<T, U> transformer
    ) {
        return options.stream()
            .map(o -> new CustomSelectTypes.SelectOption<U>(
                o.id(),
                o.label(),
                o.description(),
                transformer.apply(o.value()),
                o.isDisabled(),
                o.isSelected(),
                o.icon()
            ))
            .toList();
    }

    /**
     * Create from values.
     */
    public static <T> List<CustomSelectTypes.SelectOption<T>> fromValues(
        List<T> values,
        Function<T, String> labelExtractor
    ) {
        List<CustomSelectTypes.SelectOption<T>> options = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            T value = values.get(i);
            options.add(new CustomSelectTypes.SelectOption<T>(
                String.valueOf(i),
                labelExtractor.apply(value),
                null,
                value,
                false,
                false,
                null
            ));
        }
        return options;
    }

    /**
     * Create from values with ID extractor.
     */
    public static <T> List<CustomSelectTypes.SelectOption<T>> fromValues(
        List<T> values,
        Function<T, String> idExtractor,
        Function<T, String> labelExtractor
    ) {
        return values.stream()
            .map(value -> new CustomSelectTypes.SelectOption<T>(
                idExtractor.apply(value),
                labelExtractor.apply(value),
                null,
                value,
                false,
                false,
                null
            ))
            .toList();
    }

    /**
     * Group options by category.
     */
    public static <T> Map<String, List<CustomSelectTypes.SelectOption<T>>> groupByCategory(
        List<CustomSelectTypes.SelectOption<T>> options,
        Function<T, String> categoryExtractor
    ) {
        Map<String, List<CustomSelectTypes.SelectOption<T>>> groups = new HashMap<>();

        for (CustomSelectTypes.SelectOption<T> option : options) {
            String category = categoryExtractor.apply(option.value());
            groups.computeIfAbsent(category, k -> new ArrayList<>()).add(option);
        }

        return groups;
    }
}