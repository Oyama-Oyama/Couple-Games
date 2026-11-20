package com.velvet.couplesgame.core.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.velvet.couplesgame.core.i18n.AssetContentLoader.GeneratorBundleDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.TemplateDto;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.core.theme.AppTheme;
import com.velvet.couplesgame.data.local.entity.BoundaryRuleEntity;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class Phase1CoreTest {

    @Test
    public void consentFilterBlocksHardNoFromEitherPlayer() {
        ConsentFilter filter = new ConsentFilter();
        filter.replaceRules(Arrays.asList(
                new BoundaryRuleEntity("blindfold", "YES", "HARD_NO"),
                new BoundaryRuleEntity("deep_talk", "YES", "YES")
        ));

        assertFalse(filter.isAllowed(Collections.singletonList("blindfold")));
        assertTrue(filter.isAllowed(Collections.singletonList("deep_talk")));
    }

    @Test
    public void taskEngineRespectsSkipQuotaAndConsentRules() {
        ConsentFilter filter = new ConsentFilter();
        filter.replaceRules(Collections.singletonList(
                new BoundaryRuleEntity("roleplay", "HARD_NO", "YES")
        ));

        TaskEngine engine = new TaskEngine(filter, null);
        List<TaskCard> pool = Arrays.asList(
                new TaskCard("c1", "d1", 1, "TRUTH", Collections.singletonList("deep_talk"), 0, "Safe card", false, false),
                new TaskCard("c2", "d1", 4, "DARE", Collections.singletonList("roleplay"), 0, "Blocked card", false, false)
        );

        for (int i = 0; i < 10; i++) {
            TaskCard drawn = engine.drawNext(pool, null, 4, null, false);
            assertNotNull(drawn);
            assertEquals("c1", drawn.id);
        }

        assertTrue(engine.consumeSkip(true));
        assertTrue(engine.consumeSkip(true));
        assertTrue(engine.consumeSkip(true));
        assertFalse(engine.consumeSkip(true));
    }

    @Test
    public void proceduralGeneratorRendersSlotsAndResolvesDuration() {
        TemplateDto tpl = new TemplateDto();
        tpl.id = "t1";
        tpl.tier = 3;
        tpl.type = "DARE";
        tpl.tags = Collections.singletonList("physical_light");
        tpl.durationSec = 30;
        tpl.pattern = "{action} for {duration}s.";

        Map<String, List<String>> slots = new HashMap<>();
        slots.put("action", Collections.singletonList("Massage shoulders"));
        slots.put("duration", Collections.singletonList("45"));

        GeneratorBundleDto bundle = new GeneratorBundleDto();
        bundle.templates = Collections.singletonList(tpl);
        bundle.slots = slots;

        ProceduralTaskGenerator generator = new ProceduralTaskGenerator(null);
        TaskCard card = generator.generateFromBundle(bundle, 3, "DARE", new ConsentFilter());

        assertNotNull(card);
        assertEquals("Massage shoulders for 45s.", card.text);
        assertEquals(45, card.durationSec);
        assertTrue(card.isProcedural);
    }

    @Test
    public void appThemeResolvesAllMoodThemesAndFallsBackSafely() {
        assertEquals(5, AppTheme.values().length);
        assertEquals(AppTheme.BLUSH_CHAMPAGNE, AppTheme.fromId("blush_champagne"));
        assertEquals(AppTheme.MIDNIGHT_PLUM, AppTheme.fromId("midnight_plum"));
        assertEquals(AppTheme.CANDLELIGHT_AMBER, AppTheme.fromId("candlelight_amber"));
        assertEquals(AppTheme.NEON_SILK, AppTheme.fromId("neon_silk"));
        assertEquals(AppTheme.VELVET_NOIR, AppTheme.fromId("unknown_id"));
        assertEquals(AppTheme.VELVET_NOIR, AppTheme.fromId(null));
    }
}
