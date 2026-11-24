package com.velvet.couplesgame.core.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.velvet.couplesgame.R;
import com.velvet.couplesgame.core.billing.FeatureGate;
import com.velvet.couplesgame.core.i18n.TagDisplayMapper;
import com.velvet.couplesgame.core.model.GameModeCatalog;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.data.local.entity.BoundaryRuleEntity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.Test;

/** Tests for the 12 social games added from comparable couple apps. */
public class Phase16SocialGamesTest {

    private static final String[] MODE_IDS = {
            "conversation_starter", "love_language", "daily_question", "riddle_me",
            "memory_lane", "most_likely_to", "bucket_list", "ldr_sync",
            "forfeit_wheel", "scenario_choice", "hot_seat", "secret_mission"
    };

    private static TaskCard card(String id, int tier, String... tags) {
        return new TaskCard(id, "deck_test", tier, "TRUTH",
                Arrays.asList(tags), 0, "Prompt " + id, false, false);
    }

    @Test
    public void allTwelveModesAreRegisteredConfiguredAndGated() {
        assertEquals(12, MODE_IDS.length);
        for (String id : MODE_IDS) {
            SocialGamesEngine.ModeConfig config = SocialGamesEngine.configFor(id);
            assertNotNull("missing config: " + id, config);
            assertNotNull("missing catalog entry: " + id, GameModeCatalog.fromId(id));
            assertNotZero(GameModeCatalog.fromId(id).navActionId);
            assertTrue("round size must be positive: " + id, config.roundSize > 0);
            assertTrue("tier cap must be 1..4: " + id, config.tierCap >= 1 && config.tierCap <= 4);
        }
        assertEquals(12, SocialGamesEngine.supportedModeIds().size());

        // VIP gating: advanced modes require subscription, warm-ups are free
        assertTrue(FeatureGate.canAccessGameMode(GameModeCatalog.fromId("love_language"), false));
        assertTrue(FeatureGate.canAccessGameMode(GameModeCatalog.fromId("daily_question"), false));
        assertFalse(FeatureGate.canAccessGameMode(GameModeCatalog.fromId("secret_mission"), false));
        assertFalse(FeatureGate.canAccessGameMode(GameModeCatalog.fromId("hot_seat"), false));
        assertTrue(FeatureGate.canAccessGameMode(GameModeCatalog.fromId("secret_mission"), true));
    }

    private static void assertNotZero(int value) {
        assertTrue("id must not be zero", value != 0);
    }

    @Test
    public void interactionStylesAreOpenEndedTaskOrTimedNoFakeAnswers() {
        Set<SocialGamesEngine.Interaction> seen = new HashSet<>();
        for (String id : MODE_IDS) {
            SocialGamesEngine.Interaction interaction = SocialGamesEngine.configFor(id).interaction;
            // No multiple-choice interaction: official cards are open prompts, not questions with answers.
            assertTrue(interaction == SocialGamesEngine.Interaction.TASK
                    || interaction == SocialGamesEngine.Interaction.TIMED);
            seen.add(interaction);
        }
        assertEquals(2, seen.size());
        assertTrue(seen.contains(SocialGamesEngine.Interaction.TASK));
        assertTrue(seen.contains(SocialGamesEngine.Interaction.TIMED));
        assertTrue(SocialGamesEngine.configFor("hot_seat").durationSec > 0);
        assertTrue(SocialGamesEngine.configFor("forfeit_wheel").durationSec > 0);
    }

    @Test
    public void loadPoolRespectsTierCapAndConsentBoundaries() {
        List<TaskCard> pool = Arrays.asList(
                card("t1_1", 1, "deep_talk"),
                card("t2_1", 2, "playful"),
                card("t3_1", 3, "roleplay"),
                card("t4_1", 4, "physical_light")
        );

        // daily_question caps at tier 2 AND prefers deep_talk only -> only t1_1 qualifies
        SocialGamesEngine warm = new SocialGamesEngine("daily_question", new Random(1));
        warm.loadPool(pool, new ConsentFilter(), 4);
        assertEquals(1, warm.getEligibleCount());

        // VIP advanced mode includes tier 4 when unlocked
        SocialGamesEngine adv = new SocialGamesEngine("secret_mission", new Random(1));
        adv.loadPool(pool, new ConsentFilter(), 4);
        assertTrue(adv.getEligibleCount() >= 1);

        // Hard boundary blocks a preferred tag
        BoundaryRuleEntity block = new BoundaryRuleEntity("deep_talk", "HARD_NO", "OK");
        ConsentFilter filter = new ConsentFilter();
        filter.replaceRules(Collections.singletonList(block));
        SocialGamesEngine gated = new SocialGamesEngine("daily_question", new Random(1));
        gated.loadPool(pool, filter, 4);
        assertEquals(0, gated.getEligibleCount()); // only matching card was blocked
    }

