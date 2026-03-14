package com.akazukin.infrastructure.persistence.repository;

import com.akazukin.domain.model.CalendarEntry;
import com.akazukin.domain.port.CalendarEntryRepository;
import com.akazukin.infrastructure.persistence.entity.CalendarEntryEntity;
import com.akazukin.infrastructure.persistence.mapper.CalendarEntryMapper;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class CalendarEntryRepositoryImpl implements CalendarEntryRepository, PanacheRepository<CalendarEntryEntity> {

    @Override
    public Optional<CalendarEntry> findById(UUID id) {
        return find("id", id)
                .firstResultOptional()
                .map(CalendarEntryMapper::toDomain);
    }

    @Override
    public List<CalendarEntry> findByUserIdAndDateRange(UUID userId, Instant from, Instant to) {
        return find("userId = ?1 AND scheduledAt >= ?2 AND scheduledAt <= ?3 ORDER BY scheduledAt ASC",
                userId, from, to)
                .list()
                .stream()
                .map(CalendarEntryMapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public CalendarEntry save(CalendarEntry calendarEntry) {
        CalendarEntryEntity entity = CalendarEntryMapper.toEntity(calendarEntry);
        CalendarEntryEntity existing = find("id", entity.id).firstResult();
        if (existing != null) {
            existing.userId = entity.userId;
            existing.postId = entity.postId;
            existing.title = entity.title;
            existing.description = entity.description;
            existing.scheduledAt = entity.scheduledAt;
            existing.platforms = entity.platforms;
            existing.color = entity.color;
            existing.updatedAt = entity.updatedAt;
            persist(existing);
            return CalendarEntryMapper.toDomain(existing);
        }
        persist(entity);
        return CalendarEntryMapper.toDomain(entity);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        delete("id", id);
    }
}
