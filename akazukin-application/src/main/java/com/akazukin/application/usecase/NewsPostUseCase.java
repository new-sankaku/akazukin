package com.akazukin.application.usecase;

import com.akazukin.application.dto.ABPanelDto;
import com.akazukin.application.dto.AngleDto;
import com.akazukin.application.dto.MultiAngleResponseDto;
import com.akazukin.application.dto.NewsABTestRequestDto;
import com.akazukin.application.dto.NewsABTestResponseDto;
import com.akazukin.application.dto.NewsItemDto;
import com.akazukin.application.dto.NewsPostGeneratedDto;
import com.akazukin.application.dto.NewsPostIdeaRequestDto;
import com.akazukin.application.dto.NewsPostIdeaResponseDto;
import com.akazukin.application.dto.NewsSourceDto;
import com.akazukin.application.dto.NewsSourceRequestDto;
import com.akazukin.application.dto.PlatformFitDto;
import com.akazukin.application.dto.TemplateMatchItemDto;
import com.akazukin.application.dto.TemplateMatchResponseDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.AiPersona;
import com.akazukin.domain.model.AiPrompt;
import com.akazukin.domain.model.AiResponse;
import com.akazukin.domain.model.NewsItem;
import com.akazukin.domain.model.NewsSource;
import com.akazukin.domain.model.PostTemplate;
import com.akazukin.domain.port.AiPersonaRepository;
import com.akazukin.domain.port.AiTextGenerator;
import com.akazukin.domain.port.NewsFeedFetcher;
import com.akazukin.domain.port.NewsItemRepository;
import com.akazukin.domain.port.NewsSourceRepository;
import com.akazukin.domain.port.PostTemplateRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class NewsPostUseCase {

    private static final Logger LOG = Logger.getLogger(NewsPostUseCase.class.getName());

    private final NewsFeedFetcher newsFeedFetcher;
    private final NewsSourceRepository newsSourceRepository;
    private final NewsItemRepository newsItemRepository;
    private final AiTextGenerator aiTextGenerator;
    private final AiPersonaRepository aiPersonaRepository;
    private final PostTemplateRepository postTemplateRepository;

    @Inject
    public NewsPostUseCase(NewsFeedFetcher newsFeedFetcher,
                           NewsSourceRepository newsSourceRepository,
                           NewsItemRepository newsItemRepository,
                           AiTextGenerator aiTextGenerator,
                           AiPersonaRepository aiPersonaRepository,
                           PostTemplateRepository postTemplateRepository) {
        this.newsFeedFetcher = newsFeedFetcher;
        this.newsSourceRepository = newsSourceRepository;
        this.newsItemRepository = newsItemRepository;
        this.aiTextGenerator = aiTextGenerator;
        this.aiPersonaRepository = aiPersonaRepository;
        this.postTemplateRepository = postTemplateRepository;
    }

    @Transactional
    public NewsPostGeneratedDto fetchAndGeneratePost(UUID userId, UUID sourceId) {
        long perfStart = System.nanoTime();
        try {
            NewsSource source = newsSourceRepository.findById(sourceId)
                    .orElseThrow(() -> new DomainException("SOURCE_NOT_FOUND",
                            "News source not found: " + sourceId));

            if (!source.getUserId().equals(userId)) {
                throw new DomainException("FORBIDDEN", "You do not own this news source");
            }

            List<NewsItem> fetched = newsFeedFetcher.fetch(source);
            if (fetched.isEmpty()) {
                throw new DomainException("NO_NEWS", "No news items found from source: " + source.getName());
            }

            NewsItem latest = fetched.get(0);

            List<NewsItem> existing = newsItemRepository.findBySourceId(sourceId, 0, 100);
            for (NewsItem item : fetched) {
                boolean alreadyExists = existing.stream()
                        .anyMatch(e -> e.getUrl() != null && e.getUrl().equals(item.getUrl()));
                if (!alreadyExists) {
                    latest = item;
                    break;
                }
            }

            newsItemRepository.save(latest);

            String prompt = String.format(
                    "Write a concise and engaging social media post about the following news article. "
                            + "Include a brief summary and a call to action. Do NOT include hashtags unless they are highly relevant.\n\n"
                            + "Title: %s\nSummary: %s\nURL: %s",
                    latest.getTitle(),
                    latest.getSummary() != null ? latest.getSummary() : "",
                    latest.getUrl() != null ? latest.getUrl() : ""
            );

            AiPrompt aiPrompt = new AiPrompt(null, prompt, 0.7, 512);
            AiResponse response = aiTextGenerator.generate(aiPrompt);

            LOG.log(Level.INFO, "Generated post from news source {0} for user {1}",
                    new Object[]{sourceId, userId});

            return new NewsPostGeneratedDto(
                    response.generatedText(),
                    latest.getTitle(),
                    latest.getUrl()
            );
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"NewsPostUseCase.fetchAndGeneratePost", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"NewsPostUseCase.fetchAndGeneratePost", perfMs});
            }
        }
    }

    public List<NewsSourceDto> listSources(UUID userId) {
        long perfStart = System.nanoTime();
        try {
            return newsSourceRepository.findByUserId(userId).stream()
                    .map(this::toDto)
                    .toList();
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"NewsPostUseCase.listSources", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"NewsPostUseCase.listSources", perfMs});
            }
        }
    }

    @Transactional
    public NewsSourceDto addSource(UUID userId, NewsSourceRequestDto request) {
        long perfStart = System.nanoTime();
        try {
            if (request.name() == null || request.name().isBlank()) {
                throw new DomainException("INVALID_INPUT", "Source name is required");
            }
            if (request.url() == null || request.url().isBlank()) {
                throw new DomainException("INVALID_INPUT", "Source URL is required");
            }

            NewsSource source = new NewsSource(
                    UUID.randomUUID(),
                    userId,
                    request.name(),
                    request.url(),
                    request.sourceType() != null ? request.sourceType() : "RSS",
                    true,
                    Instant.now()
            );

            NewsSource saved = newsSourceRepository.save(source);
            LOG.log(Level.INFO, "News source added: {0} for user {1}",
                    new Object[]{saved.getId(), userId});

            return toDto(saved);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"NewsPostUseCase.addSource", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"NewsPostUseCase.addSource", perfMs});
            }
        }
    }

    @Transactional
    public void removeSource(UUID sourceId, UUID userId) {
        long perfStart = System.nanoTime();
        try {
            NewsSource source = newsSourceRepository.findById(sourceId)
                    .orElseThrow(() -> new DomainException("SOURCE_NOT_FOUND",
                            "News source not found: " + sourceId));

            if (!source.getUserId().equals(userId)) {
                throw new DomainException("FORBIDDEN", "You do not own this news source");
            }

            newsItemRepository.deleteBySourceId(sourceId);
            newsSourceRepository.deleteById(sourceId);
            LOG.log(Level.INFO, "News source removed: {0}", sourceId);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"NewsPostUseCase.removeSource", perfMs});
            } else {
                LOG.log(Level.FINE, "[PERF] {0} took {1}ms", new Object[]{"NewsPostUseCase.removeSource", perfMs});
            }
        }
    }

    @Transactional
    public List<NewsItemDto> fetchArticles(UUID userId, UUID sourceId) {
        long perfStart = System.nanoTime();
        try {
            NewsSource source = newsSourceRepository.findById(sourceId)
                    .orElseThrow(() -> new DomainException("SOURCE_NOT_FOUND",
                            "News source not found: " + sourceId));

            if (!source.getUserId().equals(userId)) {
                throw new DomainException("FORBIDDEN", "You do not own this news source");
            }

            List<NewsItem> fetched = newsFeedFetcher.fetch(source);
            List<NewsItem> existing = newsItemRepository.findBySourceId(sourceId, 0, 200);

            List<NewsItemDto> result = new ArrayList<>();
            for (NewsItem item : fetched) {
                boolean alreadyExists = existing.stream()
                        .anyMatch(e -> e.getUrl() != null && e.getUrl().equals(item.getUrl()));
                if (!alreadyExists) {
                    newsItemRepository.save(item);
                }
                result.add(new NewsItemDto(
                        item.getId(), sourceId, source.getName(),
                        item.getTitle(), item.getUrl(), item.getSummary(), item.getPublishedAt()
                ));
            }

            return result;
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"NewsPostUseCase.fetchArticles", perfMs});
            }
        }
    }

    public List<NewsItemDto> listArticles(UUID userId) {
        long perfStart = System.nanoTime();
        try {
            List<NewsSource> sources = newsSourceRepository.findByUserId(userId);
            List<NewsItemDto> allItems = new ArrayList<>();
            for (NewsSource source : sources) {
                List<NewsItem> items = newsItemRepository.findBySourceId(source.getId(), 0, 50);
                for (NewsItem item : items) {
                    allItems.add(new NewsItemDto(
                            item.getId(), source.getId(), source.getName(),
                            item.getTitle(), item.getUrl(), item.getSummary(), item.getPublishedAt()
                    ));
                }
            }
            allItems.sort((a, b) -> {
                if (a.publishedAt() == null && b.publishedAt() == null) return 0;
                if (a.publishedAt() == null) return 1;
                if (b.publishedAt() == null) return -1;
                return b.publishedAt().compareTo(a.publishedAt());
            });
            return allItems;
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"NewsPostUseCase.listArticles", perfMs});
            }
        }
    }

    public NewsPostIdeaResponseDto generatePostIdea(UUID userId, NewsPostIdeaRequestDto request) {
        long perfStart = System.nanoTime();
        try {
            NewsItem newsItem = newsItemRepository.findById(request.newsItemId())
                    .orElseThrow(() -> new DomainException("NOT_FOUND", "News item not found: " + request.newsItemId()));

            AiPersona persona = aiPersonaRepository.findById(request.personaId())
                    .orElseThrow(() -> new DomainException("NOT_FOUND", "Persona not found: " + request.personaId()));

            if (!persona.getUserId().equals(userId)) {
                throw new DomainException("FORBIDDEN", "You do not own this persona");
            }

            String userInput = String.format(
                    "Create a social media post about this news article, written in the style of this persona.\n\n"
                            + "Title: %s\nSummary: %s\nURL: %s\n\n"
                            + "Output ONLY the post text, nothing else.",
                    newsItem.getTitle(),
                    newsItem.getSummary() != null ? newsItem.getSummary() : "",
                    newsItem.getUrl() != null ? newsItem.getUrl() : ""
            );

            AiResponse response = aiTextGenerator.generateWithPersona(persona, userInput);

            return new NewsPostIdeaResponseDto(
                    newsItem.getId(),
                    newsItem.getTitle(),
                    persona.getName(),
                    response.generatedText()
            );
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"NewsPostUseCase.generatePostIdea", perfMs});
            }
        }
    }

    public MultiAngleResponseDto generateMultiAngle(UUID userId, UUID newsItemId) {
        long perfStart = System.nanoTime();
        try {
            NewsItem newsItem = newsItemRepository.findById(newsItemId)
                    .orElseThrow(() -> new DomainException("NOT_FOUND", "News item not found: " + newsItemId));

            String prompt = String.format(
                    "Generate 3 different social media post angles for this news article.\n\n"
                            + "Title: %s\nSummary: %s\n\n"
                            + "Output EXACTLY 3 angles in this format (use | as delimiter, one angle per block):\n"
                            + "BREAKING|<breaking news style post text>\n"
                            + "ANALYSIS|<analytical deep-dive post text>\n"
                            + "DIALOGUE|<conversational engagement-focused post text>\n\n"
                            + "Output ONLY the 3 lines, no extra text.",
                    newsItem.getTitle(),
                    newsItem.getSummary() != null ? newsItem.getSummary() : ""
            );

            AiPrompt aiPrompt = new AiPrompt(null, prompt, 0.8, 1024);
            AiResponse response = aiTextGenerator.generate(aiPrompt);

            List<AngleDto> angles = parseAngles(response.generatedText());

            return new MultiAngleResponseDto(newsItem.getId(), newsItem.getTitle(), angles);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"NewsPostUseCase.generateMultiAngle", perfMs});
            }
        }
    }

    public TemplateMatchResponseDto matchTemplates(UUID userId, UUID newsItemId) {
        long perfStart = System.nanoTime();
        try {
            NewsItem newsItem = newsItemRepository.findById(newsItemId)
                    .orElseThrow(() -> new DomainException("NOT_FOUND", "News item not found: " + newsItemId));

            List<PostTemplate> templates = postTemplateRepository.findByUserId(userId);
            if (templates.isEmpty()) {
                return new TemplateMatchResponseDto(newsItem.getId(), newsItem.getTitle(), List.of());
            }

            StringBuilder templateList = new StringBuilder();
            for (int i = 0; i < templates.size(); i++) {
                templateList.append(String.format("%d. %s (category: %s)\n",
                        i + 1, templates.get(i).getName(), templates.get(i).getCategory()));
            }

            String prompt = String.format(
                    "Match the following news article to the best templates and score their fit (0-100).\n\n"
                            + "News Title: %s\nNews Summary: %s\n\n"
                            + "Templates:\n%s\n"
                            + "Output EXACTLY one line per template in format: RANK|TEMPLATE_INDEX|SCORE|REASON\n"
                            + "Sort by score descending. Output top 3 only.",
                    newsItem.getTitle(),
                    newsItem.getSummary() != null ? newsItem.getSummary() : "",
                    templateList
            );

            AiPrompt aiPrompt = new AiPrompt(null, prompt, 0.5, 512);
            AiResponse response = aiTextGenerator.generate(aiPrompt);

            List<TemplateMatchItemDto> matches = parseTemplateMatches(response.generatedText(), templates);

            return new TemplateMatchResponseDto(newsItem.getId(), newsItem.getTitle(), matches);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"NewsPostUseCase.matchTemplates", perfMs});
            }
        }
    }

    public NewsABTestResponseDto generateABTest(UUID userId, NewsABTestRequestDto request) {
        long perfStart = System.nanoTime();
        try {
            NewsItem newsItem = newsItemRepository.findById(request.newsItemId())
                    .orElseThrow(() -> new DomainException("NOT_FOUND", "News item not found: " + request.newsItemId()));

            AiPersona personaA = aiPersonaRepository.findById(request.personaIdA())
                    .orElseThrow(() -> new DomainException("NOT_FOUND", "Persona A not found: " + request.personaIdA()));
            AiPersona personaB = aiPersonaRepository.findById(request.personaIdB())
                    .orElseThrow(() -> new DomainException("NOT_FOUND", "Persona B not found: " + request.personaIdB()));

            if (!personaA.getUserId().equals(userId) || !personaB.getUserId().equals(userId)) {
                throw new DomainException("FORBIDDEN", "You do not own one or more personas");
            }

            if (personaA.getId().equals(personaB.getId())) {
                throw new DomainException("INVALID_INPUT", "Please select two different personas");
            }

            String userInputA = String.format(
                    "Create a social media post about this news, in the style of this persona.\n\n"
                            + "Title: %s\nSummary: %s\n\nOutput ONLY the post text.",
                    newsItem.getTitle(), newsItem.getSummary() != null ? newsItem.getSummary() : ""
            );
            String userInputB = userInputA;

            AiResponse responseA = aiTextGenerator.generateWithPersona(personaA, userInputA);
            AiResponse responseB = aiTextGenerator.generateWithPersona(personaB, userInputB);

            ABPanelDto panelA = new ABPanelDto("A", personaA.getId(), personaA.getName(), responseA.generatedText());
            ABPanelDto panelB = new ABPanelDto("B", personaB.getId(), personaB.getName(), responseB.generatedText());

            return new NewsABTestResponseDto(newsItem.getId(), newsItem.getTitle(), panelA, panelB);
        } finally {
            long perfMs = (System.nanoTime() - perfStart) / 1_000_000;
            if (perfMs >= 100) {
                LOG.log(Level.WARNING, "[PERF] {0} took {1}ms", new Object[]{"NewsPostUseCase.generateABTest", perfMs});
            }
        }
    }

    private List<AngleDto> parseAngles(String text) {
        List<AngleDto> angles = new ArrayList<>();
        String[] lines = text.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            String[] parts = trimmed.split("\\|", 2);
            if (parts.length >= 2) {
                String type = parts[0].trim().toUpperCase();
                String body = parts[1].trim();
                List<PlatformFitDto> fits = switch (type) {
                    case "BREAKING" -> List.of(
                            new PlatformFitDto("X (Twitter)", "high"),
                            new PlatformFitDto("Facebook", "mid"));
                    case "ANALYSIS" -> List.of(
                            new PlatformFitDto("note", "high"),
                            new PlatformFitDto("LinkedIn", "high"),
                            new PlatformFitDto("X (Twitter)", "mid"));
                    case "DIALOGUE" -> List.of(
                            new PlatformFitDto("Instagram", "high"),
                            new PlatformFitDto("X (Twitter)", "mid"));
                    default -> List.of();
                };
                String displayType = switch (type) {
                    case "BREAKING" -> "breaking";
                    case "ANALYSIS" -> "analysis";
                    case "DIALOGUE" -> "dialogue";
                    default -> type.toLowerCase();
                };
                angles.add(new AngleDto(displayType, body, fits));
            }
        }
        if (angles.isEmpty()) {
            angles.add(new AngleDto("breaking", text, List.of(new PlatformFitDto("X (Twitter)", "high"))));
        }
        return angles;
    }

    private List<TemplateMatchItemDto> parseTemplateMatches(String text, List<PostTemplate> templates) {
        List<TemplateMatchItemDto> matches = new ArrayList<>();
        String[] lines = text.split("\n");
        int rank = 1;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            String[] parts = trimmed.split("\\|");
            if (parts.length >= 4) {
                try {
                    int templateIndex = Integer.parseInt(parts[1].trim()) - 1;
                    int score = Integer.parseInt(parts[2].trim());
                    String reason = parts[3].trim();
                    if (templateIndex >= 0 && templateIndex < templates.size()) {
                        PostTemplate tpl = templates.get(templateIndex);
                        matches.add(new TemplateMatchItemDto(rank++, tpl.getId(), tpl.getName(), reason, score));
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            if (rank > 3) break;
        }
        return matches;
    }

    private NewsSourceDto toDto(NewsSource source) {
        return new NewsSourceDto(
                source.getId(),
                source.getName(),
                source.getUrl(),
                source.getSourceType(),
                source.isActive(),
                source.getCreatedAt()
        );
    }
}
