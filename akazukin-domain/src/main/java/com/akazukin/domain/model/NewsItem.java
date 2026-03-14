package com.akazukin.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class NewsItem {

    private UUID id;
    private UUID sourceId;
    private String title;
    private String url;
    private String summary;
    private Instant publishedAt;
    private Instant fetchedAt;

    public NewsItem(UUID id, UUID sourceId, String title, String url, String summary,
                    Instant publishedAt, Instant fetchedAt) {
        this.id = id;
        this.sourceId = sourceId;
        this.title = title;
        this.url = url;
        this.summary = summary;
        this.publishedAt = publishedAt;
        this.fetchedAt = fetchedAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSourceId() {
        return sourceId;
    }

    public void setSourceId(UUID sourceId) {
        this.sourceId = sourceId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(Instant fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NewsItem newsItem = (NewsItem) o;
        return Objects.equals(id, newsItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
