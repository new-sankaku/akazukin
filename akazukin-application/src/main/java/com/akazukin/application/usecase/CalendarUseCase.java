package com.akazukin.application.usecase;

import com.akazukin.application.dto.CalendarEntryDto;
import com.akazukin.application.dto.CalendarEntryRequestDto;
import com.akazukin.domain.exception.DomainException;
import com.akazukin.domain.model.CalendarEntry;
import com.akazukin.domain.model.SnsPlatform;
import com.akazukin.domain.port.CalendarEntryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class CalendarUseCase {

    private static final Logger LOG = Logger.getLogger(CalendarUseCase.class.getName());

    private final CalendarEntryRepository calendarEntryRepository;

    @Inject
    public CalendarUseCase(CalendarEntryRepository calendarEntryRepository) {
        this.calendarEntryRepository = calendarEntryRepository;
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
