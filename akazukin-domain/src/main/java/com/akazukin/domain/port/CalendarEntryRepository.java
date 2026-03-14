package com.akazukin.domain.port;

import com.akazukin.domain.model.CalendarEntry;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CalendarEntryRepository {

    Optional<CalendarEntry> findById(UUID id);

    List<CalendarEntry> findByUserIdAndDateRange(UUID userId, Instant from, Instant to);

    CalendarEntry save(CalendarEntry calendarEntry);

    void deleteById(UUID id);
}
