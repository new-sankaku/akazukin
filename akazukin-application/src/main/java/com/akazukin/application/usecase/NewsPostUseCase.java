package com.akazukin.application.usecase;

import com.akazukin.application.dto.NewsPostGeneratedDto;
import com.akazukin.application.dto.NewsSourceDto;
import com.akazukin.application.dto.NewsSourceRequestDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.AiPrompt;
import com.akazukin.domain.model.AiResponse;
import com.akazukin.domain.model.NewsItem;
import com.akazukin.domain.model.NewsSource;
import com.akazukin.domain.port.AiTextGenerator;
import com.akazukin.domain.port.NewsFeedFetcher;
import com.akazukin.domain.port.NewsItemRepository;
import com.akazukin.domain.port.NewsSourceRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
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

    @Inject
    public NewsPostUseCase(NewsFeedFetcher newsFeedFetcher,
                           NewsSourceRepository newsSourceRepository,
                           NewsItemRepository newsItemRepository,
                           AiTextGenerator aiTextGenerator) {
        this.newsFeedFetcher = newsFeedFetcher;
        this.newsSourceRepository = newsSourceRepository;
        this.newsItemRepository = newsItemRepository;
        this.aiTextGenerator = aiTextGenerator;
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
