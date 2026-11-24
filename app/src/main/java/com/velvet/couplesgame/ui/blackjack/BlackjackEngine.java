package com.velvet.couplesgame.ui.blackjack;

import com.velvet.couplesgame.core.engine.ConsentFilter;
import com.velvet.couplesgame.core.engine.PromptHistoryStore;
import java.util.Set;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.BlackjackStakeDto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Pure-Java 2-player Velvet 21 (Blackjack with intimate chip/clothing stakes & Double Down).
 * Guarantees strict chip conservation across all hands and automatic stand on natural 21.
 */
public class BlackjackEngine {
    public static final int STARTING_CHIPS = 5;

    public enum Phase {
        TURN_A,
        TURN_B,
        SHOWDOWN
    }

    public enum Outcome {
        NONE,
        PLAYER_A_WINS,
        PLAYER_B_WINS,
        PUSH
    }

    public static class PlayingCard {
        public final String rank;
        public final String suit;
        public final int baseValue;

        public PlayingCard(String rank, String suit, int baseValue) {
            this.rank = rank;
            this.suit = suit;
            this.baseValue = baseValue;
        }

        public String display() {
            return rank + suit;
        }
    }

    private final List<PlayingCard> deck = new ArrayList<>();
    private final List<PlayingCard> handA = new ArrayList<>();
    private final List<PlayingCard> handB = new ArrayList<>();
    private final List<BlackjackStakeDto> stakesPool = new ArrayList<>();
    private int stakeCursor = 0;
    private static final String HISTORY_BUCKET = "velvet_blackjack";
    private final Random random;
    private PromptHistoryStore historyStore;

    private Phase phase = Phase.TURN_A;
    private Outcome lastOutcome = Outcome.NONE;
    private int chipsA = STARTING_CHIPS;
    private int chipsB = STARTING_CHIPS;
    private int currentBet = 1;
    private int lastTransferredChips = 0;
    private BlackjackStakeDto activeStake;

    public BlackjackEngine() {
        this(new Random());
    }

    public BlackjackEngine(Random random) {
        this.random = random != null ? random : new Random();
        rebuildDeck();
    }

    public void attachHistoryStore(PromptHistoryStore store) {
        this.historyStore = store;
    }

    public void loadStakes(List<BlackjackStakeDto> source, ConsentFilter filter, int maxTier) {
        stakesPool.clear();
        stakeCursor = 0;
        Set<String> recent = historyStore != null ? historyStore.getRecentSet(HISTORY_BUCKET) : Collections.emptySet();
        List<BlackjackStakeDto> fresh = new ArrayList<>();
        List<BlackjackStakeDto> seen = new ArrayList<>();
        if (source != null) {
            for (BlackjackStakeDto s : source) {
                if (s.tier <= maxTier && (filter == null || filter.isAllowed(s.tags))) {
                    if (recent.contains(s.id)) seen.add(s);
                    else fresh.add(s);
                }
            }
        }
        Collections.shuffle(fresh, random);
        Collections.shuffle(seen, random);
        stakesPool.addAll(fresh);
        stakesPool.addAll(seen);
    }

    public void resetMatch() {
        chipsA = STARTING_CHIPS;
        chipsB = STARTING_CHIPS;
        startNewHand();
    }

    public void startNewHand() {
        if (deck.size() < 12) rebuildDeck();
        handA.clear();
        handB.clear();
        currentBet = 1;
        lastTransferredChips = 0;
        lastOutcome = Outcome.NONE;
        phase = Phase.TURN_A;

        handA.add(drawCard());
        handA.add(drawCard());
        handB.add(drawCard());
        handB.add(drawCard());

        if (!stakesPool.isEmpty()) {
            if (stakeCursor >= stakesPool.size()) {
                String prevId = activeStake != null ? activeStake.id : null;
                Collections.shuffle(stakesPool, random);
                stakeCursor = 0;
                if (prevId != null && stakesPool.size() > 1 && prevId.equals(stakesPool.get(0).id)) {
                    Collections.swap(stakesPool, 0, stakesPool.size() - 1);
                }
            }
            activeStake = stakesPool.get(stakeCursor++);
            if (historyStore != null && activeStake.id != null) historyStore.recordShown(HISTORY_BUCKET, activeStake.id, 180);
        } else {
            activeStake = null;
        }

        if (computeScore(handA) == 21) {
            standCurrentPlayer();
        }
    }

