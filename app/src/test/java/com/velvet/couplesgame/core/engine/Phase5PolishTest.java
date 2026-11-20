package com.velvet.couplesgame.core.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.velvet.couplesgame.core.billing.PricingCatalog;
import com.velvet.couplesgame.core.model.PlayerProfile;
import com.velvet.couplesgame.core.profile.CoupleProfileManager;
import com.velvet.couplesgame.data.local.entity.BoundaryRuleEntity;
import com.velvet.couplesgame.ui.consent.ConsentFragment;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class Phase5PolishTest {

    @Test
    public void pricingLadderMaintainsRationalAnchorHierarchy() {
        assertTrue(PricingCatalog.FALLBACK_WEEKLY_USD < PricingCatalog.FALLBACK_ANNUAL_USD);
        assertTrue(PricingCatalog.FALLBACK_ANNUAL_USD < PricingCatalog.FALLBACK_LIFETIME_USD);
    }

    @Test
    public void coupleProfileSanitizesNamesAndComputesTokenInitials() {
        assertEquals("Alex", CoupleProfileManager.sanitizeName("   ", "Alex"));
        assertEquals("Maya", CoupleProfileManager.sanitizeName("  Maya  ", "Partner"));
        assertEquals(18, CoupleProfileManager.sanitizeName("AVeryLongPartnerNameExceedingLimit", "A").length());

        PlayerProfile p = new PlayerProfile("maya", "She / Her", 3);
        assertEquals("M", p.getInitial());
    }

    @Test
    public void privateBoundarySummaryCountsShieldsAnonymously() {
        List<BoundaryRuleEntity> rules = Arrays.asList(
                new BoundaryRuleEntity("public_space", "HARD_NO", "YES"),
                new BoundaryRuleEntity("blindfold", "YES", "HARD_NO"),
                new BoundaryRuleEntity("roleplay", "SOFT_LIMIT", "YES"),
                new BoundaryRuleEntity("deep_talk", "YES", "YES")
        );

        assertEquals(2, ConsentFragment.countBlockedRules(rules));
        assertEquals(1, ConsentFragment.countSoftRules(rules));
    }
}
