/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code utils/permissions/PermissionUpdate.ts
 */
package com.anthropic.claudecode.utils.permissions;

import java.util.*;

/**
 * Permission update types and utilities.
 */
public sealed interface PermissionUpdate permits
    PermissionUpdate.SetMode,
    PermissionUpdate.AddRules,
    PermissionUpdate.ReplaceRules,
    PermissionUpdate.RemoveRules,
    PermissionUpdate.AddDirectories,
    PermissionUpdate.RemoveDirectories {

    PermissionUpdateDestination destination();

    record SetMode(String mode, PermissionUpdateDestination destination) implements PermissionUpdate {}

    record AddRules(
        List<PermissionRuleValue> rules,
        PermissionBehavior behavior,
        PermissionUpdateDestination destination
    ) implements PermissionUpdate {
        public AddRules(List<PermissionRuleValue> rules, PermissionBehavior behavior, PermissionUpdateDestination destination) {
            this.rules = rules != null ? List.copyOf(rules) : List.of();
            this.behavior = behavior;
            this.destination = destination;
        }
    }

    record ReplaceRules(
        List<PermissionRuleValue> rules,
        PermissionBehavior behavior,
        PermissionUpdateDestination destination
    ) implements PermissionUpdate {
        public ReplaceRules(List<PermissionRuleValue> rules, PermissionBehavior behavior, PermissionUpdateDestination destination) {
            this.rules = rules != null ? List.copyOf(rules) : List.of();
            this.behavior = behavior;
            this.destination = destination;
        }
    }

    record RemoveRules(
        List<PermissionRuleValue> rules,
        PermissionBehavior behavior,
        PermissionUpdateDestination destination
    ) implements PermissionUpdate {
        public RemoveRules(List<PermissionRuleValue> rules, PermissionBehavior behavior, PermissionUpdateDestination destination) {
            this.rules = rules != null ? List.copyOf(rules) : List.of();
            this.behavior = behavior;
            this.destination = destination;
        }
    }

    record AddDirectories(
        List<String> directories,
        PermissionUpdateDestination destination
    ) implements PermissionUpdate {
        public AddDirectories(List<String> directories, PermissionUpdateDestination destination) {
            this.directories = directories != null ? List.copyOf(directories) : List.of();
            this.destination = destination;
        }
    }

    record RemoveDirectories(
        List<String> directories,
        PermissionUpdateDestination destination
    ) implements PermissionUpdate {
        public RemoveDirectories(List<String> directories, PermissionUpdateDestination destination) {
            this.directories = directories != null ? List.copyOf(directories) : List.of();
            this.destination = destination;
        }
    }
}

enum PermissionUpdateDestination {
    LOCAL_SETTINGS("localSettings"),
    USER_SETTINGS("userSettings"),
    PROJECT_SETTINGS("projectSettings"),
    POLICY_SETTINGS("policySettings"),
    FLAG_SETTINGS("flagSettings"),
    CLI_ARG("cliArg"),
    SESSION("session");

    private final String id;

    PermissionUpdateDestination(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public boolean supportsPersistence() {
        return this == LOCAL_SETTINGS ||
               this == USER_SETTINGS ||
               this == PROJECT_SETTINGS;
    }

    public static PermissionUpdateDestination fromId(String id) {
        for (PermissionUpdateDestination dest : values()) {
            if (dest.id.equals(id)) {
                return dest;
            }
        }
        return SESSION;
    }
}

final class PermissionUpdates {
    private PermissionUpdates() {}

    public static ToolPermissionContext applyUpdate(
        ToolPermissionContext context,
        PermissionUpdate update
    ) {
        if (update instanceof PermissionUpdate.SetMode setMode) {
            return new ToolPermissionContext(
                setMode.mode(),
                context.alwaysAllowRules(),
                context.alwaysDenyRules(),
                context.alwaysAskRules(),
                context.additionalWorkingDirectories(),
                context.isBypassPermissionsModeAvailable(),
                context.shouldAvoidPermissionPrompts()
            );
        } else if (update instanceof PermissionUpdate.AddRules addRules) {
            return applyAddRules(context, addRules);
        } else if (update instanceof PermissionUpdate.ReplaceRules replaceRules) {
            return applyReplaceRules(context, replaceRules);
        } else if (update instanceof PermissionUpdate.RemoveRules removeRules) {
            return applyRemoveRules(context, removeRules);
        } else if (update instanceof PermissionUpdate.AddDirectories addDirs) {
            Map<String, String> newDirs = new HashMap<>(context.additionalWorkingDirectories());
            for (String dir : addDirs.directories()) {
                newDirs.put(dir, addDirs.destination().getId());
            }
            return new ToolPermissionContext(
                context.mode(),
                context.alwaysAllowRules(),
                context.alwaysDenyRules(),
                context.alwaysAskRules(),
                newDirs,
                context.isBypassPermissionsModeAvailable(),
                context.shouldAvoidPermissionPrompts()
            );
        } else if (update instanceof PermissionUpdate.RemoveDirectories removeDirs) {
            Map<String, String> newDirs = new HashMap<>(context.additionalWorkingDirectories());
            for (String dir : removeDirs.directories()) {
                newDirs.remove(dir);
            }
            return new ToolPermissionContext(
                context.mode(),
                context.alwaysAllowRules(),
                context.alwaysDenyRules(),
                context.alwaysAskRules(),
                newDirs,
                context.isBypassPermissionsModeAvailable(),
                context.shouldAvoidPermissionPrompts()
            );
        }
        return context;
    }

