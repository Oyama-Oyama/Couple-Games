package com.velvet.couplesgame.ui.chess;

public class BoardTile {
    public final int index;
    public final int tier; // 1..4
    public final BoardTileType type;
    public final int col;  // 0..5 on 6x6 serpentine grid
    public final int row;  // 0..5 (bottom to top visual layout)

    public BoardTile(int index, int tier, BoardTileType type, int col, int row) {
        this.index = index;
        this.tier = tier;
        this.type = type;
        this.col = col;
        this.row = row;
    }
}
