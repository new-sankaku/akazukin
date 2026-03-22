package com.akazukin.domain.service;

import com.akazukin.domain.model.SolarTerm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SolarTermProviderTest {

    private SolarTermProvider provider;

    @BeforeEach
    void setUp() {
        provider = new SolarTermProvider();
    }

    @Test
    void getSolarTerms_returns24Terms() {
        List<SolarTerm> terms = provider.getSolarTerms(2026);

        assertEquals(24, terms.size());
    }

    @Test
    void getSolarTerms_containsShoukan() {
        List<SolarTerm> terms = provider.getSolarTerms(2026);

        boolean found = terms.stream()
                .anyMatch(t -> t.name().equals("小寒") && t.date().equals(LocalDate.of(2026, 1, 5)));
        assertTrue(found);
    }

    @Test
    void getSolarTerms_containsDaikan() {
        List<SolarTerm> terms = provider.getSolarTerms(2026);

        boolean found = terms.stream()
                .anyMatch(t -> t.name().equals("大寒") && t.date().equals(LocalDate.of(2026, 1, 20)));
        assertTrue(found);
    }

    @Test
    void getSolarTerms_containsRisshun() {
        List<SolarTerm> terms = provider.getSolarTerms(2026);

        boolean found = terms.stream()
                .anyMatch(t -> t.name().equals("立春") && t.date().equals(LocalDate.of(2026, 2, 4)));
        assertTrue(found);
    }

    @Test
    void getSolarTerms_containsShunbun() {
        List<SolarTerm> terms = provider.getSolarTerms(2026);

        boolean found = terms.stream()
                .anyMatch(t -> t.name().equals("春分") && t.date().equals(LocalDate.of(2026, 3, 20)));
        assertTrue(found);
    }

    @Test
    void getSolarTerms_containsGeshi() {
        List<SolarTerm> terms = provider.getSolarTerms(2026);

        boolean found = terms.stream()
                .anyMatch(t -> t.name().equals("夏至") && t.date().equals(LocalDate.of(2026, 6, 21)));
        assertTrue(found);
    }

    @Test
    void getSolarTerms_containsShuubun() {
        List<SolarTerm> terms = provider.getSolarTerms(2026);

        boolean found = terms.stream()
                .anyMatch(t -> t.name().equals("秋分") && t.date().equals(LocalDate.of(2026, 9, 22)));
        assertTrue(found);
    }

    @Test
    void getSolarTerms_containsTouji() {
        List<SolarTerm> terms = provider.getSolarTerms(2026);

        boolean found = terms.stream()
                .anyMatch(t -> t.name().equals("冬至") && t.date().equals(LocalDate.of(2026, 12, 22)));
        assertTrue(found);
    }

    @Test
    void getSolarTerms_allTermsHaveNameAndEnglishName() {
        List<SolarTerm> terms = provider.getSolarTerms(2026);

        for (SolarTerm term : terms) {
            assertNotNull(term.name());
            assertNotNull(term.nameEn());
            assertFalse(term.name().isBlank());
            assertFalse(term.nameEn().isBlank());
        }
    }

    @Test
    void getSolarTerms_allTermsHaveHint() {
        List<SolarTerm> terms = provider.getSolarTerms(2026);

        for (SolarTerm term : terms) {
            assertNotNull(term.hint());
            assertNotNull(term.hintEn());
            assertFalse(term.hint().isBlank());
            assertFalse(term.hintEn().isBlank());
        }
    }

    @Test
    void getSolarTerms_allTermsAreInRequestedYear() {
        List<SolarTerm> terms = provider.getSolarTerms(2026);

        for (SolarTerm term : terms) {
            assertEquals(2026, term.date().getYear());
        }
    }

    @Test
    void getSolarTerms_termsAreOrderedChronologically() {
        List<SolarTerm> terms = provider.getSolarTerms(2026);

        for (int i = 1; i < terms.size(); i++) {
            assertFalse(terms.get(i).date().isBefore(terms.get(i - 1).date()));
        }
    }

    @Test
    void getSolarTerms_differentYearReturnsDifferentDates() {
        List<SolarTerm> terms2026 = provider.getSolarTerms(2026);
        List<SolarTerm> terms2027 = provider.getSolarTerms(2027);

        assertEquals(terms2026.size(), terms2027.size());
        assertFalse(terms2026.get(0).date().equals(terms2027.get(0).date()));
    }

    @Test
    void getSolarTerms_firstTermIsInJanuary() {
        List<SolarTerm> terms = provider.getSolarTerms(2026);

        assertEquals(1, terms.get(0).date().getMonthValue());
    }

    @Test
    void getSolarTerms_lastTermIsInDecember() {
        List<SolarTerm> terms = provider.getSolarTerms(2026);

        assertEquals(12, terms.get(terms.size() - 1).date().getMonthValue());
    }
}