    @Test
    public void drawNextAvoidsImmediateRepeatsOnSmallPool() {
        List<TaskCard> pool = new ArrayList<>();
        for (int i = 0; i < 8; i++) pool.add(card("c" + i, 1, "deep_talk"));
        SocialGamesEngine engine = new SocialGamesEngine("daily_question", new Random(42));
        engine.loadPool(pool, new ConsentFilter(), 4);

        String prev = null;
        for (int i = 0; i < 100; i++) {
            TaskCard drawn = engine.drawNext();
            assertNotNull(drawn);
            if (prev != null) {
                assertFalse("immediate repeat at iteration " + i, prev.equals(drawn.id));
            }
            prev = drawn.id;
        }
    }

    @Test
    public void emptyAndFullyBlockedPoolsReturnNullSafely() {
        SocialGamesEngine empty = new SocialGamesEngine("riddle_me", new Random(1));
        empty.loadPool(Collections.emptyList(), new ConsentFilter(), 4);
        assertNull(empty.drawNext());

        // A pool whose only card exceeds the player's tier cap is safely empty
        SocialGamesEngine blocked = new SocialGamesEngine("love_language", new Random(1));
        blocked.loadPool(Collections.singletonList(card("high", 4, "deep_talk")), new ConsentFilter(), 1);
        assertEquals(0, blocked.getEligibleCount());
        assertNull(blocked.drawNext());
    }

    @Test
    public void roundProgressTracksCompletion() {
        SocialGamesEngine engine = new SocialGamesEngine("riddle_me", new Random(7));
        engine.loadPool(Collections.singletonList(card("only", 1, "playful")), new ConsentFilter(), 4);
        assertNotNull(engine.drawNext());
        int target = engine.getConfig().roundSize;
        boolean reached = false;
        for (int i = 0; i < target; i++) reached = engine.markDone();
        assertTrue(reached);
        assertEquals(target, engine.getCompletedInRound());
    }

    /** Real Fragment flow alternates drawNext()/markDone(); completion must survive each draw. */
    @Test
    public void completionSurvivesDrawsUntilRoundReset() {
        SocialGamesEngine engine = new SocialGamesEngine("daily_question", new Random(11));
        engine.loadPool(Arrays.asList(card("a", 1, "deep_talk"), card("b", 1, "deep_talk")),
                new ConsentFilter(), 4);
        int target = engine.getConfig().roundSize;
        assertNotNull(engine.drawNext());
        for (int i = 0; i < target - 1; i++) {
            engine.markDone();
            assertNotNull(engine.drawNext());
            assertTrue("completion reset by drawNext at " + i,
                    engine.getCompletedInRound() == i + 1);
        }
        engine.markDone();
        assertTrue(engine.getCompletedInRound() >= target);
        engine.resetRound();
        assertEquals(0, engine.getCompletedInRound());
    }

    /** Every social mode must map to its own history label, never silently fall back. */
    @Test
    public void allSocialModesMapToTheirOwnHistoryLabel() {
        int fallback = TagDisplayMapper.resolveGameModeRes("COUPLE_BOARD");
        for (String id : MODE_IDS) {
            int label = TagDisplayMapper.resolveGameModeRes(id.toUpperCase());
            assertTrue("history label fell back for " + id, label != fallback);
        }
        assertEquals(R.string.mode_daily_question,
                TagDisplayMapper.resolveGameModeRes("DAILY_QUESTION"));
        assertEquals(R.string.mode_secret_mission,
                TagDisplayMapper.resolveGameModeRes("SECRET_MISSION"));
    }
}
