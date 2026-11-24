package com.velvet.couplesgame.core.letter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.velvet.couplesgame.ui.letter.LetterRules;

import org.junit.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

/** Unit tests for the sealed-letter reveal rules. */
public class SealedLetterTest {

    private static final long NOW = 1_700_000_000_000L;

    private static SealedLetter letter(String revealIso, long sealedAt, long openedAt) {
        return new SealedLetter("id1", true, "Title", "Body", revealIso, sealedAt, openedAt,
                "seal_crimson");
    }

    @Test
    public void letterStaysSealedBeforeRevealDate() {
        LocalDate today = LocalDate.of(2026, 3, 1);
        SealedLetter l = letter("2026-03-05", NOW, 0L);

        assertEquals(SealedLetter.State.SEALED, l.stateOn(today, NOW + 1000L));
        assertEquals(4, l.daysUntilReveal(today));
    }

    @Test
    public void letterBecomesReadyOnRevealDate() {
        LocalDate today = LocalDate.of(2026, 3, 5);
        SealedLetter l = letter("2026-03-05", NOW, 0L);

        assertEquals(SealedLetter.State.READY, l.stateOn(today, NOW + 1000L));
        assertEquals(0, l.daysUntilReveal(today));
    }

    @Test
    public void openedLetterStaysOpened() {
        LocalDate today = LocalDate.of(2026, 3, 5);
        SealedLetter l = letter("2026-03-05", NOW, NOW + 2000L);

        assertEquals(SealedLetter.State.OPENED, l.stateOn(today, NOW + 3000L));
    }

    @Test
    public void clockRollbackCannotBypassTheRevealDelay() {
        // The letter was sealed "later" than the current clock, so the device
        // date was moved backwards: the letter must stay sealed.
        LocalDate today = LocalDate.of(2026, 3, 10);
        SealedLetter l = letter("2026-03-01", NOW, 0L);

        assertEquals(SealedLetter.State.SEALED, l.stateOn(today, NOW - 5000L));
    }

    @Test
    public void invalidRevealDateKeepsLetterSealed() {
        SealedLetter l = letter("not-a-date", NOW, 0L);

        assertEquals(SealedLetter.State.SEALED, l.stateOn(LocalDate.of(2026, 3, 5), NOW));
        assertEquals(0, l.daysUntilReveal(LocalDate.of(2026, 3, 5)));
    }

    @Test
    public void revealDateIsDerivedFromSealTimePlusDelay() {
        long sealedAt = LocalDate.of(2026, 3, 1)
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

        assertEquals(LocalDate.of(2026, 3, 8),
                LetterRules.revealDateFor(sealedAt, 7));
        assertEquals(LocalDate.of(2026, 3, 2),
                LetterRules.revealDateFor(sealedAt, 1));
    }

    @Test
    public void revealDelayIsAtLeastOneDay() {
        long sealedAt = LocalDate.of(2026, 3, 1)
                .atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();

        assertEquals(LocalDate.of(2026, 3, 2), LetterRules.revealDateFor(sealedAt, 0));
        assertEquals(LocalDate.of(2026, 3, 2), LetterRules.revealDateFor(sealedAt, -5));
    }

    @Test
    public void promptsRotateDeterministically() {
        List<String> prompts = Arrays.asList("one", "two", "three");

        assertEquals("one", LetterRules.nextPrompt(prompts, 0));
        assertEquals("two", LetterRules.nextPrompt(prompts, 1));
        assertEquals("three", LetterRules.nextPrompt(prompts, 2));
        // Wraps around instead of throwing.
        assertEquals("one", LetterRules.nextPrompt(prompts, 3));
        assertEquals("three", LetterRules.nextPrompt(prompts, -1));
    }

    @Test
    public void emptyPromptListReturnsEmptyString() {
        assertEquals("", LetterRules.nextPrompt(null, 0));
        assertEquals("", LetterRules.nextPrompt(Arrays.asList(), 0));
    }

    @Test
    public void sanitizeTrimsAndClamps() {
        assertEquals("hello", LetterRules.sanitize("  hello  ", 10));
        assertEquals("abc", LetterRules.sanitize("abcdef", 3));
        assertEquals("", LetterRules.sanitize(null, 10));
    }

    @Test
    public void dateParsingRejectsGarbage() {
        assertNotNull(SealedLetter.parseDate("2026-03-05"));
        org.junit.Assert.assertNull(SealedLetter.parseDate("05/03/2026"));
        org.junit.Assert.assertNull(SealedLetter.parseDate(""));
        org.junit.Assert.assertNull(SealedLetter.parseDate(null));
    }

    @Test
    public void withBodyProducesReadableCopy() {
        SealedLetter original = letter("2026-03-05", NOW, 0L);
        SealedLetter opened = original.withBody("secret words", NOW + 10L);

        assertEquals("secret words", opened.body);
        assertEquals(SealedLetter.State.OPENED,
                opened.stateOn(LocalDate.of(2026, 3, 5), NOW + 20L));
        // The source letter is untouched: still unopened.
        assertEquals(0L, original.openedAt);
        assertEquals(SealedLetter.State.READY,
                original.stateOn(LocalDate.of(2026, 3, 5), NOW + 20L));
        assertFalse(opened.title.isEmpty());
    }
}
