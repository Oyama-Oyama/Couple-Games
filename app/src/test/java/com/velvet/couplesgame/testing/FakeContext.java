package com.velvet.couplesgame.testing;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;

/**
 * 纯 JVM 的 Context 假实现。
 *
 * <p>借用 {@link ContextWrapper} 已实现的全部 Context 抽象方法（委托 mBase），
 * 仅覆写 Manager 构造时真正用到的两个方法，避免实现数十个抽象方法。
 * 构造函数传 null base 不会触碰 android.jar 的桩实现。
 */
public final class FakeContext extends ContextWrapper {

    private final SharedPreferences prefs;
    private String lastPrefsName;

    public FakeContext(SharedPreferences prefs) {
        super(null);
        this.prefs = prefs;
    }

    @Override
    public Context getApplicationContext() {
        return this;
    }

    @Override
    public SharedPreferences getSharedPreferences(String name, int mode) {
        lastPrefsName = name;
        return prefs;
    }

    /** 供测试断言 Manager 使用的 SharedPreferences 文件名契约。 */
    public String getLastPrefsName() {
        return lastPrefsName;
    }
}
