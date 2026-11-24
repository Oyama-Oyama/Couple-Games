package com.velvet.couplesgame.core.streak;

import com.velvet.couplesgame.R;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Milestone badges derived from play history; no persistence needed. */
public final class BadgeCatalog {
    private BadgeCatalog() {}

    public static final class Badge {
        public final String id;
        public final int labelRes;
        public final int threshold;
        public final Kind kind;

        public enum Kind { SESSIONS, STREAK, DAYS }

        public Badge(String id, int labelRes, int threshold, Kind kind) {
            this.id = id;
            this.labelRes = labelRes;
            this.threshold = threshold;
            this.kind = kind;
        }

        public boolean isUnlocked(StreakCalculator.Result stats, int totalSessions) {
            switch (kind) {
                case SESSIONS: return totalSessions >= threshold;
                case STREAK: return stats.longestStreak >= threshold;
                case DAYS: return stats.totalActiveDays >= threshold;
                default: return false;
            }
        }
    }

    private static final List<Badge> ALL;
    static {
        List<Badge> list = new ArrayList<>();
        list.add(new Badge("first_night", R.string.badge_first_night, 1, Badge.Kind.SESSIONS));
        list.add(new Badge("five_nights", R.string.badge_five_nights, 5, Badge.Kind.SESSIONS));
        list.add(new Badge("streak_3", R.string.badge_streak_3, 3, Badge.Kind.STREAK));
        list.add(new Badge("streak_7", R.string.badge_streak_7, 7, Badge.Kind.STREAK));
        list.add(new Badge("days_14", R.string.badge_days_14, 14, Badge.Kind.DAYS));
        ALL = Collections.unmodifiableList(list);
    }

    public static List<Badge> all() { return ALL; }
}
