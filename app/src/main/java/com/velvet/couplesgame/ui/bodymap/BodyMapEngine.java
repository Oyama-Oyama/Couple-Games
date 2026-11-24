package com.velvet.couplesgame.ui.bodymap;

import com.velvet.couplesgame.core.engine.ConsentFilter;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.BodyMapZoneDto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure-Java state machine for Secret Body Map Exploration:
 * Phase 1 (SET_TRAPS): Receiver secretly marks up to 3 Desire Hot-Spots on the stylized body silhouette.
 * Phase 2 (EXPLORE): Explorer taps body zones to perform zone-specific intimate actions;
 *                    hitting a hidden Hot-Spot triggers the amplified bold reward action!
 */
public class BodyMapEngine {
    public static final int MAX_HOT_SPOTS = 3;

    public enum Phase {
        SET_TRAPS,
        EXPLORE
    }

    public static class TapResult {
        public final BodyMapZoneDto zone;
        public final boolean wasHotSpot;
        public final boolean allHotSpotsFound;

        public TapResult(BodyMapZoneDto zone, boolean wasHotSpot, boolean allHotSpotsFound) {
            this.zone = zone;
            this.wasHotSpot = wasHotSpot;
            this.allHotSpotsFound = allHotSpotsFound;
        }
    }

    private final List<BodyMapZoneDto> zones = new ArrayList<>();
    private final Set<String> secretHotSpotIds = new HashSet<>();
    private final Set<String> exploredIds = new HashSet<>();
    private final Set<String> foundHotSpotIds = new HashSet<>();

    private Phase phase = Phase.SET_TRAPS;
    private boolean receiverIsPlayerA = false; // Player A explores Player B by default
    private boolean allFoundCelebrated = false;

    public void loadZones(List<BodyMapZoneDto> source, ConsentFilter filter, int maxTier) {
        zones.clear();
        if (source != null) {
            for (BodyMapZoneDto z : source) {
                if (z.tier <= maxTier && (filter == null || filter.isAllowed(z.tags))) {
                    zones.add(z);
                }
            }
        }
        resetRound();
    }

    public void resetRound() {
        secretHotSpotIds.clear();
        exploredIds.clear();
        foundHotSpotIds.clear();
        allFoundCelebrated = false;
        phase = Phase.SET_TRAPS;
    }

    public List<BodyMapZoneDto> getZones() {
        return Collections.unmodifiableList(zones);
    }

    public Phase getPhase() { return phase; }
    public boolean isReceiverPlayerA() { return receiverIsPlayerA; }
    public void swapRoles() { receiverIsPlayerA = !receiverIsPlayerA; }

    public int getTargetHotSpotCount() {
        return Math.min(MAX_HOT_SPOTS, Math.max(1, zones.size() / 2));
    }

    public boolean toggleSecretHotSpot(String zoneId) {
        if (phase != Phase.SET_TRAPS || zoneId == null) return false;
        if (secretHotSpotIds.contains(zoneId)) {
            secretHotSpotIds.remove(zoneId);
            return true;
        }
        if (secretHotSpotIds.size() >= getTargetHotSpotCount()) {
            return false;
        }
        secretHotSpotIds.add(zoneId);
        return true;
    }

    public boolean isSecretHotSpot(String zoneId) {
        return secretHotSpotIds.contains(zoneId);
    }

    public boolean isExplored(String zoneId) {
        return exploredIds.contains(zoneId);
    }

    public boolean isFoundHotSpot(String zoneId) {
        return foundHotSpotIds.contains(zoneId);
    }

    public int getSelectedHotSpotCount() {
        return secretHotSpotIds.size();
    }

    public int getFoundHotSpotCount() {
        return foundHotSpotIds.size();
    }

    public boolean canBeginExplore() {
        return !secretHotSpotIds.isEmpty();
    }

    public void beginExplore() {
        if (canBeginExplore()) {
            phase = Phase.EXPLORE;
        }
    }

    public TapResult exploreZone(String zoneId) {
        if (phase != Phase.EXPLORE || zoneId == null) return null;
        BodyMapZoneDto found = null;
        for (BodyMapZoneDto z : zones) {
            if (zoneId.equals(z.id)) {
                found = z;
                break;
            }
        }
        if (found == null) return null;
        exploredIds.add(zoneId);
        boolean hot = secretHotSpotIds.contains(zoneId);
        if (hot) {
            foundHotSpotIds.add(zoneId);
        }
        boolean justCompletedAll = !allFoundCelebrated && foundHotSpotIds.size() >= secretHotSpotIds.size();
        if (justCompletedAll) {
            allFoundCelebrated = true;
        }
        return new TapResult(found, hot, justCompletedAll);
    }
}
