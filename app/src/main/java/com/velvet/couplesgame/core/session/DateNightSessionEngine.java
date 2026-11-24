package com.velvet.couplesgame.core.session;

import com.velvet.couplesgame.core.engine.ConsentFilter;
import com.velvet.couplesgame.core.model.TaskCard;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * A 项 · 引导式「约会夜」串场的抽卡引擎（架架规格 §7 [API-008]）。
 *
 * <p><b>纯 Java、无 Android 依赖</b>，构造注入 {@link Random} 以支持确定性单测。
 *
 * <p>抽卡必须依次满足：
 * <ol>
 *   <li>tier &le; min(stage.maxTier, playerMaxTier)</li>
 *   <li>{@link ConsentFilter#isAllowed(List)} 为 true（HARD_NO 全局阻断）</li>
 *   <li>命中 stage.preferredTags 中至少一个</li>
 *   <li>不在会话级 recentIds 的 shield 窗口内</li>
 * </ol>
 * shieldSize = min(12, max(0, poolSize - 4))，与 {@code SocialGamesEngine} 同值同算法。
 *
 * <p><b>绝不通过放宽 HARD_NO 来补足卡池</b>（安安 RED-A3 / RED-B4）。
 * 池空时逐级降档（tier-1 → 最低可用档），每档至少保留 4 张可用卡；
 * 全局仍空则 {@link #drawNext()} 返回 {@code null}，由 Fragment 展示降级提示，
 * <b>不得抛异常</b>。
 */
public class DateNightSessionEngine {

    /** 与 SocialGamesEngine 同值（第六轮 ADR-002），本项不修改 SocialGamesEngine。 */
    private static final int MAX_RECENT_SHIELD = 12;

    /** 与 SocialGamesEngine 同值：始终保留 4 张可用卡。 */
    private static final int MIN_AVAILABLE = 4;

    /** 统计键：不新增 GameModeCatalog 项，仅作为 session_stats 的键使用。 */
    public static final String SESSION_KEY = "DATE_NIGHT";

    /** 会话结束结果。 */
    public static final class SessionResult {
        public final int completed;
        public final int skipped;
        public final int stagesPlayed;

        SessionResult(int completed, int skipped, int stagesPlayed) {
            this.completed = completed;
            this.skipped = skipped;
            this.stagesPlayed = stagesPlayed;
        }

        /** 双方完成张数相同（沿用 DailyReveal / SocialGameFragment 的 A=B 语义）。 */
        public int completedForPartnerA() {
            return completed;
        }

        /** 双方完成张数相同（沿用 DailyReveal / SocialGameFragment 的 A=B 语义）。 */
        public int completedForPartnerB() {
            return completed;
        }
    }

    private final Random random;
    private final List<TaskCard> allCards = new ArrayList<>();
    private final Deque<String> recentIds = new ArrayDeque<>();
    private final Set<String> seenThisSession = new HashSet<>();

    private ConsentFilter consentFilter;
    private int playerMaxTier = 2;
    private int sessionMood = 2;

    private DateNightStage stage = DateNightStage.WARM_UP;
    private int stageIndex;
    private int stageDrawCount;
    private int completedCount;
    private int skippedCount;
    private int stagesPlayed;
    private TaskCard currentCard;
    private boolean sessionStarted;
    private boolean sessionFinished;

    public DateNightSessionEngine() {
        this(new Random());
    }

    public DateNightSessionEngine(Random random) {
        this.random = random != null ? random : new Random();
    }

    // ------------------------------------------------------------------
    // Session lifecycle
    // ------------------------------------------------------------------

    /**
     * 开始一段会话。
     *
     * @param sessionMood 本次会话的心情基调（1~4），与 playerMaxTier 一起决定抽卡上限；
     *                    传 0 表示不额外限制（仅按 playerMaxTier 与 stage.maxTier 取小）。
     */
    public void startSession(int sessionMood) {
        this.sessionMood = sessionMood <= 0 ? playerMaxTier : Math.max(1, Math.min(4, sessionMood));
        this.stage = DateNightStage.WARM_UP;
        this.stageIndex = 0;
        this.stageDrawCount = 0;
        this.completedCount = 0;
        this.skippedCount = 0;
        this.stagesPlayed = 0;
        this.currentCard = null;
        this.sessionStarted = true;
        this.sessionFinished = false;
        this.recentIds.clear();
        this.seenThisSession.clear();
    }

    /** 注入官方卡池与边界过滤器（不触碰 IO，由调用方在主线程回调里设置）。 */
    public void loadPool(List<TaskCard> cards, ConsentFilter filter, int maxTier) {
        this.allCards.clear();
        if (cards != null) {
            for (TaskCard card : cards) {
                if (card != null) this.allCards.add(card);
            }
        }
        this.consentFilter = filter;
        this.playerMaxTier = Math.max(1, Math.min(4, maxTier));
        if (!sessionStarted) startSession(0);
    }

    // ------------------------------------------------------------------
    // Drawing
    // ------------------------------------------------------------------

    /**
     * 抽下一张卡。
     *
     * @return 下一张卡；池空且无法降档时返回 {@code null}（调用方必须判空）
     */
    public TaskCard drawNext() {
        if (!sessionStarted) startSession(0);
        currentCard = null;

        // 池空时有限次推进阶段重试（规格 §1.6：先降 tier、再降偏好 tag；
        // 4 个阶段全部耗尽才放弃，绝不放宽 HARD_NO）
        for (int attempt = 0; attempt < DateNightStage.stageCount(); attempt++) {
            if (sessionFinished) break;
            List<TaskCard> eligible = buildEligiblePool();
            if (eligible.isEmpty()) {
                eligible = buildFallbackPool();
            }
            if (eligible.isEmpty()) {
                // 该阶段无任何合规卡：推进到下一阶段再试（不抛异常、不放宽 HARD_NO）
                advanceStage();
                continue;
            }
            TaskCard drawn = drawFrom(eligible);
            if (drawn != null) return drawn;
            // 池非空但抽样失败：极少见，推进阶段再试
            advanceStage();
        }
        // 全阶段耗尽：返回 null，由 Fragment 展示降级提示（[API-008] 错误约定）
        sessionFinished = true;
        return null;
    }

    /** 在非空池中抽样：先剔 shield 窗口，再优先会话内未出现过的卡。 */
    private TaskCard drawFrom(List<TaskCard> eligible) {
        int shieldSize = Math.min(MAX_RECENT_SHIELD,
                Math.max(0, eligible.size() - MIN_AVAILABLE));
        Set<String> shielded = new HashSet<>();
        int counted = 0;
        for (String id : recentIds) {
            if (counted++ >= shieldSize) break;
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
        if (pool.isEmpty()) return null;

        List<TaskCard> unseen = new ArrayList<>();
        for (TaskCard card : pool) {
            if (!seenThisSession.contains(card.id)) unseen.add(card);
        }
        List<TaskCard> drawFrom = !unseen.isEmpty() ? unseen : pool;

        TaskCard picked = drawFrom.get(random.nextInt(drawFrom.size()));
        recentIds.addFirst(picked.id);
        while (recentIds.size() > MAX_RECENT_SHIELD) recentIds.removeLast();
        seenThisSession.add(picked.id);
        stageDrawCount++;
        currentCard = picked;
        return picked;
    }

    /**
     * 降档回退（规格 §1.6 的顺序：<b>先降 tier，再降偏好 tag</b>）。
     * 任一档都至少保留 MIN_AVAILABLE 张可用卡；全程不违反 HARD_NO。
     */
    private List<TaskCard> buildFallbackPool() {
        // 第 1 步：放宽 tier 上限（仍要求偏好 tag）
        for (int cap = effectiveMaxTier() - 1; cap >= 1; cap--) {
            List<TaskCard> pool = buildPool(cap, true);
            if (pool.size() >= MIN_AVAILABLE) return pool;
        }
        // 第 2 步：tier 与偏好 tag 都放宽（仍绝不违反 HARD_NO）
        for (int cap = effectiveMaxTier(); cap >= 1; cap--) {
            List<TaskCard> pool = buildPool(cap, false);
            if (pool.size() >= MIN_AVAILABLE) return pool;
        }
        return Collections.emptyList();
    }

    /** 当前可抽卡池：满足 tier 上限 + HARD_NO + 偏好标签。 */
    private List<TaskCard> buildEligiblePool() {
        int cap = effectiveMaxTier();
        return buildPool(cap, true);
    }

    private List<TaskCard> buildPool(int cap, boolean requirePreferredTag) {
        List<TaskCard> result = new ArrayList<>();
        for (TaskCard card : allCards) {
            if (card.tier > cap) continue;
            if (consentFilter != null && !consentFilter.isAllowed(card.tags)) continue;
            if (requirePreferredTag && !matchesPreference(card)) continue;
            result.add(card);
        }
        return result;
    }

    private boolean matchesPreference(TaskCard card) {
        DateNightStage s = stage;
        if (s.preferredTags.isEmpty()) return true;
        for (String tag : card.tags) {
            if (s.preferredTags.contains(tag)) return true;
        }
        return false;
    }

    /** 抽卡上限 = min(stage.maxTier, playerMaxTier, sessionMood)。 */
    public int effectiveMaxTier() {
        int cap = Math.min(stage.maxTier, Math.max(1, playerMaxTier));
        if (sessionMood > 0) cap = Math.min(cap, sessionMood);
        return Math.max(1, cap);
    }

    // ------------------------------------------------------------------
    // Progress
    // ------------------------------------------------------------------

    /**
     * 标记当前卡完成。
     *
     * <p>本阶段抽满时**立即推进到下一阶段**（规格 §1.2 的 4 阶段 Phase 机；
     * 若不在此推进，WARM_UP 抽满后 {@link #currentStage()} 仍停留在 WARM_UP，
     * DEEPEN / PEAK / CLOSING 永远进不去）。
     *
     * @return true 表示本阶段已抽满（调用方据此刷新阶段指示）
     */
    public boolean markDone() {
        if (currentCard == null) return false;
        completedCount++;
        if (stageDrawCount < stage.cardCount) return false;
        advanceStage();
        return true;
    }

    /** 跳过当前卡（计入跳过次数，不计入完成）。 */
    public void markSkipped() {
        if (currentCard != null) skippedCount++;
    }

    /** 推进到下一阶段；已是最后阶段则结束会话。 */
    private void advanceStage() {
        stagesPlayed++;
        stageIndex++;
        stageDrawCount = 0;
        if (stageIndex >= DateNightStage.stageCount()) {
            stageIndex = DateNightStage.stageCount() - 1;
            sessionFinished = true;
        } else {
            stage = DateNightStage.values()[stageIndex];
        }
    }

    /** 当前阶段。 */
    public DateNightStage currentStage() {
        return stage;
    }

    /** 当前阶段序号（0 基）。 */
    public int currentStageIndex() {
        return stageIndex;
    }

    /** 当前阶段已抽卡数。 */
    public int stageProgress() {
        return stageDrawCount;
    }

    /** 当前阶段总卡数。 */
    public int stageTotal() {
        return stage.cardCount;
    }

    /** 全场已完成张数。 */
    public int completedCount() {
        return completedCount;
    }

    /** 全场跳过张数。 */
    public int skippedCount() {
        return skippedCount;
    }

    /** 会话是否已走完全部阶段。 */
    public boolean isFinished() {
        return sessionFinished;
    }

    /** 当前卡（可能为 null）。 */
    public TaskCard currentCard() {
        return currentCard;
    }

    /** 本次会话的心情基调。 */
    public int sessionMood() {
        return sessionMood;
    }

    /** 结束会话并产出统计结果。 */
    public SessionResult finishSession() {
        sessionFinished = true;
        return new SessionResult(completedCount, skippedCount, stagesPlayed);
    }

    /** 供 UI 展示的阶段序号列表（不可变）。 */
    public static List<DateNightStage> stages() {
        return Collections.unmodifiableList(new ArrayList<>(java.util.Arrays.asList(DateNightStage.values())));
    }
}
