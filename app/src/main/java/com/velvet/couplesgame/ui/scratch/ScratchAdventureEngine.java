package com.velvet.couplesgame.ui.scratch;

import com.velvet.couplesgame.core.engine.ConsentFilter;
import com.velvet.couplesgame.core.engine.PromptHistoryStore;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.ScratchCardDto;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Pure-Java engine for Scratch-Off Adventure Cards across DARE / LOCATION / FANTASY categories.
 * Uses full-cycle non-repeating draws plus a rolling recent-card shield across recycles.
 */
public class ScratchAdventureEngine {
    public static final String CAT_ALL = "ALL";
    public static final String CAT_DARE = "DARE";
    public static final String CAT_LOCATION = "LOCATION";
    public static final String CAT_FANTASY = "FANTASY";
    private static final int RECENT_SHIELD_CAP = 8;

    private final List<ScratchCardDto> allEligible = new ArrayList<>();
    private final Set<String> revealedIds = new HashSet<>();
    private final Deque<String> recentIds = new ArrayDeque<>();
    private static final String HISTORY_BUCKET = "scratch_adventure";
    private final Random random;
    private PromptHistoryStore historyStore;

    private String activeCategory = CAT_ALL;
    private ScratchCardDto currentCard;
    private int completedCount = 0;

    public ScratchAdventureEngine() {
        this(new Random());
    }

    public ScratchAdventureEngine(Random random) {
        this.random = random != null ? random : new Random();
    }

    public void attachHistoryStore(PromptHistoryStore store) {
        this.historyStore = store;
    }

    public void loadCards(List<ScratchCardDto> source, ConsentFilter filter, int maxTier) {
        allEligible.clear();
        revealedIds.clear();
        recentIds.clear();
        currentCard = null;
        if (historyStore != null) {
            for (String id : historyStore.getRecentList(HISTORY_BUCKET)) {
                revealedIds.add(id);
                recentIds.addLast(id);
            }
        }
        if (source != null) {
            for (ScratchCardDto c : source) {
                if (c.tier <= maxTier && (filter == null || filter.isAllowed(c.tags))) {
                    allEligible.add(c);
                }
            }
        }
        Collections.shuffle(allEligible, random);
    }

    public void setCategory(String category) {
        this.activeCategory = category != null ? category : CAT_ALL;
    }

    public String getActiveCategory() {
        return activeCategory;
    }

    public ScratchCardDto drawNextCard() {
        List<ScratchCardDto> categoryAll = collectForCategory(true);
        if (categoryAll.isEmpty()) {
            currentCard = null;
            return null;
        }
        if (categoryAll.size() == 1) {
            currentCard = categoryAll.get(0);
            recordRevealed(currentCard.id, 0);
            return currentCard;
        }

        List<ScratchCardDto> unseen = collectForCategory(false);
        if (unseen.isEmpty()) {
            // Recycle only this category's cards while shielding the most recent draws
            for (ScratchCardDto c : categoryAll) {
                revealedIds.remove(c.id);
            }
            int shieldSize = Math.min(RECENT_SHIELD_CAP, Math.max(1, categoryAll.size() / 2));
            List<String> tail = recentTail(shieldSize);
            for (ScratchCardDto c : categoryAll) {
                if (!tail.contains(c.id)) {
                    unseen.add(c);
                }
            }
            if (unseen.isEmpty()) {
                for (ScratchCardDto c : categoryAll) {
                    if (currentCard == null || !c.id.equals(currentCard.id)) {
                        unseen.add(c);
                    }
                }
            }
        }

        if (unseen.isEmpty()) {
            unseen = categoryAll;
        }

        currentCard = unseen.get(random.nextInt(unseen.size()));
        recordRevealed(currentCard.id, Math.min(RECENT_SHIELD_CAP, Math.max(1, allEligible.size() - 1)));
        return currentCard;
    }

    private void recordRevealed(String id, int maxRecent) {
        if (id == null) return;
        revealedIds.add(id);
        recentIds.remove(id);
        recentIds.addLast(id);
        while (recentIds.size() > maxRecent) {
            recentIds.removeFirst();
        }
        if (historyStore != null) {
            historyStore.recordShown(HISTORY_BUCKET, id, 200);
        }
    }

    private List<String> recentTail(int count) {
        List<String> all = new ArrayList<>(recentIds);
        int from = Math.max(0, all.size() - count);
        return all.subList(from, all.size());
    }

    private List<ScratchCardDto> collectForCategory(boolean ignoreUsed) {
        List<ScratchCardDto> res = new ArrayList<>();
        for (ScratchCardDto c : allEligible) {
            if (!CAT_ALL.equals(activeCategory) && !activeCategory.equals(c.category)) continue;
            if (!ignoreUsed && revealedIds.contains(c.id)) continue;
            res.add(c);
        }
        return res;
    }

    public ScratchCardDto getCurrentCard() {
        return currentCard;
    }

    public int getCompletedCount() {
        return completedCount;
    }

    public void markCurrentCompleted() {
        if (currentCard != null) completedCount++;
    }
}
