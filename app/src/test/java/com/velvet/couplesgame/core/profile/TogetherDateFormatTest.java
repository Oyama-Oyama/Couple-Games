package com.velvet.couplesgame.core.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Locale;

/**
 * Rules for the together-since date: parsing, locale-aware display and the
 * UTC round trip the Material date picker relies on.
 */
public class TogetherDateFormatTest {

    private static final LocalDate DAY = LocalDate.of(2024, 5, 20);

    @Test
    public void parsesValidIsoDate() {
        assertEquals(DAY, TogetherDateFormat.parse("2024-05-20"));
    }

    @Test
    public void rejectsMalformedOrEmptyDates() {
        assertNull(TogetherDateFormat.parse(""));
        assertNull(TogetherDateFormat.parse(null));
        assertNull(TogetherDateFormat.parse("not-a-date"));
        assertNull(TogetherDateFormat.parse("2024-13-45"));
    }

    @Test
    public void formatsDateInRequestedLocale() {
        String english = TogetherDateFormat.display("2024-05-20", Locale.US);
        assertNotNull(english);
        assertTrue("expected a year in the formatted date: " + english,
                english.contains("2024"));

        String chinese = TogetherDateFormat.display("2024-05-20", Locale.SIMPLIFIED_CHINESE);
        assertNotNull(chinese);
        assertTrue("expected a year in the formatted date: " + chinese,
                chinese.contains("2024"));
    }

    @Test
    public void displayReturnsNullWhenUnset() {
        assertNull(TogetherDateFormat.display("", Locale.US));
        assertNull(TogetherDateFormat.display("garbage", Locale.US));
    }

    @Test
    public void utcRoundTripKeepsTheSameDay() {
        long millis = TogetherDateFormat.toUtcMillis(DAY);
        assertEquals(DAY, TogetherDateFormat.fromUtcMillis(millis));
    }

    @Test
    public void utcRoundTripHoldsAcrossTheWholeYear() {
        // Walks every day of a leap year so no off-by-one can hide in a
        // month boundary or a leap day.
        LocalDate cursor = LocalDate.of(2024, 1, 1);
        LocalDate end = LocalDate.of(2025, 1, 1);
        while (cursor.isBefore(end)) {
            long millis = TogetherDateFormat.toUtcMillis(cursor);
            assertEquals("round trip failed for " + cursor,
                    cursor, TogetherDateFormat.fromUtcMillis(millis));
            cursor = cursor.plusDays(1);
        }
    }

    @Test
    public void utcMillisAreAnchoredAtUtcMidnight() {
        long millis = TogetherDateFormat.toUtcMillis(DAY);
        assertEquals(DAY, java.time.Instant.ofEpochMilli(millis)
                .atZone(java.time.ZoneOffset.UTC)
                .toLocalDate());
        assertEquals(0L, millis % 86400000L);
    }

    @Test
    public void fromUtcMillisIgnoresNonPositiveValues() {
        assertNull(TogetherDateFormat.fromUtcMillis(0L));
        assertNull(TogetherDateFormat.fromUtcMillis(-1L));
    }

    @Test
    public void toIsoRoundTrips() {
        assertEquals("2024-05-20", TogetherDateFormat.toIso(DAY));
        assertEquals("", TogetherDateFormat.toIso(null));
    }

    @Test
    public void daysTogetherCountsElapsedDays() {
        LocalDate today = LocalDate.of(2024, 5, 22);
        assertEquals(2, TogetherDateFormat.daysTogether("2024-05-20", today));
        assertEquals(0, TogetherDateFormat.daysTogether("2024-05-22", today));
    }

    @Test
    public void daysTogetherRejectsUnsetMalformedAndFutureDates() {
        LocalDate today = LocalDate.of(2024, 5, 22);
        assertEquals(-1, TogetherDateFormat.daysTogether("", today));
        assertEquals(-1, TogetherDateFormat.daysTogether("garbage", today));
        assertEquals(-1, TogetherDateFormat.daysTogether("2024-05-23", today));
    }
}
