package com.velvet.couplesgame.core.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.velvet.couplesgame.core.i18n.AssetContentLoader.GeneratorBundleDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.TemplateDto;
import com.velvet.couplesgame.core.model.TaskCard;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.Test;

public class Phase13VarietyEngineTest {

    @Test
    public void taskEngineShieldsRecentCardsAcrossDeckRecycle() {
        TaskEngine engine = new TaskEngine(new ConsentFilter(), null, new Random(7));
        List<TaskCard> pool = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            pool.add(new TaskCard(
                    "card_" + i, "d1", (i % 4) + 1, "DARE",
                    Collections.singletonList("physical_light"), 30, "Prompt " + i, false, false
            ));
        }

        // First 16 draws must all be unique
        List<String> history = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            TaskCard drawn = engine.drawNext(pool, null, 4, null, false);
            assertNotNull(drawn);
            assertFalse("Unexpected duplicate during first pass: " + drawn.id, history.contains(drawn.id));
            history.add(drawn.id);
        }

        // Next 4 draws after recycle MUST NOT collide with the last 12 cards drawn
        Set<String> shieldedTail = new HashSet<>(history.subList(history.size() - 12, history.size()));
        for (int i = 0; i < 4; i++) {
            TaskCard next = engine.drawNext(pool, null, 4, null, false);
            assertNotNull(next);
            assertFalse("Recycled draw collided with recent shield: " + next.id, shieldedTail.contains(next.id));
        }
    }

    @Test
    public void proceduralGeneratorAvoidsDuplicateTextsAndConsecutiveTemplates() {
        GeneratorBundleDto bundle = new GeneratorBundleDto();
        List<TemplateDto> templates = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            TemplateDto t = new TemplateDto();
            t.id = "tpl_" + i;
            t.tier = (i <= 5) ? 3 : 4;
            t.type = (i % 3 == 0) ? "TRUTH" : "DARE";
            t.tags = Collections.singletonList("physical_light");
            t.durationSec = 45;
            t.pattern = "Pattern " + i + ": {action} on {zone} at {spot} for {duration}s";
            templates.add(t);
        }
        bundle.templates = templates;

        Map<String, List<String>> slots = new HashMap<>();
        slots.put("action", Arrays.asList("kiss", "nibble", "lick", "trace", "tease", "caress"));
        slots.put("zone", Arrays.asList("neck", "collarbone", "waist", "inner thigh", "hip", "spine"));
        slots.put("spot", Arrays.asList("bed edge", "mirror", "wall", "couch", "rug"));
        slots.put("duration", Arrays.asList("30", "40", "45", "60"));
        bundle.slots = slots;

        ProceduralTaskGenerator gen = new ProceduralTaskGenerator(null, new Random(123));
        Set<String> seenTexts = new HashSet<>();
        String prevTemplatePrefix = "";

        for (int i = 0; i < 30; i++) {
            TaskCard card = gen.generateFromBundle(bundle, 4, null, new ConsentFilter());
            assertNotNull(card);
            assertTrue("Duplicate procedural prompt detected: " + card.text, seenTexts.add(card.text));
            String tplPrefix = card.id.substring(0, card.id.lastIndexOf('_'));
            assertFalse("Same template repeated back-to-back: " + tplPrefix, tplPrefix.equals(prevTemplatePrefix));
            prevTemplatePrefix = tplPrefix;
        }
        assertEquals(30, seenTexts.size());
    }
}
