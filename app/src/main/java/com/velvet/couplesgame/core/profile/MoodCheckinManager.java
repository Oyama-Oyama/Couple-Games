package com.velvet.couplesgame.core.profile;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * Offline daily mood check-in plus a short gratitude note for each partner.
 * One entry per calendar day (key = yyyy-MM-dd); checking in again overwrites that day.
 */
public class MoodCheckinManager {
    private static final String PREF_NAME = "velvet_mood_checkin";
    private static final String KEY_PREFIX = "day_";
    private static final String KEY_MOOD = "mood";
    private static final String KEY_GRATITUDE_A = "gratitude_a";
    private static final String KEY_GRATITUDE_B = "gratitude_b";
    private static final String KEY_LAST_DAY = "last_day";
    private static final int MAX_NOTE = 140;

    private final SharedPreferences prefs;
    private final MutableLiveData<Integer> streakLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> checkedInTodayLiveData = new MutableLiveData<>();

    public MoodCheckinManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        refresh();
    }

    public LiveData<Integer> observeStreak() { return streakLiveData; }
    public LiveData<Boolean> observeCheckedInToday() { return checkedInTodayLiveData; }

    public boolean isCheckedInToday() {
        return prefs.contains(prefix() + KEY_MOOD);
    }

    /** Save today's check-in. Empty gratitude notes are allowed. */
    public void checkIn(int moodScore, String gratitudeA, String gratitudeB) {
        String day = prefix();
        prefs.edit()
                .putInt(day + KEY_MOOD, clampMood(moodScore))
                .putString(day + KEY_GRATITUDE_A, clean(gratitudeA))
                .putString(day + KEY_GRATITUDE_B, clean(gratitudeB))
                .putString(KEY_LAST_DAY, LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
                .apply();
        refresh();
    }

    public int getTodayMood() {
        return prefs.getInt(prefix() + KEY_MOOD, -1);
    }

    public String getTodayGratitude(boolean isPlayerA) {
        return prefs.getString(prefix() + (isPlayerA ? KEY_GRATITUDE_A : KEY_GRATITUDE_B), "");
    }

    private void refresh() {
        checkedInTodayLiveData.setValue(isCheckedInToday());
        streakLiveData.setValue(computeStreak());
    }

    /** Consecutive calendar days ending today or yesterday; 0 when never checked in. */
    int computeStreak() {
        String last = prefs.getString(KEY_LAST_DAY, "");
        if (last == null || last.isEmpty()) return 0;
        LocalDate cursor = parse(last);
        if (cursor == null) return 0;
        LocalDate today = LocalDate.now();
        long gap = ChronoUnit.DAYS.between(cursor, today);
        if (gap < 0 || gap > 1) return 0; // streak broken if last check-in older than yesterday
        int streak = 0;
        while (prefs.contains("day_" + cursor.format(DateTimeFormatter.ISO_LOCAL_DATE) + KEY_MOOD)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private String prefix() {
        return "day_" + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + "_";
    }

    /** Total number of calendar days the couple has checked in on. */
    public int getTotalCheckInDays() {
        int count = 0;
        for (String key : prefs.getAll().keySet()) {
            if (key.startsWith("day_") && key.endsWith(KEY_MOOD)) count++;
        }
        return count;
    }

    private static int clampMood(int score) {
        return Math.max(1, Math.min(5, score));
    }

    private static String clean(String note) {
        if (note == null) return "";
        String trimmed = note.trim();
        return trimmed.length() > MAX_NOTE ? trimmed.substring(0, MAX_NOTE) : trimmed;
    }

    private static LocalDate parse(String iso) {
        try {
            return LocalDate.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return null;
        }
    }
}
