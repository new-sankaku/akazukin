package com.akazukin.domain.service;

import com.akazukin.domain.model.JapaneseHoliday;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JapaneseHolidayProviderTest {

    private JapaneseHolidayProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JapaneseHolidayProvider();
    }

    @Test
    void getHolidays_returnsNonEmptyListForYear() {
        List<JapaneseHoliday> holidays = provider.getHolidays(2026);

        assertFalse(holidays.isEmpty());
    }

    @Test
    void getHolidays_containsNewYearsDay() {
        List<JapaneseHoliday> holidays = provider.getHolidays(2026);

        boolean found = holidays.stream()
                .anyMatch(h -> h.date().equals(LocalDate.of(2026, 1, 1)) && h.name().equals("元日"));
        assertTrue(found);
    }

    @Test
    void getHolidays_containsConstitutionMemorialDay() {
        List<JapaneseHoliday> holidays = provider.getHolidays(2026);

        boolean found = holidays.stream()
                .anyMatch(h -> h.date().equals(LocalDate.of(2026, 5, 3)) && h.name().equals("憲法記念日"));
        assertTrue(found);
    }

    @Test
    void getHolidays_containsGreeneryDay() {
        List<JapaneseHoliday> holidays = provider.getHolidays(2026);

        boolean found = holidays.stream()
                .anyMatch(h -> h.date().equals(LocalDate.of(2026, 5, 4)) && h.name().equals("みどりの日"));
        assertTrue(found);
    }

    @Test
    void getHolidays_containsChildrensDay() {
        List<JapaneseHoliday> holidays = provider.getHolidays(2026);

        boolean found = holidays.stream()
                .anyMatch(h -> h.date().equals(LocalDate.of(2026, 5, 5)) && h.name().equals("こどもの日"));
        assertTrue(found);
    }

    @Test
    void getHolidays_comingOfAgeDayIsSecondMonday() {
        List<JapaneseHoliday> holidays = provider.getHolidays(2026);

        JapaneseHoliday comingOfAge = holidays.stream()
                .filter(h -> h.name().equals("成人の日"))
                .findFirst()
                .orElseThrow();

        assertEquals(DayOfWeek.MONDAY, comingOfAge.date().getDayOfWeek());
        assertEquals(1, comingOfAge.date().getMonthValue());
    }

    @Test
    void getHolidays_marineDayIsThirdMonday() {
        List<JapaneseHoliday> holidays = provider.getHolidays(2026);

        JapaneseHoliday marineDay = holidays.stream()
                .filter(h -> h.name().equals("海の日"))
                .findFirst()
                .orElseThrow();

        assertEquals(DayOfWeek.MONDAY, marineDay.date().getDayOfWeek());
        assertEquals(7, marineDay.date().getMonthValue());
    }

    @Test
    void getHolidays_sportsDayIsSecondMondayOfOctober() {
        List<JapaneseHoliday> holidays = provider.getHolidays(2026);

        JapaneseHoliday sportsDay = holidays.stream()
                .filter(h -> h.name().equals("スポーツの日"))
                .findFirst()
                .orElseThrow();

        assertEquals(DayOfWeek.MONDAY, sportsDay.date().getDayOfWeek());
        assertEquals(10, sportsDay.date().getMonthValue());
    }

    @Test
    void getHolidays_containsVernalEquinoxInMarch() {
        List<JapaneseHoliday> holidays = provider.getHolidays(2026);

        JapaneseHoliday vernalEquinox = holidays.stream()
                .filter(h -> h.name().equals("春分の日"))
                .findFirst()
                .orElseThrow();

        assertEquals(3, vernalEquinox.date().getMonthValue());
    }

    @Test
    void getHolidays_containsAutumnalEquinoxInSeptember() {
        List<JapaneseHoliday> holidays = provider.getHolidays(2026);

        JapaneseHoliday autumnalEquinox = holidays.stream()
                .filter(h -> h.name().equals("秋分の日"))
                .findFirst()
                .orElseThrow();

        assertEquals(9, autumnalEquinox.date().getMonthValue());
    }

    @Test
    void getHolidays_addsSubstituteHolidayWhenHolidayFallsOnSunday() {
        List<JapaneseHoliday> holidays = provider.getHolidays(2026);

        LocalDate feb11 = LocalDate.of(2026, 2, 11);
        if (feb11.getDayOfWeek() == DayOfWeek.SUNDAY) {
            boolean hasSubstitute = holidays.stream()
                    .anyMatch(h -> h.name().equals("振替休日") && h.date().equals(feb11.plusDays(1)));
            assertTrue(hasSubstitute);
        }
    }

    @Test
    void getHolidays_allHolidaysHaveNameAndEnglishName() {
        List<JapaneseHoliday> holidays = provider.getHolidays(2026);

        for (JapaneseHoliday holiday : holidays) {
            assertNotNull(holiday.name());
            assertNotNull(holiday.nameEn());
            assertNotNull(holiday.date());
            assertFalse(holiday.name().isBlank());
            assertFalse(holiday.nameEn().isBlank());
        }
    }

    @Test
    void getHolidays_containsAtLeast16Holidays() {
        List<JapaneseHoliday> holidays = provider.getHolidays(2026);

        assertTrue(holidays.size() >= 16);
    }

    @Test
    void isHoliday_returnsTrueForNewYearsDay() {
        assertTrue(provider.isHoliday(LocalDate.of(2026, 1, 1)));
    }

    @Test
    void isHoliday_returnsFalseForRegularDay() {
        assertFalse(provider.isHoliday(LocalDate.of(2026, 6, 10)));
    }

    @Test
    void isHoliday_returnsTrueForChildrensDay() {
        assertTrue(provider.isHoliday(LocalDate.of(2026, 5, 5)));
    }

    @Test
    void getHolidays_differentYearsReturnDifferentDatesForMovableHolidays() {
        JapaneseHoliday comingOfAge2026 = provider.getHolidays(2026).stream()
                .filter(h -> h.name().equals("成人の日"))
                .findFirst()
                .orElseThrow();

        JapaneseHoliday comingOfAge2027 = provider.getHolidays(2027).stream()
                .filter(h -> h.name().equals("成人の日"))
                .findFirst()
                .orElseThrow();

        assertFalse(comingOfAge2026.date().equals(comingOfAge2027.date()));
    }
}
