package com.velvet.couplesgame.ui.letter;

import com.velvet.couplesgame.core.letter.SealedLetter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Pure-Java rules behind the Sealed Letter mode.
 *
 * <p>Kept free of Android dependencies so the reveal-date arithmetic and the
 * prompt rotation can be unit-tested directly.
 */
public final class LetterRules {

    /** Maximum characters accepted in a letter body. */
    public static final int MAX_BODY = 800;
    /** Maximum characters accepted in a letter title. */
    public static final int MAX_TITLE = 40;

    private LetterRules() {}

    /** The day a letter sealed on {@code sealedAt} becomes readable. */
    public static LocalDate revealDateFor(long sealedAt, int delayDays) {
        int days = Math.max(1, delayDays);
        return java.time.Instant.ofEpochMilli(sealedAt)
                .atZone(java.time.ZoneOffset.UTC)
                .toLocalDate()
                .plusDays(days);
    }

    /** Whole days left before the letter may be opened; 0 once it is ready. */
    public static int daysUntilReveal(SealedLetter letter, LocalDate today) {
        if (letter == null) return 0;
        LocalDate reveal = SealedLetter.parseDate(letter.revealDateIso);
        if (reveal == null) return 0;
        LocalDate now = today != null ? today : LocalDate.now();
        long days = java.time.temporal.ChronoUnit.DAYS.between(now, reveal);
        return days < 0 ? 0 : (int) days;
    }

    /**
     * Pick the writing prompt to offer next.
     *
     * <p>Rotates deterministically through the prompt list so both partners see
     * every prompt across repeated sessions without any randomness.
     */
    public static String nextPrompt(java.util.List<String> prompts, int index) {
        if (prompts == null || prompts.isEmpty()) return "";
        int safe = Math.floorMod(index, prompts.size());
        return prompts.get(safe);
    }

    /** Trim and clamp user input. */
    public static String sanitize(String value, int max) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }

    /** Format the reveal date for display. */
    public static String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }
}
