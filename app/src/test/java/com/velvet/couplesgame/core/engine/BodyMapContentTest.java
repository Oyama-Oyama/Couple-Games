package com.velvet.couplesgame.core.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class BodyMapContentTest {
    private static final String[] LANGS = {"en", "es", "fr", "pt", "ja", "ko", "zh", "zh_tw"};
    private static final Set<String> ADDED_ZONE_IDS = new LinkedHashSet<>(Arrays.asList(
            "z_side_waist", "z_navel", "z_inner_arm", "z_inner_thigh", "z_ankle", "z_toes"
    ));
    private static final String ASSET_ROOT = "src/main/assets/content/";

    private static class Root {
        List<Zone> bodyMapZones;
    }

    private static class Zone {
        String id;
        String label;
        float nx;
        float ny;
        int tier;
        List<String> tags;
        String normalAction;
        String hotAction;
    }

    private Root load(String lang) throws Exception {
        File file = new File(ASSET_ROOT + lang + "/intimate_modes.json");
        assertTrue("missing asset " + file.getPath(), file.exists());
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8)) {
            return new Gson().fromJson(reader, Root.class);
        }
    }

    @Test
    public void allLanguagesIncludeTheSameAccessibleBodyMapZones() throws Exception {
        Set<String> baseline = null;
        for (String lang : LANGS) {
            Root root = load(lang);
            assertNotNull("empty root for " + lang, root);
            assertEquals("unexpected zone count in " + lang, 16, root.bodyMapZones.size());
            Set<String> ids = new LinkedHashSet<>();
            for (Zone zone : root.bodyMapZones) {
                ids.add(zone.id);
                assertNotNull("missing label for " + zone.id + " in " + lang, zone.label);
                assertFalse("empty prompt for " + zone.id + " in " + lang, zone.normalAction.trim().isEmpty());
                assertFalse("empty alternate prompt for " + zone.id + " in " + lang, zone.hotAction.trim().isEmpty());
                assertTrue("x coordinate outside the view for " + zone.id, zone.nx >= 0f && zone.nx <= 1f);
                assertTrue("y coordinate outside the view for " + zone.id, zone.ny >= 0f && zone.ny <= 1f);
            }
            assertEquals("duplicate zone ids in " + lang, 16, ids.size());
            assertTrue(ids.containsAll(ADDED_ZONE_IDS));
            if (baseline == null) baseline = ids;
            else assertEquals("zone ids differ in " + lang, baseline, ids);
        }
    }
}
