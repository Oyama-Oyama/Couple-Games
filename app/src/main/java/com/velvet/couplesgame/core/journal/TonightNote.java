package com.velvet.couplesgame.core.journal;

/**
 * C 项 · 「今晚一句话」的一条记录（架架规格 §3.2）。
 *
 * <p>纯 Java、无 Android 依赖。
 *
 * <p><b>明确不含字段</b>：无可分享标识、无云端 id、无「公开」标记 ——
 * 本项不支持分享 / 导出（安安 RED-A6 / RED-P），加这些字段只会制造未来越界风险。
 */
public class TonightNote {

    /** ISO 日（yyyy-MM-dd），主键的一部分。 */
    public final String dateIso;

    /** 伴侣 A 的一句话（允许空串）。 */
    public final String noteA;

    /** 伴侣 B 的一句话（允许空串）。 */
    public final String noteB;

    /** 心情标记 1~5；-1 表示未标。 */
    public final int moodScore;

    /** 本地时间戳，用于排序。 */
    public final long createdAtMillis;

    public TonightNote(String dateIso, String noteA, String noteB, int moodScore,
                       long createdAtMillis) {
        this.dateIso = dateIso != null ? dateIso : "";
        this.noteA = noteA != null ? noteA : "";
        this.noteB = noteB != null ? noteB : "";
        this.moodScore = moodScore;
        this.createdAtMillis = createdAtMillis;
    }

    /** 当天两人都没有留下内容。 */
    public boolean isEmpty() {
        return noteA.trim().isEmpty() && noteB.trim().isEmpty();
    }

    /** 折叠态用的摘要（最多两行）。 */
    public String summary() {
        String a = noteA.trim();
        String b = noteB.trim();
        if (!a.isEmpty() && !b.isEmpty()) {
            return a + "  ·  " + b;
        }
        if (!a.isEmpty()) return a;
        if (!b.isEmpty()) return b;
        return "";
    }
}
