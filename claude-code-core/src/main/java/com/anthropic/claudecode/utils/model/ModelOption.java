/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code model options
 */
package com.anthropic.claudecode.utils.model;

import java.util.*;

/**
 * Model option for picker/display.
 */
public record ModelOption(
        String value,
        String label,
        String description,
        String descriptionForModel
) {
    public ModelOption(String value, String label, String description) {
        this(value, label, description, null);
    }
}

/**
 * Model options and selection utilities.
 */
final class ModelOptions {
    private ModelOptions() {}

    /**
     * Get default option for user.
     */
    public static ModelOption getDefaultOptionForUser(boolean fastMode) {
        String userType = System.getenv("USER_TYPE");
        if ("ant".equals(userType)) {
            String currentModel = renderDefaultModelSetting(getDefaultMainLoopModelSetting());
            return new ModelOption(
                    null,
                    "Default (recommended)",
                    "Use the default model for Ants (currently " + currentModel + ")",
                    "Default model (currently " + currentModel + ")"
            );
        }

        // Default for all users
        String defaultModel = ModelStrings.getDefaultSonnetModel();
        return new ModelOption(
                null,
                "Default (recommended)",
                "Use the default model (currently " + defaultModel + ")",
                null
        );
    }

    /**
     * Get Sonnet 4.6 option.
     */
    public static ModelOption getSonnet46Option() {
        boolean is3P = !Providers.isFirstParty();
        return new ModelOption(
                is3P ? ModelStrings.SONNET_46 : "sonnet",
                "Sonnet",
                "Sonnet 4.6 · Best for everyday tasks",
                "Sonnet 4.6 - best for everyday tasks"
        );
    }

    /**
     * Get Sonnet 4.6 1M option.
     */
    public static ModelOption getSonnet46_1MOption() {
        boolean is3P = !Providers.isFirstParty();
        return new ModelOption(
                is3P ? ModelStrings.SONNET_46 + "[1m]" : "sonnet[1m]",
                "Sonnet (1M context)",
                "Sonnet 4.6 for long sessions",
                "Sonnet 4.6 with 1M context window - for long sessions"
        );
    }

    /**
     * Get Opus 4.6 option.
     */
    public static ModelOption getOpus46Option(boolean fastMode) {
        boolean is3P = !Providers.isFirstParty();
        return new ModelOption(
                is3P ? ModelStrings.OPUS_46 : "opus",
                "Opus",
                "Opus 4.6 · Most capable for complex work",
                "Opus 4.6 - most capable for complex work"
        );
    }

    /**
     * Get Opus 4.6 1M option.
     */
    public static ModelOption getOpus46_1MOption(boolean fastMode) {
        boolean is3P = !Providers.isFirstParty();
        return new ModelOption(
                is3P ? ModelStrings.OPUS_46 + "[1m]" : "opus[1m]",
                "Opus (1M context)",
                "Opus 4.6 for long sessions",
                "Opus 4.6 with 1M context window - for long sessions"
        );
    }

    /**
     * Get Haiku option.
     */
    public static ModelOption getHaikuOption() {
        return new ModelOption(
                "haiku",
                "Haiku",
                "Haiku 4.5 · Fastest for quick answers",
                "Haiku 4.5 - fastest for quick answers"
        );
    }

    /**
     * Get Opus Plan Mode option.
     */
    public static ModelOption getOpusPlanOption() {
        return new ModelOption(
                "opusplan",
                "Opus Plan Mode",
                "Use Opus 4.6 in plan mode, Sonnet 4.6 otherwise",
                null
        );
    }

    /**
     * Get all model options.
     */
    public static List<ModelOption> getModelOptions(boolean fastMode) {
        List<ModelOption> options = new ArrayList<>();

        // Add default option
        options.add(getDefaultOptionForUser(fastMode));

        // Add Sonnet options
        options.add(getSonnet46Option());
        options.add(getSonnet46_1MOption());

        // Add Opus options
        options.add(getOpus46Option(fastMode));
        options.add(getOpus46_1MOption(fastMode));

        // Add Haiku option
        options.add(getHaikuOption());

        // Add custom model option if set
        String customModel = System.getenv("ANTHROPIC_CUSTOM_MODEL_OPTION");
        if (customModel != null && !customModel.isEmpty()) {
            String name = System.getenv("ANTHROPIC_CUSTOM_MODEL_OPTION_NAME");
            String desc = System.getenv("ANTHROPIC_CUSTOM_MODEL_OPTION_DESCRIPTION");
            options.add(new ModelOption(
                    customModel,
                    name != null ? name : customModel,
                    desc != null ? desc : "Custom model (" + customModel + ")"
            ));
        }

        return options;
    }

    /**
     * Parse user-specified model.
     */
    public static String parseUserSpecifiedModel(String modelInput) {
        if (modelInput == null || modelInput.isEmpty()) {
            return ModelStrings.getDefaultSonnetModel();
        }

        String trimmed = modelInput.trim();
        String normalized = trimmed.toLowerCase();

        boolean has1m = ModelStrings.has1mContext(normalized);
        String modelString = ModelStrings.strip1mContext(normalized);

        String suffix = has1m ? "[1m]" : "";

        // Handle aliases
        switch (modelString) {
            case "opusplan":
                return ModelStrings.getDefaultSonnetModel() + suffix;
            case "sonnet":
                return ModelStrings.getDefaultSonnetModel() + suffix;
            case "haiku":
                return ModelStrings.getDefaultHaikuModel() + suffix;
            case "opus":
                return ModelStrings.getDefaultOpusModel() + suffix;
            case "best":
                return ModelStrings.getDefaultOpusModel();
            default:
                // Return original with preserved case
                return ModelStrings.strip1mContext(trimmed) + suffix;
        }
    }

    /**
     * Get default main loop model setting.
     */
    public static String getDefaultMainLoopModelSetting() {
        // Default to Sonnet for most users
        return ModelStrings.getDefaultSonnetModel();
    }

    /**
     * Render model setting for display.
     */
    public static String renderModelSetting(String setting) {
        if (setting == null) {
            return "Default (" + ModelStrings.getDefaultSonnetModel() + ")";
        }
        if ("opusplan".equals(setting)) {
            return "Opus Plan";
        }
        return renderModelName(parseUserSpecifiedModel(setting));
    }

    /**
     * Render model name for display.
     */
    public static String renderModelName(String model) {
        String publicName = ModelStrings.getPublicModelDisplayName(model);
        if (publicName != null) {
            return publicName;
        }
        return model;
    }

    /**
     * Render default model setting.
     */
    public static String renderDefaultModelSetting(String setting) {
        if ("opusplan".equals(setting)) {
            return "Opus 4.6 in plan mode, else Sonnet 4.6";
        }
        return renderModelName(parseUserSpecifiedModel(setting));
    }

    /**
     * Get main loop model.
     */
    public static String getMainLoopModel() {
        String modelOverride = System.getenv("ANTHROPIC_MODEL");
        if (modelOverride != null && !modelOverride.isEmpty()) {
            return parseUserSpecifiedModel(modelOverride);
        }
        return ModelStrings.getDefaultSonnetModel();
    }

    /**
     * Get best available model.
     */
    public static String getBestModel() {
        return ModelStrings.getDefaultOpusModel();
    }
}