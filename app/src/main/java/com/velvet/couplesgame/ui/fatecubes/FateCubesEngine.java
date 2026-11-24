package com.velvet.couplesgame.ui.fatecubes;

import com.velvet.couplesgame.core.engine.ConsentFilter;
import com.velvet.couplesgame.core.engine.PromptHistoryStore;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.CubeFaceDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.CubeModifierDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.FateCubesBundleDto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Pure-Java state machine for Fate Cubes (Top Action Cube x Bottom Body-Spot Cube + Bonus Modifier).
 * Uses full-cycle shuffle-bag exhaustion for actions, spots, and modifiers PLUS cross-session
 * PromptHistoryStore persistence and (action, spot) pair deduplication so rolled options never
 * repeat frequently.
 */
public class FateCubesEngine {
    private static final String BUCKET_ACTIONS = "fate_cubes_actions";
    private static final String BUCKET_SPOTS = "fate_cubes_spots";
    private static final String BUCKET_MODS = "fate_cubes_mods";
    private static final String BUCKET_COMBOS = "fate_cubes_combos";

    private final List<CubeFaceDto> eligibleActions = new ArrayList<>();
    private final List<CubeFaceDto> eligibleSpots = new ArrayList<>();
    private final List<CubeModifierDto> eligibleModifiers = new ArrayList<>();

    private final List<CubeFaceDto> actionDeck = new ArrayList<>();
    private final List<CubeFaceDto> spotDeck = new ArrayList<>();
    private final List<CubeModifierDto> modifierDeck = new ArrayList<>();
    private int actionCursor = 0;
    private int spotCursor = 0;
    private int modifierCursor = 0;

    private final Set<String> seenCombos = new HashSet<>();
    private final Random random;
    private PromptHistoryStore historyStore;

    private CubeFaceDto selectedAction;
    private CubeFaceDto selectedSpot;
    private CubeModifierDto selectedModifier;

    private boolean giverIsPlayerA = true;
    private boolean wildModifierEnabled = true;
    private int roundsCompleted = 0;

    public FateCubesEngine() {
        this(new Random());
    }

    public FateCubesEngine(Random random) {
        this.random = random != null ? random : new Random();
    }

    public void attachHistoryStore(PromptHistoryStore store) {
        this.historyStore = store;
        if (store != null) {
            seenCombos.addAll(store.getRecentSet(BUCKET_COMBOS));
        }
    }

    public void loadBundle(FateCubesBundleDto bundle, ConsentFilter filter, int maxTier) {
        eligibleActions.clear();
        eligibleSpots.clear();
        eligibleModifiers.clear();
        actionDeck.clear();
        spotDeck.clear();
        modifierDeck.clear();
        actionCursor = 0;
        spotCursor = 0;
        modifierCursor = 0;

        if (bundle != null) {
            if (bundle.actions != null) {
                for (CubeFaceDto f : bundle.actions) {
                    if (f.tier <= maxTier && (filter == null || filter.isAllowed(f.tags))) {
                        eligibleActions.add(f);
                    }
                }
            }
            if (bundle.spots != null) {
                for (CubeFaceDto f : bundle.spots) {
                    if (f.tier <= maxTier && (filter == null || filter.isAllowed(f.tags))) {
                        eligibleSpots.add(f);
                    }
                }
            }
            if (bundle.modifiers != null) {
                for (CubeModifierDto m : bundle.modifiers) {
                    if (m.tier <= maxTier && (filter == null || filter.isAllowed(m.tags))) {
                        eligibleModifiers.add(m);
                    }
                }
            }
        }

        rebuildFaceDeck(eligibleActions, actionDeck, BUCKET_ACTIONS, null);
        rebuildFaceDeck(eligibleSpots, spotDeck, BUCKET_SPOTS, null);
        rebuildModDeck(eligibleModifiers, modifierDeck, null);
    }

    public List<CubeFaceDto> getEligibleActions() {
        return Collections.unmodifiableList(eligibleActions);
    }

    public List<CubeFaceDto> getEligibleSpots() {
        return Collections.unmodifiableList(eligibleSpots);
    }

    public CubeFaceDto rollAction() {
        if (eligibleActions.isEmpty()) return null;
        if (actionDeck.isEmpty() || actionCursor >= actionDeck.size()) {
            String prevId = selectedAction != null ? selectedAction.id : null;
            rebuildFaceDeck(eligibleActions, actionDeck, null, prevId);
            actionCursor = 0;
        }
        CubeFaceDto picked = pickNonCollidingAction();
        selectedAction = picked;
        if (historyStore != null && picked.id != null) {
            historyStore.recordShown(BUCKET_ACTIONS, picked.id, Math.max(8, eligibleActions.size() - 2));
        }
        recordComboIfReady();
        refreshModifierIfReady();
        return picked;
    }

    public CubeFaceDto rollSpot() {
        if (eligibleSpots.isEmpty()) return null;
        if (spotDeck.isEmpty() || spotCursor >= spotDeck.size()) {
            String prevId = selectedSpot != null ? selectedSpot.id : null;
            rebuildFaceDeck(eligibleSpots, spotDeck, null, prevId);
            spotCursor = 0;
        }
        CubeFaceDto picked = pickNonCollidingSpot();
        selectedSpot = picked;
        if (historyStore != null && picked.id != null) {
            historyStore.recordShown(BUCKET_SPOTS, picked.id, Math.max(8, eligibleSpots.size() - 2));
        }
        recordComboIfReady();
        refreshModifierIfReady();
        return picked;
    }

