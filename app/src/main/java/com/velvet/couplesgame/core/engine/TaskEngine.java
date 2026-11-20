package com.velvet.couplesgame.core.engine;

import com.velvet.couplesgame.core.model.TaskCard;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Shared prompt dispatcher for Chess, Blind Match, Wheel, Minesweeper, and Draw & Guess.
 */
public class TaskEngine {
    public static final int DEFAULT_SKIP_QUOTA = 3;

    private final ConsentFilter consentFilter;
    private final ProceduralTaskGenerator proceduralGenerator;
    private final Set<String> usedCardIds = new HashSet<>();
    private final Random random;

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

    public void resetSession() {
        usedCardIds.clear();
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
        List<TaskCard> eligible = collectEligible(candidatePool, mutualMatchBoostPool, maxTier, requiredType);

        // Recycle handcrafted deck BEFORE falling back to procedural generator so curated cards stay in rotation
        if (eligible.isEmpty() && !usedCardIds.isEmpty()) {
            usedCardIds.clear();
            eligible = collectEligible(candidatePool, mutualMatchBoostPool, maxTier, requiredType);
        }

        // Mix procedural offline prompts (30% chance) or use as fallback when no handcrafted cards fit
        boolean triggerGenerator = eligible.isEmpty() || (enableProceduralMix && random.nextInt(100) < 30);
        if (triggerGenerator && proceduralGenerator != null) {
            TaskCard generated = proceduralGenerator.generate(maxTier, requiredType, consentFilter);
            if (generated != null) {
                return generated;
            }
        }

        if (eligible.isEmpty()) return null;

        TaskCard picked = eligible.get(random.nextInt(eligible.size()));
        usedCardIds.add(picked.id);
        return picked;
    }

    private List<TaskCard> collectEligible(List<TaskCard> candidatePool,
                                           List<TaskCard> mutualMatchBoostPool,
                                           int maxTier,
                                           String requiredType) {
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
