/*
 * Copyright 2024-2026 Anthropic. All Rights Reserved.
 */
package com.anthropic.claudecode.components.agents;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * Tests for AgentTypes.
 */
@DisplayName("AgentTypes Tests")
class AgentTypesTest {

    @Test
    @DisplayName("AgentPaths constants are correct")
    void agentPathsConstantsAreCorrect() {
        assertEquals(".claude", AgentTypes.AgentPaths.FOLDER_NAME);
        assertEquals("agents", AgentTypes.AgentPaths.AGENTS_DIR);
    }

    @Test
    @DisplayName("SettingSource enum has correct values")
    void settingSourceEnumHasCorrectValues() {
        AgentTypes.SettingSource[] sources = AgentTypes.SettingSource.values();

        assertEquals(5, sources.length);
        assertTrue(Arrays.asList(sources).contains(AgentTypes.SettingSource.USER));
        assertTrue(Arrays.asList(sources).contains(AgentTypes.SettingSource.PROJECT));
        assertTrue(Arrays.asList(sources).contains(AgentTypes.SettingSource.ENTERPRISE));
        assertTrue(Arrays.asList(sources).contains(AgentTypes.SettingSource.MANAGED));
        assertTrue(Arrays.asList(sources).contains(AgentTypes.SettingSource.PLUGIN));
    }

    @Test
    @DisplayName("MainMenuMode record works correctly")
    void mainMenuModeRecordWorksCorrectly() {
        AgentTypes.MainMenuMode mode = new AgentTypes.MainMenuMode();

        assertNotNull(mode);
        assertTrue(mode instanceof AgentTypes.ModeState);
    }

    @Test
    @DisplayName("ListAgentsMode record works correctly")
    void listAgentsModeRecordWorksCorrectly() {
        AgentTypes.ListAgentsMode mode = new AgentTypes.ListAgentsMode(AgentTypes.SettingSource.USER);

        assertEquals(AgentTypes.SettingSource.USER, mode.source());
        assertTrue(mode instanceof AgentTypes.ModeState);
    }

    @Test
    @DisplayName("AgentMenuMode record works correctly")
    void agentMenuModeRecordWorksCorrectly() {
        AgentTypes.AgentDefinition agent = createTestAgent();
        AgentTypes.MainMenuMode previous = new AgentTypes.MainMenuMode();

        AgentTypes.AgentMenuMode mode = new AgentTypes.AgentMenuMode(agent, previous);

        assertEquals(agent, mode.agent());
        assertEquals(previous, mode.previousMode());
        assertTrue(mode instanceof AgentTypes.ModeState);
    }

    @Test
    @DisplayName("ViewAgentMode record works correctly")
    void viewAgentModeRecordWorksCorrectly() {
        AgentTypes.AgentDefinition agent = createTestAgent();
        AgentTypes.ListAgentsMode previous = new AgentTypes.ListAgentsMode(AgentTypes.SettingSource.PROJECT);

        AgentTypes.ViewAgentMode mode = new AgentTypes.ViewAgentMode(agent, previous);

        assertEquals(agent, mode.agent());
        assertEquals(previous, mode.previousMode());
        assertTrue(mode instanceof AgentTypes.ModeState);
    }

    @Test
    @DisplayName("CreateAgentMode record works correctly")
    void createAgentModeRecordWorksCorrectly() {
        AgentTypes.CreateAgentMode mode = new AgentTypes.CreateAgentMode();

        assertNotNull(mode);
        assertTrue(mode instanceof AgentTypes.ModeState);
    }

    @Test
    @DisplayName("EditAgentMode record works correctly")
    void editAgentModeRecordWorksCorrectly() {
        AgentTypes.AgentDefinition agent = createTestAgent();
        AgentTypes.AgentMenuMode previous = new AgentTypes.AgentMenuMode(agent, null);

        AgentTypes.EditAgentMode mode = new AgentTypes.EditAgentMode(agent, previous);

        assertEquals(agent, mode.agent());
        assertEquals(previous, mode.previousMode());
        assertTrue(mode instanceof AgentTypes.ModeState);
    }

