package com.akazukin.application.usecase;

import com.akazukin.application.dto.MultiAngleResponseDto;
import com.akazukin.application.dto.NewsABTestRequestDto;
import com.akazukin.application.dto.NewsABTestResponseDto;
import com.akazukin.application.dto.NewsItemDto;
import com.akazukin.application.dto.NewsPostGeneratedDto;
import com.akazukin.application.dto.NewsPostIdeaRequestDto;
import com.akazukin.application.dto.NewsPostIdeaResponseDto;
import com.akazukin.application.dto.NewsSourceDto;
import com.akazukin.application.dto.NewsSourceRequestDto;
import com.akazukin.application.dto.TemplateMatchResponseDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.AiPersona;
import com.akazukin.domain.model.AiPrompt;
import com.akazukin.domain.model.AiResponse;
import com.akazukin.domain.model.ContentTone;
import com.akazukin.domain.model.NewsItem;
import com.akazukin.domain.model.NewsSource;
import com.akazukin.domain.model.PostTemplate;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.AiPersonaRepository;
import com.akazukin.domain.port.AiTextGenerator;
import com.akazukin.domain.port.NewsFeedFetcher;
import com.akazukin.domain.port.NewsItemRepository;
import com.akazukin.domain.port.NewsSourceRepository;
import com.akazukin.domain.port.PostTemplateRepository;

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

class NewsPostUseCaseTest {

    private StubNewsFeedFetcher newsFeedFetcher;
    private InMemoryNewsSourceRepository newsSourceRepository;
    private InMemoryNewsItemRepository newsItemRepository;
    private StubAiTextGenerator aiTextGenerator;
    private InMemoryAiPersonaRepository aiPersonaRepository;
    private InMemoryPostTemplateRepository postTemplateRepository;
    private NewsPostUseCase newsPostUseCase;

    private UUID userId;

    @BeforeEach
    void setUp() {
        newsFeedFetcher = new StubNewsFeedFetcher();
        newsSourceRepository = new InMemoryNewsSourceRepository();
        newsItemRepository = new InMemoryNewsItemRepository();
        aiTextGenerator = new StubAiTextGenerator();
        aiPersonaRepository = new InMemoryAiPersonaRepository();
        postTemplateRepository = new InMemoryPostTemplateRepository();
        newsPostUseCase = new NewsPostUseCase(newsFeedFetcher, newsSourceRepository,
                newsItemRepository, aiTextGenerator, aiPersonaRepository, postTemplateRepository);

        userId = UUID.randomUUID();
    }

