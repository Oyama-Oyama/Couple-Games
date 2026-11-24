package com.velvet.couplesgame.ui.chess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Pure-Java state machine for the 36-tile escalating Couple Board (Flying Chess).
 */
public class CoupleBoardEngine {
    public static final int TOTAL_TILES = 36;
    public static final int GRID_SIZE = 6;
    public static final int FINISH_INDEX = TOTAL_TILES - 1;

    public static class MoveResult {
        public final boolean movedPlayerA;
        public final int diceValue;
        public final int fromIndex;
        public final int landedIndex;
        public final int finalIndex;
        public final BoardTileType triggeredType;
        public final int landedTier;
        public final boolean grantExtraRoll;
        public final boolean swapped;
        public final boolean wonGame;

        public MoveResult(boolean movedPlayerA, int diceValue, int fromIndex, int landedIndex,
                          int finalIndex, BoardTileType triggeredType, int landedTier,
                          boolean grantExtraRoll, boolean swapped, boolean wonGame) {
            this.movedPlayerA = movedPlayerA;
            this.diceValue = diceValue;
            this.fromIndex = fromIndex;
            this.landedIndex = landedIndex;
            this.finalIndex = finalIndex;
            this.triggeredType = triggeredType;
            this.landedTier = landedTier;
            this.grantExtraRoll = grantExtraRoll;
            this.swapped = swapped;
            this.wonGame = wonGame;
        }
    }

    private final List<BoardTile> tiles;
    private final Random random;

    private int playerAPos = 0;
    private int playerBPos = 0;
    private boolean playerATurn = true;
    private int playerACompleted = 0;
    private int playerBCompleted = 0;
    private int skipsUsed = 0;
    private String playerAReward = "30-min shoulder massage";
    private String playerBReward = "Breakfast in bed tomorrow";
    private boolean rewardsCustomized = false;

    public CoupleBoardEngine() {
        this(new Random());
    }

    public CoupleBoardEngine(Random random) {
        this.random = random;
        this.tiles = buildSerpentineBoard();
    }

    private List<BoardTile> buildSerpentineBoard() {
        List<BoardTile> list = new ArrayList<>(TOTAL_TILES);
        for (int i = 0; i < TOTAL_TILES; i++) {
            int rowFromBottom = i / GRID_SIZE;
            int offsetInRow = i % GRID_SIZE;
            int col = (rowFromBottom % 2 == 0) ? offsetInRow : (GRID_SIZE - 1 - offsetInRow);
            int visualRow = (GRID_SIZE - 1) - rowFromBottom; // Row 5 is bottom, Row 0 is top

            int tier;
            if (i <= 8) tier = 1;
            else if (i <= 17) tier = 2;
            else if (i <= 26) tier = 3;
            else tier = 4;

            BoardTileType type = resolveTileType(i);
            list.add(new BoardTile(i, tier, type, col, visualRow));
        }
        return Collections.unmodifiableList(list);
    }

    private BoardTileType resolveTileType(int index) {
        if (index == 0) return BoardTileType.START;
        if (index == FINISH_INDEX) return BoardTileType.FINISH;
        if (index == 4 || index == 14 || index == 24) return BoardTileType.BOOST_FORWARD;
        if (index == 11 || index == 21 || index == 31) return BoardTileType.STEP_BACK;
        if (index == 16 || index == 28) return BoardTileType.SWAP_POSITIONS;
        if (index == 7 || index == 19) return BoardTileType.EXTRA_ROLL;
        if (index == 12 || index == 22 || index == 32) return BoardTileType.DESIRE_WILDCARD;
        return BoardTileType.TASK;
    }

    public List<BoardTile> getTiles() {
        return tiles;
    }

    public int getPlayerAPos() {
        return playerAPos;
    }

    public int getPlayerBPos() {
        return playerBPos;
    }

    public boolean isPlayerATurn() {
        return playerATurn;
    }

    public int getPlayerACompleted() {
        return playerACompleted;
    }

    public int getPlayerBCompleted() {
        return playerBCompleted;
    }

    public int getSkipsUsed() {
        return skipsUsed;
    }

    public String getPlayerAReward() {
        return playerAReward;
    }

    public String getPlayerBReward() {
        return playerBReward;
    }

    public void initDefaultRewardsIfUnmodified(String defaultA, String defaultB) {
        if (rewardsCustomized) return;
        if (defaultA != null && !defaultA.trim().isEmpty()) this.playerAReward = defaultA.trim();
        if (defaultB != null && !defaultB.trim().isEmpty()) this.playerBReward = defaultB.trim();
    }

    public void setRewards(String rewardA, String rewardB) {
        if (rewardA != null && !rewardA.trim().isEmpty()) {
            this.playerAReward = rewardA.trim();
            this.rewardsCustomized = true;
        }
        if (rewardB != null && !rewardB.trim().isEmpty()) {
            this.playerBReward = rewardB.trim();
            this.rewardsCustomized = true;
        }
    }

    public void resetMatch() {
        playerAPos = 0;
        playerBPos = 0;
        playerATurn = true;
        playerACompleted = 0;
        playerBCompleted = 0;
        skipsUsed = 0;
    }

    public MoveResult rollAndAdvance() {
        int dice = random.nextInt(6) + 1;
        return advanceByDice(dice);
    }

    public MoveResult advanceByDice(int diceValue) {
        boolean actingA = playerATurn;
        int from = actingA ? playerAPos : playerBPos;
        int landed = Math.min(FINISH_INDEX, from + diceValue);
        BoardTile landedTile = tiles.get(landed);

        int finalPos = landed;
        boolean grantExtra = false;
        boolean swapped = false;

        switch (landedTile.type) {
            case BOOST_FORWARD:
                finalPos = Math.min(FINISH_INDEX, landed + 2);
                break;
            case STEP_BACK:
                finalPos = Math.max(0, landed - 2);
                break;
            case SWAP_POSITIONS:
                swapped = true;
                int otherPos = actingA ? playerBPos : playerAPos;
                finalPos = otherPos;
                if (actingA) {
                    playerBPos = landed;
                } else {
                    playerAPos = landed;
                }
                break;
            case EXTRA_ROLL:
                grantExtra = true;
                break;
            default:
                break;
        }

        if (actingA) {
            playerAPos = finalPos;
        } else {
            playerBPos = finalPos;
        }

        boolean won = (finalPos >= FINISH_INDEX);
        if (!won && !grantExtra) {
            playerATurn = !playerATurn;
        }

        return new MoveResult(
                actingA,
                diceValue,
                from,
                landed,
                finalPos,
                landedTile.type,
                tiles.get(finalPos).tier,
                grantExtra,
                swapped,
                won
        );
    }

    public void recordTaskApproved(boolean actingPlayerA) {
        if (actingPlayerA) playerACompleted++;
        else playerBCompleted++;
    }

    public void recordTaskSkipped() {
        skipsUsed++;
    }

    /** Gentle penalty if partner requests a redo/step-back on a dare. */
    public void applyStepBackPenalty(boolean actingPlayerA) {
        if (actingPlayerA) {
            playerAPos = Math.max(0, playerAPos - 1);
        } else {
            playerBPos = Math.max(0, playerBPos - 1);
        }
    }
}
