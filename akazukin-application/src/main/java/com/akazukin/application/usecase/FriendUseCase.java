package com.akazukin.application.usecase;

import com.akazukin.application.dto.FriendTargetDto;
import com.akazukin.application.dto.FriendTargetRequestDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.FriendTarget;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.FriendTargetRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class FriendUseCase {

    private final FriendTargetRepository friendTargetRepository;

    @Inject
    public FriendUseCase(FriendTargetRepository friendTargetRepository) {
        this.friendTargetRepository = friendTargetRepository;
    }

    public List<FriendTargetDto> listFriends(UUID userId) {
        List<FriendTarget> friends = friendTargetRepository.findByUserId(userId);
        return friends.stream()
                .map(this::toDto)
                .toList();
    }

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

    public void removeFriend(UUID friendId, UUID userId) {
        FriendTarget friend = friendTargetRepository.findById(friendId)
                .orElseThrow(() -> new DomainException("NOT_FOUND", "Friend target not found: " + friendId));

        if (!friend.getUserId().equals(userId)) {
            throw new DomainException("FORBIDDEN", "You do not own this friend target");
        }

        friendTargetRepository.deleteById(friendId);
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
