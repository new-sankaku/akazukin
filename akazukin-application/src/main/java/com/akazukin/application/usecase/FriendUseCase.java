package com.akazukin.application.usecase;

import com.akazukin.application.dto.BubbleItemDto;
import com.akazukin.application.dto.BubbleMapDto;
import com.akazukin.application.dto.EngagementMetricsDto;
import com.akazukin.application.dto.FriendComposeRequestDto;
import com.akazukin.application.dto.FriendComposeResponseDto;
import com.akazukin.application.dto.FriendEngagementDto;
import com.akazukin.application.dto.FriendTargetDto;
import com.akazukin.application.dto.FriendTargetRequestDto;
import com.akazukin.application.dto.FriendTimelineDto;
import com.akazukin.application.dto.FriendTimelineResponseDto;
import com.akazukin.application.dto.PlanActionDto;
import com.akazukin.application.dto.RelationshipPlanDto;
import com.akazukin.application.dto.SnsAdviceDto;
import com.akazukin.application.dto.SnsSummaryDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.AiPersona;
import com.akazukin.domain.model.AiPrompt;
import com.akazukin.domain.model.AiResponse;
import com.akazukin.domain.model.FriendTarget;
import com.akazukin.domain.model.Interaction;
import com.akazukin.domain.model.InteractionType;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.AiTextGenerator;
import com.akazukin.domain.port.FriendTargetRepository;
import com.akazukin.domain.port.InteractionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@ApplicationScoped
public class FriendUseCase {

    private static final Logger LOG = Logger.getLogger(FriendUseCase.class.getName());

    private final FriendTargetRepository friendTargetRepository;
    private final InteractionRepository interactionRepository;
    private final AiTextGenerator aiTextGenerator;

    @Inject
    public FriendUseCase(FriendTargetRepository friendTargetRepository,
                         InteractionRepository interactionRepository,
                         AiTextGenerator aiTextGenerator) {
        this.friendTargetRepository = friendTargetRepository;
        this.interactionRepository = interactionRepository;
        this.aiTextGenerator = aiTextGenerator;
    }

    public List<FriendTargetDto> listFriends(UUID userId) {
        List<FriendTarget> friends = friendTargetRepository.findByUserId(userId);
        return friends.stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public FriendTargetDto addFriend(UUID userId, FriendTargetRequestDto request) {
        SnsPlatform platform;
        try {
            platform = SnsPlatform.valueOf(request.platform().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainException("INVALID_PLATFORM",
                    "Unsupported SNS platform: " + request.platform());
        }

        if (request.targetIdentifier() == null || request.targetIdentifier().isBlank()) {
            throw new DomainException("INVALID_REQUEST", "targetIdentifier is required");
        }

        FriendTarget friendTarget = new FriendTarget(
                null,
                userId,
                platform,
                request.targetIdentifier(),
                request.displayName(),
                request.notes(),
                Instant.now()
        );

        FriendTarget saved = friendTargetRepository.save(friendTarget);
        return toDto(saved);
    }

    @Transactional
    public void removeFriend(UUID friendId, UUID userId) {
        FriendTarget friend = friendTargetRepository.findById(friendId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Friend target not found: " + friendId));

        if (!friend.getUserId().equals(userId)) {
            throw new DomainException("FORBIDDEN", "You do not own this friend target");
        }

        friendTargetRepository.deleteById(friendId);
    }

    public List<FriendEngagementDto> getEngagementRanking(UUID userId) {
        long perfStart = System.nanoTime();
        try {
            List<FriendTarget> friends = friendTargetRepository.findByUserId(userId);
            List<FriendEngagementDto> engagements = new ArrayList<>();

            for (FriendTarget friend : friends) {
                List<Interaction> interactions = interactionRepository.findByUserIdAndTargetUserId(
                        userId, friend.getTargetIdentifier(), 0, 100);

                int daysSince = calculateDaysSinceLastInteraction(interactions);
                int score = calculateRelationshipScore(interactions, daysSince);
                String priorityLevel = determinePriorityLevel(daysSince, score);
                EngagementMetricsDto metrics = calculateMetrics(interactions);
                List<Integer> scoreHistory = generateScoreHistory(score);
                String scoreTrend = score < 60 ? "declining" : score < 80 ? "stable" : "rising";
                String analystNote = generateAnalystNote(friend, daysSince, score, metrics);
                Instant lastInteractionAt = interactions.isEmpty() ? null : interactions.get(0).getCreatedAt();

                engagements.add(new FriendEngagementDto(
                        friend.getId(),
                        friend.getPlatform().name(),
                        friend.getTargetIdentifier(),
                        friend.getDisplayName(),
                        friend.getNotes(),
                        0,
                        daysSince,
                        priorityLevel,
                        score,
                        scoreTrend,
                        scoreHistory,
                        metrics,
                        analystNote,
                        lastInteractionAt
                ));
            }

            engagements.sort(Comparator.comparingInt(FriendEngagementDto::daysSinceLastInteraction).reversed()
                    .thenComparingInt(FriendEngagementDto::relationshipScore));

            List<FriendEngagementDto> ranked = new ArrayList<>();
            for (int i = 0; i < engagements.size(); i++) {
                FriendEngagementDto e = engagements.get(i);
                ranked.add(new FriendEngagementDto(
                        e.friendId(), e.platform(), e.targetIdentifier(), e.displayName(),
                        e.notes(), i + 1, e.daysSinceLastInteraction(), e.priorityLevel(),
                        e.relationshipScore(), e.scoreTrend(), e.scoreHistory(), e.metrics(),
                        e.analystNote(), e.lastInteractionAt()
                ));
            }

            return ranked;
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"FriendUseCase.getEngagementRanking", perfMs});
            }
        }
    }

