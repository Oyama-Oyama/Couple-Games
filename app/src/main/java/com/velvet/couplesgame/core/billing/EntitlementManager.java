package com.velvet.couplesgame.core.billing;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.velvet.couplesgame.core.theme.AppTheme;

/**
 * Offline-first entitlement & preference store.
 * Persists Google Play purchase state, preferred intensity tier, and FLAG_SECURE privacy mode.
 */
public class EntitlementManager {
    private static final String PREF_NAME = "velvet_entitlements";
    private static final String KEY_SUB_ACTIVE = "premium_unlocked";
    private static final String KEY_LIFETIME_UNLOCKED = "lifetime_unlocked";
    private static final String KEY_DEBUG_OVERRIDE = "debug_vip_override";
    private static final String KEY_SELECTED_MAX_TIER = "selected_max_tier";
    private static final String KEY_SCREEN_SHIELD = "screen_privacy_shield";

    private final SharedPreferences prefs;
    private final MutableLiveData<Boolean> premiumLiveData = new MutableLiveData<>();

    public EntitlementManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.premiumLiveData.setValue(isPremiumUnlocked());
    }

    public boolean isPremiumUnlocked() {
        return prefs.getBoolean(KEY_LIFETIME_UNLOCKED, false)
                || prefs.getBoolean(KEY_SUB_ACTIVE, false)
                || prefs.getBoolean(KEY_DEBUG_OVERRIDE, false);
    }

    public boolean isSubscriptionActive() {
        return prefs.getBoolean(KEY_SUB_ACTIVE, false);
    }

    public boolean isLifetimeUnlocked() {
        return prefs.getBoolean(KEY_LIFETIME_UNLOCKED, false);
    }

    public LiveData<Boolean> observePremiumStatus() {
        return premiumLiveData;
    }

    public boolean canAccessTier(int tierLevel) {
        return FeatureGate.canAccessTier(tierLevel, isPremiumUnlocked());
    }

    public boolean canAccessTheme(AppTheme theme) {
        return FeatureGate.canAccessTheme(theme, isPremiumUnlocked());
    }

    public int getSelectedMaxTier() {
        int saved = prefs.getInt(KEY_SELECTED_MAX_TIER, FeatureGate.FREE_MAX_TIER);
        return FeatureGate.resolveEffectiveMaxTier(saved, isPremiumUnlocked());
    }

    public void setSelectedMaxTier(int tier) {
        int effective = FeatureGate.resolveEffectiveMaxTier(tier, isPremiumUnlocked());
        prefs.edit().putInt(KEY_SELECTED_MAX_TIER, effective).apply();
    }

    public boolean isScreenShieldEnabled() {
        return prefs.getBoolean(KEY_SCREEN_SHIELD, false);
    }

    public void setScreenShieldEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SCREEN_SHIELD, enabled).apply();
    }

    public void setLifetimeUnlocked(boolean unlocked) {
        prefs.edit().putBoolean(KEY_LIFETIME_UNLOCKED, unlocked).apply();
        premiumLiveData.postValue(isPremiumUnlocked());
    }

    public void setSubscriptionActive(boolean active) {
        prefs.edit().putBoolean(KEY_SUB_ACTIVE, active).apply();
        premiumLiveData.postValue(isPremiumUnlocked());
    }

    /** Only invoked from BuildConfig.DEBUG builds for local sandbox verification. */
    public void setDebugOverride(boolean enabled) {
        prefs.edit().putBoolean(KEY_DEBUG_OVERRIDE, enabled).apply();
        premiumLiveData.postValue(isPremiumUnlocked());
    }
}
