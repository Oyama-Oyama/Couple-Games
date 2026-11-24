package com.velvet.couplesgame;

import static org.junit.Assert.*;

import com.velvet.couplesgame.core.engine.ConsentFilter;
import com.velvet.couplesgame.core.engine.ModePromptSynthesizer;
import com.velvet.couplesgame.core.engine.PromptHistoryStore;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.*;
import com.velvet.couplesgame.ui.fatecubes.FateCubesEngine;
import com.velvet.couplesgame.ui.foreplay.ForeplayEscalatorEngine;
import com.velvet.couplesgame.ui.connect4.Connect4Engine;
import com.velvet.couplesgame.ui.ladder.DesireLadderEngine;
import com.velvet.couplesgame.ui.roleplay.RoleplayEngine;
import java.util.*;
import org.junit.Test;

public class AntiRepeatV2Test {

    @Test
    public void fateCubesZeroRepeatFullCycleAtEveryTier() {
        FateCubesBundleDto bundle = new FateCubesBundleDto();
        bundle.actions = new ArrayList<>();
        bundle.spots = new ArrayList<>();
        bundle.modifiers = new ArrayList<>();
        for (int t = 1; t <= 4; t++) {
            for (int i = 0; i < 14; i++) {
                CubeFaceDto a = new CubeFaceDto();
                a.id = "a_t" + t + "_" + i;
                a.label = "Action " + a.id;
                a.tier = t;
                a.tags = Collections.singletonList("physical_light");
                bundle.actions.add(a);

                CubeFaceDto s = new CubeFaceDto();
                s.id = "s_t" + t + "_" + i;
                s.label = "Spot " + s.id;
                s.tier = t;
                s.tags = Collections.singletonList("physical_light");
                bundle.spots.add(s);
            }
            for (int i = 0; i < 7; i++) {
                CubeModifierDto m = new CubeModifierDto();
                m.id = "m_t" + t + "_" + i;
                m.label = "Mod " + m.id;
                m.tier = t;
                m.durationSec = 25;
                m.tags = Collections.singletonList("playful");
                bundle.modifiers.add(m);
            }
        }

        for (int tier = 1; tier <= 4; tier++) {
            FateCubesEngine engine = new FateCubesEngine(new Random(100 + tier));
            engine.attachHistoryStore(new PromptHistoryStore());
            engine.loadBundle(bundle, new ConsentFilter(), tier);
            int expectedCycle = tier * 14;
            Set<String> seenActions = new HashSet<>();
            Set<String> seenSpots = new HashSet<>();
            for (int r = 0; r < expectedCycle; r++) {
                CubeFaceDto a = engine.rollAction();
                CubeFaceDto s = engine.rollSpot();
                assertNotNull(a);
                assertNotNull(s);
                assertTrue("Action repeated inside cycle at tier " + tier, seenActions.add(a.id));
                assertTrue("Spot repeated inside cycle at tier " + tier, seenSpots.add(s.id));
                engine.completeRoundAndPrepareNext();
            }
            assertEquals(expectedCycle, seenActions.size());
            assertEquals(expectedCycle, seenSpots.size());
        }
    }

    @Test
    public void foreplayEscalatorRotatesDistinctArcsAcrossSessions() {
        List<ForeplayStageDto> stages = new ArrayList<>();
        for (int arc = 1; arc <= 3; arc++) {
            for (int step = 1; step <= 5; step++) {
                ForeplayStageDto dto = new ForeplayStageDto();
                dto.step = step;
                dto.stageTitle = "Arc " + arc + " Step " + step;
                dto.instruction = "Instruction " + arc + "-" + step;
                dto.durationSec = 60;
                dto.tags = Collections.singletonList("physical_light");
                stages.add(dto);
            }
        }
        PromptHistoryStore history = new PromptHistoryStore();
        ForeplayEscalatorEngine engine = new ForeplayEscalatorEngine(new Random(7));
        engine.attachHistoryStore(history);
        engine.loadStages(stages, new ConsentFilter());

        Set<String> run1 = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            run1.add(engine.getCurrentStage().stageTitle);
            engine.nextStage();
        }
        assertEquals(5, run1.size());
        assertEquals(1, engine.getCompletedSessions());

