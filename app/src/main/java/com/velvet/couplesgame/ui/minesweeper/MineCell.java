package com.velvet.couplesgame.ui.minesweeper;

public class MineCell {
    public final int row;
    public final int col;
    public boolean hasMine;
    public boolean revealed;
    public boolean flagged;
    public int adjacentMines;

    public MineCell(int row, int col) {
        this.row = row;
        this.col = col;
    }
}
