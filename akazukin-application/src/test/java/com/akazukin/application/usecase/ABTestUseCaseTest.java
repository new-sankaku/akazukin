package com.akazukin.application.usecase;

import com.akazukin.application.dto.ABTestDto;
import com.akazukin.application.dto.ABTestMultiPlatformRequestDto;
import com.akazukin.application.dto.ABTestMultiPlatformResponseDto;
import com.akazukin.application.dto.ABTestRequestDto;
import com.akazukin.application.dto.ABTestVariantGenerateRequestDto;
import com.akazukin.application.dto.ABTestVariantGenerateResponseDto;
import com.akazukin.application.dto.ABTestWinPatternDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.ABTest;
import com.akazukin.domain.model.ABTestStatus;
import com.akazukin.domain.model.AiPersona;
import com.akazukin.domain.model.AiPrompt;
import com.akazukin.domain.model.AiResponse;
import com.akazukin.domain.model.ContentTone;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.ABTestRepository;
import com.akazukin.domain.port.AiPersonaRepository;
import com.akazukin.domain.port.AiTextGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ABTestUseCaseTest {

    private InMemoryABTestRepository abTestRepository;
    private StubAiTextGenerator aiTextGenerator;
    private InMemoryAiPersonaRepository aiPersonaRepository;
    private ABTestUseCase abTestUseCase;

    private UUID userId;

    @BeforeEach
    void setUp() {
        abTestRepository = new InMemoryABTestRepository();
        aiTextGenerator = new StubAiTextGenerator();
        aiPersonaRepository = new InMemoryAiPersonaRepository();
        abTestUseCase = new ABTestUseCase(abTestRepository, aiTextGenerator, aiPersonaRepository);
        userId = UUID.randomUUID();
    }

    @Test
    void createTest_createsTestWithDraftStatus() {
        ABTestRequestDto request = new ABTestRequestDto(
                "Test 1", "Variant A text", "Variant B text", null, List.of("TWITTER"));

        ABTestDto result = abTestUseCase.createTest(userId, request);

        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals("Test 1", result.name());
        assertEquals("Variant A text", result.variantA());
        assertEquals("Variant B text", result.variantB());
        assertEquals("DRAFT", result.status());
        assertEquals(List.of("TWITTER"), result.platforms());
    }

    @Test
    void createTest_createsTestWithVariantC() {
        ABTestRequestDto request = new ABTestRequestDto(
                "Test C", "A", "B", "C", List.of("TWITTER"));

        ABTestDto result = abTestUseCase.createTest(userId, request);

        assertEquals("C", result.variantC());
    }

    @Test
    void createTest_throwsWhenNameIsNull() {
        ABTestRequestDto request = new ABTestRequestDto(
                null, "A", "B", null, List.of());

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.createTest(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void createTest_throwsWhenNameIsBlank() {
        ABTestRequestDto request = new ABTestRequestDto(
                "", "A", "B", null, List.of());

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.createTest(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void createTest_throwsWhenVariantAIsNull() {
        ABTestRequestDto request = new ABTestRequestDto(
                "Test", null, "B", null, List.of());

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.createTest(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void createTest_throwsWhenVariantAIsBlank() {
        ABTestRequestDto request = new ABTestRequestDto(
                "Test", "", "B", null, List.of());

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.createTest(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void createTest_throwsWhenVariantBIsNull() {
        ABTestRequestDto request = new ABTestRequestDto(
                "Test", "A", null, null, List.of());

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.createTest(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void createTest_throwsWhenVariantBIsBlank() {
        ABTestRequestDto request = new ABTestRequestDto(
                "Test", "A", "", null, List.of());

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.createTest(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void createTest_throwsWhenPlatformIsInvalid() {
        ABTestRequestDto request = new ABTestRequestDto(
                "Test", "A", "B", null, List.of("INVALID_PLATFORM"));

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.createTest(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void createTest_acceptsNullPlatforms() {
        ABTestRequestDto request = new ABTestRequestDto(
                "Test", "A", "B", null, null);

        ABTestDto result = abTestUseCase.createTest(userId, request);

        assertNotNull(result);
        assertTrue(result.platforms().isEmpty());
    }

    @Test
    void listTests_returnsTestsForUser() {
        ABTestRequestDto request1 = new ABTestRequestDto(
                "Test 1", "A1", "B1", null, List.of());
        ABTestRequestDto request2 = new ABTestRequestDto(
                "Test 2", "A2", "B2", null, List.of());
        abTestUseCase.createTest(userId, request1);
        abTestUseCase.createTest(userId, request2);

        List<ABTestDto> results = abTestUseCase.listTests(userId);

        assertEquals(2, results.size());
    }

    @Test
    void listTests_returnsEmptyForUserWithNoTests() {
        UUID otherUserId = UUID.randomUUID();

        List<ABTestDto> results = abTestUseCase.listTests(otherUserId);

        assertTrue(results.isEmpty());
    }

    @Test
    void startTest_changesStatusToRunning() {
        ABTestRequestDto request = new ABTestRequestDto(
                "Test", "A", "B", null, List.of());
        ABTestDto created = abTestUseCase.createTest(userId, request);

        ABTestDto result = abTestUseCase.startTest(created.id(), userId);

        assertEquals("RUNNING", result.status());
        assertNotNull(result.startedAt());
    }

    @Test
    void startTest_throwsWhenTestNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.startTest(nonExistentId, userId));
        assertEquals("TEST_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void startTest_throwsWhenNotOwner() {
        ABTestRequestDto request = new ABTestRequestDto(
                "Test", "A", "B", null, List.of());
        ABTestDto created = abTestUseCase.createTest(userId, request);

        UUID otherUserId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.startTest(created.id(), otherUserId));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void startTest_throwsWhenStatusIsNotDraft() {
        ABTestRequestDto request = new ABTestRequestDto(
                "Test", "A", "B", null, List.of());
        ABTestDto created = abTestUseCase.createTest(userId, request);
        abTestUseCase.startTest(created.id(), userId);

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.startTest(created.id(), userId));
        assertEquals("INVALID_STATUS", exception.getErrorCode());
    }

    @Test
    void completeTest_changesStatusToCompleted() {
        ABTestRequestDto request = new ABTestRequestDto(
                "Test", "A", "B", null, List.of());
        ABTestDto created = abTestUseCase.createTest(userId, request);
        abTestUseCase.startTest(created.id(), userId);

        ABTestDto result = abTestUseCase.completeTest(created.id(), userId, "A");

        assertEquals("COMPLETED", result.status());
        assertEquals("A", result.winnerVariant());
        assertNotNull(result.completedAt());
    }

    @Test
    void completeTest_throwsWhenTestNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.completeTest(nonExistentId, userId, "A"));
        assertEquals("TEST_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void completeTest_throwsWhenNotOwner() {
        ABTestRequestDto request = new ABTestRequestDto(
                "Test", "A", "B", null, List.of());
        ABTestDto created = abTestUseCase.createTest(userId, request);

        UUID otherUserId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.completeTest(created.id(), otherUserId, "A"));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void completeTest_throwsWhenAlreadyCompleted() {
        ABTestRequestDto request = new ABTestRequestDto(
                "Test", "A", "B", null, List.of());
        ABTestDto created = abTestUseCase.createTest(userId, request);
        abTestUseCase.startTest(created.id(), userId);
        abTestUseCase.completeTest(created.id(), userId, "A");

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.completeTest(created.id(), userId, "B"));
        assertEquals("ALREADY_COMPLETED", exception.getErrorCode());
    }

    @Test
    void completeTest_throwsWhenWinnerVariantIsNull() {
        ABTestRequestDto request = new ABTestRequestDto(
                "Test", "A", "B", null, List.of());
        ABTestDto created = abTestUseCase.createTest(userId, request);
        abTestUseCase.startTest(created.id(), userId);

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.completeTest(created.id(), userId, null));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void completeTest_throwsWhenWinnerVariantIsBlank() {
        ABTestRequestDto request = new ABTestRequestDto(
                "Test", "A", "B", null, List.of());
        ABTestDto created = abTestUseCase.createTest(userId, request);
        abTestUseCase.startTest(created.id(), userId);

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.completeTest(created.id(), userId, ""));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void cancelTest_changesStatusToCancelled() {
        ABTestRequestDto request = new ABTestRequestDto(
                "Test", "A", "B", null, List.of());
        ABTestDto created = abTestUseCase.createTest(userId, request);
        abTestUseCase.startTest(created.id(), userId);

        ABTestDto result = abTestUseCase.cancelTest(created.id(), userId);

        assertEquals("CANCELLED", result.status());
        assertNotNull(result.completedAt());
    }

    @Test
    void cancelTest_throwsWhenTestNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.cancelTest(nonExistentId, userId));
        assertEquals("TEST_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void cancelTest_throwsWhenNotOwner() {
        ABTestRequestDto request = new ABTestRequestDto(
                "Test", "A", "B", null, List.of());
        ABTestDto created = abTestUseCase.createTest(userId, request);
        abTestUseCase.startTest(created.id(), userId);

        UUID otherUserId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.cancelTest(created.id(), otherUserId));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void cancelTest_throwsWhenStatusIsNotRunning() {
        ABTestRequestDto request = new ABTestRequestDto(
                "Test", "A", "B", null, List.of());
        ABTestDto created = abTestUseCase.createTest(userId, request);

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.cancelTest(created.id(), userId));
        assertEquals("INVALID_STATUS", exception.getErrorCode());
    }

    @Test
    void deleteTest_removesTest() {
        ABTestRequestDto request = new ABTestRequestDto(
                "Test", "A", "B", null, List.of());
        ABTestDto created = abTestUseCase.createTest(userId, request);

        abTestUseCase.deleteTest(created.id(), userId);

        assertTrue(abTestUseCase.listTests(userId).isEmpty());
    }

    @Test
    void deleteTest_throwsWhenTestNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.deleteTest(nonExistentId, userId));
        assertEquals("TEST_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void deleteTest_throwsWhenNotOwner() {
        ABTestRequestDto request = new ABTestRequestDto(
                "Test", "A", "B", null, List.of());
        ABTestDto created = abTestUseCase.createTest(userId, request);

        UUID otherUserId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.deleteTest(created.id(), otherUserId));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void generateVariants_returnsGeneratedVariants() {
        aiTextGenerator.nextResponse = new AiResponse(
                "{\"variantB\":\"casual text\",\"variantBLabel\":\"Casual\"," +
                        "\"variantBDiff\":\"more casual tone\"," +
                        "\"variantC\":\"formal text\",\"variantCLabel\":\"Formal\"," +
                        "\"variantCDiff\":\"more formal tone\"}",
                100, 500, "test-model");

        ABTestVariantGenerateRequestDto request =
                new ABTestVariantGenerateRequestDto("original text", null);

        ABTestVariantGenerateResponseDto result =
                abTestUseCase.generateVariants(userId, request);

        assertNotNull(result);
        assertEquals("original text", result.variantA());
        assertEquals("casual text", result.variantB());
        assertEquals("formal text", result.variantC());
    }

    @Test
    void generateVariants_throwsWhenOriginalTextIsNull() {
        ABTestVariantGenerateRequestDto request =
                new ABTestVariantGenerateRequestDto(null, null);

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.generateVariants(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void generateVariants_throwsWhenOriginalTextIsBlank() {
        ABTestVariantGenerateRequestDto request =
                new ABTestVariantGenerateRequestDto("", null);

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.generateVariants(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void generateVariants_usesPersonaWhenProvided() {
        Instant now = Instant.now();
        UUID personaId = UUID.randomUUID();
        AiPersona persona = new AiPersona(personaId, userId, "TestPersona", "system prompt",
                ContentTone.CASUAL, "ja", null, false, now, now);
        aiPersonaRepository.save(persona);

        aiTextGenerator.nextPersonaResponse = new AiResponse(
                "{\"variantB\":\"persona B\",\"variantBLabel\":\"Label B\"," +
                        "\"variantBDiff\":\"diff B\"," +
                        "\"variantC\":\"persona C\",\"variantCLabel\":\"Label C\"," +
                        "\"variantCDiff\":\"diff C\"}",
                100, 500, "test-model");

        ABTestVariantGenerateRequestDto request =
                new ABTestVariantGenerateRequestDto("original", personaId);

        ABTestVariantGenerateResponseDto result =
                abTestUseCase.generateVariants(userId, request);

        assertEquals("persona B", result.variantB());
    }

    @Test
    void generateVariants_throwsWhenPersonaNotFound() {
        UUID nonExistentPersonaId = UUID.randomUUID();
        ABTestVariantGenerateRequestDto request =
                new ABTestVariantGenerateRequestDto("text", nonExistentPersonaId);

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.generateVariants(userId, request));
        assertEquals("PERSONA_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void generateVariants_throwsWhenPersonaNotOwnedByUser() {
        Instant now = Instant.now();
        UUID personaId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        AiPersona persona = new AiPersona(personaId, otherUserId, "Other", "prompt",
                ContentTone.FORMAL, "en", null, false, now, now);
        aiPersonaRepository.save(persona);

        ABTestVariantGenerateRequestDto request =
                new ABTestVariantGenerateRequestDto("text", personaId);

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.generateVariants(userId, request));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void generateVariants_throwsWhenAiReturnsInvalidJson() {
        aiTextGenerator.nextResponse = new AiResponse(
                "not json at all", 100, 500, "test-model");

        ABTestVariantGenerateRequestDto request =
                new ABTestVariantGenerateRequestDto("text", null);

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.generateVariants(userId, request));
        assertEquals("AI_PARSE_ERROR", exception.getErrorCode());
    }

    @Test
    void predictOutcome_returnsPrediction() {
        ABTestRequestDto createRequest = new ABTestRequestDto(
                "Test", "A", "B", null, List.of());
        ABTestDto created = abTestUseCase.createTest(userId, createRequest);

        aiTextGenerator.nextResponse = new AiResponse(
                "{\"totalImpressions\":1000,\"totalEngagements\":100," +
                        "\"averageEngagementRate\":10.0," +
                        "\"variantStats\":[{\"variant\":\"A\",\"engagementRate\":12.0,\"clickRate\":5.0}," +
                        "{\"variant\":\"B\",\"engagementRate\":8.0,\"clickRate\":3.0}]," +
                        "\"confidencePercent\":85.0,\"aiVerdict\":\"Variant A is better\"}",
                200, 1000, "test-model");

        var result = abTestUseCase.predictOutcome(created.id(), userId);

        assertNotNull(result);
        assertEquals(1000, result.totalImpressions());
        assertEquals(85.0, result.confidencePercent());
    }

    @Test
    void predictOutcome_throwsWhenTestNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.predictOutcome(nonExistentId, userId));
        assertEquals("TEST_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void predictOutcome_throwsWhenNotOwner() {
        ABTestRequestDto request = new ABTestRequestDto(
                "Test", "A", "B", null, List.of());
        ABTestDto created = abTestUseCase.createTest(userId, request);

        UUID otherUserId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.predictOutcome(created.id(), otherUserId));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void analyzeLoser_returnsAnalysisForCompletedTest() {
        ABTestRequestDto request = new ABTestRequestDto(
                "Test", "A", "B", null, List.of());
        ABTestDto created = abTestUseCase.createTest(userId, request);
        abTestUseCase.startTest(created.id(), userId);
        abTestUseCase.completeTest(created.id(), userId, "A");

        aiTextGenerator.nextResponse = new AiResponse(
                "{\"winFactors\":[\"better CTA\",\"shorter text\"]," +
                        "\"suggestedNextTestName\":\"Next Test\"," +
                        "\"suggestedNextTestText\":\"Try this\"," +
                        "\"aiSummary\":\"A won because of CTA\"}",
                150, 800, "test-model");

        var result = abTestUseCase.analyzeLoser(created.id(), userId);

        assertNotNull(result);
        assertEquals(2, result.winFactors().size());
        assertEquals("Next Test", result.suggestedNextTestName());
    }

    @Test
    void analyzeLoser_throwsWhenTestNotCompleted() {
        ABTestRequestDto request = new ABTestRequestDto(
                "Test", "A", "B", null, List.of());
        ABTestDto created = abTestUseCase.createTest(userId, request);
        abTestUseCase.startTest(created.id(), userId);

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.analyzeLoser(created.id(), userId));
        assertEquals("TEST_NOT_COMPLETED", exception.getErrorCode());
    }

    @Test
    void analyzeLoser_throwsWhenTestNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.analyzeLoser(nonExistentId, userId));
        assertEquals("TEST_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void analyzeLoser_throwsWhenNotOwner() {
        ABTestRequestDto request = new ABTestRequestDto(
                "Test", "A", "B", null, List.of());
        ABTestDto created = abTestUseCase.createTest(userId, request);
        abTestUseCase.startTest(created.id(), userId);
        abTestUseCase.completeTest(created.id(), userId, "A");

        UUID otherUserId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.analyzeLoser(created.id(), otherUserId));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void generateMultiPlatformVariants_returnsVariants() {
        aiTextGenerator.nextResponse = new AiResponse(
                "{\"variants\":[{\"platform\":\"TWITTER\",\"variant\":\"A\"," +
                        "\"preview\":\"Twitter optimized\",\"optimizationNote\":\"shortened\"}]}",
                100, 500, "test-model");

        ABTestMultiPlatformRequestDto request =
                new ABTestMultiPlatformRequestDto("Multi", "original text", List.of("TWITTER"));

        ABTestMultiPlatformResponseDto result =
                abTestUseCase.generateMultiPlatformVariants(userId, request);

        assertNotNull(result);
        assertEquals(1, result.variants().size());
        assertEquals("TWITTER", result.variants().get(0).platform());
    }

    @Test
    void generateMultiPlatformVariants_throwsWhenOriginalTextIsNull() {
        ABTestMultiPlatformRequestDto request =
                new ABTestMultiPlatformRequestDto("Multi", null, List.of("TWITTER"));

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.generateMultiPlatformVariants(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void generateMultiPlatformVariants_throwsWhenOriginalTextIsBlank() {
        ABTestMultiPlatformRequestDto request =
                new ABTestMultiPlatformRequestDto("Multi", "", List.of("TWITTER"));

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.generateMultiPlatformVariants(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void generateMultiPlatformVariants_throwsWhenPlatformsIsNull() {
        ABTestMultiPlatformRequestDto request =
                new ABTestMultiPlatformRequestDto("Multi", "text", null);

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.generateMultiPlatformVariants(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void generateMultiPlatformVariants_throwsWhenPlatformsIsEmpty() {
        ABTestMultiPlatformRequestDto request =
                new ABTestMultiPlatformRequestDto("Multi", "text", List.of());

        DomainException exception = assertThrows(DomainException.class,
                () -> abTestUseCase.generateMultiPlatformVariants(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void analyzeWinPatterns_returnsEmptyWhenNoCompletedTests() {
        ABTestWinPatternDto result = abTestUseCase.analyzeWinPatterns(userId);

        assertNotNull(result);
        assertEquals(0, result.totalTestsAnalyzed());
        assertTrue(result.patterns().isEmpty());
    }

    @Test
    void analyzeWinPatterns_returnsAnalysisForCompletedTests() {
        ABTestRequestDto request = new ABTestRequestDto(
                "Test", "A", "B", null, List.of());
        ABTestDto created = abTestUseCase.createTest(userId, request);
        abTestUseCase.startTest(created.id(), userId);
        abTestUseCase.completeTest(created.id(), userId, "A");

        aiTextGenerator.nextResponse = new AiResponse(
                "{\"patterns\":[{\"patternName\":\"CTA\",\"winCount\":1," +
                        "\"winRate\":100.0,\"description\":\"Strong CTA wins\"}]," +
                        "\"aiSummary\":\"CTA is key\"}",
                150, 800, "test-model");

        ABTestWinPatternDto result = abTestUseCase.analyzeWinPatterns(userId);

        assertEquals(1, result.totalTestsAnalyzed());
        assertEquals(1, result.patterns().size());
        assertEquals("CTA", result.patterns().get(0).patternName());
    }

    private static class InMemoryABTestRepository implements ABTestRepository {

        private final Map<UUID, ABTest> store = new HashMap<>();

        @Override
        public Optional<ABTest> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<ABTest> findByUserId(UUID userId) {
            return store.values().stream()
                    .filter(test -> test.getUserId().equals(userId))
                    .toList();
        }

        @Override
        public ABTest save(ABTest abTest) {
            store.put(abTest.getId(), abTest);
            return abTest;
        }

        @Override
        public void deleteById(UUID id) {
            store.remove(id);
        }

        @Override
        public List<ABTest> findByUserIdAndStatus(UUID userId, ABTestStatus status) {
            return store.values().stream()
                    .filter(test -> test.getUserId().equals(userId))
                    .filter(test -> test.getStatus() == status)
                    .toList();
        }

        @Override
        public List<ABTest> findCompletedByUserId(UUID userId) {
            return store.values().stream()
                    .filter(test -> test.getUserId().equals(userId))
                    .filter(test -> test.getStatus() == ABTestStatus.COMPLETED)
                    .toList();
        }
    }

    private static class StubAiTextGenerator implements AiTextGenerator {

        AiResponse nextResponse;
        AiResponse nextPersonaResponse;

        @Override
        public AiResponse generate(AiPrompt prompt) {
            return nextResponse;
        }

        @Override
        public AiResponse generateWithPersona(AiPersona persona, String userInput) {
            return nextPersonaResponse;
        }
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
                    .filter(persona -> persona.getUserId().equals(userId))
                    .toList();
        }

        @Override
        public Optional<AiPersona> findDefaultByUserId(UUID userId) {
            return store.values().stream()
                    .filter(persona -> persona.getUserId().equals(userId))
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
}
