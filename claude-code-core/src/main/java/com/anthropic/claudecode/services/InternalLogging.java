/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code services/internalLogging.ts
 */
package com.anthropic.claudecode.services;

import com.anthropic.claudecode.services.analytics.AnalyticsMetadata;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import com.anthropic.claudecode.utils.EnvUtils;

/**
 * Internal logging utilities for Ant (internal) users.
 * Provides Kubernetes namespace and container ID detection.
 */
public final class InternalLogging {
    private InternalLogging() {}

    private static volatile String cachedNamespace = null;
    private static volatile String cachedContainerId = null;
    private static volatile boolean namespaceLoaded = false;
    private static volatile boolean containerIdLoaded = false;

    /**
     * Get the current Kubernetes namespace.
     * Returns null on laptops/local development.
     */
    public static CompletableFuture<String> getKubernetesNamespace() {
        if (!EnvUtils.isUserTypeAnt()) {
            return CompletableFuture.completedFuture(null);
        }
        if (namespaceLoaded) {
            return CompletableFuture.completedFuture(cachedNamespace);
        }
        return CompletableFuture.supplyAsync(() -> {
            String namespacePath = "/var/run/secrets/kubernetes.io/serviceaccount/namespace";
            try {
                String content = Files.readString(Paths.get(namespacePath)).trim();
                cachedNamespace = content;
                namespaceLoaded = true;
                return content;
            } catch (Exception e) {
                cachedNamespace = "namespace not found";
                namespaceLoaded = true;
                return cachedNamespace;
            }
        });
    }

    /**
     * Get the OCI container ID from within a running container.
     * Pattern matches both Docker and containerd/CRI-O container IDs.
     */
    public static CompletableFuture<String> getContainerId() {
        if (!EnvUtils.isUserTypeAnt()) {
            return CompletableFuture.completedFuture(null);
        }
        if (containerIdLoaded) {
            return CompletableFuture.completedFuture(cachedContainerId);
        }
        return CompletableFuture.supplyAsync(() -> {
            String mountinfoPath = "/proc/self/mountinfo";
            try {
                String mountinfo = Files.readString(Paths.get(mountinfoPath)).trim();
                // Pattern to match both Docker and containerd/CRI-O container IDs
                // Docker: /docker/containers/[64-char-hex]
                // Containerd: /sandboxes/[64-char-hex]
                java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "(?:/docker/containers/|/sandboxes/)([0-9a-f]{64})"
                );
                String[] lines = mountinfo.split("\n");
                for (String line : lines) {
                    java.util.regex.Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        cachedContainerId = matcher.group(1);
                        containerIdLoaded = true;
                        return cachedContainerId;
                    }
                }
                cachedContainerId = "container ID not found in mountinfo";
                containerIdLoaded = true;
                return cachedContainerId;
            } catch (Exception e) {
                cachedContainerId = "container ID not found";
                containerIdLoaded = true;
                return cachedContainerId;
            }
        });
    }

    /**
     * Logs an event with the current namespace and tool permission context.
     */
    public static void logPermissionContextForAnts(
        Object toolPermissionContext,
        String moment
    ) {
        if (!EnvUtils.isUserTypeAnt()) {
            return;
        }
        // Get namespace and container ID asynchronously
        getKubernetesNamespace().thenAccept(namespace -> {
            getContainerId().thenAccept(containerId -> {
                AnalyticsMetadata.logEvent("tengu_internal_record_permission_context", Map.of(
                    "moment", moment,
                    "namespace", namespace != null ? namespace : "unknown",
                    "container_id", containerId != null ? containerId : "unknown"
                ), true);
            });
        });
    }

    /**
     * Log an internal analytics event.
     */
    public static void logEvent(String eventName, Map<String, Object> data) {
        if (!EnvUtils.isUserTypeAnt()) {
            return;
        }
        AnalyticsMetadata.logEvent(eventName, data, true);
    }
}