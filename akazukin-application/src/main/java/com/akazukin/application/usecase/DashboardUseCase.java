package com.akazukin.application.usecase;

import com.akazukin.application.dto.PostResponseDto;
import com.akazukin.application.dto.PostTargetDto;
import com.akazukin.domain.model.DashboardSummary;
import com.akazukin.domain.model.Post;
import com.akazukin.domain.model.PostStatus;
import com.akazukin.domain.model.PostTarget;
import com.akazukin.domain.model.SnsAccount;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.PostRepository;
import com.akazukin.domain.port.PostTargetRepository;
import com.akazukin.domain.port.SnsAccountRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class DashboardUseCase {

    private final PostRepository postRepository;
    private final PostTargetRepository postTargetRepository;
    private final SnsAccountRepository snsAccountRepository;

    @Inject
    public DashboardUseCase(PostRepository postRepository,
                            PostTargetRepository postTargetRepository,
                            SnsAccountRepository snsAccountRepository) {
        this.postRepository = postRepository;
        this.postTargetRepository = postTargetRepository;
        this.snsAccountRepository = snsAccountRepository;
    }

    public DashboardSummary getSummary(UUID userId) {
        long totalPosts = postRepository.countByUserId(userId);
        long publishedPosts = postRepository.countByUserIdAndStatus(userId, PostStatus.PUBLISHED);
        long failedPosts = postRepository.countByUserIdAndStatus(userId, PostStatus.FAILED);
        long scheduledPosts = postRepository.countByUserIdAndStatus(userId, PostStatus.SCHEDULED);

        List<SnsAccount> accounts = snsAccountRepository.findByUserId(userId);
        int connectedAccounts = accounts.size();

        Map<SnsPlatform, Integer> postCountByPlatform = calculatePostCountByPlatform(userId);

        return new DashboardSummary(
                (int) totalPosts,
                (int) publishedPosts,
                (int) failedPosts,
                (int) scheduledPosts,
                connectedAccounts,
                postCountByPlatform,
                List.of()
        );
    }

    public List<PostResponseDto> getRecentPosts(UUID userId, int limit) {
        List<Post> posts = postRepository.findByUserId(userId, 0, limit);

        return posts.stream()
                .map(post -> {
                    List<PostTarget> targets = postTargetRepository.findByPostId(post.getId());
                    List<PostTargetDto> targetDtos = targets.stream()
                            .map(target -> new PostTargetDto(
                                    target.getId(),
                                    target.getPlatform().name(),
                                    target.getStatus().name(),
                                    target.getPlatformPostId(),
                                    target.getErrorMessage(),
                                    target.getPublishedAt()
                            ))
                            .toList();

                    return new PostResponseDto(
                            post.getId(),
                            post.getContent(),
                            post.getStatus().name(),
                            post.getScheduledAt(),
                            post.getCreatedAt(),
                            targetDtos
                    );
                })
                .toList();
    }

    public Map<String, Object> getTimelineSummary(UUID userId) {
        List<PostResponseDto> recentPosts = getRecentPosts(userId, 20);
        DashboardSummary summary = getSummary(userId);

        return Map.of(
                "summary", summary,
                "recentPosts", recentPosts
        );
    }

    private Map<SnsPlatform, Integer> calculatePostCountByPlatform(UUID userId) {
        Map<SnsPlatform, Integer> counts = new EnumMap<>(SnsPlatform.class);
        List<Post> posts = postRepository.findByUserId(userId, 0, Integer.MAX_VALUE);

        for (Post post : posts) {
            List<PostTarget> targets = postTargetRepository.findByPostId(post.getId());
            for (PostTarget target : targets) {
                counts.merge(target.getPlatform(), 1, Integer::sum);
            }
        }

        return counts;
    }
}
