package com.velvet.couplesgame.core.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.velvet.couplesgame.core.billing.FeatureGate;
import com.velvet.couplesgame.core.theme.AppTheme;
import org.junit.Test;

public class Phase4ReleaseTest {

    @Test
    public void featureGateEnforcesFreeAndVipTiers() {
        assertTrue(FeatureGate.canAccessTier(1, false));
        assertTrue(FeatureGate.canAccessTier(2, false));
        assertFalse(FeatureGate.canAccessTier(3, false));
        assertFalse(FeatureGate.canAccessTier(4, false));

        assertTrue(FeatureGate.canAccessTier(3, true));
        assertTrue(FeatureGate.canAccessTier(4, true));

        assertEquals(2, FeatureGate.resolveEffectiveMaxTier(4, false));
        assertEquals(4, FeatureGate.resolveEffectiveMaxTier(4, true));
        assertEquals(1, FeatureGate.resolveEffectiveMaxTier(0, false));
    }

    @Test
    public void featureGateEnforcesVipMoodThemes() {
        assertTrue(FeatureGate.canAccessTheme(AppTheme.VELVET_NOIR, false));
        assertTrue(FeatureGate.canAccessTheme(AppTheme.BLUSH_CHAMPAGNE, false));
        assertTrue(FeatureGate.canAccessTheme(AppTheme.MIDNIGHT_PLUM, false));

        assertFalse(FeatureGate.canAccessTheme(AppTheme.CANDLELIGHT_AMBER, false));
        assertFalse(FeatureGate.canAccessTheme(AppTheme.NEON_SILK, false));

        assertTrue(FeatureGate.canAccessTheme(AppTheme.CANDLELIGHT_AMBER, true));
        assertTrue(FeatureGate.canAccessTheme(AppTheme.NEON_SILK, true));
    }
}
