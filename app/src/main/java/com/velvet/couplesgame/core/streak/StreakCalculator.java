package com.velvet.couplesgame.core.streak;

import com.velvet.couplesgame.data.local.entity.SessionStatEntity;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Pure-Java derivation of play streaks from the session history. No Android dependency. */
public final class StreakCalculator {
    private StreakCalculator() {}

    public static class Result {
        public final int currentStreak;
        public final int longestStreak;
        public final int totalActiveDays;
        public Result(int current, int longest, int total) {
            this.currentStreak = current;
            this.longestStreak = longest;
            this.totalActiveDays = total;
        }
    }

    /** Overload for tests / callers that already normalized history to distinct days. */
    public static Result compute(Set<LocalDate> activeDays) {
        if (activeDays == null || activeDays.isEmpty()) return new Result(0, 0, 0);

        int longest = 0;
        for (LocalDate day : activeDays) {
            int run = 0;
            LocalDate cursor = day;
            while (activeDays.contains(cursor)) {
                run++;
                cursor = cursor.minusDays(1);
            }
            if (run > longest) longest = run;
        }

        LocalDate today = LocalDate.now();
        LocalDate anchor = activeDays.contains(today) ? today : today.minusDays(1);
        int current = 0;
        LocalDate cursor = anchor;
        while (activeDays.contains(cursor)) {
            current++;
            cursor = cursor.minusDays(1);
        }
        return new Result(current, longest, activeDays.size());
    }

    /** Compute from raw session history. A day counts when both players completed > 0. */
    public static Result compute(List<SessionStatEntity> history) {
        Set<LocalDate> days = new HashSet<>();
        if (history != null) {
            for (SessionStatEntity s : history) {
                if (s == null) continue;
                if (s.playerACompleted + s.playerBCompleted <= 0) continue;
                LocalDate day = java.time.Instant.ofEpochMilli(s.playedAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate();
                days.add(day);
            }
        }
        return compute(days);
    }
}
