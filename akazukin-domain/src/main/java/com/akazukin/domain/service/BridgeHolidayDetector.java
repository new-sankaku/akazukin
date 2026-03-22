package com.akazukin.domain.service;

import com.akazukin.domain.model.BridgeHolidayPeriod;
import com.akazukin.domain.model.BridgePhase;
import com.akazukin.domain.model.JapaneseHoliday;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BridgeHolidayDetector {

    private static final DateTimeFormatter JP_DATE_FMT = DateTimeFormatter.ofPattern("M/d（E）", Locale.JAPAN);

    public List<BridgeHolidayPeriod> detectPeriods(int year) {
        JapaneseHolidayProvider holidayProvider = new JapaneseHolidayProvider();
        List<JapaneseHoliday> holidays = holidayProvider.getHolidays(year);

        List<BridgeHolidayPeriod> periods = new ArrayList<>();

        LocalDate gwStart = LocalDate.of(year, 4, 29);
        LocalDate gwEnd = LocalDate.of(year, 5, 6);
        if (gwEnd.getDayOfWeek() == DayOfWeek.SATURDAY || gwEnd.getDayOfWeek() == DayOfWeek.SUNDAY) {
            gwEnd = gwEnd.plusDays(1);
            while (gwEnd.getDayOfWeek() == DayOfWeek.SATURDAY || gwEnd.getDayOfWeek() == DayOfWeek.SUNDAY) {
                gwEnd = gwEnd.plusDays(1);
            }
        }
        periods.add(new BridgeHolidayPeriod("GW", "Golden Week", gwStart, gwEnd));

        periods.add(new BridgeHolidayPeriod("お盆", "Obon", LocalDate.of(year, 8, 13), LocalDate.of(year, 8, 16)));

        periods.add(new BridgeHolidayPeriod("年末年始", "New Year",
                LocalDate.of(year, 12, 28), LocalDate.of(year + 1, 1, 3)));

        return periods;
    }

    public List<BridgePhase> generatePhases(BridgeHolidayPeriod period) {
        List<BridgePhase> phases = new ArrayList<>();

        LocalDate preStart = period.startDate().minusDays(4);
        List<BridgePhase.BridgePhaseEntry> preEntries = new ArrayList<>();
        LocalDate cursor = preStart;
        for (int i = 0; i < 3; i++) {
            while (cursor.getDayOfWeek() == DayOfWeek.SUNDAY) {
                cursor = cursor.plusDays(1);
            }
            String title;
            String titleEn;
            switch (i) {
                case 0 -> {
                    title = period.name() + "期間のお知らせ";
                    titleEn = period.nameEn() + " period announcement";
                }
                case 1 -> {
                    title = "連休中の対応について";
                    titleEn = "About operations during holidays";
                }
                default -> {
                    title = period.name() + "直前リマインド";
                    titleEn = period.nameEn() + " last reminder";
                }
            }
            preEntries.add(new BridgePhase.BridgePhaseEntry(cursor, title, titleEn));
            cursor = cursor.plusDays(1);
        }
        phases.add(new BridgePhase(1, "PHASE 1 - 告知", "PHASE 1 - Announcement",
                "連休前の事前告知", "Pre-holiday announcements", preEntries));

        LocalDate midStart = period.startDate().plusDays(1);
        List<BridgePhase.BridgePhaseEntry> midEntries = new ArrayList<>();
        cursor = midStart;
        String[] midTitles = {
                period.name() + "の過ごし方提案",
                "フォロワー参加企画",
                "連休折り返し投稿"
        };
        String[] midTitlesEn = {
                period.nameEn() + " activity suggestions",
                "Follower participation campaign",
                "Mid-holiday post"
        };
        for (int i = 0; i < 3; i++) {
            midEntries.add(new BridgePhase.BridgePhaseEntry(cursor, midTitles[i], midTitlesEn[i]));
            cursor = cursor.plusDays(2);
        }
        phases.add(new BridgePhase(2, "PHASE 2 - 連休中", "PHASE 2 - During Holiday",
                "連休中の軽量投稿", "Lightweight posts during holidays", midEntries));

        LocalDate postStart = period.endDate().plusDays(1);
        List<BridgePhase.BridgePhaseEntry> postEntries = new ArrayList<>();
        cursor = postStart;
        while (cursor.getDayOfWeek() == DayOfWeek.SATURDAY || cursor.getDayOfWeek() == DayOfWeek.SUNDAY) {
            cursor = cursor.plusDays(1);
        }
        String[] postTitles = {
                "営業再開のご挨拶",
                period.name() + "振り返り+今後の予定",
                "新企画のティーザー"
        };
        String[] postTitlesEn = {
                "Business resumption greeting",
                period.nameEn() + " review + upcoming plans",
                "New project teaser"
        };
        for (int i = 0; i < 3; i++) {
            postEntries.add(new BridgePhase.BridgePhaseEntry(cursor, postTitles[i], postTitlesEn[i]));
            cursor = cursor.plusDays(1);
            while (cursor.getDayOfWeek() == DayOfWeek.SATURDAY || cursor.getDayOfWeek() == DayOfWeek.SUNDAY) {
                cursor = cursor.plusDays(1);
            }
        }
        phases.add(new BridgePhase(3, "PHASE 3 - 再開挨拶", "PHASE 3 - Resumption",
                "連休明けの再開", "Post-holiday resumption", postEntries));

        return phases;
    }
}
