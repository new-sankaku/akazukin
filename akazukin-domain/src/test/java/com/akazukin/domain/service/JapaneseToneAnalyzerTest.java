package com.akazukin.domain.service;

import com.akazukin.domain.model.ToneAnalysisResult;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JapaneseToneAnalyzerTest {

    private JapaneseToneAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new JapaneseToneAnalyzer();
    }

    @Test
    void analyze_returnsUnknownForNullText() {
        ToneAnalysisResult result = analyzer.analyze(null);

        assertEquals("UNKNOWN", result.toneLevel());
        assertEquals(0.0, result.formalityScore());
    }

    @Test
    void analyze_returnsUnknownForEmptyText() {
        ToneAnalysisResult result = analyzer.analyze("");

        assertEquals("UNKNOWN", result.toneLevel());
    }

    @Test
    void analyze_returnsUnknownForBlankText() {
        ToneAnalysisResult result = analyzer.analyze("   ");

        assertEquals("UNKNOWN", result.toneLevel());
    }

    @Test
    void analyze_returnsNeutralForTextWithoutIndicators() {
        ToneAnalysisResult result = analyzer.analyze("今日は天気が良い");

        assertEquals("NEUTRAL", result.toneLevel());
        assertEquals(0.5, result.formalityScore());
    }

    @Test
    void analyze_detectsPoliteEndingsAsTeineigo() {
        ToneAnalysisResult result = analyzer.analyze("今日は天気が良いです。明日も晴れます。");

        assertEquals("TEINEIGO", result.toneLevel());
        assertTrue(result.formalityScore() >= 0.6);
    }

    @Test
    void analyze_detectsCasualIndicatorsAsTameguchi() {
        ToneAnalysisResult result = analyzer.analyze("やばいwww すごい！！笑");

        assertEquals("TAMEGUCHI", result.toneLevel());
        assertTrue(result.formalityScore() < 0.4);
    }

    @Test
    void analyze_detectsKeigoWithHighFormalityAsKeigo() {
        ToneAnalysisResult result = analyzer.analyze(
                "ご確認いただきありがとうございます。ご検討くださいますようお願い申し上げます。");

        assertEquals("KEIGO", result.toneLevel());
        assertTrue(result.formalityScore() >= 0.8);
    }

    @Test
    void analyze_returnsSuggestionsForMixedTone() {
        ToneAnalysisResult result = analyzer.analyze("ご確認ください！！www");

        assertNotNull(result.suggestions());
        assertFalse(result.suggestions().isEmpty());
    }

    @Test
    void analyze_suggestionsContainMixedWarningWhenCasualAndPoliteCoexist() {
        ToneAnalysisResult result = analyzer.analyze("今日は天気が良いです笑 明日も晴れますwww");

        boolean hasMixedWarning = result.suggestions().stream()
                .anyMatch(s -> s.contains("混在"));
        assertTrue(hasMixedWarning);
    }

    @Test
    void analyze_formalityScoreIsBetweenZeroAndOne() {
        ToneAnalysisResult result = analyzer.analyze("これはテストです。よろしくお願いいたします。");

        assertTrue(result.formalityScore() >= 0.0);
        assertTrue(result.formalityScore() <= 1.0);
    }

    @Test
    void analyze_detectsFormalEndingsCorrectly() {
        ToneAnalysisResult result = analyzer.analyze("これは事実である。問題ではない。");

        assertNotNull(result.toneLevel());
        assertFalse("UNKNOWN".equals(result.toneLevel()));
    }

    @Test
    void analyze_suggestionsListIsNeverNull() {
        ToneAnalysisResult result = analyzer.analyze(null);

        assertNotNull(result.suggestions());
    }

    @Test
    void analyze_teineigoSuggestsAddingKeigoWhenNoKeigoPresent() {
        ToneAnalysisResult result = analyzer.analyze("本日はお休みです。明日は出勤します。");

        if ("TEINEIGO".equals(result.toneLevel())) {
            boolean suggestsKeigo = result.suggestions().stream()
                    .anyMatch(s -> s.contains("敬語"));
            assertTrue(suggestsKeigo);
        }
    }
}
