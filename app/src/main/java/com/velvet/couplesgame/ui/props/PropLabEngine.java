package com.velvet.couplesgame.ui.props;

import com.velvet.couplesgame.core.engine.ConsentFilter;
import com.velvet.couplesgame.core.engine.PromptHistoryStore;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.PropChallengeDto;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Pure-Java Prop & Sensory Lab engine.
 * Filters bold sensory dares strictly to props the couple actually has on hand tonight.
 */
public class PropLabEngine {
    public static final List<String> ALL_PROP_KEYS = Arrays.asList(
            "ice", "blindfold", "oil", "tie", "honey", "mirror", "feather"
    );

    private final List<PropChallengeDto> allEligible = new ArrayList<>();
    private final Set<String> selectedProps = new HashSet<>(Arrays.asList("ice", "blindfold", "oil", "mirror"));
    private static final String HISTORY_BUCKET = "prop_lab";
    private final Set<String> usedIds = new HashSet<>();
    private PromptHistoryStore historyStore;
    private final Random random;
    private PropChallengeDto currentChallenge;
    private int completedCount = 0;

    public PropLabEngine() {
        this(new Random());
    }

    public PropLabEngine(Random random) {
        this.random = random != null ? random : new Random();
    }

    public void attachHistoryStore(PromptHistoryStore store) {
        this.historyStore = store;
        if (store != null) {
            usedIds.addAll(store.getRecentSet(HISTORY_BUCKET));
        }
    }

    public void loadChallenges(List<PropChallengeDto> source, ConsentFilter filter) {
        loadChallenges(source, filter, 4);
    }

    public void loadChallenges(List<PropChallengeDto> source, ConsentFilter filter, int maxTier) {
        allEligible.clear();
        List<PropChallengeDto> consentOnly = new ArrayList<>();
        if (source != null) {
            for (PropChallengeDto c : source) {
                if (filter == null || filter.isAllowed(c.tags)) {
                    consentOnly.add(c);
                    if (c.tier <= maxTier) {
                        allEligible.add(c);
                    }
                }
            }
        }
        if (allEligible.isEmpty() && !consentOnly.isEmpty()) {
            allEligible.addAll(consentOnly);
        }
    }

    public boolean toggleProp(String propKey) {
        if (selectedProps.contains(propKey)) {
            if (selectedProps.size() <= 1) return true; // Keep at least 1 prop selected
            selectedProps.remove(propKey);
            return false;
        }
        selectedProps.add(propKey);
        return true;
    }

    public boolean isPropSelected(String propKey) {
        return selectedProps.contains(propKey);
    }

    public void removePropSelection(String propKey) {
        selectedProps.remove(propKey);
    }

    public PropChallengeDto drawNextChallenge() {
        List<PropChallengeDto> matching = collectMatching(false);
        if (matching.isEmpty()) {
            String prevId = currentChallenge != null ? currentChallenge.id : null;
            usedIds.clear();
            matching = collectMatching(true);
            if (prevId != null && matching.size() > 1) {
                matching.removeIf(item -> prevId.equals(item.id));
            }
        }
        if (matching.isEmpty()) {
            currentChallenge = null;
            return null;
        }
        currentChallenge = matching.get(random.nextInt(matching.size()));
        usedIds.add(currentChallenge.id);
        if (historyStore != null) historyStore.recordShown(HISTORY_BUCKET, currentChallenge.id, Math.max(120, Math.min(220, allEligible.size() - 4)));
        return currentChallenge;
    }

    private List<PropChallengeDto> collectMatching(boolean ignoreUsed) {
        List<PropChallengeDto> res = new ArrayList<>();
        for (PropChallengeDto c : allEligible) {
            if (!selectedProps.contains(c.propKey)) continue;
            if (!ignoreUsed && usedIds.contains(c.id)) continue;
            res.add(c);
        }
        return res;
    }

    public PropChallengeDto getCurrentChallenge() { return currentChallenge; }
    public void markCompleted() { completedCount++; }
    public int getCompletedCount() { return completedCount; }
}
