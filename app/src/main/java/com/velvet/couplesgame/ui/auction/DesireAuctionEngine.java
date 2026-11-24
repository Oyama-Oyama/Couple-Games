package com.velvet.couplesgame.ui.auction;

import com.velvet.couplesgame.core.engine.ConsentFilter;
import com.velvet.couplesgame.core.engine.PromptHistoryStore;
import java.util.Set;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.AuctionContractDto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Pure-Java 100-chip competitive Desire Auction engine.
 * Partners bid Desire Chips for bold dominance & privilege contracts;
 * broke players can perform a bailout dare to earn +30 chips back.
 */
public class DesireAuctionEngine {
    public static final int STARTING_CHIPS = 100;
    public static final int BAILOUT_REWARD = 30;

    public enum BidPhase {
        BIDDING,
        SOLD
    }

    private final List<AuctionContractDto> pool = new ArrayList<>();
    private static final String HISTORY_BUCKET = "desire_auction";
    private final Random random;
    private PromptHistoryStore historyStore;

    private int cursor = 0;
    private AuctionContractDto activeContract;
    private BidPhase phase = BidPhase.BIDDING;

    private int chipsA = STARTING_CHIPS;
    private int chipsB = STARTING_CHIPS;
    private int currentBid = 0;
    private Boolean highestBidderIsA = null;
    private boolean bidderTurnIsA = true;
    private int contractsWonA = 0;
    private int contractsWonB = 0;

    public DesireAuctionEngine() {
        this(new Random());
    }

    public DesireAuctionEngine(Random random) {
        this.random = random != null ? random : new Random();
    }

    public void attachHistoryStore(PromptHistoryStore store) {
        this.historyStore = store;
    }

    public void loadContracts(List<AuctionContractDto> source, ConsentFilter filter, int maxTier) {
        pool.clear();
        cursor = 0;
        Set<String> recent = historyStore != null ? historyStore.getRecentSet(HISTORY_BUCKET) : Collections.emptySet();
        List<AuctionContractDto> fresh = new ArrayList<>();
        List<AuctionContractDto> seen = new ArrayList<>();
        if (source != null) {
            for (AuctionContractDto c : source) {
                if (c.tier <= maxTier && (filter == null || filter.isAllowed(c.tags))) {
                    if (recent.contains(c.id)) seen.add(c);
                    else fresh.add(c);
                }
            }
        }
        Collections.shuffle(fresh, random);
        Collections.shuffle(seen, random);
        pool.addAll(fresh);
        pool.addAll(seen);
        chipsA = STARTING_CHIPS;
        chipsB = STARTING_CHIPS;
        contractsWonA = 0;
        contractsWonB = 0;
        nextContract();
    }

    public AuctionContractDto nextContract() {
        if (pool.isEmpty()) {
            activeContract = null;
            return null;
        }
        if (cursor >= pool.size()) {
            String prevId = activeContract != null ? activeContract.id : null;
            Collections.shuffle(pool, random);
            cursor = 0;
            if (prevId != null && pool.size() > 1 && prevId.equals(pool.get(0).id)) {
                Collections.swap(pool, 0, pool.size() - 1);
            }
        }
        activeContract = pool.get(cursor++);
        if (historyStore != null && activeContract.id != null) historyStore.recordShown(HISTORY_BUCKET, activeContract.id, 180);
        currentBid = 0;
        highestBidderIsA = null;
        phase = BidPhase.BIDDING;
        return activeContract;
    }

    public boolean placeBidIncrement(int increment) {
        if (phase != BidPhase.BIDDING || increment <= 0) return false;
        int targetBid = currentBid + increment;
        int available = bidderTurnIsA ? chipsA : chipsB;
        if (targetBid > available) return false;

        currentBid = targetBid;
        highestBidderIsA = bidderTurnIsA;
        bidderTurnIsA = !bidderTurnIsA;
        return true;
    }

    public boolean bidAllIn() {
        if (phase != BidPhase.BIDDING) return false;
        int available = bidderTurnIsA ? chipsA : chipsB;
        if (available <= currentBid) return false;
        currentBid = available;
        highestBidderIsA = bidderTurnIsA;
        bidderTurnIsA = !bidderTurnIsA;
        return true;
    }

    /** Current bidder folds. If no bid yet, awards contract free to the other partner. */
    public boolean foldCurrentBidder() {
        if (phase != BidPhase.BIDDING) return false;
        boolean winnerIsA = (highestBidderIsA != null) ? highestBidderIsA : !bidderTurnIsA;
        if (winnerIsA) {
            chipsA = Math.max(0, chipsA - currentBid);
            contractsWonA++;
        } else {
            chipsB = Math.max(0, chipsB - currentBid);
            contractsWonB++;
        }
        highestBidderIsA = winnerIsA;
        phase = BidPhase.SOLD;
        bidderTurnIsA = !winnerIsA; // Loser opens bidding next round
        return winnerIsA;
    }

    public void grantBailoutChips(boolean forPlayerA) {
        if (forPlayerA) chipsA += BAILOUT_REWARD;
        else chipsB += BAILOUT_REWARD;
    }

    public AuctionContractDto getActiveContract() { return activeContract; }
    public BidPhase getPhase() { return phase; }
    public int getChipsA() { return chipsA; }
    public int getChipsB() { return chipsB; }
    public int getCurrentBid() { return currentBid; }
    public boolean isBidderTurnA() { return bidderTurnIsA; }
    public Boolean getHighestBidderIsA() { return highestBidderIsA; }
    public int getContractsWonA() { return contractsWonA; }
    public int getContractsWonB() { return contractsWonB; }
}
