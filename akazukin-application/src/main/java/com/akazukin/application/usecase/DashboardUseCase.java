package com.akazukin.application.usecase;

import com.akazukin.domain.port.PostRepository;
import com.akazukin.domain.port.SnsAccountRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class DashboardUseCase {

    @Inject
    PostRepository postRepository;

    @Inject
    SnsAccountRepository snsAccountRepository;

    public Map<String, Object> getSummary(UUID userId) {
        int totalPosts = postRepository.findByUserId(userId, 0, Integer.MAX_VALUE).size();
        int totalAccounts = snsAccountRepository.findByUserId(userId).size();
        return Map.of(
                "totalPosts", totalPosts,
                "totalAccounts", totalAccounts
        );
    }
}
