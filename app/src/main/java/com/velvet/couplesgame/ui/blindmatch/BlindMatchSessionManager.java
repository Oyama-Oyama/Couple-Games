package com.velvet.couplesgame.ui.blindmatch;

import com.velvet.couplesgame.core.engine.ConsentFilter;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.data.local.entity.BlindMatchEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure-Java Consent-First state machine for Blind Desire Match.
 * Player A and Player B swipe blindly; ONLY mutually liked cards are ever exposed.
 */
public class BlindMatchSessionManager {
    public enum Phase {
        EMPTY,
        PLAYER_A,
        HANDOFF_TO_B,
        PLAYER_B,
        REVEAL
    }

    private final List<TaskCard> sessionCards = new ArrayList<>();
    private final Map<String, Boolean> votesA = new HashMap<>();
    private final Map<String, Boolean> votesB = new HashMap<>();
    private Phase phase = Phase.EMPTY;
    private int currentIndex = 0;

    public void startSession(List<TaskCard> sourceCards, ConsentFilter filter) {
        startSession(sourceCards, filter, 4);
    }

    public void startSession(List<TaskCard> sourceCards, ConsentFilter filter, int maxTier) {
        sessionCards.clear();
        votesA.clear();
        votesB.clear();
        currentIndex = 0;

        if (sourceCards != null) {
            for (TaskCard card : sourceCards) {
                if (card.tier <= maxTier && (filter == null || filter.isAllowed(card.tags))) {
                    sessionCards.add(card);
                }
            }
        }
        phase = sessionCards.isEmpty() ? Phase.EMPTY : Phase.PLAYER_A;
    }

    public Phase getPhase() {
        return phase;
    }

    public int getTotalCount() {
        return sessionCards.size();
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public TaskCard getCurrentCard() {
        if ((phase == Phase.PLAYER_A || phase == Phase.PLAYER_B)
                && currentIndex >= 0 && currentIndex < sessionCards.size()) {
            return sessionCards.get(currentIndex);
        }
        return null;
    }

    /** Records current player's blind swipe and advances phase automatically. */
    public Phase recordSwipe(boolean liked) {
        TaskCard card = getCurrentCard();
        if (card == null) return phase;

        if (phase == Phase.PLAYER_A) {
            votesA.put(card.id, liked);
            currentIndex++;
            if (currentIndex >= sessionCards.size()) {
                phase = Phase.HANDOFF_TO_B;
                currentIndex = 0;
            }
        } else if (phase == Phase.PLAYER_B) {
            votesB.put(card.id, liked);
            currentIndex++;
            if (currentIndex >= sessionCards.size()) {
                phase = Phase.REVEAL;
            }
        }
        return phase;
    }

    public void beginPlayerBTurn() {
        if (phase == Phase.HANDOFF_TO_B) {
            currentIndex = 0;
            phase = Phase.PLAYER_B;
        }
    }

    /** Returns strictly the cards where BOTH Player A and Player B swiped Right. */
    public List<TaskCard> getMutualMatches() {
        if (phase != Phase.REVEAL) return Collections.emptyList();
        List<TaskCard> matches = new ArrayList<>();
        for (TaskCard card : sessionCards) {
            boolean aYes = Boolean.TRUE.equals(votesA.get(card.id));
            boolean bYes = Boolean.TRUE.equals(votesB.get(card.id));
            if (aYes && bYes) {
                matches.add(card);
            }
        }
        return matches;
    }

    public List<BlindMatchEntity> buildPersistenceEntities(long timestampMs) {
        List<BlindMatchEntity> entities = new ArrayList<>();
        for (TaskCard card : sessionCards) {
            boolean aYes = Boolean.TRUE.equals(votesA.get(card.id));
            boolean bYes = Boolean.TRUE.equals(votesB.get(card.id));
            entities.add(new BlindMatchEntity(card.id, aYes, bYes, timestampMs));
        }
        return entities;
    }
}
