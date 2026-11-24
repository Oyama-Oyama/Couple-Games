package com.velvet.couplesgame.core.model;

public class BoardTheme {
    public final String id;
    public final String name;
    public final String bgHex;
    public final String primaryHex;
    public final String accentHex;
    public final int totalTiles;
    public final boolean premium;

    public BoardTheme(String id, String name, String bgHex, String primaryHex, String accentHex,
                      int totalTiles, boolean premium) {
        this.id = id;
        this.name = name;
        this.bgHex = bgHex != null ? bgHex : "#121013";
        this.primaryHex = primaryHex;
        this.accentHex = accentHex;
        this.totalTiles = totalTiles;
        this.premium = premium;
    }
}
