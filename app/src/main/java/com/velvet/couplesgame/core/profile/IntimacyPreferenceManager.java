package com.velvet.couplesgame.core.profile;

import android.content.Context;
import android.content.SharedPreferences;
import com.velvet.couplesgame.core.model.GameModeCatalog;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Shared couple play-style preferences, stored only on this device. */
public final class IntimacyPreferenceManager {
    private static final String PREF_NAME = "velvet_intimacy_preferences";
    private static final String KEY_STYLE_MASK = "style_mask";

    public enum Style {
        ROMANTIC(
                "date_planner", "couple_quiz", "daily_question", "love_language",
                "sealed_letter", "daily_reveal", "conversation_starter", "memory_lane"
        ),
        PLAYFUL(
                "party_duel", "lucky_wheel", "riddle_me", "couple_mines",
                "naughty_connect4", "forfeit_wheel", "most_likely_to"
        ),
        SENSORY(
                "heartbeat_hold", "body_map_hunt", "prop_sensory_lab",
                "foreplay_escalator", "positions_roulette", "scratch_adventure"
        ),
        IMAGINATIVE(
                "roleplay_theater", "scenario_choice", "secret_mission",
                "velvet_bomb", "desire_auction", "desire_ladder", "bucket_list"
        );

        private final Set<String> modeIds;

        Style(String... modeIds) {
            Set<String> ids = new HashSet<>();
            Collections.addAll(ids, modeIds);
            this.modeIds = Collections.unmodifiableSet(ids);
        }

        boolean recommends(String modeId) {
            return modeIds.contains(modeId);
        }
    }

    private final SharedPreferences preferences;

    public IntimacyPreferenceManager(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public Set<Style> getSelectedStyles() {
        int mask = preferences.getInt(KEY_STYLE_MASK, 0);
        EnumSet<Style> selected = EnumSet.noneOf(Style.class);
        for (Style style : Style.values()) {
            if ((mask & (1 << style.ordinal())) != 0) selected.add(style);
        }
        return Collections.unmodifiableSet(selected);
    }

    public boolean saveSelectedStyles(Set<Style> selectedStyles) {
        int mask = 0;
        if (selectedStyles != null) {
            for (Style style : selectedStyles) {
                if (style != null) mask |= 1 << style.ordinal();
            }
        }
        return preferences.edit().putInt(KEY_STYLE_MASK, mask).commit();
    }

    /** Returns selected-style matches first; the result never changes content or consent filters. */
    public List<GameModeCatalog> recommendModes(List<GameModeCatalog> candidates) {
        Set<Style> selected = getSelectedStyles();
        if (selected.isEmpty() || candidates == null || candidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<GameModeCatalog> recommended = new ArrayList<>();
        for (GameModeCatalog mode : candidates) {
            if (mode != null && preferenceScore(mode, selected) > 0) recommended.add(mode);
        }
        recommended.sort(Comparator
                .comparingInt((GameModeCatalog mode) -> preferenceScore(mode, selected))
                .reversed()
                .thenComparingInt(mode -> mode.heatRating)
                .thenComparing(mode -> mode.id));
        return Collections.unmodifiableList(recommended);
    }

    private static int preferenceScore(GameModeCatalog mode, Set<Style> selected) {
        int score = 0;
        for (Style style : selected) {
            if (style.recommends(mode.id)) score++;
        }
        return score;
    }
}
