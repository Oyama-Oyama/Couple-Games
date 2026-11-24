package com.velvet.couplesgame.core.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.data.local.entity.BoundaryRuleEntity;
import com.velvet.couplesgame.ui.blindmatch.BlindMatchSessionManager;
import com.velvet.couplesgame.ui.chess.BoardTileType;
import com.velvet.couplesgame.ui.chess.CoupleBoardEngine;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class Phase2GameplayTest {

    @Test
    public void blindMatchRevealsOnlyMutualLikesAndRespectsConsentFilter() {
        ConsentFilter filter = new ConsentFilter();
        filter.replaceRules(Collections.singletonList(
                new BoundaryRuleEntity("blindfold", "YES", "HARD_NO")
        ));

        List<TaskCard> cards = Arrays.asList(
                new TaskCard("m1", "dm", 1, "DESIRE_MATCH", Collections.singletonList("deep_talk"), 0, "Candlelight dinner", false, false),
                new TaskCard("m2", "dm", 2, "DESIRE_MATCH", Collections.singletonList("playful"), 0, "Slow dance", false, false),
                new TaskCard("m3", "dm", 4, "DESIRE_MATCH", Collections.singletonList("blindfold"), 0, "Blocked blindfold", false, false)
        );

        BlindMatchSessionManager mgr = new BlindMatchSessionManager();
        mgr.startSession(cards, filter);

        assertEquals(2, mgr.getTotalCount()); // m3 blocked by ConsentFilter
        assertEquals(BlindMatchSessionManager.Phase.PLAYER_A, mgr.getPhase());

        // Player A likes m1, passes m2
        assertEquals(BlindMatchSessionManager.Phase.PLAYER_A, mgr.recordSwipe(true));
        assertEquals(BlindMatchSessionManager.Phase.HANDOFF_TO_B, mgr.recordSwipe(false));

        // Mutual matches hidden before reveal
        assertTrue(mgr.getMutualMatches().isEmpty());

        mgr.beginPlayerBTurn();
        assertEquals(BlindMatchSessionManager.Phase.PLAYER_B, mgr.getPhase());

        // Player B likes both m1 and m2 -> only m1 is a mutual match
        assertEquals(BlindMatchSessionManager.Phase.PLAYER_B, mgr.recordSwipe(true));
        assertEquals(BlindMatchSessionManager.Phase.REVEAL, mgr.recordSwipe(true));

        List<TaskCard> mutual = mgr.getMutualMatches();
        assertEquals(1, mutual.size());
        assertEquals("m1", mutual.get(0).id);
    }

    @Test
    public void coupleBoardHandlesSpecialEventsAndVictory() {
        CoupleBoardEngine engine = new CoupleBoardEngine();
        assertEquals(36, engine.getTiles().size());
        assertEquals(BoardTileType.START, engine.getTiles().get(0).type);
        assertEquals(BoardTileType.FINISH, engine.getTiles().get(35).type);

        // Tile 4 is BOOST_FORWARD (+2 -> lands on 6)
        CoupleBoardEngine.MoveResult r1 = engine.advanceByDice(4);
        assertTrue(r1.movedPlayerA);
        assertEquals(4, r1.landedIndex);
        assertEquals(6, r1.finalIndex);
        assertEquals(6, engine.getPlayerAPos());
        assertFalse(engine.isPlayerATurn()); // Turn switches to Player B

        // Player B rolls 7 (via test advance) -> EXTRA_ROLL tile 7 keeps Player B's turn
        CoupleBoardEngine.MoveResult r2 = engine.advanceByDice(7);
        assertFalse(r2.movedPlayerA);
        assertTrue(r2.grantExtraRoll);
        assertFalse(engine.isPlayerATurn()); // Still Player B's turn

        // Player B reaches finish (clamped to 35)
        CoupleBoardEngine.MoveResult win = engine.advanceByDice(40);
        assertTrue(win.wonGame);
        assertEquals(35, engine.getPlayerBPos());
    }
}
