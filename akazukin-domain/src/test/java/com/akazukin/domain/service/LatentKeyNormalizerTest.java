package com.akazukin.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LatentKeyNormalizerTest {

    private final LatentKeyNormalizer normalizer = new LatentKeyNormalizer();

    @Test
    void removesE1AtPosition9and10() {
        assertEquals("12345678ABCD", normalizer.removeCharAt9and10("12345678E1ABCD"));
    }

    @Test
    void keepsNonE1AtPosition9and10() {
        assertEquals("12345678XXABCD", normalizer.removeCharAt9and10("12345678XXABCD"));
    }

    @Test
    void keepsStringWhenLengthIsLessThan10() {
        assertEquals("123456789", normalizer.removeCharAt9and10("123456789"));
    }

    @Test
    void removesE1WhenStringIsExactly10Characters() {
        assertEquals("12345678", normalizer.removeCharAt9and10("12345678E1"));
    }

    @Test
    void keepsNonE1WhenStringIsExactly10Characters() {
        assertEquals("12345678AB", normalizer.removeCharAt9and10("12345678AB"));
    }

    @Test
    void keepsEmptyString() {
        assertEquals("", normalizer.removeCharAt9and10(""));
    }

    @Test
    void removesOnlyE1AndPreservesRemainder() {
        assertEquals("ABCDEFGH9999", normalizer.removeCharAt9and10("ABCDEFGHE19999"));
    }

    @Test
    void removesE1FromActualLatentKey() {
        assertEquals("1450226601", normalizer.removeCharAt9and10("14502266E101"));
    }

    @ParameterizedTest
    @CsvSource({
        "12345678E1,       12345678",
        "12345678E1XYZ,    12345678XYZ",
        "12345678A1XYZ,    12345678A1XYZ",
        "12345678e1XYZ,    12345678e1XYZ",
        "1234567,          1234567",
        "12345678E,        12345678E",
        "12345678E1E1TAIL, 12345678E1TAIL",
    })
    void parameterizedCases(String input, String expected) {
        assertEquals(expected.trim(), normalizer.removeCharAt9and10(input.trim()));
    }
}