    @Test
    void listSources_returnsEmptyWhenNoSources() {
        List<NewsSourceDto> result = newsPostUseCase.listSources(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void listSources_returnsSourcesForUser() {
        createSource(userId, "Tech News", "https://tech.example.com/rss");
        createSource(userId, "Science", "https://science.example.com/rss");

        List<NewsSourceDto> result = newsPostUseCase.listSources(userId);

        assertEquals(2, result.size());
    }

    @Test
    void listSources_doesNotReturnOtherUsersSources() {
        createSource(userId, "My Feed", "https://my.example.com/rss");
        UUID otherUserId = UUID.randomUUID();
        createSource(otherUserId, "Other Feed", "https://other.example.com/rss");

        List<NewsSourceDto> result = newsPostUseCase.listSources(userId);

        assertEquals(1, result.size());
        assertEquals("My Feed", result.get(0).name());
    }

    @Test
    void addSource_createsNewSource() {
        NewsSourceRequestDto request = new NewsSourceRequestDto(
                "Tech News", "https://tech.example.com/rss", "RSS");

        NewsSourceDto result = newsPostUseCase.addSource(userId, request);

        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals("Tech News", result.name());
        assertEquals("https://tech.example.com/rss", result.url());
        assertEquals("RSS", result.sourceType());
        assertTrue(result.isActive());
    }

    @Test
    void addSource_defaultsToRssWhenSourceTypeIsNull() {
        NewsSourceRequestDto request = new NewsSourceRequestDto(
                "Tech News", "https://tech.example.com/rss", null);

        NewsSourceDto result = newsPostUseCase.addSource(userId, request);

        assertEquals("RSS", result.sourceType());
    }

    @Test
    void addSource_throwsInvalidInputWhenNameIsNull() {
        NewsSourceRequestDto request = new NewsSourceRequestDto(
                null, "https://tech.example.com/rss", "RSS");

        DomainException exception = assertThrows(DomainException.class,
                () -> newsPostUseCase.addSource(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void addSource_throwsInvalidInputWhenNameIsBlank() {
        NewsSourceRequestDto request = new NewsSourceRequestDto(
                "", "https://tech.example.com/rss", "RSS");

        DomainException exception = assertThrows(DomainException.class,
                () -> newsPostUseCase.addSource(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void addSource_throwsInvalidInputWhenUrlIsNull() {
        NewsSourceRequestDto request = new NewsSourceRequestDto(
                "Tech News", null, "RSS");

        DomainException exception = assertThrows(DomainException.class,
                () -> newsPostUseCase.addSource(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void addSource_throwsInvalidInputWhenUrlIsBlank() {
        NewsSourceRequestDto request = new NewsSourceRequestDto(
                "Tech News", "", "RSS");

        DomainException exception = assertThrows(DomainException.class,
                () -> newsPostUseCase.addSource(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void removeSource_deletesSourceAndItems() {
        NewsSource source = createSource(userId, "Tech News", "https://tech.example.com/rss");
        NewsItem item = new NewsItem(UUID.randomUUID(), source.getId(), "Article", "https://example.com/1",
                "summary", Instant.now(), Instant.now());
        newsItemRepository.save(item);

        newsPostUseCase.removeSource(source.getId(), userId);

        assertTrue(newsSourceRepository.findById(source.getId()).isEmpty());
        assertTrue(newsItemRepository.findBySourceId(source.getId(), 0, 100).isEmpty());
    }

    @Test
    void removeSource_throwsSourceNotFoundForNonExistentSource() {
        UUID nonExistentId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> newsPostUseCase.removeSource(nonExistentId, userId));
        assertEquals("SOURCE_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void removeSource_throwsForbiddenWhenNotOwner() {
        NewsSource source = createSource(userId, "Tech News", "https://tech.example.com/rss");
        UUID otherUserId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> newsPostUseCase.removeSource(source.getId(), otherUserId));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void fetchAndGeneratePost_returnsGeneratedPost() {
        NewsSource source = createSource(userId, "Tech News", "https://tech.example.com/rss");
        NewsItem newsItem = new NewsItem(UUID.randomUUID(), source.getId(), "Breaking News",
                "https://example.com/1", "Summary of news", Instant.now(), Instant.now());
        newsFeedFetcher.nextItems = List.of(newsItem);
        aiTextGenerator.nextResponse = "Check out this breaking news!";

        NewsPostGeneratedDto result = newsPostUseCase.fetchAndGeneratePost(userId, source.getId());

        assertNotNull(result);
        assertEquals("Check out this breaking news!", result.generatedText());
        assertEquals("Breaking News", result.newsTitle());
        assertEquals("https://example.com/1", result.newsUrl());
    }

    @Test
    void fetchAndGeneratePost_throwsSourceNotFoundForNonExistentSource() {
        UUID nonExistentId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> newsPostUseCase.fetchAndGeneratePost(userId, nonExistentId));
        assertEquals("SOURCE_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void fetchAndGeneratePost_throwsForbiddenWhenNotOwner() {
        NewsSource source = createSource(userId, "Tech News", "https://tech.example.com/rss");
        UUID otherUserId = UUID.randomUUID();
        newsFeedFetcher.nextItems = List.of();

        DomainException exception = assertThrows(DomainException.class,
                () -> newsPostUseCase.fetchAndGeneratePost(otherUserId, source.getId()));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void fetchAndGeneratePost_throwsNoNewsWhenFetchReturnsEmpty() {
        NewsSource source = createSource(userId, "Tech News", "https://tech.example.com/rss");
        newsFeedFetcher.nextItems = List.of();

        DomainException exception = assertThrows(DomainException.class,
                () -> newsPostUseCase.fetchAndGeneratePost(userId, source.getId()));
        assertEquals("NO_NEWS", exception.getErrorCode());
    }

    @Test
    void fetchArticles_returnsFetchedArticles() {
        NewsSource source = createSource(userId, "Tech News", "https://tech.example.com/rss");
        NewsItem item1 = new NewsItem(UUID.randomUUID(), source.getId(), "Article 1",
                "https://example.com/1", "summary1", Instant.now(), Instant.now());
        NewsItem item2 = new NewsItem(UUID.randomUUID(), source.getId(), "Article 2",
                "https://example.com/2", "summary2", Instant.now(), Instant.now());
        newsFeedFetcher.nextItems = List.of(item1, item2);

        List<NewsItemDto> result = newsPostUseCase.fetchArticles(userId, source.getId());

        assertEquals(2, result.size());
    }

    @Test
    void fetchArticles_throwsSourceNotFoundForNonExistentSource() {
        UUID nonExistentId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> newsPostUseCase.fetchArticles(userId, nonExistentId));
        assertEquals("SOURCE_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void fetchArticles_throwsForbiddenWhenNotOwner() {
        NewsSource source = createSource(userId, "Tech News", "https://tech.example.com/rss");
        UUID otherUserId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> newsPostUseCase.fetchArticles(otherUserId, source.getId()));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void listArticles_returnsEmptyWhenNoSources() {
        List<NewsItemDto> result = newsPostUseCase.listArticles(userId);

        assertTrue(result.isEmpty());
    }

    @Test
    void listArticles_returnsArticlesFromAllSources() {
        NewsSource source1 = createSource(userId, "Feed 1", "https://feed1.example.com/rss");
        NewsSource source2 = createSource(userId, "Feed 2", "https://feed2.example.com/rss");

        newsItemRepository.save(new NewsItem(UUID.randomUUID(), source1.getId(), "Article A",
                "https://a.com", null, Instant.now(), Instant.now()));
        newsItemRepository.save(new NewsItem(UUID.randomUUID(), source2.getId(), "Article B",
                "https://b.com", null, Instant.now().minusSeconds(60), Instant.now()));

        List<NewsItemDto> result = newsPostUseCase.listArticles(userId);

        assertEquals(2, result.size());
    }

    @Test
    void generatePostIdea_returnsGeneratedIdea() {
        NewsItem newsItem = new NewsItem(UUID.randomUUID(), UUID.randomUUID(), "Tech Breakthrough",
                "https://example.com/tech", "Big news", Instant.now(), Instant.now());
        newsItemRepository.save(newsItem);

        Instant now = Instant.now();
        AiPersona persona = new AiPersona(UUID.randomUUID(), userId, "TechWriter",
                "Write like a tech blogger", ContentTone.PROFESSIONAL, "en", null, false, now, now);
        aiPersonaRepository.save(persona);

        aiTextGenerator.nextResponse = "Exciting tech breakthrough!";

        NewsPostIdeaRequestDto request = new NewsPostIdeaRequestDto(newsItem.getId(), persona.getId());
        NewsPostIdeaResponseDto result = newsPostUseCase.generatePostIdea(userId, request);

        assertNotNull(result);
        assertEquals(newsItem.getId(), result.newsItemId());
        assertEquals("Tech Breakthrough", result.newsTitle());
        assertEquals("TechWriter", result.personaName());
        assertEquals("Exciting tech breakthrough!", result.generatedText());
    }

    @Test
    void generatePostIdea_throwsNotFoundForNonExistentNewsItem() {
        Instant now = Instant.now();
        AiPersona persona = new AiPersona(UUID.randomUUID(), userId, "Writer",
                "prompt", ContentTone.CASUAL, "en", null, false, now, now);
        aiPersonaRepository.save(persona);

        NewsPostIdeaRequestDto request = new NewsPostIdeaRequestDto(UUID.randomUUID(), persona.getId());

        DomainException exception = assertThrows(DomainException.class,
                () -> newsPostUseCase.generatePostIdea(userId, request));
        assertEquals("NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void generatePostIdea_throwsNotFoundForNonExistentPersona() {
        NewsItem newsItem = new NewsItem(UUID.randomUUID(), UUID.randomUUID(), "Title",
                "https://example.com", null, Instant.now(), Instant.now());
        newsItemRepository.save(newsItem);

        NewsPostIdeaRequestDto request = new NewsPostIdeaRequestDto(newsItem.getId(), UUID.randomUUID());

        DomainException exception = assertThrows(DomainException.class,
                () -> newsPostUseCase.generatePostIdea(userId, request));
        assertEquals("NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void generatePostIdea_throwsForbiddenWhenPersonaNotOwned() {
        NewsItem newsItem = new NewsItem(UUID.randomUUID(), UUID.randomUUID(), "Title",
                "https://example.com", null, Instant.now(), Instant.now());
        newsItemRepository.save(newsItem);

        UUID otherUserId = UUID.randomUUID();
        Instant now = Instant.now();
        AiPersona persona = new AiPersona(UUID.randomUUID(), otherUserId, "Writer",
                "prompt", ContentTone.CASUAL, "en", null, false, now, now);
        aiPersonaRepository.save(persona);

        NewsPostIdeaRequestDto request = new NewsPostIdeaRequestDto(newsItem.getId(), persona.getId());

        DomainException exception = assertThrows(DomainException.class,
                () -> newsPostUseCase.generatePostIdea(userId, request));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void generateMultiAngle_returnsAngles() {
        NewsItem newsItem = new NewsItem(UUID.randomUUID(), UUID.randomUUID(), "Big Event",
                "https://example.com/event", "Something big", Instant.now(), Instant.now());
        newsItemRepository.save(newsItem);

        aiTextGenerator.nextResponse = "BREAKING|Breaking news about event\n"
                + "ANALYSIS|Deep analysis of the event\n"
                + "DIALOGUE|What do you think about this?";

        MultiAngleResponseDto result = newsPostUseCase.generateMultiAngle(userId, newsItem.getId());

        assertNotNull(result);
        assertEquals(newsItem.getId(), result.newsItemId());
        assertEquals("Big Event", result.newsTitle());
        assertEquals(3, result.angles().size());
    }

    @Test
    void generateMultiAngle_throwsNotFoundForNonExistentNewsItem() {
        UUID nonExistentId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> newsPostUseCase.generateMultiAngle(userId, nonExistentId));
        assertEquals("NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void matchTemplates_returnsEmptyMatchesWhenNoTemplates() {
        NewsItem newsItem = new NewsItem(UUID.randomUUID(), UUID.randomUUID(), "Title",
                "https://example.com", null, Instant.now(), Instant.now());
        newsItemRepository.save(newsItem);

        TemplateMatchResponseDto result = newsPostUseCase.matchTemplates(userId, newsItem.getId());

        assertNotNull(result);
        assertEquals(newsItem.getId(), result.newsItemId());
        assertTrue(result.matches().isEmpty());
    }

    @Test
    void matchTemplates_returnsMatchesWhenTemplatesExist() {
        NewsItem newsItem = new NewsItem(UUID.randomUUID(), UUID.randomUUID(), "Title",
                "https://example.com", "summary", Instant.now(), Instant.now());
        newsItemRepository.save(newsItem);

        Instant now = Instant.now();
        PostTemplate template = new PostTemplate(UUID.randomUUID(), userId, "Announcement",
                "content", List.of(), List.of(SnsPlatform.TWITTER), "news", 0, now, now);
        postTemplateRepository.save(template);

        aiTextGenerator.nextResponse = "1|1|85|Good match for announcements";

        TemplateMatchResponseDto result = newsPostUseCase.matchTemplates(userId, newsItem.getId());

        assertNotNull(result);
        assertEquals(1, result.matches().size());
    }

    @Test
    void matchTemplates_throwsNotFoundForNonExistentNewsItem() {
        UUID nonExistentId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> newsPostUseCase.matchTemplates(userId, nonExistentId));
        assertEquals("NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void generateABTest_returnsTwoPanels() {
        NewsItem newsItem = new NewsItem(UUID.randomUUID(), UUID.randomUUID(), "Title",
                "https://example.com", "summary", Instant.now(), Instant.now());
        newsItemRepository.save(newsItem);

        Instant now = Instant.now();
        AiPersona personaA = new AiPersona(UUID.randomUUID(), userId, "Casual",
                "Write casually", ContentTone.CASUAL, "en", null, false, now, now);
        AiPersona personaB = new AiPersona(UUID.randomUUID(), userId, "Formal",
                "Write formally", ContentTone.FORMAL, "en", null, false, now, now);
        aiPersonaRepository.save(personaA);
        aiPersonaRepository.save(personaB);

        aiTextGenerator.nextResponse = "Generated post text";

        NewsABTestRequestDto request = new NewsABTestRequestDto(
                newsItem.getId(), personaA.getId(), personaB.getId());

        NewsABTestResponseDto result = newsPostUseCase.generateABTest(userId, request);

        assertNotNull(result);
        assertEquals(newsItem.getId(), result.newsItemId());
        assertEquals("A", result.panelA().label());
        assertEquals("Casual", result.panelA().personaName());
        assertEquals("B", result.panelB().label());
        assertEquals("Formal", result.panelB().personaName());
    }

    @Test
    void generateABTest_throwsNotFoundForNonExistentNewsItem() {
        Instant now = Instant.now();
        AiPersona personaA = new AiPersona(UUID.randomUUID(), userId, "A",
                "prompt", ContentTone.CASUAL, "en", null, false, now, now);
        AiPersona personaB = new AiPersona(UUID.randomUUID(), userId, "B",
                "prompt", ContentTone.FORMAL, "en", null, false, now, now);
        aiPersonaRepository.save(personaA);
        aiPersonaRepository.save(personaB);

        NewsABTestRequestDto request = new NewsABTestRequestDto(
                UUID.randomUUID(), personaA.getId(), personaB.getId());

        DomainException exception = assertThrows(DomainException.class,
                () -> newsPostUseCase.generateABTest(userId, request));
        assertEquals("NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void generateABTest_throwsNotFoundForNonExistentPersonaA() {
        NewsItem newsItem = new NewsItem(UUID.randomUUID(), UUID.randomUUID(), "Title",
                "https://example.com", null, Instant.now(), Instant.now());
        newsItemRepository.save(newsItem);

        Instant now = Instant.now();
        AiPersona personaB = new AiPersona(UUID.randomUUID(), userId, "B",
                "prompt", ContentTone.FORMAL, "en", null, false, now, now);
        aiPersonaRepository.save(personaB);

        NewsABTestRequestDto request = new NewsABTestRequestDto(
                newsItem.getId(), UUID.randomUUID(), personaB.getId());

        DomainException exception = assertThrows(DomainException.class,
                () -> newsPostUseCase.generateABTest(userId, request));
        assertEquals("NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void generateABTest_throwsForbiddenWhenPersonaNotOwned() {
        NewsItem newsItem = new NewsItem(UUID.randomUUID(), UUID.randomUUID(), "Title",
                "https://example.com", null, Instant.now(), Instant.now());
        newsItemRepository.save(newsItem);

        UUID otherUserId = UUID.randomUUID();
        Instant now = Instant.now();
        AiPersona personaA = new AiPersona(UUID.randomUUID(), otherUserId, "A",
                "prompt", ContentTone.CASUAL, "en", null, false, now, now);
        AiPersona personaB = new AiPersona(UUID.randomUUID(), userId, "B",
                "prompt", ContentTone.FORMAL, "en", null, false, now, now);
        aiPersonaRepository.save(personaA);
        aiPersonaRepository.save(personaB);

        NewsABTestRequestDto request = new NewsABTestRequestDto(
                newsItem.getId(), personaA.getId(), personaB.getId());

        DomainException exception = assertThrows(DomainException.class,
                () -> newsPostUseCase.generateABTest(userId, request));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void generateABTest_throwsInvalidInputWhenSamePersona() {
        NewsItem newsItem = new NewsItem(UUID.randomUUID(), UUID.randomUUID(), "Title",
                "https://example.com", null, Instant.now(), Instant.now());
        newsItemRepository.save(newsItem);

        Instant now = Instant.now();
        AiPersona persona = new AiPersona(UUID.randomUUID(), userId, "A",
                "prompt", ContentTone.CASUAL, "en", null, false, now, now);
        aiPersonaRepository.save(persona);

        NewsABTestRequestDto request = new NewsABTestRequestDto(
                newsItem.getId(), persona.getId(), persona.getId());

        DomainException exception = assertThrows(DomainException.class,
                () -> newsPostUseCase.generateABTest(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    private NewsSource createSource(UUID ownerUserId, String name, String url) {
        NewsSource source = new NewsSource(UUID.randomUUID(), ownerUserId, name, url, "RSS", true, Instant.now());
        return newsSourceRepository.save(source);
    }

    private static class StubNewsFeedFetcher implements NewsFeedFetcher {

        List<NewsItem> nextItems = List.of();

        @Override
        public List<NewsItem> fetch(NewsSource source) {
            return nextItems;
        }
    }

    private static class InMemoryNewsSourceRepository implements NewsSourceRepository {

        private final Map<UUID, NewsSource> store = new HashMap<>();

        @Override
        public Optional<NewsSource> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<NewsSource> findByUserId(UUID userId) {
            return store.values().stream()
                    .filter(s -> s.getUserId().equals(userId))
                    .toList();
        }

        @Override
        public List<NewsSource> findActiveByUserId(UUID userId) {
            return store.values().stream()
                    .filter(s -> s.getUserId().equals(userId))
                    .filter(NewsSource::isActive)
                    .toList();
        }

        @Override
        public NewsSource save(NewsSource newsSource) {
            store.put(newsSource.getId(), newsSource);
            return newsSource;
        }

        @Override
        public void deleteById(UUID id) {
            store.remove(id);
        }
    }

    private static class InMemoryNewsItemRepository implements NewsItemRepository {

        private final Map<UUID, NewsItem> store = new HashMap<>();

        @Override
        public Optional<NewsItem> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<NewsItem> findBySourceId(UUID sourceId, int offset, int limit) {
            List<NewsItem> filtered = store.values().stream()
                    .filter(item -> sourceId.equals(item.getSourceId()))
                    .toList();
            int end = Math.min(offset + limit, filtered.size());
            if (offset >= filtered.size()) {
                return List.of();
            }
            return new ArrayList<>(filtered.subList(offset, end));
        }

        @Override
        public NewsItem save(NewsItem newsItem) {
            store.put(newsItem.getId(), newsItem);
            return newsItem;
        }

        @Override
        public void deleteBySourceId(UUID sourceId) {
            List<UUID> toRemove = store.values().stream()
                    .filter(item -> sourceId.equals(item.getSourceId()))
                    .map(NewsItem::getId)
                    .toList();
            toRemove.forEach(store::remove);
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

    private static class InMemoryAiPersonaRepository implements AiPersonaRepository {

        private final Map<UUID, AiPersona> store = new HashMap<>();

        @Override
        public Optional<AiPersona> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<AiPersona> findByUserId(UUID userId) {
            return store.values().stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .toList();
        }

        @Override
        public Optional<AiPersona> findDefaultByUserId(UUID userId) {
            return store.values().stream()
                    .filter(p -> p.getUserId().equals(userId))
                    .filter(AiPersona::isDefault)
                    .findFirst();
        }

        @Override
        public AiPersona save(AiPersona aiPersona) {
            store.put(aiPersona.getId(), aiPersona);
            return aiPersona;
        }

        @Override
        public void deleteById(UUID id) {
            store.remove(id);
        }
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
                    .filter(t -> t.getUserId().equals(userId))
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
