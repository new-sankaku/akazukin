package com.akazukin.application.usecase;

import com.akazukin.application.dto.ApprovalDecisionDto;
import com.akazukin.application.dto.ApprovalRequestDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.exception.PostNotFoundException;
import com.akazukin.domain.model.ApprovalAction;
import com.akazukin.domain.model.ApprovalRequest;
import com.akazukin.domain.model.Notification;
import com.akazukin.domain.model.Post;
import com.akazukin.domain.model.PostStatus;
import com.akazukin.domain.model.Role;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.User;
import com.akazukin.domain.port.ApprovalRequestRepository;
import com.akazukin.domain.port.NotificationRepository;
import com.akazukin.domain.port.NotificationSender;
import com.akazukin.domain.port.PostPublisher;
import com.akazukin.domain.port.PostRepository;
import com.akazukin.domain.port.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApprovalUseCaseTest {

    private InMemoryApprovalRequestRepository approvalRequestRepository;
    private InMemoryPostRepository postRepository;
    private RecordingNotificationSender notificationSender;
    private InMemoryNotificationRepository notificationRepository;
    private RecordingPostPublisher postPublisher;
    private InMemoryUserRepository userRepository;
    private ApprovalUseCase approvalUseCase;

    private UUID requesterId;
    private UUID approverId;
    private UUID teamId;

    @BeforeEach
    void setUp() {
        approvalRequestRepository = new InMemoryApprovalRequestRepository();
        postRepository = new InMemoryPostRepository();
        notificationSender = new RecordingNotificationSender();
        notificationRepository = new InMemoryNotificationRepository();
        postPublisher = new RecordingPostPublisher();
        userRepository = new InMemoryUserRepository();
        approvalUseCase = new ApprovalUseCase(approvalRequestRepository, postRepository,
                notificationSender, notificationRepository, postPublisher, userRepository);

        requesterId = UUID.randomUUID();
        approverId = UUID.randomUUID();
        teamId = UUID.randomUUID();

        Instant now = Instant.now();
        User requester = new User(requesterId, "requester", "requester@example.com",
                "hashed:pass", Role.USER, now, now);
        User approver = new User(approverId, "approver", "approver@example.com",
                "hashed:pass", Role.USER, now, now);
        userRepository.save(requester);
        userRepository.save(approver);
    }

    @Test
    void submitForApproval_changesPostStatusToPendingApproval() {
        Post post = createDraftPost();

        approvalUseCase.submitForApproval(post.getId(), requesterId, approverId, teamId);

        Post updated = postRepository.findById(post.getId()).orElseThrow();
        assertEquals(PostStatus.PENDING_APPROVAL, updated.getStatus());
    }

    @Test
    void submitForApproval_createsApprovalRequest() {
        Post post = createDraftPost();

        approvalUseCase.submitForApproval(post.getId(), requesterId, approverId, teamId);

        List<ApprovalRequest> pending =
                approvalRequestRepository.findPendingByApproverId(approverId, 0, 10);
        assertEquals(1, pending.size());
        assertEquals(post.getId(), pending.get(0).getPostId());
        assertEquals(requesterId, pending.get(0).getRequesterId());
    }

    @Test
    void submitForApproval_sendsNotificationToApprover() {
        Post post = createDraftPost();

        approvalUseCase.submitForApproval(post.getId(), requesterId, approverId, teamId);

        assertEquals(1, notificationSender.sentNotifications.size());
        assertEquals(approverId, notificationSender.sentNotifications.get(0).getUserId());
    }

    @Test
    void submitForApproval_throwsWhenPostNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        assertThrows(PostNotFoundException.class,
                () -> approvalUseCase.submitForApproval(nonExistentId, requesterId, approverId, teamId));
    }

    @Test
    void submitForApproval_throwsWhenPostIsNotDraft() {
        Post post = createDraftPost();
        post.setStatus(PostStatus.PUBLISHED);
        postRepository.save(post);

        DomainException exception = assertThrows(DomainException.class,
                () -> approvalUseCase.submitForApproval(post.getId(), requesterId, approverId, teamId));
        assertEquals("INVALID_STATUS", exception.getErrorCode());
    }

    @Test
    void listPendingApprovals_returnsPendingApprovalsForApprover() {
        Post post1 = createDraftPost();
        Post post2 = createDraftPost();
        approvalUseCase.submitForApproval(post1.getId(), requesterId, approverId, teamId);
        approvalUseCase.submitForApproval(post2.getId(), requesterId, approverId, teamId);

        List<ApprovalRequestDto> results = approvalUseCase.listPendingApprovals(approverId, 0, 10);

        assertEquals(2, results.size());
    }

    @Test
    void listPendingApprovals_returnsEmptyWhenNoPending() {
        List<ApprovalRequestDto> results = approvalUseCase.listPendingApprovals(approverId, 0, 10);

        assertTrue(results.isEmpty());
    }

    @Test
    void listPendingApprovals_respectsPagination() {
        for (int i = 0; i < 5; i++) {
            Post post = createDraftPost();
            approvalUseCase.submitForApproval(post.getId(), requesterId, approverId, teamId);
        }

        List<ApprovalRequestDto> page0 = approvalUseCase.listPendingApprovals(approverId, 0, 2);
        List<ApprovalRequestDto> page1 = approvalUseCase.listPendingApprovals(approverId, 1, 2);

        assertEquals(2, page0.size());
        assertEquals(2, page1.size());
    }

    @Test
    void decide_approveSetsPostStatusToApprovedAndPublishes() {
        Post post = createDraftPost();
        approvalUseCase.submitForApproval(post.getId(), requesterId, approverId, teamId);

        ApprovalRequest approval =
                approvalRequestRepository.findPendingByApproverId(approverId, 0, 1).get(0);
        ApprovalDecisionDto decision = new ApprovalDecisionDto("APPROVE", "Looks good");

        approvalUseCase.decide(approval.getId(), approverId, decision);

        Post updated = postRepository.findById(post.getId()).orElseThrow();
        assertEquals(PostStatus.APPROVED, updated.getStatus());
        assertTrue(postPublisher.publishedPostIds.contains(post.getId()));
    }

    @Test
    void decide_rejectSetsPostStatusToRejected() {
        Post post = createDraftPost();
        approvalUseCase.submitForApproval(post.getId(), requesterId, approverId, teamId);

        ApprovalRequest approval =
                approvalRequestRepository.findPendingByApproverId(approverId, 0, 1).get(0);
        ApprovalDecisionDto decision = new ApprovalDecisionDto("REJECT", "Not suitable");

        approvalUseCase.decide(approval.getId(), approverId, decision);

        Post updated = postRepository.findById(post.getId()).orElseThrow();
        assertEquals(PostStatus.REJECTED, updated.getStatus());
    }

    @Test
    void decide_requestChangesSetsPostStatusToReturned() {
        Post post = createDraftPost();
        approvalUseCase.submitForApproval(post.getId(), requesterId, approverId, teamId);

        ApprovalRequest approval =
                approvalRequestRepository.findPendingByApproverId(approverId, 0, 1).get(0);
        ApprovalDecisionDto decision = new ApprovalDecisionDto("REQUEST_CHANGES", "Please fix typos");

        approvalUseCase.decide(approval.getId(), approverId, decision);

        Post updated = postRepository.findById(post.getId()).orElseThrow();
        assertEquals(PostStatus.RETURNED, updated.getStatus());
    }

    @Test
    void decide_sendsNotificationToRequester() {
        Post post = createDraftPost();
        approvalUseCase.submitForApproval(post.getId(), requesterId, approverId, teamId);
        notificationSender.sentNotifications.clear();

        ApprovalRequest approval =
                approvalRequestRepository.findPendingByApproverId(approverId, 0, 1).get(0);
        ApprovalDecisionDto decision = new ApprovalDecisionDto("APPROVE", null);

        approvalUseCase.decide(approval.getId(), approverId, decision);

        assertEquals(1, notificationSender.sentNotifications.size());
        assertEquals(requesterId, notificationSender.sentNotifications.get(0).getUserId());
    }

    @Test
    void decide_savesCommentOnApprovalRequest() {
        Post post = createDraftPost();
        approvalUseCase.submitForApproval(post.getId(), requesterId, approverId, teamId);

        ApprovalRequest approval =
                approvalRequestRepository.findPendingByApproverId(approverId, 0, 1).get(0);
        ApprovalDecisionDto decision = new ApprovalDecisionDto("APPROVE", "Well written");

        approvalUseCase.decide(approval.getId(), approverId, decision);

        ApprovalRequest updated = approvalRequestRepository.findById(approval.getId()).orElseThrow();
        assertEquals("Well written", updated.getComment());
        assertNotNull(updated.getDecidedAt());
    }

    @Test
    void decide_throwsWhenApprovalNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        ApprovalDecisionDto decision = new ApprovalDecisionDto("APPROVE", null);

        DomainException exception = assertThrows(DomainException.class,
                () -> approvalUseCase.decide(nonExistentId, approverId, decision));
        assertEquals("APPROVAL_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void decide_throwsWhenNotDesignatedApprover() {
        Post post = createDraftPost();
        approvalUseCase.submitForApproval(post.getId(), requesterId, approverId, teamId);

        ApprovalRequest approval =
                approvalRequestRepository.findPendingByApproverId(approverId, 0, 1).get(0);
        UUID otherUserId = UUID.randomUUID();
        ApprovalDecisionDto decision = new ApprovalDecisionDto("APPROVE", null);

        DomainException exception = assertThrows(DomainException.class,
                () -> approvalUseCase.decide(approval.getId(), otherUserId, decision));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void getApproval_returnsApprovalForDesignatedApprover() {
        Post post = createDraftPost();
        approvalUseCase.submitForApproval(post.getId(), requesterId, approverId, teamId);

        ApprovalRequest approval =
                approvalRequestRepository.findPendingByApproverId(approverId, 0, 1).get(0);

        ApprovalRequestDto result = approvalUseCase.getApproval(approval.getId(), approverId);

        assertNotNull(result);
        assertEquals(approval.getId(), result.id());
        assertEquals(post.getId(), result.postId());
        assertEquals("requester", result.requesterName());
    }

    @Test
    void getApproval_throwsWhenApprovalNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> approvalUseCase.getApproval(nonExistentId, approverId));
        assertEquals("APPROVAL_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void getApproval_throwsWhenNotDesignatedApprover() {
        Post post = createDraftPost();
        approvalUseCase.submitForApproval(post.getId(), requesterId, approverId, teamId);

        ApprovalRequest approval =
                approvalRequestRepository.findPendingByApproverId(approverId, 0, 1).get(0);
        UUID otherUserId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> approvalUseCase.getApproval(approval.getId(), otherUserId));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void countPending_returnsCorrectCount() {
        Post post1 = createDraftPost();
        Post post2 = createDraftPost();
        approvalUseCase.submitForApproval(post1.getId(), requesterId, approverId, teamId);
        approvalUseCase.submitForApproval(post2.getId(), requesterId, approverId, teamId);

        long count = approvalUseCase.countPending(approverId);

        assertEquals(2, count);
    }

    @Test
    void countPending_returnsZeroWhenNoPending() {
        long count = approvalUseCase.countPending(approverId);

        assertEquals(0, count);
    }

    private Post createDraftPost() {
        Instant now = Instant.now();
        Post post = new Post(UUID.randomUUID(), requesterId, "Test content",
                List.of(), PostStatus.DRAFT, null, now, now);
        return postRepository.save(post);
    }

    private static class InMemoryApprovalRequestRepository implements ApprovalRequestRepository {

        private final Map<UUID, ApprovalRequest> store = new HashMap<>();

        @Override
        public Optional<ApprovalRequest> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<ApprovalRequest> findByPostId(UUID postId) {
            return store.values().stream()
                    .filter(a -> a.getPostId().equals(postId))
                    .findFirst();
        }

        @Override
        public List<ApprovalRequest> findPendingByApproverId(UUID approverId, int offset, int limit) {
            List<ApprovalRequest> pending = store.values().stream()
                    .filter(a -> a.getApproverId().equals(approverId))
                    .filter(a -> a.getStatus() == null)
                    .toList();
            int end = Math.min(offset + limit, pending.size());
            if (offset >= pending.size()) {
                return List.of();
            }
            return new ArrayList<>(pending.subList(offset, end));
        }

        @Override
        public List<ApprovalRequest> findPendingByTeamId(UUID teamId, int offset, int limit) {
            List<ApprovalRequest> pending = store.values().stream()
                    .filter(a -> teamId.equals(a.getTeamId()))
                    .filter(a -> a.getStatus() == null)
                    .toList();
            int end = Math.min(offset + limit, pending.size());
            if (offset >= pending.size()) {
                return List.of();
            }
            return new ArrayList<>(pending.subList(offset, end));
        }

        @Override
        public ApprovalRequest save(ApprovalRequest approvalRequest) {
            store.put(approvalRequest.getId(), approvalRequest);
            return approvalRequest;
        }

        @Override
        public long countPendingByApproverId(UUID approverId) {
            return store.values().stream()
                    .filter(a -> a.getApproverId().equals(approverId))
                    .filter(a -> a.getStatus() == null)
                    .count();
        }

        @Override
        public long countByTeamIdAndStatus(UUID teamId, ApprovalAction status) {
            return store.values().stream()
                    .filter(a -> teamId.equals(a.getTeamId()))
                    .filter(a -> a.getStatus() == status)
                    .count();
        }

        @Override
        public long countByTeamIdAndStatusAndDecidedAfter(UUID teamId, ApprovalAction status, Instant after) {
            return store.values().stream()
                    .filter(a -> teamId.equals(a.getTeamId()))
                    .filter(a -> a.getStatus() == status)
                    .filter(a -> a.getDecidedAt() != null && a.getDecidedAt().isAfter(after))
                    .count();
        }

        @Override
        public List<ApprovalRequest> findByTeamIdAndStatus(UUID teamId, ApprovalAction status) {
            return store.values().stream()
                    .filter(a -> teamId.equals(a.getTeamId()))
                    .filter(a -> a.getStatus() == status)
                    .toList();
        }
    }

    private static class InMemoryPostRepository implements PostRepository {

        private final Map<UUID, Post> store = new HashMap<>();

        @Override
        public Optional<Post> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Post> findByUserId(UUID userId, int offset, int limit) {
            List<Post> userPosts = store.values().stream()
                    .filter(post -> post.getUserId().equals(userId))
                    .toList();
            int end = Math.min(offset + limit, userPosts.size());
            if (offset >= userPosts.size()) {
                return List.of();
            }
            return new ArrayList<>(userPosts.subList(offset, end));
        }

        @Override
        public List<Post> findScheduledBefore(Instant before) {
            return store.values().stream()
                    .filter(post -> post.getStatus() == PostStatus.SCHEDULED)
                    .filter(post -> post.getScheduledAt() != null && post.getScheduledAt().isBefore(before))
                    .toList();
        }

        @Override
        public Post save(Post post) {
            store.put(post.getId(), post);
            return post;
        }

        @Override
        public void deleteById(UUID id) {
            store.remove(id);
        }

        @Override
        public long countByUserId(UUID userId) {
            return store.values().stream()
                    .filter(post -> post.getUserId().equals(userId))
                    .count();
        }

        @Override
        public long countByUserIdAndStatus(UUID userId, PostStatus status) {
            return store.values().stream()
                    .filter(post -> post.getUserId().equals(userId))
                    .filter(post -> post.getStatus() == status)
                    .count();
        }

        @Override
        public Map<SnsPlatform, Long> countByUserIdGroupByPlatform(UUID userId) {
            return new java.util.EnumMap<>(SnsPlatform.class);
        }
    }

    private static class InMemoryNotificationRepository implements NotificationRepository {

        private final Map<UUID, Notification> store = new HashMap<>();

        @Override
        public Optional<Notification> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<Notification> findByUserId(UUID userId, int offset, int limit) {
            return store.values().stream()
                    .filter(n -> n.getUserId().equals(userId))
                    .toList();
        }

        @Override
        public List<Notification> findUnreadByUserId(UUID userId) {
            return store.values().stream()
                    .filter(n -> n.getUserId().equals(userId))
                    .filter(n -> !n.isRead())
                    .toList();
        }

        @Override
        public Notification save(Notification notification) {
            store.put(notification.getId(), notification);
            return notification;
        }

        @Override
        public void markAsRead(UUID id) {
            Notification n = store.get(id);
            if (n != null) {
                n.setRead(true);
            }
        }

        @Override
        public void markAllAsReadByUserId(UUID userId) {
            store.values().stream()
                    .filter(n -> n.getUserId().equals(userId))
                    .forEach(n -> n.setRead(true));
        }

        @Override
        public long countUnreadByUserId(UUID userId) {
            return store.values().stream()
                    .filter(n -> n.getUserId().equals(userId))
                    .filter(n -> !n.isRead())
                    .count();
        }
    }

    private static class RecordingNotificationSender implements NotificationSender {

        final List<Notification> sentNotifications = new ArrayList<>();

        @Override
        public void send(Notification notification) {
            sentNotifications.add(notification);
        }
    }

    private static class RecordingPostPublisher implements PostPublisher {

        final List<UUID> publishedPostIds = new ArrayList<>();
        final List<UUID> scheduledPostIds = new ArrayList<>();
        final List<UUID> cancelledPostIds = new ArrayList<>();

        @Override
        public void publishForProcessing(UUID postId) {
            publishedPostIds.add(postId);
        }

        @Override
        public void schedulePost(UUID postId, Instant scheduledAt) {
            scheduledPostIds.add(postId);
        }

        @Override
        public void cancelScheduledPost(UUID postId) {
            cancelledPostIds.add(postId);
        }
    }

    private static class InMemoryUserRepository implements UserRepository {

        private final Map<UUID, User> store = new HashMap<>();

        @Override
        public Optional<User> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return store.values().stream()
                    .filter(user -> user.getUsername().equals(username))
                    .findFirst();
        }

        @Override
        public Optional<User> findByEmail(String email) {
            return store.values().stream()
                    .filter(user -> user.getEmail().equals(email))
                    .findFirst();
        }

        @Override
        public List<User> findAllByIds(Collection<UUID> ids) {
            return ids.stream()
                    .map(store::get)
                    .filter(user -> user != null)
                    .toList();
        }

        @Override
        public User save(User user) {
            store.put(user.getId(), user);
            return user;
        }

        @Override
        public void deleteById(UUID id) {
            store.remove(id);
        }
    }
}
