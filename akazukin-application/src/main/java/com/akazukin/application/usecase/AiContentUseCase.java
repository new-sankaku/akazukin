package com.akazukin.application.usecase;

import com.akazukin.application.dto.AiCompareColumnDto;
import com.akazukin.application.dto.AiCompareRequestDto;
import com.akazukin.application.dto.AiCompareResultDto;
import com.akazukin.application.dto.AiGenerateRequestDto;
import com.akazukin.application.dto.AiGenerateResponseDto;
import com.akazukin.application.dto.AiPersonaDto;
import com.akazukin.application.dto.AiPersonaRequestDto;
import com.akazukin.application.dto.AiTryoutRequestDto;
import com.akazukin.application.dto.AiTryoutResponseDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.AiPersona;
import com.akazukin.domain.model.AiPrompt;
import com.akazukin.domain.model.AiResponse;
import com.akazukin.domain.model.ContentTone;
import com.akazukin.domain.port.AiPersonaRepository;
import com.akazukin.domain.port.AiTextGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class AiContentUseCase {

    private static final Logger LOG = Logger.getLogger(AiContentUseCase.class.getName());

    private final AiTextGenerator aiTextGenerator;
    private final AiPersonaRepository aiPersonaRepository;

    @Inject
    public AiContentUseCase(AiTextGenerator aiTextGenerator,
                            AiPersonaRepository aiPersonaRepository) {
        this.aiTextGenerator = aiTextGenerator;
        this.aiPersonaRepository = aiPersonaRepository;
    }

    public AiGenerateResponseDto generate(UUID userId, AiGenerateRequestDto request) {
        long perfStart = System.nanoTime();
        try {
            if (request.prompt() == null || request.prompt().isBlank()) {
                throw new DomainException("INVALID_INPUT", "Prompt is required");
            }

            AiResponse response;

            if (request.personaId() != null) {
                AiPersona persona = aiPersonaRepository.findById(request.personaId())
                        .orElseThrow(() -> new DomainException("PERSONA_NOT_FOUND",
                                "AI persona not found: " + request.personaId()));

                if (!persona.getUserId().equals(userId)) {
                    throw new DomainException("FORBIDDEN", "You do not own this persona");
                }

                response = aiTextGenerator.generateWithPersona(persona, request.prompt());
            } else {
                AiPrompt prompt = new AiPrompt(
                        null,
                        request.prompt(),
                        request.temperature(),
                        request.maxTokens()
                );
                response = aiTextGenerator.generate(prompt);
            }

            LOG.log(Level.INFO, "AI content generated for user {0}, tokens used: {1}",
                    new Object[]{userId, response.tokensUsed()});

            return new AiGenerateResponseDto(
                    response.generatedText(),
                    response.tokensUsed(),
                    response.durationMs(),
                    response.modelName()
            );
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AiContentUseCase.generate", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AiContentUseCase.generate", perfMs});
            }
        }
    }

    public List<AiPersonaDto> listPersonas(UUID userId) {
        long perfStart = System.nanoTime();
        try {
            return aiPersonaRepository.findByUserId(userId).stream()
                    .map(this::toPersonaDto)
                    .toList();
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AiContentUseCase.listPersonas", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AiContentUseCase.listPersonas", perfMs});
            }
        }
    }

    public AiPersonaDto createPersona(UUID userId, AiPersonaRequestDto request) {
        long perfStart = System.nanoTime();
        try {
            if (request.name() == null || request.name().isBlank()) {
                throw new DomainException("INVALID_INPUT", "Persona name is required");
            }
            if (request.systemPrompt() == null || request.systemPrompt().isBlank()) {
                throw new DomainException("INVALID_INPUT", "System prompt is required");
            }

            Instant now = Instant.now();
            ContentTone tone = parseTone(request.tone());

            AiPersona persona = new AiPersona(
                    UUID.randomUUID(),
                    userId,
                    request.name(),
                    request.systemPrompt(),
                    tone,
                    request.language(),
                    request.avatarUrl(),
                    request.isDefault(),
                    now,
                    now
            );

            AiPersona saved = aiPersonaRepository.save(persona);
            LOG.log(Level.INFO, "AI persona created: {0} for user {1}",
                    new Object[]{saved.getId(), userId});

            return toPersonaDto(saved);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AiContentUseCase.createPersona", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AiContentUseCase.createPersona", perfMs});
            }
        }
    }

    public AiPersonaDto updatePersona(UUID personaId, UUID userId, AiPersonaRequestDto request) {
        long perfStart = System.nanoTime();
        try {
            AiPersona persona = aiPersonaRepository.findById(personaId)
                    .orElseThrow(() -> new DomainException("PERSONA_NOT_FOUND",
                            "AI persona not found: " + personaId));

            if (!persona.getUserId().equals(userId)) {
                throw new DomainException("FORBIDDEN", "You do not own this persona");
            }

            if (request.name() == null || request.name().isBlank()) {
                throw new DomainException("INVALID_INPUT", "Persona name is required");
            }

            ContentTone tone = parseTone(request.tone());

            persona.setName(request.name());
            persona.setSystemPrompt(request.systemPrompt());
            persona.setTone(tone);
            persona.setLanguage(request.language());
            persona.setAvatarUrl(request.avatarUrl());
            persona.setDefault(request.isDefault());
            persona.setUpdatedAt(Instant.now());

            AiPersona saved = aiPersonaRepository.save(persona);
            LOG.log(Level.INFO, "AI persona updated: {0}", personaId);

            return toPersonaDto(saved);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AiContentUseCase.updatePersona", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AiContentUseCase.updatePersona", perfMs});
            }
        }
    }

    public void deletePersona(UUID personaId, UUID userId) {
        long perfStart = System.nanoTime();
        try {
            AiPersona persona = aiPersonaRepository.findById(personaId)
                    .orElseThrow(() -> new DomainException("PERSONA_NOT_FOUND",
                            "AI persona not found: " + personaId));

            if (!persona.getUserId().equals(userId)) {
                throw new DomainException("FORBIDDEN", "You do not own this persona");
            }

            aiPersonaRepository.deleteById(personaId);
            LOG.log(Level.INFO, "AI persona deleted: {0}", personaId);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"AiContentUseCase.deletePersona", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"AiContentUseCase.deletePersona", perfMs});
            }
        }
    }

    public AiCompareResultDto compareGenerate(UUID userId, AiCompareRequestDto request) {
        long perfStart = System.nanoTime();
        try {
            if (request.prompt() == null || request.prompt().isBlank()) {
                throw new DomainException("INVALID_INPUT", "Prompt is required");
            }
            if (request.personaIds() == null || request.personaIds().size() < 2 || request.personaIds().size() > 4) {
                throw new DomainException("INVALID_INPUT", "Select 2 to 4 personas");
            }

            List<AiCompareColumnDto> columns = new ArrayList<>();
            for (UUID personaId : request.personaIds()) {
                AiPersona persona = aiPersonaRepository.findById(personaId)
                        .orElseThrow(() -> new DomainException("PERSONA_NOT_FOUND",
                                "AI persona not found: " + personaId));
                if (!persona.getUserId().equals(userId)) {
                    throw new DomainException("FORBIDDEN", "You do not own this persona");
                }

                AiResponse response = aiTextGenerator.generateWithPersona(persona, request.prompt());

                columns.add(new AiCompareColumnDto(
                        persona.getId(),
                        persona.getName(),
                        persona.getTone().name(),
                        persona.getLanguage(),
                        response.generatedText(),
                        response.tokensUsed(),
                        response.durationMs(),
                        response.modelName()
                ));
            }

            LOG.log(Level.INFO, "Compare generation completed for user {0}, {1} personas",
                    new Object[]{userId, columns.size()});
            return new AiCompareResultDto(columns);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms",
                        new Object[]{"AiContentUseCase.compareGenerate", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms",
                        new Object[]{"AiContentUseCase.compareGenerate", perfMs});
            }
        }
    }

    public AiTryoutResponseDto tryout(UUID userId, AiTryoutRequestDto request) {
        long perfStart = System.nanoTime();
        try {
            if (request.text() == null || request.text().isBlank()) {
                throw new DomainException("INVALID_INPUT", "Text is required");
            }
            if (request.personaId() == null) {
                throw new DomainException("INVALID_INPUT", "Persona ID is required");
            }

            AiPersona persona = aiPersonaRepository.findById(request.personaId())
                    .orElseThrow(() -> new DomainException("PERSONA_NOT_FOUND",
                            "AI persona not found: " + request.personaId()));
            if (!persona.getUserId().equals(userId)) {
                throw new DomainException("FORBIDDEN", "You do not own this persona");
            }

            AiResponse response = aiTextGenerator.generateWithPersona(persona, request.text());

            LOG.log(Level.INFO, "Tryout completed for user {0}, persona {1}",
                    new Object[]{userId, persona.getName()});

            return new AiTryoutResponseDto(
                    persona.getId(),
                    request.text(),
                    response.generatedText(),
                    response.tokensUsed(),
                    response.durationMs()
            );
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms",
                        new Object[]{"AiContentUseCase.tryout", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms",
                        new Object[]{"AiContentUseCase.tryout", perfMs});
            }
        }
    }

    private ContentTone parseTone(String tone) {
        if (tone == null || tone.isBlank()) {
            throw new DomainException("INVALID_INPUT", "Tone is required");
        }
        try {
            return ContentTone.valueOf(tone.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainException("INVALID_INPUT", "Invalid tone: " + tone);
        }
    }

    private AiPersonaDto toPersonaDto(AiPersona persona) {
        return new AiPersonaDto(
                persona.getId(),
                persona.getName(),
                persona.getSystemPrompt(),
                persona.getTone().name(),
                persona.getLanguage(),
                persona.getAvatarUrl(),
                persona.isDefault(),
                persona.getCreatedAt()
        );
    }
}