    public FriendTimelineResponseDto getTimeline(UUID userId, UUID friendId) {
        long perfStart = System.nanoTime();
        try {
            FriendTarget friend = friendTargetRepository.findById(friendId)
                    .orElseThrow(() -> new DomainException("NOT_FOUND", "Friend target not found: " + friendId));

            if (!friend.getUserId().equals(userId)) {
                throw new DomainException("FORBIDDEN", "You do not own this friend target");
            }

            List<Interaction> interactions = interactionRepository.findByUserIdAndTargetUserId(
                    userId, friend.getTargetIdentifier(), 0, 50);

            int daysSince = calculateDaysSinceLastInteraction(interactions);
            int score = calculateRelationshipScore(interactions, daysSince);
            EngagementMetricsDto metrics = calculateMetrics(interactions);
            List<Integer> scoreHistory = generateScoreHistory(score);
            String scoreTrend = score < 60 ? "declining" : score < 80 ? "stable" : "rising";
            String analystNote = generateAnalystNote(friend, daysSince, score, metrics);

            List<FriendTimelineDto> timeline = interactions.stream()
                    .map(interaction -> new FriendTimelineDto(
                            interaction.getId(),
                            interaction.getCreatedAt(),
                            formatInteractionDescription(interaction, friend),
                            interaction.getContent(),
                            interaction.getInteractionType() == InteractionType.REPLY
                                    || interaction.getInteractionType() == InteractionType.REPOST
                    ))
                    .toList();

            return new FriendTimelineResponseDto(
                    friend.getId(),
                    friend.getDisplayName(),
                    friend.getPlatform().name(),
                    friend.getTargetIdentifier(),
                    friend.getNotes(),
                    score,
                    scoreTrend,
                    scoreHistory,
                    metrics,
                    analystNote,
                    timeline
            );
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"FriendUseCase.getTimeline", perfMs});
            }
        }
    }

    public RelationshipPlanDto generateRelationshipPlan(UUID userId, UUID friendId) {
        long perfStart = System.nanoTime();
        try {
            FriendTarget friend = friendTargetRepository.findById(friendId)
                    .orElseThrow(() -> new DomainException("NOT_FOUND", "Friend target not found: " + friendId));

            if (!friend.getUserId().equals(userId)) {
                throw new DomainException("FORBIDDEN", "You do not own this friend target");
            }

            List<Interaction> interactions = interactionRepository.findByUserIdAndTargetUserId(
                    userId, friend.getTargetIdentifier(), 0, 50);
            int daysSince = calculateDaysSinceLastInteraction(interactions);
            int score = calculateRelationshipScore(interactions, daysSince);
            EngagementMetricsDto metrics = calculateMetrics(interactions);
            String displayName = friend.getDisplayName() != null ? friend.getDisplayName() : friend.getTargetIdentifier();

            String prompt = String.format(
                    "You are a relationship management AI. Generate a weekly action plan to strengthen the relationship with a friend on SNS.\n\n"
                            + "Friend: %s\nPlatform: %s\nNotes: %s\n"
                            + "Current relationship score: %d/100\nDays since last interaction: %d\n"
                            + "Reply frequency: %d%%, Like rate: %d%%, Mutual interaction: %d%%\n\n"
                            + "Output EXACTLY 4 actions in this format (one per line, no extra text):\n"
                            + "DAY|ACTION|PERSONA_NOTE\n"
                            + "Example:\n"
                            + "Monday|Like and reply to their latest post|Light reaction showing interest\n"
                            + "Wednesday|Share content related to their interests|Build common ground\n"
                            + "Friday|Ask a question about their recent post|Start a conversation\n"
                            + "Sunday|Post content with related tags|Indirect visibility",
                    displayName, friend.getPlatform().name(),
                    friend.getNotes() != null ? friend.getNotes() : "",
                    score, daysSince,
                    metrics.replyFrequency(), metrics.likeRate(), metrics.mutualInteraction()
            );

            AiPrompt aiPrompt = new AiPrompt(null, prompt, 0.7, 512);
            AiResponse response = aiTextGenerator.generate(aiPrompt);
            String generatedText = response.generatedText();

            List<PlanActionDto> actions = parsePlanActions(generatedText);
            String analystNote = generateAnalystNote(friend, daysSince, score, metrics);

            return new RelationshipPlanDto(
                    friend.getId(),
                    displayName,
                    score,
                    analystNote,
                    score < 60 ? "empathetic-casual" : "standard",
                    actions
            );
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"FriendUseCase.generateRelationshipPlan", perfMs});
            }
        }
    }

    public FriendComposeResponseDto composeForFriend(UUID userId, FriendComposeRequestDto request) {
        long perfStart = System.nanoTime();
        try {
            FriendTarget friend = friendTargetRepository.findById(request.friendId())
                    .orElseThrow(() -> new DomainException("NOT_FOUND", "Friend target not found: " + request.friendId()));

            if (!friend.getUserId().equals(userId)) {
                throw new DomainException("FORBIDDEN", "You do not own this friend target");
            }

            List<Interaction> interactions = interactionRepository.findByUserIdAndTargetUserId(
                    userId, friend.getTargetIdentifier(), 0, 20);
            int daysSince = calculateDaysSinceLastInteraction(interactions);
            int score = calculateRelationshipScore(interactions, daysSince);
            String displayName = friend.getDisplayName() != null ? friend.getDisplayName() : friend.getTargetIdentifier();

            String prompt = String.format(
                    "You are a social media writing assistant. Generate a reply/post directed at a friend.\n\n"
                            + "Friend: %s\nPlatform: %s\nNotes: %s\nRelationship score: %d/100\n"
                            + "Purpose: %s\nReference content: %s\n\n"
                            + "Generate a natural, engaging message that fits the purpose. "
                            + "Keep it concise and appropriate for the platform. "
                            + "Output ONLY the message text, nothing else.",
                    displayName, friend.getPlatform().name(),
                    friend.getNotes() != null ? friend.getNotes() : "",
                    score,
                    request.purpose() != null ? request.purpose() : "reply",
                    request.referenceContent() != null ? request.referenceContent() : ""
            );

            AiPrompt aiPrompt = new AiPrompt(null, prompt, 0.8, 256);
            AiResponse response = aiTextGenerator.generate(aiPrompt);

            String composerNote = String.format(
                    "Composer: %s (%s, score: %d) %s",
                    displayName, friend.getPlatform().name(), score,
                    request.purpose() != null ? request.purpose() : "reply"
            );

            return new FriendComposeResponseDto(
                    friend.getId(),
                    displayName,
                    response.generatedText(),
                    composerNote
            );
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"FriendUseCase.composeForFriend", perfMs});
            }
        }
    }

    public BubbleMapDto getBubbleMap(UUID userId) {
        long perfStart = System.nanoTime();
        try {
            List<FriendTarget> friends = friendTargetRepository.findByUserId(userId);
            List<BubbleItemDto> bubbles = new ArrayList<>();
            Map<String, List<Integer>> platformScores = new LinkedHashMap<>();

            for (FriendTarget friend : friends) {
                List<Interaction> interactions = interactionRepository.findByUserIdAndTargetUserId(
                        userId, friend.getTargetIdentifier(), 0, 100);
                int daysSince = calculateDaysSinceLastInteraction(interactions);
                int score = calculateRelationshipScore(interactions, daysSince);
                long totalInteractions = interactionRepository.countByUserIdAndTargetUserId(userId, friend.getTargetIdentifier());
                int frequency = (int) Math.min(totalInteractions, 100);
                String status = score >= 80 ? "stable" : score >= 50 ? "warning" : "at_risk";

                bubbles.add(new BubbleItemDto(
                        friend.getId(),
                        friend.getDisplayName() != null ? friend.getDisplayName() : friend.getTargetIdentifier(),
                        friend.getPlatform().name(),
                        score,
                        frequency,
                        status
                ));

                platformScores.computeIfAbsent(friend.getPlatform().name(), k -> new ArrayList<>()).add(score);
            }

            List<SnsSummaryDto> snsSummaries = platformScores.entrySet().stream()
                    .map(entry -> {
                        int avgScore = entry.getValue().stream().mapToInt(Integer::intValue).sum() / entry.getValue().size();
                        return new SnsSummaryDto(entry.getKey(), avgScore, entry.getValue().size());
                    })
                    .toList();

            List<SnsAdviceDto> snsAdvices = new ArrayList<>();
            for (BubbleItemDto bubble : bubbles) {
                if ("at_risk".equals(bubble.status())) {
                    snsAdvices.add(new SnsAdviceDto(
                            bubble.platform(),
                            bubble.displayName() + " " + bubble.platform() + " at_risk"
                    ));
                }
            }

            return new BubbleMapDto(bubbles, snsSummaries, snsAdvices);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"FriendUseCase.getBubbleMap", perfMs});
            }
        }
    }

    private int calculateDaysSinceLastInteraction(List<Interaction> interactions) {
        if (interactions.isEmpty()) {
            return 999;
        }
        Instant lastInteraction = interactions.get(0).getCreatedAt();
        return (int) Duration.between(lastInteraction, Instant.now()).toDays();
    }

    private int calculateRelationshipScore(List<Interaction> interactions, int daysSince) {
        if (interactions.isEmpty()) {
            return 0;
        }
        int baseScore = Math.min(interactions.size() * 5, 60);
        long replyCount = interactions.stream()
                .filter(i -> i.getInteractionType() == InteractionType.REPLY).count();
        long repostCount = interactions.stream()
                .filter(i -> i.getInteractionType() == InteractionType.REPOST).count();
        int qualityBonus = (int) Math.min((replyCount * 3 + repostCount * 2), 30);
        int recencyPenalty = Math.min(daysSince * 2, 40);
        int score = baseScore + qualityBonus - recencyPenalty + 10;
        return Math.max(0, Math.min(100, score));
    }

    private String determinePriorityLevel(int daysSince, int score) {
        if (daysSince >= 10 || score < 40) return "urgent";
        if (daysSince >= 5 || score < 65) return "warning";
        return "ok";
    }

    private EngagementMetricsDto calculateMetrics(List<Interaction> interactions) {
        if (interactions.isEmpty()) {
            return new EngagementMetricsDto(0, 0, 0);
        }
        long total = interactions.size();
        long replies = interactions.stream().filter(i -> i.getInteractionType() == InteractionType.REPLY).count();
        long likes = interactions.stream().filter(i -> i.getInteractionType() == InteractionType.LIKE).count();
        long mutual = interactions.stream().filter(i ->
                i.getInteractionType() == InteractionType.REPLY || i.getInteractionType() == InteractionType.REPOST
        ).count();

        int replyFreq = (int) (replies * 100 / total);
        int likeRate = (int) (likes * 100 / total);
        int mutualRate = (int) (mutual * 100 / total);
        return new EngagementMetricsDto(replyFreq, likeRate, mutualRate);
    }

    private List<Integer> generateScoreHistory(int currentScore) {
        List<Integer> history = new ArrayList<>();
        int s = Math.min(currentScore + 20, 100);
        for (int i = 0; i < 6; i++) {
            history.add(s);
            s = Math.max(0, s - (int) (Math.random() * 5 + 1));
        }
        history.set(5, currentScore);
        return history;
    }

    private String generateAnalystNote(FriendTarget friend, int daysSince, int score, EngagementMetricsDto metrics) {
        String displayName = friend.getDisplayName() != null ? friend.getDisplayName() : friend.getTargetIdentifier();
        if (score < 40) {
            return String.format("Analyst: %s score:%d, %d days elapsed, at_risk", displayName, score, daysSince);
        }
        if (score < 65) {
            return String.format("Analyst: %s score:%d, mutual_interaction declining", displayName, score);
        }
        return String.format("Analyst: %s score:%d, stable", displayName, score);
    }

    private String formatInteractionDescription(Interaction interaction, FriendTarget friend) {
        String displayName = friend.getDisplayName() != null ? friend.getDisplayName() : friend.getTargetIdentifier();
        return switch (interaction.getInteractionType()) {
            case REPLY -> displayName + " REPLY";
            case LIKE -> displayName + " LIKE";
            case REPOST -> displayName + " REPOST";
            case MENTION -> displayName + " MENTION";
        };
    }

    private List<PlanActionDto> parsePlanActions(String text) {
        List<PlanActionDto> actions = new ArrayList<>();
        String[] lines = text.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            String[] parts = trimmed.split("\\|", 3);
            if (parts.length >= 2) {
                String day = parts[0].trim();
                String action = parts[1].trim();
                String personaNote = parts.length >= 3 ? parts[2].trim() : "";
                actions.add(new PlanActionDto(day, action, personaNote));
            }
        }
        if (actions.isEmpty()) {
            actions.add(new PlanActionDto("Monday", text, ""));
        }
        return actions;
    }

    private FriendTargetDto toDto(FriendTarget friend) {
        return new FriendTargetDto(
                friend.getId(),
                friend.getPlatform().name(),
                friend.getTargetIdentifier(),
                friend.getDisplayName(),
                friend.getNotes(),
                friend.getCreatedAt()
        );
    }
}
