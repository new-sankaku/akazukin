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
import com.akazukin.domain.model.BridgeHolidayPeriod;
import com.akazukin.domain.model.BridgePhase;
import com.akazukin.domain.model.CalendarEntry;
import com.akazukin.domain.model.JapaneseHoliday;
import com.akazukin.domain.model.LinkageScenario;
import com.akazukin.domain.model.SeasonalEvent;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.model.SolarTerm;
import com.akazukin.domain.port.CalendarEntryRepository;
import com.akazukin.domain.service.BridgeHolidayDetector;
import com.akazukin.domain.service.JapaneseHolidayProvider;
import com.akazukin.domain.service.LinkageScenarioProvider;
import com.akazukin.domain.service.SeasonalEventProvider;
import com.akazukin.domain.service.SolarTermProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class CalendarUseCase {

    private static final Logger LOG = Logger.getLogger(CalendarUseCase.class.getName());
    private static final ZoneId JST = ZoneId.of("Asia/Tokyo");

    private final CalendarEntryRepository calendarEntryRepository;
    private final JapaneseHolidayProvider holidayProvider;
    private final SolarTermProvider solarTermProvider;
    private final SeasonalEventProvider seasonalEventProvider;
    private final BridgeHolidayDetector bridgeHolidayDetector;
    private final LinkageScenarioProvider linkageScenarioProvider;

    @Inject
    public CalendarUseCase(CalendarEntryRepository calendarEntryRepository) {
        this.calendarEntryRepository = calendarEntryRepository;
        this.holidayProvider = new JapaneseHolidayProvider();
        this.solarTermProvider = new SolarTermProvider();
        this.seasonalEventProvider = new SeasonalEventProvider();
        this.bridgeHolidayDetector = new BridgeHolidayDetector();
        this.linkageScenarioProvider = new LinkageScenarioProvider();
    }

    public List<CalendarEntryDto> getEntries(UUID userId, Instant from, Instant to) {
        if (from == null || to == null) {
            throw new DomainException("INVALID_INPUT", "Date range (from, to) is required");
        }

        return calendarEntryRepository.findByUserIdAndDateRange(userId, from, to).stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional
    public CalendarEntryDto createEntry(UUID userId, CalendarEntryRequestDto request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new DomainException("INVALID_INPUT", "Title is required");
        }
        if (request.scheduledAt() == null) {
            throw new DomainException("INVALID_INPUT", "Scheduled time is required");
        }

        List<SnsPlatform> platforms = parsePlatforms(request.platforms());
        Instant now = Instant.now();

        CalendarEntry entry = new CalendarEntry(
                UUID.randomUUID(),
                userId,
                null,
                request.title(),
                request.description(),
                request.scheduledAt(),
                platforms,
                request.color() != null ? request.color() : "#C8A96E",
                now,
                now
        );

        CalendarEntry saved = calendarEntryRepository.save(entry);
        LOG.log(Level.INFO, "Calendar entry created: {0} for user {1}",
                new Object[]{saved.getId(), userId});

        return toDto(saved);
    }

    @Transactional
    public CalendarEntryDto updateEntry(UUID entryId, UUID userId, CalendarEntryRequestDto request) {
        CalendarEntry entry = calendarEntryRepository.findById(entryId)
                .orElseThrow(() -> new DomainException("ENTRY_NOT_FOUND",
                        "Calendar entry not found: " + entryId));

        if (!entry.getUserId().equals(userId)) {
            throw new DomainException("FORBIDDEN", "You do not own this calendar entry");
        }

        if (request.title() == null || request.title().isBlank()) {
            throw new DomainException("INVALID_INPUT", "Title is required");
        }

        List<SnsPlatform> platforms = parsePlatforms(request.platforms());

        entry.setTitle(request.title());
        entry.setDescription(request.description());
        entry.setScheduledAt(request.scheduledAt());
        entry.setPlatforms(platforms);
        entry.setColor(request.color() != null ? request.color() : entry.getColor());
        entry.setUpdatedAt(Instant.now());

        CalendarEntry saved = calendarEntryRepository.save(entry);
        LOG.log(Level.INFO, "Calendar entry updated: {0}", entryId);

        return toDto(saved);
    }

    @Transactional
    public void deleteEntry(UUID entryId, UUID userId) {
        CalendarEntry entry = calendarEntryRepository.findById(entryId)
                .orElseThrow(() -> new DomainException("ENTRY_NOT_FOUND",
                        "Calendar entry not found: " + entryId));

        if (!entry.getUserId().equals(userId)) {
            throw new DomainException("FORBIDDEN", "You do not own this calendar entry");
        }

        calendarEntryRepository.deleteById(entryId);
        LOG.log(Level.INFO, "Calendar entry deleted: {0}", entryId);
    }

    public List<CalendarTimelineEventDto> getTimelineEvents(int year) {
        List<CalendarTimelineEventDto> events = new ArrayList<>();

        for (JapaneseHoliday h : holidayProvider.getHolidays(year)) {
            events.add(new CalendarTimelineEventDto(
                    h.date(), h.name(), "holiday",
                    h.name() + "。SNS投稿に季節感を取り入れましょう"
            ));
        }

        for (SolarTerm st : solarTermProvider.getSolarTerms(year)) {
            events.add(new CalendarTimelineEventDto(
                    st.date(), st.name(), "solar_term", st.hint()
            ));
        }

        for (SeasonalEvent se : seasonalEventProvider.getEvents(year)) {
            events.add(new CalendarTimelineEventDto(
                    se.date(), se.name(), "seasonal_event", se.hint()
            ));
        }

        events.sort(Comparator.comparing(CalendarTimelineEventDto::date));
        return events;
    }

    public EngagementHeatmapDto getEngagementHeatmap(UUID userId, int year, int month) {
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1);
        Instant from = start.atStartOfDay(JST).toInstant();
        Instant to = end.atStartOfDay(JST).toInstant();

        List<CalendarEntry> entries = calendarEntryRepository.findByUserIdAndDateRange(userId, from, to);

        Map<LocalDate, Integer> levels = new LinkedHashMap<>();
        for (LocalDate d = start; d.isBefore(end); d = d.plusDays(1)) {
            final LocalDate date = d;
            long count = entries.stream()
                    .filter(e -> e.getScheduledAt().atZone(JST).toLocalDate().equals(date))
                    .count();
            int level;
            if (count == 0) level = 0;
            else if (count == 1) level = 1;
            else if (count == 2) level = 2;
            else if (count == 3) level = 3;
            else if (count <= 5) level = 4;
            else level = 5;
            levels.put(date, level);
        }

        return new EngagementHeatmapDto(levels);
    }

    @Transactional
    public List<CalendarEntryDto> generateAiPlan(UUID userId, AiPlanRequestDto request) {
        if (request.theme() == null || request.theme().isBlank()) {
            throw new DomainException("INVALID_INPUT", "Theme is required");
        }
        if (request.platforms() == null || request.platforms().isEmpty()) {
            throw new DomainException("INVALID_INPUT", "At least one platform is required");
        }
        if (request.postCount() < 1 || request.postCount() > 50) {
            throw new DomainException("INVALID_INPUT", "Post count must be between 1 and 50");
        }

        List<SnsPlatform> platforms = parsePlatforms(request.platforms());
        Instant now = Instant.now();
        ZonedDateTime baseDate = now.atZone(JST).plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);

        List<CalendarEntryDto> results = new ArrayList<>();
        String[] snsColors = {"#4A90D9", "#C13584", "#41C9B4", "#FF0000", "#333333", "#e5a323"};

        for (int i = 0; i < request.postCount(); i++) {
            ZonedDateTime scheduled = baseDate.plusDays((long) i * 2);
            SnsPlatform platform = platforms.get(i % platforms.size());
            String color = snsColors[i % snsColors.length];

            String title = request.theme() + " #" + (i + 1) + " [" + platform.name() + "]";
            String description = request.theme() + " - " + platform.name() + " (" + (i + 1) + "/" + request.postCount() + ")";

            CalendarEntry entry = new CalendarEntry(
                    UUID.randomUUID(),
                    userId,
                    null,
                    title,
                    description,
                    scheduled.toInstant(),
                    List.of(platform),
                    color,
                    now,
                    now
            );

            CalendarEntry saved = calendarEntryRepository.save(entry);
            results.add(toDto(saved));
        }

        LOG.log(Level.INFO, "AI plan generated: {0} entries for user {1}",
                new Object[]{results.size(), userId});

        return results;
    }

    public List<BridgeHolidayPlanDto> getBridgeHolidayPlans(int year) {
        List<BridgeHolidayPeriod> periods = bridgeHolidayDetector.detectPeriods(year);
        List<BridgeHolidayPlanDto> plans = new ArrayList<>();

        for (BridgeHolidayPeriod period : periods) {
            List<BridgePhase> phases = bridgeHolidayDetector.generatePhases(period);
            List<BridgeHolidayPlanDto.PhaseDto> phaseDtos = phases.stream()
                    .map(p -> new BridgeHolidayPlanDto.PhaseDto(
                            p.phaseNumber(),
                            p.label(),
                            p.title(),
                            p.entries().stream()
                                    .map(e -> new BridgeHolidayPlanDto.PhaseEntryDto(e.date(), e.title()))
                                    .toList()
                    ))
                    .toList();

            plans.add(new BridgeHolidayPlanDto(
                    period.name(),
                    period.startDate(),
                    period.endDate(),
                    phaseDtos
            ));
        }

        return plans;
    }

    @Transactional
    public List<CalendarEntryDto> applyBridgePlan(UUID userId, String periodName, int year) {
        List<BridgeHolidayPlanDto> plans = getBridgeHolidayPlans(year);
        BridgeHolidayPlanDto targetPlan = plans.stream()
                .filter(p -> p.periodName().equals(periodName))
                .findFirst()
                .orElseThrow(() -> new DomainException("PLAN_NOT_FOUND",
                        "Bridge holiday plan not found: " + periodName));

        Instant now = Instant.now();
        List<CalendarEntryDto> results = new ArrayList<>();

        for (BridgeHolidayPlanDto.PhaseDto phase : targetPlan.phases()) {
            for (BridgeHolidayPlanDto.PhaseEntryDto entry : phase.entries()) {
                ZonedDateTime scheduled = entry.date().atTime(10, 0).atZone(JST);
                CalendarEntry calEntry = new CalendarEntry(
                        UUID.randomUUID(),
                        userId,
                        null,
                        entry.title(),
                        phase.label() + " - " + phase.title(),
                        scheduled.toInstant(),
                        List.of(),
                        "#C8A96E",
                        now,
                        now
                );
                CalendarEntry saved = calendarEntryRepository.save(calEntry);
                results.add(toDto(saved));
            }
        }

        LOG.log(Level.INFO, "Bridge plan applied: {0} entries for period {1}, user {2}",
                new Object[]{results.size(), periodName, userId});

        return results;
    }

    public TimeSlotMatrixDto getTimeSlotMatrix(UUID userId) {
        List<String> platformNames = List.of("X", "Instagram", "note", "YouTube", "TikTok",
                "niconico", "Threads", "LINE", "Pinterest", "Facebook", "LinkedIn", "Bluesky", "Mastodon");
        List<String> dayLabels = List.of("月", "火", "水", "木", "金", "土", "日");
        List<String> hourLabels = List.of("6-9", "9-12", "12-15", "15-18", "18-21", "21-24");

        Map<String, Map<String, Integer>> engagementLevels = new LinkedHashMap<>();

        for (int si = 0; si < platformNames.size(); si++) {
            String sns = platformNames.get(si);
            Map<String, Integer> cellLevels = new LinkedHashMap<>();
            for (int di = 0; di < dayLabels.size(); di++) {
                for (int hi = 0; hi < hourLabels.size(); hi++) {
                    int seed = ((si * 7 + di) * 6 + hi + si * 3 + di * 2) % 11;
                    int level;
                    if (seed <= 1) level = 0;
                    else if (seed <= 3) level = 1;
                    else if (seed <= 5) level = 2;
                    else if (seed <= 7) level = 3;
                    else if (seed <= 9) level = 4;
                    else level = 5;
                    if (sns.equals("X") && (hi == 3 || hi == 4)) level = Math.min(level + 2, 5);
                    if (sns.equals("Instagram") && (hi == 2 || hi == 4)) level = Math.min(level + 1, 5);
                    if (sns.equals("YouTube") && hi == 4) level = Math.min(level + 2, 5);
                    if (sns.equals("TikTok") && hi == 5) level = Math.min(level + 2, 5);
                    cellLevels.put(dayLabels.get(di) + "_" + hourLabels.get(hi), level);
                }
            }
            engagementLevels.put(sns, cellLevels);
        }

        return new TimeSlotMatrixDto(platformNames, dayLabels, hourLabels, engagementLevels);
    }

    @Transactional
    public CalendarEntryDto createEntryFromSlot(UUID userId, String platform, String day, String hour) {
        Map<String, Integer> dayMap = Map.of("月", 1, "火", 2, "水", 3, "木", 4, "金", 5, "土", 6, "日", 7);
        Integer dayOfWeek = dayMap.get(day);
        if (dayOfWeek == null) {
            throw new DomainException("INVALID_INPUT", "Invalid day: " + day);
        }

        String[] parts = hour.split("-");
        int startHour = Integer.parseInt(parts[0]);

        ZonedDateTime now = ZonedDateTime.now(JST);
        ZonedDateTime nextDay = now.plusDays(1);
        while (nextDay.getDayOfWeek().getValue() != dayOfWeek) {
            nextDay = nextDay.plusDays(1);
        }
        ZonedDateTime scheduled = nextDay.withHour(startHour).withMinute(0).withSecond(0).withNano(0);

        List<SnsPlatform> platforms = List.of();
        try {
            platforms = List.of(SnsPlatform.valueOf(platform.toUpperCase()));
        } catch (IllegalArgumentException ignored) {
        }

        Instant nowInstant = Instant.now();
        CalendarEntry entry = new CalendarEntry(
                UUID.randomUUID(),
                userId,
                null,
                platform + " " + day + " " + hour,
                platform + " " + day + "曜日 " + hour + "時投稿予約",
                scheduled.toInstant(),
                platforms,
                "#C8A96E",
                nowInstant,
                nowInstant
        );

        CalendarEntry saved = calendarEntryRepository.save(entry);
        LOG.log(Level.INFO, "Calendar entry from slot created: {0}", saved.getId());
        return toDto(saved);
    }

    public List<LinkageScenarioDto> getLinkageScenarios() {
        return linkageScenarioProvider.getScenarios().stream()
                .map(s -> new LinkageScenarioDto(
                        s.name(),
                        s.steps().stream()
                                .map(step -> new LinkageScenarioDto.StepDto(step.platform(), step.action()))
                                .toList()
                ))
                .toList();
    }

    @Transactional
    public List<CalendarEntryDto> applyScenario(UUID userId, String scenarioName) {
        LinkageScenario scenario = linkageScenarioProvider.getScenarios().stream()
                .filter(s -> s.name().equals(scenarioName))
                .findFirst()
                .orElseThrow(() -> new DomainException("SCENARIO_NOT_FOUND",
                        "Scenario not found: " + scenarioName));

        Instant now = Instant.now();
        ZonedDateTime baseDate = now.atZone(JST).plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);

        List<CalendarEntryDto> results = new ArrayList<>();

        for (int i = 0; i < scenario.steps().size(); i++) {
            LinkageScenario.LinkageStep step = scenario.steps().get(i);
            ZonedDateTime scheduled = baseDate.plusHours((long) i * 3);

            List<SnsPlatform> platforms = List.of();
            try {
                platforms = List.of(SnsPlatform.valueOf(step.platform().toUpperCase().replace(" ", "").replace("(TWITTER)", "")));
            } catch (IllegalArgumentException ignored) {
            }

            CalendarEntry entry = new CalendarEntry(
                    UUID.randomUUID(),
                    userId,
                    null,
                    scenario.name() + " - " + step.platform(),
                    step.action(),
                    scheduled.toInstant(),
                    platforms,
                    "#C8A96E",
                    now,
                    now
            );

            CalendarEntry saved = calendarEntryRepository.save(entry);
            results.add(toDto(saved));
        }

        LOG.log(Level.INFO, "Scenario applied: {0}, {1} entries for user {2}",
                new Object[]{scenarioName, results.size(), userId});

        return results;
    }

    private List<SnsPlatform> parsePlatforms(List<String> platformNames) {
        if (platformNames == null || platformNames.isEmpty()) {
            return List.of();
        }
        return platformNames.stream()
                .map(name -> {
                    try {
                        return SnsPlatform.valueOf(name.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        throw new DomainException("INVALID_PLATFORM",
                                "Unsupported platform: " + name, e);
                    }
                })
                .toList();
    }

    private CalendarEntryDto toDto(CalendarEntry entry) {
        List<String> platformNames = entry.getPlatforms() != null
                ? entry.getPlatforms().stream().map(SnsPlatform::name).toList()
                : List.of();

        return new CalendarEntryDto(
                entry.getId(),
                entry.getPostId(),
                entry.getTitle(),
                entry.getDescription(),
                entry.getScheduledAt(),
                platformNames,
                entry.getColor()
        );
    }
}
