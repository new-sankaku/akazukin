package com.akazukin.application.usecase;

import com.akazukin.domain.port.PostRepository;
import com.akazukin.domain.port.SnsAccountRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class AnalyticsUseCase {

    private final PostRepository postRepository;
    private final SnsAccountRepository snsAccountRepository;

    @Inject
    public AnalyticsUseCase(PostRepository postRepository,
                            SnsAccountRepository snsAccountRepository) {
        this.postRepository = postRepository;
        this.snsAccountRepository = snsAccountRepository;
    }

    public Map<String, Object> getDashboardSummary(UUID userId) {
        Map<String, Object> summary = new HashMap<>();

        int totalPosts = postRepository.findByUserId(userId, 0, Integer.MAX_VALUE).size();
        int connectedAccounts = snsAccountRepository.findByUserId(userId).size();

        summary.put("totalPosts", totalPosts);
        summary.put("connectedAccounts", connectedAccounts);

        return summary;
    }
}
