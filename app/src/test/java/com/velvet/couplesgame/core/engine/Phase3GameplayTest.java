package com.velvet.couplesgame.core.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.velvet.couplesgame.core.model.DrawWord;
import com.velvet.couplesgame.data.local.entity.BoundaryRuleEntity;
import com.velvet.couplesgame.ui.drawguess.DrawGuessSessionManager;
import com.velvet.couplesgame.ui.minesweeper.CoupleMinesEngine;
import com.velvet.couplesgame.ui.wheel.LuckyWheelEngine;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.Test;

public class Phase3GameplayTest {

    @Test
    public void luckyWheelResolvesClockwiseRotationUnderTopPointer() {
        // 8 slices -> 45 deg each. Rotation 0 -> slice 0
        assertEquals(0, LuckyWheelEngine.resolveSelectedSectorIndex(0f, 8));
        // Rotating clockwise 45 deg brings slice 7 (last slice) under 12 o'clock pointer
        assertEquals(7, LuckyWheelEngine.resolveSelectedSectorIndex(45f, 8));
        // Rotating clockwise 90 deg brings slice 6 under pointer
        assertEquals(6, LuckyWheelEngine.resolveSelectedSectorIndex(90f, 8));
        // Full 360 + 10 deg stays inside slice 7
        assertEquals(7, LuckyWheelEngine.resolveSelectedSectorIndex(370f, 8));
    }

    @Test
    public void coupleMinesTriggersHeartMineWithoutEndingAndFloodFillsZeros() {
        CoupleMinesEngine engine = new CoupleMinesEngine(2, new Random(42));
        engine.setDeterministicMines(new int[][]{{5, 5}, {5, 4}});

        // Flag prevents accidental reveal
        assertTrue(engine.toggleFlag(5, 5));
        assertEquals(CoupleMinesEngine.RevealOutcome.IGNORED, engine.revealCell(5, 5).type);
        assertTrue(engine.toggleFlag(5, 5)); // unflag

        // Reveal (0, 0) which has 0 adjacent mines -> flood fills most safe tiles
        CoupleMinesEngine.MoveOutcome safe = engine.revealCell(0, 0);
        assertEquals(CoupleMinesEngine.RevealOutcome.BOARD_CLEARED, safe.type);
        assertEquals(34, safe.newlyRevealedCount);

        // Fresh deterministic board to test Heart Mine trigger + turn alternation
        CoupleMinesEngine mineBoard = new CoupleMinesEngine(1, new Random(1));
        mineBoard.setDeterministicMines(new int[][]{{0, 1}});
        assertTrue(mineBoard.isPlayerATurn());

        CoupleMinesEngine.MoveOutcome hit = mineBoard.revealCell(0, 1);
        assertEquals(CoupleMinesEngine.RevealOutcome.HEART_MINE_TRIGGERED, hit.type);
        assertTrue(hit.actingPlayerA);
        assertEquals(1, mineBoard.getPlayerAMinesHit());
        assertFalse(mineBoard.isPlayerATurn()); // Switched to Player B
    }

    @Test
    public void drawGuessFiltersConsentBoundariesAndAlternatesDrawers() {
        ConsentFilter filter = new ConsentFilter();
        filter.replaceRules(Collections.singletonList(
                new BoundaryRuleEntity("blindfold", "HARD_NO", "YES")
        ));

        List<DrawWord> words = Arrays.asList(
                new DrawWord("Candlelight Dinner", 1, Collections.singletonList("playful")),
                new DrawWord("Silk Blindfold", 4, Collections.singletonList("blindfold"))
        );

        DrawGuessSessionManager mgr = new DrawGuessSessionManager(new Random(7));
        mgr.loadWords(words, filter, 4);

        DrawWord drawn = mgr.drawNextWord();
        assertNotNull(drawn);
        assertEquals("Candlelight Dinner", drawn.word);
        assertTrue(mgr.isPlayerADrawing());

        mgr.markRoundGuessed();
        assertEquals(1, mgr.getScoreA());
        assertEquals(1, mgr.getScoreB());
        assertFalse(mgr.isPlayerADrawing());
    }
}
