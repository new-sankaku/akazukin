package com.akazukin.application.usecase;

import com.akazukin.application.dto.ABTestDto;
import com.akazukin.application.dto.ABTestRequestDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.ABTest;
import com.akazukin.domain.model.ABTestStatus;
import com.akazukin.domain.port.ABTestRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class ABTestUseCase {

    private static final Logger LOG = Logger.getLogger(ABTestUseCase.class.getName());

    private final ABTestRepository abTestRepository;

    @Inject
    public ABTestUseCase(ABTestRepository abTestRepository) {
        this.abTestRepository = abTestRepository;
    }

    @Transactional
    public ABTestDto createTest(UUID userId, ABTestRequestDto request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new DomainException("INVALID_INPUT", "Test name is required");
        }
        if (request.variantA() == null || request.variantA().isBlank()) {
            throw new DomainException("INVALID_INPUT", "Variant A is required");
        }
        if (request.variantB() == null || request.variantB().isBlank()) {
            throw new DomainException("INVALID_INPUT", "Variant B is required");
        }

        Instant now = Instant.now();
        ABTest test = new ABTest(
                UUID.randomUUID(),
                userId,
                request.name(),
                request.variantA(),
                request.variantB(),
                ABTestStatus.DRAFT,
                null,
                null,
                null,
                now
        );

        ABTest saved = abTestRepository.save(test);
        LOG.log(Level.INFO, "A/B test created: {0} for user {1}",
                new Object[]{saved.getId(), userId});

        return toDto(saved);
    }

    public List<ABTestDto> listTests(UUID userId) {
        return abTestRepository.findByUserId(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public ABTestDto completeTest(UUID testId, UUID userId, String winnerVariant) {
        ABTest test = abTestRepository.findById(testId)
                .orElseThrow(() -> new DomainException("TEST_NOT_FOUND",
                        "A/B test not found: " + testId));

        if (!test.getUserId().equals(userId)) {
            throw new DomainException("FORBIDDEN", "You do not own this A/B test");
        }

        if (test.getStatus() == ABTestStatus.COMPLETED) {
            throw new DomainException("ALREADY_COMPLETED", "This A/B test is already completed");
        }

        if (winnerVariant == null || winnerVariant.isBlank()) {
            throw new DomainException("INVALID_INPUT", "Winner variant is required");
        }

        test.setStatus(ABTestStatus.COMPLETED);
        test.setCompletedAt(Instant.now());
        test.setWinnerVariant(winnerVariant);

        ABTest saved = abTestRepository.save(test);
        LOG.log(Level.INFO, "A/B test completed: {0}, winner: {1}",
                new Object[]{testId, winnerVariant});

        return toDto(saved);
    }

    @Transactional
    public void deleteTest(UUID testId, UUID userId) {
        ABTest test = abTestRepository.findById(testId)
                .orElseThrow(() -> new DomainException("TEST_NOT_FOUND",
                        "A/B test not found: " + testId));

        if (!test.getUserId().equals(userId)) {
            throw new DomainException("FORBIDDEN", "You do not own this A/B test");
        }

        abTestRepository.deleteById(testId);
        LOG.log(Level.INFO, "A/B test deleted: {0}", testId);
    }

    private ABTestDto toDto(ABTest test) {
        return new ABTestDto(
                test.getId(),
                test.getName(),
                test.getVariantA(),
                test.getVariantB(),
                test.getStatus().name(),
                test.getStartedAt(),
                test.getCompletedAt(),
                test.getWinnerVariant(),
                test.getCreatedAt()
        );
    }
}
