package com.velvet.couplesgame.core.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.velvet.couplesgame.core.billing.FeatureGate;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.Connect4ActionDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.ForeplayStageDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.LadderStepDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.PositionDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.PropChallengeDto;
import com.velvet.couplesgame.core.model.GameModeCatalog;
import com.velvet.couplesgame.ui.connect4.Connect4Engine;
import com.velvet.couplesgame.ui.foreplay.ForeplayEscalatorEngine;
import com.velvet.couplesgame.ui.ladder.DesireLadderEngine;
import com.velvet.couplesgame.ui.positions.PositionsEngine;
import com.velvet.couplesgame.ui.props.PropLabEngine;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.Test;

public class Phase10ExtremeModesTest {

    @Test
    public void gameModeCatalogSortsByStimulationAndEnforcesVipSubscriptionGating() {
        assertEquals(36, GameModeCatalog.values().length);

        List<GameModeCatalog> hottest = GameModeCatalog.queryModes(GameModeCatalog.SortFilter.HOTTEST_FIRST);
        assertEquals(36, hottest.size());
        for (int i = 1; i < hottest.size(); i++) {
            assertTrue(hottest.get(i - 1).heatRating >= hottest.get(i).heatRating);
        }

        List<GameModeCatalog> gentle = GameModeCatalog.queryModes(GameModeCatalog.SortFilter.GENTLE_TO_WILD);
        for (int i = 1; i < gentle.size(); i++) {
            assertTrue(gentle.get(i - 1).heatRating <= gentle.get(i).heatRating);
        }

        List<GameModeCatalog> vipOnly = GameModeCatalog.queryModes(GameModeCatalog.SortFilter.VIP_ONLY);
        List<GameModeCatalog> freeOnly = GameModeCatalog.queryModes(GameModeCatalog.SortFilter.FREE_ONLY);
        assertEquals(12, vipOnly.size());
        assertEquals(24, freeOnly.size());

        // Free user blocked from 5-flame VIP modes, allowed on free modes
        assertFalse(FeatureGate.canAccessGameMode(GameModeCatalog.POSITIONS_ROULETTE, false));
        assertFalse(FeatureGate.canAccessGameMode(GameModeCatalog.FOREPLAY_ESCALATOR, false));
        assertFalse(FeatureGate.canAccessGameMode(GameModeCatalog.BODY_MAP_HUNT, false));
        assertTrue(FeatureGate.canAccessGameMode(GameModeCatalog.FATE_CUBES, false));
        assertTrue(FeatureGate.canAccessGameMode(GameModeCatalog.COUPLE_BOARD, false));
        // VIP subscriber unlocks everything
        assertTrue(FeatureGate.canAccessGameMode(GameModeCatalog.POSITIONS_ROULETTE, true));
    }

    @Test
    public void connect4DetectsHorizontalWinAndReturnsFourWinningDares() {
        Connect4ActionDto act = new Connect4ActionDto();
        act.id = "a1"; act.shortLabel = "Kiss"; act.tier = 2;
        act.tags = Collections.singletonList("physical_light"); act.fullDare = "Kiss 20s";

        Connect4Engine engine = new Connect4Engine(new Random(1));
        engine.loadActionsAndReset(Collections.singletonList(act), new ConsentFilter());

        // Player A drops cols 0,1,2,3 while Player B drops col 5 repeatedly
        assertFalse(engine.dropInColumn(0).wonMatch); // A
        assertFalse(engine.dropInColumn(5).wonMatch); // B
        assertFalse(engine.dropInColumn(1).wonMatch); // A
        assertFalse(engine.dropInColumn(5).wonMatch); // B
        assertFalse(engine.dropInColumn(2).wonMatch); // A
        assertFalse(engine.dropInColumn(5).wonMatch); // B

        Connect4Engine.DropResult win = engine.dropInColumn(3); // A connects 4 on bottom row!
        assertTrue(win.valid);
        assertTrue(win.wonMatch);
        assertTrue(win.droppedByA);
        assertEquals(4, win.winningLine.size());
        assertEquals(1, engine.getWinsA());
    }

    @Test
    public void propLabFiltersStrictlyToCheckedProps() {
        PropChallengeDto ice = new PropChallengeDto();
        ice.id = "i1"; ice.propKey = "ice"; ice.tier = 3; ice.durationSec = 30;
        ice.tags = Collections.singletonList("physical_light"); ice.text = "Ice tease";

        PropChallengeDto tie = new PropChallengeDto();
        tie.id = "t1"; tie.propKey = "tie"; tie.tier = 4; tie.durationSec = 45;
        tie.tags = Collections.singletonList("restraint_light"); tie.text = "Tie tease";

        PropLabEngine engine = new PropLabEngine(new Random(2));
        engine.loadChallenges(Arrays.asList(ice, tie), new ConsentFilter());
        // By default ice is selected and tie is not
        assertTrue(engine.isPropSelected("ice"));
        assertFalse(engine.isPropSelected("tie"));

        PropChallengeDto drawn = engine.drawNextChallenge();
        assertNotNull(drawn);
        assertEquals("ice", drawn.propKey);
    }

    @Test
    public void positionsForeplayAndDesireLadderProgressCleanly() {
        PositionDto p = new PositionDto();
        p.id = "p1"; p.name = "Lotus"; p.difficulty = 1; p.heat = 4; p.durationSec = 60;
        p.tags = Collections.singletonList("physical_light"); p.setup = "S"; p.tip = "T";
        PositionsEngine posEng = new PositionsEngine(new Random(3));
        posEng.loadPositions(Collections.singletonList(p), new ConsentFilter());
        assertNotNull(posEng.spinRandom());
        assertTrue(posEng.toggleTriedCurrent());
        assertEquals(1, posEng.getTriedCount());

        ForeplayStageDto s1 = new ForeplayStageDto(); s1.step = 1; s1.stageTitle = "S1";
        ForeplayStageDto s2 = new ForeplayStageDto(); s2.step = 2; s2.stageTitle = "S2";
        ForeplayEscalatorEngine fpEng = new ForeplayEscalatorEngine();
        fpEng.loadStages(Arrays.asList(s1, s2));
        assertFalse(fpEng.nextStage());
        assertTrue(fpEng.nextStage());
        assertEquals(1, fpEng.getCompletedSessions());

        LadderStepDto l1 = new LadderStepDto(); l1.step = 1; l1.tags = Collections.singletonList("deep_talk");
        LadderStepDto l2 = new LadderStepDto(); l2.step = 2; l2.tags = Collections.singletonList("deep_talk");
        DesireLadderEngine ladEng = new DesireLadderEngine();
        ladEng.loadSteps(Arrays.asList(l1, l2), new ConsentFilter());
        assertFalse(ladEng.climbNext());
        assertTrue(ladEng.climbNext());
        assertEquals(1, ladEng.getSummitsReached());
    }
}
