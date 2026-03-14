package com.akazukin.domain.service;

import com.akazukin.domain.model.ToneAnalysisResult;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JapaneseToneAnalyzer {

    private static final Pattern POLITE_ENDING = Pattern.compile("(です|ます|ました|ません|でしょう|ございます)");
    private static final Pattern FORMAL_ENDING = Pattern.compile("(である|であった|だった|だ。|だろう|ではない)");
    private static final Pattern CASUAL_INDICATOR = Pattern.compile("([wW]+|！{2,}|笑|www|ｗ+)");
    private static final Pattern KEIGO_PATTERN = Pattern.compile("(いただ[きけく]|くださ[いる]|おっしゃ[いる]|いらっしゃ[いる]|ご[覧確認検討依頼])");

    public ToneAnalysisResult analyze(String text) {
        if (text == null || text.isBlank()) {
            return new ToneAnalysisResult("UNKNOWN", 0.0, List.of("テキストが空です"));
        }

        int politeCount = countMatches(POLITE_ENDING, text);
        int formalCount = countMatches(FORMAL_ENDING, text);
        int casualCount = countMatches(CASUAL_INDICATOR, text);
        int keigoCount = countMatches(KEIGO_PATTERN, text);

        int totalIndicators = politeCount + formalCount + casualCount + keigoCount;
        if (totalIndicators == 0) {
            return new ToneAnalysisResult("NEUTRAL", 0.5, List.of("トーンの判定に十分な情報がありません"));
        }

        double formalityScore = calculateFormalityScore(politeCount, formalCount, casualCount, keigoCount);
        String toneLevel = determineToneLevel(formalityScore, keigoCount);
        List<String> suggestions = buildSuggestions(toneLevel, politeCount, formalCount, casualCount, keigoCount);

        return new ToneAnalysisResult(toneLevel, formalityScore, suggestions);
    }

    private int countMatches(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private double calculateFormalityScore(int politeCount, int formalCount, int casualCount, int keigoCount) {
        double score = 0.5;
        int total = politeCount + formalCount + casualCount + keigoCount;

        score += (politeCount * 0.3) / total;
        score += (formalCount * 0.2) / total;
        score += (keigoCount * 0.4) / total;
        score -= (casualCount * 0.5) / total;

        return Math.max(0.0, Math.min(1.0, score));
    }

    private String determineToneLevel(double formalityScore, int keigoCount) {
        if (keigoCount > 0 && formalityScore >= 0.8) {
            return "KEIGO";
        } else if (formalityScore >= 0.6) {
            return "TEINEIGO";
        } else if (formalityScore >= 0.4) {
            return "NEUTRAL";
        } else {
            return "TAMEGUCHI";
        }
    }

    private List<String> buildSuggestions(String toneLevel, int politeCount, int formalCount,
                                          int casualCount, int keigoCount) {
        List<String> suggestions = new ArrayList<>();

        switch (toneLevel) {
            case "KEIGO" -> suggestions.add("非常にフォーマルなトーンです。ビジネスコミュニケーションに適しています。");
            case "TEINEIGO" -> suggestions.add("丁寧なトーンです。一般的なSNS投稿に適しています。");
            case "NEUTRAL" -> suggestions.add("中立的なトーンです。フォーマルさを調整することを検討してください。");
            case "TAMEGUCHI" -> suggestions.add("カジュアルなトーンです。親しいフォロワー向けのコンテンツに適しています。");
            default -> suggestions.add("トーンの分析結果が不明です。");
        }

        if (casualCount > 0 && politeCount > 0) {
            suggestions.add("カジュアルな表現とフォーマルな表現が混在しています。統一することを推奨します。");
        }

        if (keigoCount == 0 && "TEINEIGO".equals(toneLevel)) {
            suggestions.add("より丁寧な印象を与えるには、敬語表現の追加を検討してください。");
        }

        return suggestions;
    }
}
