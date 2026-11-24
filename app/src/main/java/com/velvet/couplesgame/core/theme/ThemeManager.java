package com.velvet.couplesgame.core.theme;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

/**
 * Persists and applies the selected atmospheric theme offline.
 */
public class ThemeManager {
    private static final String PREF_NAME = "velvet_theme_prefs";
    private static final String KEY_THEME_ID = "selected_theme_id";

    private final SharedPreferences prefs;

    public ThemeManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public AppTheme getCurrentTheme() {
        String id = prefs.getString(KEY_THEME_ID, AppTheme.VELVET_NOIR.id);
        return AppTheme.fromId(id);
    }

    /** Must be called in Activity.onCreate() BEFORE super.onCreate() and setContentView(). */
    public void applySavedTheme(Activity activity) {
        activity.setTheme(getCurrentTheme().styleRes);
    }

    public boolean selectTheme(Activity activity, AppTheme nextTheme) {
        if (nextTheme == null) return false;
        AppTheme current = getCurrentTheme();
        if (current == nextTheme) return false;

        prefs.edit().putString(KEY_THEME_ID, nextTheme.id).apply();
        activity.recreate();
        return true;
    }

    /** Persists theme preference without destroying an active gameplay Activity. */
    public void saveThemePreferenceOnly(AppTheme nextTheme) {
        if (nextTheme == null) return;
        prefs.edit().putString(KEY_THEME_ID, nextTheme.id).apply();
    }
}
