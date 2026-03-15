package com.akazukin.application.usecase;

import com.akazukin.application.dto.ApprovalDecisionDto;
import com.akazukin.application.dto.ApprovalRequestDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.exception.PostNotFoundException;
import com.akazukin.domain.model.ApprovalAction;
import com.akazukin.domain.model.ApprovalRequest;
import com.akazukin.domain.model.Notification;
import com.akazukin.domain.model.NotificationType;
import com.akazukin.domain.model.Post;
import com.akazukin.domain.model.PostStatus;
import com.akazukin.domain.model.User;
import com.akazukin.domain.port.ApprovalRequestRepository;
import com.akazukin.domain.port.NotificationRepository;
import com.akazukin.domain.port.NotificationSender;
import com.akazukin.domain.port.PostPublisher;
import com.akazukin.domain.port.PostRepository;
import com.akazukin.domain.port.UserRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class ApprovalUseCase {

    private static final Logger LOG = Logger.getLogger(ApprovalUseCase.class.getName());

    private final ApprovalRequestRepository approvalRequestRepository;
    private final PostRepository postRepository;
    private final NotificationSender notificationSender;
    private final NotificationRepository notificationRepository;
    private final PostPublisher postPublisher;
    private final UserRepository userRepository;

    @Inject
    public ApprovalUseCase(ApprovalRequestRepository approvalRequestRepository,
                           PostRepository postRepository,
                           NotificationSender notificationSender,
                           NotificationRepository notificationRepository,
                           PostPublisher postPublisher,
                           UserRepository userRepository) {
        this.approvalRequestRepository = approvalRequestRepository;
        this.postRepository = postRepository;
        this.notificationSender = notificationSender;
        this.notificationRepository = notificationRepository;
        this.postPublisher = postPublisher;
        this.userRepository = userRepository;
    }

    @Transactional
    public void submitForApproval(UUID postId, UUID requesterId, UUID approverId, UUID teamId) {
        long perfStart = System.nanoTime();
        try {
            Post post = postRepository.findById(postId)
                    .orElseThrow(() -> new PostNotFoundException(postId));

            if (post.getStatus() != PostStatus.DRAFT) {
                throw new DomainException("INVALID_STATUS",
                        "Only draft posts can be submitted for approval");
            }

            post.setStatus(PostStatus.PENDING_APPROVAL);
            post.setUpdatedAt(Instant.now());
            postRepository.save(post);

            Instant now = Instant.now();
            ApprovalRequest approval = new ApprovalRequest(
                    UUID.randomUUID(),
                    postId,
                    requesterId,
                    approverId,
                    teamId,
                    null,
                    null,
                    now,
                    null
            );
            approvalRequestRepository.save(approval);

            Notification notification = new Notification(
                    UUID.randomUUID(),
                    approverId,
                    NotificationType.APPROVAL_REQUESTED,
                    "Approval requested",
                    "A post requires your approval",
                    postId,
                    false,
                    now
            );
            notificationRepository.save(notification);
            notificationSender.send(notification);

            LOG.log(Level.INFO, "Post {0} submitted for approval by user {1} to approver {2}",
                    new Object[]{postId, requesterId, approverId});
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"ApprovalUseCase.submitForApproval", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"ApprovalUseCase.submitForApproval", perfMs});
            }
        }
    }

    public List<ApprovalRequestDto> listPendingApprovals(UUID approverId, int page, int size) {
        long perfStart = System.nanoTime();
        try {
            int offset = page * size;
            List<ApprovalRequest> approvals =
                    approvalRequestRepository.findPendingByApproverId(approverId, offset, size);

            return approvals.stream()
                    .map(this::toApprovalRequestDto)
                    .toList();
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"ApprovalUseCase.listPendingApprovals", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"ApprovalUseCase.listPendingApprovals", perfMs});
            }
        }
    }

    @Transactional
    public void decide(UUID approvalId, UUID deciderId, ApprovalDecisionDto decision) {
        long perfStart = System.nanoTime();
        try {
            ApprovalRequest approval = approvalRequestRepository.findById(approvalId)
                    .orElseThrow(() -> new DomainException("APPROVAL_NOT_FOUND",
                            "Approval request not found: " + approvalId));

            if (!approval.getApproverId().equals(deciderId)) {
                throw new DomainException("FORBIDDEN",
                        "You are not the designated approver for this request");
            }

            ApprovalAction action = ApprovalAction.valueOf(decision.action().toUpperCase());
            Instant now = Instant.now();

            approval.setStatus(action);
            approval.setComment(decision.comment());
            approval.setDecidedAt(now);
            approvalRequestRepository.save(approval);

            Post post = postRepository.findById(approval.getPostId())
                    .orElseThrow(() -> new PostNotFoundException(approval.getPostId()));

            if (action == ApprovalAction.APPROVE) {
                post.setStatus(PostStatus.APPROVED);
                post.setUpdatedAt(now);
                postRepository.save(post);
                postPublisher.publishForProcessing(post.getId());
                LOG.log(Level.INFO, "Post {0} approved and sent for publishing", post.getId());
            } else {
                post.setStatus(PostStatus.REJECTED);
                post.setUpdatedAt(now);
                postRepository.save(post);
                LOG.log(Level.INFO, "Post {0} rejected with action: {1}",
                        new Object[]{post.getId(), action});
            }

            String decisionText = action == ApprovalAction.APPROVE ? "approved" : "rejected";
            Notification notification = new Notification(
                    UUID.randomUUID(),
                    approval.getRequesterId(),
                    NotificationType.APPROVAL_DECIDED,
                    "Post " + decisionText,
                    "Your post has been " + decisionText +
                            (decision.comment() != null ? ": " + decision.comment() : ""),
                    post.getId(),
                    false,
                    now
            );
            notificationRepository.save(notification);
            notificationSender.send(notification);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"ApprovalUseCase.decide", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"ApprovalUseCase.decide", perfMs});
            }
        }
    }

    public long countPending(UUID approverId) {
        long perfStart = System.nanoTime();
        try {
            return approvalRequestRepository.countPendingByApproverId(approverId);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"ApprovalUseCase.countPending", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"ApprovalUseCase.countPending", perfMs});
            }
        }
    }

    private ApprovalRequestDto toApprovalRequestDto(ApprovalRequest approval) {
        String postContent = postRepository.findById(approval.getPostId())
                .map(Post::getContent)
                .orElse("[deleted]");

        String requesterName = userRepository.findById(approval.getRequesterId())
                .map(User::getUsername)
                .orElse("[unknown]");

        return new ApprovalRequestDto(
                approval.getId(),
                approval.getPostId(),
                postContent,
                requesterName,
                approval.getStatus() != null ? approval.getStatus().name() : "PENDING",
                approval.getComment(),
                approval.getRequestedAt(),
                approval.getDecidedAt()
        );
    }
}
