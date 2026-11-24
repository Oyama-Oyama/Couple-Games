package com.velvet.couplesgame.core.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.velvet.couplesgame.core.billing.EntitlementManager;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.ForeplayStageDto;
import com.velvet.couplesgame.data.local.entity.BoundaryRuleEntity;
import com.velvet.couplesgame.ui.blackjack.BlackjackEngine;
import com.velvet.couplesgame.ui.foreplay.ForeplayEscalatorEngine;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import org.junit.Test;

public class Phase11FinalAuditTest {

    @Test
    public void vipUnlockAutoPromotesHeatTierFromFreeDefaultWithoutOverridingUserChoice() {
        assertEquals(4, EntitlementManager.resolveTierAfterVipUnlock(2, false));
        assertEquals(4, EntitlementManager.resolveTierAfterVipUnlock(1, false));
        assertEquals(2, EntitlementManager.resolveTierAfterVipUnlock(2, true));
    }

    @Test
    public void foreplayEscalatorStrictlyRespectsConsentFilterHardNo() {
        ForeplayStageDto s1 = new ForeplayStageDto();
        s1.step = 1; s1.stageTitle = "Undress"; s1.tags = Collections.singletonList("clothing_dare");
        ForeplayStageDto s2 = new ForeplayStageDto();
        s2.step = 2; s2.stageTitle = "Massage"; s2.tags = Collections.singletonList("physical_light");

        ConsentFilter filter = new ConsentFilter();
        filter.replaceRules(Collections.singletonList(
                new BoundaryRuleEntity("clothing_dare", "YES", "HARD_NO")
        ));

        ForeplayEscalatorEngine engine = new ForeplayEscalatorEngine();
        engine.loadStages(Arrays.asList(s1, s2), filter);
        assertEquals(1, engine.getTotalStages());
        assertEquals("Massage", engine.getCurrentStage().stageTitle);
    }

    @Test
    public void blackjackConservesTotalChipsAcrossDoubleDownAndBankruptcy() {
        BlackjackEngine engine = new BlackjackEngine(new Random(12345));
        engine.resetMatch();
        final int totalPool = BlackjackEngine.STARTING_CHIPS * 2;

        for (int hand = 0; hand < 30; hand++) {
            if (engine.isMatchOver()) engine.resetMatch();
            if (engine.canDoubleDown()) {
                engine.doubleDownCurrentPlayer();
            } else {
                engine.standCurrentPlayer();
            }
            if (engine.getPhase() == BlackjackEngine.Phase.TURN_B) {
                engine.standCurrentPlayer();
            }
            assertEquals(BlackjackEngine.Phase.SHOWDOWN, engine.getPhase());
            assertEquals(totalPool, engine.getChipsA() + engine.getChipsB());
            assertTrue(engine.getChipsA() >= 0 && engine.getChipsB() >= 0);
            engine.startNewHand();
        }
    }
}
