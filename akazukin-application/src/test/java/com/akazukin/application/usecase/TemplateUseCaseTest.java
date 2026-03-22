package com.akazukin.application.usecase;

import com.akazukin.application.dto.PostTemplateDto;
import com.akazukin.application.dto.PostTemplateRequestDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.PostTemplate;
import com.akazukin.domain.port.PostTemplateRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateUseCaseTest {

    private InMemoryPostTemplateRepository postTemplateRepository;
    private TemplateUseCase templateUseCase;

    private UUID userId;

    @BeforeEach
    void setUp() {
        postTemplateRepository = new InMemoryPostTemplateRepository();
        templateUseCase = new TemplateUseCase(postTemplateRepository);

        userId = UUID.randomUUID();
    }

    @Test
    void createTemplate_createsTemplateWithCorrectAttributes() {
        PostTemplateRequestDto request = new PostTemplateRequestDto(
                "Weekly Update", "This week: {{topic}}", List.of("topic"), List.of("TWITTER"), "marketing");

        PostTemplateDto result = templateUseCase.createTemplate(userId, request);

        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals("Weekly Update", result.name());
        assertEquals("This week: {{topic}}", result.content());
        assertEquals(List.of("topic"), result.placeholders());
        assertEquals(List.of("TWITTER"), result.platforms());
        assertEquals("marketing", result.category());
        assertEquals(0, result.usageCount());
    }

    @Test
    void createTemplate_persistsInRepository() {
        PostTemplateRequestDto request = new PostTemplateRequestDto(
                "Template A", "Content A", List.of(), List.of(), null);

        PostTemplateDto result = templateUseCase.createTemplate(userId, request);

        Optional<PostTemplate> stored = postTemplateRepository.findById(result.id());
        assertTrue(stored.isPresent());
        assertEquals("Template A", stored.get().getName());
    }

    @Test
    void createTemplate_throwsWhenNameIsNull() {
        PostTemplateRequestDto request = new PostTemplateRequestDto(
                null, "Content", List.of(), List.of(), null);

        DomainException exception = assertThrows(DomainException.class,
                () -> templateUseCase.createTemplate(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void createTemplate_throwsWhenNameIsBlank() {
        PostTemplateRequestDto request = new PostTemplateRequestDto(
                "", "Content", List.of(), List.of(), null);

        DomainException exception = assertThrows(DomainException.class,
                () -> templateUseCase.createTemplate(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void createTemplate_throwsWhenContentIsNull() {
        PostTemplateRequestDto request = new PostTemplateRequestDto(
                "Name", null, List.of(), List.of(), null);

        DomainException exception = assertThrows(DomainException.class,
                () -> templateUseCase.createTemplate(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void createTemplate_throwsWhenContentIsBlank() {
        PostTemplateRequestDto request = new PostTemplateRequestDto(
                "Name", "", List.of(), List.of(), null);

        DomainException exception = assertThrows(DomainException.class,
                () -> templateUseCase.createTemplate(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void createTemplate_handlesNullPlaceholdersAsList() {
        PostTemplateRequestDto request = new PostTemplateRequestDto(
                "Name", "Content", null, List.of("TWITTER"), null);

        PostTemplateDto result = templateUseCase.createTemplate(userId, request);

        assertNotNull(result.placeholders());
        assertTrue(result.placeholders().isEmpty());
    }

    @Test
    void createTemplate_handlesNullPlatformsAsList() {
        PostTemplateRequestDto request = new PostTemplateRequestDto(
                "Name", "Content", List.of(), null, null);

        PostTemplateDto result = templateUseCase.createTemplate(userId, request);

        assertNotNull(result.platforms());
        assertTrue(result.platforms().isEmpty());
    }

    @Test
    void listTemplates_returnsTemplatesForUser() {
        templateUseCase.createTemplate(userId,
                new PostTemplateRequestDto("T1", "C1", List.of(), List.of(), null));
        templateUseCase.createTemplate(userId,
                new PostTemplateRequestDto("T2", "C2", List.of(), List.of(), null));

        List<PostTemplateDto> result = templateUseCase.listTemplates(userId);

        assertEquals(2, result.size());
    }

    @Test
    void listTemplates_returnsEmptyForUserWithNoTemplates() {
        UUID otherUserId = UUID.randomUUID();

        List<PostTemplateDto> result = templateUseCase.listTemplates(otherUserId);

        assertTrue(result.isEmpty());
    }

    @Test
    void getTemplate_returnsExistingTemplate() {
        PostTemplateDto created = templateUseCase.createTemplate(userId,
                new PostTemplateRequestDto("Name", "Content", List.of(), List.of(), null));

        PostTemplateDto found = templateUseCase.getTemplate(created.id());

        assertEquals(created.id(), found.id());
        assertEquals("Name", found.name());
    }

    @Test
    void getTemplate_throwsWhenTemplateNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> templateUseCase.getTemplate(nonExistentId));
        assertEquals("TEMPLATE_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void deleteTemplate_removesTemplate() {
        PostTemplateDto created = templateUseCase.createTemplate(userId,
                new PostTemplateRequestDto("Name", "Content", List.of(), List.of(), null));

        templateUseCase.deleteTemplate(created.id(), userId);

        assertThrows(DomainException.class,
                () -> templateUseCase.getTemplate(created.id()));
    }

    @Test
    void deleteTemplate_throwsWhenTemplateNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> templateUseCase.deleteTemplate(nonExistentId, userId));
        assertEquals("TEMPLATE_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void deleteTemplate_throwsForbiddenWhenNotOwner() {
        PostTemplateDto created = templateUseCase.createTemplate(userId,
                new PostTemplateRequestDto("Name", "Content", List.of(), List.of(), null));
        UUID otherUserId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> templateUseCase.deleteTemplate(created.id(), otherUserId));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void recordUsage_incrementsUsageCount() {
        PostTemplateDto created = templateUseCase.createTemplate(userId,
                new PostTemplateRequestDto("Name", "Content", List.of(), List.of(), null));

        templateUseCase.recordUsage(created.id());

        PostTemplate stored = postTemplateRepository.findById(created.id()).orElseThrow();
        assertEquals(1, stored.getUsageCount());
    }

    @Test
    void recordUsage_incrementsMultipleTimes() {
        PostTemplateDto created = templateUseCase.createTemplate(userId,
                new PostTemplateRequestDto("Name", "Content", List.of(), List.of(), null));

        templateUseCase.recordUsage(created.id());
        templateUseCase.recordUsage(created.id());
        templateUseCase.recordUsage(created.id());

        PostTemplate stored = postTemplateRepository.findById(created.id()).orElseThrow();
        assertEquals(3, stored.getUsageCount());
    }

    @Test
    void recordUsage_throwsWhenTemplateNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> templateUseCase.recordUsage(nonExistentId));
        assertEquals("TEMPLATE_NOT_FOUND", exception.getErrorCode());
    }

    private static class InMemoryPostTemplateRepository implements PostTemplateRepository {

        private final Map<UUID, PostTemplate> store = new HashMap<>();

        @Override
        public Optional<PostTemplate> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<PostTemplate> findByUserId(UUID userId) {
            return store.values().stream()
                    .filter(template -> template.getUserId().equals(userId))
                    .toList();
        }

        @Override
        public PostTemplate save(PostTemplate postTemplate) {
            store.put(postTemplate.getId(), postTemplate);
            return postTemplate;
        }

        @Override
        public void deleteById(UUID id) {
            store.remove(id);
        }

        @Override
        public void incrementUsageCount(UUID id) {
            PostTemplate template = store.get(id);
            if (template != null) {
                template.setUsageCount(template.getUsageCount() + 1);
            }
        }
    }
}
