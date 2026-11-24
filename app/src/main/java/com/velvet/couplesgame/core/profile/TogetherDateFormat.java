package com.velvet.couplesgame.core.profile;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Pure helpers for turning the stored "together since" date into something the
 * UI can show, and for moving between ISO days and the UTC millisecond values
 * the Material date picker works with.
 *
 * <p>No Android dependency, so every rule here is unit-testable.
 */
public final class TogetherDateFormat {

    private TogetherDateFormat() {}

    /**
     * Parses a stored ISO day ("yyyy-MM-dd").
     *
     * @return the parsed day, or {@code null} when the value is empty or malformed
     */
    public static LocalDate parse(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return null;
        try {
            return LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Formats the stored day for display using the locale-aware medium style,
     * e.g. "May 20, 2024" / "2024年5月20日".
     *
     * @return the formatted date, or {@code null} when the value cannot be parsed
     */
    public static String display(String isoDate, Locale locale) {
        LocalDate date = parse(isoDate);
        if (date == null) return null;
        Locale target = locale != null ? locale : Locale.getDefault();
        return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(target));
    }

    /**
     * Converts a local day into the UTC millisecond value the Material date
     * picker expects. The picker works exclusively in UTC, so the day has to be
     * anchored at UTC midnight to avoid an off-by-one-day shift.
     */
    public static long toUtcMillis(LocalDate date) {
        if (date == null) return 0L;
        return date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

    /**
     * Converts a UTC millisecond value from the date picker back into a local
     * day, undoing the UTC anchoring done by {@link #toUtcMillis(LocalDate)}.
     */
    public static LocalDate fromUtcMillis(long utcMillis) {
        if (utcMillis <= 0L) return null;
        return Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate();
    }

    /** Serialises a day back into the stored ISO form. */
    public static String toIso(LocalDate date) {
        if (date == null) return "";
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /**
     * Whole days between the stored day and today, or {@code -1} when the value
     * is unset, malformed or in the future.
     */
    public static int daysTogether(String isoDate) {
        return daysTogether(isoDate, LocalDate.now());
    }

    /** Testable overload that takes "today" explicitly. */
    public static int daysTogether(String isoDate, LocalDate today) {
        LocalDate start = parse(isoDate);
        if (start == null || today == null) return -1;
        long days = ChronoUnit.DAYS.between(start, today);
        return days < 0 ? -1 : (int) days;
    }
}