    private CubeFaceDto pickNonCollidingAction() {
        if (selectedSpot != null && actionCursor < actionDeck.size()) {
            for (int i = actionCursor; i < actionDeck.size(); i++) {
                CubeFaceDto cand = actionDeck.get(i);
                String key = cand.id + "|" + selectedSpot.id;
                if (!seenCombos.contains(key)) {
                    Collections.swap(actionDeck, actionCursor, i);
                    return actionDeck.get(actionCursor++);
                }
            }
        }
        return actionDeck.get(actionCursor++);
    }

    private CubeFaceDto pickNonCollidingSpot() {
        if (selectedAction != null && spotCursor < spotDeck.size()) {
            for (int i = spotCursor; i < spotDeck.size(); i++) {
                CubeFaceDto cand = spotDeck.get(i);
                String key = selectedAction.id + "|" + cand.id;
                if (!seenCombos.contains(key)) {
                    Collections.swap(spotDeck, spotCursor, i);
                    return spotDeck.get(spotCursor++);
                }
            }
        }
        return spotDeck.get(spotCursor++);
    }

    private void recordComboIfReady() {
        if (selectedAction != null && selectedSpot != null) {
            String key = selectedAction.id + "|" + selectedSpot.id;
            seenCombos.add(key);
            if (seenCombos.size() > 160) {
                seenCombos.clear();
                seenCombos.add(key);
            }
            if (historyStore != null) {
                historyStore.recordShown(BUCKET_COMBOS, key, 120);
            }
        }
    }

    private void rebuildFaceDeck(List<CubeFaceDto> source,
                                 List<CubeFaceDto> targetDeck,
                                 String historyBucket,
                                 String avoidFirstId) {
        targetDeck.clear();
        if (source.isEmpty()) return;

        Set<String> recent = (historyStore != null && historyBucket != null)
                ? historyStore.getRecentSet(historyBucket)
                : Collections.emptySet();

        List<CubeFaceDto> fresh = new ArrayList<>();
        List<CubeFaceDto> seen = new ArrayList<>();
        for (CubeFaceDto f : source) {
            if (recent.contains(f.id)) seen.add(f);
            else fresh.add(f);
        }
        Collections.shuffle(fresh, random);
        Collections.shuffle(seen, random);
        targetDeck.addAll(fresh);
        targetDeck.addAll(seen);

        if (avoidFirstId != null && targetDeck.size() > 1 && avoidFirstId.equals(targetDeck.get(0).id)) {
            Collections.swap(targetDeck, 0, targetDeck.size() - 1);
        }
    }

    private void rebuildModDeck(List<CubeModifierDto> source,
                                List<CubeModifierDto> targetDeck,
                                String avoidFirstId) {
        targetDeck.clear();
        if (source.isEmpty()) return;

        Set<String> recent = (historyStore != null && avoidFirstId == null)
                ? historyStore.getRecentSet(BUCKET_MODS)
                : Collections.emptySet();

        List<CubeModifierDto> fresh = new ArrayList<>();
        List<CubeModifierDto> seen = new ArrayList<>();
        for (CubeModifierDto m : source) {
            if (recent.contains(m.id)) seen.add(m);
            else fresh.add(m);
        }
        Collections.shuffle(fresh, random);
        Collections.shuffle(seen, random);
        targetDeck.addAll(fresh);
        targetDeck.addAll(seen);

        if (avoidFirstId != null && targetDeck.size() > 1 && avoidFirstId.equals(targetDeck.get(0).id)) {
            Collections.swap(targetDeck, 0, targetDeck.size() - 1);
        }
    }

    private void refreshModifierIfReady() {
        if (!isComboReady() || !wildModifierEnabled || eligibleModifiers.isEmpty()) {
            selectedModifier = null;
            return;
        }
        if (modifierDeck.isEmpty() || modifierCursor >= modifierDeck.size()) {
            String prevId = selectedModifier != null ? selectedModifier.id : null;
            rebuildModDeck(eligibleModifiers, modifierDeck, prevId);
            modifierCursor = 0;
        }
        selectedModifier = modifierDeck.get(modifierCursor++);
        if (historyStore != null && selectedModifier.id != null) {
            historyStore.recordShown(BUCKET_MODS, selectedModifier.id, Math.max(4, eligibleModifiers.size() - 2));
        }
    }

    public void setWildModifierEnabled(boolean enabled) {
        this.wildModifierEnabled = enabled;
        refreshModifierIfReady();
    }

    public boolean isWildModifierEnabled() {
        return wildModifierEnabled;
    }

    public boolean isComboReady() {
        return selectedAction != null && selectedSpot != null;
    }

    public CubeFaceDto getSelectedAction() {
        return selectedAction;
    }

    public CubeFaceDto getSelectedSpot() {
        return selectedSpot;
    }

    public CubeModifierDto getSelectedModifier() {
        return selectedModifier;
    }

    public boolean isGiverPlayerA() {
        return giverIsPlayerA;
    }

    public int getRoundsCompleted() {
        return roundsCompleted;
    }

    public void swapRoles() {
        giverIsPlayerA = !giverIsPlayerA;
    }

    public void completeRoundAndPrepareNext() {
        if (isComboReady()) {
            roundsCompleted++;
        }
        selectedAction = null;
        selectedSpot = null;
        selectedModifier = null;
        giverIsPlayerA = !giverIsPlayerA;
    }

    public void clearSelection() {
        selectedAction = null;
        selectedSpot = null;
        selectedModifier = null;
    }
}
