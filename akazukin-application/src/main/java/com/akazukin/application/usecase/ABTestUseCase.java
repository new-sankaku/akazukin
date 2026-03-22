package com.akazukin.application.usecase;

import com.akazukin.application.dto.ABTestDto;
import com.akazukin.application.dto.ABTestLoserAnalysisDto;
import com.akazukin.application.dto.ABTestMultiPlatformRequestDto;
import com.akazukin.application.dto.ABTestMultiPlatformResponseDto;
import com.akazukin.application.dto.ABTestPredictionDto;
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
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.ABTestRepository;
import com.akazukin.domain.port.AiPersonaRepository;
import com.akazukin.domain.port.AiTextGenerator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class ABTestUseCase {

    private static final Logger LOG = Logger.getLogger(ABTestUseCase.class.getName());

    private final ABTestRepository abTestRepository;
    private final AiTextGenerator aiTextGenerator;
    private final AiPersonaRepository aiPersonaRepository;

    @Inject
    public ABTestUseCase(ABTestRepository abTestRepository,
                         AiTextGenerator aiTextGenerator,
                         AiPersonaRepository aiPersonaRepository) {
        this.abTestRepository = abTestRepository;
        this.aiTextGenerator = aiTextGenerator;
        this.aiPersonaRepository = aiPersonaRepository;
    }

    @Transactional
    public ABTestDto createTest(UUID userId, ABTestRequestDto request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new DomainException("INVALID_INPUT", "Test name is required");
        }
        if (request.variantA() == null || request.variantA().isBlank()) {
            throw new DomainException("INVALID_INPUT", "Variant A is required");
        }
        if (request.variantB() == null || request.variantB().isBlank()) {
            throw new DomainException("INVALID_INPUT", "Variant B is required");
        }

        List<SnsPlatform> platforms = parsePlatforms(request.platforms());

        Instant now = Instant.now();
        ABTest test = new ABTest(
                UUID.randomUUID(),
                userId,
                request.name(),
                request.variantA(),
                request.variantB(),
                request.variantC(),
                ABTestStatus.DRAFT,
                null,
                null,
                null,
                platforms,
                now
        );

        ABTest saved = abTestRepository.save(test);
        LOG.log(Level.INFO, "A/B test created: {0} for user {1}",
                new Object[]{saved.getId(), userId});

        return toDto(saved);
    }

    public List<ABTestDto> listTests(UUID userId) {
        return abTestRepository.findByUserId(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ABTestDto completeTest(UUID testId, UUID userId, String winnerVariant) {
        ABTest test = abTestRepository.findById(testId)
                .orElseThrow(() -> new DomainException("TEST_NOT_FOUND",
                        "A/B test not found: " + testId));

        if (!test.getUserId().equals(userId)) {
            throw new DomainException("FORBIDDEN", "You do not own this A/B test");
        }

        if (test.getStatus() == ABTestStatus.COMPLETED) {
            throw new DomainException("ALREADY_COMPLETED", "This A/B test is already completed");
        }

        if (winnerVariant == null || winnerVariant.isBlank()) {
            throw new DomainException("INVALID_INPUT", "Winner variant is required");
        }

        test.setStatus(ABTestStatus.COMPLETED);
        test.setCompletedAt(Instant.now());
        test.setWinnerVariant(winnerVariant);

        ABTest saved = abTestRepository.save(test);
        LOG.log(Level.INFO, "A/B test completed: {0}, winner: {1}",
                new Object[]{testId, winnerVariant});

        return toDto(saved);
    }

    @Transactional
    public ABTestDto cancelTest(UUID testId, UUID userId) {
        ABTest test = abTestRepository.findById(testId)
                .orElseThrow(() -> new DomainException("TEST_NOT_FOUND",
                        "A/B test not found: " + testId));

        if (!test.getUserId().equals(userId)) {
            throw new DomainException("FORBIDDEN", "You do not own this A/B test");
        }

        if (test.getStatus() != ABTestStatus.RUNNING) {
            throw new DomainException("INVALID_STATUS", "Only running tests can be cancelled");
        }

        test.setStatus(ABTestStatus.CANCELLED);
        test.setCompletedAt(Instant.now());

        ABTest saved = abTestRepository.save(test);
        LOG.log(Level.INFO, "A/B test cancelled: {0}", testId);

        return toDto(saved);
    }

    @Transactional
    public void deleteTest(UUID testId, UUID userId) {
        ABTest test = abTestRepository.findById(testId)
                .orElseThrow(() -> new DomainException("TEST_NOT_FOUND",
                        "A/B test not found: " + testId));

        if (!test.getUserId().equals(userId)) {
            throw new DomainException("FORBIDDEN", "You do not own this A/B test");
        }

        abTestRepository.deleteById(testId);
        LOG.log(Level.INFO, "A/B test deleted: {0}", testId);
    }

    public ABTestVariantGenerateResponseDto generateVariants(UUID userId,
                                                              ABTestVariantGenerateRequestDto request) {
        if (request.originalText() == null || request.originalText().isBlank()) {
            throw new DomainException("INVALID_INPUT", "Original text is required");
        }

        String systemInstruction = buildVariantGenerationPrompt();
        String userInput = request.originalText();

        AiResponse response;
        if (request.personaId() != null) {
            AiPersona persona = aiPersonaRepository.findById(request.personaId())
                    .orElseThrow(() -> new DomainException("PERSONA_NOT_FOUND",
                            "AI persona not found: " + request.personaId()));
            if (!persona.getUserId().equals(userId)) {
                throw new DomainException("FORBIDDEN", "You do not own this persona");
            }
            response = aiTextGenerator.generateWithPersona(persona,
                    systemInstruction + "\n\n" + userInput);
        } else {
            AiPrompt prompt = new AiPrompt(systemInstruction, userInput, 0.8, 2000);
            response = aiTextGenerator.generate(prompt);
        }

        return parseVariantResponse(request.originalText(), response.generatedText());
    }

    public ABTestPredictionDto predictOutcome(UUID testId, UUID userId) {
        ABTest test = abTestRepository.findById(testId)
                .orElseThrow(() -> new DomainException("TEST_NOT_FOUND",
                        "A/B test not found: " + testId));

        if (!test.getUserId().equals(userId)) {
            throw new DomainException("FORBIDDEN", "You do not own this A/B test");
        }

        String prompt = "A/Bテストの統計分析を行ってください。\n" +
                "テスト名: " + test.getName() + "\n" +
                "バリアントA: " + test.getVariantA() + "\n" +
                "バリアントB: " + test.getVariantB() + "\n" +
                (test.getVariantC() != null ? "バリアントC: " + test.getVariantC() + "\n" : "") +
                "ステータス: " + test.getStatus().name() + "\n" +
                "開始日: " + (test.getStartedAt() != null ? test.getStartedAt().toString() : "未開始") + "\n\n" +
                "以下のJSON形式で回答してください:\n" +
                "{\"totalImpressions\":数値,\"totalEngagements\":数値,\"averageEngagementRate\":数値," +
                "\"variantStats\":[{\"variant\":\"A\",\"engagementRate\":数値,\"clickRate\":数値}," +
                "{\"variant\":\"B\",\"engagementRate\":数値,\"clickRate\":数値}]," +
                "\"confidencePercent\":数値,\"aiVerdict\":\"判定コメント\"}";

        AiPrompt aiPrompt = new AiPrompt(null, prompt, 0.3, 1500);
        AiResponse response = aiTextGenerator.generate(aiPrompt);

        return parsePredictionResponse(response.generatedText());
    }

    public ABTestLoserAnalysisDto analyzeLoser(UUID testId, UUID userId) {
        ABTest test = abTestRepository.findById(testId)
                .orElseThrow(() -> new DomainException("TEST_NOT_FOUND",
                        "A/B test not found: " + testId));

        if (!test.getUserId().equals(userId)) {
            throw new DomainException("FORBIDDEN", "You do not own this A/B test");
        }

        if (test.getStatus() != ABTestStatus.COMPLETED) {
            throw new DomainException("TEST_NOT_COMPLETED", "Test must be completed for loser analysis");
        }

        String prompt = "A/Bテストの勝因・敗因分析を行い、次のテスト提案をしてください。\n" +
                "テスト名: " + test.getName() + "\n" +
                "バリアントA: " + test.getVariantA() + "\n" +
                "バリアントB: " + test.getVariantB() + "\n" +
                (test.getVariantC() != null ? "バリアントC: " + test.getVariantC() + "\n" : "") +
                "勝者: バリアント" + test.getWinnerVariant() + "\n\n" +
                "以下のJSON形式で回答してください:\n" +
                "{\"winFactors\":[\"要因1\",\"要因2\",\"要因3\"]," +
                "\"suggestedNextTestName\":\"次回テスト名\"," +
                "\"suggestedNextTestText\":\"次回テスト用テキスト\"," +
                "\"aiSummary\":\"分析サマリー\"}";

        AiPrompt aiPrompt = new AiPrompt(null, prompt, 0.5, 1500);
        AiResponse response = aiTextGenerator.generate(aiPrompt);

        return parseLoserAnalysisResponse(response.generatedText());
    }

    public ABTestMultiPlatformResponseDto generateMultiPlatformVariants(
            UUID userId, ABTestMultiPlatformRequestDto request) {
        if (request.originalText() == null || request.originalText().isBlank()) {
            throw new DomainException("INVALID_INPUT", "Original text is required");
        }
        if (request.platforms() == null || request.platforms().isEmpty()) {
            throw new DomainException("INVALID_INPUT", "At least one platform is required");
        }

        String platformList = String.join(", ", request.platforms());
        String prompt = "以下のテキストを各SNSプラットフォームに最適化した2つのバリアント(A/B)を生成してください。\n" +
                "対象プラットフォーム: " + platformList + "\n" +
                "元テキスト: " + request.originalText() + "\n\n" +
                "以下のJSON形式で回答してください:\n" +
                "{\"variants\":[{\"platform\":\"TWITTER\",\"variant\":\"A\",\"preview\":\"最適化テキスト\"," +
                "\"optimizationNote\":\"最適化ポイント\"}]}";

        AiPrompt aiPrompt = new AiPrompt(null, prompt, 0.7, 3000);
        AiResponse response = aiTextGenerator.generate(aiPrompt);

        return parseMultiPlatformResponse(response.generatedText());
    }

    public ABTestWinPatternDto analyzeWinPatterns(UUID userId) {
        List<ABTest> completedTests = abTestRepository.findCompletedByUserId(userId);

        if (completedTests.isEmpty()) {
            return new ABTestWinPatternDto(0, Collections.emptyList(),
                    "No completed tests available for analysis");
        }

        StringBuilder testSummary = new StringBuilder();
        testSummary.append("以下の完了済みA/Bテスト結果を横断分析し、勝ちパターンを抽出してください。\n\n");
        for (ABTest t : completedTests) {
            testSummary.append("テスト: ").append(t.getName())
                    .append(" / 勝者: バリアント").append(t.getWinnerVariant())
                    .append(" / A: ").append(t.getVariantA())
                    .append(" / B: ").append(t.getVariantB());
            if (t.getVariantC() != null) {
                testSummary.append(" / C: ").append(t.getVariantC());
            }
            testSummary.append("\n");
        }
        testSummary.append("\n以下のJSON形式で回答してください:\n")
                .append("{\"patterns\":[{\"patternName\":\"パターン名\",\"winCount\":数値,")
                .append("\"winRate\":数値,\"description\":\"説明\"}],\"aiSummary\":\"総合分析\"}");

        AiPrompt aiPrompt = new AiPrompt(null, testSummary.toString(), 0.5, 2000);
        AiResponse response = aiTextGenerator.generate(aiPrompt);

        return parseWinPatternResponse(completedTests.size(), response.generatedText());
    }

    @Transactional
    public ABTestDto startTest(UUID testId, UUID userId) {
        ABTest test = abTestRepository.findById(testId)
                .orElseThrow(() -> new DomainException("TEST_NOT_FOUND",
                        "A/B test not found: " + testId));

        if (!test.getUserId().equals(userId)) {
            throw new DomainException("FORBIDDEN", "You do not own this A/B test");
        }

        if (test.getStatus() != ABTestStatus.DRAFT) {
            throw new DomainException("INVALID_STATUS", "Only draft tests can be started");
        }

        test.setStatus(ABTestStatus.RUNNING);
        test.setStartedAt(Instant.now());

        ABTest saved = abTestRepository.save(test);
        LOG.log(Level.INFO, "A/B test started: {0}", testId);

        return toDto(saved);
    }

    private ABTestDto toDto(ABTest test) {
        List<String> platformNames = test.getPlatforms() != null
                ? test.getPlatforms().stream().map(SnsPlatform::name).toList()
                : Collections.emptyList();

        return new ABTestDto(
                test.getId(),
                test.getName(),
                test.getVariantA(),
                test.getVariantB(),
                test.getVariantC(),
                test.getStatus().name(),
                test.getStartedAt(),
                test.getCompletedAt(),
                test.getWinnerVariant(),
                platformNames,
                test.getCreatedAt()
        );
    }

    private List<SnsPlatform> parsePlatforms(List<String> platformNames) {
        if (platformNames == null || platformNames.isEmpty()) {
            return Collections.emptyList();
        }
        List<SnsPlatform> result = new ArrayList<>();
        for (String name : platformNames) {
            try {
                result.add(SnsPlatform.valueOf(name.toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new DomainException("INVALID_INPUT", "Invalid platform: " + name);
            }
        }
        return result;
    }

    private String buildVariantGenerationPrompt() {
        return "A/Bテスト用のバリアントを生成してください。" +
                "元テキスト(バリアントA)をそのまま残し、" +
                "バリアントB(カジュアル文体)とバリアントC(フォーマル文体)を生成してください。\n" +
                "以下のJSON形式で回答してください:\n" +
                "{\"variantBLabel\":\"ラベル\",\"variantB\":\"テキスト\",\"variantBDiff\":\"変更ポイント\"," +
                "\"variantCLabel\":\"ラベル\",\"variantC\":\"テキスト\",\"variantCDiff\":\"変更ポイント\"}";
    }

    private ABTestVariantGenerateResponseDto parseVariantResponse(String originalText,
                                                                   String aiOutput) {
        String cleaned = extractJson(aiOutput);
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(cleaned);
            return new ABTestVariantGenerateResponseDto(
                    originalText,
                    "Original",
                    node.path("variantB").asText(),
                    node.path("variantBLabel").asText(),
                    node.path("variantBDiff").asText(),
                    node.path("variantC").asText(),
                    node.path("variantCLabel").asText(),
                    node.path("variantCDiff").asText()
            );
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to parse AI variant response: {0}", e.getMessage());
            throw new DomainException("AI_PARSE_ERROR",
                    "Failed to parse AI response for variant generation");
        }
    }

    private ABTestPredictionDto parsePredictionResponse(String aiOutput) {
        String cleaned = extractJson(aiOutput);
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(cleaned);

            List<ABTestPredictionDto.VariantStat> stats = new ArrayList<>();
            var variantStatsNode = node.path("variantStats");
            if (variantStatsNode.isArray()) {
                for (var statNode : variantStatsNode) {
                    stats.add(new ABTestPredictionDto.VariantStat(
                            statNode.path("variant").asText(),
                            statNode.path("engagementRate").asDouble(),
                            statNode.path("clickRate").asDouble()
                    ));
                }
            }

            return new ABTestPredictionDto(
                    node.path("totalImpressions").asLong(),
                    node.path("totalEngagements").asLong(),
                    node.path("averageEngagementRate").asDouble(),
                    stats,
                    node.path("confidencePercent").asDouble(),
                    node.path("aiVerdict").asText()
            );
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to parse AI prediction response: {0}", e.getMessage());
            throw new DomainException("AI_PARSE_ERROR",
                    "Failed to parse AI response for prediction");
        }
    }

    private ABTestLoserAnalysisDto parseLoserAnalysisResponse(String aiOutput) {
        String cleaned = extractJson(aiOutput);
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(cleaned);

            List<String> winFactors = new ArrayList<>();
            var factorsNode = node.path("winFactors");
            if (factorsNode.isArray()) {
                for (var factorNode : factorsNode) {
                    winFactors.add(factorNode.asText());
                }
            }

            return new ABTestLoserAnalysisDto(
                    winFactors,
                    node.path("suggestedNextTestName").asText(),
                    node.path("suggestedNextTestText").asText(),
                    node.path("aiSummary").asText()
            );
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to parse AI loser analysis response: {0}", e.getMessage());
            throw new DomainException("AI_PARSE_ERROR",
                    "Failed to parse AI response for loser analysis");
        }
    }

    private ABTestMultiPlatformResponseDto parseMultiPlatformResponse(String aiOutput) {
        String cleaned = extractJson(aiOutput);
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(cleaned);

            List<ABTestMultiPlatformResponseDto.PlatformVariant> variants = new ArrayList<>();
            var variantsNode = node.path("variants");
            if (variantsNode.isArray()) {
                for (var v : variantsNode) {
                    variants.add(new ABTestMultiPlatformResponseDto.PlatformVariant(
                            v.path("platform").asText(),
                            v.path("variant").asText(),
                            v.path("preview").asText(),
                            v.path("optimizationNote").asText()
                    ));
                }
            }

            return new ABTestMultiPlatformResponseDto(variants);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to parse AI multi-platform response: {0}", e.getMessage());
            throw new DomainException("AI_PARSE_ERROR",
                    "Failed to parse AI response for multi-platform optimization");
        }
    }

    private ABTestWinPatternDto parseWinPatternResponse(int totalTests, String aiOutput) {
        String cleaned = extractJson(aiOutput);
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            var node = mapper.readTree(cleaned);

            List<ABTestWinPatternDto.Pattern> patterns = new ArrayList<>();
            var patternsNode = node.path("patterns");
            if (patternsNode.isArray()) {
                for (var p : patternsNode) {
                    patterns.add(new ABTestWinPatternDto.Pattern(
                            p.path("patternName").asText(),
                            p.path("winCount").asInt(),
                            p.path("winRate").asDouble(),
                            p.path("description").asText()
                    ));
                }
            }

            return new ABTestWinPatternDto(
                    totalTests,
                    patterns,
                    node.path("aiSummary").asText()
            );
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Failed to parse AI win pattern response: {0}", e.getMessage());
            throw new DomainException("AI_PARSE_ERROR",
                    "Failed to parse AI response for win pattern analysis");
        }
    }

    private String extractJson(String raw) {
        if (raw == null) {
            throw new DomainException("AI_PARSE_ERROR", "AI returned null response");
        }
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end < 0 || end <= start) {
            throw new DomainException("AI_PARSE_ERROR", "No JSON found in AI response");
        }
        return raw.substring(start, end + 1);
    }
}
