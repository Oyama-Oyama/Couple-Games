package com.velvet.couplesgame.core.dateplanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure-Java engine behind the Date Planner mode.
 *
 * <p>The planner runs a short negotiation between both partners:
 * <ol>
 *   <li>{@link Phase#PICK_A} - partner A reviews every shortlisted activity and
 *       marks the ones she would actually want to do.</li>
 *   <li>{@link Phase#PICK_B} - the phone is handed over and partner B does the
 *       same, without seeing A's choices.</li>
 *   <li>{@link Phase#RESULT} - activities both partners picked become the
 *       shortlist; if the partners disagree on everything the session ends with
 *       an empty shortlist and the couple can reroll.</li>
 * </ol>
 *
 * <p>No Android dependency, so the whole state machine is unit-testable.
 */
public class DatePlannerEngine {

    public enum Phase {
        PICK_A,
        PICK_B,
        RESULT,
        EMPTY
    }

    /** One candidate activity shown to the couple. */
    public static final class Activity {
        public final String id;
        public final String category;
        public final int tier;
        public final int durationMin;
        public final List<String> tags;
        public final String title;
        public final String detail;

        public Activity(String id, String category, int tier, int durationMin,
                        List<String> tags, String title, String detail) {
            this.id = id;
            this.category = category;
            this.tier = tier;
            this.durationMin = durationMin;
            this.tags = tags == null ? Collections.<String>emptyList() : Collections.unmodifiableList(tags);
            this.title = title;
            this.detail = detail;
        }
    }

    public static final int DEFAULT_SHORTLIST_SIZE = 6;

    private final List<Activity> pool = new ArrayList<>();
    private final List<Activity> shortlist = new ArrayList<>();
    private final Set<String> pickedByA = new LinkedHashSet<>();
    private final Set<String> pickedByB = new LinkedHashSet<>();

    private Phase phase = Phase.EMPTY;
    private int cursor = 0;

    /** Load the full activity pool and reset the session. */
    public void loadPool(List<Activity> activities) {
        pool.clear();
        if (activities != null) {
            for (Activity a : activities) {
                if (a != null && a.id != null && !a.id.isEmpty()) {
                    pool.add(a);
                }
            }
        }
        resetSession();
    }

    public void resetSession() {
        shortlist.clear();
        pickedByA.clear();
        pickedByB.clear();
        cursor = 0;
        phase = pool.isEmpty() ? Phase.EMPTY : Phase.PICK_A;
    }

    public Phase getPhase() {
        return phase;
    }

    public List<Activity> getShortlist() {
        return Collections.unmodifiableList(shortlist);
    }

    public int getShortlistSize() {
        return shortlist.size();
    }

    public int getCursor() {
        return cursor;
    }

    /**
     * Build the candidate shortlist both partners will vote on.
     * Deterministic given the same pool order: takes the first N activities
     * that respect the player's tier ceiling.
     */
    public void buildShortlist(int size, int maxTier) {
        shortlist.clear();
        int ceiling = Math.max(1, Math.min(4, maxTier));
        for (Activity a : pool) {
            if (a.tier <= ceiling) {
                shortlist.add(a);
            }
            if (shortlist.size() >= size) break;
        }
        // If the tier filter was too strict, fall back to whatever is available.
        if (shortlist.isEmpty()) {
            for (Activity a : pool) {
                shortlist.add(a);
                if (shortlist.size() >= size) break;
            }
        }
        pickedByA.clear();
        pickedByB.clear();
        cursor = 0;
        phase = shortlist.isEmpty() ? Phase.EMPTY : Phase.PICK_A;
    }

    /** Activity currently under review, or null when the list is exhausted. */
    public Activity getCurrentActivity() {
        if (cursor < 0 || cursor >= shortlist.size()) return null;
        return shortlist.get(cursor);
    }

    /** Record partner A's vote on the current activity. */
    public boolean recordPickA(boolean liked) {
        if (phase != Phase.PICK_A) return false;
        Activity current = getCurrentActivity();
        if (current == null) return false;
        if (liked) pickedByA.add(current.id);
        cursor++;
        return true;
    }

    /** Record partner B's vote on the current activity. */
    public boolean recordPickB(boolean liked) {
        if (phase != Phase.PICK_B) return false;
        Activity current = getCurrentActivity();
        if (current == null) return false;
        if (liked) pickedByB.add(current.id);
        cursor++;
        if (cursor >= shortlist.size()) {
            phase = Phase.RESULT;
        }
        return true;
    }

    /** Hand over the phone: partner B now reviews the same shortlist. */
    public void handOffToPartnerB() {
        if (phase != Phase.PICK_A) return;
        cursor = 0;
        phase = Phase.PICK_B;
    }

    /** Activities both partners independently wanted. */
    public List<Activity> getMutualMatches() {
        List<Activity> matches = new ArrayList<>();
        for (Activity a : shortlist) {
            if (pickedByA.contains(a.id) && pickedByB.contains(a.id)) {
                matches.add(a);
            }
        }
        return matches;
    }

    public int getPickedCountA() {
        return pickedByA.size();
    }

    public int getPickedCountB() {
        return pickedByB.size();
    }

    /** Total duration in minutes of the agreed activities. */
    public int getTotalDurationMin(List<Activity> activities) {
        int total = 0;
        if (activities == null) return 0;
        for (Activity a : activities) {
            total += Math.max(0, a.durationMin);
        }
        return total;
    }

    /** Look up an activity by id inside the loaded pool. */
    public Activity findById(String id) {
        if (id == null) return null;
        for (Activity a : pool) {
            if (id.equals(a.id)) return a;
        }
        return null;
    }
}
