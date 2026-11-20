package com.velvet.couplesgame.core.billing;

import com.velvet.couplesgame.core.model.IntensityTier;
import com.velvet.couplesgame.core.theme.AppTheme;

/**
 * Pure-Java commercialization gating rules for easy unit testing and consistent enforcement.
 */
public final class FeatureGate {
    public static final int FREE_MAX_TIER = 2;
    public static final int PREMIUM_MAX_TIER = 4;

    private FeatureGate() {}

    public static boolean canAccessTier(int tierLevel, boolean isPremiumUnlocked) {
        IntensityTier tier = IntensityTier.fromLevel(tierLevel);
        return !tier.premiumRequired || isPremiumUnlocked;
    }

    public static boolean canAccessTheme(AppTheme theme, boolean isPremiumUnlocked) {
        if (theme == null) return true;
        return !theme.premium || isPremiumUnlocked;
    }

    public static int resolveEffectiveMaxTier(int requestedTier, boolean isPremiumUnlocked) {
        int clamped = Math.max(1, Math.min(PREMIUM_MAX_TIER, requestedTier));
        return isPremiumUnlocked ? clamped : Math.min(FREE_MAX_TIER, clamped);
    }
}
