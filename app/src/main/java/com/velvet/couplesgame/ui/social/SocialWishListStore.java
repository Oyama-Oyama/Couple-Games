package com.velvet.couplesgame.ui.social;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Stores a couple's shared wishes on this device only. */
final class SocialWishListStore {
    enum AddResult { ADDED, DUPLICATE, FULL }

    private static final String PREFERENCES = "social_wishlist_local";
    private static final String KEY_WISHES = "wishes";
    private static final int MAX_WISHES = 100;
    static final int MAX_WISH_LENGTH = 160;

    private final SharedPreferences preferences;

    SocialWishListStore(Context context) {
        preferences = context.getApplicationContext()
                .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE);
    }

    List<String> getWishes() {
        return Collections.unmodifiableList(readWishes());
    }

    AddResult addWish(String wish) {
        if (wish == null || wish.trim().isEmpty()) {
            throw new IllegalArgumentException("Wish must not be blank");
        }
        String normalized = wish.trim();
        if (normalized.length() > MAX_WISH_LENGTH) {
            throw new IllegalArgumentException("Wish exceeds maximum length");
        }

        List<String> wishes = readWishes();
        String comparison = normalized.toLowerCase(Locale.ROOT);
        for (String existing : wishes) {
            if (existing.toLowerCase(Locale.ROOT).equals(comparison)) return AddResult.DUPLICATE;
        }
        if (wishes.size() >= MAX_WISHES) return AddResult.FULL;
        wishes.add(normalized);
        persist(wishes);
        return AddResult.ADDED;
    }

    boolean removeWish(String wish) {
        if (wish == null) return false;
        List<String> wishes = readWishes();
        boolean removed = wishes.remove(wish);
        if (removed) persist(wishes);
        return removed;
    }

    private List<String> readWishes() {
        String stored = preferences.getString(KEY_WISHES, "[]");
        try {
            JSONArray array = new JSONArray(stored);
            List<String> wishes = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++) {
                String wish = array.optString(i, "").trim();
                if (!wish.isEmpty()) wishes.add(wish);
            }
            return wishes;
        } catch (JSONException exception) {
            throw new IllegalStateException("Saved shared wish list is invalid", exception);
        }
    }

    private void persist(List<String> wishes) {
        JSONArray array = new JSONArray();
        for (String wish : wishes) array.put(wish);
        preferences.edit().putString(KEY_WISHES, array.toString()).apply();
    }
}
