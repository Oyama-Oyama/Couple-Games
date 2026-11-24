package com.velvet.couplesgame.core.journal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.velvet.couplesgame.core.profile.TogetherDateFormat;
import com.velvet.couplesgame.testing.FakeContext;
import com.velvet.couplesgame.testing.FakeSharedPreferences;
import com.velvet.couplesgame.testing.JvmTaskExecutorRule;

import org.junit.Rule;
import org.junit.Test;

import java.time.LocalDate;
import java.util.List;

/**
 * C 项 · 「今晚一句话」本地日记单测（架架规格 §3 / [API-010]，AC-105 / AC-702）。
 *
 * <p>覆盖 SharedPreferences 分键范式、同日覆盖写、ISO 日期与心情标记。
 * UGC 仅存本机：本测试不触碰网络 / 导入导出 / 分享接口。
 */
public class TonightNoteManagerTest {

    @Rule
    public final JvmTaskExecutorRule taskExecutorRule = new JvmTaskExecutorRule();

    private static TonightNoteManager newManager(FakeSharedPreferences prefs) {
        return new TonightNoteManager(new FakeContext(prefs));
    }

    @Test
    public void emptyReadReturnsNullAndNoNotes() {
        TonightNoteManager manager = newManager(new FakeSharedPreferences());

        assertNull("空态读某一天应返回 null", manager.getNote("2024-05-20"));
        assertNull("null 日期应返回 null", manager.getNote(null));
        assertNull("空白日期应返回 null", manager.getNote("   "));
        assertTrue("空态列表应为空", manager.getAllNotes().isEmpty());
        assertFalse("空态不应有今日记录", manager.hasNoteToday());
    }

    @Test
    public void saveThenReadRoundTripPreservesAllFields() {
        TonightNoteManager manager = newManager(new FakeSharedPreferences());

        assertTrue(manager.saveNote("2024-05-20", "今天很甜", "我也觉得", 4));
        TonightNote note = manager.getNote("2024-05-20");

        assertNotNull("写入后应可读", note);
        assertEquals("2024-05-20", note.dateIso);
        assertEquals("今天很甜", note.noteA);
        assertEquals("我也觉得", note.noteB);
        assertEquals(4, note.moodScore);
        assertTrue("本地时间戳应已写入", note.createdAtMillis > 0L);
        assertFalse(note.isEmpty());
        assertTrue(note.summary().contains("今天很甜"));
    }

    @Test
    public void sameDaySaveOverwritesNotAppends() {
        FakeSharedPreferences prefs = new FakeSharedPreferences();
        TonightNoteManager manager = newManager(prefs);

        assertTrue(manager.saveNote("2024-05-20", "第一句", "旧B", 1));
        int keysAfterFirstWrite = prefs.size();
        assertTrue(manager.saveNote("2024-05-20", "第二句", "新B", 5));

        assertEquals("同日覆盖不得新增 SharedPreferences 键", keysAfterFirstWrite, prefs.size());
        assertEquals("每天每人只有一条", 1, manager.getAllNotes().size());

        TonightNote note = manager.getNote("2024-05-20");
        assertNotNull(note);
        assertEquals("第二句", note.noteA);
        assertEquals("新B", note.noteB);
        assertEquals(5, note.moodScore);
        assertFalse("不得残留第一次写入的内容", note.summary().contains("第一句"));
    }

    @Test
    public void differentDaysAreIndependentAndSortedDescending() {
        TonightNoteManager manager = newManager(new FakeSharedPreferences());

        assertTrue(manager.saveNote("2024-05-20", "A20", "B20", 2));
        assertTrue(manager.saveNote("2024-05-22", "A22", "B22", 4));
        assertTrue(manager.saveNote("2024-05-21", "A21", "B21", 3));

        List<TonightNote> all = manager.getAllNotes();
        assertEquals(3, all.size());
        assertEquals("2024-05-22", all.get(0).dateIso);
        assertEquals("2024-05-21", all.get(1).dateIso);
        assertEquals("2024-05-20", all.get(2).dateIso);
        assertEquals("不同日期内容互不影响", "A20", manager.getNote("2024-05-20").noteA);
    }

