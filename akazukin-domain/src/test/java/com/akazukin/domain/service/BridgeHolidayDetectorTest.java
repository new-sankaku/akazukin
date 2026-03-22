package com.akazukin.domain.service;

import com.akazukin.domain.model.BridgeHolidayPeriod;
import com.akazukin.domain.model.BridgePhase;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BridgeHolidayDetectorTest {

    private BridgeHolidayDetector detector;

    @BeforeEach
    void setUp() {
        detector = new BridgeHolidayDetector();
    }

    @Test
    void detectPeriods_returnsThreePeriods() {
        List<BridgeHolidayPeriod> periods = detector.detectPeriods(2026);

        assertEquals(3, periods.size());
    }

    @Test
    void detectPeriods_containsGoldenWeek() {
        List<BridgeHolidayPeriod> periods = detector.detectPeriods(2026);

        boolean found = periods.stream().anyMatch(p -> p.name().equals("GW"));
        assertTrue(found);
    }

    @Test
    void detectPeriods_containsObon() {
        List<BridgeHolidayPeriod> periods = detector.detectPeriods(2026);

        boolean found = periods.stream().anyMatch(p -> p.name().equals("お盆"));
        assertTrue(found);
    }

    @Test
    void detectPeriods_containsNewYear() {
        List<BridgeHolidayPeriod> periods = detector.detectPeriods(2026);

        boolean found = periods.stream().anyMatch(p -> p.name().equals("年末年始"));
        assertTrue(found);
    }

    @Test
    void detectPeriods_goldenWeekStartsOnApril29() {
        List<BridgeHolidayPeriod> periods = detector.detectPeriods(2026);

        BridgeHolidayPeriod gw = periods.stream()
                .filter(p -> p.name().equals("GW"))
                .findFirst()
                .orElseThrow();

        assertEquals(LocalDate.of(2026, 4, 29), gw.startDate());
    }

    @Test
    void detectPeriods_obonStartsOnAugust13() {
        List<BridgeHolidayPeriod> periods = detector.detectPeriods(2026);

        BridgeHolidayPeriod obon = periods.stream()
                .filter(p -> p.name().equals("お盆"))
                .findFirst()
                .orElseThrow();

        assertEquals(LocalDate.of(2026, 8, 13), obon.startDate());
        assertEquals(LocalDate.of(2026, 8, 16), obon.endDate());
    }

    @Test
    void detectPeriods_newYearSpansYearBoundary() {
        List<BridgeHolidayPeriod> periods = detector.detectPeriods(2026);

        BridgeHolidayPeriod newYear = periods.stream()
                .filter(p -> p.name().equals("年末年始"))
                .findFirst()
                .orElseThrow();

        assertEquals(LocalDate.of(2026, 12, 28), newYear.startDate());
        assertEquals(LocalDate.of(2027, 1, 3), newYear.endDate());
    }

    @Test
    void detectPeriods_allPeriodsHaveNameAndEnglishName() {
        List<BridgeHolidayPeriod> periods = detector.detectPeriods(2026);

        for (BridgeHolidayPeriod period : periods) {
            assertNotNull(period.name());
            assertNotNull(period.nameEn());
            assertFalse(period.name().isBlank());
            assertFalse(period.nameEn().isBlank());
        }
    }

    @Test
    void detectPeriods_allPeriodsHaveStartBeforeEnd() {
        List<BridgeHolidayPeriod> periods = detector.detectPeriods(2026);

        for (BridgeHolidayPeriod period : periods) {
            assertTrue(period.startDate().isBefore(period.endDate()));
        }
    }

    @Test
    void generatePhases_returnsThreePhases() {
        BridgeHolidayPeriod gw = new BridgeHolidayPeriod("GW", "Golden Week",
                LocalDate.of(2026, 4, 29), LocalDate.of(2026, 5, 6));

        List<BridgePhase> phases = detector.generatePhases(gw);

        assertEquals(3, phases.size());
    }

    @Test
    void generatePhases_phaseNumbersAreSequential() {
        BridgeHolidayPeriod gw = new BridgeHolidayPeriod("GW", "Golden Week",
                LocalDate.of(2026, 4, 29), LocalDate.of(2026, 5, 6));

        List<BridgePhase> phases = detector.generatePhases(gw);

        assertEquals(1, phases.get(0).phaseNumber());
        assertEquals(2, phases.get(1).phaseNumber());
        assertEquals(3, phases.get(2).phaseNumber());
    }

    @Test
    void generatePhases_eachPhaseHasEntries() {
        BridgeHolidayPeriod gw = new BridgeHolidayPeriod("GW", "Golden Week",
                LocalDate.of(2026, 4, 29), LocalDate.of(2026, 5, 6));

        List<BridgePhase> phases = detector.generatePhases(gw);

        for (BridgePhase phase : phases) {
            assertFalse(phase.entries().isEmpty());
        }
    }

    @Test
    void generatePhases_eachPhaseHasThreeEntries() {
        BridgeHolidayPeriod gw = new BridgeHolidayPeriod("GW", "Golden Week",
                LocalDate.of(2026, 4, 29), LocalDate.of(2026, 5, 6));

        List<BridgePhase> phases = detector.generatePhases(gw);

        for (BridgePhase phase : phases) {
            assertEquals(3, phase.entries().size());
        }
    }

    @Test
    void generatePhases_allPhasesHaveLabelsAndTitles() {
        BridgeHolidayPeriod obon = new BridgeHolidayPeriod("お盆", "Obon",
                LocalDate.of(2026, 8, 13), LocalDate.of(2026, 8, 16));

        List<BridgePhase> phases = detector.generatePhases(obon);

        for (BridgePhase phase : phases) {
            assertNotNull(phase.label());
            assertNotNull(phase.labelEn());
            assertNotNull(phase.title());
            assertNotNull(phase.titleEn());
            assertFalse(phase.label().isBlank());
        }
    }

    @Test
    void generatePhases_entriesHaveDatesAndTitles() {
        BridgeHolidayPeriod gw = new BridgeHolidayPeriod("GW", "Golden Week",
                LocalDate.of(2026, 4, 29), LocalDate.of(2026, 5, 6));

        List<BridgePhase> phases = detector.generatePhases(gw);

        for (BridgePhase phase : phases) {
            for (BridgePhase.BridgePhaseEntry entry : phase.entries()) {
                assertNotNull(entry.date());
                assertNotNull(entry.title());
                assertNotNull(entry.titleEn());
                assertFalse(entry.title().isBlank());
            }
        }
    }

    @Test
    void generatePhases_phase1EntriesAreBeforePeriodStart() {
        BridgeHolidayPeriod gw = new BridgeHolidayPeriod("GW", "Golden Week",
                LocalDate.of(2026, 4, 29), LocalDate.of(2026, 5, 6));

        List<BridgePhase> phases = detector.generatePhases(gw);
        BridgePhase phase1 = phases.get(0);

        for (BridgePhase.BridgePhaseEntry entry : phase1.entries()) {
            assertTrue(entry.date().isBefore(gw.startDate()));
        }
    }

    @Test
    void generatePhases_phase3EntriesAreAfterPeriodEnd() {
        BridgeHolidayPeriod gw = new BridgeHolidayPeriod("GW", "Golden Week",
                LocalDate.of(2026, 4, 29), LocalDate.of(2026, 5, 6));

        List<BridgePhase> phases = detector.generatePhases(gw);
        BridgePhase phase3 = phases.get(2);

        for (BridgePhase.BridgePhaseEntry entry : phase3.entries()) {
            assertTrue(entry.date().isAfter(gw.endDate()));
        }
    }
}
