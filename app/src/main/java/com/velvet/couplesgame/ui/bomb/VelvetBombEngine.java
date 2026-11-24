package com.velvet.couplesgame.ui.bomb;

import com.velvet.couplesgame.core.engine.ConsentFilter;
import com.velvet.couplesgame.core.engine.PromptHistoryStore;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.SimplePromptDto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Pure-Java Hot-Potato Intimate Time Bomb engine with persistent anti-repeat relay rotation.
 */
public class VelvetBombEngine {
    public static final int MIN_FUSE_MS = 15_000;
    public static final int MAX_FUSE_MS = 38_000;
    private static final String HISTORY_BUCKET = "velvet_bomb";

    private final List<SimplePromptDto> eligibleRelays = new ArrayList<>();
    private final Random random;
    private PromptHistoryStore historyStore;

    private SimplePromptDto activeRelay;
    private int cursor = 0;
    private boolean holderIsPlayerA = true;
    private int passCount = 0;
    private long fuseDurationMs = 22_000L;
    private boolean armed = false;
    private int explosionsA = 0;
    private int explosionsB = 0;

    public VelvetBombEngine() {
        this(new Random());
    }

    public VelvetBombEngine(Random random) {
        this.random = random != null ? random : new Random();
    }

    public void attachHistoryStore(PromptHistoryStore store) {
        this.historyStore = store;
    }

    public void loadRelays(List<SimplePromptDto> source, ConsentFilter filter, int maxTier) {
        eligibleRelays.clear();
        cursor = 0;
        activeRelay = null;
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
        eligibleRelays.addAll(fresh);
        eligibleRelays.addAll(seen);
    }

    public SimplePromptDto armNewRound() {
        if (eligibleRelays.isEmpty()) {
            activeRelay = null;
            armed = false;
            return null;
        }
        activeRelay = drawNextRelayInternal();
        fuseDurationMs = MIN_FUSE_MS + random.nextInt(MAX_FUSE_MS - MIN_FUSE_MS + 1);
        passCount = 0;
        armed = true;
        return activeRelay;
    }

    public SimplePromptDto passBomb() {
        if (!armed) return activeRelay;
        passCount++;
        holderIsPlayerA = !holderIsPlayerA;
        if (!eligibleRelays.isEmpty()) {
            activeRelay = drawNextRelayInternal();
        }
        return activeRelay;
    }

    private SimplePromptDto drawNextRelayInternal() {
        if (eligibleRelays.size() == 1) {
            return eligibleRelays.get(0);
        }
        String previousId = activeRelay != null ? activeRelay.id : null;
        if (cursor >= eligibleRelays.size()) {
            Collections.shuffle(eligibleRelays, random);
            cursor = 0;
            if (previousId != null && previousId.equals(eligibleRelays.get(0).id)) {
                Collections.swap(eligibleRelays, 0, eligibleRelays.size() - 1);
            }
        }
        SimplePromptDto picked = eligibleRelays.get(cursor++);
        if (historyStore != null && picked.id != null) {
            historyStore.recordShown(HISTORY_BUCKET, picked.id, 200);
        }
        return picked;
    }

    public boolean detonate() {
        armed = false;
        if (holderIsPlayerA) explosionsA++;
        else explosionsB++;
        return holderIsPlayerA;
    }

    public boolean isArmed() { return armed; }
    public boolean isHolderPlayerA() { return holderIsPlayerA; }
    public long getFuseDurationMs() { return fuseDurationMs; }
    public int getPassCount() { return passCount; }
    public int getExplosionsA() { return explosionsA; }
    public int getExplosionsB() { return explosionsB; }
    public SimplePromptDto getActiveRelay() { return activeRelay; }
}
