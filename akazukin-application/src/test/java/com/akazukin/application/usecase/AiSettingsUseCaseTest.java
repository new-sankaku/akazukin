package com.akazukin.application.usecase;

import com.akazukin.application.dto.AiCostMonitorDto;
import com.akazukin.application.dto.AiOllamaStatusDto;
import com.akazukin.application.dto.AiTaskProviderSettingDto;
import com.akazukin.application.dto.AiTaskProviderSettingsRequestDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.AiModelProvider;
import com.akazukin.domain.model.AiTaskProviderSetting;
import com.akazukin.domain.model.AiTaskType;
import com.akazukin.domain.port.AiInfrastructurePort;
import com.akazukin.domain.port.AiTaskProviderSettingRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiSettingsUseCaseTest {

    private StubAiInfrastructurePort infrastructurePort;
    private InMemoryAiTaskProviderSettingRepository settingRepository;
    private AiSettingsUseCase aiSettingsUseCase;

    private UUID userId;

    @BeforeEach
    void setUp() {
        infrastructurePort = new StubAiInfrastructurePort();
        settingRepository = new InMemoryAiTaskProviderSettingRepository();
        aiSettingsUseCase = new AiSettingsUseCase(infrastructurePort, settingRepository);
        userId = UUID.randomUUID();
    }

    @Test
    void getOllamaStatus_returnsConnectedStatusWithModels() {
        infrastructurePort.available = true;

        AiOllamaStatusDto result = aiSettingsUseCase.getOllamaStatus();

        assertTrue(result.connected());
        assertEquals("http://localhost:11434", result.endpoint());
        assertEquals("llama3", result.currentModel());
        assertEquals(List.of("llama3", "mistral"), result.availableModels());
    }

    @Test
    void getOllamaStatus_returnsDisconnectedStatusWithEmptyModels() {
        infrastructurePort.available = false;

        AiOllamaStatusDto result = aiSettingsUseCase.getOllamaStatus();

        assertFalse(result.connected());
        assertTrue(result.availableModels().isEmpty());
    }

    @Test
    void reconnectOllama_returnsCurrentStatus() {
        infrastructurePort.available = true;

        AiOllamaStatusDto result = aiSettingsUseCase.reconnectOllama();

        assertNotNull(result);
        assertTrue(result.connected());
    }

    @Test
    void getTaskProviderSettings_returnsSettingsForUser() {
        settingRepository.save(new AiTaskProviderSetting(
                UUID.randomUUID(), userId, AiTaskType.COMPOSER, AiModelProvider.OLLAMA));
        settingRepository.save(new AiTaskProviderSetting(
                UUID.randomUUID(), userId, AiTaskType.TRANSLATE, AiModelProvider.OPENAI));

        List<AiTaskProviderSettingDto> result = aiSettingsUseCase.getTaskProviderSettings(userId);

        assertEquals(2, result.size());
    }

    @Test
    void getTaskProviderSettings_returnsEmptyForUserWithNoSettings() {
        List<AiTaskProviderSettingDto> result = aiSettingsUseCase.getTaskProviderSettings(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void saveTaskProviderSettings_createsNewSettings() {
        List<AiTaskProviderSettingDto> settings = List.of(
                new AiTaskProviderSettingDto("COMPOSER", "OLLAMA"),
                new AiTaskProviderSettingDto("TRANSLATE", "OPENAI"));
        AiTaskProviderSettingsRequestDto request = new AiTaskProviderSettingsRequestDto(settings);

        List<AiTaskProviderSettingDto> result = aiSettingsUseCase.saveTaskProviderSettings(userId, request);

        assertEquals(2, result.size());
    }

    @Test
    void saveTaskProviderSettings_updatesExistingSettings() {
        settingRepository.save(new AiTaskProviderSetting(
                UUID.randomUUID(), userId, AiTaskType.COMPOSER, AiModelProvider.OLLAMA));

        List<AiTaskProviderSettingDto> settings = List.of(
                new AiTaskProviderSettingDto("COMPOSER", "OPENAI"));
        AiTaskProviderSettingsRequestDto request = new AiTaskProviderSettingsRequestDto(settings);

        List<AiTaskProviderSettingDto> result = aiSettingsUseCase.saveTaskProviderSettings(userId, request);

        assertEquals(1, result.size());
        assertEquals("OPENAI", result.get(0).provider());
    }

    @Test
    void saveTaskProviderSettings_throwsWhenRequestIsNull() {
        DomainException exception = assertThrows(DomainException.class,
                () -> aiSettingsUseCase.saveTaskProviderSettings(userId, null));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void saveTaskProviderSettings_throwsWhenSettingsListIsNull() {
        AiTaskProviderSettingsRequestDto request = new AiTaskProviderSettingsRequestDto(null);

        DomainException exception = assertThrows(DomainException.class,
                () -> aiSettingsUseCase.saveTaskProviderSettings(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void saveTaskProviderSettings_throwsWhenSettingsListIsEmpty() {
        AiTaskProviderSettingsRequestDto request = new AiTaskProviderSettingsRequestDto(List.of());

        DomainException exception = assertThrows(DomainException.class,
                () -> aiSettingsUseCase.saveTaskProviderSettings(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void saveTaskProviderSettings_throwsWhenTaskTypeIsNull() {
        List<AiTaskProviderSettingDto> settings = List.of(
                new AiTaskProviderSettingDto(null, "OLLAMA"));
        AiTaskProviderSettingsRequestDto request = new AiTaskProviderSettingsRequestDto(settings);

        DomainException exception = assertThrows(DomainException.class,
                () -> aiSettingsUseCase.saveTaskProviderSettings(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void saveTaskProviderSettings_throwsWhenTaskTypeIsInvalid() {
        List<AiTaskProviderSettingDto> settings = List.of(
                new AiTaskProviderSettingDto("INVALID_TYPE", "OLLAMA"));
        AiTaskProviderSettingsRequestDto request = new AiTaskProviderSettingsRequestDto(settings);

        DomainException exception = assertThrows(DomainException.class,
                () -> aiSettingsUseCase.saveTaskProviderSettings(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void saveTaskProviderSettings_throwsWhenProviderIsNull() {
        List<AiTaskProviderSettingDto> settings = List.of(
                new AiTaskProviderSettingDto("COMPOSER", null));
        AiTaskProviderSettingsRequestDto request = new AiTaskProviderSettingsRequestDto(settings);

        DomainException exception = assertThrows(DomainException.class,
                () -> aiSettingsUseCase.saveTaskProviderSettings(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void saveTaskProviderSettings_throwsWhenProviderIsInvalid() {
        List<AiTaskProviderSettingDto> settings = List.of(
                new AiTaskProviderSettingDto("COMPOSER", "INVALID_PROVIDER"));
        AiTaskProviderSettingsRequestDto request = new AiTaskProviderSettingsRequestDto(settings);

        DomainException exception = assertThrows(DomainException.class,
                () -> aiSettingsUseCase.saveTaskProviderSettings(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void getCostMonitor_returnsZeroWhenNoCalls() {
        AiCostMonitorDto result = aiSettingsUseCase.getCostMonitor();

        assertEquals(0, result.totalCalls());
        assertTrue(result.callsByProvider().isEmpty());
    }

    @Test
    void recordExternalAiCall_incrementsTotalAndProviderCounts() {
        aiSettingsUseCase.recordExternalAiCall("OPENAI");
        aiSettingsUseCase.recordExternalAiCall("OPENAI");
        aiSettingsUseCase.recordExternalAiCall("OLLAMA");

        AiCostMonitorDto result = aiSettingsUseCase.getCostMonitor();

        assertEquals(3, result.totalCalls());
        assertEquals(2L, result.callsByProvider().get("OPENAI"));
        assertEquals(1L, result.callsByProvider().get("OLLAMA"));
    }

    private static class StubAiInfrastructurePort implements AiInfrastructurePort {

        boolean available = true;

        @Override
        public boolean isOllamaAvailable() {
            return available;
        }

        @Override
        public String getOllamaEndpoint() {
            return "http://localhost:11434";
        }

        @Override
        public String getOllamaDefaultModel() {
            return "llama3";
        }

        @Override
        public List<String> listOllamaModels() {
            return List.of("llama3", "mistral");
        }
    }

    private static class InMemoryAiTaskProviderSettingRepository implements AiTaskProviderSettingRepository {

        private final Map<UUID, AiTaskProviderSetting> store = new HashMap<>();

        @Override
        public List<AiTaskProviderSetting> findByUserId(UUID userId) {
            return store.values().stream()
                    .filter(s -> s.getUserId().equals(userId))
                    .toList();
        }

        @Override
        public Optional<AiTaskProviderSetting> findByUserIdAndTaskType(UUID userId, AiTaskType taskType) {
            return store.values().stream()
                    .filter(s -> s.getUserId().equals(userId))
                    .filter(s -> s.getTaskType() == taskType)
                    .findFirst();
        }

        @Override
        public AiTaskProviderSetting save(AiTaskProviderSetting setting) {
            store.put(setting.getId(), setting);
            return setting;
        }

        @Override
        public void saveAll(List<AiTaskProviderSetting> settings) {
            for (AiTaskProviderSetting setting : settings) {
                store.put(setting.getId(), setting);
            }
        }
    }
}
