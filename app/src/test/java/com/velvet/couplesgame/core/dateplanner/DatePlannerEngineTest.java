package com.velvet.couplesgame.core.dateplanner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.velvet.couplesgame.core.model.GameModeCatalog;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/**
 * Covers the Date Planner negotiation state machine, the tier ceiling and the
 * mutual-match logic that turns two independent votes into one shared plan.
 */
public class DatePlannerEngineTest {

    private static DatePlannerEngine.Activity activity(String id, int tier, String category, int durationMin) {
        return new DatePlannerEngine.Activity(
                id, category, tier, durationMin,
                Collections.singletonList("deep_talk"),
                "Title " + id,
                "Detail " + id
        );
    }

    private static List<DatePlannerEngine.Activity> pool() {
        List<DatePlannerEngine.Activity> list = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            list.add(activity("dp_test_" + i, (i % 4) + 1, "dining", 60 + i));
        }
        return list;
    }

    @Test
    public void emptyPoolStartsInEmptyPhase() {
        DatePlannerEngine engine = new DatePlannerEngine();
        engine.loadPool(new ArrayList<>());
        assertEquals(DatePlannerEngine.Phase.EMPTY, engine.getPhase());
        assertNull(engine.getCurrentActivity());
    }

    @Test
    public void shortlistRespectsTierCeiling() {
        DatePlannerEngine engine = new DatePlannerEngine();
        engine.loadPool(pool());
        engine.buildShortlist(4, 2);
        assertEquals(4, engine.getShortlistSize());
        for (DatePlannerEngine.Activity a : engine.getShortlist()) {
            assertTrue("activity " + a.id + " tier " + a.tier + " exceeds ceiling", a.tier <= 2);
        }
    }

    @Test
    public void tierCeilingFallsBackWhenPoolIsTooStrict() {
        DatePlannerEngine engine = new DatePlannerEngine();
        List<DatePlannerEngine.Activity> highTier = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            highTier.add(activity("dp_high_" + i, 4, "ritual", 120));
        }
        engine.loadPool(highTier);
        engine.buildShortlist(3, 1);
        assertEquals(3, engine.getShortlistSize());
        assertEquals(DatePlannerEngine.Phase.PICK_A, engine.getPhase());
    }

    @Test
    public void partnerAPicksAreNotVisibleToPartnerB() {
        DatePlannerEngine engine = new DatePlannerEngine();
        engine.loadPool(pool());
        engine.buildShortlist(3, 4);

        String firstId = engine.getCurrentActivity().id;
        engine.recordPickA(true);
        assertEquals(1, engine.getPickedCountA());
        assertEquals(0, engine.getPickedCountB());

        engine.handOffToPartnerB();
        assertEquals(DatePlannerEngine.Phase.PICK_B, engine.getPhase());
        assertEquals(0, engine.getCursor());
        // The same shortlist is reviewed from the start, in the same order.
        assertEquals(firstId, engine.getCurrentActivity().id);
    }

    @Test
    public void mutualPicksBecomeMatches() {
        DatePlannerEngine engine = new DatePlannerEngine();
        engine.loadPool(pool());
        engine.buildShortlist(3, 4);

        engine.recordPickA(true);  // idx 0 liked
        engine.recordPickA(false); // idx 1 skipped
        engine.recordPickA(true);  // idx 2 liked
        engine.handOffToPartnerB();

        engine.recordPickB(false); // idx 0 skipped -> no match
        engine.recordPickB(true);  // idx 1 liked, but A skipped it
        engine.recordPickB(true);  // idx 2 liked -> match

        assertEquals(DatePlannerEngine.Phase.RESULT, engine.getPhase());
        List<DatePlannerEngine.Activity> matches = engine.getMutualMatches();
        assertEquals(1, matches.size());
        assertEquals(engine.getShortlist().get(2).id, matches.get(0).id);
    }

    @Test
    public void noMutualPicksYieldsEmptyMatchList() {
        DatePlannerEngine engine = new DatePlannerEngine();
        engine.loadPool(pool());
        engine.buildShortlist(2, 4);

        engine.recordPickA(true);
        engine.recordPickA(true);
        engine.handOffToPartnerB();
        engine.recordPickB(false);
        engine.recordPickB(false);

        assertEquals(DatePlannerEngine.Phase.RESULT, engine.getPhase());
        assertTrue(engine.getMutualMatches().isEmpty());
    }

    @Test
    public void votesAreRejectedOutsideTheirPhase() {
        DatePlannerEngine engine = new DatePlannerEngine();
        engine.loadPool(pool());
        engine.buildShortlist(3, 4);

        // Partner B cannot vote before the handoff.
        assertFalse(engine.recordPickB(true));
        engine.handOffToPartnerB();
        assertFalse(engine.recordPickA(true));
    }

    @Test
    public void resetSessionRestartsFromPlayerA() {
        DatePlannerEngine engine = new DatePlannerEngine();
        engine.loadPool(pool());
        engine.buildShortlist(3, 4);
        engine.recordPickA(true);
        engine.handOffToPartnerB();
        engine.recordPickB(true);

        engine.resetSession();
        assertEquals(DatePlannerEngine.Phase.PICK_A, engine.getPhase());
        assertEquals(0, engine.getCursor());
        assertEquals(0, engine.getPickedCountA());
        assertEquals(0, engine.getPickedCountB());
        assertTrue(engine.getMutualMatches().isEmpty());
    }

    @Test
    public void findByIdLocatesPoolEntries() {
        DatePlannerEngine engine = new DatePlannerEngine();
        engine.loadPool(pool());
        DatePlannerEngine.Activity found = engine.findById("dp_test_3");
        assertNotNull(found);
        assertEquals("dp_test_3", found.id);
        assertNull(engine.findById("does_not_exist"));
    }

    @Test
    public void totalDurationSumsActivities() {
        DatePlannerEngine engine = new DatePlannerEngine();
        engine.loadPool(pool());
        List<DatePlannerEngine.Activity> chosen = Arrays.asList(
                engine.findById("dp_test_1"),
                engine.findById("dp_test_2")
        );
        assertEquals(61 + 62, engine.getTotalDurationMin(chosen));
    }

    @Test
    public void datePlannerIsRegisteredAsFreeGameMode() {
        GameModeCatalog mode = GameModeCatalog.fromId("date_planner");
        assertNotNull(mode);
        assertEquals(2, mode.heatRating);
        assertFalse(mode.vipRequired);
        assertEquals(com.velvet.couplesgame.R.string.mode_date_planner, mode.titleRes);
    }
}
