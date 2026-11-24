package com.velvet.couplesgame.ui.roleplay;

import com.velvet.couplesgame.core.engine.ConsentFilter;
import com.velvet.couplesgame.core.engine.PromptHistoryStore;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.RoleplayActDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.RoleplayScenarioDto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Pure-Java 3-Act Guided Roleplay runner with Dominant/Lead vs Partner role swapping
 * and shuffled unseen-first scenario ordering.
 */
public class RoleplayEngine {
    private static final String HISTORY_BUCKET = "roleplay_scenarios";
    private final List<RoleplayScenarioDto> availableScenarios = new ArrayList<>();
    private final Random random;
    private PromptHistoryStore historyStore;
    private RoleplayScenarioDto activeScenario;
    private int currentActIndex = 0;
    private boolean leadIsPlayerA = true;
    private int completedScenarios = 0;

    public RoleplayEngine() {
        this(new Random());
    }

    public RoleplayEngine(Random random) {
        this.random = random != null ? random : new Random();
    }

    public void attachHistoryStore(PromptHistoryStore store) {
        this.historyStore = store;
    }

    public void loadScenarios(List<RoleplayScenarioDto> source, ConsentFilter filter, int maxTier) {
        availableScenarios.clear();
        Set<String> played = historyStore != null ? historyStore.getRecentSet(HISTORY_BUCKET) : Collections.emptySet();
        List<RoleplayScenarioDto> fresh = new ArrayList<>();
        List<RoleplayScenarioDto> seen = new ArrayList<>();
        if (source != null) {
            for (RoleplayScenarioDto s : source) {
                if (s.tier <= maxTier && (filter == null || filter.isAllowed(s.tags))) {
                    if (played.contains(s.id)) seen.add(s);
                    else fresh.add(s);
                }
            }
        }
        Collections.shuffle(fresh, random);
        Collections.shuffle(seen, random);
        availableScenarios.addAll(fresh);
        availableScenarios.addAll(seen);
    }

    public List<RoleplayScenarioDto> getAvailableScenarios() {
        return Collections.unmodifiableList(availableScenarios);
    }

    public void startScenario(RoleplayScenarioDto scenario) {
        this.activeScenario = scenario;
        this.currentActIndex = 0;
    }

    public void exitScenario() {
        this.activeScenario = null;
        this.currentActIndex = 0;
    }

    public RoleplayScenarioDto getActiveScenario() {
        return activeScenario;
    }

    public int getCurrentActIndex() {
        return currentActIndex;
    }

    public int getTotalActs() {
        return (activeScenario != null && activeScenario.acts != null) ? activeScenario.acts.size() : 0;
    }

    public RoleplayActDto getCurrentAct() {
        if (activeScenario == null || activeScenario.acts == null
                || currentActIndex < 0 || currentActIndex >= activeScenario.acts.size()) {
            return null;
        }
        return activeScenario.acts.get(currentActIndex);
    }

    public boolean isLastAct() {
        return getTotalActs() > 0 && currentActIndex >= getTotalActs() - 1;
    }

    /** Advances to next act; returns true if the full 3-act scenario just completed. */
    public boolean advanceAct() {
        if (activeScenario == null) return false;
        if (currentActIndex + 1 < getTotalActs()) {
            currentActIndex++;
            return false;
        }
        completedScenarios++;
        if (historyStore != null && activeScenario.id != null) {
            int cap = Math.max(36, Math.min(160, availableScenarios.size() - 2));
            historyStore.recordShown(HISTORY_BUCKET, activeScenario.id, cap);
        }
        return true;
    }

    public boolean isLeadPlayerA() {
        return leadIsPlayerA;
    }

    public void swapRoles() {
        leadIsPlayerA = !leadIsPlayerA;
    }

    public int getCompletedScenarios() {
        return completedScenarios;
    }

    public String renderDirection(String rawDirection, String leadName, String partnerName) {
        if (rawDirection == null) return "";
        return rawDirection
                .replace("{lead}", leadName != null ? leadName : "Lead")
                .replace("{partner}", partnerName != null ? partnerName : "Partner");
    }
}
