package com.velvet.couplesgame.core.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.velvet.couplesgame.core.i18n.AssetContentLoader.AuctionContractDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.BodyMapZoneDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.SimplePromptDto;
import com.velvet.couplesgame.data.local.entity.BoundaryRuleEntity;
import com.velvet.couplesgame.ui.auction.DesireAuctionEngine;
import com.velvet.couplesgame.ui.blackjack.BlackjackEngine;
import com.velvet.couplesgame.ui.bodymap.BodyMapEngine;
import com.velvet.couplesgame.ui.bomb.VelvetBombEngine;
import com.velvet.couplesgame.ui.heartbeat.HeartbeatEngine;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import org.junit.Test;

public class Phase9IntimateModesTest {

    @Test
    public void heartbeatEngineFiltersAndTracksSyncsAndForfeits() {
        SimplePromptDto p1 = makePrompt("h1", 1, "eye_contact");
        SimplePromptDto p2 = makePrompt("h2", 4, "bite_lick");

        ConsentFilter filter = new ConsentFilter();
        filter.replaceRules(Collections.singletonList(new BoundaryRuleEntity("bite_lick", "HARD_NO", "YES")));

        HeartbeatEngine engine = new HeartbeatEngine(new Random(2));
        engine.loadPrompts(Arrays.asList(p1, p2), filter, 4);

        assertEquals("h1", engine.drawNextPrompt().id);
        engine.recordSyncSuccess();
        engine.recordForfeit(true);
        assertEquals(1, engine.getSyncedRounds());
        assertEquals(1, engine.getForfeitsA());
    }

    @Test
    public void blackjackComputesAceReductionAndShowdownChipTransfer() {
        int scoreWithTwoAces = BlackjackEngine.computeScore(Arrays.asList(
                new BlackjackEngine.PlayingCard("A", "♥", 11),
                new BlackjackEngine.PlayingCard("A", "♠", 11),
                new BlackjackEngine.PlayingCard("9", "♦", 9)
        ));
        assertEquals(21, scoreWithTwoAces);

        BlackjackEngine engine = new BlackjackEngine(new Random(7));
        engine.resetMatch();
        assertEquals(BlackjackEngine.Phase.TURN_A, engine.getPhase());
        engine.standCurrentPlayer();
        assertEquals(BlackjackEngine.Phase.TURN_B, engine.getPhase());
        engine.standCurrentPlayer();
        assertEquals(BlackjackEngine.Phase.SHOWDOWN, engine.getPhase());
        assertEquals(BlackjackEngine.STARTING_CHIPS * 2, engine.getChipsA() + engine.getChipsB());
    }

    @Test
    public void blackjackFinalScoreCountsMultipleAcesAsElevenOrOneAsNeeded() {
        assertEquals(13, BlackjackEngine.computeScore(Arrays.asList(
                new BlackjackEngine.PlayingCard("A", "♥", 11),
                new BlackjackEngine.PlayingCard("A", "♠", 11),
                new BlackjackEngine.PlayingCard("9", "♦", 9),
                new BlackjackEngine.PlayingCard("2", "♣", 2)
        )));
        assertEquals(22, BlackjackEngine.computeScore(Arrays.asList(
                new BlackjackEngine.PlayingCard("10", "♥", 10),
                new BlackjackEngine.PlayingCard("9", "♠", 9),
                new BlackjackEngine.PlayingCard("A", "♦", 11),
                new BlackjackEngine.PlayingCard("2", "♣", 2)
        )));
    }

    @Test
    public void velvetBombArmsWithinFuseBoundsPassesAndDetonatesOnHolder() {
        VelvetBombEngine engine = new VelvetBombEngine(new Random(11));
        engine.loadRelays(Collections.singletonList(makePrompt("b1", 2, "playful")), new ConsentFilter(), 2);

        assertNotNull(engine.armNewRound());
        assertTrue(engine.isArmed());
        assertTrue(engine.getFuseDurationMs() >= VelvetBombEngine.MIN_FUSE_MS);
        assertTrue(engine.getFuseDurationMs() <= VelvetBombEngine.MAX_FUSE_MS);

        assertTrue(engine.isHolderPlayerA());
        engine.passBomb();
        assertFalse(engine.isHolderPlayerA());
        assertEquals(1, engine.getPassCount());

        boolean victimIsA = engine.detonate();
        assertFalse(victimIsA);
        assertFalse(engine.isArmed());
        assertEquals(1, engine.getExplosionsB());
    }

    @Test
    public void bodyMapHidesHotSpotsAndDetectsDiscovery() {
        BodyMapZoneDto z1 = makeZone("z1", 1, "physical_light");
        BodyMapZoneDto z2 = makeZone("z2", 2, "physical_light");
        BodyMapZoneDto z3 = makeZone("z3", 4, "restraint_light");

        BodyMapEngine engine = new BodyMapEngine();
        engine.loadZones(Arrays.asList(z1, z2, z3), new ConsentFilter(), 2);
        assertEquals(2, engine.getZones().size()); // z3 gated out by tier=2

        assertFalse(engine.canBeginExplore());
        assertTrue(engine.toggleSecretHotSpot("z2"));
        assertTrue(engine.canBeginExplore());

        engine.beginExplore();
        assertEquals(BodyMapEngine.Phase.EXPLORE, engine.getPhase());

        BodyMapEngine.TapResult miss = engine.exploreZone("z1");
        assertNotNull(miss);
        assertFalse(miss.wasHotSpot);
        assertFalse(miss.allHotSpotsFound);

        BodyMapEngine.TapResult hit = engine.exploreZone("z2");
        assertNotNull(hit);
        assertTrue(hit.wasHotSpot);
        assertTrue(hit.allHotSpotsFound);
    }

    @Test
    public void desireAuctionTracksBiddingFoldPayoutAndBailout() {
        AuctionContractDto c = new AuctionContractDto();
        c.id = "a1"; c.tier = 2; c.durationSec = 30;
        c.tags = Collections.singletonList("physical_light");
        c.title = "Contract"; c.text = "Text";

        DesireAuctionEngine engine = new DesireAuctionEngine(new Random(9));
        engine.loadContracts(Collections.singletonList(c), new ConsentFilter(), 2);

        assertTrue(engine.isBidderTurnA());
        assertTrue(engine.placeBidIncrement(15)); // A bids 15
        assertFalse(engine.isBidderTurnA());
        assertTrue(engine.placeBidIncrement(15)); // B bids 30
        assertTrue(engine.isBidderTurnA());

        boolean winnerIsA = engine.foldCurrentBidder(); // A folds -> B wins for 30
        assertFalse(winnerIsA);
        assertEquals(DesireAuctionEngine.BidPhase.SOLD, engine.getPhase());
        assertEquals(100, engine.getChipsA());
        assertEquals(70, engine.getChipsB());
        assertEquals(1, engine.getContractsWonB());

        engine.grantBailoutChips(false);
        assertEquals(100, engine.getChipsB());
    }

    private SimplePromptDto makePrompt(String id, int tier, String tag) {
        SimplePromptDto d = new SimplePromptDto();
        d.id = id; d.tier = tier; d.tags = Collections.singletonList(tag); d.text = "Test";
        return d;
    }

    private BodyMapZoneDto makeZone(String id, int tier, String tag) {
        BodyMapZoneDto z = new BodyMapZoneDto();
        z.id = id; z.label = id; z.nx = 0.5f; z.ny = 0.5f; z.tier = tier;
        z.tags = Collections.singletonList(tag); z.normalAction = "N"; z.hotAction = "H"; z.durationSec = 20;
        return z;
    }
}
