package com.velvet.couplesgame.core.model;

public class PlayerProfile {
    public final String name;
    public final String pronoun; // He / She / They
    public int skipsRemaining;

    public PlayerProfile(String name, String pronoun, int skipsRemaining) {
        this.name = (name != null && !name.trim().isEmpty()) ? name.trim() : "Partner";
        this.pronoun = (pronoun != null && !pronoun.trim().isEmpty()) ? pronoun.trim() : "They";
        this.skipsRemaining = skipsRemaining;
    }

    public String getInitial() {
        if (name.isEmpty()) return "♥";
        return name.substring(0, 1).toUpperCase();
    }
}
