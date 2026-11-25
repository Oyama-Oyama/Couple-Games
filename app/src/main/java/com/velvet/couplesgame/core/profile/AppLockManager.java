package com.velvet.couplesgame.core.profile;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Stores only the local app-lock preference and whether the app left the foreground. */
public final class AppLockManager {

    private static final Logger LOGGER = Logger.getLogger(AppLockManager.class.getName());
    private static final String PREF_NAME = "velvet_app_lock";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_PENDING_AUTHENTICATION = "pending_authentication";

    private final SharedPreferences prefs;
    private boolean pendingAuthentication;

    public AppLockManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isEnabled() {
        try {
            return prefs.getBoolean(KEY_ENABLED, false);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "Unable to read app-lock preference; keeping the lock enabled.", e);
            return true;
        }
    }

    public boolean isAuthenticationRequired() {
        if (pendingAuthentication) return true;
        try {
            return prefs.getBoolean(KEY_ENABLED, false)
                    && prefs.getBoolean(KEY_PENDING_AUTHENTICATION, false);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unable to read app-lock state; requiring authentication.", e);
            return true;
        }
    }

    public boolean setEnabled(boolean enabled) {
        try {
            SharedPreferences.Editor editor = prefs.edit().putBoolean(KEY_ENABLED, enabled);
            if (!enabled) {
                editor.putBoolean(KEY_PENDING_AUTHENTICATION, false);
            }
            boolean saved = editor.commit();
            if (saved && !enabled) pendingAuthentication = false;
            return saved;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unable to save app-lock preference.", e);
            return false;
        }
    }

    public void markBackgrounded() {
        if (!isEnabled()) return;
        pendingAuthentication = true;
        try {
            if (!prefs.edit().putBoolean(KEY_PENDING_AUTHENTICATION, true).commit()) {
                LOGGER.severe("Unable to persist the pending app-lock state.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unable to persist the pending app-lock state.", e);
        }
    }

    public boolean clearPendingAuthentication() {
        try {
            if (!prefs.edit().putBoolean(KEY_PENDING_AUTHENTICATION, false).commit()) {
                LOGGER.severe("Unable to clear the pending app-lock state.");
                return false;
            }
            pendingAuthentication = false;
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unable to clear the pending app-lock state.", e);
            return false;
        }
    }
}
