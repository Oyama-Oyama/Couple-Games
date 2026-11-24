package com.velvet.couplesgame.core.profile;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.velvet.couplesgame.core.engine.TaskEngine;
import com.velvet.couplesgame.core.model.PlayerProfile;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Offline store for couple names, inclusive pronouns (He/She/They), optional Safe Word,
 * and first-launch onboarding completion state.
 */
public class CoupleProfileManager {
    public static class CoupleState {
        public final PlayerProfile partnerA;
        public final PlayerProfile partnerB;
        public final String safeWord;
        public final boolean onboardingCompleted;
        /** "yyyy-MM-dd" when the couple got together; empty when unset. */
        public final String togetherSince;

        public CoupleState(PlayerProfile partnerA, PlayerProfile partnerB, String safeWord,
                           boolean onboardingCompleted, String togetherSince) {
            this.partnerA = partnerA;
            this.partnerB = partnerB;
            this.safeWord = safeWord;
            this.onboardingCompleted = onboardingCompleted;
            this.togetherSince = togetherSince != null ? togetherSince : "";
        }

        /** Backward-compatible constructor used by older callers / tests. */
        public CoupleState(PlayerProfile partnerA, PlayerProfile partnerB, String safeWord,
                           boolean onboardingCompleted) {
            this(partnerA, partnerB, safeWord, onboardingCompleted, "");
        }
    }

    private static final String PREF_NAME = "velvet_couple_profile";
    private static final String KEY_NAME_A = "name_a";
    private static final String KEY_PRONOUN_A = "pronoun_a";
    private static final String KEY_NAME_B = "name_b";
    private static final String KEY_PRONOUN_B = "pronoun_b";
    private static final String KEY_SAFE_WORD = "safe_word";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final String KEY_TOGETHER_SINCE = "together_since";

    public static final String DEFAULT_NAME_A = "Alex";
    public static final String DEFAULT_NAME_B = "Maya";
    public static final String DEFAULT_SAFE_WORD = "Velvet";

    private final SharedPreferences prefs;
    private final MutableLiveData<CoupleState> stateLiveData = new MutableLiveData<>();

    public CoupleProfileManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.stateLiveData.setValue(readCurrentState());
    }

    private CoupleState readCurrentState() {
        PlayerProfile a = new PlayerProfile(
                prefs.getString(KEY_NAME_A, DEFAULT_NAME_A),
                prefs.getString(KEY_PRONOUN_A, "He"),
                TaskEngine.DEFAULT_SKIP_QUOTA
        );
        PlayerProfile b = new PlayerProfile(
                prefs.getString(KEY_NAME_B, DEFAULT_NAME_B),
                prefs.getString(KEY_PRONOUN_B, "She"),
                TaskEngine.DEFAULT_SKIP_QUOTA
        );
        String safe = prefs.getString(KEY_SAFE_WORD, DEFAULT_SAFE_WORD);
        boolean done = prefs.getBoolean(KEY_ONBOARDING_DONE, false);
        String togetherSince = prefs.getString(KEY_TOGETHER_SINCE, "");
        return new CoupleState(a, b, safe, done, togetherSince);
    }

    public CoupleState getState() {
        CoupleState current = stateLiveData.getValue();
        return current != null ? current : readCurrentState();
    }

    public LiveData<CoupleState> observeState() {
        return stateLiveData;
    }

    public String getPlayerName(boolean isPlayerA) {
        CoupleState s = getState();
        return isPlayerA ? s.partnerA.name : s.partnerB.name;
    }

    public String getPlayerInitial(boolean isPlayerA) {
        CoupleState s = getState();
        return isPlayerA ? s.partnerA.getInitial() : s.partnerB.getInitial();
    }

    public String getSafeWord() {
        return getState().safeWord;
    }

    public boolean isOnboardingCompleted() {
        return getState().onboardingCompleted;
    }

    public void saveProfiles(String nameA, String pronounA,
                             String nameB, String pronounB,
                             String safeWord,
                             boolean markOnboardingDone) {
        String cleanA = sanitizeName(nameA, DEFAULT_NAME_A);
        String cleanB = sanitizeName(nameB, DEFAULT_NAME_B);
        String cleanSafe = sanitizeName(safeWord, DEFAULT_SAFE_WORD);

        prefs.edit()
                .putString(KEY_NAME_A, cleanA)
                .putString(KEY_PRONOUN_A, pronounA != null ? pronounA : "He")
                .putString(KEY_NAME_B, cleanB)
                .putString(KEY_PRONOUN_B, pronounB != null ? pronounB : "She")
                .putString(KEY_SAFE_WORD, cleanSafe)
                .putBoolean(KEY_ONBOARDING_DONE, markOnboardingDone || isOnboardingCompleted())
                .apply();

        stateLiveData.postValue(readCurrentState());
    }

    /** Persist the date the couple got together ("yyyy-MM-dd"; empty clears it). */
    public void saveTogetherSince(String date) {
        prefs.edit().putString(KEY_TOGETHER_SINCE, date != null ? date : "").apply();
        stateLiveData.postValue(readCurrentState());
    }

    public String getTogetherSince() {
        return getState().togetherSince;
    }

    /** Whole days together, or -1 when no date has been set / the date is invalid / in the future. */
    public int getDaysTogether() {
        return daysTogetherFrom(getState().togetherSince);
    }

    /** Days until the next anniversary (0 = today), or -1 when unset/invalid. */
    public int getAnniversaryCountdown() {
        return anniversaryCountdownFrom(getState().togetherSince);
    }

    /** Pure helpers (unit-testable, no Android dependency). */
    public static int daysTogetherFrom(String isoDate) {
        LocalDate start = parseDate(isoDate);
        if (start == null) return -1;
        long days = ChronoUnit.DAYS.between(start, LocalDate.now());
        return days < 0 ? -1 : (int) days;
    }

    public static int anniversaryCountdownFrom(String isoDate) {
        LocalDate start = parseDate(isoDate);
        if (start == null) return -1;
        LocalDate today = LocalDate.now();
        LocalDate next = start.withYear(today.getYear());
        if (next.isBefore(today)) next = next.plusYears(1);
        return (int) ChronoUnit.DAYS.between(today, next);
    }

    private static LocalDate parseDate(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) return null;
        try {
            return LocalDate.parse(isoDate, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    public static String sanitizeName(String input, String fallback) {
        if (input == null) return fallback;
        String trimmed = input.trim();
        if (trimmed.isEmpty()) return fallback;
        return trimmed.length() > 18 ? trimmed.substring(0, 18) : trimmed;
    }
}
