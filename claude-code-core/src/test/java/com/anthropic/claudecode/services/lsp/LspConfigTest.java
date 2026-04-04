/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.services.lsp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for LspConfig.
 */
class LspConfigTest {

    @BeforeEach
    void setUp() {
        LspConfig.clearServers();
    }

    @Test
    @DisplayName("LspConfig ScopedLspServerConfig record")
    void scopedLspServerConfigRecord() {
        LspConfig.ScopedLspServerConfig config = new LspConfig.ScopedLspServerConfig(
            "my-plugin", "typescript-language-server",
            "typescript-language-server", List.of("--stdio"),
            Map.of("NODE_PATH", "/usr/local"), "/home/user/project",
            List.of(".ts", ".tsx"), List.of("typescript", "typescriptreact")
        );

        assertEquals("my-plugin", config.scope());
        assertEquals("typescript-language-server", config.name());
        assertEquals("typescript-language-server", config.command());
        assertEquals(1, config.args().size());
        assertEquals("--stdio", config.args().get(0));
        assertEquals("/usr/local", config.env().get("NODE_PATH"));
        assertEquals("/home/user/project", config.cwd());
        assertEquals(2, config.fileExtensions().size());
        assertEquals(2, config.languages().size());
    }

    @Test
    @DisplayName("LspConfig registerServer adds server")
    void registerServer() {
        LspConfig.ScopedLspServerConfig config = new LspConfig.ScopedLspServerConfig(
            "test", "test-server", "test-cmd", List.of(), Map.of(), null, null, null
        );

        LspConfig.registerServer("test-key", config);

        assertEquals(1, LspConfig.getServerCount());
        assertEquals(config, LspConfig.getServer("test-key"));
    }

    @Test
    @DisplayName("LspConfig unregisterServer removes server")
    void unregisterServer() {
        LspConfig.ScopedLspServerConfig config = new LspConfig.ScopedLspServerConfig(
            "test", "test-server", "test-cmd", List.of(), Map.of(), null, null, null
        );

        LspConfig.registerServer("test-key", config);
        assertEquals(1, LspConfig.getServerCount());

        LspConfig.unregisterServer("test-key");
        assertEquals(0, LspConfig.getServerCount());
        assertNull(LspConfig.getServer("test-key"));
    }

    @Test
    @DisplayName("LspConfig getServer returns null for missing key")
    void getServerMissing() {
        assertNull(LspConfig.getServer("non-existent"));
    }

    @Test
    @DisplayName("LspConfig getServerForExtension finds matching server")
    void getServerForExtension() {
        LspConfig.ScopedLspServerConfig config = new LspConfig.ScopedLspServerConfig(
            "test", "java-server", "jdtls", List.of(), Map.of(), null,
            List.of(".java"), List.of("java")
        );

        LspConfig.registerServer("java", config);

        LspConfig.ScopedLspServerConfig found = LspConfig.getServerForExtension(".java");
        assertNotNull(found);
        assertEquals("java-server", found.name());
    }

    @Test
    @DisplayName("LspConfig getServerForExtension returns null for no match")
    void getServerForExtensionNoMatch() {
        LspConfig.ScopedLspServerConfig config = new LspConfig.ScopedLspServerConfig(
            "test", "java-server", "jdtls", List.of(), Map.of(), null,
            List.of(".java"), List.of("java")
        );

        LspConfig.registerServer("java", config);

        assertNull(LspConfig.getServerForExtension(".py"));
    }

    @Test
    @DisplayName("LspConfig getServerForLanguage finds matching server")
    void getServerForLanguage() {
        LspConfig.ScopedLspServerConfig config = new LspConfig.ScopedLspServerConfig(
            "test", "python-server", "pylsp", List.of(), Map.of(), null,
            List.of(".py"), List.of("python")
        );

        LspConfig.registerServer("python", config);

        LspConfig.ScopedLspServerConfig found = LspConfig.getServerForLanguage("python");
        assertNotNull(found);
        assertEquals("python-server", found.name());
    }

    @Test
    @DisplayName("LspConfig getServerForLanguage returns null for no match")
    void getServerForLanguageNoMatch() {
        LspConfig.ScopedLspServerConfig config = new LspConfig.ScopedLspServerConfig(
            "test", "python-server", "pylsp", List.of(), Map.of(), null,
            List.of(".py"), List.of("python")
        );

        LspConfig.registerServer("python", config);

        assertNull(LspConfig.getServerForLanguage("rust"));
    }

    @Test
    @DisplayName("LspConfig clearServers removes all servers")
    void clearServers() {
        LspConfig.registerServer("key1", new LspConfig.ScopedLspServerConfig(
            "test", "server1", "cmd", List.of(), Map.of(), null, null, null
        ));
        LspConfig.registerServer("key2", new LspConfig.ScopedLspServerConfig(
            "test", "server2", "cmd", List.of(), Map.of(), null, null, null
        ));

        assertEquals(2, LspConfig.getServerCount());

        LspConfig.clearServers();
        assertEquals(0, LspConfig.getServerCount());
    }

    @Test
    @DisplayName("LspConfig getServerCount returns correct count")
    void getServerCount() {
        assertEquals(0, LspConfig.getServerCount());

        LspConfig.registerServer("key1", new LspConfig.ScopedLspServerConfig(
            "test", "server", "cmd", List.of(), Map.of(), null, null, null
        ));
        assertEquals(1, LspConfig.getServerCount());

        LspConfig.registerServer("key2", new LspConfig.ScopedLspServerConfig(
            "test", "server", "cmd", List.of(), Map.of(), null, null, null
        ));
        assertEquals(2, LspConfig.getServerCount());
    }

    @Test
    @DisplayName("LspConfig getAllLspServers returns copy")
    void getAllLspServers() {
        LspConfig.registerServer("key1", new LspConfig.ScopedLspServerConfig(
            "test", "server", "cmd", List.of(), Map.of(), null, null, null
        ));

        CompletableFuture<Map<String, LspConfig.ScopedLspServerConfig>> future = LspConfig.getAllLspServers();
        Map<String, LspConfig.ScopedLspServerConfig> servers = future.join();

        assertEquals(1, servers.size());
        assertTrue(servers.containsKey("key1"));

        // Verify it's a copy - modifying shouldn't affect internal state
        servers.clear();
        assertEquals(1, LspConfig.getServerCount());
    }
}