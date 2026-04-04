/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code plan file utilities
 */
package com.anthropic.claudecode.utils.plan;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.regex.*;

/**
 * Plan file management utilities.
 */
public final class PlanUtils {
    private PlanUtils() {}

    private static final Pattern SLUG_PATTERN = Pattern.compile("[^a-zA-Z0-9-]");
    private static final int MAX_SLUG_LENGTH = 50;

    /**
     * Plan metadata.
     */
    public record PlanMetadata(
            String id,
            String title,
            String description,
            String status,
            Instant createdAt,
            Instant updatedAt,
            String branch,
            List<String> tags
    ) {}

    /**
     * Generate a slug from a title.
     */
    public static String generateSlug(String title) {
        if (title == null || title.isEmpty()) {
            return "untitled";
        }

        // Convert to lowercase and replace spaces with hyphens
        String slug = title.toLowerCase()
                .replaceAll("\\s+", "-");

        // Remove non-alphanumeric characters except hyphens
        slug = SLUG_PATTERN.matcher(slug).replaceAll("");

        // Remove consecutive hyphens
        slug = slug.replaceAll("-+", "-");

        // Remove leading/trailing hyphens
        slug = slug.replaceAll("^-+|-+$", "");

        // Truncate to max length
        if (slug.length() > MAX_SLUG_LENGTH) {
            slug = slug.substring(0, MAX_SLUG_LENGTH);
            // Remove trailing hyphen after truncation
            slug = slug.replaceAll("-$", "");
        }

        return slug.isEmpty() ? "untitled" : slug;
    }

