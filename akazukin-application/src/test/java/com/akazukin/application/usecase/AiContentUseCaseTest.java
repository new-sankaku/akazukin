package com.akazukin.application.usecase;

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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiContentUseCaseTest {

    private InMemoryAiPersonaRepository personaRepository;
    private StubAiTextGenerator textGenerator;
    private AiContentUseCase aiContentUseCase;

    private UUID userId;

    @BeforeEach
    void setUp() {
        personaRepository = new InMemoryAiPersonaRepository();
        textGenerator = new StubAiTextGenerator();
        aiContentUseCase = new AiContentUseCase(textGenerator, personaRepository);
        userId = UUID.randomUUID();
    }

    @Test
    void generate_returnsResponseWithoutPersona() {
        AiGenerateRequestDto request = new AiGenerateRequestDto(null, "Hello AI", 0.7, 100);

        AiGenerateResponseDto result = aiContentUseCase.generate(userId, request);

        assertNotNull(result);
        assertEquals("stub:Hello AI", result.generatedText());
        assertEquals(42, result.tokensUsed());
        assertEquals("stub-model", result.modelName());
    }

    @Test
    void generate_returnsResponseWithPersona() {
        AiPersona persona = createPersona(userId, "TestBot", ContentTone.CASUAL);
        personaRepository.save(persona);

        AiGenerateRequestDto request = new AiGenerateRequestDto(persona.getId(), "Hello AI", 0.7, 100);

        AiGenerateResponseDto result = aiContentUseCase.generate(userId, request);

        assertNotNull(result);
        assertEquals("persona:Hello AI", result.generatedText());
    }

    @Test
    void generate_throwsWhenPromptIsNull() {
        AiGenerateRequestDto request = new AiGenerateRequestDto(null, null, 0.7, 100);

        DomainException exception = assertThrows(DomainException.class,
                () -> aiContentUseCase.generate(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void generate_throwsWhenPromptIsBlank() {
        AiGenerateRequestDto request = new AiGenerateRequestDto(null, "  ", 0.7, 100);

        DomainException exception = assertThrows(DomainException.class,
                () -> aiContentUseCase.generate(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void generate_throwsWhenPersonaNotFound() {
        UUID nonExistentPersonaId = UUID.randomUUID();
        AiGenerateRequestDto request = new AiGenerateRequestDto(nonExistentPersonaId, "Hello", 0.7, 100);

        DomainException exception = assertThrows(DomainException.class,
                () -> aiContentUseCase.generate(userId, request));
        assertEquals("PERSONA_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void generate_throwsWhenPersonaBelongsToOtherUser() {
        UUID otherUserId = UUID.randomUUID();
        AiPersona persona = createPersona(otherUserId, "OtherBot", ContentTone.FORMAL);
        personaRepository.save(persona);

        AiGenerateRequestDto request = new AiGenerateRequestDto(persona.getId(), "Hello", 0.7, 100);

        DomainException exception = assertThrows(DomainException.class,
                () -> aiContentUseCase.generate(userId, request));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void listPersonas_returnsPersonasForUser() {
        personaRepository.save(createPersona(userId, "Bot1", ContentTone.CASUAL));
        personaRepository.save(createPersona(userId, "Bot2", ContentTone.FORMAL));
        personaRepository.save(createPersona(UUID.randomUUID(), "OtherBot", ContentTone.HUMOROUS));

        List<AiPersonaDto> result = aiContentUseCase.listPersonas(userId);

        assertEquals(2, result.size());
    }

    @Test
    void listPersonas_returnsEmptyForUserWithNoPersonas() {
        List<AiPersonaDto> result = aiContentUseCase.listPersonas(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void createPersona_createsAndReturnsPersona() {
        AiPersonaRequestDto request = new AiPersonaRequestDto(
                "NewBot", "You are helpful", "CASUAL", "ja", null, false);

        AiPersonaDto result = aiContentUseCase.createPersona(userId, request);

        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals("NewBot", result.name());
        assertEquals("You are helpful", result.systemPrompt());
        assertEquals("CASUAL", result.tone());
        assertEquals("ja", result.language());
    }

    @Test
    void createPersona_throwsWhenNameIsNull() {
        AiPersonaRequestDto request = new AiPersonaRequestDto(
                null, "prompt", "CASUAL", "ja", null, false);

        DomainException exception = assertThrows(DomainException.class,
                () -> aiContentUseCase.createPersona(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void createPersona_throwsWhenNameIsBlank() {
        AiPersonaRequestDto request = new AiPersonaRequestDto(
                "  ", "prompt", "CASUAL", "ja", null, false);

        DomainException exception = assertThrows(DomainException.class,
                () -> aiContentUseCase.createPersona(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void createPersona_throwsWhenSystemPromptIsNull() {
        AiPersonaRequestDto request = new AiPersonaRequestDto(
                "Bot", null, "CASUAL", "ja", null, false);

        DomainException exception = assertThrows(DomainException.class,
                () -> aiContentUseCase.createPersona(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void createPersona_throwsWhenSystemPromptIsBlank() {
        AiPersonaRequestDto request = new AiPersonaRequestDto(
                "Bot", "  ", "CASUAL", "ja", null, false);

        DomainException exception = assertThrows(DomainException.class,
                () -> aiContentUseCase.createPersona(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void createPersona_throwsWhenToneIsNull() {
        AiPersonaRequestDto request = new AiPersonaRequestDto(
                "Bot", "prompt", null, "ja", null, false);

        DomainException exception = assertThrows(DomainException.class,
                () -> aiContentUseCase.createPersona(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void createPersona_throwsWhenToneIsInvalid() {
        AiPersonaRequestDto request = new AiPersonaRequestDto(
                "Bot", "prompt", "INVALID_TONE", "ja", null, false);

        DomainException exception = assertThrows(DomainException.class,
                () -> aiContentUseCase.createPersona(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void updatePersona_updatesAndReturnsPersona() {
        AiPersona persona = createPersona(userId, "OldName", ContentTone.CASUAL);
        personaRepository.save(persona);

        AiPersonaRequestDto request = new AiPersonaRequestDto(
                "NewName", "new prompt", "FORMAL", "en", "http://avatar.png", true);

        AiPersonaDto result = aiContentUseCase.updatePersona(persona.getId(), userId, request);

        assertEquals("NewName", result.name());
        assertEquals("FORMAL", result.tone());
        assertEquals("en", result.language());
    }

    @Test
    void updatePersona_throwsWhenPersonaNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        AiPersonaRequestDto request = new AiPersonaRequestDto(
                "Name", "prompt", "CASUAL", "ja", null, false);

        DomainException exception = assertThrows(DomainException.class,
                () -> aiContentUseCase.updatePersona(nonExistentId, userId, request));
        assertEquals("PERSONA_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void updatePersona_throwsWhenNotOwner() {
        UUID otherUserId = UUID.randomUUID();
        AiPersona persona = createPersona(otherUserId, "Bot", ContentTone.CASUAL);
        personaRepository.save(persona);

        AiPersonaRequestDto request = new AiPersonaRequestDto(
                "Name", "prompt", "CASUAL", "ja", null, false);

        DomainException exception = assertThrows(DomainException.class,
                () -> aiContentUseCase.updatePersona(persona.getId(), userId, request));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void updatePersona_throwsWhenNameIsBlank() {
        AiPersona persona = createPersona(userId, "Bot", ContentTone.CASUAL);
        personaRepository.save(persona);

        AiPersonaRequestDto request = new AiPersonaRequestDto(
                "", "prompt", "CASUAL", "ja", null, false);

        DomainException exception = assertThrows(DomainException.class,
                () -> aiContentUseCase.updatePersona(persona.getId(), userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void deletePersona_removesPersona() {
        AiPersona persona = createPersona(userId, "Bot", ContentTone.CASUAL);
        personaRepository.save(persona);

        aiContentUseCase.deletePersona(persona.getId(), userId);

        assertTrue(personaRepository.findById(persona.getId()).isEmpty());
    }

    @Test
    void deletePersona_throwsWhenPersonaNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> aiContentUseCase.deletePersona(nonExistentId, userId));
        assertEquals("PERSONA_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void deletePersona_throwsWhenNotOwner() {
        UUID otherUserId = UUID.randomUUID();
        AiPersona persona = createPersona(otherUserId, "Bot", ContentTone.CASUAL);
        personaRepository.save(persona);

        DomainException exception = assertThrows(DomainException.class,
                () -> aiContentUseCase.deletePersona(persona.getId(), userId));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void compareGenerate_returnsColumnsForMultiplePersonas() {
        AiPersona persona1 = createPersona(userId, "Bot1", ContentTone.CASUAL);
        AiPersona persona2 = createPersona(userId, "Bot2", ContentTone.FORMAL);
        personaRepository.save(persona1);
        personaRepository.save(persona2);

        AiCompareRequestDto request = new AiCompareRequestDto(
                "Compare this", List.of(persona1.getId(), persona2.getId()));

        AiCompareResultDto result = aiContentUseCase.compareGenerate(userId, request);

        assertNotNull(result);
        assertEquals(2, result.columns().size());
        assertEquals("Bot1", result.columns().get(0).personaName());
        assertEquals("Bot2", result.columns().get(1).personaName());
    }

    @Test
    void compareGenerate_throwsWhenPromptIsNull() {
        AiCompareRequestDto request = new AiCompareRequestDto(
                null, List.of(UUID.randomUUID(), UUID.randomUUID()));

        DomainException exception = assertThrows(DomainException.class,
                () -> aiContentUseCase.compareGenerate(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void compareGenerate_throwsWhenPromptIsBlank() {
        AiCompareRequestDto request = new AiCompareRequestDto(
                "  ", List.of(UUID.randomUUID(), UUID.randomUUID()));

        DomainException exception = assertThrows(DomainException.class,
                () -> aiContentUseCase.compareGenerate(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void compareGenerate_throwsWhenPersonaIdsIsNull() {
        AiCompareRequestDto request = new AiCompareRequestDto("Hello", null);

        DomainException exception = assertThrows(DomainException.class,
                () -> aiContentUseCase.compareGenerate(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void compareGenerate_throwsWhenFewerThanTwoPersonas() {
        AiCompareRequestDto request = new AiCompareRequestDto(
                "Hello", List.of(UUID.randomUUID()));

        DomainException exception = assertThrows(DomainException.class,
                () -> aiContentUseCase.compareGenerate(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void compareGenerate_throwsWhenMoreThanFourPersonas() {
        AiCompareRequestDto request = new AiCompareRequestDto(
                "Hello", List.of(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()));

        DomainException exception = assertThrows(DomainException.class,
                () -> aiContentUseCase.compareGenerate(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void compareGenerate_throwsWhenPersonaNotFound() {
        AiPersona persona1 = createPersona(userId, "Bot1", ContentTone.CASUAL);
        personaRepository.save(persona1);

        AiCompareRequestDto request = new AiCompareRequestDto(
                "Hello", List.of(persona1.getId(), UUID.randomUUID()));

        DomainException exception = assertThrows(DomainException.class,
                () -> aiContentUseCase.compareGenerate(userId, request));
        assertEquals("PERSONA_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void compareGenerate_throwsWhenPersonaBelongsToOtherUser() {
        AiPersona persona1 = createPersona(userId, "Bot1", ContentTone.CASUAL);
        AiPersona persona2 = createPersona(UUID.randomUUID(), "Bot2", ContentTone.FORMAL);
        personaRepository.save(persona1);
        personaRepository.save(persona2);

        AiCompareRequestDto request = new AiCompareRequestDto(
                "Hello", List.of(persona1.getId(), persona2.getId()));

        DomainException exception = assertThrows(DomainException.class,
                () -> aiContentUseCase.compareGenerate(userId, request));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void tryout_returnsTransformedText() {
        AiPersona persona = createPersona(userId, "Bot", ContentTone.CASUAL);
        personaRepository.save(persona);

        AiTryoutRequestDto request = new AiTryoutRequestDto(persona.getId(), "Transform this");

        AiTryoutResponseDto result = aiContentUseCase.tryout(userId, request);

        assertNotNull(result);
        assertEquals(persona.getId(), result.personaId());
        assertEquals("Transform this", result.originalText());
        assertEquals("persona:Transform this", result.transformedText());
    }

    @Test
    void tryout_throwsWhenTextIsNull() {
        AiTryoutRequestDto request = new AiTryoutRequestDto(UUID.randomUUID(), null);

        DomainException exception = assertThrows(DomainException.class,
                () -> aiContentUseCase.tryout(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void tryout_throwsWhenTextIsBlank() {
        AiTryoutRequestDto request = new AiTryoutRequestDto(UUID.randomUUID(), "  ");

        DomainException exception = assertThrows(DomainException.class,
                () -> aiContentUseCase.tryout(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void tryout_throwsWhenPersonaIdIsNull() {
        AiTryoutRequestDto request = new AiTryoutRequestDto(null, "Hello");

        DomainException exception = assertThrows(DomainException.class,
                () -> aiContentUseCase.tryout(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void tryout_throwsWhenPersonaNotFound() {
        AiTryoutRequestDto request = new AiTryoutRequestDto(UUID.randomUUID(), "Hello");

        DomainException exception = assertThrows(DomainException.class,
                () -> aiContentUseCase.tryout(userId, request));
        assertEquals("PERSONA_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void tryout_throwsWhenPersonaBelongsToOtherUser() {
        AiPersona persona = createPersona(UUID.randomUUID(), "Bot", ContentTone.CASUAL);
        personaRepository.save(persona);

        AiTryoutRequestDto request = new AiTryoutRequestDto(persona.getId(), "Hello");

        DomainException exception = assertThrows(DomainException.class,
                () -> aiContentUseCase.tryout(userId, request));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    private AiPersona createPersona(UUID ownerId, String name, ContentTone tone) {
        Instant now = Instant.now();
        return new AiPersona(UUID.randomUUID(), ownerId, name, "system prompt",
                tone, "ja", null, false, now, now);
    }

    private static class InMemoryAiPersonaRepository implements AiPersonaRepository {

        private final Map<UUID, AiPersona> store = new HashMap<>();

        @Override
        public Optional<AiPersona> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<AiPersona> findByUserId(UUID userId) {
            return store.values().stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .toList();
        }

        @Override
        public Optional<AiPersona> findDefaultByUserId(UUID userId) {
            return store.values().stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .filter(AiPersona::isDefault)
                    .findFirst();
        }

        @Override
        public AiPersona save(AiPersona aiPersona) {
            store.put(aiPersona.getId(), aiPersona);
            return aiPersona;
        }

        @Override
        public void deleteById(UUID id) {
            store.remove(id);
        }
    }

    private static class StubAiTextGenerator implements AiTextGenerator {

        @Override
        public AiResponse generate(AiPrompt prompt) {
            return new AiResponse("stub:" + prompt.userPrompt(), 42, 100L, "stub-model");
        }

        @Override
        public AiResponse generateWithPersona(AiPersona persona, String userInput) {
            return new AiResponse("persona:" + userInput, 50, 120L, "stub-model");
        }
    }
}
