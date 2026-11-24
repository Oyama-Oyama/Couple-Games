package com.velvet.couplesgame.ui.ladder;

import com.velvet.couplesgame.core.engine.ConsentFilter;
import com.velvet.couplesgame.core.engine.PromptHistoryStore;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.LadderStepDto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

/**
 * Pure-Java 10-Step Desire Ladder ("Truth & Continuous Foreplay").
 * Groups multi-variation steps by rung (1..10) and rotates unseen variations across runs.
 */
public class DesireLadderEngine {
    private static final String HISTORY_BUCKET = "desire_ladder";

    private final List<LadderStepDto> allEligible = new ArrayList<>();
    private final List<LadderStepDto> steps = new ArrayList<>();
    private final Set<String> seenThisCycle = new HashSet<>();
    private final Random random;
    private PromptHistoryStore historyStore;
    private int currentIndex = 0;
    private boolean teaserIsPlayerA = true;
    private int summitsReached = 0;

    public DesireLadderEngine() {
        this(new Random());
    }

    public DesireLadderEngine(Random random) {
        this.random = random != null ? random : new Random();
    }

    public void attachHistoryStore(PromptHistoryStore store) {
        this.historyStore = store;
        if (store != null) {
            seenThisCycle.addAll(store.getRecentSet(HISTORY_BUCKET));
        }
    }

    public void loadSteps(List<LadderStepDto> source, ConsentFilter filter) {
        allEligible.clear();
        if (source != null) {
            for (LadderStepDto s : source) {
                if (filter == null || filter.isAllowed(s.tags)) {
                    allEligible.add(s);
                }
            }
        }
        buildFreshClimb();
    }

    private void buildFreshClimb() {
        steps.clear();
        currentIndex = 0;
        if (allEligible.isEmpty()) return;

        Map<Integer, List<LadderStepDto>> byStep = new TreeMap<>();
        for (LadderStepDto s : allEligible) {
            byStep.computeIfAbsent(s.step, k -> new ArrayList<>()).add(s);
        }

        Set<String> recent = new HashSet<>(seenThisCycle);
        if (historyStore != null) {
            recent.addAll(historyStore.getRecentSet(HISTORY_BUCKET));
        }

        for (List<LadderStepDto> candidates : byStep.values()) {
            List<LadderStepDto> fresh = new ArrayList<>();
            for (LadderStepDto c : candidates) {
                if (!recent.contains(stepKey(c))) fresh.add(c);
            }
            if (fresh.isEmpty()) {
                for (LadderStepDto c : candidates) {
                    seenThisCycle.remove(stepKey(c));
                }
                fresh = candidates;
            }
            LadderStepDto picked = fresh.get(random.nextInt(fresh.size()));
            String key = stepKey(picked);
            steps.add(picked);
            seenThisCycle.add(key);
            if (historyStore != null) {
                historyStore.recordShown(HISTORY_BUCKET, key, Math.max(40, Math.min(200, allEligible.size() - 5)));
            }
        }
    }

    private String stepKey(LadderStepDto s) {
        String act = s.foreplayAction != null ? s.foreplayAction : "";
        String truth = s.truthQuestion != null ? s.truthQuestion : "";
        return s.step + ":" + act.hashCode() + ":" + truth.hashCode();
    }

    public LadderStepDto getCurrentStep() {
        if (currentIndex < 0 || currentIndex >= steps.size()) return null;
        return steps.get(currentIndex);
    }

    public int getCurrentIndex() { return currentIndex; }
    public int getTotalSteps() { return steps.size(); }
    public boolean isTeaserPlayerA() { return teaserIsPlayerA; }
    public void swapRoles() { teaserIsPlayerA = !teaserIsPlayerA; }
    public boolean isSummit() { return !steps.isEmpty() && currentIndex >= steps.size() - 1; }

    public boolean climbNext() {
        if (steps.isEmpty()) return false;
        if (currentIndex + 1 < steps.size()) {
            currentIndex++;
            teaserIsPlayerA = !teaserIsPlayerA;
            return false;
        }
        summitsReached++;
        buildFreshClimb();
        return true;
    }

    public int getSummitsReached() { return summitsReached; }
}
