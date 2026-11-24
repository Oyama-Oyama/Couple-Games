package com.velvet.couplesgame.core.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.velvet.couplesgame.core.i18n.AssetContentLoader.BlackjackStakeDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.BodyMapZoneDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.Connect4ActionDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.GeneratorBundleDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.ModeTemplateDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.PositionDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.ScratchCardDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.WyrCardDto;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.data.local.entity.BoundaryRuleEntity;
import com.velvet.couplesgame.ui.blackjack.BlackjackEngine;
import com.velvet.couplesgame.ui.bodymap.BodyMapEngine;
import com.velvet.couplesgame.ui.connect4.Connect4Engine;
import com.velvet.couplesgame.ui.positions.PositionsEngine;
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

public class Phase15ComprehensiveAuditTest {

    @Test
    public void promptHistoryStoreTracksRollingCapacityCleanly() {
        PromptHistoryStore store = new PromptHistoryStore();
        for (int i = 1; i <= 10; i++) {
            store.recordShown("scratch", "id_" + i, 5);
        }
        assertFalse(store.wasRecentlyShown("scratch", "id_1"));
        assertFalse(store.wasRecentlyShown("scratch", "id_5"));
        assertTrue(store.wasRecentlyShown("scratch", "id_6"));
        assertTrue(store.wasRecentlyShown("scratch", "id_10"));
        assertEquals(5, store.getRecentSet("scratch").size());
    }

    @Test
    public void blackjackEngineDealsNonRepeatingStakesAcrossFullCycle() {
        List<BlackjackStakeDto> stakes = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            BlackjackStakeDto s = new BlackjackStakeDto();
            s.id = "bj_" + i;
            s.tier = 2;
            s.tags = Collections.singletonList("physical_light");
            s.label = "Stake " + i;
            stakes.add(s);
        }
        BlackjackEngine engine = new BlackjackEngine(new Random(21));
        engine.loadStakes(stakes, new ConsentFilter(), 4);

