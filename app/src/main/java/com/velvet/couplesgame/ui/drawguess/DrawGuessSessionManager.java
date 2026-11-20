package com.velvet.couplesgame.ui.drawguess;

import com.velvet.couplesgame.core.engine.ConsentFilter;
import com.velvet.couplesgame.core.model.DrawWord;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Pure-Java session manager for Draw & Guess.
 * Respects global ConsentFilter, rotates drawer/guesser roles, and scores each round.
 */
public class DrawGuessSessionManager {
    private final List<DrawWord> eligibleWords = new ArrayList<>();
    private final Random random;

    private int wordCursor = 0;
    private DrawWord currentWord;
    private boolean playerADrawing = true;
    private int scoreA = 0;
    private int scoreB = 0;
    private int roundsPlayed = 0;

    public DrawGuessSessionManager() {
        this(new Random());
    }

    public DrawGuessSessionManager(Random random) {
        this.random = random;
    }

    public void loadWords(List<DrawWord> source, ConsentFilter filter, int maxTier) {
        eligibleWords.clear();
        if (source != null) {
            for (DrawWord w : source) {
                if (w.tier <= maxTier && (filter == null || filter.isAllowed(w.tags))) {
                    eligibleWords.add(w);
                }
            }
        }
        Collections.shuffle(eligibleWords, random);
        wordCursor = 0;
    }

    public DrawWord drawNextWord() {
        if (eligibleWords.isEmpty()) return null;
        if (wordCursor >= eligibleWords.size()) {
            Collections.shuffle(eligibleWords, random);
            wordCursor = 0;
        }
        currentWord = eligibleWords.get(wordCursor++);
        return currentWord;
    }

    public DrawWord getCurrentWord() {
        return currentWord;
    }

    public boolean isPlayerADrawing() {
        return playerADrawing;
    }

    public int getScoreA() {
        return scoreA;
    }

    public int getScoreB() {
        return scoreB;
    }

    public int getRoundsPlayed() {
        return roundsPlayed;
    }

    public void markRoundGuessed() {
        // Both partners celebrate — drawer & guesser gain a point, then swap roles
        scoreA++;
        scoreB++;
        roundsPlayed++;
        playerADrawing = !playerADrawing;
    }

    public void markRoundMissed() {
        roundsPlayed++;
        playerADrawing = !playerADrawing;
    }
}
