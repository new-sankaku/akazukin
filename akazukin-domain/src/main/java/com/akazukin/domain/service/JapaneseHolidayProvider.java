package com.akazukin.domain.service;

import com.akazukin.domain.model.JapaneseHoliday;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JapaneseHolidayProvider {

    public List<JapaneseHoliday> getHolidays(int year) {
        List<JapaneseHoliday> holidays = new ArrayList<>();

        holidays.add(new JapaneseHoliday(LocalDate.of(year, 1, 1), "元日", "New Year's Day"));
        holidays.add(new JapaneseHoliday(secondMonday(year, 1), "成人の日", "Coming of Age Day"));
        holidays.add(new JapaneseHoliday(LocalDate.of(year, 2, 11), "建国記念の日", "National Foundation Day"));
        holidays.add(new JapaneseHoliday(LocalDate.of(year, 2, 23), "天皇誕生日", "Emperor's Birthday"));
        holidays.add(new JapaneseHoliday(vernalEquinox(year), "春分の日", "Vernal Equinox Day"));
        holidays.add(new JapaneseHoliday(LocalDate.of(year, 4, 29), "昭和の日", "Showa Day"));
        holidays.add(new JapaneseHoliday(LocalDate.of(year, 5, 3), "憲法記念日", "Constitution Memorial Day"));
        holidays.add(new JapaneseHoliday(LocalDate.of(year, 5, 4), "みどりの日", "Greenery Day"));
        holidays.add(new JapaneseHoliday(LocalDate.of(year, 5, 5), "こどもの日", "Children's Day"));
        holidays.add(new JapaneseHoliday(thirdMonday(year, 7), "海の日", "Marine Day"));
        holidays.add(new JapaneseHoliday(LocalDate.of(year, 8, 11), "山の日", "Mountain Day"));
        holidays.add(new JapaneseHoliday(thirdMonday(year, 9), "敬老の日", "Respect for the Aged Day"));
        holidays.add(new JapaneseHoliday(autumnalEquinox(year), "秋分の日", "Autumnal Equinox Day"));
        holidays.add(new JapaneseHoliday(secondMonday(year, 10), "スポーツの日", "Sports Day"));
        holidays.add(new JapaneseHoliday(LocalDate.of(year, 11, 3), "文化の日", "Culture Day"));
        holidays.add(new JapaneseHoliday(LocalDate.of(year, 11, 23), "勤労感謝の日", "Labor Thanksgiving Day"));

        // Add substitute holidays (振替休日): if a holiday falls on Sunday, the next Monday is a holiday
        List<JapaneseHoliday> substituteHolidays = new ArrayList<>();
        for (JapaneseHoliday holiday : holidays) {
            if (holiday.date().getDayOfWeek() == DayOfWeek.SUNDAY) {
                LocalDate substitute = holiday.date().plusDays(1);
                // Ensure substitute doesn't conflict with an existing holiday
                while (containsDate(holidays, substitute) || containsDate(substituteHolidays, substitute)) {
                    substitute = substitute.plusDays(1);
                }
                substituteHolidays.add(new JapaneseHoliday(substitute, "振替休日", "Substitute Holiday"));
            }
        }
        holidays.addAll(substituteHolidays);

        return holidays;
    }

    public boolean isHoliday(LocalDate date) {
        return getHolidays(date.getYear()).stream()
                .anyMatch(h -> h.date().equals(date));
    }

    private boolean containsDate(List<JapaneseHoliday> holidays, LocalDate date) {
        return holidays.stream().anyMatch(h -> h.date().equals(date));
    }

    private LocalDate secondMonday(int year, int month) {
        return nthDayOfWeek(year, month, DayOfWeek.MONDAY, 2);
    }

    private LocalDate thirdMonday(int year, int month) {
        return nthDayOfWeek(year, month, DayOfWeek.MONDAY, 3);
    }

    private LocalDate nthDayOfWeek(int year, int month, DayOfWeek dayOfWeek, int n) {
        LocalDate first = LocalDate.of(year, month, 1);
        int daysUntil = (dayOfWeek.getValue() - first.getDayOfWeek().getValue() + 7) % 7;
        return first.plusDays(daysUntil + (long) (n - 1) * 7);
    }

    private LocalDate vernalEquinox(int year) {
        // Approximate vernal equinox calculation for years 2000-2099
        int day = (int) (20.8431 + 0.242194 * (year - 1980) - (int) ((year - 1980) / 4.0));
        return LocalDate.of(year, 3, day);
    }

    private LocalDate autumnalEquinox(int year) {
        // Approximate autumnal equinox calculation for years 2000-2099
        int day = (int) (23.2488 + 0.242194 * (year - 1980) - (int) ((year - 1980) / 4.0));
        return LocalDate.of(year, 9, day);
    }
}
