package com.akazukin.domain.service;

import com.akazukin.domain.model.LinkageScenario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LinkageScenarioProviderTest {

    private LinkageScenarioProvider provider;

    @BeforeEach
    void setUp() {
        provider = new LinkageScenarioProvider();
    }

    @Test
    void getScenarios_returnsFourScenarios() {
        List<LinkageScenario> scenarios = provider.getScenarios();

        assertEquals(4, scenarios.size());
    }

    @Test
    void getScenarios_containsLongFormExpansion() {
        List<LinkageScenario> scenarios = provider.getScenarios();

        boolean found = scenarios.stream().anyMatch(s -> s.name().equals("長文展開型"));
        assertTrue(found);
    }

    @Test
    void getScenarios_containsVisualExpansion() {
        List<LinkageScenario> scenarios = provider.getScenarios();

        boolean found = scenarios.stream().anyMatch(s -> s.name().equals("ビジュアル展開型"));
        assertTrue(found);
    }

    @Test
    void getScenarios_containsCommunityNurturing() {
        List<LinkageScenario> scenarios = provider.getScenarios();

        boolean found = scenarios.stream().anyMatch(s -> s.name().equals("コミュニティ育成型"));
        assertTrue(found);
    }

    @Test
    void getScenarios_containsEcIntegration() {
        List<LinkageScenario> scenarios = provider.getScenarios();

        boolean found = scenarios.stream().anyMatch(s -> s.name().equals("EC連動型"));
        assertTrue(found);
    }

    @Test
    void getScenarios_allScenariosHaveNameAndEnglishName() {
        List<LinkageScenario> scenarios = provider.getScenarios();

        for (LinkageScenario scenario : scenarios) {
            assertNotNull(scenario.name());
            assertNotNull(scenario.nameEn());
            assertFalse(scenario.name().isBlank());
            assertFalse(scenario.nameEn().isBlank());
        }
    }

    @Test
    void getScenarios_allScenariosHaveSteps() {
        List<LinkageScenario> scenarios = provider.getScenarios();

        for (LinkageScenario scenario : scenarios) {
            assertNotNull(scenario.steps());
            assertFalse(scenario.steps().isEmpty());
        }
    }

    @Test
    void getScenarios_eachScenarioHasThreeSteps() {
        List<LinkageScenario> scenarios = provider.getScenarios();

        for (LinkageScenario scenario : scenarios) {
            assertEquals(3, scenario.steps().size());
        }
    }

    @Test
    void getScenarios_allStepsHavePlatformAndAction() {
        List<LinkageScenario> scenarios = provider.getScenarios();

        for (LinkageScenario scenario : scenarios) {
            for (LinkageScenario.LinkageStep step : scenario.steps()) {
                assertNotNull(step.platform());
                assertNotNull(step.action());
                assertNotNull(step.actionEn());
                assertFalse(step.platform().isBlank());
                assertFalse(step.action().isBlank());
                assertFalse(step.actionEn().isBlank());
            }
        }
    }

    @Test
    void getScenarios_returnsImmutableList() {
        List<LinkageScenario> scenarios = provider.getScenarios();

        assertNotNull(scenarios);
        assertEquals(4, scenarios.size());
    }

    @Test
    void getScenarios_longFormExpansionStartsWithNote() {
        List<LinkageScenario> scenarios = provider.getScenarios();

        LinkageScenario longForm = scenarios.stream()
                .filter(s -> s.name().equals("長文展開型"))
                .findFirst()
                .orElseThrow();

        assertEquals("note", longForm.steps().get(0).platform());
    }

    @Test
    void getScenarios_visualExpansionStartsWithYouTube() {
        List<LinkageScenario> scenarios = provider.getScenarios();

        LinkageScenario visual = scenarios.stream()
                .filter(s -> s.name().equals("ビジュアル展開型"))
                .findFirst()
                .orElseThrow();

        assertEquals("YouTube", visual.steps().get(0).platform());
    }
}