    public boolean canDoubleDown() {
        if (phase == Phase.SHOWDOWN) return false;
        List<PlayingCard> activeHand = (phase == Phase.TURN_A) ? handA : handB;
        return activeHand.size() == 2 && chipsA >= 2 && chipsB >= 2 && currentBet == 1;
    }

    public void hitCurrentPlayer() {
        if (phase == Phase.SHOWDOWN) return;
        List<PlayingCard> hand = (phase == Phase.TURN_A) ? handA : handB;
        hand.add(drawCard());
        if (computeScore(hand) >= 21) {
            standCurrentPlayer();
        }
    }

    public void doubleDownCurrentPlayer() {
        if (!canDoubleDown()) return;
        currentBet = 2;
        List<PlayingCard> hand = (phase == Phase.TURN_A) ? handA : handB;
        hand.add(drawCard());
        standCurrentPlayer();
    }

    public void standCurrentPlayer() {
        if (phase == Phase.TURN_A) {
            phase = Phase.TURN_B;
            if (computeScore(handB) == 21) {
                resolveShowdown();
            }
        } else if (phase == Phase.TURN_B) {
            resolveShowdown();
        }
    }

    private void resolveShowdown() {
        phase = Phase.SHOWDOWN;
        int scoreA = computeScore(handA);
        int scoreB = computeScore(handB);
        boolean bustA = scoreA > 21;
        boolean bustB = scoreB > 21;

        if (bustA && bustB) {
            lastOutcome = Outcome.PUSH;
        } else if (bustA) {
            lastOutcome = Outcome.PLAYER_B_WINS;
        } else if (bustB) {
            lastOutcome = Outcome.PLAYER_A_WINS;
        } else if (scoreA > scoreB) {
            lastOutcome = Outcome.PLAYER_A_WINS;
        } else if (scoreB > scoreA) {
            lastOutcome = Outcome.PLAYER_B_WINS;
        } else {
            lastOutcome = Outcome.PUSH;
        }

        if (lastOutcome == Outcome.PLAYER_A_WINS) {
            lastTransferredChips = Math.min(currentBet, chipsB);
            chipsB -= lastTransferredChips;
            chipsA += lastTransferredChips;
        } else if (lastOutcome == Outcome.PLAYER_B_WINS) {
            lastTransferredChips = Math.min(currentBet, chipsA);
            chipsA -= lastTransferredChips;
            chipsB += lastTransferredChips;
        } else {
            lastTransferredChips = 0;
        }
    }

    public static int computeScore(List<PlayingCard> hand) {
        int sum = 0;
        int aces = 0;
        for (PlayingCard c : hand) {
            sum += c.baseValue;
            if ("A".equals(c.rank)) aces++;
        }
        while (sum > 21 && aces > 0) {
            sum -= 10;
            aces--;
        }
        return sum;
    }

    private void rebuildDeck() {
        deck.clear();
        String[] suits = {"♠", "♥", "♦", "♣"};
        String[] ranks = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};
        int[] values = {11, 2, 3, 4, 5, 6, 7, 8, 9, 10, 10, 10, 10};
        for (String suit : suits) {
            for (int i = 0; i < ranks.length; i++) {
                deck.add(new PlayingCard(ranks[i], suit, values[i]));
            }
        }
        Collections.shuffle(deck, random);
    }

    private PlayingCard drawCard() {
        if (deck.isEmpty()) rebuildDeck();
        return deck.remove(deck.size() - 1);
    }

    public Phase getPhase() { return phase; }
    public Outcome getLastOutcome() { return lastOutcome; }
    public int getChipsA() { return chipsA; }
    public int getChipsB() { return chipsB; }
    public int getCurrentBet() { return currentBet; }
    public int getLastTransferredChips() { return lastTransferredChips; }
    public BlackjackStakeDto getActiveStake() { return activeStake; }
    public List<PlayingCard> getHandA() { return Collections.unmodifiableList(handA); }
    public List<PlayingCard> getHandB() { return Collections.unmodifiableList(handB); }
    public int getScoreA() { return computeScore(handA); }
    public int getScoreB() { return computeScore(handB); }
    public boolean isMatchOver() { return chipsA <= 0 || chipsB <= 0; }
}
