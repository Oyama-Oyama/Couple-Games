package com.velvet.couplesgame.core.profile;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * B 项 · 18+ 年龄确认的本地存储（架架规格 §2.2 / [API-009]）。
 *
 * <p>只存一个 boolean；非敏感数据，{@code allowBackup=false}，与安安 RED-P
 * 「数据不出本机」一致，**不需要加密**。
 *
 * <p>门禁强度采用**软门禁**（主理人裁决）：用户点「我未满 18 岁」→
 * 记录 declined = true → 成人向内容不可见，但 App 仍可使用基础玩法，
 * 不硬阻断整个 App。
 *
 * <p>读取失败一律视为「未确认」（fail-closed），不得默认放行。
 */
public class AgeGateManager {

    private static final String PREF_NAME = "velvet_age_gate";
    private static final String KEY_CONFIRMED = "confirmed";
    private static final String KEY_DECLINED = "declined";

    private final SharedPreferences prefs;

    public AgeGateManager(Context context) {
        this.prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    /** 是否已完成年龄确认（写入过确认或拒绝）。 */
    public boolean isConfirmed() {
        try {
            return prefs.getBoolean(KEY_CONFIRMED, false);
        } catch (Exception e) {
            // fail-closed：读失败当作未确认
            return false;
        }
    }

    /** 用户是否选择了「我未满 18 岁」。 */
    public boolean isDeclined() {
        try {
            return prefs.getBoolean(KEY_DECLINED, false);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 写盘先于关窗，避免进程被杀导致重复弹窗。
     *
     * @param declined true 表示用户点了「我未满 18 岁」
     */
    public void confirm(boolean declined) {
        try {
            prefs.edit()
                    .putBoolean(KEY_CONFIRMED, true)
                    .putBoolean(KEY_DECLINED, declined)
                    .commit();
        } catch (Exception e) {
            // 写盘失败时保持 fail-closed：不写即视为未确认，下次仍会询问
        }
    }

    /** 软门禁：未确认或已拒绝时，成人向内容不可见。 */
    public boolean isAdultContentAllowed() {
        return isConfirmed() && !isDeclined();
    }
}
