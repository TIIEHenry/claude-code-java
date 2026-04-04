/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/todo/types.ts
 */
package com.anthropic.claudecode.utils.todo;

import java.util.*;

/**
 * Todo types and utilities.
 */
public final class TodoTypes {
    private TodoTypes() {}

    /**
     * Todo status enum.
     */
    public enum TodoStatus {
        PENDING("pending"),
        IN_PROGRESS("in_progress"),
        COMPLETED("completed");

        private final String id;

        TodoStatus(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public static TodoStatus fromId(String id) {
            for (TodoStatus status : values()) {
                if (status.id.equals(id)) {
                    return status;
                }
            }
            return PENDING;
        }
    }

    /**
     * Todo item record.
     */
    public record TodoItem(
            String content,
            TodoStatus status,
            String activeForm
    ) {
        public TodoItem {
            if (content == null || content.isEmpty()) {
                throw new IllegalArgumentException("Content cannot be empty");
            }
            status = status != null ? status : TodoStatus.PENDING;
            activeForm = activeForm != null ? activeForm : content;
        }

        public static TodoItem of(String content) {
            return new TodoItem(content, TodoStatus.PENDING, content);
        }

        public static TodoItem inProgress(String content) {
            return new TodoItem(content, TodoStatus.IN_PROGRESS, content);
        }

        public static TodoItem completed(String content) {
            return new TodoItem(content, TodoStatus.COMPLETED, content);
        }

        public TodoItem withStatus(TodoStatus newStatus) {
            return new TodoItem(content, newStatus, activeForm);
        }

        public TodoItem withActiveForm(String newActiveForm) {
            return new TodoItem(content, status, newActiveForm);
        }

        public boolean isPending() {
            return status == TodoStatus.PENDING;
        }

        public boolean isInProgress() {
            return status == TodoStatus.IN_PROGRESS;
        }

        public boolean isCompleted() {
            return status == TodoStatus.COMPLETED;
        }
    }

    /**
     * Todo list utilities.
     */
    public static final class TodoList {
        private final List<TodoItem> items;

        public TodoList() {
            this.items = new ArrayList<>();
        }

        public TodoList(List<TodoItem> items) {
            this.items = new ArrayList<>(items);
        }

        public List<TodoItem> getItems() {
            return Collections.unmodifiableList(items);
        }

        public int size() {
            return items.size();
        }

        public boolean isEmpty() {
            return items.isEmpty();
        }

        public TodoList add(TodoItem item) {
            items.add(item);
            return this;
        }

        public TodoList add(String content) {
            return add(TodoItem.of(content));
        }

        public TodoList remove(int index) {
            if (index >= 0 && index < items.size()) {
                items.remove(index);
            }
            return this;
        }

        public TodoList updateStatus(int index, TodoStatus status) {
            if (index >= 0 && index < items.size()) {
                TodoItem old = items.get(index);
                items.set(index, old.withStatus(status));
            }
            return this;
        }

        public TodoList markInProgress(int index) {
            return updateStatus(index, TodoStatus.IN_PROGRESS);
        }

        public TodoList markCompleted(int index) {
            return updateStatus(index, TodoStatus.COMPLETED);
        }

        public TodoList markPending(int index) {
            return updateStatus(index, TodoStatus.PENDING);
        }

        public int indexOf(String content) {
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).content().equals(content)) {
                    return i;
                }
            }
            return -1;
        }

        public List<TodoItem> getPending() {
            return items.stream()
                    .filter(TodoItem::isPending)
                    .toList();
        }

        public List<TodoItem> getInProgress() {
            return items.stream()
                    .filter(TodoItem::isInProgress)
                    .toList();
        }

        public List<TodoItem> getCompleted() {
            return items.stream()
                    .filter(TodoItem::isCompleted)
                    .toList();
        }

        public int getPendingCount() {
            return (int) items.stream().filter(TodoItem::isPending).count();
        }

        public int getInProgressCount() {
            return (int) items.stream().filter(TodoItem::isInProgress).count();
        }

        public int getCompletedCount() {
            return (int) items.stream().filter(TodoItem::isCompleted).count();
        }

        public double getProgress() {
            if (items.isEmpty()) return 0.0;
            return (double) getCompletedCount() / items.size() * 100;
        }

        public void clear() {
            items.clear();
        }

        public static TodoList of(TodoItem... items) {
            return new TodoList(Arrays.asList(items));
        }

        public static TodoList fromStrings(String... contents) {
            TodoList list = new TodoList();
            for (String content : contents) {
                list.add(content);
            }
            return list;
        }
    }
}