    @Test
    public void moodScoreInRangeIsPreserved() {
        TonightNoteManager manager = newManager(new FakeSharedPreferences());

        for (int score = 1; score <= 5; score++) {
            String iso = "2024-06-0" + score;
            assertTrue(manager.saveNote(iso, "A" + score, "B" + score, score));
            assertEquals("1~5 的合法心情值应原样保存：score=" + score,
                    score, manager.getNote(iso).moodScore);
        }
    }

    @Test
    public void moodScoreOutOfRangeBecomesUnset() {
        TonightNoteManager manager = newManager(new FakeSharedPreferences());

        // 当前实现与 TonightNote 文档一致：-1 表示未标；越界值不会写成 0 / 6。
        assertTrue(manager.saveNote("2024-06-10", "A", "B", 0));
        assertEquals(-1, manager.getNote("2024-06-10").moodScore);
        assertTrue(manager.saveNote("2024-06-11", "A", "B", 6));
        assertEquals(-1, manager.getNote("2024-06-11").moodScore);
        assertTrue(manager.saveNote("2024-06-12", "A", "B", -3));
        assertEquals(-1, manager.getNote("2024-06-12").moodScore);
    }

    @Test
    public void dateIsoMatchesTogetherDateFormat() {
        TonightNoteManager manager = newManager(new FakeSharedPreferences());

        assertEquals(TogetherDateFormat.toIso(LocalDate.now()), manager.getTodayIsoDate());
        assertTrue(manager.saveNote(manager.getTodayIsoDate(), "今晚", "晚安", 5));
        assertTrue("今日写入后 hasNoteToday 应为 true", manager.hasNoteToday());
    }

    @Test
    public void invalidDateIsRejectedWithoutHalfWrittenKeys() {
        FakeSharedPreferences prefs = new FakeSharedPreferences();
        TonightNoteManager manager = newManager(prefs);
        int keysBefore = prefs.size();

        assertFalse(manager.saveNote(null, "A", "B", 3));
        assertFalse(manager.saveNote("", "A", "B", 3));
        assertFalse(manager.saveNote("   ", "A", "B", 3));
        assertFalse(manager.saveNote("not-a-date", "A", "B", 3));
        assertFalse(manager.saveNote("2024-13-45", "A", "B", 3));

        assertEquals("非法日期不得写入半截键", keysBefore, prefs.size());
    }

    @Test
    public void noteTextIsTrimmedAndCappedAt140() {
        TonightNoteManager manager = newManager(new FakeSharedPreferences());
        StringBuilder tooLong = new StringBuilder();
        for (int i = 0; i < 200; i++) tooLong.append('字');

        assertTrue(manager.saveNote("2024-05-20", "  前后空格  ", tooLong.toString(), 3));
        TonightNote note = manager.getNote("2024-05-20");
        assertNotNull(note);
        assertEquals("首尾空格应被裁剪", "前后空格", note.noteA);
        assertEquals("单条最多 140 字", 140, note.noteB.length());
    }

    @Test
    public void observeNotesLiveDataUpdatesAfterSave() {
        TonightNoteManager manager = newManager(new FakeSharedPreferences());

        List<TonightNote> initial = manager.observeNotes().getValue();
        assertNotNull(initial);
        assertTrue(initial.isEmpty());

        assertTrue(manager.saveNote("2024-05-20", "A", "B", 3));
        List<TonightNote> afterSave = manager.observeNotes().getValue();
        assertNotNull(afterSave);
        assertEquals(1, afterSave.size());
        assertEquals("2024-05-20", afterSave.get(0).dateIso);
    }
}
