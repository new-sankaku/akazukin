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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class AiSettingsUseCase {

    private static final Logger LOG = Logger.getLogger(AiSettingsUseCase.class.getName());

    private final AiInfrastructurePort aiInfrastructurePort;
    private final AiTaskProviderSettingRepository settingRepository;

    private final AtomicLong totalExternalCalls = new AtomicLong(0);
    private final Map<String, AtomicLong> callsByProvider = new LinkedHashMap<>();

    @Inject
    public AiSettingsUseCase(AiInfrastructurePort aiInfrastructurePort,
                             AiTaskProviderSettingRepository settingRepository) {
        this.aiInfrastructurePort = aiInfrastructurePort;
        this.settingRepository = settingRepository;
    }

    public AiOllamaStatusDto getOllamaStatus() {
        long perfStart = System.nanoTime();
        try {
            boolean connected = aiInfrastructurePort.isOllamaAvailable();
            String currentModel = aiInfrastructurePort.getOllamaDefaultModel();
            List<String> availableModels = List.of();

            if (connected) {
                availableModels = aiInfrastructurePort.listOllamaModels();
            }

            return new AiOllamaStatusDto(
                    connected,
                    aiInfrastructurePort.getOllamaEndpoint(),
                    currentModel,
                    availableModels
            );
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms",
                        new Object[]{"AiSettingsUseCase.getOllamaStatus", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms",
                        new Object[]{"AiSettingsUseCase.getOllamaStatus", perfMs});
            }
        }
    }

    public AiOllamaStatusDto reconnectOllama() {
        LOG.log(Level.INFO, "Ollama reconnection requested");
        return getOllamaStatus();
    }

    public List<AiTaskProviderSettingDto> getTaskProviderSettings(UUID userId) {
        long perfStart = System.nanoTime();
        try {
            return settingRepository.findByUserId(userId).stream()
                    .map(s -> new AiTaskProviderSettingDto(s.getTaskType().name(), s.getProvider().name()))
                    .collect(Collectors.toList());
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms",
                        new Object[]{"AiSettingsUseCase.getTaskProviderSettings", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms",
                        new Object[]{"AiSettingsUseCase.getTaskProviderSettings", perfMs});
            }
        }
    }

    public List<AiTaskProviderSettingDto> saveTaskProviderSettings(UUID userId,
                                                                    AiTaskProviderSettingsRequestDto request) {
        long perfStart = System.nanoTime();
        try {
            if (request == null || request.settings() == null || request.settings().isEmpty()) {
                throw new DomainException("INVALID_INPUT", "Settings list is required");
            }

            List<AiTaskProviderSetting> existingSettings = settingRepository.findByUserId(userId);
            Map<AiTaskType, AiTaskProviderSetting> existingMap = existingSettings.stream()
                    .collect(Collectors.toMap(AiTaskProviderSetting::getTaskType, s -> s));

            for (AiTaskProviderSettingDto dto : request.settings()) {
                AiTaskType taskType = parseTaskType(dto.taskType());
                AiModelProvider provider = parseProvider(dto.provider());

                AiTaskProviderSetting existing = existingMap.get(taskType);
                if (existing != null) {
                    existing.setProvider(provider);
                    settingRepository.save(existing);
                } else {
                    AiTaskProviderSetting newSetting = new AiTaskProviderSetting(
                            UUID.randomUUID(), userId, taskType, provider);
                    settingRepository.save(newSetting);
                }
            }

            LOG.log(Level.INFO, "AI task provider settings saved for user {0}", userId);
            return getTaskProviderSettings(userId);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms",
                        new Object[]{"AiSettingsUseCase.saveTaskProviderSettings", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms",
                        new Object[]{"AiSettingsUseCase.saveTaskProviderSettings", perfMs});
            }
        }
    }

    public AiCostMonitorDto getCostMonitor() {
        Map<String, Long> snapshot = new LinkedHashMap<>();
        for (Map.Entry<String, AtomicLong> entry : callsByProvider.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().get());
        }
        return new AiCostMonitorDto(totalExternalCalls.get(), snapshot);
    }

    public void recordExternalAiCall(String providerName) {
        totalExternalCalls.incrementAndGet();
        callsByProvider.computeIfAbsent(providerName, k -> new AtomicLong(0)).incrementAndGet();
    }

    private AiTaskType parseTaskType(String taskType) {
        if (taskType == null || taskType.isBlank()) {
            throw new DomainException("INVALID_INPUT", "Task type is required");
        }
        try {
            return AiTaskType.valueOf(taskType.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainException("INVALID_INPUT", "Invalid task type: " + taskType);
        }
    }

    private AiModelProvider parseProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            throw new DomainException("INVALID_INPUT", "Provider is required");
        }
        try {
            return AiModelProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainException("INVALID_INPUT", "Invalid provider: " + provider);
        }
    }
}
