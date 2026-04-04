/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Cwd.
 */
class CwdTest {

    @BeforeEach
    void resetCwd() {
        Cwd.setCwd(System.getProperty("user.dir"));
    }

    @Test
    @DisplayName("Cwd getCwd returns current directory")
    void getCwd() {
        Path cwd = Cwd.getCwd();

        assertNotNull(cwd);
        assertTrue(cwd.isAbsolute());
    }

    @Test
    @DisplayName("Cwd setCwd changes directory")
    void setCwd() {
        Path original = Cwd.getCwd();
        Path newPath = Paths.get("/tmp");

        Cwd.setCwd(newPath);

        assertEquals(newPath.toAbsolutePath(), Cwd.getCwd());

        Cwd.setCwd(original); // Reset
    }

    @Test
    @DisplayName("Cwd setCwd with string works")
    void setCwdString() {
        Path original = Cwd.getCwd();

        Cwd.setCwd("/tmp");

        assertEquals(Paths.get("/tmp").toAbsolutePath(), Cwd.getCwd());

        Cwd.setCwd(original);
    }

    @Test
    @DisplayName("Cwd toAbsolute resolves relative path")
    void toAbsoluteRelative() {
        Cwd.setCwd("/home/user");

        Path absolute = Cwd.toAbsolute(Paths.get("subdir/file.txt"));

        assertEquals("/home/user/subdir/file.txt", absolute.toString());
    }

    @Test
    @DisplayName("Cwd toAbsolute keeps absolute path")
    void toAbsoluteAbsolute() {
        Path absolute = Cwd.toAbsolute(Paths.get("/absolute/path"));

        assertEquals("/absolute/path", absolute.toString());
    }

    @Test
    @DisplayName("Cwd toAbsolute with string works")
    void toAbsoluteString() {
        Cwd.setCwd("/home/user");

        Path absolute = Cwd.toAbsolute("file.txt");

        assertTrue(absolute.isAbsolute());
        assertTrue(absolute.toString().endsWith("file.txt"));
    }

    @Test
    @DisplayName("Cwd isWithinCwd checks path")
    void isWithinCwd() {
        Cwd.setCwd("/home/user");

        assertTrue(Cwd.isWithinCwd(Paths.get("/home/user/subdir")));
        assertTrue(Cwd.isWithinCwd(Paths.get("/home/user")));
        assertFalse(Cwd.isWithinCwd(Paths.get("/other/path")));
    }

    @Test
    @DisplayName("Cwd handles relative path in isWithinCwd")
    void isWithinCwdRelative() {
        // Use actual cwd since toAbsolutePath uses JVM's working directory
        Path actualCwd = Paths.get(System.getProperty("user.dir"));
        Cwd.setCwd(actualCwd);

        // Relative paths get resolved against JVM's actual cwd
        assertTrue(Cwd.isWithinCwd(Paths.get("subdir")));

        // Reset
        Cwd.setCwd(System.getProperty("user.dir"));
    }
}