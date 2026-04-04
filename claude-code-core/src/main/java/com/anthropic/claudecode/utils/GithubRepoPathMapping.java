/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code GitHub repo path mapping
 */
package com.anthropic.claudecode.utils;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * GitHub repository path mapping utilities.
 * Tracks known local paths for GitHub repositories.
 */
public final class GithubRepoPathMapping {
    private GithubRepoPathMapping() {}

    // In-memory cache of repo paths
    private static final Map<String, List<String>> repoPathsCache = new ConcurrentHashMap<>();

    /**
     * Update the GitHub repository path mapping.
     */
    public static CompletableFuture<Void> updateGithubRepoPathMapping() {
        return CompletableFuture.runAsync(() -> {
            try {
                String repo = DetectRepository.detectCurrentRepository();
                if (repo == null) {
                    Debug.log("Not in a GitHub repository, skipping path mapping update");
                    return;
                }

                Path cwd = Paths.get(System.getProperty("user.dir"));
                Path gitRoot = Git.findGitRoot(cwd);
                Path basePath = gitRoot != null ? gitRoot : cwd;

                // Resolve symlinks
                String currentPath;
                try {
                    currentPath = basePath.toRealPath().toString();
                } catch (Exception e) {
                    currentPath = basePath.toString();
                }

                // Normalize repo key to lowercase
                String repoKey = repo.toLowerCase();

                List<String> existingPaths = repoPathsCache.getOrDefault(repoKey, new ArrayList<>());

                if (!existingPaths.isEmpty() && existingPaths.get(0).equals(currentPath)) {
                    Debug.log("Path " + currentPath + " already tracked for repo " + repoKey);
                    return;
                }

                // Remove if present elsewhere, then prepend
                List<String> updatedPaths = new ArrayList<>();
                updatedPaths.add(currentPath);
                for (String path : existingPaths) {
                    if (!path.equals(currentPath)) {
                        updatedPaths.add(path);
                    }
                }

                repoPathsCache.put(repoKey, updatedPaths);
                Debug.log("Added " + currentPath + " to tracked paths for repo " + repoKey);
            } catch (Exception e) {
                Debug.log("Error updating repo path mapping: " + e.getMessage());
            }
        });
    }

    /**
     * Get known local paths for a GitHub repository.
     */
    public static List<String> getKnownPathsForRepo(String repo) {
        String repoKey = repo.toLowerCase();
        return repoPathsCache.getOrDefault(repoKey, new ArrayList<>());
    }

    /**
     * Filter paths to only those that exist.
     */
    public static CompletableFuture<List<String>> filterExistingPaths(List<String> paths) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> existing = new ArrayList<>();
            for (String path : paths) {
                if (Files.exists(Paths.get(path))) {
                    existing.add(path);
                }
            }
            return existing;
        });
    }

    /**
     * Validate that a path contains the expected GitHub repository.
     */
    public static CompletableFuture<Boolean> validateRepoAtPath(String path, String expectedRepo) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String remoteUrl = Git.getRemoteUrlForDir(path);
                if (remoteUrl == null) {
                    return false;
                }

                String actualRepo = DetectRepository.parseGitHubRepository(remoteUrl);
                if (actualRepo == null) {
                    return false;
                }

                return actualRepo.equalsIgnoreCase(expectedRepo);
            } catch (Exception e) {
                return false;
            }
        });
    }

    /**
     * Remove a path from the tracked paths for a repository.
     */
    public static void removePathFromRepo(String repo, String pathToRemove) {
        String repoKey = repo.toLowerCase();
        List<String> existingPaths = repoPathsCache.getOrDefault(repoKey, new ArrayList<>());

        List<String> updatedPaths = new ArrayList<>();
        for (String path : existingPaths) {
            if (!path.equals(pathToRemove)) {
                updatedPaths.add(path);
            }
        }

        if (updatedPaths.size() == existingPaths.size()) {
            // Path wasn't in the list
            return;
        }

        if (updatedPaths.isEmpty()) {
            repoPathsCache.remove(repoKey);
        } else {
            repoPathsCache.put(repoKey, updatedPaths);
        }

        Debug.log("Removed " + pathToRemove + " from tracked paths for repo " + repoKey);
    }

    /**
     * Clear the cache.
     */
    public static void clearCache() {
        repoPathsCache.clear();
    }
}