package com.akazukin.domain.service;

import com.akazukin.domain.model.SeasonalEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeasonalEventProviderTest {

    private SeasonalEventProvider provider;

    @BeforeEach
    void setUp() {
        provider = new SeasonalEventProvider();
    }

    @Test
    void getEvents_returnsNonEmptyList() {
        List<SeasonalEvent> events = provider.getEvents(2026);

        assertFalse(events.isEmpty());
    }

    @Test
    void getEvents_returns16Events() {
        List<SeasonalEvent> events = provider.getEvents(2026);

        assertEquals(16, events.size());
    }

    @Test
    void getEvents_containsNanakusa() {
        List<SeasonalEvent> events = provider.getEvents(2026);

        boolean found = events.stream()
                .anyMatch(e -> e.name().equals("七草") && e.date().equals(LocalDate.of(2026, 1, 7)));
        assertTrue(found);
    }

    @Test
    void getEvents_containsSetsubun() {
        List<SeasonalEvent> events = provider.getEvents(2026);

        boolean found = events.stream()
                .anyMatch(e -> e.name().equals("節分") && e.date().equals(LocalDate.of(2026, 2, 3)));
        assertTrue(found);
    }

    @Test
    void getEvents_containsValentinesDay() {
        List<SeasonalEvent> events = provider.getEvents(2026);

        boolean found = events.stream()
                .anyMatch(e -> e.name().equals("バレンタインデー") && e.date().equals(LocalDate.of(2026, 2, 14)));
        assertTrue(found);
    }

    @Test
    void getEvents_containsHinamatsuri() {
        List<SeasonalEvent> events = provider.getEvents(2026);

        boolean found = events.stream()
                .anyMatch(e -> e.name().equals("ひな祭り") && e.date().equals(LocalDate.of(2026, 3, 3)));
        assertTrue(found);
    }

    @Test
    void getEvents_containsTanabata() {
        List<SeasonalEvent> events = provider.getEvents(2026);

        boolean found = events.stream()
                .anyMatch(e -> e.name().equals("七夕") && e.date().equals(LocalDate.of(2026, 7, 7)));
        assertTrue(found);
    }

    @Test
    void getEvents_containsHalloween() {
        List<SeasonalEvent> events = provider.getEvents(2026);

        boolean found = events.stream()
                .anyMatch(e -> e.name().equals("ハロウィン") && e.date().equals(LocalDate.of(2026, 10, 31)));
        assertTrue(found);
    }

    @Test
    void getEvents_containsChristmas() {
        List<SeasonalEvent> events = provider.getEvents(2026);

        boolean found = events.stream()
                .anyMatch(e -> e.name().equals("クリスマス") && e.date().equals(LocalDate.of(2026, 12, 25)));
        assertTrue(found);
    }

    @Test
    void getEvents_containsNewYearsEve() {
        List<SeasonalEvent> events = provider.getEvents(2026);

        boolean found = events.stream()
                .anyMatch(e -> e.name().equals("大晦日") && e.date().equals(LocalDate.of(2026, 12, 31)));
        assertTrue(found);
    }

    @Test
    void getEvents_allEventsHaveNameAndEnglishName() {
        List<SeasonalEvent> events = provider.getEvents(2026);

        for (SeasonalEvent event : events) {
            assertNotNull(event.name());
            assertNotNull(event.nameEn());
            assertFalse(event.name().isBlank());
            assertFalse(event.nameEn().isBlank());
        }
    }

    @Test
    void getEvents_allEventsHaveHint() {
        List<SeasonalEvent> events = provider.getEvents(2026);

        for (SeasonalEvent event : events) {
            assertNotNull(event.hint());
            assertNotNull(event.hintEn());
            assertFalse(event.hint().isBlank());
            assertFalse(event.hintEn().isBlank());
        }
    }

    @Test
    void getEvents_allEventsAreInRequestedYear() {
        List<SeasonalEvent> events = provider.getEvents(2026);

        for (SeasonalEvent event : events) {
            assertEquals(2026, event.date().getYear());
        }
    }

    @Test
    void getEvents_differentYearReturnsDifferentDates() {
        List<SeasonalEvent> events2026 = provider.getEvents(2026);
        List<SeasonalEvent> events2027 = provider.getEvents(2027);

        assertEquals(events2026.size(), events2027.size());
        assertFalse(events2026.get(0).date().equals(events2027.get(0).date()));
    }
}
