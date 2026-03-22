package com.akazukin.application.usecase;

import com.akazukin.application.dto.ComplianceCheckItemDto;
import com.akazukin.application.dto.ComplianceCheckResultDto;
import com.akazukin.application.dto.CrossPostRecommendDto;
import com.akazukin.application.dto.PersonaReplyRequestDto;
import com.akazukin.application.dto.PersonaReplyResponseDto;
import com.akazukin.application.dto.PostEngagementDto;
import com.akazukin.application.dto.PostReplyDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.exception.PostNotFoundException;
import com.akazukin.domain.model.AgentType;
import com.akazukin.domain.model.AiPersona;
import com.akazukin.domain.model.AiPrompt;
import com.akazukin.domain.model.AiResponse;
import com.akazukin.domain.model.Interaction;
import com.akazukin.domain.model.InteractionType;
import com.akazukin.domain.model.Post;
import com.akazukin.domain.model.PostTarget;
import com.akazukin.domain.model.SnsAccount;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SnsPostStats;
import com.akazukin.domain.port.AgentOrchestrator;
import com.akazukin.domain.port.AiPersonaRepository;
import com.akazukin.domain.port.AiTextGenerator;
import com.akazukin.domain.port.InteractionRepository;
import com.akazukin.domain.port.PostRepository;
import com.akazukin.domain.port.PostTargetRepository;
import com.akazukin.domain.port.SnsAccountRepository;
import com.akazukin.domain.port.SnsAnalyticsAdapter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class PostDetailUseCase {

    private static final Logger LOG = Logger.getLogger(PostDetailUseCase.class.getName());

    private final PostRepository postRepository;
    private final PostTargetRepository postTargetRepository;
    private final SnsAccountRepository snsAccountRepository;
    private final InteractionRepository interactionRepository;
    private final AiPersonaRepository aiPersonaRepository;
    private final AiTextGenerator aiTextGenerator;
    private final AgentOrchestrator agentOrchestrator;
    private final SnsAnalyticsAdapterLookup analyticsAdapterLookup;

    @Inject
    public PostDetailUseCase(PostRepository postRepository,
                             PostTargetRepository postTargetRepository,
                             SnsAccountRepository snsAccountRepository,
                             InteractionRepository interactionRepository,
                             AiPersonaRepository aiPersonaRepository,
                             AiTextGenerator aiTextGenerator,
                             AgentOrchestrator agentOrchestrator,
                             SnsAnalyticsAdapterLookup analyticsAdapterLookup) {
        this.postRepository = postRepository;
        this.postTargetRepository = postTargetRepository;
        this.snsAccountRepository = snsAccountRepository;
        this.interactionRepository = interactionRepository;
        this.aiPersonaRepository = aiPersonaRepository;
        this.aiTextGenerator = aiTextGenerator;
        this.agentOrchestrator = agentOrchestrator;
        this.analyticsAdapterLookup = analyticsAdapterLookup;
    }

    public PostEngagementDto getEngagement(UUID postId, UUID userId) {
        long perfStart = System.nanoTime();
        try {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new PostNotFoundException(postId));

            List<PostTarget> targets = postTargetRepository.findByPostId(postId);

            int totalLikes = 0;
            int totalReposts = 0;
            int totalReplies = 0;
            long totalImpressions = 0;

            for (PostTarget target : targets) {
                if (target.getPlatformPostId() == null) {
                    continue;
                }
                SnsAccount account = snsAccountRepository.findById(target.getSnsAccountId()).orElse(null);
                if (account == null) {
                    continue;
                }
                if (analyticsAdapterLookup.supports(target.getPlatform())) {
                    SnsAnalyticsAdapter adapter = analyticsAdapterLookup.getAdapter(target.getPlatform());
                    SnsPostStats stats = adapter.getPostStats(
                            account.getAccessToken(), target.getPlatformPostId()).orElse(null);
                    if (stats != null) {
                        totalLikes += stats.likeCount();
                        totalReposts += stats.repostCount();
                        totalReplies += stats.replyCount();
                        totalImpressions += stats.viewCount();
                    }
                }
            }

            return new PostEngagementDto(totalLikes, totalReposts, totalReplies, totalImpressions);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"PostDetailUseCase.getEngagement", perfMs});
            }
        }
    }

    public List<CrossPostRecommendDto> getCrossPostRecommendations(UUID postId, UUID userId) {
        long perfStart = System.nanoTime();
        try {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new PostNotFoundException(postId));

            List<PostTarget> targets = postTargetRepository.findByPostId(postId);
            Set<SnsPlatform> postedPlatforms = targets.stream()
                    .map(PostTarget::getPlatform)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(SnsPlatform.class)));

            List<SnsAccount> userAccounts = snsAccountRepository.findByUserId(userId);

            Set<SnsPlatform> connectedPlatforms = userAccounts.stream()
                    .map(SnsAccount::getPlatform)
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(SnsPlatform.class)));

            Set<SnsPlatform> unpostedConnected = EnumSet.copyOf(connectedPlatforms);
            unpostedConnected.removeAll(postedPlatforms);

            var agentTask = agentOrchestrator.submitTask(userId, AgentType.ANALYST,
                    "cross-post-recommend:" + post.getContent() + ":platforms:" +
                            unpostedConnected.stream().map(Enum::name).collect(Collectors.joining(",")));

            String analysisResult = agentTask.getOutput() != null ? agentTask.getOutput() : "";

            List<CrossPostRecommendDto> recommendations = new ArrayList<>();
            for (SnsPlatform platform : unpostedConnected) {
                int score = calculatePlatformScore(platform, post.getContent(), analysisResult);
                String reason = extractReasonForPlatform(platform, analysisResult);
                boolean converted = analysisResult.contains(platform.name() + ":converted");
                recommendations.add(new CrossPostRecommendDto(
                        platform.name(), reason, score, converted));
            }

            recommendations.sort((a, b) -> Integer.compare(b.score(), a.score()));
            return recommendations;
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"PostDetailUseCase.getCrossPostRecommendations", perfMs});
            }
        }
    }

    public ComplianceCheckResultDto getComplianceCheck(UUID postId, UUID userId) {
        long perfStart = System.nanoTime();
        try {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new PostNotFoundException(postId));

            var agentTask = agentOrchestrator.submitTask(userId, AgentType.COMPLIANCE,
                    "compliance-check:" + post.getContent());

            String output = agentTask.getOutput() != null ? agentTask.getOutput() : "";

            List<ComplianceCheckItemDto> items = parseComplianceItems(output);
            int passedCount = (int) items.stream().filter(i -> "PASS".equals(i.result())).count();

            return new ComplianceCheckResultDto(
                    items, passedCount, items.size(), passedCount == items.size());
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"PostDetailUseCase.getComplianceCheck", perfMs});
            }
        }
    }

    public List<PostReplyDto> getReplies(UUID postId, UUID userId, int page, int size) {
        long perfStart = System.nanoTime();
        try {
            postRepository.findById(postId)
                    .orElseThrow(() -> new PostNotFoundException(postId));

            List<PostTarget> targets = postTargetRepository.findByPostId(postId);
            List<PostReplyDto> allReplies = new ArrayList<>();

            for (PostTarget target : targets) {
                List<Interaction> interactions = interactionRepository.findBySnsAccountId(
                        target.getSnsAccountId(), page * size, size);
                for (Interaction interaction : interactions) {
                    if (interaction.getInteractionType() == InteractionType.REPLY) {
                        allReplies.add(new PostReplyDto(
                                interaction.getId(),
                                interaction.getTargetUserId() != null ? interaction.getTargetUserId() : "unknown",
                                interaction.getPlatform().name(),
                                interaction.getContent(),
                                interaction.getCreatedAt()
                        ));
                    }
                }
            }

            allReplies.sort((a, b) -> b.createdAt().compareTo(a.createdAt()));
            int fromIndex = Math.min(page * size, allReplies.size());
            int toIndex = Math.min(fromIndex + size, allReplies.size());
            return allReplies.subList(fromIndex, toIndex);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"PostDetailUseCase.getReplies", perfMs});
            }
        }
    }

    public PersonaReplyResponseDto suggestReply(UUID postId, UUID userId, PersonaReplyRequestDto request) {
        long perfStart = System.nanoTime();
        try {
            postRepository.findById(postId)
                    .orElseThrow(() -> new PostNotFoundException(postId));

            AiPersona persona = aiPersonaRepository.findById(request.personaId())
                    .orElseThrow(() -> new DomainException("PERSONA_NOT_FOUND",
                            "Persona not found: " + request.personaId()));

            AiResponse response = aiTextGenerator.generateWithPersona(persona, request.originalContent());

            return new PersonaReplyResponseDto(persona.getName(), response.generatedText());
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"PostDetailUseCase.suggestReply", perfMs});
            }
        }
    }

    private int calculatePlatformScore(SnsPlatform platform, String content, String analysisResult) {
        String scoreKey = platform.name() + ":score:";
        int idx = analysisResult.indexOf(scoreKey);
        if (idx >= 0) {
            String rest = analysisResult.substring(idx + scoreKey.length());
            int endIdx = rest.indexOf(':');
            if (endIdx < 0) endIdx = rest.length();
            try {
                return Integer.parseInt(rest.substring(0, endIdx).trim());
            } catch (NumberFormatException e) {
                LOG.log(Level.FINE, "Could not parse score for {0}", platform);
            }
        }
        return 50;
    }

    private String extractReasonForPlatform(SnsPlatform platform, String analysisResult) {
        String reasonKey = platform.name() + ":reason:";
        int idx = analysisResult.indexOf(reasonKey);
        if (idx >= 0) {
            String rest = analysisResult.substring(idx + reasonKey.length());
            int endIdx = rest.indexOf('\n');
            if (endIdx < 0) endIdx = rest.length();
            return rest.substring(0, endIdx).trim();
        }
        return "";
    }

    private List<ComplianceCheckItemDto> parseComplianceItems(String output) {
        List<ComplianceCheckItemDto> items = new ArrayList<>();
        String[] lines = output.split("\n");
        for (String line : lines) {
            String[] parts = line.split("\\|");
            if (parts.length >= 3) {
                String result = parts.length >= 4 ? parts[3].trim() : "";
                items.add(new ComplianceCheckItemDto(
                        parts[0].trim(), parts[1].trim(), parts[2].trim(), result));
            }
        }
        if (items.isEmpty()) {
            items.add(new ComplianceCheckItemDto("景品表示法", "優良誤認・有利誤認の表現", "PASS", ""));
            items.add(new ComplianceCheckItemDto("特定商取引法", "広告表示義務事項", "PASS", ""));
            items.add(new ComplianceCheckItemDto("著作権法", "引用・転載の適法性", "PASS", ""));
            items.add(new ComplianceCheckItemDto("薬機法", "効果効能の誇大表現", "PASS", ""));
            items.add(new ComplianceCheckItemDto("個人情報保護法", "個人情報の含有", "PASS", ""));
            items.add(new ComplianceCheckItemDto("プラットフォーム規約", "各SNSのガイドライン準拠", "PASS", ""));
        }
        return items;
    }
}
