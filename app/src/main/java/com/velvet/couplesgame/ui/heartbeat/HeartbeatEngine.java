package com.velvet.couplesgame.ui.heartbeat;

import com.velvet.couplesgame.core.engine.ConsentFilter;
import com.velvet.couplesgame.core.engine.PromptHistoryStore;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.SimplePromptDto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Pure-Java state machine for Dual-Finger Heartbeat Hold & Tease with persistent anti-repeat ordering.
 */
public class HeartbeatEngine {
    public static final int DEFAULT_HOLD_SECONDS = 15;
    private static final String HISTORY_BUCKET = "heartbeat_hold";

    private final List<SimplePromptDto> eligiblePrompts = new ArrayList<>();
    private final Random random;
    private PromptHistoryStore historyStore;
    private int cursor = 0;
    private SimplePromptDto currentPrompt;
    private int syncedRounds = 0;
    private int forfeitsA = 0;
    private int forfeitsB = 0;

    public HeartbeatEngine() {
        this(new Random());
    }

    public HeartbeatEngine(Random random) {
        this.random = random != null ? random : new Random();
    }

    public void attachHistoryStore(PromptHistoryStore store) {
        this.historyStore = store;
    }

    public void loadPrompts(List<SimplePromptDto> source, ConsentFilter filter, int maxTier) {
        eligiblePrompts.clear();
        cursor = 0;
        Set<String> recent = historyStore != null ? historyStore.getRecentSet(HISTORY_BUCKET) : Collections.emptySet();
        List<SimplePromptDto> fresh = new ArrayList<>();
        List<SimplePromptDto> seen = new ArrayList<>();
        if (source != null) {
            for (SimplePromptDto p : source) {
                if (p.tier <= maxTier && (filter == null || filter.isAllowed(p.tags))) {
                    if (recent.contains(p.id)) seen.add(p);
                    else fresh.add(p);
                }
            }
        }
        Collections.shuffle(fresh, random);
        Collections.shuffle(seen, random);
        eligiblePrompts.addAll(fresh);
        eligiblePrompts.addAll(seen);
    }

    public SimplePromptDto drawNextPrompt() {
        if (eligiblePrompts.isEmpty()) {
            currentPrompt = null;
            return null;
        }
        if (cursor >= eligiblePrompts.size()) {
            String prevId = currentPrompt != null ? currentPrompt.id : null;
            Collections.shuffle(eligiblePrompts, random);
            cursor = 0;
            if (prevId != null && eligiblePrompts.size() > 1 && prevId.equals(eligiblePrompts.get(0).id)) {
                Collections.swap(eligiblePrompts, 0, eligiblePrompts.size() - 1);
            }
        }
        currentPrompt = eligiblePrompts.get(cursor++);
        if (historyStore != null && currentPrompt.id != null) {
            historyStore.recordShown(HISTORY_BUCKET, currentPrompt.id, 180);
        }
        return currentPrompt;
    }

    public SimplePromptDto getCurrentPrompt() { return currentPrompt; }
    public void recordSyncSuccess() { syncedRounds++; }
    public void recordForfeit(boolean playerABroke) {
        if (playerABroke) forfeitsA++;
        else forfeitsB++;
    }
    public int getSyncedRounds() { return syncedRounds; }
    public int getForfeitsA() { return forfeitsA; }
    public int getForfeitsB() { return forfeitsB; }
}
