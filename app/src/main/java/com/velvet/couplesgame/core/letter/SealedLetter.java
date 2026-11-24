package com.velvet.couplesgame.core.letter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * One sealed love letter stored entirely on the device.
 *
 * <p>The body stays encrypted (see {@link LetterCrypto}) until the reveal date
 * arrives, so a letter written today cannot be peeked at tomorrow even by the
 * writer. Nothing in this class touches the network.
 */
public class SealedLetter {

    /** Where the letter currently sits in its lifecycle. */
    public enum State {
        /** Written and sealed, still waiting for its reveal date. */
        SEALED,
        /** Reveal date has arrived; the letter can be opened now. */
        READY,
        /** Already opened once; the content stays readable. */
        OPENED
    }

    public final String id;
    /** True when partner A wrote it, false for partner B. */
    public final boolean fromPlayerA;
    public final String title;
    /** Plaintext body. Only populated after decryption succeeded. */
    public final String body;
    /** ISO date (yyyy-MM-dd) the letter becomes readable. */
    public final String revealDateIso;
    /** Epoch millis when the letter was sealed. */
    public final long sealedAt;
    /** Epoch millis when the letter was opened; 0 while still sealed. */
    public final long openedAt;
    /** Wax-seal style key, e.g. {@code seal_crimson}. */
    public final String sealId;

    public SealedLetter(String id,
                        boolean fromPlayerA,
                        String title,
                        String body,
                        String revealDateIso,
                        long sealedAt,
                        long openedAt,
                        String sealId) {
        this.id = id != null ? id : "";
        this.fromPlayerA = fromPlayerA;
        this.title = title != null ? title : "";
        this.body = body != null ? body : "";
        this.revealDateIso = revealDateIso != null ? revealDateIso : "";
        this.sealedAt = sealedAt;
        this.openedAt = openedAt;
        this.sealId = sealId != null ? sealId : "seal_crimson";
    }

    /**
     * Lifecycle state evaluated against the given day.
     *
     * <p>A letter never unlocks while the device clock sits before the moment
     * it was sealed, so rolling the system date backwards cannot bypass the
     * reveal delay.
     */
    @NonNull
    public State stateOn(@Nullable LocalDate today) {
        return stateOn(today, System.currentTimeMillis());
    }

    /** Testable overload that takes the current time explicitly. */
    @NonNull
    public State stateOn(@Nullable LocalDate today, long nowMillis) {
        if (openedAt > 0) return State.OPENED;
        if (sealedAt > 0 && nowMillis < sealedAt) return State.SEALED;
        LocalDate reveal = parseDate(revealDateIso);
        if (reveal == null) return State.SEALED;
        LocalDate now = today != null ? today : LocalDate.now();
        return now.isBefore(reveal) ? State.SEALED : State.READY;
    }

    /** Whole days left before the letter can be opened; 0 once it is ready. */
    public int daysUntilReveal(@Nullable LocalDate today) {
        LocalDate reveal = parseDate(revealDateIso);
        if (reveal == null) return 0;
        LocalDate now = today != null ? today : LocalDate.now();
        long days = ChronoUnit.DAYS.between(now, reveal);
        return days < 0 ? 0 : (int) days;
    }

    @Nullable
    public static LocalDate parseDate(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return null;
        try {
            return LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    /** Copy of this letter carrying a decrypted body. */
    public SealedLetter withBody(String decryptedBody, long openedAtMillis) {
        return new SealedLetter(id, fromPlayerA, title, decryptedBody,
                revealDateIso, sealedAt, openedAtMillis, sealId);
    }
}