        Set<String> firstPass = new HashSet<>();
        String prevId = null;
        for (int i = 0; i < 8; i++) {
            engine.startNewHand();
            BlackjackStakeDto st = engine.getActiveStake();
            assertNotNull(st);
            assertTrue("Duplicate stake in first pass: " + st.id, firstPass.add(st.id));
            prevId = st.id;
        }
        engine.startNewHand();
        assertNotEquals("Stake repeated immediately across reshuffle", prevId, engine.getActiveStake().id);
    }

    @Test
    public void bodyMapCelebratesAllHotSpotsFoundExactlyOncePerRound() {
        BodyMapZoneDto z1 = new BodyMapZoneDto();
        z1.id = "z1"; z1.label = "Neck"; z1.tier = 2; z1.tags = Collections.singletonList("physical_light");
        z1.normalAction = "N"; z1.hotAction = "H"; z1.durationSec = 25;

        BodyMapZoneDto z2 = new BodyMapZoneDto();
        z2.id = "z2"; z2.label = "Lips"; z2.tier = 1; z2.tags = Collections.singletonList("physical_light");
        z2.normalAction = "N"; z2.hotAction = "H"; z2.durationSec = 20;

        BodyMapEngine engine = new BodyMapEngine();
        engine.loadZones(Arrays.asList(z1, z2), new ConsentFilter(), 4);
        assertTrue(engine.toggleSecretHotSpot("z1"));
        engine.beginExplore();

        BodyMapEngine.TapResult firstHit = engine.exploreZone("z1");
        assertTrue(firstHit.wasHotSpot);
        assertTrue("First completion should celebrate", firstHit.allHotSpotsFound);

        // Re-tapping z1 or tapping z2 afterwards must NOT fire allHotSpotsFound again!
        BodyMapEngine.TapResult repeatHit = engine.exploreZone("z1");
        assertFalse("Repeat tap must not re-fire celebration", repeatHit.allHotSpotsFound);
        BodyMapEngine.TapResult otherTap = engine.exploreZone("z2");
        assertFalse("Subsequent normal tap must not re-fire celebration", otherTap.allHotSpotsFound);
    }

    @Test
    public void connect4PopulatesThirtyDistinctCellsAndRespectsMaxTier() {
        List<Connect4ActionDto> pool = new ArrayList<>();
        for (int i = 1; i <= 36; i++) {
            Connect4ActionDto a = new Connect4ActionDto();
            a.id = "c4_" + i;
            a.shortLabel = "Act " + i;
            a.tier = (i <= 30) ? 2 : 4;
            a.tags = Collections.singletonList("physical_light");
            a.fullDare = "Full dare " + i;
            pool.add(a);
        }
        Connect4Engine engine = new Connect4Engine(new Random(5));
        engine.loadActionsAndReset(pool, new ConsentFilter(), 2);

        Set<String> boardIds = new HashSet<>();
        for (Connect4Engine.Cell cell : engine.flattenCells()) {
            assertNotNull(cell.action);
            assertTrue("Expected tier <= 2, got " + cell.action.tier, cell.action.tier <= 2);
            boardIds.add(cell.action.id);
        }
        assertEquals("Expected 30 distinct actions on 5x6 Connect4 board", 30, boardIds.size());
    }

    @Test
    public void positionsEngineCyclesAllPositionsWithoutImmediateRepeat() {
        List<PositionDto> list = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            PositionDto p = new PositionDto();
            p.id = "p_" + i; p.name = "Pos " + i; p.difficulty = 2; p.heat = 4; p.durationSec = 60;
            p.tags = Collections.singletonList("physical_light"); p.setup = "S"; p.tip = "T";
            list.add(p);
        }
        PositionsEngine engine = new PositionsEngine(new Random(33));
        engine.attachHistoryStore(new PromptHistoryStore());
        engine.loadPositions(list, new ConsentFilter());

        Set<String> seen = new HashSet<>();
        String prev = null;
        for (int i = 0; i < 6; i++) {
            PositionDto p = engine.spinRandom();
            assertNotNull(p);
            assertTrue("Duplicate inside first full cycle: " + p.id, seen.add(p.id));
            prev = p.id;
        }
        PositionDto afterCycle = engine.spinRandom();
        assertNotEquals("Immediate repeat after full cycle", prev, afterCycle.id);
    }

    @Test
    public void taskEngineResetSkipsOnlyPreservesAntiRepeatShield() {
        TaskEngine engine = new TaskEngine(new ConsentFilter(), null, new Random(1));
        List<TaskCard> pool = Arrays.asList(
                new TaskCard("c1", "d", 1, "TRUTH", Collections.singletonList("deep_talk"), 0, "One", false, false),
                new TaskCard("c2", "d", 1, "TRUTH", Collections.singletonList("deep_talk"), 0, "Two", false, false)
        );
        TaskCard first = engine.drawNext(pool, null, 4, null, false);
        assertTrue(engine.consumeSkip(true));
        assertEquals(2, engine.getSkipsLeft(true));

        engine.resetSkipsOnly();
        assertEquals(TaskEngine.DEFAULT_SKIP_QUOTA, engine.getSkipsLeft(true));

        TaskCard second = engine.drawNext(pool, null, 4, null, false);
        assertNotEquals("resetSkipsOnly must preserve used card history", first.id, second.id);
    }

    @Test
    public void modePromptSynthesizerGeneratesHundredsOfUniqueConsentSafePrompts() {
        GeneratorBundleDto bundle = new GeneratorBundleDto();
        Map<String, List<String>> slots = new HashMap<>();
        slots.put("action_bold", Arrays.asList("lick", "suck", "nibble", "kiss", "bite", "trace"));
        slots.put("intimate_zone", Arrays.asList("neck", "earlobe", "collarbone", "waist", "inner thigh", "hip", "spine"));
        slots.put("explore_end", Arrays.asList("lower belly", "hip bone", "inner thigh", "ribs", "collarbone"));
        slots.put("tempo_rule", Arrays.asList("freeze 3s", "slow down", "hold eyes", "whisper name"));
        slots.put("duration", Arrays.asList("30", "40", "45", "60"));
        bundle.slots = slots;

        ModeTemplateDto allowedTpl = new ModeTemplateDto();
        allowedTpl.id = "t_ok"; allowedTpl.category = "DARE"; allowedTpl.tier = 3; allowedTpl.durationSec = 40;
        allowedTpl.tags = Collections.singletonList("physical_light");
        allowedTpl.title = "{intimate_zone} Tease";
        allowedTpl.pattern = "{action_bold} {intimate_zone} to {explore_end} for {duration}s ({tempo_rule})";

        ModeTemplateDto blockedTpl = new ModeTemplateDto();
        blockedTpl.id = "t_no"; blockedTpl.category = "DARE"; blockedTpl.tier = 4; blockedTpl.durationSec = 45;
        blockedTpl.tags = Collections.singletonList("blindfold");
        blockedTpl.title = "Blocked";
        blockedTpl.pattern = "Blocked {intimate_zone}";

        Map<String, List<ModeTemplateDto>> mt = new HashMap<>();
        mt.put("scratch", Arrays.asList(allowedTpl, blockedTpl));
        bundle.modeTemplates = mt;

        ConsentFilter filter = new ConsentFilter();
        filter.replaceRules(Collections.singletonList(new BoundaryRuleEntity("blindfold", "YES", "HARD_NO")));

        ModePromptSynthesizer syn = new ModePromptSynthesizer(new Random(77));
        List<ScratchCardDto> generated = syn.synthesizeScratchCards(bundle, filter, 4, 200);
        assertEquals(200, generated.size());

        Set<String> uniqueTexts = new HashSet<>();
        for (ScratchCardDto c : generated) {
            assertFalse("Blocked blindfold tag slipped through!", c.tags.contains("blindfold"));
            assertTrue("Duplicate synthesized scratch prompt!", uniqueTexts.add(c.text));
        }

        // Verify WYR cross-pair synthesizer
        List<WyrCardDto> baseWyr = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            WyrCardDto w = new WyrCardDto();
            w.id = "w_" + i; w.tier = 2; w.tags = Collections.singletonList("playful");
            w.optionA = "OptA_" + i; w.optionB = "OptB_" + i;
            baseWyr.add(w);
        }
        List<WyrCardDto> synWyr = syn.synthesizeWyrMatchups(baseWyr, filter, 4, 120);
        assertEquals(120, synWyr.size());
    }
}
