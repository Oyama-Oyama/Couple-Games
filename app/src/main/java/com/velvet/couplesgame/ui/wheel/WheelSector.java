package com.velvet.couplesgame.ui.wheel;

public class WheelSector {
    public final String id;
    public final String label;
    public final int tier;
    public final String requiredType;      // TRUTH / DARE / CONNECTION / null
    public final boolean desireWildcard;
    public final String customPromptText;  // Non-null for user-entered wheel options

    public WheelSector(String id, String label, int tier, String requiredType,
                       boolean desireWildcard, String customPromptText) {
        this.id = id;
        this.label = label;
        this.tier = tier;
        this.requiredType = requiredType;
        this.desireWildcard = desireWildcard;
        this.customPromptText = customPromptText;
    }
}
