package com.velvet.couplesgame.ui.connect4;

import com.velvet.couplesgame.core.engine.ConsentFilter;
import com.velvet.couplesgame.core.engine.PromptHistoryStore;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.Connect4ActionDto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Pure-Java playable 5x6 Naughty Connect Four engine.
 * Populates the 30-cell board with non-repeating shuffled dares respecting ConsentFilter & maxTier,
 * and persists shown/played cells across boards via PromptHistoryStore.
 */
public class Connect4Engine {
    public static final int ROWS = 5;
    public static final int COLS = 6;
    public static final int EMPTY = 0;
    public static final int PLAYER_A = 1;
    public static final int PLAYER_B = 2;

    public static class Cell {
        public final int row;
        public final int col;
        public int owner = EMPTY;
        public boolean winningCell = false;
        public Connect4ActionDto action;

        public Cell(int row, int col, Connect4ActionDto action) {
            this.row = row;
            this.col = col;
            this.action = action;
        }
    }

    public static class DropResult {
        public final boolean valid;
        public final boolean droppedByA;
        public final Cell landedCell;
        public final boolean wonMatch;
        public final boolean boardFullDraw;
        public final List<Cell> winningLine;

        public DropResult(boolean valid, boolean droppedByA, Cell landedCell,
                          boolean wonMatch, boolean boardFullDraw, List<Cell> winningLine) {
            this.valid = valid;
            this.droppedByA = droppedByA;
            this.landedCell = landedCell;
            this.wonMatch = wonMatch;
            this.boardFullDraw = boardFullDraw;
            this.winningLine = winningLine != null ? winningLine : Collections.emptyList();
        }
    }

    private static final String HISTORY_BUCKET = "naughty_connect4";
    private final List<Connect4ActionDto> actionPool = new ArrayList<>();
    private final Set<String> seenSessionIds = new HashSet<>();
    private PromptHistoryStore historyStore;
    private final Cell[][] grid = new Cell[ROWS][COLS];
    private final Random random;

    private boolean playerATurn = true;
    private boolean gameOver = false;
    private int winsA = 0;
    private int winsB = 0;

    public Connect4Engine() {
        this(new Random());
    }

    public Connect4Engine(Random random) {
        this.random = random != null ? random : new Random();
    }

    public void attachHistoryStore(PromptHistoryStore store) {
        this.historyStore = store;
        if (store != null) {
            seenSessionIds.addAll(store.getRecentSet(HISTORY_BUCKET));
        }
    }

    public void loadActionsAndReset(List<Connect4ActionDto> source, ConsentFilter filter) {
        loadActionsAndReset(source, filter, 4);
    }

    public void loadActionsAndReset(List<Connect4ActionDto> source, ConsentFilter filter, int maxTier) {
        actionPool.clear();
        List<Connect4ActionDto> consentOnly = new ArrayList<>();
        if (source != null) {
            for (Connect4ActionDto a : source) {
                if (filter == null || filter.isAllowed(a.tags)) {
                    consentOnly.add(a);
                    if (a.tier <= maxTier) {
                        actionPool.add(a);
                    }
                }
            }
        }
        if (actionPool.isEmpty() && !consentOnly.isEmpty()) {
            actionPool.addAll(consentOnly);
        }
        resetBoard();
    }

    public void resetBoard() {
        playerATurn = true;
        gameOver = false;
        Set<String> recent = new HashSet<>(seenSessionIds);
        if (historyStore != null) {
            recent.addAll(historyStore.getRecentSet(HISTORY_BUCKET));
        }
        List<Connect4ActionDto> fresh = new ArrayList<>();
        List<Connect4ActionDto> seen = new ArrayList<>();
        for (Connect4ActionDto a : actionPool) {
            if (a.id != null && recent.contains(a.id)) seen.add(a);
            else fresh.add(a);
        }
        Collections.shuffle(fresh, random);
        Collections.shuffle(seen, random);
        if (fresh.size() < ROWS * COLS) {
            seenSessionIds.clear();
        }
        List<Connect4ActionDto> deck = new ArrayList<>(actionPool.size());
        deck.addAll(fresh);
        deck.addAll(seen);
        int cursor = 0;

        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                Connect4ActionDto act = null;
                if (!deck.isEmpty()) {
                    if (cursor >= deck.size()) {
                        Collections.shuffle(deck, random);
                        cursor = 0;
                    }
                    act = deck.get(cursor++);
                    if (act.id != null) {
                        seenSessionIds.add(act.id);
                    }
                }
                grid[r][c] = new Cell(r, c, act);
            }
        }
    }

    public DropResult dropInColumn(int col) {
        if (gameOver || col < 0 || col >= COLS) {
            return new DropResult(false, playerATurn, null, false, false, null);
        }
        int targetRow = -1;
        for (int r = ROWS - 1; r >= 0; r--) {
            if (grid[r][col].owner == EMPTY) {
                targetRow = r;
                break;
            }
        }
        if (targetRow == -1) {
            return new DropResult(false, playerATurn, null, false, false, null);
        }

        boolean actingA = playerATurn;
        int mark = actingA ? PLAYER_A : PLAYER_B;
        Cell cell = grid[targetRow][col];
        cell.owner = mark;
        if (cell.action != null && cell.action.id != null) {
            seenSessionIds.add(cell.action.id);
            if (historyStore != null) {
                historyStore.recordShown(HISTORY_BUCKET, cell.action.id, Math.max(90, Math.min(200, actionPool.size() - 5)));
            }
        }

        List<Cell> winLine = findWinningLine(targetRow, col, mark);
        if (!winLine.isEmpty()) {
            gameOver = true;
            for (Cell wc : winLine) wc.winningCell = true;
            if (actingA) winsA++;
            else winsB++;
            return new DropResult(true, actingA, cell, true, false, winLine);
        }

        boolean full = isBoardFull();
        if (full) {
            gameOver = true;
            return new DropResult(true, actingA, cell, false, true, null);
        }

        playerATurn = !playerATurn;
        return new DropResult(true, actingA, cell, false, false, null);
    }

    private List<Cell> findWinningLine(int row, int col, int mark) {
        int[][] dirs = {{0, 1}, {1, 0}, {1, 1}, {1, -1}};
        for (int[] d : dirs) {
            List<Cell> line = new ArrayList<>();
            line.add(grid[row][col]);
            for (int k = 1; k < 4; k++) {
                int nr = row + d[0] * k;
                int nc = col + d[1] * k;
                if (inBounds(nr, nc) && grid[nr][nc].owner == mark) line.add(grid[nr][nc]);
                else break;
            }
            for (int k = 1; k < 4; k++) {
                int nr = row - d[0] * k;
                int nc = col - d[1] * k;
                if (inBounds(nr, nc) && grid[nr][nc].owner == mark) line.add(grid[nr][nc]);
                else break;
            }
            if (line.size() >= 4) {
                return line.subList(0, 4);
            }
        }
        return Collections.emptyList();
    }

    private boolean isBoardFull() {
        for (int c = 0; c < COLS; c++) {
            if (grid[0][c].owner == EMPTY) return false;
        }
        return true;
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < ROWS && c >= 0 && c < COLS;
    }

    public List<Cell> flattenCells() {
        List<Cell> list = new ArrayList<>(ROWS * COLS);
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                list.add(grid[r][c]);
            }
        }
        return list;
    }

    public boolean hasEligibleActions() { return !actionPool.isEmpty(); }
    public boolean isPlayerATurn() { return playerATurn; }
    public boolean isGameOver() { return gameOver; }
    public int getWinsA() { return winsA; }
    public int getWinsB() { return winsB; }
}
