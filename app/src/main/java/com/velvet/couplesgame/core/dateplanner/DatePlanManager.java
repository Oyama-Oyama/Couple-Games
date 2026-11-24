package com.velvet.couplesgame.core.dateplanner;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores the couple's upcoming date plan (chosen activity + date + note).
 *
 * <p>A plan is keyed by ISO date, so planning a new date naturally replaces any
 * previous plan for the same day. At most one plan per day is kept, and the
 * newest list is always sorted ascending by date.
 */
public class DatePlanManager {

    private static final String PREF_NAME = "velvet_date_plans";
    private static final String KEY_PREFIX = "plan_";
    private static final String KEY_ACTIVITY_ID = "activity_id";
    private static final String KEY_TITLE = "title";
    private static final String KEY_NOTE = "note";
    private static final String KEY_CREATED = "created";
    private static final int MAX_PLANS = 30;
    private static final int MAX_NOTE = 120;

    /** One saved date plan. */
    public static final class Plan {
        public final String dateIso;
        public final String activityId;
        public final String title;
        public final String note;
        public final long createdAt;

        public Plan(String dateIso, String activityId, String title, String note, long createdAt) {
            this.dateIso = dateIso;
            this.activityId = activityId;
            this.title = title;
            this.note = note;
            this.createdAt = createdAt;
        }

        public LocalDate getDate() {
            return parse(dateIso);
        }

        /** Days from today until this plan; negative when the date has passed. */
        public long daysUntilToday() {
            LocalDate d = getDate();
            if (d == null) return 0;
            return java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), d);
        }
    }

    private final SharedPreferences prefs;
    private final MutableLiveData<List<Plan>> plansLiveData = new MutableLiveData<>();

    public DatePlanManager(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        refresh();
    }

    public LiveData<List<Plan>> observePlans() {
        return plansLiveData;
    }

    /** Save (or replace) the plan for a given day. */
    public void savePlan(LocalDate date, String activityId, String title, String note) {
        if (date == null || activityId == null) return;
        String iso = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        prefs.edit()
                .putString(KEY_PREFIX + iso + KEY_ACTIVITY_ID, activityId)
                .putString(KEY_PREFIX + iso + KEY_TITLE, clean(title))
                .putString(KEY_PREFIX + iso + KEY_NOTE, clean(note))
                .putLong(KEY_PREFIX + iso + KEY_CREATED, System.currentTimeMillis())
                .apply();
        trimOldPlans();
        refresh();
    }

    public void deletePlan(LocalDate date) {
        if (date == null) return;
        String iso = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        prefs.edit()
                .remove(KEY_PREFIX + iso + KEY_ACTIVITY_ID)
                .remove(KEY_PREFIX + iso + KEY_TITLE)
                .remove(KEY_PREFIX + iso + KEY_NOTE)
                .remove(KEY_PREFIX + iso + KEY_CREATED)
                .apply();
        refresh();
    }

    /** All plans, ascending by date. */
    public List<Plan> getPlans() {
        List<Plan> plans = new ArrayList<>();
        for (String key : prefs.getAll().keySet()) {
            if (!key.startsWith(KEY_PREFIX) || !key.endsWith(KEY_ACTIVITY_ID)) continue;
            String iso = key.substring(KEY_PREFIX.length(), key.length() - KEY_ACTIVITY_ID.length());
            if (parse(iso) == null) continue;
            plans.add(new Plan(
                    iso,
                    prefs.getString(KEY_PREFIX + iso + KEY_ACTIVITY_ID, ""),
                    prefs.getString(KEY_PREFIX + iso + KEY_TITLE, ""),
                    prefs.getString(KEY_PREFIX + iso + KEY_NOTE, ""),
                    prefs.getLong(KEY_PREFIX + iso + KEY_CREATED, 0L)
            ));
        }
        plans.sort((a, b) -> a.dateIso.compareTo(b.dateIso));
        return plans;
    }

    /** The nearest upcoming plan, or null when none is scheduled. */
    public Plan getUpcomingPlan() {
        LocalDate today = LocalDate.now();
        Plan best = null;
        for (Plan p : getPlans()) {
            LocalDate d = p.getDate();
            if (d != null && !d.isBefore(today)) {
                if (best == null || p.dateIso.compareTo(best.dateIso) < 0) {
                    best = p;
                }
            }
        }
        return best;
    }

    public Plan getPlanFor(LocalDate date) {
        if (date == null) return null;
        String iso = date.format(DateTimeFormatter.ISO_LOCAL_DATE);
        if (!prefs.contains(KEY_PREFIX + iso + KEY_ACTIVITY_ID)) return null;
        return new Plan(
                iso,
                prefs.getString(KEY_PREFIX + iso + KEY_ACTIVITY_ID, ""),
                prefs.getString(KEY_PREFIX + iso + KEY_TITLE, ""),
                prefs.getString(KEY_PREFIX + iso + KEY_NOTE, ""),
                prefs.getLong(KEY_PREFIX + iso + KEY_CREATED, 0L)
        );
    }

    private void trimOldPlans() {
        List<Plan> plans = getPlans();
        if (plans.size() <= MAX_PLANS) return;
        SharedPreferences.Editor editor = prefs.edit();
        for (int i = 0; i < plans.size() - MAX_PLANS; i++) {
            String iso = plans.get(i).dateIso;
            editor.remove(KEY_PREFIX + iso + KEY_ACTIVITY_ID)
                    .remove(KEY_PREFIX + iso + KEY_TITLE)
                    .remove(KEY_PREFIX + iso + KEY_NOTE)
                    .remove(KEY_PREFIX + iso + KEY_CREATED);
        }
        editor.apply();
    }

    private void refresh() {
        plansLiveData.setValue(getPlans());
    }

    private static String clean(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.length() > MAX_NOTE ? trimmed.substring(0, MAX_NOTE) : trimmed;
    }

    private static LocalDate parse(String iso) {
        if (iso == null || iso.isEmpty()) return null;
        try {
            return LocalDate.parse(iso, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return null;
        }
    }
}
