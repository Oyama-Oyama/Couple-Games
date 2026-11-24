package com.velvet.couplesgame.core.engine;

import com.velvet.couplesgame.core.model.TaskCard;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Shared prompt dispatcher for Quick Spark, Couple Board, Wheel, Minesweeper, and penalties.
 * Uses a rolling recent-card shield and heat-weighted selection to eliminate repetitive draws.
 */
public class TaskEngine {
    public static final int DEFAULT_SKIP_QUOTA = 3;
    private static final int RECENT_CARD_SHIELD = 36;
    /** 回收牌堆时至少保留的可用卡数，避免小题库因全量 shield 而退化成完全随机重抽。 */
    private static final int MIN_RECYCLE_AVAILABLE = 4;
    private static final int PROCEDURAL_MIX_CHANCE_PERCENT = 45;

    private final ConsentFilter consentFilter;
    private final ProceduralTaskGenerator proceduralGenerator;
    private final Set<String> usedCardIds = new HashSet<>();
    private final Deque<String> recentCardIds = new ArrayDeque<>();
    private static final String HISTORY_BUCKET = "task_engine_cards";
    private final Random random;
    private PromptHistoryStore historyStore;

    private int playerASkipsLeft = DEFAULT_SKIP_QUOTA;
    private int playerBSkipsLeft = DEFAULT_SKIP_QUOTA;

    public TaskEngine(ConsentFilter consentFilter, ProceduralTaskGenerator proceduralGenerator) {
        this(consentFilter, proceduralGenerator, new Random());
    }

    public TaskEngine(ConsentFilter consentFilter, ProceduralTaskGenerator proceduralGenerator, Random random) {
        this.consentFilter = consentFilter;
        this.proceduralGenerator = proceduralGenerator;
        this.random = random != null ? random : new Random();
    }

    public void attachHistoryStore(PromptHistoryStore store) {
        this.historyStore = store;
        if (store != null) {
            for (String id : store.getRecentList(HISTORY_BUCKET)) {
                usedCardIds.add(id);
                recentCardIds.remove(id);
                recentCardIds.addLast(id);
                while (recentCardIds.size() > RECENT_CARD_SHIELD) {
                    recentCardIds.removeFirst();
                }
            }
        }
    }

    public void resetSession() {
        usedCardIds.clear();
        recentCardIds.clear();
        playerASkipsLeft = DEFAULT_SKIP_QUOTA;
        playerBSkipsLeft = DEFAULT_SKIP_QUOTA;
    }

    /** Resets per-match skip quotas WITHOUT wiping the cross-game anti-repeat card shield. */
    public void resetSkipsOnly() {
        playerASkipsLeft = DEFAULT_SKIP_QUOTA;
        playerBSkipsLeft = DEFAULT_SKIP_QUOTA;
    }

    public int getSkipsLeft(boolean isPlayerA) {
        return isPlayerA ? playerASkipsLeft : playerBSkipsLeft;
    }

    public boolean canSkip(boolean isPlayerA) {
        return getSkipsLeft(isPlayerA) > 0;
    }

    public boolean consumeSkip(boolean isPlayerA) {
        if (!canSkip(isPlayerA)) return false;
        if (isPlayerA) {
            playerASkipsLeft--;
        } else {
            playerBSkipsLeft--;
        }
        return true;
    }

    public TaskCard drawNext(List<TaskCard> candidatePool,
                             List<TaskCard> mutualMatchBoostPool,
                             int maxTier,
                             String requiredType,
                             boolean enableProceduralMix) {
        int poolSize = candidatePool == null ? 0 : candidatePool.size();
        List<TaskCard> eligible = collectEligible(candidatePool, mutualMatchBoostPool, maxTier, requiredType, true);

        // Recycle handcrafted deck while keeping the most recent cards shielded so repeats never feel immediate.
        // For small pools the global shield window can cover every card; cap it so a usable tail remains.
        if (eligible.isEmpty() && !usedCardIds.isEmpty()) {
            usedCardIds.clear();
            int keepShielded = Math.min(recentCardIds.size(),
                    Math.max(0, poolSize - MIN_RECYCLE_AVAILABLE));
            int skip = recentCardIds.size() - keepShielded;
            int index = 0;
            for (String id : recentCardIds) {
                if (index++ >= skip) usedCardIds.add(id);
            }
            eligible = collectEligible(candidatePool, mutualMatchBoostPool, maxTier, requiredType, true);
        }

        // If the filtered pool is smaller than the shield window, release the shield cleanly
        if (eligible.isEmpty() && !usedCardIds.isEmpty()) {
            usedCardIds.clear();
            eligible = collectEligible(candidatePool, mutualMatchBoostPool, maxTier, requiredType, false);
        }

        // Mix procedural prompts (45% chance when enabled) or use as fallback when no handcrafted cards fit
        boolean triggerGenerator = eligible.isEmpty()
                || (enableProceduralMix && random.nextInt(100) < PROCEDURAL_MIX_CHANCE_PERCENT);
        if (triggerGenerator && proceduralGenerator != null) {
            TaskCard generated = proceduralGenerator.generate(maxTier, requiredType, consentFilter);
            if (generated != null) {
                return generated;
            }
        }

        if (eligible.isEmpty()) return null;

        TaskCard picked = eligible.get(random.nextInt(eligible.size()));
        recordDrawnCard(picked.id);
        return picked;
    }

    private void recordDrawnCard(String cardId) {
        if (cardId == null) return;
        usedCardIds.add(cardId);
        recentCardIds.remove(cardId);
        recentCardIds.addLast(cardId);
        while (recentCardIds.size() > RECENT_CARD_SHIELD) {
            recentCardIds.removeFirst();
        }
        if (historyStore != null) {
            historyStore.recordShown(HISTORY_BUCKET, cardId, 180);
        }
    }

    private List<TaskCard> collectEligible(List<TaskCard> candidatePool,
                                           List<TaskCard> mutualMatchBoostPool,
                                           int maxTier,
                                           String requiredType,
                                           boolean applyHeatWeight) {
        List<TaskCard> eligible = new ArrayList<>();

        // Give mutual Blind-Match items 2x weight in active game pools
        if (mutualMatchBoostPool != null) {
            for (TaskCard card : mutualMatchBoostPool) {
                if (isEligible(card, maxTier, requiredType)) {
                    eligible.add(card);
                    eligible.add(card);
                }
            }
        }

        if (candidatePool != null) {
            for (TaskCard card : candidatePool) {
                if (isEligible(card, maxTier, requiredType)) {
                    eligible.add(card);
                    // When playing at higher heat tiers, gently favor cards matching the active ceiling
                    if (applyHeatWeight && maxTier >= 3 && card.tier == maxTier) {
                        eligible.add(card);
                    }
                }
            }
        }
        return eligible;
    }

    private boolean isEligible(TaskCard card, int maxTier, String requiredType) {
        if (card == null) return false;
        if (usedCardIds.contains(card.id)) return false;
        if (card.tier > maxTier) return false;
        if (requiredType != null && !requiredType.equals(card.type)) return false;
        return consentFilter == null || consentFilter.isAllowed(card.tags);
    }
}