    private static ToolPermissionContext applyAddRules(ToolPermissionContext context, PermissionUpdate.AddRules addRules) {
        Map<String, List<String>> ruleMap = getRuleMap(context, addRules.behavior());
        Map<String, List<String>> newRuleMap = new HashMap<>(ruleMap);
        String destKey = addRules.destination().getId();
        List<String> existingRules = new ArrayList<>(newRuleMap.getOrDefault(destKey, List.of()));
        for (PermissionRuleValue rule : addRules.rules()) {
            existingRules.add(rule.toRuleString());
        }
        newRuleMap.put(destKey, existingRules);

        return createContextWithRuleMap(context, addRules.behavior(), newRuleMap);
    }

    private static ToolPermissionContext applyReplaceRules(ToolPermissionContext context, PermissionUpdate.ReplaceRules replaceRules) {
        Map<String, List<String>> ruleMap = getRuleMap(context, replaceRules.behavior());
        Map<String, List<String>> newRuleMap = new HashMap<>(ruleMap);
        String destKey = replaceRules.destination().getId();
        List<String> newRules = new ArrayList<>();
        for (PermissionRuleValue rule : replaceRules.rules()) {
            newRules.add(rule.toRuleString());
        }
        newRuleMap.put(destKey, newRules);

        return createContextWithRuleMap(context, replaceRules.behavior(), newRuleMap);
    }

    private static ToolPermissionContext applyRemoveRules(ToolPermissionContext context, PermissionUpdate.RemoveRules removeRules) {
        Map<String, List<String>> ruleMap = getRuleMap(context, removeRules.behavior());
        Map<String, List<String>> newRuleMap = new HashMap<>(ruleMap);
        String destKey = removeRules.destination().getId();
        Set<String> toRemove = new HashSet<>();
        for (PermissionRuleValue rule : removeRules.rules()) {
            toRemove.add(rule.toRuleString());
        }
        List<String> existingRules = newRuleMap.getOrDefault(destKey, List.of());
        List<String> filteredRules = existingRules.stream()
            .filter(r -> !toRemove.contains(r))
            .toList();
        newRuleMap.put(destKey, filteredRules);

        return createContextWithRuleMap(context, removeRules.behavior(), newRuleMap);
    }

    private static Map<String, List<String>> getRuleMap(ToolPermissionContext context, PermissionBehavior behavior) {
        return switch (behavior) {
            case ALLOW -> context.alwaysAllowRules();
            case DENY -> context.alwaysDenyRules();
            case ASK -> context.alwaysAskRules();
        };
    }

    private static ToolPermissionContext createContextWithRuleMap(
            ToolPermissionContext context,
            PermissionBehavior behavior,
            Map<String, List<String>> newRuleMap) {
        return switch (behavior) {
            case ALLOW -> new ToolPermissionContext(
                context.mode(),
                newRuleMap,
                context.alwaysDenyRules(),
                context.alwaysAskRules(),
                context.additionalWorkingDirectories(),
                context.isBypassPermissionsModeAvailable(),
                context.shouldAvoidPermissionPrompts()
            );
            case DENY -> new ToolPermissionContext(
                context.mode(),
                context.alwaysAllowRules(),
                newRuleMap,
                context.alwaysAskRules(),
                context.additionalWorkingDirectories(),
                context.isBypassPermissionsModeAvailable(),
                context.shouldAvoidPermissionPrompts()
            );
            case ASK -> new ToolPermissionContext(
                context.mode(),
                context.alwaysAllowRules(),
                context.alwaysDenyRules(),
                newRuleMap,
                context.additionalWorkingDirectories(),
                context.isBypassPermissionsModeAvailable(),
                context.shouldAvoidPermissionPrompts()
            );
        };
    }

    public static ToolPermissionContext applyUpdates(
        ToolPermissionContext context,
        List<PermissionUpdate> updates
    ) {
        ToolPermissionContext result = context;
        for (PermissionUpdate update : updates) {
            result = applyUpdate(result, update);
        }
        return result;
    }

    public static List<PermissionRuleValue> extractRules(List<PermissionUpdate> updates) {
        if (updates == null) return List.of();

        return updates.stream()
            .filter(u -> u instanceof PermissionUpdate.AddRules)
            .flatMap(u -> ((PermissionUpdate.AddRules) u).rules().stream())
            .toList();
    }

    public static boolean hasRules(List<PermissionUpdate> updates) {
        return !extractRules(updates).isEmpty();
    }
}