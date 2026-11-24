package com.velvet.couplesgame.core.model;

public enum IntensityTier {
    SPARK(1, false),
    PLAYFUL(2, false),
    FLIRTY(3, true),
    BOLD(4, true);

    public final int level;
    public final boolean premiumRequired;

    IntensityTier(int level, boolean premiumRequired) {
        this.level = level;
        this.premiumRequired = premiumRequired;
    }

    public static IntensityTier fromLevel(int level) {
        for (IntensityTier tier : values()) {
            if (tier.level == level) return tier;
        }
        return SPARK;
    }
}
