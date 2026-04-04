/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 * Java port of Claude Code constants/files.ts
 */
package com.anthropic.claudecode.constants;

/**
 * File-related constants.
 */
public final class Files {
    private Files() {}

    // Config files
    public static final String CLAUDE_MD = "CLAUDE.md";
    public static final String CLAUDE_MD_LOCAL = "CLAUDE.local.md";
    public static final String CONFIG_JSON = "config.json";
    public static final String SETTINGS_JSON = "settings.json";
    public static final String PACKAGE_JSON = "package.json";

    // Directories
    public static final String CLAUDE_DIR = ".claude";
    public static final String GIT_DIR = ".git";
    public static final String NODE_MODULES = "node_modules";
    public static final String SKILLS_DIR = "skills";
    public static final String PLUGINS_DIR = "plugins";

    // Extensions
    public static final String[] TEXT_EXTENSIONS = {
        ".txt", ".md", ".json", ".xml", ".yaml", ".yml",
        ".js", ".ts", ".jsx", ".tsx", ".py", ".java", ".go", ".rs",
        ".c", ".cpp", ".h", ".hpp", ".cs", ".rb", ".php", ".swift",
        ".kt", ".scala", ".sh", ".bash", ".zsh", ".fish",
        ".css", ".scss", ".less", ".html", ".svg",
        ".sql", ".graphql", ".proto", ".toml", ".ini", ".cfg",
        ".env", ".gitignore", ".dockerignore", ".editorconfig"
    };

    // Binary extensions
    public static final String[] BINARY_EXTENSIONS = {
        ".exe", ".dll", ".so", ".dylib",
        ".zip", ".tar", ".gz", ".rar", ".7z",
        ".pdf", ".doc", ".docx", ".xls", ".xlsx",
        ".png", ".jpg", ".jpeg", ".gif", ".bmp", ".ico",
        ".mp3", ".mp4", ".wav", ".avi", ".mov",
        ".db", ".sqlite", ".sqlite3"
    };

    // Ignored directories
    public static final String[] IGNORED_DIRS = {
        "node_modules", ".git", ".svn", ".hg",
        "target", "build", "dist", "out",
        "__pycache__", ".pytest_cache", ".mypy_cache",
        ".idea", ".vscode", ".settings",
        "venv", ".venv", "env", ".env"
    };

    // Hidden files
    public static final String[] HIDDEN_FILES = {
        ".env", ".env.local", ".env.production",
        ".gitignore", ".dockerignore",
        ".eslintrc", ".prettierrc", ".editorconfig"
    };

    /**
     * Check if extension is text.
     */
    public static boolean isTextExtension(String ext) {
        String lower = ext.toLowerCase();
        for (String textExt : TEXT_EXTENSIONS) {
            if (textExt.equals(lower)) return true;
        }
        return false;
    }

    /**
     * Check if extension is binary.
     */
    public static boolean isBinaryExtension(String ext) {
        String lower = ext.toLowerCase();
        for (String binExt : BINARY_EXTENSIONS) {
            if (binExt.equals(lower)) return true;
        }
        return false;
    }

    /**
     * Check if directory should be ignored.
     */
    public static boolean isIgnoredDir(String name) {
        for (String ignored : IGNORED_DIRS) {
            if (ignored.equals(name)) return true;
        }
        return false;
    }
}