package com.akazukin.application.usecase;

import com.akazukin.application.dto.PostTemplateDto;
import com.akazukin.application.dto.PostTemplateRequestDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.PostTemplate;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.PostTemplateRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class TemplateUseCase {

    private static final Logger LOG = Logger.getLogger(TemplateUseCase.class.getName());

    private final PostTemplateRepository postTemplateRepository;

    @Inject
    public TemplateUseCase(PostTemplateRepository postTemplateRepository) {
        this.postTemplateRepository = postTemplateRepository;
    }

    public PostTemplateDto createTemplate(UUID userId, PostTemplateRequestDto request) {
        if (request.name() == null || request.name().isBlank()) {
            throw new DomainException("INVALID_INPUT", "Template name is required");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new DomainException("INVALID_INPUT", "Template content is required");
        }

        List<SnsPlatform> platforms = request.platforms() != null
                ? request.platforms().stream()
                    .map(p -> SnsPlatform.valueOf(p.toUpperCase()))
                    .toList()
                : List.of();

        Instant now = Instant.now();
        PostTemplate template = new PostTemplate(
                UUID.randomUUID(),
                userId,
                request.name(),
                request.content(),
                request.placeholders() != null ? request.placeholders() : List.of(),
                platforms,
                request.category(),
                0,
                now,
                now
        );

        PostTemplate saved = postTemplateRepository.save(template);
        LOG.log(Level.INFO, "Post template created: {0} for user {1}",
                new Object[]{saved.getId(), userId});

        return toPostTemplateDto(saved);
    }

    public List<PostTemplateDto> listTemplates(UUID userId) {
        return postTemplateRepository.findByUserId(userId).stream()
                .map(this::toPostTemplateDto)
                .toList();
    }

    public PostTemplateDto getTemplate(UUID templateId) {
        PostTemplate template = postTemplateRepository.findById(templateId)
                .orElseThrow(() -> new DomainException("TEMPLATE_NOT_FOUND",
                        "Post template not found: " + templateId));

        return toPostTemplateDto(template);
    }

    public void deleteTemplate(UUID templateId, UUID userId) {
        PostTemplate template = postTemplateRepository.findById(templateId)
                .orElseThrow(() -> new DomainException("TEMPLATE_NOT_FOUND",
                        "Post template not found: " + templateId));

        if (!template.getUserId().equals(userId)) {
            throw new DomainException("FORBIDDEN", "You do not own this template");
        }

        postTemplateRepository.deleteById(templateId);
        LOG.log(Level.INFO, "Post template deleted: {0}", templateId);
    }

    public void recordUsage(UUID templateId) {
        postTemplateRepository.findById(templateId)
                .orElseThrow(() -> new DomainException("TEMPLATE_NOT_FOUND",
                        "Post template not found: " + templateId));

        postTemplateRepository.incrementUsageCount(templateId);
        LOG.log(Level.FINE, "Template usage recorded: {0}", templateId);
    }

    private PostTemplateDto toPostTemplateDto(PostTemplate template) {
        List<String> platformNames = template.getPlatforms().stream()
                .map(SnsPlatform::name)
                .toList();

        return new PostTemplateDto(
                template.getId(),
                template.getName(),
                template.getContent(),
                template.getPlaceholders(),
                platformNames,
                template.getCategory(),
                template.getUsageCount()
        );
    }
}
