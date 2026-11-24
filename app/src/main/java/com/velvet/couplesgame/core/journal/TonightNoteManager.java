package com.velvet.couplesgame.core.journal;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.velvet.couplesgame.core.profile.TogetherDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * C 项 · 「今晚一句话」的本地存档（架架规格 §3.3 / [API-010]）。
 *
 * <p><b>存储仅限本机 SharedPreferences（分键范式，非 Gson）</b>：避免新增 Gson 模型
 * 带来的 R8 混淆字段名风险（历史第十一轮踩坑）。目录结构与
 * {@code MoodCheckinManager} 平行。
 *
 * <p>不上传 / 不预审 / 不分享 / 不导出 —— 同时满足安安 RED-A6 与 RED-P，
 * 也保证 IARC 问卷「有限 UGC，无发布 / 分享」的申报口径不被破坏。
 */
public class TonightNoteManager {

    private static final String PREF_NAME = "velvet_tonight_note";
    private static final String KEY_PREFIX = "day_";
    private static final String KEY_NOTE_A = "_note_a";
    private static final String KEY_NOTE_B = "_note_b";
    private static final String KEY_MOOD = "_mood";
    private static final int MAX_NOTE = 140;

    private final SharedPreferences prefs;
    private final MutableLiveData<List<TonightNote>> notesLiveData = new MutableLiveData<>();

    public TonightNoteManager(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        refresh();
    }

    /** 按 dateIso 倒序的记录列表。 */
    public LiveData<List<TonightNote>> observeNotes() {
        return notesLiveData;
    }

    /** 今天的 ISO 日期（yyyy-MM-dd）。 */
    public String getTodayIsoDate() {
        return TogetherDateFormat.toIso(LocalDate.now());
    }

    /** 读取某一天的记录；不存在时返回 null。 */
    public TonightNote getNote(String dateIso) {
        if (dateIso == null || dateIso.trim().isEmpty()) return null;
        String base = KEY_PREFIX + dateIso.trim();
        boolean hasA = prefs.contains(base + KEY_NOTE_A);
        boolean hasB = prefs.contains(base + KEY_NOTE_B);
        boolean hasMood = prefs.contains(base + KEY_MOOD);
        if (!hasA && !hasB && !hasMood) return null;
        String a = prefs.getString(base + KEY_NOTE_A, "");
        String b = prefs.getString(base + KEY_NOTE_B, "");
        int mood = prefs.getInt(base + KEY_MOOD, -1);
        long created = prefs.getLong(base + "_ts", 0L);
        return new TonightNote(dateIso.trim(), a, b, mood, created);
    }

    /** 今天是否已经留下过内容。 */
    public boolean hasNoteToday() {
        return getNote(getTodayIsoDate()) != null;
    }

    /**
     * 保存某一天的记录。同一天重复保存**覆盖**当日记录（每人每天一句），不追加历史。
     *
     * @param dateIso   ISO 日期；非法或非 ISO 格式时拒绝写入并返回 false（不得写入半截键）
     * @param noteA     伴侣 A 的一句话，允许空串
     * @param noteB     伴侣 B 的一句话，允许空串
     * @param moodScore 心情标记 1~5；-1 表示未标
     * @return 是否写入成功
     */
    public boolean saveNote(String dateIso, String noteA, String noteB, int moodScore) {
        if (dateIso == null) return false;
        String iso = dateIso.trim();
        if (iso.isEmpty() || TogetherDateFormat.parse(iso) == null) return false;

        String base = KEY_PREFIX + iso;
        SharedPreferences.Editor editor = prefs.edit()
                .putString(base + KEY_NOTE_A, clean(noteA))
                .putString(base + KEY_NOTE_B, clean(noteB))
                .putInt(base + KEY_MOOD, clampMood(moodScore))
                .putLong(base + "_ts", System.currentTimeMillis());
        boolean ok = editor.commit();
        if (ok) refresh();
        return ok;
    }

    /** 全部记录，按 dateIso 倒序。 */
    public List<TonightNote> getAllNotes() {
        List<TonightNote> result = new ArrayList<>();
        for (String key : prefs.getAll().keySet()) {
            if (!key.startsWith(KEY_PREFIX) || !key.endsWith(KEY_MOOD)) continue;
            String iso = key.substring(KEY_PREFIX.length(), key.length() - KEY_MOOD.length());
            TonightNote note = getNote(iso);
            if (note != null) result.add(note);
        }
        Collections.sort(result, (x, y) -> y.dateIso.compareTo(x.dateIso));
        return result;
    }

    private void refresh() {
        notesLiveData.setValue(getAllNotes());
    }

    private static int clampMood(int score) {
        if (score < 1 || score > 5) return -1;
        return score;
    }

    private static String clean(String note) {
        if (note == null) return "";
        String trimmed = note.trim();
        return trimmed.length() > MAX_NOTE ? trimmed.substring(0, MAX_NOTE) : trimmed;
    }
}