        Set<String> run2 = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            run2.add(engine.getCurrentStage().stageTitle);
            engine.nextStage();
        }
        for (String title : run2) {
            assertFalse("Second run should pick unseen arc variations", run1.contains(title));
        }
    }

    @Test
    public void desireLadderRotatesDistinctRungsAcrossRuns() {
        List<LadderStepDto> steps = new ArrayList<>();
        for (int arc = 1; arc <= 2; arc++) {
            for (int rung = 1; rung <= 10; rung++) {
                LadderStepDto s = new LadderStepDto();
                s.step = rung;
                s.tier = Math.min(4, 1 + (rung - 1) / 3);
                s.durationSec = 35;
                s.tags = Collections.singletonList("deep_talk");
                s.foreplayAction = "Action " + arc + "-" + rung;
                s.truthQuestion = "Truth " + arc + "-" + rung;
                steps.add(s);
            }
        }
        PromptHistoryStore history = new PromptHistoryStore();
        DesireLadderEngine e1 = new DesireLadderEngine(new Random(11));
        e1.attachHistoryStore(history);
        e1.loadSteps(steps, new ConsentFilter());
        Set<String> run1 = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            run1.add(e1.getCurrentStep().truthQuestion);
            // 末步不再 climbNext：登顶会自动开启新一轮并把全部变体写入共享历史，
            // 使 e2 无法验证"轮换到未见过的变体"这一意图。
            if (i < 9) e1.climbNext();
        }

        DesireLadderEngine e2 = new DesireLadderEngine(new Random(22));
        e2.attachHistoryStore(history);
        e2.loadSteps(steps, new ConsentFilter());
        for (int i = 0; i < 10; i++) {
            assertFalse("Second run should avoid recently seen ladder truths",
                    run1.contains(e2.getCurrentStep().truthQuestion));
            e2.climbNext();
        }
    }

    @Test
    public void synthesizerGeneratesUniqueNhieCards() {
        GeneratorBundleDto g = new GeneratorBundleDto();
        g.slots = new HashMap<>();
        g.slots.put("timeframe", Arrays.asList("last night", "this morning", "last weekend", "on vacation"));
        g.slots.put("feeling", Arrays.asList("flustered", "butterflies", "warm", "shy"));
        g.modeTemplates = new HashMap<>();
        ModeTemplateDto tpl = new ModeTemplateDto();
        tpl.id = "nh_1";
        tpl.tier = 1;
        tpl.tags = Collections.singletonList("deep_talk");
        tpl.pattern = "Never have I ever thought of {timeframe} and felt {feeling}.";
        g.modeTemplates.put("nhie", Collections.singletonList(tpl));

        ModePromptSynthesizer syn = new ModePromptSynthesizer(new Random(42));
        List<NhieCardDto> cards = syn.synthesizeNhieCards(g, new ConsentFilter(), 2, 16);
        assertEquals(16, cards.size());
        Set<String> uniqueTexts = new HashSet<>();
        for (NhieCardDto c : cards) {
            assertFalse(c.text.contains("{"));
            assertTrue(uniqueTexts.add(c.text));
        }
    }

    @Test
    public void connect4PersistsPlayedActionsAcrossBoards() {
        List<Connect4ActionDto> pool = new ArrayList<>();
        for (int i = 0; i < 65; i++) {
            Connect4ActionDto dto = new Connect4ActionDto();
            dto.id = "c4_" + i;
            dto.shortLabel = "Act " + i;
            dto.tier = 1 + (i % 4);
            dto.tags = Collections.singletonList("physical_light");
            dto.fullDare = "Full dare " + i;
            pool.add(dto);
        }
        PromptHistoryStore history = new PromptHistoryStore();
        Connect4Engine engine = new Connect4Engine(new Random(99));
        engine.attachHistoryStore(history);
        engine.loadActionsAndReset(pool, new ConsentFilter(), 4);

        Set<String> board1Ids = new HashSet<>();
        for (Connect4Engine.Cell cell : engine.flattenCells()) {
            assertNotNull(cell.action);
            assertTrue(board1Ids.add(cell.action.id));
        }
        Connect4Engine.DropResult drop = engine.dropInColumn(0);
        assertTrue(drop.valid);
        assertTrue(history.wasRecentlyShown("naughty_connect4", drop.landedCell.action.id));

        engine.resetBoard();
        int overlap = 0;
        for (Connect4Engine.Cell cell : engine.flattenCells()) {
            if (board1Ids.contains(cell.action.id)) overlap++;
        }
        assertEquals("Second 30-cell board should pick unseen actions from 65-item pool", 0, overlap);
    }

    @Test
    public void synthesizerGeneratesPositionsForeplayAndRoleplay() {
        GeneratorBundleDto g = new GeneratorBundleDto();
        g.slots = new HashMap<>();
        g.slots.put("room_spot", Arrays.asList("sofa", "wall", "bed edge", "balcony"));
        g.slots.put("constraint", Arrays.asList("eye contact", "whisper only", "slow breath"));
        g.slots.put("action_gentle", Arrays.asList("kiss", "trace", "caress"));
        g.slots.put("safe_zone", Arrays.asList("wrist", "neck", "collarbone", "jaw"));
        g.slots.put("touch_contrast", Arrays.asList("warm then cool", "firm then soft"));
        g.slots.put("whisper_topic", Arrays.asList("a secret wish", "tonight's spark"));
        g.slots.put("secret_question", Arrays.asList("What makes you blush?", "Where next?"));
        g.slots.put("date_setting", Arrays.asList("rooftop", "candlelit lounge"));
        g.slots.put("duration", Arrays.asList("30", "45", "60"));
        g.modeTemplates = new HashMap<>();

        ModeTemplateDto mp = new ModeTemplateDto();
        mp.id = "mpos_1"; mp.tier = 1; mp.category = "1"; mp.durationSec = 50;
        mp.tags = Collections.singletonList("physical_light");
        mp.title = "Pose · {room_spot}";
        mp.pattern = "At {room_spot} with {constraint}, {action_gentle} {safe_zone}.";
        mp.secondaryPattern = "Try {touch_contrast}.";
        g.modeTemplates.put("positions", Collections.singletonList(mp));

        ModeTemplateDto mfp = new ModeTemplateDto();
        mfp.id = "mfp_1"; mfp.tier = 1; mfp.durationSec = 60;
        mfp.tags = Collections.singletonList("physical_light");
        mfp.title = "Warmth · {room_spot}";
        mfp.pattern = "Move to {room_spot} with {constraint}, {action_gentle} {safe_zone} ({duration}s).";
        g.modeTemplates.put("foreplay", Collections.singletonList(mfp));

        ModeTemplateDto mrp = new ModeTemplateDto();
        mrp.id = "mrp_1"; mrp.tier = 1; mrp.durationSec = 45;
        mrp.tags = Collections.singletonList("roleplay");
        mrp.title = "{date_setting} Spark";
        mrp.pattern = "At {date_setting} near {room_spot}, whisper {whisper_topic} and {action_gentle} {safe_zone}.";
        g.modeTemplates.put("roleplay", Collections.singletonList(mrp));

        ModePromptSynthesizer syn = new ModePromptSynthesizer(new Random(123));
        List<PositionDto> poses = syn.synthesizePositions(g, new ConsentFilter(), 2, 12);
        assertEquals(12, poses.size());
        List<ForeplayStageDto> stages = syn.synthesizeForeplayStages(g, new ConsentFilter(), 2, 2);
        assertEquals(10, stages.size());
        List<RoleplayScenarioDto> scenes = syn.synthesizeRoleplayScenarios(g, new ConsentFilter(), 2, 8);
        assertEquals(8, scenes.size());
        assertEquals(3, scenes.get(0).acts.size());
    }
}
