package com.velvet.couplesgame.core.dateplanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

/**
 * Verifies the shipped date_planner.json assets for every supported language:
 * identical id sets, complete text fields, and a pool large enough for the
 * planner shortlist plus rerolls.
 */
public class DatePlannerContentTest {

    private static final String[] LANGS = {"en", "zh", "zh_tw", "ja", "ko", "es", "fr", "pt"};
    private static final int MIN_ACTIVITIES = 42;
    private static final String ASSET_ROOT = "src/main/assets/content/";

    private static class Root {
        int version;
        List<String> categories;
        List<Activity> activities;
    }

    private static class Activity {
        String id;
        String category;
        int tier;
        int durationMin;
        List<String> tags;
        String title;
        String detail;
    }

    private Root load(String lang) throws Exception {
        File file = new File(ASSET_ROOT + lang + "/date_planner.json");
        assertTrue("missing asset " + file.getPath(), file.exists());
        try (InputStream is = new FileInputStream(file)) {
            InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            Type type = new TypeToken<Root>() {}.getType();
            return new Gson().fromJson(reader, type);
        }
    }

    @Test
    public void everyLanguageShipsTheSameActivities() throws Exception {
        Set<String> baseline = null;
        for (String lang : LANGS) {
            Root root = load(lang);
            assertNotNull("empty root for " + lang, root);
            assertTrue(lang + " only has " + root.activities.size() + " activities", root.activities.size() >= MIN_ACTIVITIES);
            Set<String> ids = new LinkedHashSet<>();
            Map<String, Integer> categoryCounts = new HashMap<>();
            for (Activity a : root.activities) {
                ids.add(a.id);
                categoryCounts.put(a.category, categoryCounts.getOrDefault(a.category, 0) + 1);
            }
            assertEquals("activity count changed unexpectedly in " + lang, MIN_ACTIVITIES, root.activities.size());
            for (String category : root.categories) {
                assertEquals("category coverage changed for " + category + " in " + lang,
                        Integer.valueOf(7), categoryCounts.get(category));
            }
            assertEquals("duplicate ids in " + lang, root.activities.size(), ids.size());
            if (baseline == null) {
                baseline = ids;
            } else {
                assertEquals("id set differs for " + lang, baseline, ids);
            }
        }
    }

    @Test
    public void everyActivityHasCompleteText() throws Exception {
        for (String lang : LANGS) {
            Root root = load(lang);
            Set<String> categories = new LinkedHashSet<>(root.categories);
            for (Activity a : root.activities) {
                assertNotNull("null id in " + lang, a.id);
                assertNotNull("null title for " + a.id + " in " + lang, a.title);
                assertNotNull("null detail for " + a.id + " in " + lang, a.detail);
                assertFalse("empty title for " + a.id + " in " + lang, a.title.trim().isEmpty());
                assertFalse("empty detail for " + a.id + " in " + lang, a.detail.trim().isEmpty());
                assertTrue("bad tier for " + a.id + " in " + lang, a.tier >= 1 && a.tier <= 4);
                assertTrue("bad duration for " + a.id + " in " + lang, a.durationMin > 0);
                assertTrue("unknown category " + a.category + " for " + a.id + " in " + lang, categories.contains(a.category));
            }
        }
    }

    @Test
    public void poolCoversEveryCategory() throws Exception {
        Root root = load("en");
        List<String> seen = new ArrayList<>();
        for (Activity a : root.activities) {
            if (!seen.contains(a.category)) seen.add(a.category);
        }
        for (String category : root.categories) {
            assertTrue("no activity for category " + category, seen.contains(category));
        }
    }

    @Test
    public void poolFeedsTheShortlistAtEveryTier() throws Exception {
        Root root = load("en");
        for (int tier = 1; tier <= 4; tier++) {
            int count = 0;
            for (Activity a : root.activities) {
                if (a.tier <= tier) count++;
            }
            assertTrue("tier " + tier + " only has " + count + " activities", count >= DatePlannerEngine.DEFAULT_SHORTLIST_SIZE);
        }
    }
}
