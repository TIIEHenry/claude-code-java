/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode;

import java.util.function.Consumer;

/**
 * Callback for tool progress updates.
 */
@FunctionalInterface
public interface ToolProgressCallback<P extends ToolProgressData> {
    void accept(ToolProgress<P> progress);

    /**
     * Create a no-op callback.
     */
    static <P extends ToolProgressData> ToolProgressCallback<P> noop() {
        return progress -> {};
    }

    /**
     * Create a callback from a consumer.
     */
    static <P extends ToolProgressData> ToolProgressCallback<P> of(Consumer<ToolProgress<P>> consumer) {
        return consumer::accept;
    }
}