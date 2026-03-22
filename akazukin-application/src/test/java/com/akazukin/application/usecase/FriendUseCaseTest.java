package com.akazukin.application.usecase;

import com.akazukin.application.dto.BubbleMapDto;
import com.akazukin.application.dto.FriendComposeRequestDto;
import com.akazukin.application.dto.FriendComposeResponseDto;
import com.akazukin.application.dto.FriendEngagementDto;
import com.akazukin.application.dto.FriendTargetDto;
import com.akazukin.application.dto.FriendTargetRequestDto;
import com.akazukin.application.dto.FriendTimelineResponseDto;
import com.akazukin.application.dto.RelationshipPlanDto;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
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

class FriendUseCaseTest {

    private InMemoryFriendTargetRepository friendTargetRepository;
    private InMemoryInteractionRepository interactionRepository;
    private StubAiTextGenerator aiTextGenerator;
    private FriendUseCase friendUseCase;

    private UUID userId;

    @BeforeEach
    void setUp() {
        friendTargetRepository = new InMemoryFriendTargetRepository();
        interactionRepository = new InMemoryInteractionRepository();
        aiTextGenerator = new StubAiTextGenerator();
        friendUseCase = new FriendUseCase(friendTargetRepository, interactionRepository, aiTextGenerator);

        userId = UUID.randomUUID();
    }

