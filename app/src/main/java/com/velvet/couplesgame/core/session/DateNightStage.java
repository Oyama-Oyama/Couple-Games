package com.velvet.couplesgame.core.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A 项 · 引导式「约会夜」串场的四个阶段（架架规格 §1.2）。
 *
 * <p>设计原则（安安 RED-D / 主理人裁决）：阶段推进靠**情绪密度**与**节奏**，
 * 不靠动作升级。WARM_UP → DEEPEN → PEAK → CLOSING 的可感知差异是
 * 「留白更多、心跳更近、语言更轻」，不是「动作更具体」。
 *
 * <p>PEAK 阶段刻意不设倒计时（durationSec = 0）：由双方自行决定何时继续，
 * 避免「计时器把节奏变成任务」的压迫感（对应架架 §4 N3）。
 */
public enum DateNightStage {

    /** 暖场：轻松、破冰、安全。 */
    WARM_UP(2, 4, 0, "deep_talk", "playful", "eye_contact"),

    /** 深入：靠近、试探、话题变私密。 */
    DEEPEN(3, 4, 0, "deep_talk", "physical_light", "roleplay"),

    /** 心跳：悬念最强，留白最大；不设倒计时。 */
    PEAK(4, 3, 0, "physical_light", "bite_lick", "roleplay"),

    /** 收尾：回落、温柔、留余韵。 */
    CLOSING(2, 2, 0, "deep_talk", "eye_contact");

    /** 本阶段允许的最高热度档（1~4）。 */
    public final int maxTier;

    /** 本阶段抽卡张数。 */
    public final int cardCount;

    /** 本阶段单张计时秒数；0 表示不计时（PEAK）。 */
    public final int durationSec;

    /** 本阶段偏好的标签（命中任意一个即可）。 */
    public final List<String> preferredTags;

    DateNightStage(int maxTier, int cardCount, int durationSec, String... preferredTags) {
        this.maxTier = maxTier;
        this.cardCount = cardCount;
        this.durationSec = durationSec;
        List<String> tags = new ArrayList<>();
        Collections.addAll(tags, preferredTags);
        this.preferredTags = Collections.unmodifiableList(tags);
    }

    /** 该阶段是否需要倒计时。 */
    public boolean isTimed() {
        return durationSec > 0;
    }

    /** 下一个阶段；已是最后一个时返回自身。 */
    public DateNightStage next() {
        DateNightStage[] all = values();
        int idx = ordinal() + 1;
        return idx >= all.length ? this : all[idx];
    }

    /** 阶段在串场中的序号（0 基）。 */
    public int index() {
        return ordinal();
    }

    /** 串场总阶段数（4）。 */
    public static int stageCount() {
        return values().length;
    }
}
