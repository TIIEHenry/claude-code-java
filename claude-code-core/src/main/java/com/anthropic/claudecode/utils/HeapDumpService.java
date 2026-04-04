/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code heap dump service
 */
package com.anthropic.claudecode.utils;

import java.io.*;
import java.lang.management.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import com.sun.management.HotSpotDiagnosticMXBean;

/**
 * Service for heap dump capture and memory diagnostics.
 */
public final class HeapDumpService {
    private HeapDumpService() {}

    /**
     * Heap dump result.
     */
    public record HeapDumpResult(boolean success, String heapPath, String diagPath, String error) {}

    /**
     * Memory diagnostics.
     */
    public record MemoryDiagnostics(
            String timestamp,
            String sessionId,
            String trigger,
            int dumpNumber,
            double uptimeSeconds,
            MemoryUsageInfo memoryUsage,
            MemoryGrowthRate memoryGrowthRate,
            V8HeapStats v8HeapStats,
            ResourceUsageInfo resourceUsage,
            int activeHandles,
            int activeRequests,
            Analysis analysis,
            String platform,
            String javaVersion
    ) {}

    /**
     * Memory usage info.
     */
    public record MemoryUsageInfo(
            long heapUsed,
            long heapTotal,
            long external,
            long rss
    ) {}

    /**
     * Memory growth rate.
     */
    public record MemoryGrowthRate(
            double bytesPerSecond,
            double mbPerHour
    ) {}

    /**
     * V8-style heap stats.
     */
    public record V8HeapStats(
            long heapSizeLimit,
            long mallocedMemory,
            long peakMallocedMemory,
            int detachedContexts,
            int nativeContexts
    ) {}

    /**
     * Resource usage info.
     */
    public record ResourceUsageInfo(
            long maxRSS,
            long userCPUTime,
            long systemCPUTime
    ) {}

    /**
     * Analysis result.
     */
    public record Analysis(List<String> potentialLeaks, String recommendation) {}

    /**
     * Capture memory diagnostics.
     */
    public static MemoryDiagnostics captureMemoryDiagnostics(String trigger, int dumpNumber) {
        Runtime runtime = Runtime.getRuntime();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        double uptimeSeconds = uptimeMs / 1000.0;

        long rss = calculateRSS();
        long heapUsed = heapUsage.getUsed();
        long heapTotal = heapUsage.getCommitted();
        long heapMax = heapUsage.getMax();

        // Calculate growth rate
        double bytesPerSecond = uptimeSeconds > 0 ? rss / uptimeSeconds : 0;
        double mbPerHour = (bytesPerSecond * 3600) / (1024 * 1024);

        // Identify potential leaks
        List<String> potentialLeaks = new ArrayList<>();
        if (mbPerHour > 100) {
            potentialLeaks.add(String.format("High memory growth rate: %.1f MB/hour", mbPerHour));
        }

        // Thread count as proxy for active handles
        int threadCount = ManagementFactory.getThreadMXBean().getThreadCount();
        if (threadCount > 100) {
            potentialLeaks.add(threadCount + " active threads - possible thread leak");
        }

        String recommendation = potentialLeaks.isEmpty()
                ? "No obvious leak indicators. Check heap dump for retained objects."
                : "WARNING: " + potentialLeaks.size() + " potential leak indicator(s) found.";

        return new MemoryDiagnostics(
                Instant.now().toString(),
                UUID.randomUUID().toString().substring(0, 8),
                trigger,
                dumpNumber,
                uptimeSeconds,
                new MemoryUsageInfo(heapUsed, heapTotal, 0, rss),
                new MemoryGrowthRate(bytesPerSecond, mbPerHour),
                new V8HeapStats(heapMax, 0, 0, 0, 0),
                new ResourceUsageInfo(rss, 0, 0),
                threadCount,
                0,
                new Analysis(potentialLeaks, recommendation),
                System.getProperty("os.name"),
                System.getProperty("java.version")
        );
    }

    /**
     * Calculate RSS (Resident Set Size) for the current process.
     */
    private static long calculateRSS() {
        // Use a simple approximation based on runtime memory
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    /**
     * Perform heap dump.
     */
    public static HeapDumpResult performHeapDump(String trigger, int dumpNumber) {
        try {
            String sessionId = UUID.randomUUID().toString().substring(0, 8);

            // Capture diagnostics first
            MemoryDiagnostics diagnostics = captureMemoryDiagnostics(trigger, dumpNumber);

            // Get desktop path
            Path desktopPath = Paths.get(System.getProperty("user.home"), "Desktop");
            if (!Files.exists(desktopPath)) {
                desktopPath = Paths.get(System.getProperty("user.home"));
            }

            String suffix = dumpNumber > 0 ? "-dump" + dumpNumber : "";
            String heapFilename = sessionId + suffix + ".hprof";
            String diagFilename = sessionId + suffix + "-diagnostics.json";
            Path heapPath = desktopPath.resolve(heapFilename);
            Path diagPath = desktopPath.resolve(diagFilename);

            // Write diagnostics first
            String diagJson = SlowOperations.jsonStringify(diagnostics);
            Files.writeString(diagPath, diagJson);
            Debug.log("[HeapDump] Diagnostics written to " + diagPath);

            // Generate heap dump using HotSpotDiagnosticMXBean
            try {
                HeapDumper.dumpHeap(heapPath.toString(), true);
                Debug.log("[HeapDump] Heap dump written to " + heapPath);
            } catch (Exception e) {
                // Fallback: just log that heap dump is not available
                Debug.log("[HeapDump] Heap dump not available: " + e.getMessage());
            }

            Analytics.logEvent("tengu_heap_dump", Map.of(
                    "trigger", trigger,
                    "dumpNumber", dumpNumber,
                    "success", true
            ));

            return new HeapDumpResult(true, heapPath.toString(), diagPath.toString(), null);
        } catch (Exception e) {
            Analytics.logEvent("tengu_heap_dump", Map.of(
                    "trigger", trigger,
                    "dumpNumber", dumpNumber,
                    "success", false
            ));
            return new HeapDumpResult(false, null, null, e.getMessage());
        }
    }

    /**
     * Heap dumper utility using HotSpotDiagnosticMXBean.
     */
    private static final class HeapDumper {
        static void dumpHeap(String filePath, boolean live) throws Exception {
            for (HotSpotDiagnosticMXBean bean : ManagementFactory.getPlatformMXBeans(HotSpotDiagnosticMXBean.class)) {
                bean.dumpHeap(filePath, live);
                return;
            }
            throw new IllegalStateException("HotSpotDiagnosticMXBean not available");
        }
    }
}