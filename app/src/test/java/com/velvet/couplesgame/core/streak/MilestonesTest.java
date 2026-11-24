package com.velvet.couplesgame.core.streak;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.velvet.couplesgame.core.profile.CoupleProfileManager;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

/** Tests for the pure-Java streak, badge and date helpers powering the milestone UI. */
public class MilestonesTest {

    @Test
    public void streakCountsConsecutiveDaysEndingTodayOrYesterday() {
        LocalDate today = LocalDate.now();
        Set<LocalDate> days = new HashSet<>();
        days.add(today.minusDays(2));
        days.add(today.minusDays(1));
        days.add(today);
        StreakCalculator.Result r = StreakCalculator.compute(days);
        assertEquals(3, r.currentStreak);
        assertEquals(3, r.longestStreak);
        assertEquals(3, r.totalActiveDays);
    }

    @Test
    public void streakUsesYesterdayAsAnchorWhenTodayNotPlayed() {
        LocalDate today = LocalDate.now();
        Set<LocalDate> days = new HashSet<>();
        days.add(today.minusDays(3));
        days.add(today.minusDays(2));
        days.add(today.minusDays(1));
        StreakCalculator.Result r = StreakCalculator.compute(days);
        assertEquals(3, r.currentStreak);
    }

    @Test
    public void streakZeroWhenBroken() {
        LocalDate today = LocalDate.now();
        Set<LocalDate> days = new HashSet<>();
        days.add(today);
        days.add(today.minusDays(2)); // gap at yesterday
        StreakCalculator.Result r = StreakCalculator.compute(days);
        assertEquals(1, r.currentStreak);
        assertEquals(1, r.longestStreak);
    }

    @Test
    public void emptyHistoryYieldsZeroStreak() {
        StreakCalculator.Result r = StreakCalculator.compute(new HashSet<>());
        assertEquals(0, r.currentStreak);
        assertEquals(0, r.longestStreak);
        assertEquals(0, r.totalActiveDays);
    }

    @Test
    public void longestStreakTracksTheBestRun() {
        LocalDate today = LocalDate.now();
        Set<LocalDate> days = new HashSet<>();
        for (int i = 0; i < 5; i++) days.add(today.minusDays(20 + i)); // old run of 5
        days.add(today); // current run of 1
        StreakCalculator.Result r = StreakCalculator.compute(days);
        assertEquals(1, r.currentStreak);
        assertEquals(5, r.longestStreak);
    }

    @Test
    public void badgesUnlockAtThresholds() {
        LocalDate today = LocalDate.now();
        Set<LocalDate> days = new HashSet<>();
        for (int i = 0; i < 7; i++) days.add(today.minusDays(i));
        StreakCalculator.Result r = StreakCalculator.compute(days);
        assertTrue(BadgeCatalog.all().get(0).isUnlocked(r, 1));   // first night
        assertTrue(BadgeCatalog.all().get(2).isUnlocked(r, 7));   // 3-day streak
        assertTrue(BadgeCatalog.all().get(3).isUnlocked(r, 7));   // 7-day streak
        assertFalse(BadgeCatalog.all().get(1).isUnlocked(r, 4));  // five nights not reached
        assertFalse(BadgeCatalog.all().get(4).isUnlocked(r, 7));  // 14 days not reached
    }

    @Test
    public void daysTogetherAndAnniversaryCountdown() {
        LocalDate today = LocalDate.now();
        String threeDaysAgo = today.minusDays(3).toString();
        assertEquals(3, CoupleProfileManager.daysTogetherFrom(threeDaysAgo));
        int countdown = CoupleProfileManager.anniversaryCountdownFrom(threeDaysAgo);
        assertTrue(countdown >= 361 && countdown <= 365); // roughly one year out
        assertEquals(0, CoupleProfileManager.anniversaryCountdownFrom(today.toString()));
        assertEquals(-1, CoupleProfileManager.daysTogetherFrom(""));
        assertEquals(-1, CoupleProfileManager.daysTogetherFrom("not-a-date"));
        assertEquals(-1, CoupleProfileManager.daysTogetherFrom(today.plusDays(5).toString()));
    }
}
