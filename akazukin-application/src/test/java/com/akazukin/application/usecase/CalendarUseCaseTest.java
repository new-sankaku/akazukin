package com.akazukin.application.usecase;

import com.akazukin.application.dto.AiPlanRequestDto;
import com.akazukin.application.dto.BridgeHolidayPlanDto;
import com.akazukin.application.dto.CalendarEntryDto;
import com.akazukin.application.dto.CalendarEntryRequestDto;
import com.akazukin.application.dto.CalendarTimelineEventDto;
import com.akazukin.application.dto.EngagementHeatmapDto;
import com.akazukin.application.dto.LinkageScenarioDto;
import com.akazukin.application.dto.TimeSlotMatrixDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.CalendarEntry;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.CalendarEntryRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

class CalendarUseCaseTest {

    private static final ZoneId JST = ZoneId.of("Asia/Tokyo");

    private InMemoryCalendarEntryRepository calendarEntryRepository;
    private CalendarUseCase calendarUseCase;
    private UUID userId;

    @BeforeEach
    void setUp() {
        calendarEntryRepository = new InMemoryCalendarEntryRepository();
        calendarUseCase = new CalendarUseCase(calendarEntryRepository);
        userId = UUID.randomUUID();
    }

    @Test
    void getEntries_returnsEntriesWithinDateRange() {
        Instant now = Instant.now();
        Instant from = now.minusSeconds(3600);
        Instant to = now.plusSeconds(3600);

        CalendarEntry entry = new CalendarEntry(
                UUID.randomUUID(), userId, null, "Test Entry", "desc",
                now, List.of(SnsPlatform.TWITTER), "#C8A96E", now, now
        );
        calendarEntryRepository.save(entry);

        List<CalendarEntryDto> result = calendarUseCase.getEntries(userId, from, to);

        assertEquals(1, result.size());
        assertEquals("Test Entry", result.get(0).title());
    }

    @Test
    void getEntries_returnsEmptyWhenNoEntriesInRange() {
        Instant from = Instant.now().minusSeconds(7200);
        Instant to = Instant.now().minusSeconds(3600);

        List<CalendarEntryDto> result = calendarUseCase.getEntries(userId, from, to);

        assertTrue(result.isEmpty());
    }