    /**
     * Generate a plan ID from a slug.
     */
    public static String generatePlanId(String slug) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        return slug + "-" + timestamp;
    }

    /**
     * Get default plans directory.
     */
    public static Path getDefaultPlansDirectory() {
        return Paths.get(System.getProperty("user.home"), ".claude", "plans");
    }

    /**
     * Get plan file path.
     */
    public static Path getPlanFilePath(String planId) {
        return getPlanFilePath(planId, null);
    }

    /**
     * Get plan file path with custom directory.
     */
    public static Path getPlanFilePath(String planId, String customDir) {
        Path dir = customDir != null ?
                Paths.get(customDir) : getDefaultPlansDirectory();
        return dir.resolve(planId + ".md");
    }

    /**
     * Check if a plan exists.
     */
    public static boolean planExists(String planId) {
        return Files.exists(getPlanFilePath(planId));
    }

    /**
     * List all plans.
     */
    public static List<String> listPlans() {
        return listPlans(null);
    }

    /**
     * List all plans in a directory.
     */
    public static List<String> listPlans(String customDir) {
        Path dir = customDir != null ?
                Paths.get(customDir) : getDefaultPlansDirectory();

        if (!Files.isDirectory(dir)) {
            return List.of();
        }

        List<String> plans = new ArrayList<>();
        try (var stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".md"))
                    .map(p -> p.getFileName().toString().replace(".md", ""))
                    .forEach(plans::add);
        } catch (IOException e) {
            // Ignore
        }

        Collections.sort(plans, Collections.reverseOrder());
        return plans;
    }

    /**
     * Create a new plan file.
     */
    public static String createPlan(String title, String content) throws IOException {
        return createPlan(title, content, null);
    }

    /**
     * Create a new plan file with custom directory.
     */
    public static String createPlan(String title, String content, String customDir) throws IOException {
        String slug = generateSlug(title);
        String planId = generatePlanId(slug);
        Path filePath = getPlanFilePath(planId, customDir);

        Files.createDirectories(filePath.getParent());

        String fullContent = buildPlanContent(title, content);
        Files.writeString(filePath, fullContent);

        return planId;
    }

    /**
     * Build plan content with frontmatter.
     */
    private static String buildPlanContent(String title, String content) {
        StringBuilder sb = new StringBuilder();
        sb.append("---\n");
        sb.append("title: \"").append(escapeYaml(title)).append("\"\n");
        sb.append("status: draft\n");
        sb.append("created: ").append(Instant.now().toString()).append("\n");
        sb.append("---\n\n");
        sb.append(content);
        return sb.toString();
    }

    /**
     * Escape YAML string.
     */
    private static String escapeYaml(String str) {
        if (str == null) return "";
        return str.replace("\"", "\\\"");
    }

    /**
     * Read plan content.
     */
    public static String readPlan(String planId) throws IOException {
        return readPlan(planId, null);
    }

    /**
     * Read plan content with custom directory.
     */
    public static String readPlan(String planId, String customDir) throws IOException {
        Path filePath = getPlanFilePath(planId, customDir);
        return Files.readString(filePath);
    }

    /**
     * Update plan content.
     */
    public static void updatePlan(String planId, String content) throws IOException {
        updatePlan(planId, content, null);
    }

    /**
     * Update plan content with custom directory.
     */
    public static void updatePlan(String planId, String content, String customDir) throws IOException {
        Path filePath = getPlanFilePath(planId, customDir);

        // Read existing content to preserve frontmatter
        String existing = Files.readString(filePath);

        // Find end of frontmatter
        int contentStart = existing.indexOf("---", 4);
        if (contentStart > 0) {
            String frontmatter = existing.substring(0, contentStart + 3);
            String updated = frontmatter + "\n\n" + content;

            // Update timestamp in frontmatter
            String now = Instant.now().toString();
            updated = updated.replaceFirst(
                    "(updated:).*",
                    "$1 " + now);

            Files.writeString(filePath, updated);
        } else {
            Files.writeString(filePath, content);
        }
    }

    /**
     * Delete a plan.
     */
    public static boolean deletePlan(String planId) {
        return deletePlan(planId, null);
    }

    /**
     * Delete a plan with custom directory.
     */
    public static boolean deletePlan(String planId, String customDir) {
        try {
            Path filePath = getPlanFilePath(planId, customDir);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * Update plan status.
     */
    public static void updateStatus(String planId, String status) throws IOException {
        Path filePath = getPlanFilePath(planId, null);
        String content = Files.readString(filePath);

        // Update status in frontmatter
        content = content.replaceFirst(
                "(status:).*",
                "$1 " + status);

        // Update timestamp
        String now = Instant.now().toString();
        content = content.replaceFirst(
                "(updated:).*",
                "$1 " + now);

        Files.writeString(filePath, content);
    }

    /**
     * Parse plan metadata from file.
     */
    public static PlanMetadata parseMetadata(String planId) throws IOException {
        String content = readPlan(planId);
        return parseMetadata(planId, content);
    }

    /**
     * Parse plan metadata from content.
     */
    public static PlanMetadata parseMetadata(String planId, String content) {
        String title = planId;
        String status = "draft";
        Instant createdAt = Instant.now();
        Instant updatedAt = Instant.now();

        // Parse frontmatter
        if (content.startsWith("---\n")) {
            int end = content.indexOf("---\n", 4);
            if (end > 0) {
                String frontmatter = content.substring(4, end);

                for (String line : frontmatter.split("\n")) {
                    if (line.startsWith("title:")) {
                        title = line.substring(6).trim().replace("\"", "");
                    } else if (line.startsWith("status:")) {
                        status = line.substring(7).trim();
                    } else if (line.startsWith("created:")) {
                        try {
                            createdAt = Instant.parse(line.substring(8).trim());
                        } catch (Exception e) {
                            // Ignore
                        }
                    } else if (line.startsWith("updated:")) {
                        try {
                            updatedAt = Instant.parse(line.substring(8).trim());
                        } catch (Exception e) {
                            // Ignore
                        }
                    }
                }
            }
        }

        return new PlanMetadata(planId, title, null, status, createdAt, updatedAt, null, List.of());
    }
}