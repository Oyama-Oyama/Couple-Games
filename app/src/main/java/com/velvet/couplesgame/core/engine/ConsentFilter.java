package com.velvet.couplesgame.core.engine;

import com.velvet.couplesgame.data.local.entity.BoundaryRuleEntity;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Thread-safe global Consent-First guard shared by all game modes.
 * Any tag marked HARD_NO by either player is strictly blocked everywhere.
 */
public class ConsentFilter {
    private final Set<String> hardBlockedTags = new CopyOnWriteArraySet<>();
    private final Set<String> softLimitTags = new CopyOnWriteArraySet<>();

    public synchronized void replaceRules(List<BoundaryRuleEntity> rules) {
        Set<String> nextHard = new HashSet<>();
        Set<String> nextSoft = new HashSet<>();
        if (rules != null) {
            for (BoundaryRuleEntity rule : rules) {
                if (rule.isBlocked()) {
                    nextHard.add(rule.tagKey);
                } else if (rule.isSoftLimit()) {
                    nextSoft.add(rule.tagKey);
                }
            }
        }
        hardBlockedTags.clear();
        hardBlockedTags.addAll(nextHard);
        softLimitTags.clear();
        softLimitTags.addAll(nextSoft);
    }

    public boolean isAllowed(Collection<String> tags) {
        if (tags == null || tags.isEmpty()) return true;
        for (String tag : tags) {
            if (hardBlockedTags.contains(tag)) {
                return false;
            }
        }
        return true;
    }

    public boolean hasSoftLimit(Collection<String> tags) {
        if (tags == null || tags.isEmpty()) return false;
        for (String tag : tags) {
            if (softLimitTags.contains(tag)) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getHardBlockedTagsSnapshot() {
        return Collections.unmodifiableSet(new HashSet<>(hardBlockedTags));
    }

    public Set<String> getSoftLimitTagsSnapshot() {
        return Collections.unmodifiableSet(new HashSet<>(softLimitTags));
    }
}
