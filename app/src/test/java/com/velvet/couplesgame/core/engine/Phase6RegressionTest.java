package com.velvet.couplesgame.core.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.velvet.couplesgame.core.billing.PlayBillingBridge;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.GeneratorBundleDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.TemplateDto;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.ui.blindmatch.BlindMatchSessionManager;
import com.velvet.couplesgame.ui.chess.CoupleBoardEngine;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.Test;

public class Phase6RegressionTest {

    @Test
    public void subscriptionPurchaseActivatesImmediatelyWithoutWaitingForFullQuery() {
        // Bug 1 regression: onPurchasesUpdated passes isFullSubscriptionQuery = false
        assertTrue(PlayBillingBridge.resolveNextSubscriptionState(false, true, false));
        // Unrelated INAPP purchase batch must not wipe an existing active subscription
        assertTrue(PlayBillingBridge.resolveNextSubscriptionState(true, false, false));
        // Full SUBS query with no active subscriptions properly deactivates expired subscription
        assertFalse(PlayBillingBridge.resolveNextSubscriptionState(true, false, true));
        // Full SUBS query with active subscription stays active
        assertTrue(PlayBillingBridge.resolveNextSubscriptionState(false, true, true));
    }

    @Test
    public void taskEngineRecyclesHandcraftedDeckEvenWhenProceduralGeneratorIsAttached() {
        // Bug 4 regression: deterministic generator + deterministic RNG that always picks handcrafted (nextInt(100) >= 30)
        TemplateDto tpl = new TemplateDto();
        tpl.id = "t1";
        tpl.tier = 1;
        tpl.type = "TRUTH";
        tpl.tags = Collections.singletonList("deep_talk");
        tpl.durationSec = 0;
        tpl.pattern = "Generated {slot}";

        Map<String, List<String>> slots = new HashMap<>();
        slots.put("slot", Collections.singletonList("prompt"));
        GeneratorBundleDto bundle = new GeneratorBundleDto();
        bundle.templates = Collections.singletonList(tpl);
        bundle.slots = slots;

        ProceduralTaskGenerator stubGen = new ProceduralTaskGenerator(null) {
            @Override
            public TaskCard generate(int maxTier, String requiredType, ConsentFilter filter) {
                return generateFromBundle(bundle, maxTier, requiredType, filter);
            }
        };

        Random alwaysHandcraftedRng = new Random() {
            @Override
            public int nextInt(int bound) {
                return bound == 100 ? 90 : 0; // never triggers 30% procedural mix
            }
        };

        TaskEngine engine = new TaskEngine(new ConsentFilter(), stubGen, alwaysHandcraftedRng);
        List<TaskCard> pool = Collections.singletonList(
                new TaskCard("curated_1", "d1", 1, "TRUTH", Collections.singletonList("deep_talk"), 0, "Curated card", false, false)
        );

        TaskCard first = engine.drawNext(pool, null, 2, null, true);
        TaskCard second = engine.drawNext(pool, null, 2, null, true);

        assertNotNull(first);
        assertNotNull(second);
        assertEquals("curated_1", first.id);
        assertEquals("curated_1", second.id);
        assertFalse(second.isProcedural);
    }

    @Test
    public void blindMatchEnforcesMaxTierVipGating() {
        // Bug 5 regression: free tier (maxTier=2) must exclude Tier 3 & Tier 4 Desire Match cards
        List<TaskCard> cards = Arrays.asList(
                new TaskCard("dm_1", "dm", 1, "DESIRE_MATCH", Collections.singletonList("deep_talk"), 0, "T1", false, false),
                new TaskCard("dm_2", "dm", 2, "DESIRE_MATCH", Collections.singletonList("playful"), 0, "T2", false, false),
                new TaskCard("dm_3", "dm", 3, "DESIRE_MATCH", Collections.singletonList("physical_light"), 0, "T3", false, false),
                new TaskCard("dm_4", "dm", 4, "DESIRE_MATCH", Collections.singletonList("blindfold"), 0, "T4", false, false)
        );

        BlindMatchSessionManager freeMgr = new BlindMatchSessionManager();
        freeMgr.startSession(cards, new ConsentFilter(), 2);
        assertEquals(2, freeMgr.getTotalCount());

        BlindMatchSessionManager vipMgr = new BlindMatchSessionManager();
        vipMgr.startSession(cards, new ConsentFilter(), 4);
        assertEquals(4, vipMgr.getTotalCount());
    }

    @Test
    public void coupleBoardInitializesLocalizedDefaultsWithoutOverwritingCustomPrizes() {
        CoupleBoardEngine engine = new CoupleBoardEngine();
        engine.initDefaultRewardsIfUnmodified("烛光按摩", "床上早餐");
        assertEquals("烛光按摩", engine.getPlayerAReward());
        assertEquals("床上早餐", engine.getPlayerBReward());

        engine.setRewards("Custom Reward A", "Custom Reward B");
        engine.initDefaultRewardsIfUnmodified("Should Not Overwrite", "Should Not Overwrite");
        assertEquals("Custom Reward A", engine.getPlayerAReward());
        assertEquals("Custom Reward B", engine.getPlayerBReward());
    }
}