    @Test
    void listFriends_returnsEmptyListWhenNoFriends() {
        List<FriendTargetDto> result = friendUseCase.listFriends(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void listFriends_returnsAllFriendsForUser() {
        createFriend(userId, SnsPlatform.TWITTER, "@alice", "Alice");
        createFriend(userId, SnsPlatform.BLUESKY, "@bob", "Bob");

        List<FriendTargetDto> result = friendUseCase.listFriends(userId);

        assertEquals(2, result.size());
    }

    @Test
    void listFriends_doesNotReturnOtherUsersFriends() {
        createFriend(userId, SnsPlatform.TWITTER, "@alice", "Alice");
        UUID otherUserId = UUID.randomUUID();
        createFriend(otherUserId, SnsPlatform.TWITTER, "@bob", "Bob");

        List<FriendTargetDto> result = friendUseCase.listFriends(userId);

        assertEquals(1, result.size());
        assertEquals("@alice", result.get(0).targetIdentifier());
    }

    @Test
    void addFriend_createsNewFriend() {
        FriendTargetRequestDto request = new FriendTargetRequestDto(
                "TWITTER", "@alice", "Alice", "tech friend");

        FriendTargetDto result = friendUseCase.addFriend(userId, request);

        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals("TWITTER", result.platform());
        assertEquals("@alice", result.targetIdentifier());
        assertEquals("Alice", result.displayName());
        assertEquals("tech friend", result.notes());
    }

    @Test
    void addFriend_throwsInvalidPlatformForUnknownPlatform() {
        FriendTargetRequestDto request = new FriendTargetRequestDto(
                "UNKNOWN_PLATFORM", "@alice", "Alice", null);

        DomainException exception = assertThrows(DomainException.class,
                () -> friendUseCase.addFriend(userId, request));
        assertEquals("INVALID_PLATFORM", exception.getErrorCode());
    }

    @Test
    void addFriend_throwsInvalidRequestWhenTargetIdentifierIsNull() {
        FriendTargetRequestDto request = new FriendTargetRequestDto(
                "TWITTER", null, "Alice", null);

        DomainException exception = assertThrows(DomainException.class,
                () -> friendUseCase.addFriend(userId, request));
        assertEquals("INVALID_REQUEST", exception.getErrorCode());
    }

    @Test
    void addFriend_throwsInvalidRequestWhenTargetIdentifierIsBlank() {
        FriendTargetRequestDto request = new FriendTargetRequestDto(
                "TWITTER", "", "Alice", null);

        DomainException exception = assertThrows(DomainException.class,
                () -> friendUseCase.addFriend(userId, request));
        assertEquals("INVALID_REQUEST", exception.getErrorCode());
    }

    @Test
    void removeFriend_deletesExistingFriend() {
        FriendTarget friend = createFriend(userId, SnsPlatform.TWITTER, "@alice", "Alice");

        friendUseCase.removeFriend(friend.getId(), userId);

        assertTrue(friendTargetRepository.findById(friend.getId()).isEmpty());
    }

    @Test
    void removeFriend_throwsNotFoundForNonExistentFriend() {
        UUID nonExistentId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> friendUseCase.removeFriend(nonExistentId, userId));
        assertEquals("NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void removeFriend_throwsForbiddenWhenNotOwner() {
        FriendTarget friend = createFriend(userId, SnsPlatform.TWITTER, "@alice", "Alice");
        UUID otherUserId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> friendUseCase.removeFriend(friend.getId(), otherUserId));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void getEngagementRanking_returnsEmptyListWhenNoFriends() {
        List<FriendEngagementDto> result = friendUseCase.getEngagementRanking(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void getEngagementRanking_returnsRankedFriends() {
        FriendTarget alice = createFriend(userId, SnsPlatform.TWITTER, "@alice", "Alice");
        FriendTarget bob = createFriend(userId, SnsPlatform.TWITTER, "@bob", "Bob");

        Instant recent = Instant.now().minusSeconds(3600);
        addInteraction(userId, "@alice", InteractionType.REPLY, recent);
        addInteraction(userId, "@alice", InteractionType.LIKE, recent);

        List<FriendEngagementDto> result = friendUseCase.getEngagementRanking(userId);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).rank());
        assertEquals(2, result.get(1).rank());
    }

    @Test
    void getEngagementRanking_assignsCorrectPriorityLevels() {
        FriendTarget friend = createFriend(userId, SnsPlatform.TWITTER, "@alice", "Alice");

        List<FriendEngagementDto> result = friendUseCase.getEngagementRanking(userId);

        assertEquals(1, result.size());
        assertEquals("urgent", result.get(0).priorityLevel());
    }

    @Test
    void getTimeline_returnsTimelineForFriend() {
        FriendTarget friend = createFriend(userId, SnsPlatform.TWITTER, "@alice", "Alice");
        Instant now = Instant.now();
        addInteraction(userId, "@alice", InteractionType.REPLY, now);
        addInteraction(userId, "@alice", InteractionType.LIKE, now.minusSeconds(3600));

        FriendTimelineResponseDto result = friendUseCase.getTimeline(userId, friend.getId());

        assertNotNull(result);
        assertEquals(friend.getId(), result.friendId());
        assertEquals("Alice", result.displayName());
        assertEquals("TWITTER", result.platform());
        assertEquals(2, result.timeline().size());
    }

    @Test
    void getTimeline_throwsNotFoundForNonExistentFriend() {
        UUID nonExistentId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> friendUseCase.getTimeline(userId, nonExistentId));
        assertEquals("NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void getTimeline_throwsForbiddenWhenNotOwner() {
        FriendTarget friend = createFriend(userId, SnsPlatform.TWITTER, "@alice", "Alice");
        UUID otherUserId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> friendUseCase.getTimeline(otherUserId, friend.getId()));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void generateRelationshipPlan_returnsPlanForFriend() {
        FriendTarget friend = createFriend(userId, SnsPlatform.TWITTER, "@alice", "Alice");
        aiTextGenerator.nextResponse = "Monday|Like post|Show interest\nWednesday|Share content|Build rapport\n"
                + "Friday|Ask question|Start conversation\nSunday|Post related|Visibility";

        RelationshipPlanDto result = friendUseCase.generateRelationshipPlan(userId, friend.getId());

        assertNotNull(result);
        assertEquals(friend.getId(), result.friendId());
        assertEquals("Alice", result.displayName());
        assertFalse(result.actions().isEmpty());
    }

    @Test
    void generateRelationshipPlan_throwsNotFoundForNonExistentFriend() {
        UUID nonExistentId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> friendUseCase.generateRelationshipPlan(userId, nonExistentId));
        assertEquals("NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void generateRelationshipPlan_throwsForbiddenWhenNotOwner() {
        FriendTarget friend = createFriend(userId, SnsPlatform.TWITTER, "@alice", "Alice");
        UUID otherUserId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> friendUseCase.generateRelationshipPlan(otherUserId, friend.getId()));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void composeForFriend_returnsGeneratedMessage() {
        FriendTarget friend = createFriend(userId, SnsPlatform.TWITTER, "@alice", "Alice");
        aiTextGenerator.nextResponse = "Hey Alice, great post!";

        FriendComposeRequestDto request = new FriendComposeRequestDto(
                friend.getId(), "reply", "interesting article");

        FriendComposeResponseDto result = friendUseCase.composeForFriend(userId, request);

        assertNotNull(result);
        assertEquals(friend.getId(), result.friendId());
        assertEquals("Alice", result.displayName());
        assertEquals("Hey Alice, great post!", result.generatedText());
        assertNotNull(result.composerNote());
    }

    @Test
    void composeForFriend_throwsNotFoundForNonExistentFriend() {
        FriendComposeRequestDto request = new FriendComposeRequestDto(
                UUID.randomUUID(), "reply", null);

        DomainException exception = assertThrows(DomainException.class,
                () -> friendUseCase.composeForFriend(userId, request));
        assertEquals("NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void composeForFriend_throwsForbiddenWhenNotOwner() {
        FriendTarget friend = createFriend(userId, SnsPlatform.TWITTER, "@alice", "Alice");
        UUID otherUserId = UUID.randomUUID();

        FriendComposeRequestDto request = new FriendComposeRequestDto(
                friend.getId(), "reply", null);

        DomainException exception = assertThrows(DomainException.class,
                () -> friendUseCase.composeForFriend(otherUserId, request));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void getBubbleMap_returnsEmptyBubblesWhenNoFriends() {
        BubbleMapDto result = friendUseCase.getBubbleMap(userId);

        assertNotNull(result);
        assertTrue(result.bubbles().isEmpty());
        assertTrue(result.snsSummaries().isEmpty());
        assertTrue(result.snsAdvices().isEmpty());
    }

    @Test
    void getBubbleMap_returnsBubblesForAllFriends() {
        createFriend(userId, SnsPlatform.TWITTER, "@alice", "Alice");
        createFriend(userId, SnsPlatform.BLUESKY, "@bob", "Bob");

        BubbleMapDto result = friendUseCase.getBubbleMap(userId);

        assertEquals(2, result.bubbles().size());
        assertFalse(result.snsSummaries().isEmpty());
    }

    @Test
    void getBubbleMap_includesAdviceForAtRiskFriends() {
        createFriend(userId, SnsPlatform.TWITTER, "@alice", "Alice");

        BubbleMapDto result = friendUseCase.getBubbleMap(userId);

        assertEquals(1, result.bubbles().size());
        assertEquals("at_risk", result.bubbles().get(0).status());
        assertFalse(result.snsAdvices().isEmpty());
    }

    private FriendTarget createFriend(UUID ownerUserId, SnsPlatform platform,
                                      String targetIdentifier, String displayName) {
        FriendTarget friend = new FriendTarget(
                UUID.randomUUID(), ownerUserId, platform, targetIdentifier,
                displayName, null, Instant.now());
        return friendTargetRepository.save(friend);
    }

    private void addInteraction(UUID ownerUserId, String targetUserId,
                                InteractionType type, Instant createdAt) {
        Interaction interaction = new Interaction(
                UUID.randomUUID(), ownerUserId, UUID.randomUUID(), SnsPlatform.TWITTER,
                type, "post-1", targetUserId, "content", createdAt);
        interactionRepository.save(interaction);
    }

    private static class InMemoryFriendTargetRepository implements FriendTargetRepository {

        private final Map<UUID, FriendTarget> store = new HashMap<>();

        @Override
        public Optional<FriendTarget> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<FriendTarget> findByUserId(UUID userId) {
            return store.values().stream()
                    .filter(f -> f.getUserId().equals(userId))
                    .toList();
        }

        @Override
        public List<FriendTarget> findByUserIdAndPlatform(UUID userId, SnsPlatform platform) {
            return store.values().stream()
                    .filter(f -> f.getUserId().equals(userId))
                    .filter(f -> f.getPlatform() == platform)
                    .toList();
        }

        @Override
        public FriendTarget save(FriendTarget friendTarget) {
            if (friendTarget.getId() == null) {
                friendTarget.setId(UUID.randomUUID());
            }
            store.put(friendTarget.getId(), friendTarget);
            return friendTarget;
        }

        @Override
        public void deleteById(UUID id) {
            store.remove(id);
        }
    }

    private static class InMemoryInteractionRepository implements InteractionRepository {

        private final List<Interaction> store = new ArrayList<>();

        @Override
        public List<Interaction> findByUserId(UUID userId, int offset, int limit) {
            List<Interaction> filtered = store.stream()
                    .filter(i -> i.getUserId().equals(userId))
                    .toList();
            int end = Math.min(offset + limit, filtered.size());
            if (offset >= filtered.size()) {
                return List.of();
            }
            return new ArrayList<>(filtered.subList(offset, end));
        }

        @Override
        public List<Interaction> findBySnsAccountId(UUID snsAccountId, int offset, int limit) {
            return List.of();
        }

        @Override
        public List<Interaction> findByUserIdAndTargetUserId(UUID userId, String targetUserId, int offset, int limit) {
            List<Interaction> filtered = store.stream()
                    .filter(i -> i.getUserId().equals(userId))
                    .filter(i -> targetUserId.equals(i.getTargetUserId()))
                    .toList();
            int end = Math.min(offset + limit, filtered.size());
            if (offset >= filtered.size()) {
                return List.of();
            }
            return new ArrayList<>(filtered.subList(offset, end));
        }

        @Override
        public Interaction save(Interaction interaction) {
            store.add(interaction);
            return interaction;
        }

        @Override
        public long countByUserId(UUID userId) {
            return store.stream()
                    .filter(i -> i.getUserId().equals(userId))
                    .count();
        }

        @Override
        public long countByUserIdAndTargetUserId(UUID userId, String targetUserId) {
            return store.stream()
                    .filter(i -> i.getUserId().equals(userId))
                    .filter(i -> targetUserId.equals(i.getTargetUserId()))
                    .count();
        }
    }

    private static class StubAiTextGenerator implements AiTextGenerator {

        String nextResponse = "stub response";

        @Override
        public AiResponse generate(AiPrompt prompt) {
            return new AiResponse(nextResponse, 100, 50, "test-model");
        }

        @Override
        public AiResponse generateWithPersona(AiPersona persona, String userInput) {
            return new AiResponse(nextResponse, 100, 50, "test-model");
        }
    }
}
