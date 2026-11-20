package com.velvet.couplesgame.ui.minesweeper;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

/**
 * Pure-Java Couple Minesweeper state machine (6x6 grid).
 * Hitting a Heart Mine does NOT end the game — it triggers an escalating couple dare
 * while players race to uncover all safe tiles.
 */
public class CoupleMinesEngine {
    public static final int GRID_SIZE = 6;
    public static final int TOTAL_CELLS = GRID_SIZE * GRID_SIZE;

    public enum RevealOutcome {
        IGNORED,
        SAFE,
        HEART_MINE_TRIGGERED,
        BOARD_CLEARED
    }

    public static class MoveOutcome {
        public final RevealOutcome type;
        public final boolean actingPlayerA;
        public final int newlyRevealedCount;
        public final int suggestedTier;

        public MoveOutcome(RevealOutcome type, boolean actingPlayerA, int newlyRevealedCount, int suggestedTier) {
            this.type = type;
            this.actingPlayerA = actingPlayerA;
            this.newlyRevealedCount = newlyRevealedCount;
            this.suggestedTier = suggestedTier;
        }
    }

    private final MineCell[][] grid = new MineCell[GRID_SIZE][GRID_SIZE];
    private final Random random;
    private int mineCount;
    private boolean minesPlaced = false;
    private boolean playerATurn = true;

    private int playerASafeScore = 0;
    private int playerBSafeScore = 0;
    private int playerAMinesHit = 0;
    private int playerBMinesHit = 0;
    private int totalSafeRevealed = 0;
    private int totalMinesTriggered = 0;

    public CoupleMinesEngine(int mineCount) {
        this(mineCount, new Random());
    }

    public CoupleMinesEngine(int mineCount, Random random) {
        this.random = random;
        reset(mineCount);
    }

    public void reset(int newMineCount) {
        this.mineCount = Math.max(3, Math.min(14, newMineCount));
        this.minesPlaced = false;
        this.playerATurn = true;
        this.playerASafeScore = 0;
        this.playerBSafeScore = 0;
        this.playerAMinesHit = 0;
        this.playerBMinesHit = 0;
        this.totalSafeRevealed = 0;
        this.totalMinesTriggered = 0;

        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                grid[r][c] = new MineCell(r, c);
            }
        }
    }

    public List<MineCell> flattenCells() {
        List<MineCell> list = new ArrayList<>(TOTAL_CELLS);
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                list.add(grid[r][c]);
            }
        }
        return list;
    }

    public MineCell getCell(int row, int col) {
        return grid[row][col];
    }

    public boolean isPlayerATurn() {
        return playerATurn;
    }

    public int getMineCount() {
        return mineCount;
    }

    public int getPlayerASafeScore() {
        return playerASafeScore;
    }

    public int getPlayerBSafeScore() {
        return playerBSafeScore;
    }

    public int getPlayerAMinesHit() {
        return playerAMinesHit;
    }

    public int getPlayerBMinesHit() {
        return playerBMinesHit;
    }

    public int getRemainingSafeCells() {
        return (TOTAL_CELLS - mineCount) - totalSafeRevealed;
    }

    public boolean toggleFlag(int row, int col) {
        MineCell cell = grid[row][col];
        if (cell.revealed) return false;
        cell.flagged = !cell.flagged;
        return true;
    }

    public MoveOutcome revealCell(int row, int col) {
        MineCell cell = grid[row][col];
        if (cell.revealed || cell.flagged) {
            return new MoveOutcome(RevealOutcome.IGNORED, playerATurn, 0, 1);
        }

        // First click is always guaranteed safe
        if (!minesPlaced) {
            placeMinesAvoiding(row, col);
            minesPlaced = true;
        }

        boolean actorA = playerATurn;
        if (cell.hasMine) {
            cell.revealed = true;
            totalMinesTriggered++;
            if (actorA) playerAMinesHit++;
            else playerBMinesHit++;

            // Escalate dare intensity tier as more Heart Mines are uncovered
            int tier = Math.min(4, 1 + (totalMinesTriggered + 1) / 2);
            playerATurn = !playerATurn;
            return new MoveOutcome(RevealOutcome.HEART_MINE_TRIGGERED, actorA, 1, tier);
        }

        int uncovered = floodRevealSafe(row, col);
        totalSafeRevealed += uncovered;
        if (actorA) playerASafeScore += uncovered;
        else playerBSafeScore += uncovered;

        boolean cleared = (totalSafeRevealed >= (TOTAL_CELLS - mineCount));
        if (!cleared) {
            playerATurn = !playerATurn;
        }
        return new MoveOutcome(
                cleared ? RevealOutcome.BOARD_CLEARED : RevealOutcome.SAFE,
                actorA,
                uncovered,
                1
        );
    }

    /** Test helper to place deterministic mine layout. */
    public void setDeterministicMines(int[][] coords) {
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                grid[r][c].hasMine = false;
            }
        }
        for (int[] rc : coords) {
            grid[rc[0]][rc[1]].hasMine = true;
        }
        this.mineCount = coords.length;
        computeAdjacencyCounts();
        this.minesPlaced = true;
    }

    private void placeMinesAvoiding(int safeRow, int safeCol) {
        int placed = 0;
        while (placed < mineCount) {
            int r = random.nextInt(GRID_SIZE);
            int c = random.nextInt(GRID_SIZE);
            if ((r == safeRow && c == safeCol) || grid[r][c].hasMine) continue;
            grid[r][c].hasMine = true;
            placed++;
        }
        computeAdjacencyCounts();
    }

    private void computeAdjacencyCounts() {
        for (int r = 0; r < GRID_SIZE; r++) {
            for (int c = 0; c < GRID_SIZE; c++) {
                if (grid[r][c].hasMine) {
                    grid[r][c].adjacentMines = -1;
                    continue;
                }
                int count = 0;
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        int nr = r + dr;
                        int nc = c + dc;
                        if (nr >= 0 && nr < GRID_SIZE && nc >= 0 && nc < GRID_SIZE && grid[nr][nc].hasMine) {
                            count++;
                        }
                    }
                }
                grid[r][c].adjacentMines = count;
            }
        }
    }

    private int floodRevealSafe(int startR, int startC) {
        Queue<MineCell> queue = new ArrayDeque<>();
        MineCell start = grid[startR][startC];
        start.revealed = true;
        queue.add(start);
        int count = 1;

        while (!queue.isEmpty()) {
            MineCell cur = queue.poll();
            if (cur.adjacentMines != 0) continue;

            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    int nr = cur.row + dr;
                    int nc = cur.col + dc;
                    if (nr >= 0 && nr < GRID_SIZE && nc >= 0 && nc < GRID_SIZE) {
                        MineCell neighbor = grid[nr][nc];
                        if (!neighbor.revealed && !neighbor.flagged && !neighbor.hasMine) {
                            neighbor.revealed = true;
                            count++;
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }
        return count;
    }
}
