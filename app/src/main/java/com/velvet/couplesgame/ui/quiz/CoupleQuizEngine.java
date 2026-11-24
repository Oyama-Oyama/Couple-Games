package com.velvet.couplesgame.ui.quiz;

import com.velvet.couplesgame.core.engine.ConsentFilter;
import com.velvet.couplesgame.core.engine.PromptHistoryStore;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.QuizQuestionDto;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Random;

/**
 * Pure-Java 2-phase Pass-and-Play Compatibility Quiz ("How Well Do You Know Me?"):
 * Phase 1: Subject answers 5 questions privately.
 * Phase 2: Guesser tries to predict Subject's exact answers.
 * Phase 3: Chemistry % reveal + optional spicy punishment dare for misses.
 */
public class CoupleQuizEngine {
    public static final int QUESTIONS_PER_ROUND = 5;

    public enum Phase {
        EMPTY,
        SUBJECT_ANSWERING,
        HANDOFF_TO_GUESSER,
        GUESSER_GUESSING,
        RESULTS
    }

    public static class QuizComparison {
        public final String questionText;
        public final String subjectAnswer;
        public final String guesserPick;
        public final boolean matched;

        public QuizComparison(String questionText, String subjectAnswer, String guesserPick, boolean matched) {
            this.questionText = questionText;
            this.subjectAnswer = subjectAnswer;
            this.guesserPick = guesserPick;
            this.matched = matched;
        }
    }

    private final List<QuizQuestionDto> roundQuestions = new ArrayList<>();
    private final List<Integer> subjectChoices = new ArrayList<>();
    private final List<Integer> guesserChoices = new ArrayList<>();
    private static final String HISTORY_BUCKET = "couple_quiz";
    private final Set<String> recentlyAskedIds = new HashSet<>();
    private PromptHistoryStore historyStore;
    private final Random random;

    private Phase phase = Phase.EMPTY;
    private int currentIndex = 0;
    private boolean subjectIsPlayerA = true;

    public CoupleQuizEngine() {
        this(new Random());
    }

    public CoupleQuizEngine(Random random) {
        this.random = random != null ? random : new Random();
    }

    public void attachHistoryStore(PromptHistoryStore store) {
        this.historyStore = store;
        if (store != null) {
            recentlyAskedIds.addAll(store.getRecentSet(HISTORY_BUCKET));
        }
    }

    public void startRound(List<QuizQuestionDto> source, ConsentFilter filter, int maxTier) {
        roundQuestions.clear();
        subjectChoices.clear();
        guesserChoices.clear();
        currentIndex = 0;

        List<QuizQuestionDto> eligible = new ArrayList<>();
        if (source != null) {
            for (QuizQuestionDto q : source) {
                if (q.tier <= maxTier && q.options != null && q.options.size() >= 2
                        && (filter == null || filter.isAllowed(q.tags))) {
                    eligible.add(q);
                }
            }
        }
        List<QuizQuestionDto> fresh = new ArrayList<>();
        List<QuizQuestionDto> seen = new ArrayList<>();
        for (QuizQuestionDto q : eligible) {
            if (recentlyAskedIds.contains(q.id)) seen.add(q);
            else fresh.add(q);
        }
        Collections.shuffle(fresh, random);
        Collections.shuffle(seen, random);
        int count = Math.min(QUESTIONS_PER_ROUND, eligible.size());
        for (QuizQuestionDto q : fresh) {
            if (roundQuestions.size() >= count) break;
            roundQuestions.add(q);
        }
        if (roundQuestions.size() < count) {
            recentlyAskedIds.clear();
            for (QuizQuestionDto q : seen) {
                if (roundQuestions.size() >= count) break;
                roundQuestions.add(q);
            }
        }
        for (QuizQuestionDto q : roundQuestions) {
            recentlyAskedIds.add(q.id);
            if (historyStore != null) {
                historyStore.recordShown(HISTORY_BUCKET, q.id, Math.max(60, Math.min(200, eligible.size() - QUESTIONS_PER_ROUND)));
            }
        }
        phase = roundQuestions.isEmpty() ? Phase.EMPTY : Phase.SUBJECT_ANSWERING;
    }

    public Phase getPhase() {
        return phase;
    }

    public boolean isSubjectPlayerA() {
        return subjectIsPlayerA;
    }

    public void swapSubjectRole() {
        subjectIsPlayerA = !subjectIsPlayerA;
    }

    public int getTotalQuestions() {
        return roundQuestions.size();
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public QuizQuestionDto getCurrentQuestion() {
        if ((phase == Phase.SUBJECT_ANSWERING || phase == Phase.GUESSER_GUESSING)
                && currentIndex >= 0 && currentIndex < roundQuestions.size()) {
            return roundQuestions.get(currentIndex);
        }
        return null;
    }

    public Phase recordSelection(int optionIndex) {
        if (phase == Phase.SUBJECT_ANSWERING) {
            subjectChoices.add(optionIndex);
            currentIndex++;
            if (currentIndex >= roundQuestions.size()) {
                phase = Phase.HANDOFF_TO_GUESSER;
                currentIndex = 0;
            }
        } else if (phase == Phase.GUESSER_GUESSING) {
            guesserChoices.add(optionIndex);
            currentIndex++;
            if (currentIndex >= roundQuestions.size()) {
                phase = Phase.RESULTS;
            }
        }
        return phase;
    }

    public void beginGuesserTurn() {
        if (phase == Phase.HANDOFF_TO_GUESSER) {
            currentIndex = 0;
            phase = Phase.GUESSER_GUESSING;
        }
    }

    public int getCorrectCount() {
        int hits = 0;
        int n = Math.min(subjectChoices.size(), guesserChoices.size());
        for (int i = 0; i < n; i++) {
            if (subjectChoices.get(i).equals(guesserChoices.get(i))) {
                hits++;
            }
        }
        return hits;
    }

    public int getMissCount() {
        return Math.max(0, roundQuestions.size() - getCorrectCount());
    }

    public int getChemistryPercent() {
        if (roundQuestions.isEmpty()) return 0;
        return Math.round((getCorrectCount() * 100f) / roundQuestions.size());
    }

    public List<QuizComparison> buildComparisons(String subjectName) {
        if (phase != Phase.RESULTS) return Collections.emptyList();
        List<QuizComparison> list = new ArrayList<>();
        int n = Math.min(roundQuestions.size(), Math.min(subjectChoices.size(), guesserChoices.size()));
        for (int i = 0; i < n; i++) {
            QuizQuestionDto q = roundQuestions.get(i);
            int sIdx = subjectChoices.get(i);
            int gIdx = guesserChoices.get(i);
            String sText = (sIdx >= 0 && sIdx < q.options.size()) ? q.options.get(sIdx) : "-";
            String gText = (gIdx >= 0 && gIdx < q.options.size()) ? q.options.get(gIdx) : "-";
            String renderedQ = q.prompt.replace("{p1}", subjectName);
            list.add(new QuizComparison(renderedQ, sText, gText, sIdx == gIdx));
        }
        return list;
    }
}
