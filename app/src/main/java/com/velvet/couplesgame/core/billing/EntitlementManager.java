package com.velvet.couplesgame.core.billing;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.velvet.couplesgame.BuildConfig;
import com.velvet.couplesgame.core.model.GameModeCatalog;
import com.velvet.couplesgame.core.theme.AppTheme;

/**
 * Offline-first entitlement & preference store.
 * Supports a compile-time debug VIP preview toggle (BuildConfig.VIP_PREVIEW_ENABLED)
 * while strictly enforcing real Play Billing entitlements in release builds.
 */
public class EntitlementManager {
    private static final String PREF_NAME = "velvet_entitlements";
    private static final String KEY_SUB_ACTIVE = "premium_unlocked";
    private static final String KEY_LIFETIME_UNLOCKED = "lifetime_unlocked";
    private static final String KEY_SELECTED_MAX_TIER = "selected_max_tier";
    private static final String KEY_TIER_AUTO_PROMOTED = "vip_tier_auto_promoted";
    private static final String KEY_SCREEN_SHIELD = "screen_privacy_shield";

    private final SharedPreferences prefs;
    private final MutableLiveData<Boolean> premiumLiveData = new MutableLiveData<>();

    public EntitlementManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.premiumLiveData.setValue(isPremiumUnlocked());
    }

    public boolean isPremiumUnlocked() {
        return resolveEffectivePremium(
                prefs.getBoolean(KEY_LIFETIME_UNLOCKED, false),
                prefs.getBoolean(KEY_SUB_ACTIVE, false),
                BuildConfig.VIP_PREVIEW_ENABLED
        );
    }

    public boolean isDevVipPreviewActive() {
        return BuildConfig.VIP_PREVIEW_ENABLED;
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

    public boolean canAccessGameMode(GameModeCatalog mode) {
        return FeatureGate.canAccessGameMode(mode, isPremiumUnlocked());
    }

    public boolean canAccessTheme(AppTheme theme) {
        return FeatureGate.canAccessTheme(theme, isPremiumUnlocked());
    }

    public int getSelectedMaxTier() {
        int defaultTier = BuildConfig.VIP_PREVIEW_ENABLED
                ? FeatureGate.PREMIUM_MAX_TIER
                : FeatureGate.FREE_MAX_TIER;
        int saved = prefs.getInt(KEY_SELECTED_MAX_TIER, defaultTier);
        return FeatureGate.resolveEffectiveMaxTier(saved, isPremiumUnlocked());
    }

    public void setSelectedMaxTier(int tier) {
        int effective = FeatureGate.resolveEffectiveMaxTier(tier, isPremiumUnlocked());
        prefs.edit().putInt(KEY_SELECTED_MAX_TIER, effective).apply();
    }

    public boolean isScreenShieldEnabled() {
        // 隐私优先：新用户默认开启防截屏/防录屏；用户手动关闭后以本地选择为准。
        return prefs.getBoolean(KEY_SCREEN_SHIELD, true);
    }

    public void setScreenShieldEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_SCREEN_SHIELD, enabled).apply();
    }

    public void setLifetimeUnlocked(boolean unlocked) {
        SharedPreferences.Editor editor = prefs.edit().putBoolean(KEY_LIFETIME_UNLOCKED, unlocked);
        if (unlocked && !prefs.getBoolean(KEY_TIER_AUTO_PROMOTED, false)) {
            editor.putInt(KEY_SELECTED_MAX_TIER, FeatureGate.PREMIUM_MAX_TIER);
            editor.putBoolean(KEY_TIER_AUTO_PROMOTED, true);
        }
        editor.apply();
        premiumLiveData.postValue(isPremiumUnlocked());
    }

    public void setSubscriptionActive(boolean active) {
        SharedPreferences.Editor editor = prefs.edit().putBoolean(KEY_SUB_ACTIVE, active);
        if (active && !prefs.getBoolean(KEY_TIER_AUTO_PROMOTED, false)) {
            editor.putInt(KEY_SELECTED_MAX_TIER, FeatureGate.PREMIUM_MAX_TIER);
            editor.putBoolean(KEY_TIER_AUTO_PROMOTED, true);
        }
        editor.apply();
        premiumLiveData.postValue(isPremiumUnlocked());
    }

    /** Pure-Java helper for unit testing dev VIP preview & real purchase resolution. */
    public static boolean resolveEffectivePremium(boolean lifetimeUnlocked,
                                                  boolean subscriptionActive,
                                                  boolean devPreviewEnabled) {
        return devPreviewEnabled || lifetimeUnlocked || subscriptionActive;
    }

    /** Pure-Java helper for unit testing initial VIP tier promotion rule. */
    public static int resolveTierAfterVipUnlock(int currentTier, boolean alreadyPromotedOnce) {
        if (!alreadyPromotedOnce && currentTier <= FeatureGate.FREE_MAX_TIER) {
            return FeatureGate.PREMIUM_MAX_TIER;
        }
        return currentTier;
    }
}
