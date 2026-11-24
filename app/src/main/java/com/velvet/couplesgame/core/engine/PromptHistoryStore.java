package com.velvet.couplesgame.core.engine;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Shared persistent rolling history store preventing prompt repetition across
 * fragment switches, game restarts, and app launches.
 * Supports a pure in-memory mode (null prefs) for deterministic JUnit tests.
 */
public class PromptHistoryStore {
    private static final String PREF_NAME = "velvet_prompt_history";
    private static final String DELIM = "\\u001F";
    private static final String RAW_DELIM = "\u001F";
    private static final int DEFAULT_MAX_PER_BUCKET = 200;

    private final SharedPreferences prefs;
    private final Map<String, Deque<String>> buckets = new HashMap<>();

    public PromptHistoryStore(Context context) {
        this.prefs = context != null
                ? context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                : null;
    }

    /** Pure-Java in-memory constructor for unit testing. */
    public PromptHistoryStore() {
        this.prefs = null;
    }

    public synchronized boolean wasRecentlyShown(String bucket, String key) {
        if (bucket == null || key == null) return false;
        Deque<String> deque = getOrLoadBucket(bucket);
        return deque.contains(key);
    }

    public synchronized Set<String> getRecentSet(String bucket) {
        if (bucket == null) return Collections.emptySet();
        return new HashSet<>(getOrLoadBucket(bucket));
    }

    public synchronized List<String> getRecentList(String bucket) {
        if (bucket == null) return Collections.emptyList();
        return new ArrayList<>(getOrLoadBucket(bucket));
    }

    public synchronized void recordShown(String bucket, String key) {
        recordShown(bucket, key, DEFAULT_MAX_PER_BUCKET);
    }

    public synchronized void recordShown(String bucket, String key, int maxEntries) {
        if (bucket == null || key == null || key.isEmpty()) return;
        int cap = Math.max(1, maxEntries);
        Deque<String> deque = getOrLoadBucket(bucket);
        deque.remove(key);
        deque.addLast(key);
        while (deque.size() > cap) {
            deque.removeFirst();
        }
        persistBucket(bucket, deque);
    }

    public synchronized void clearBucket(String bucket) {
        if (bucket == null) return;
        buckets.remove(bucket);
        if (prefs != null) {
            prefs.edit().remove(bucket).apply();
        }
    }

    public synchronized void clearAll() {
        buckets.clear();
        if (prefs != null) {
            prefs.edit().clear().apply();
        }
    }

    private Deque<String> getOrLoadBucket(String bucket) {
        Deque<String> existing = buckets.get(bucket);
        if (existing != null) return existing;

        Deque<String> loaded = new ArrayDeque<>();
        if (prefs != null) {
            String raw = prefs.getString(bucket, "");
            if (raw != null && !raw.isEmpty()) {
                String[] parts = raw.split(DELIM);
                for (String part : parts) {
                    if (!part.isEmpty()) {
                        loaded.addLast(part);
                    }
                }
            }
        }
        buckets.put(bucket, loaded);
        return loaded;
    }

    private void persistBucket(String bucket, Deque<String> deque) {
        if (prefs == null) return;
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (String item : deque) {
            if (!first) sb.append(RAW_DELIM);
            sb.append(item);
            first = false;
        }
        prefs.edit().putString(bucket, sb.toString()).apply();
    }
}
