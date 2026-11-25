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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class RoleplayContentTest {
    private static final String[] LANGS = {"en", "es", "fr", "pt", "ja", "ko", "zh", "zh_tw"};
    private static final String ASSET_ROOT = "src/main/assets/content/";

    private static class Root {
        List<Scenario> scenarios;
    }

    private static class Scenario {
        String id;
        String title;
        String subtitle;
        String leadRole;
        String partnerRole;
        List<Act> acts;
    }

    private static class Act {
        String actTitle;
        String direction;
        String whisperLine;
    }

    private Root load(String lang) throws Exception {
        File file = new File(ASSET_ROOT + lang + "/roleplay_scenarios.json");
        assertTrue("missing asset " + file.getPath(), file.exists());
        try (InputStreamReader reader = new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8)) {
            return new Gson().fromJson(reader, Root.class);
        }
    }

    @Test
    public void everyLanguageHasTheSameCompleteRoleplayScenarios() throws Exception {
        Set<String> baseline = null;
        for (String lang : LANGS) {
            Root root = load(lang);
            assertNotNull("empty root for " + lang, root);
            assertEquals("unexpected scenario count in " + lang, 50, root.scenarios.size());
            Set<String> ids = new LinkedHashSet<>();
            for (Scenario scenario : root.scenarios) {
                ids.add(scenario.id);
                assertNotNull("missing title for " + scenario.id + " in " + lang, scenario.title);
                assertNotNull("missing subtitle for " + scenario.id + " in " + lang, scenario.subtitle);
                assertEquals("scenario must contain three acts: " + scenario.id, 3, scenario.acts.size());
                for (Act act : scenario.acts) {
                    assertFalse("empty direction in " + scenario.id + " / " + lang,
                            act.direction.trim().isEmpty());
                    assertFalse("empty whisper line in " + scenario.id + " / " + lang,
                            act.whisperLine.trim().isEmpty());
                }
            }
            assertEquals("duplicate scenario ids in " + lang, root.scenarios.size(), ids.size());
            assertTrue(ids.contains("rp_v22_01"));
            assertTrue(ids.contains("rp_v22_02"));
            if (baseline == null) baseline = ids;
            else assertEquals("scenario ids differ in " + lang, baseline, ids);
        }
    }
}