    @Test
    @DisplayName("DeleteConfirmMode record works correctly")
    void deleteConfirmModeRecordWorksCorrectly() {
        AgentTypes.AgentDefinition agent = createTestAgent();
        AgentTypes.ListAgentsMode previous = new AgentTypes.ListAgentsMode(AgentTypes.SettingSource.USER);

        AgentTypes.DeleteConfirmMode mode = new AgentTypes.DeleteConfirmMode(agent, previous);

        assertEquals(agent, mode.agent());
        assertEquals(previous, mode.previousMode());
        assertTrue(mode instanceof AgentTypes.ModeState);
    }

    @Test
    @DisplayName("AgentDefinition record works correctly")
    void agentDefinitionRecordWorksCorrectly() {
        AgentTypes.AgentDefinition agent = new AgentTypes.AgentDefinition(
            "test-agent",
            "Test agent description",
            "claude-opus-4-6",
            List.of("Read", "Write"),
            Map.of("custom", "config"),
            AgentTypes.SettingSource.USER,
            "/path/to/agent.md"
        );

        assertEquals("test-agent", agent.name());
        assertEquals("Test agent description", agent.description());
        assertEquals("claude-opus-4-6", agent.model());
        assertEquals(2, agent.tools().size());
        assertEquals(1, agent.config().size());
        assertEquals(AgentTypes.SettingSource.USER, agent.source());
        assertEquals("/path/to/agent.md", agent.path());
    }

    @Test
    @DisplayName("AgentValidationResult valid factory works correctly")
    void agentValidationResultValidFactoryWorksCorrectly() {
        AgentTypes.AgentValidationResult result = AgentTypes.AgentValidationResult.valid();

        assertTrue(result.isValid());
        assertTrue(result.warnings().isEmpty());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    @DisplayName("AgentValidationResult withWarnings factory works correctly")
    void agentValidationResultWithWarningsFactoryWorksCorrectly() {
        List<String> warnings = List.of("Warning 1", "Warning 2");

        AgentTypes.AgentValidationResult result = AgentTypes.AgentValidationResult.withWarnings(warnings);

        assertTrue(result.isValid());
        assertEquals(2, result.warnings().size());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    @DisplayName("AgentValidationResult withErrors factory works correctly")
    void agentValidationResultWithErrorsFactoryWorksCorrectly() {
        List<String> errors = List.of("Error 1", "Error 2");

        AgentTypes.AgentValidationResult result = AgentTypes.AgentValidationResult.withErrors(errors);

        assertFalse(result.isValid());
        assertTrue(result.warnings().isEmpty());
        assertEquals(2, result.errors().size());
    }

    @Test
    @DisplayName("AgentValidationResult record works correctly")
    void agentValidationResultRecordWorksCorrectly() {
        AgentTypes.AgentValidationResult result = new AgentTypes.AgentValidationResult(
            true,
            List.of("warning"),
            List.of("error")
        );

        assertTrue(result.isValid());
        assertEquals(1, result.warnings().size());
        assertEquals(1, result.errors().size());
    }

    @Test
    @DisplayName("ModeState sealed interface permits correct types")
    void modeStateSealedInterfacePermitsCorrectTypes() {
        // Verify all mode types implement ModeState
        AgentTypes.ModeState mainMenu = new AgentTypes.MainMenuMode();
        AgentTypes.ModeState listAgents = new AgentTypes.ListAgentsMode(AgentTypes.SettingSource.USER);
        AgentTypes.AgentDefinition agent = createTestAgent();
        AgentTypes.ModeState agentMenu = new AgentTypes.AgentMenuMode(agent, mainMenu);
        AgentTypes.ModeState viewAgent = new AgentTypes.ViewAgentMode(agent, mainMenu);
        AgentTypes.ModeState createAgent = new AgentTypes.CreateAgentMode();
        AgentTypes.ModeState editAgent = new AgentTypes.EditAgentMode(agent, mainMenu);
        AgentTypes.ModeState deleteConfirm = new AgentTypes.DeleteConfirmMode(agent, mainMenu);

        assertNotNull(mainMenu);
        assertNotNull(listAgents);
        assertNotNull(agentMenu);
        assertNotNull(viewAgent);
        assertNotNull(createAgent);
        assertNotNull(editAgent);
        assertNotNull(deleteConfirm);
    }

    private AgentTypes.AgentDefinition createTestAgent() {
        return new AgentTypes.AgentDefinition(
            "test-agent",
            "Test agent",
            "claude-sonnet-4-6",
            List.of(),
            Map.of(),
            AgentTypes.SettingSource.PROJECT,
            "/test/agent.md"
        );
    }
}