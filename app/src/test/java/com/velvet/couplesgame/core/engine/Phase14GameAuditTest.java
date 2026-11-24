package com.velvet.couplesgame.core.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.velvet.couplesgame.core.billing.EntitlementManager;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.CubeFaceDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.CubeModifierDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.FateCubesBundleDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.ScratchCardDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.SimplePromptDto;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.ui.blindmatch.BlindMatchSessionManager;
import com.velvet.couplesgame.ui.bomb.VelvetBombEngine;
import com.velvet.couplesgame.ui.fatecubes.FateCubesEngine;
import com.velvet.couplesgame.ui.scratch.ScratchAdventureEngine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.Test;

public class Phase14GameAuditTest {

    @Test
    public void velvetBombDealsFreshPromptOnEveryPass() {
        List<SimplePromptDto> relays = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            SimplePromptDto p = new SimplePromptDto();
            p.id = "bm_" + i;
            p.tier = 2;
            p.tags = Collections.singletonList("physical_light");
            p.text = "Relay " + i;
            relays.add(p);
        }

        VelvetBombEngine engine = new VelvetBombEngine(new Random(42));
        engine.loadRelays(relays, new ConsentFilter(), 4);

        SimplePromptDto first = engine.armNewRound();
        assertNotNull(first);
        String prevId = first.id;

        for (int pass = 0; pass < 12; pass++) {
            SimplePromptDto next = engine.passBomb();
            assertNotNull(next);
            assertNotEquals("Bomb prompt did not change on pass " + pass, prevId, next.id);
            assertEquals(next.id, engine.getActiveRelay().id);
            prevId = next.id;
        }
    }

    @Test
    public void scratchAdventureNeverRepeatsBackToBackAcrossRecycles() {
        List<ScratchCardDto> cards = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            ScratchCardDto c = new ScratchCardDto();
            c.id = "sc_" + i;
            c.category = "DARE";
            c.tier = 2;
            c.durationSec = 30;
            c.tags = Collections.singletonList("physical_light");
            c.title = "Title " + i;
            c.text = "Body " + i;
            cards.add(c);
        }

        ScratchAdventureEngine engine = new ScratchAdventureEngine(new Random(17));
        engine.loadCards(cards, new ConsentFilter(), 4);

        Set<String> firstCycle = new HashSet<>();
        String prevId = null;
        for (int i = 0; i < 6; i++) {
            ScratchCardDto c = engine.drawNextCard();
            assertNotNull(c);
            assertTrue("Duplicate inside first cycle: " + c.id, firstCycle.add(c.id));
            prevId = c.id;
        }

        // Across next 12 draws after recycle, never repeat immediately back-to-back
        for (int i = 0; i < 12; i++) {
            ScratchCardDto c = engine.drawNextCard();
            assertNotNull(c);
            assertNotEquals("Immediate repeat after recycle: " + c.id, prevId, c.id);
            prevId = c.id;
        }
    }

    @Test
    public void blindMatchShuffledRoundsDeliverFreshUnseenCardsAcrossRestarts() {
        List<TaskCard> pool = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            pool.add(new TaskCard(
                    "dm_" + i, "deck_desire_match", (i % 4) + 1, "DESIRE_MATCH",
                    Collections.singletonList("indoor"), 0, "Desire " + i, false, false
            ));
        }

        BlindMatchSessionManager mgr = new BlindMatchSessionManager(new Random(99));
        mgr.startShuffledRound(pool, new ConsentFilter(), 4, 10);
        assertEquals(10, mgr.getTotalCount());

        Set<String> round1Ids = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            round1Ids.add(mgr.getCurrentCard().id);
            mgr.recordSwipe(true);
        }
        assertEquals(10, round1Ids.size());

        // Starting round 2 MUST pick the 10 completely unseen cards!
        mgr.startShuffledRound(pool, new ConsentFilter(), 4, 10);
        assertEquals(10, mgr.getTotalCount());
        Set<String> round2Ids = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            round2Ids.add(mgr.getCurrentCard().id);
            mgr.recordSwipe(true);
        }
        assertEquals(10, round2Ids.size());
        round2Ids.retainAll(round1Ids);
        assertTrue("Expected zero overlap between round 1 and round 2 when 20 cards exist", round2Ids.isEmpty());
    }

    @Test
    public void fateCubesAvoidsRecentFaceRepeats() {
        FateCubesBundleDto bundle = new FateCubesBundleDto();
        List<CubeFaceDto> actions = new ArrayList<>();
        List<CubeFaceDto> spots = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            CubeFaceDto a = new CubeFaceDto();
            a.id = "a_" + i; a.label = "A" + i; a.icon = "🔥"; a.tier = 2; a.tags = Collections.singletonList("bite_lick");
            actions.add(a);
            CubeFaceDto s = new CubeFaceDto();
            s.id = "s_" + i; s.label = "S" + i; s.icon = "♥"; s.tier = 2; s.tags = Collections.singletonList("physical_light");
            spots.add(s);
        }
        CubeModifierDto mod = new CubeModifierDto();
        mod.id = "m1"; mod.label = "Mod"; mod.durationSec = 25; mod.tier = 2; mod.tags = Collections.singletonList("playful");
        bundle.actions = actions;
        bundle.spots = spots;
        bundle.modifiers = Collections.singletonList(mod);

        FateCubesEngine engine = new FateCubesEngine(new Random(7));
        engine.loadBundle(bundle, new ConsentFilter(), 4);

        // First 5 action rolls & 5 spot rolls must all be distinct (cooldown = 4)
        Set<String> rolledActions = new HashSet<>();
        Set<String> rolledSpots = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            rolledActions.add(engine.rollAction().id);
            rolledSpots.add(engine.rollSpot().id);
        }
        assertEquals(5, rolledActions.size());
        assertEquals(5, rolledSpots.size());
    }

    @Test
    public void devVipPreviewFlagUnlocksPremiumOnlyWhenEnabled() {
        assertFalse(EntitlementManager.resolveEffectivePremium(false, false, false));
        assertTrue(EntitlementManager.resolveEffectivePremium(false, false, true));
        assertTrue(EntitlementManager.resolveEffectivePremium(true, false, false));
        assertTrue(EntitlementManager.resolveEffectivePremium(false, true, false));
    }
}
