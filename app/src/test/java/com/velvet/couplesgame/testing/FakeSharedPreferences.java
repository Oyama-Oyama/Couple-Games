package com.velvet.couplesgame.testing;

import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 纯 JVM 的 SharedPreferences 假实现，供本地单测注入 Manager。
 *
 * <p>不依赖 Robolectric / Mockito（工程 testImplementation 只有 JUnit 4）。
 * 语义与真机一致：edit() 返回暂存编辑器，commit()/apply() 才原子写回。
 */
public final class FakeSharedPreferences implements SharedPreferences {

    private final Map<String, Object> store = new HashMap<>();
    private boolean failReads;
    private boolean failWrites;

    /** 模拟存储读取异常，用于验证 Manager 的 fail-closed 语义。 */
    public void setFailReads(boolean failReads) {
        this.failReads = failReads;
    }

    /** 模拟存储写入异常，用于验证「写失败不误置为已确认」。 */
    public void setFailWrites(boolean failWrites) {
        this.failWrites = failWrites;
    }

    @Override
    public Map<String, ?> getAll() {
        if (failReads) throw new RuntimeException("simulated read failure");
        return new HashMap<>(store);
    }

    @Override
    public String getString(String key, String defValue) {
        if (failReads) throw new RuntimeException("simulated read failure");
        Object value = store.get(key);
        return value instanceof String ? (String) value : defValue;
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        Object value = store.get(key);
        if (value instanceof Set) {
            return new HashSet<>((Set<String>) value);
        }
        return defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
        Object value = store.get(key);
        return value instanceof Integer ? (Integer) value : defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        Object value = store.get(key);
        return value instanceof Long ? (Long) value : defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        Object value = store.get(key);
        return value instanceof Float ? (Float) value : defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        if (failReads) throw new RuntimeException("simulated read failure");
        Object value = store.get(key);
        return value instanceof Boolean ? (Boolean) value : defValue;
    }

    @Override
    public boolean contains(String key) {
        if (failReads) throw new RuntimeException("simulated read failure");
        return store.containsKey(key);
    }

    @Override
    public Editor edit() {
        if (failWrites) throw new RuntimeException("simulated write failure");
        return new FakeEditor();
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        // 测试假实现不需要监听通知。
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        // 测试假实现不需要监听通知。
    }

    /** 暴露底层写入次数，便于验证「同日覆盖不追加」等语义。 */
    public int size() {
        return store.size();
    }

    private final class FakeEditor implements Editor {

        private final Map<String, Object> pending = new HashMap<>();
        private boolean clearAll;

        @Override
        public Editor putString(String key, String value) {
            pending.put(key, value);
            return this;
        }

        @Override
        public Editor putStringSet(String key, Set<String> values) {
            pending.put(key, values != null ? new HashSet<>(values) : null);
            return this;
        }

        @Override
        public Editor putInt(String key, int value) {
            pending.put(key, value);
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            pending.put(key, value);
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            pending.put(key, value);
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            pending.put(key, value);
            return this;
        }

        @Override
        public Editor remove(String key) {
            pending.remove(key);
            return this;
        }

        @Override
        public Editor clear() {
            clearAll = true;
            return this;
        }

        @Override
        public boolean commit() {
            if (clearAll) store.clear();
            store.putAll(pending);
            return true;
        }

        @Override
        public void apply() {
            commit();
        }
    }
}
