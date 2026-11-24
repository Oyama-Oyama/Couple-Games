package com.velvet.couplesgame.ui.positions;

import com.velvet.couplesgame.core.engine.ConsentFilter;
import com.velvet.couplesgame.core.engine.PromptHistoryStore;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.PositionDto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Pure-Java Kama Positions Roulette & Tried-Checklist engine with full-cycle non-repeating spins.
 */
public class PositionsEngine {
    private static final String HISTORY_BUCKET = "positions_roulette";

    private final List<PositionDto> eligible = new ArrayList<>();
    private final Set<String> triedIds = new HashSet<>();
    private final Set<String> seenThisCycle = new HashSet<>();
    private final Random random;
    private PromptHistoryStore historyStore;
    private PositionDto currentPosition;
    private int difficultyFilter = 0; // 0 = All, 1 = Easy, 2 = Medium, 3 = Athletic

    public PositionsEngine() {
        this(new Random());
    }

    public PositionsEngine(Random random) {
        this.random = random != null ? random : new Random();
    }

    public void attachHistoryStore(PromptHistoryStore store) {
        this.historyStore = store;
        if (store != null) {
            seenThisCycle.addAll(store.getRecentSet(HISTORY_BUCKET));
        }
    }

    public void loadPositions(List<PositionDto> source, ConsentFilter filter) {
        eligible.clear();
        if (source != null) {
            for (PositionDto p : source) {
                if (filter == null || filter.isAllowed(p.tags)) {
                    eligible.add(p);
                }
            }
        }
        Collections.shuffle(eligible, random);
    }

    public void setDifficultyFilter(int diff) {
        this.difficultyFilter = diff;
    }

    public int getDifficultyFilter() {
        return difficultyFilter;
    }

    public List<PositionDto> getFilteredPool() {
        List<PositionDto> out = new ArrayList<>();
        for (PositionDto p : eligible) {
            if (difficultyFilter == 0 || p.difficulty == difficultyFilter) {
                out.add(p);
            }
        }
        return out;
    }

    public PositionDto spinRandom() {
        List<PositionDto> pool = getFilteredPool();
        if (pool.isEmpty()) {
            currentPosition = null;
            return null;
        }
        if (pool.size() == 1) {
            currentPosition = pool.get(0);
            recordSeen(currentPosition.id);
            return currentPosition;
        }

        List<PositionDto> unseen = new ArrayList<>();
        for (PositionDto p : pool) {
            if (!seenThisCycle.contains(p.id) && (currentPosition == null || !p.id.equals(currentPosition.id))) {
                unseen.add(p);
            }
        }
        if (unseen.isEmpty()) {
            for (PositionDto p : pool) {
                seenThisCycle.remove(p.id);
            }
            for (PositionDto p : pool) {
                if (currentPosition == null || !p.id.equals(currentPosition.id)) {
                    unseen.add(p);
                }
            }
        }
        List<PositionDto> pickFrom = unseen.isEmpty() ? pool : unseen;
        currentPosition = pickFrom.get(random.nextInt(pickFrom.size()));
        recordSeen(currentPosition.id);
        return currentPosition;
    }

    private void recordSeen(String id) {
        if (id == null) return;
        seenThisCycle.add(id);
        if (historyStore != null) {
            historyStore.recordShown(HISTORY_BUCKET, id, Math.max(12, Math.min(200, eligible.size() - 4)));
        }
    }

    public PositionDto getCurrentPosition() { return currentPosition; }

    public boolean toggleTriedCurrent() {
        if (currentPosition == null) return false;
        if (triedIds.contains(currentPosition.id)) {
            triedIds.remove(currentPosition.id);
            return false;
        }
        triedIds.add(currentPosition.id);
        return true;
    }

    public boolean isCurrentTried() {
        return currentPosition != null && triedIds.contains(currentPosition.id);
    }

    public int getTriedCount() { return triedIds.size(); }
    public int getTotalEligibleCount() { return eligible.size(); }
}
