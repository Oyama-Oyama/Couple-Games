package com.velvet.couplesgame.ui.duel;

import com.velvet.couplesgame.core.engine.ConsentFilter;
import com.velvet.couplesgame.core.engine.PromptHistoryStore;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.NhieCardDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.PartyDuelRootDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.WyrCardDto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Pure-Java dual-mode engine for NHIE & WYR with persistent cross-session anti-repeat ordering.
 */
public class PartyDuelEngine {
    public enum Mode {
        NHIE,
        WYR
    }

    private static final String BUCKET_NHIE = "party_duel_nhie";
    private static final String BUCKET_WYR = "party_duel_wyr";

    private final List<NhieCardDto> nhiePool = new ArrayList<>();
    private final List<WyrCardDto> wyrPool = new ArrayList<>();
    private final Random random;
    private PromptHistoryStore historyStore;

    private Mode activeMode = Mode.NHIE;
    private int nhieCursor = 0;
    private int wyrCursor = 0;
    private NhieCardDto currentNhie;
    private WyrCardDto currentWyr;

    private int confessionsA = 0;
    private int confessionsB = 0;
    private int wyrMatches = 0;
    private int wyrDisagrees = 0;

    public PartyDuelEngine() {
        this(new Random());
    }

    public PartyDuelEngine(Random random) {
        this.random = random != null ? random : new Random();
    }

    public void attachHistoryStore(PromptHistoryStore store) {
        this.historyStore = store;
    }

    public void loadBundle(PartyDuelRootDto bundle, ConsentFilter filter, int maxTier) {
        nhiePool.clear();
        wyrPool.clear();
        nhieCursor = 0;
        wyrCursor = 0;

        Set<String> recentNhie = historyStore != null ? historyStore.getRecentSet(BUCKET_NHIE) : Collections.emptySet();
        Set<String> recentWyr = historyStore != null ? historyStore.getRecentSet(BUCKET_WYR) : Collections.emptySet();

        List<NhieCardDto> freshN = new ArrayList<>();
        List<NhieCardDto> seenN = new ArrayList<>();
        List<WyrCardDto> freshW = new ArrayList<>();
        List<WyrCardDto> seenW = new ArrayList<>();

        if (bundle != null) {
            if (bundle.nhie != null) {
                for (NhieCardDto c : bundle.nhie) {
                    if (c.tier <= maxTier && (filter == null || filter.isAllowed(c.tags))) {
                        if (recentNhie.contains(c.id)) seenN.add(c);
                        else freshN.add(c);
                    }
                }
            }
            if (bundle.wyr != null) {
                for (WyrCardDto c : bundle.wyr) {
                    if (c.tier <= maxTier && (filter == null || filter.isAllowed(c.tags))) {
                        if (recentWyr.contains(c.id)) seenW.add(c);
                        else freshW.add(c);
                    }
                }
            }
        }
        Collections.shuffle(freshN, random);
        Collections.shuffle(seenN, random);
        nhiePool.addAll(freshN);
        nhiePool.addAll(seenN);

        Collections.shuffle(freshW, random);
        Collections.shuffle(seenW, random);
        wyrPool.addAll(freshW);
        wyrPool.addAll(seenW);
    }

    public Mode getActiveMode() { return activeMode; }
    public void setActiveMode(Mode mode) { this.activeMode = mode != null ? mode : Mode.NHIE; }

    public NhieCardDto nextNhie() {
        if (nhiePool.isEmpty()) {
            currentNhie = null;
            return null;
        }
        if (nhieCursor >= nhiePool.size()) {
            String prevId = currentNhie != null ? currentNhie.id : null;
            Collections.shuffle(nhiePool, random);
            nhieCursor = 0;
            if (prevId != null && nhiePool.size() > 1 && prevId.equals(nhiePool.get(0).id)) {
                Collections.swap(nhiePool, 0, nhiePool.size() - 1);
            }
        }
        currentNhie = nhiePool.get(nhieCursor++);
        if (historyStore != null && currentNhie.id != null) {
            historyStore.recordShown(BUCKET_NHIE, currentNhie.id, 180);
        }
        return currentNhie;
    }

    public WyrCardDto nextWyr() {
        if (wyrPool.isEmpty()) {
            currentWyr = null;
            return null;
        }
        if (wyrCursor >= wyrPool.size()) {
            String prevId = currentWyr != null ? currentWyr.id : null;
            Collections.shuffle(wyrPool, random);
            wyrCursor = 0;
            if (prevId != null && wyrPool.size() > 1 && prevId.equals(wyrPool.get(0).id)) {
                Collections.swap(wyrPool, 0, wyrPool.size() - 1);
            }
        }
        currentWyr = wyrPool.get(wyrCursor++);
        if (historyStore != null && currentWyr.id != null) {
            historyStore.recordShown(BUCKET_WYR, currentWyr.id, 200);
        }
        return currentWyr;
    }

    public NhieCardDto getCurrentNhie() { return currentNhie; }
    public WyrCardDto getCurrentWyr() { return currentWyr; }
    public void recordConfession(boolean playerA) {
        if (playerA) confessionsA++;
        else confessionsB++;
    }
    public void recordWyrOutcome(boolean agreed) {
        if (agreed) wyrMatches++;
        else wyrDisagrees++;
    }
    public int getConfessionsA() { return confessionsA; }
    public int getConfessionsB() { return confessionsB; }
    public int getWyrMatches() { return wyrMatches; }
    public int getWyrDisagrees() { return wyrDisagrees; }
}
