package com.velvet.couplesgame.ui.foreplay;

import com.velvet.couplesgame.core.engine.ConsentFilter;
import com.velvet.couplesgame.core.engine.PromptHistoryStore;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.ForeplayStageDto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

/**
 * Pure-Java 5-Stage Continuous Foreplay Escalator state machine respecting ConsentFilter.
 * Uses per-step full-cycle exhaustion + PromptHistoryStore persistence so every session
 * rotates unseen variations across all 5 steps without early repeats.
 */
public class ForeplayEscalatorEngine {
    private static final String HISTORY_BUCKET = "foreplay_escalator";

    private final List<ForeplayStageDto> allEligible = new ArrayList<>();
    private final List<ForeplayStageDto> stages = new ArrayList<>();
    private final Set<String> seenThisCycle = new HashSet<>();
    private final Random random;
    private PromptHistoryStore historyStore;
    private int currentIndex = 0;
    private boolean leadIsPlayerA = true;
    private int completedSessions = 0;

    public ForeplayEscalatorEngine() {
        this(new Random());
    }

    public ForeplayEscalatorEngine(Random random) {
        this.random = random != null ? random : new Random();
    }

    public void attachHistoryStore(PromptHistoryStore store) {
        this.historyStore = store;
        if (store != null) {
            seenThisCycle.addAll(store.getRecentSet(HISTORY_BUCKET));
        }
    }

    public void loadStages(List<ForeplayStageDto> source) {
        loadStages(source, null);
    }

    public void loadStages(List<ForeplayStageDto> source, ConsentFilter filter) {
        allEligible.clear();
        if (source != null) {
            for (ForeplayStageDto s : source) {
                if (filter == null || filter.isAllowed(s.tags)) {
                    allEligible.add(s);
                }
            }
        }
        buildFreshArc();
    }

    private void buildFreshArc() {
        stages.clear();
        currentIndex = 0;
        if (allEligible.isEmpty()) return;

        Map<Integer, List<ForeplayStageDto>> byStep = new TreeMap<>();
        for (ForeplayStageDto s : allEligible) {
            byStep.computeIfAbsent(s.step, k -> new ArrayList<>()).add(s);
        }

        Set<String> recent = new HashSet<>(seenThisCycle);
        if (historyStore != null) {
            recent.addAll(historyStore.getRecentSet(HISTORY_BUCKET));
        }

        for (List<ForeplayStageDto> candidates : byStep.values()) {
            List<ForeplayStageDto> fresh = new ArrayList<>();
            for (ForeplayStageDto c : candidates) {
                if (!recent.contains(stageKey(c))) fresh.add(c);
            }
            if (fresh.isEmpty()) {
                for (ForeplayStageDto c : candidates) {
                    seenThisCycle.remove(stageKey(c));
                }
                fresh = candidates;
            }
            ForeplayStageDto picked = fresh.get(random.nextInt(fresh.size()));
            String key = stageKey(picked);
            stages.add(picked);
            seenThisCycle.add(key);
            if (historyStore != null) {
                historyStore.recordShown(HISTORY_BUCKET, key, Math.max(20, Math.min(200, allEligible.size() - 5)));
            }
        }
    }

    private String stageKey(ForeplayStageDto s) {
        String title = s.stageTitle != null ? s.stageTitle : "";
        String instr = s.instruction != null ? s.instruction : "";
        return s.step + ":" + title.hashCode() + ":" + instr.hashCode();
    }

    public List<ForeplayStageDto> getStages() {
        return Collections.unmodifiableList(stages);
    }

    public ForeplayStageDto getCurrentStage() {
        if (currentIndex < 0 || currentIndex >= stages.size()) return null;
        return stages.get(currentIndex);
    }

    public int getCurrentIndex() { return currentIndex; }
    public int getTotalStages() { return stages.size(); }
    public boolean isLeadPlayerA() { return leadIsPlayerA; }
    public void swapLead() { leadIsPlayerA = !leadIsPlayerA; }
    public boolean isLastStage() { return !stages.isEmpty() && currentIndex >= stages.size() - 1; }

    public boolean nextStage() {
        if (stages.isEmpty()) return false;
        if (currentIndex + 1 < stages.size()) {
            currentIndex++;
            leadIsPlayerA = !leadIsPlayerA;
            return false;
        }
        completedSessions++;
        buildFreshArc();
        return true;
    }

    public void restart() {
        buildFreshArc();
    }

    public int getCompletedSessions() { return completedSessions; }
}
