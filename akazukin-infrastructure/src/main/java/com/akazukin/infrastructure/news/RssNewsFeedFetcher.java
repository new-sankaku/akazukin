package com.akazukin.infrastructure.news;

import com.akazukin.domain.model.NewsItem;
import com.akazukin.domain.model.NewsSource;
import com.akazukin.domain.port.NewsFeedFetcher;
import jakarta.enterprise.context.ApplicationScoped;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@ApplicationScoped
public class RssNewsFeedFetcher implements NewsFeedFetcher {

    private static final Logger LOG = Logger.getLogger(RssNewsFeedFetcher.class.getName());
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(15);

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .version(HttpClient.Version.HTTP_2)
            .build();

    @Override
    public List<NewsItem> fetch(NewsSource source) {
        LOG.log(Level.INFO, "Fetching RSS feed from: {0}", source.getUrl());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(source.getUrl()))
                .header("Accept", "application/rss+xml, application/xml, text/xml")
                .header("User-Agent", "Akazukin/1.0 RSS Reader")
                .GET()
                .timeout(READ_TIMEOUT)
                .build();

        try {
            HttpResponse<InputStream> response = HTTP_CLIENT.send(request,
                    HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() >= 400) {
                throw new IOException("HTTP " + response.statusCode() + " fetching RSS feed: " + source.getUrl());
            }

            return parseRss(response.body(), source.getId());
        } catch (IOException e) {
            throw new RssFeedException("Failed to fetch RSS feed: " + source.getUrl(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RssFeedException("RSS fetch interrupted: " + source.getUrl(), e);
        }
    }

    private List<NewsItem> parseRss(InputStream inputStream, UUID sourceId) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputStream);

            List<NewsItem> items = new ArrayList<>();
            NodeList itemNodes = doc.getElementsByTagName("item");
            Instant now = Instant.now();

            for (int i = 0; i < itemNodes.getLength(); i++) {
                Element itemElement = (Element) itemNodes.item(i);

                String title = getElementText(itemElement, "title");
                String link = getElementText(itemElement, "link");
                String description = getElementText(itemElement, "description");
                String pubDateStr = getElementText(itemElement, "pubDate");

                Instant publishedAt = parsePubDate(pubDateStr);

                if (title != null && !title.isBlank()) {
                    items.add(new NewsItem(
                            UUID.randomUUID(),
                            sourceId,
                            title.length() > 500 ? title.substring(0, 500) : title,
                            link,
                            description,
                            publishedAt,
                            now
                    ));
                }
            }

            LOG.log(Level.INFO, "Parsed {0} items from RSS feed", items.size());
            return items;
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RssFeedException("Failed to parse RSS XML", e);
        }
    }

    private String getElementText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            String text = nodes.item(0).getTextContent();
            return text != null ? text.trim() : null;
        }
        return null;
    }

    private Instant parsePubDate(String pubDateStr) {
        if (pubDateStr == null || pubDateStr.isBlank()) {
            return null;
        }
        try {
            return ZonedDateTime.parse(pubDateStr, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (DateTimeParseException e) {
            LOG.log(Level.FINE, "Could not parse pubDate: {0}", pubDateStr);
            return null;
        }
    }

    public static class RssFeedException extends RuntimeException {
        public RssFeedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
