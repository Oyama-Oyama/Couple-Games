package com.velvet.couplesgame.core.session;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.velvet.couplesgame.core.engine.ConsentFilter;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.data.local.entity.BoundaryRuleEntity;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * A 项 · 约会夜串场引擎单测（架架规格 §1 / [API-008]，AC-101~103）。
 *
 * <p>重点回归：markDone() 抽满即推进阶段的历史 bug —— 修复前 WARM_UP 抽满 4 张后
 * currentStage() 仍停在 WARM_UP，DEEPEN/PEAK/CLOSING 永不可达。
 *
 * <p>所有用例固定 Random 种子、自建卡池与 Fake 规则，可独立重复、无外部 IO。
 */
public class DateNightSessionEngineTest {

    private static final String[] ALL_TAGS = {
            "deep_talk", "playful", "eye_contact", "physical_light", "roleplay", "bite_lick"
    };

    private ConsentFilter openFilter;

    @Before
    public void setUp() {
        openFilter = new ConsentFilter();
        openFilter.replaceRules(Collections.emptyList());
    }

    private static TaskCard card(String id, int tier, String... tags) {
        return new TaskCard(id, "deck_test", tier, "TALK", Arrays.asList(tags),
                0, "text-" + id, false, false);
    }

    /** 每张卡都命中全部阶段偏好 tag，保证四个阶段的合规池都充足。 */
    private static List<TaskCard> richPool(int count) {
        List<TaskCard> cards = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            cards.add(card("rich-" + i, 1, ALL_TAGS));
        }
        return cards;
    }

    private static DateNightSessionEngine engineWith(List<TaskCard> pool, ConsentFilter filter,
                                                     int maxTier) {
        DateNightSessionEngine engine = new DateNightSessionEngine(new Random(20240520L));
        engine.loadPool(pool, filter, maxTier);
        engine.startSession(0);
        return engine;
    }

    /** 按真实 Fragment 流程抽一张并标记完成，返回抽到的卡。 */
    private static TaskCard drawAndDone(DateNightSessionEngine engine) {
        TaskCard drawn = engine.drawNext();
        if (drawn != null) {
            engine.markDone();
        }
        return drawn;
    }

    // ------------------------------------------------------------------
    // 阶段推进（核心回归）
    // ------------------------------------------------------------------

    @Test
    public void warmUpDrawsFourThenAdvancesToDeepen() {
        DateNightSessionEngine engine = engineWith(richPool(24), openFilter, 4);

        assertEquals("会话应从 WARM_UP 开始", DateNightStage.WARM_UP, engine.currentStage());
        assertEquals(4, engine.stageTotal());

        for (int i = 1; i <= 4; i++) {
            assertNotNull("暖场第 " + i + " 张不应为 null", engine.drawNext());
            engine.markDone();
        }

        // 回归点：修复前此处仍是 WARM_UP，后续三阶段永不可达。
        assertEquals("WARM_UP 抽满 4 张后必须推进到 DEEPEN（markDone 回归）",
                DateNightStage.DEEPEN, engine.currentStage());
        assertEquals(0, engine.stageProgress());
        assertFalse("刚进入 DEEPEN 时会话不应结束", engine.isFinished());
    }

    @Test
    public void allFourStagesReachableInOrder() {
        DateNightSessionEngine engine = engineWith(richPool(24), openFilter, 4);

        DateNightStage[] expected = {
                DateNightStage.WARM_UP, DateNightStage.WARM_UP,
                DateNightStage.WARM_UP, DateNightStage.WARM_UP,
                DateNightStage.DEEPEN, DateNightStage.DEEPEN,
                DateNightStage.DEEPEN, DateNightStage.DEEPEN,
                DateNightStage.PEAK, DateNightStage.PEAK, DateNightStage.PEAK,
                DateNightStage.CLOSING, DateNightStage.CLOSING
        };

        for (int i = 0; i < expected.length; i++) {
            assertEquals("第 " + (i + 1) + " 张应处于 " + expected[i],
                    expected[i], engine.currentStage());
            assertNotNull("第 " + (i + 1) + " 张不应为 null", drawAndDone(engine));
        }
        assertEquals("13 张抽完后仍应停留在 CLOSING", DateNightStage.CLOSING, engine.currentStage());
    }

    @Test
    public void totalCardsEqualToThirteen() {
        DateNightSessionEngine engine = engineWith(richPool(24), openFilter, 4);

        int drawn = 0;
        while (!engine.isFinished() && drawn < 100) {
            if (engine.drawNext() == null) break;
            drawn++;
            engine.markDone();
        }

        assertEquals("4+4+3+2 应恰好 13 张", 13, drawn);
        assertTrue("13 张全部完成后才结束", engine.isFinished());
        assertNull("会话结束后不应再产出卡片", engine.drawNext());
    }

    @Test
    public void peakStageHasNoCountdown() {
        assertEquals(0, DateNightStage.PEAK.durationSec);
        assertFalse("PEAK 阶段刻意不设倒计时（架架 §4 N3）", DateNightStage.PEAK.isTimed());
        assertEquals(3, DateNightStage.PEAK.cardCount);
    }

    @Test
    public void stageProgressMatchesEnumCardCount() {
        DateNightSessionEngine engine = engineWith(richPool(24), openFilter, 4);

        assertEquals(0, engine.stageProgress());
        assertEquals(DateNightStage.WARM_UP.cardCount, engine.stageTotal());
        drawAndDone(engine);
        assertEquals(1, engine.stageProgress());
        assertEquals(4, DateNightSessionEngine.stages().size());
        assertEquals("统计键必须独立于 GameModeCatalog", "DATE_NIGHT",
                DateNightSessionEngine.SESSION_KEY);
    }

    // ------------------------------------------------------------------
    // 抽卡约束
    // ------------------------------------------------------------------

    @Test
    public void hardNoTagIsNeverDrawn() {
        List<TaskCard> pool = new ArrayList<>();
        // 含 HARD_NO tag 的卡数量更多，确保引擎不能靠「碰巧抽不到」蒙混过关。
        for (int i = 0; i < 20; i++) {
            pool.add(card("blocked-" + i, 1, ALL_TAGS));
        }
        for (int i = 0; i < 20; i++) {
            pool.add(card("clean-" + i, 1,
                    "deep_talk", "playful", "eye_contact", "physical_light", "roleplay"));
        }
        ConsentFilter filter = new ConsentFilter();
        filter.replaceRules(Collections.singletonList(
                new BoundaryRuleEntity("bite_lick", "HARD_NO", "OK")));
        assertTrue("HARD_NO 规则应生效", filter.getHardBlockedTagsSnapshot().contains("bite_lick"));

        DateNightSessionEngine engine = engineWith(pool, filter, 4);
        int drawn = 0;
        while (!engine.isFinished() && drawn < 100) {
            TaskCard current = engine.drawNext();
            if (current == null) break;
            assertFalse("HARD_NO tag 被抽到：" + current.id, current.tags.contains("bite_lick"));
            drawn++;
            engine.markDone();
        }
        assertEquals(13, drawn);
    }

    @Test
    public void tierRespectsStageMaxAndPlayerMax() {
        List<TaskCard> pool = new ArrayList<>();
        for (int i = 0; i < 16; i++) pool.add(card("low-" + i, 1, ALL_TAGS));
        for (int i = 0; i < 16; i++) pool.add(card("high-" + i, 4, ALL_TAGS));

        DateNightSessionEngine engine = engineWith(pool, openFilter, 2);
        assertEquals(2, engine.effectiveMaxTier());

        int drawn = 0;
        while (!engine.isFinished() && drawn < 100) {
            TaskCard current = engine.drawNext();
            if (current == null) break;
            int cap = Math.min(engine.currentStage().maxTier, 2);
            assertTrue("tier 不得超过 min(stage.maxTier, playerMaxTier)：" + current.id,
                    current.tier <= cap);
            drawn++;
            engine.markDone();
        }
        assertEquals(13, drawn);
    }

    @Test
    public void preferredTagsAreHonored() {
        List<TaskCard> pool = new ArrayList<>();
        for (int i = 0; i < 12; i++) pool.add(card("pref-" + i, 1, ALL_TAGS));
        // 不匹配任何阶段偏好的卡：合规池充足时不应被抽到（fallback 不触发）。
        for (int i = 0; i < 12; i++) pool.add(card("neutral-" + i, 1, "neutral"));

        DateNightSessionEngine engine = engineWith(pool, openFilter, 4);
        int drawn = 0;
        while (!engine.isFinished() && drawn < 100) {
            TaskCard current = engine.drawNext();
            if (current == null) break;
            assertFalse("偏好池充足时不应抽到无偏好 tag 的卡：" + current.id,
                    current.id.startsWith("neutral"));
            boolean hit = false;
            for (String tag : current.tags) {
                if (engine.currentStage().preferredTags.contains(tag)) hit = true;
            }
            assertTrue("抽到的卡应命中当前阶段偏好 tag：" + current.id, hit);
            drawn++;
            engine.markDone();
        }
        assertEquals(13, drawn);
    }

    @Test
    public void recentShieldPreventsImmediateRepeat() {
        DateNightSessionEngine engine = engineWith(richPool(24), openFilter, 4);

        Set<String> drawnIds = new HashSet<>();
        int drawn = 0;
        while (!engine.isFinished() && drawn < 100) {
            TaskCard current = engine.drawNext();
            if (current == null) break;
            assertTrue("会话内 13 张不应立即重复：" + current.id, drawnIds.add(current.id));
            drawn++;
            engine.markDone();
        }
        assertEquals(13, drawn);
        assertEquals(13, drawnIds.size());
    }

    // ------------------------------------------------------------------
    // 降级 / 边界
    // ------------------------------------------------------------------

    @Test
    public void poolEmptyFallsBackWithoutNull() {
        List<TaskCard> pool = new ArrayList<>();
        // tier 超上限：主池空；neutral 卡 tier 合规但不命中偏好 → 需走第 2 步降档回退。
        for (int i = 0; i < 12; i++) {
            pool.add(card("tooHot-" + i, 4, ALL_TAGS));
        }
        for (int i = 0; i < 8; i++) {
            pool.add(card("neutral-" + i, 1, "neutral"));
        }

        DateNightSessionEngine engine = engineWith(pool, openFilter, 2);
        TaskCard current = engine.drawNext();

        assertNotNull("池不足时应降档回退到 ≥4 张可用卡，不得返回 null", current);
        assertTrue("回退不得抬高 tier 上限", current.tier <= 2);
        assertTrue("回退卡应来自保留的 neutral 池", current.id.startsWith("neutral"));
    }

    @Test
    public void fullyEmptyPoolReturnsNullWithoutException() {
        DateNightSessionEngine engine = engineWith(Collections.emptyList(), openFilter, 4);

        assertNull("全局无合规卡时返回 null 由 UI 降级提示（[API-008]）", engine.drawNext());
        assertTrue(engine.isFinished());
    }

    @Test
    public void midSessionExitDoesNotCorruptState() {
        DateNightSessionEngine engine = engineWith(richPool(24), openFilter, 4);
        for (int i = 0; i < 3; i++) {
            assertNotNull(drawAndDone(engine));
        }
        int completedBeforeExit = engine.completedCount();

        DateNightSessionEngine.SessionResult result = engine.finishSession();
        assertEquals(3, result.completed);
        assertTrue(engine.isFinished());

        assertNull("中途退出后不应再产出新卡", engine.drawNext());
        assertEquals("退出不得篡改已完成数", completedBeforeExit, engine.completedCount());
        assertFalse("退出后 currentCard 为空，markDone 不应误计数", engine.markDone());
    }

    @Test
    public void finishSessionReportsCountsForBothPartners() {
        DateNightSessionEngine engine = engineWith(richPool(24), openFilter, 4);

        drawAndDone(engine);                 // 完成 1
        assertNotNull(engine.drawNext());
        engine.markSkipped();                // 跳过 1
        drawAndDone(engine);                 // 完成 2

        DateNightSessionEngine.SessionResult result = engine.finishSession();
        assertEquals(2, result.completed);
        assertEquals(1, result.skipped);
        assertEquals("A/B 完成数一致（沿用 DailyReveal 语义）", 2, result.completedForPartnerA());
        assertEquals("A/B 完成数一致（沿用 DailyReveal 语义）", 2, result.completedForPartnerB());
    }
}