    @Test
    void getEntries_throwsWhenFromIsNull() {
        DomainException exception = assertThrows(DomainException.class,
                () -> calendarUseCase.getEntries(userId, null, Instant.now()));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void getEntries_throwsWhenToIsNull() {
        DomainException exception = assertThrows(DomainException.class,
                () -> calendarUseCase.getEntries(userId, Instant.now(), null));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void createEntry_createsEntryWithValidInput() {
        Instant scheduledAt = Instant.now().plusSeconds(3600);
        CalendarEntryRequestDto request = new CalendarEntryRequestDto(
                "New Entry", "description", scheduledAt, List.of("TWITTER"), "#FF0000"
        );

        CalendarEntryDto result = calendarUseCase.createEntry(userId, request);

        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals("New Entry", result.title());
        assertEquals("description", result.description());
        assertEquals(scheduledAt, result.scheduledAt());
        assertEquals(List.of("TWITTER"), result.platforms());
        assertEquals("#FF0000", result.color());
    }

    @Test
    void createEntry_usesDefaultColorWhenColorIsNull() {
        CalendarEntryRequestDto request = new CalendarEntryRequestDto(
                "Entry", "desc", Instant.now(), List.of("TWITTER"), null
        );

        CalendarEntryDto result = calendarUseCase.createEntry(userId, request);

        assertEquals("#C8A96E", result.color());
    }

    @Test
    void createEntry_throwsWhenTitleIsNull() {
        CalendarEntryRequestDto request = new CalendarEntryRequestDto(
                null, "desc", Instant.now(), List.of("TWITTER"), "#FF0000"
        );

        DomainException exception = assertThrows(DomainException.class,
                () -> calendarUseCase.createEntry(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void createEntry_throwsWhenTitleIsBlank() {
        CalendarEntryRequestDto request = new CalendarEntryRequestDto(
                "  ", "desc", Instant.now(), List.of("TWITTER"), "#FF0000"
        );

        DomainException exception = assertThrows(DomainException.class,
                () -> calendarUseCase.createEntry(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void createEntry_throwsWhenScheduledAtIsNull() {
        CalendarEntryRequestDto request = new CalendarEntryRequestDto(
                "Entry", "desc", null, List.of("TWITTER"), "#FF0000"
        );

        DomainException exception = assertThrows(DomainException.class,
                () -> calendarUseCase.createEntry(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void createEntry_throwsWhenPlatformIsInvalid() {
        CalendarEntryRequestDto request = new CalendarEntryRequestDto(
                "Entry", "desc", Instant.now(), List.of("INVALID_PLATFORM"), "#FF0000"
        );

        DomainException exception = assertThrows(DomainException.class,
                () -> calendarUseCase.createEntry(userId, request));
        assertEquals("INVALID_PLATFORM", exception.getErrorCode());
    }

    @Test
    void updateEntry_updatesExistingEntry() {
        Instant now = Instant.now();
        UUID entryId = UUID.randomUUID();
        CalendarEntry entry = new CalendarEntry(
                entryId, userId, null, "Old Title", "old desc",
                now, List.of(SnsPlatform.TWITTER), "#C8A96E", now, now
        );
        calendarEntryRepository.save(entry);

        Instant newSchedule = now.plusSeconds(7200);
        CalendarEntryRequestDto request = new CalendarEntryRequestDto(
                "New Title", "new desc", newSchedule, List.of("BLUESKY"), "#FF0000"
        );

        CalendarEntryDto result = calendarUseCase.updateEntry(entryId, userId, request);

        assertEquals("New Title", result.title());
        assertEquals("new desc", result.description());
        assertEquals(newSchedule, result.scheduledAt());
        assertEquals(List.of("BLUESKY"), result.platforms());
        assertEquals("#FF0000", result.color());
    }

    @Test
    void updateEntry_throwsWhenEntryNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        CalendarEntryRequestDto request = new CalendarEntryRequestDto(
                "Title", "desc", Instant.now(), List.of("TWITTER"), "#FF0000"
        );

        DomainException exception = assertThrows(DomainException.class,
                () -> calendarUseCase.updateEntry(nonExistentId, userId, request));
        assertEquals("ENTRY_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void updateEntry_throwsForbiddenWhenNotOwner() {
        Instant now = Instant.now();
        UUID entryId = UUID.randomUUID();
        CalendarEntry entry = new CalendarEntry(
                entryId, userId, null, "Title", "desc",
                now, List.of(SnsPlatform.TWITTER), "#C8A96E", now, now
        );
        calendarEntryRepository.save(entry);

        UUID otherUserId = UUID.randomUUID();
        CalendarEntryRequestDto request = new CalendarEntryRequestDto(
                "New Title", "desc", now, List.of("TWITTER"), "#FF0000"
        );

        DomainException exception = assertThrows(DomainException.class,
                () -> calendarUseCase.updateEntry(entryId, otherUserId, request));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void updateEntry_throwsWhenTitleIsBlank() {
        Instant now = Instant.now();
        UUID entryId = UUID.randomUUID();
        CalendarEntry entry = new CalendarEntry(
                entryId, userId, null, "Title", "desc",
                now, List.of(SnsPlatform.TWITTER), "#C8A96E", now, now
        );
        calendarEntryRepository.save(entry);

        CalendarEntryRequestDto request = new CalendarEntryRequestDto(
                "", "desc", now, List.of("TWITTER"), "#FF0000"
        );

        DomainException exception = assertThrows(DomainException.class,
                () -> calendarUseCase.updateEntry(entryId, userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void updateEntry_keepsExistingColorWhenNewColorIsNull() {
        Instant now = Instant.now();
        UUID entryId = UUID.randomUUID();
        CalendarEntry entry = new CalendarEntry(
                entryId, userId, null, "Title", "desc",
                now, List.of(SnsPlatform.TWITTER), "#ABCDEF", now, now
        );
        calendarEntryRepository.save(entry);

        CalendarEntryRequestDto request = new CalendarEntryRequestDto(
                "Updated", "desc", now, List.of("TWITTER"), null
        );

        CalendarEntryDto result = calendarUseCase.updateEntry(entryId, userId, request);

        assertEquals("#ABCDEF", result.color());
    }

    @Test
    void deleteEntry_deletesExistingEntry() {
        Instant now = Instant.now();
        UUID entryId = UUID.randomUUID();
        CalendarEntry entry = new CalendarEntry(
                entryId, userId, null, "Title", "desc",
                now, List.of(SnsPlatform.TWITTER), "#C8A96E", now, now
        );
        calendarEntryRepository.save(entry);

        calendarUseCase.deleteEntry(entryId, userId);

        assertTrue(calendarEntryRepository.findById(entryId).isEmpty());
    }

    @Test
    void deleteEntry_throwsWhenEntryNotFound() {
        UUID nonExistentId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> calendarUseCase.deleteEntry(nonExistentId, userId));
        assertEquals("ENTRY_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void deleteEntry_throwsForbiddenWhenNotOwner() {
        Instant now = Instant.now();
        UUID entryId = UUID.randomUUID();
        CalendarEntry entry = new CalendarEntry(
                entryId, userId, null, "Title", "desc",
                now, List.of(SnsPlatform.TWITTER), "#C8A96E", now, now
        );
        calendarEntryRepository.save(entry);

        UUID otherUserId = UUID.randomUUID();

        DomainException exception = assertThrows(DomainException.class,
                () -> calendarUseCase.deleteEntry(entryId, otherUserId));
        assertEquals("FORBIDDEN", exception.getErrorCode());
    }

    @Test
    void getTimelineEvents_returnsEventsForYear() {
        List<CalendarTimelineEventDto> events = calendarUseCase.getTimelineEvents(2026);

        assertFalse(events.isEmpty());

        boolean hasHoliday = events.stream().anyMatch(e -> "holiday".equals(e.type()));
        boolean hasSolarTerm = events.stream().anyMatch(e -> "solar_term".equals(e.type()));
        boolean hasSeasonalEvent = events.stream().anyMatch(e -> "seasonal_event".equals(e.type()));

        assertTrue(hasHoliday);
        assertTrue(hasSolarTerm);
        assertTrue(hasSeasonalEvent);
    }

    @Test
    void getTimelineEvents_returnsSortedByDate() {
        List<CalendarTimelineEventDto> events = calendarUseCase.getTimelineEvents(2026);

        for (int i = 1; i < events.size(); i++) {
            assertTrue(events.get(i).date().compareTo(events.get(i - 1).date()) >= 0);
        }
    }

    @Test
    void getEngagementHeatmap_returnsLevelsForEntireMonth() {
        EngagementHeatmapDto result = calendarUseCase.getEngagementHeatmap(userId, 2026, 3);

        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = start.plusMonths(1);
        int expectedDays = (int) (end.toEpochDay() - start.toEpochDay());

        assertEquals(expectedDays, result.levels().size());
    }

    @Test
    void getEngagementHeatmap_returnsCorrectLevelForMultipleEntries() {
        LocalDate targetDate = LocalDate.of(2026, 3, 15);
        Instant scheduledAt = targetDate.atTime(12, 0).atZone(JST).toInstant();
        Instant now = Instant.now();

        for (int i = 0; i < 3; i++) {
            CalendarEntry entry = new CalendarEntry(
                    UUID.randomUUID(), userId, null, "Entry " + i, "desc",
                    scheduledAt.plusSeconds(i * 60), List.of(SnsPlatform.TWITTER),
                    "#C8A96E", now, now
            );
            calendarEntryRepository.save(entry);
        }

        EngagementHeatmapDto result = calendarUseCase.getEngagementHeatmap(userId, 2026, 3);

        assertEquals(3, result.levels().get(targetDate));
    }

    @Test
    void generateAiPlan_createsEntriesWithValidInput() {
        AiPlanRequestDto request = new AiPlanRequestDto("Spring Campaign", List.of("TWITTER", "BLUESKY"), 3);

        List<CalendarEntryDto> result = calendarUseCase.generateAiPlan(userId, request);

        assertEquals(3, result.size());
        for (CalendarEntryDto entry : result) {
            assertNotNull(entry.id());
            assertTrue(entry.title().startsWith("Spring Campaign"));
            assertNotNull(entry.scheduledAt());
        }
    }

    @Test
    void generateAiPlan_distributesAcrossPlatforms() {
        AiPlanRequestDto request = new AiPlanRequestDto("Theme", List.of("TWITTER", "BLUESKY"), 4);

        List<CalendarEntryDto> result = calendarUseCase.generateAiPlan(userId, request);

        long twitterCount = result.stream()
                .filter(e -> e.title().contains("TWITTER"))
                .count();
        long blueskyCount = result.stream()
                .filter(e -> e.title().contains("BLUESKY"))
                .count();

        assertEquals(2, twitterCount);
        assertEquals(2, blueskyCount);
    }

    @Test
    void generateAiPlan_throwsWhenThemeIsNull() {
        AiPlanRequestDto request = new AiPlanRequestDto(null, List.of("TWITTER"), 3);

        DomainException exception = assertThrows(DomainException.class,
                () -> calendarUseCase.generateAiPlan(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void generateAiPlan_throwsWhenThemeIsBlank() {
        AiPlanRequestDto request = new AiPlanRequestDto("  ", List.of("TWITTER"), 3);

        DomainException exception = assertThrows(DomainException.class,
                () -> calendarUseCase.generateAiPlan(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void generateAiPlan_throwsWhenPlatformsIsNull() {
        AiPlanRequestDto request = new AiPlanRequestDto("Theme", null, 3);

        DomainException exception = assertThrows(DomainException.class,
                () -> calendarUseCase.generateAiPlan(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void generateAiPlan_throwsWhenPlatformsIsEmpty() {
        AiPlanRequestDto request = new AiPlanRequestDto("Theme", List.of(), 3);

        DomainException exception = assertThrows(DomainException.class,
                () -> calendarUseCase.generateAiPlan(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void generateAiPlan_throwsWhenPostCountIsZero() {
        AiPlanRequestDto request = new AiPlanRequestDto("Theme", List.of("TWITTER"), 0);

        DomainException exception = assertThrows(DomainException.class,
                () -> calendarUseCase.generateAiPlan(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void generateAiPlan_throwsWhenPostCountExceedsFifty() {
        AiPlanRequestDto request = new AiPlanRequestDto("Theme", List.of("TWITTER"), 51);

        DomainException exception = assertThrows(DomainException.class,
                () -> calendarUseCase.generateAiPlan(userId, request));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void getBridgeHolidayPlans_returnsPlansForYear() {
        List<BridgeHolidayPlanDto> plans = calendarUseCase.getBridgeHolidayPlans(2026);

        assertFalse(plans.isEmpty());
        assertTrue(plans.stream().anyMatch(p -> "GW".equals(p.periodName())));
        assertTrue(plans.stream().anyMatch(p -> "お盆".equals(p.periodName())));
        assertTrue(plans.stream().anyMatch(p -> "年末年始".equals(p.periodName())));
    }

    @Test
    void getBridgeHolidayPlans_eachPlanHasPhases() {
        List<BridgeHolidayPlanDto> plans = calendarUseCase.getBridgeHolidayPlans(2026);

        for (BridgeHolidayPlanDto plan : plans) {
            assertFalse(plan.phases().isEmpty());
        }
    }

    @Test
    void applyBridgePlan_createsEntriesForExistingPlan() {
        List<CalendarEntryDto> result = calendarUseCase.applyBridgePlan(userId, "GW", 2026);

        assertFalse(result.isEmpty());
        for (CalendarEntryDto entry : result) {
            assertNotNull(entry.id());
            assertNotNull(entry.title());
            assertNotNull(entry.scheduledAt());
        }
    }

    @Test
    void applyBridgePlan_throwsWhenPlanNotFound() {
        DomainException exception = assertThrows(DomainException.class,
                () -> calendarUseCase.applyBridgePlan(userId, "NonExistentPlan", 2026));
        assertEquals("PLAN_NOT_FOUND", exception.getErrorCode());
    }

    @Test
    void getTimeSlotMatrix_returnsAllPlatformsAndSlots() {
        TimeSlotMatrixDto result = calendarUseCase.getTimeSlotMatrix(userId);

        assertFalse(result.platforms().isEmpty());
        assertFalse(result.dayLabels().isEmpty());
        assertFalse(result.hourLabels().isEmpty());
        assertEquals(result.platforms().size(), result.engagementLevels().size());

        for (Map.Entry<String, Map<String, Integer>> entry : result.engagementLevels().entrySet()) {
            int expectedCells = result.dayLabels().size() * result.hourLabels().size();
            assertEquals(expectedCells, entry.getValue().size());
        }
    }

    @Test
    void getTimeSlotMatrix_levelsAreBetweenZeroAndFive() {
        TimeSlotMatrixDto result = calendarUseCase.getTimeSlotMatrix(userId);

        for (Map<String, Integer> cellLevels : result.engagementLevels().values()) {
            for (int level : cellLevels.values()) {
                assertTrue(level >= 0 && level <= 5);
            }
        }
    }

    @Test
    void createEntryFromSlot_createsEntryWithValidDayAndHour() {
        CalendarEntryDto result = calendarUseCase.createEntryFromSlot(userId, "X", "月", "9-12");

        assertNotNull(result);
        assertNotNull(result.id());
        assertTrue(result.title().contains("X"));
        assertTrue(result.title().contains("月"));
        assertTrue(result.title().contains("9-12"));
    }

    @Test
    void createEntryFromSlot_throwsWhenDayIsInvalid() {
        DomainException exception = assertThrows(DomainException.class,
                () -> calendarUseCase.createEntryFromSlot(userId, "X", "無", "9-12"));
        assertEquals("INVALID_INPUT", exception.getErrorCode());
    }

    @Test
    void getLinkageScenarios_returnsScenarios() {
        List<LinkageScenarioDto> result = calendarUseCase.getLinkageScenarios();

        assertFalse(result.isEmpty());
        for (LinkageScenarioDto scenario : result) {
            assertNotNull(scenario.name());
            assertFalse(scenario.steps().isEmpty());
            for (LinkageScenarioDto.StepDto step : scenario.steps()) {
                assertNotNull(step.platform());
                assertNotNull(step.action());
            }
        }
    }

    @Test
    void applyScenario_createsEntriesForExistingScenario() {
        List<CalendarEntryDto> result = calendarUseCase.applyScenario(userId, "長文展開型");

        assertEquals(3, result.size());
        for (CalendarEntryDto entry : result) {
            assertNotNull(entry.id());
            assertTrue(entry.title().startsWith("長文展開型"));
        }
    }

    @Test
    void applyScenario_throwsWhenScenarioNotFound() {
        DomainException exception = assertThrows(DomainException.class,
                () -> calendarUseCase.applyScenario(userId, "NonExistent"));
        assertEquals("SCENARIO_NOT_FOUND", exception.getErrorCode());
    }

    private static class InMemoryCalendarEntryRepository implements CalendarEntryRepository {

        private final Map<UUID, CalendarEntry> store = new HashMap<>();

        @Override
        public Optional<CalendarEntry> findById(UUID id) {
            return Optional.ofNullable(store.get(id));
        }

        @Override
        public List<CalendarEntry> findByUserIdAndDateRange(UUID userId, Instant from, Instant to) {
            return store.values().stream()
                    .filter(entry -> entry.getUserId().equals(userId))
                    .filter(entry -> {
                        Instant scheduled = entry.getScheduledAt();
                        return scheduled != null
                                && !scheduled.isBefore(from)
                                && scheduled.isBefore(to);
                    })
                    .toList();
        }

        @Override
        public CalendarEntry save(CalendarEntry calendarEntry) {
            store.put(calendarEntry.getId(), calendarEntry);
            return calendarEntry;
        }

        @Override
        public void deleteById(UUID id) {
            store.remove(id);
        }
    }
}
