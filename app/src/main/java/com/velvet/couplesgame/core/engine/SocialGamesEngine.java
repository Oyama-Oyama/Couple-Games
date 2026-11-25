package com.velvet.couplesgame.core.engine;

import com.velvet.couplesgame.core.model.TaskCard;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Config-driven engine powering the 12 social games sourced from comparable couple apps.
 *
 * <p>Design goals:
 * <ul>
 *   <li>Pure Java (no Android deps) so it is fully unit-testable.</li>
 *   <li>Reuses the existing 8-language official card pool -> no new translation risk.</li>
 *   <li>Consent-first: every card passes {@link ConsentFilter} and the player's max tier.</li>
 *   <li>Anti-repeat: adaptive recent shield that always keeps a usable pool.</li>
 * </ul>
 *
 * <p>Interaction styles are expressed by {@link Interaction} so the shared host can preserve
 * each mode's intended play pattern:
 * <ul>
 *   <li>{@code PRIVATE_ANSWERS}/{@code PRIVATE_CHOICES} - answer in turns and reveal together.</li>
 *   <li>{@code WISH_LIST} - save a shared wish locally.</li>
 *   <li>{@code SHARED_COUNTDOWN} - start and stop one local countdown together.</li>
 *   <li>{@code TIMED_TASK}/{@code MISSION} - complete a timed prompt or private mission.</li>
 * </ul>
 */
public class SocialGamesEngine {

    public enum Interaction {
        PRIVATE_ANSWERS,
        PRIVATE_CHOICES,
        TIMED_PRIVATE_ANSWERS,
        WISH_LIST,
        SHARED_COUNTDOWN,
        TIMED_TASK,
        MISSION
    }

    /** Static per-mode configuration; tag preference biases which official cards are drawn. */
    public static final class ModeConfig {
        public final String id;
        public final Interaction interaction;
        public final int tierCap;
        public final List<String> preferredTags;
        public final int roundSize;
        public final int durationSec;

        ModeConfig(String id, Interaction interaction, int tierCap, int roundSize,
                   int durationSec, String... preferredTags) {
            this.id = id;
            this.interaction = interaction;
            this.tierCap = tierCap;
            this.roundSize = roundSize;
            this.durationSec = durationSec;
            List<String> tags = new ArrayList<>();
            Collections.addAll(tags, preferredTags);
            this.preferredTags = Collections.unmodifiableList(tags);
        }
    }

    private static final Map<String, ModeConfig> CONFIGS = new HashMap<>();
    static {
        // Warm-up / flirty (free)
        CONFIGS.put("conversation_starter", new ModeConfig("conversation_starter", Interaction.PRIVATE_ANSWERS,
                2, 6, 0, "deep_talk", "playful"));
        CONFIGS.put("love_language", new ModeConfig("love_language", Interaction.PRIVATE_CHOICES,
                2, 6, 0, "deep_talk", "eye_contact"));
        CONFIGS.put("daily_question", new ModeConfig("daily_question", Interaction.PRIVATE_ANSWERS,
                2, 5, 0, "deep_talk"));
        CONFIGS.put("riddle_me", new ModeConfig("riddle_me", Interaction.PRIVATE_ANSWERS,
                2, 6, 0, "playful", "deep_talk"));
        CONFIGS.put("memory_lane", new ModeConfig("memory_lane", Interaction.PRIVATE_ANSWERS,
                2, 6, 0, "deep_talk", "indoor"));
        CONFIGS.put("most_likely_to", new ModeConfig("most_likely_to", Interaction.PRIVATE_CHOICES,
                2, 6, 0, "playful", "deep_talk"));
        CONFIGS.put("bucket_list", new ModeConfig("bucket_list", Interaction.WISH_LIST,
                3, 6, 0, "deep_talk", "indoor"));
        CONFIGS.put("ldr_sync", new ModeConfig("ldr_sync", Interaction.SHARED_COUNTDOWN,
                3, 5, 30, "eye_contact", "deep_talk"));
        CONFIGS.put("forfeit_wheel", new ModeConfig("forfeit_wheel", Interaction.TIMED_TASK,
                3, 5, 20, "playful", "physical_light", "timed"));
        // VIP / spicier
        CONFIGS.put("scenario_choice", new ModeConfig("scenario_choice", Interaction.PRIVATE_ANSWERS,
                3, 6, 0, "roleplay", "deep_talk"));
        CONFIGS.put("hot_seat", new ModeConfig("hot_seat", Interaction.TIMED_PRIVATE_ANSWERS,
                3, 3, 15, "timed", "playful"));
        CONFIGS.put("secret_mission", new ModeConfig("secret_mission", Interaction.MISSION,
                4, 6, 0, "physical_light", "indoor", "roleplay"));
    }

    private static final int MAX_RECENT_SHIELD = 12;
    private static final int MIN_AVAILABLE = 4;

