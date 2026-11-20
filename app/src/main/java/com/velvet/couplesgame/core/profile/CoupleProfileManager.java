package com.velvet.couplesgame.core.profile;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.velvet.couplesgame.core.engine.TaskEngine;
import com.velvet.couplesgame.core.model.PlayerProfile;

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

        public CoupleState(PlayerProfile partnerA, PlayerProfile partnerB, String safeWord, boolean onboardingCompleted) {
            this.partnerA = partnerA;
            this.partnerB = partnerB;
            this.safeWord = safeWord;
            this.onboardingCompleted = onboardingCompleted;
        }
    }

    private static final String PREF_NAME = "velvet_couple_profile";
    private static final String KEY_NAME_A = "name_a";
    private static final String KEY_PRONOUN_A = "pronoun_a";
    private static final String KEY_NAME_B = "name_b";
    private static final String KEY_PRONOUN_B = "pronoun_b";
    private static final String KEY_SAFE_WORD = "safe_word";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";

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
                prefs.getString(KEY_PRONOUN_A, "They"),
                TaskEngine.DEFAULT_SKIP_QUOTA
        );
        PlayerProfile b = new PlayerProfile(
                prefs.getString(KEY_NAME_B, DEFAULT_NAME_B),
                prefs.getString(KEY_PRONOUN_B, "She"),
                TaskEngine.DEFAULT_SKIP_QUOTA
        );
        String safe = prefs.getString(KEY_SAFE_WORD, DEFAULT_SAFE_WORD);
        boolean done = prefs.getBoolean(KEY_ONBOARDING_DONE, false);
        return new CoupleState(a, b, safe, done);
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
                .putString(KEY_PRONOUN_A, pronounA != null ? pronounA : "They")
                .putString(KEY_NAME_B, cleanB)
                .putString(KEY_PRONOUN_B, pronounB != null ? pronounB : "She")
                .putString(KEY_SAFE_WORD, cleanSafe)
                .putBoolean(KEY_ONBOARDING_DONE, markOnboardingDone || isOnboardingCompleted())
                .apply();

        stateLiveData.postValue(readCurrentState());
    }

    public static String sanitizeName(String input, String fallback) {
        if (input == null) return fallback;
        String trimmed = input.trim();
        if (trimmed.isEmpty()) return fallback;
        return trimmed.length() > 18 ? trimmed.substring(0, 18) : trimmed;
    }
}