    private final Random random;
    private final String modeId;
    private final ModeConfig config;
    private final Deque<String> recentIds = new ArrayDeque<>();
    private final Set<String> seenThisSession = new HashSet<>();

    private List<TaskCard> eligible = new ArrayList<>();
    private TaskCard currentCard;
    private int roundIndex;
    private int completedInRound;

    public SocialGamesEngine(String modeId) {
        this(modeId, new Random());
    }

    public SocialGamesEngine(String modeId, Random random) {
        this.random = random != null ? random : new Random();
        this.modeId = modeId;
        ModeConfig c = CONFIGS.get(modeId);
        this.config = c != null ? c : CONFIGS.get("daily_question");
    }

    public ModeConfig getConfig() { return config; }
    public String getModeId() { return modeId; }
    public TaskCard getCurrentCard() { return currentCard; }
    public int getRoundIndex() { return roundIndex; }
    public int getCompletedInRound() { return completedInRound; }
    public int getEligibleCount() { return eligible.size(); }

    /**
     * Loads and filters the official card pool. Cards are eligible when:
     * tier &lt;= min(config.tierCap, playerMaxTier) AND consent allows every tag AND
     * (preferredTags empty OR card shares at least one preferred tag).
     */
    public void loadPool(List<TaskCard> allCards, ConsentFilter consentFilter, int playerMaxTier) {
        List<TaskCard> result = new ArrayList<>();
        if (allCards == null) {
            this.eligible = result;
            return;
        }
        int cap = Math.min(config.tierCap, Math.max(1, playerMaxTier));
        for (TaskCard card : allCards) {
            if (card == null || card.tier > cap) continue;
            if (consentFilter != null && !consentFilter.isAllowed(card.tags)) continue;
            if (!matchesPreference(card)) continue;
            result.add(card);
        }
        this.eligible = result;
        resetRound();
    }

    private boolean matchesPreference(TaskCard card) {
        if (config.preferredTags.isEmpty()) return true;
        for (String tag : card.tags) {
            if (config.preferredTags.contains(tag)) return true;
        }
        return false;
    }

    public void resetRound() {
        roundIndex = 0;
        completedInRound = 0;
        currentCard = null;
        recentIds.clear();
        seenThisSession.clear();
    }

    /** Draws the next card, avoiding the most recently shown cards. Returns null on empty pool. */
    public TaskCard drawNext() {
        if (eligible.isEmpty()) {
            currentCard = null;
            return null;
        }

        int shieldSize = Math.min(MAX_RECENT_SHIELD,
                Math.max(0, eligible.size() - MIN_AVAILABLE));
        Set<String> shielded = new HashSet<>();
        int count = 0;
        for (String id : recentIds) {
            if (count++ >= shieldSize) break;
            shielded.add(id);
        }

        List<TaskCard> fresh = new ArrayList<>();
        List<TaskCard> fallback = new ArrayList<>();
        for (TaskCard card : eligible) {
            if (shielded.contains(card.id)) {
                fallback.add(card);
            } else {
                fresh.add(card);
            }
        }
        List<TaskCard> pool = fresh.isEmpty() ? fallback : fresh;
        if (pool.isEmpty()) {
            currentCard = null;
            return null;
        }

        // Slight bias towards not-yet-seen cards, but never hard-block (keeps small pools playable)
        List<TaskCard> unseen = new ArrayList<>();
        for (TaskCard card : pool) {
            if (!seenThisSession.contains(card.id)) unseen.add(card);
        }
        List<TaskCard> drawFrom = !unseen.isEmpty() ? unseen : pool;

        currentCard = drawFrom.get(random.nextInt(drawFrom.size()));
        recentIds.addFirst(currentCard.id);
        while (recentIds.size() > MAX_RECENT_SHIELD) recentIds.removeLast();
        seenThisSession.add(currentCard.id);

        roundIndex++;
        // NOTE: completedInRound is intentionally NOT reset here. It counts prompts completed
        // within the current round and is cleared only by resetRound(). Resetting on every draw
        // made markDone() never reach roundSize in the real Fragment flow, so history was rarely
        // written unless the player happened to draw exactly roundSize cards.
        return currentCard;
    }

    /** Marks the current card complete. Returns true if the round target has been reached. */
    public boolean markDone() {
        if (currentCard == null) return false;
        completedInRound++;
        return completedInRound >= Math.max(1, config.roundSize);
    }

    /** True when the current interaction should run a countdown. */
    public boolean isTimed() {
        return config.interaction == Interaction.TIMED_TASK
                || config.interaction == Interaction.TIMED_PRIVATE_ANSWERS;
    }

    public int getDurationSec() {
        return config.durationSec;
    }

    public static ModeConfig configFor(String modeId) {
        return CONFIGS.get(modeId);
    }

    public static Set<String> supportedModeIds() {
        return Collections.unmodifiableSet(CONFIGS.keySet());
    }
}
