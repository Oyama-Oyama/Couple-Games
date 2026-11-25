package com.velvet.couplesgame.core.profile;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.velvet.couplesgame.testing.FakeContext;
import com.velvet.couplesgame.testing.FakeSharedPreferences;

import org.junit.Test;

public class AppLockManagerTest {

    @Test
    public void lockIsOptInAndRequiresAuthenticationAfterBackgrounding() {
        FakeSharedPreferences prefs = new FakeSharedPreferences();
        AppLockManager manager = new AppLockManager(new FakeContext(prefs));

        assertFalse(manager.isEnabled());
        assertFalse(manager.isAuthenticationRequired());

        assertTrue(manager.setEnabled(true));
        assertFalse(manager.isAuthenticationRequired());
        manager.markBackgrounded();
        assertTrue(manager.isAuthenticationRequired());

        assertTrue(manager.clearPendingAuthentication());
        assertFalse(manager.isAuthenticationRequired());
    }

    @Test
    public void disablingClearsPendingLockAndBackgroundingDoesNothing() {
        FakeSharedPreferences prefs = new FakeSharedPreferences();
        AppLockManager manager = new AppLockManager(new FakeContext(prefs));

        assertTrue(manager.setEnabled(true));
        manager.markBackgrounded();
        assertTrue(manager.isAuthenticationRequired());

        assertTrue(manager.setEnabled(false));
        assertFalse(manager.isAuthenticationRequired());
        manager.markBackgrounded();
        assertFalse(manager.isAuthenticationRequired());
    }

    @Test
    public void storageReadFailureFailsClosedAndWriteFailureIsReported() {
        FakeSharedPreferences prefs = new FakeSharedPreferences();
        AppLockManager manager = new AppLockManager(new FakeContext(prefs));

        prefs.setFailReads(true);
        assertTrue(manager.isEnabled());
        assertTrue(manager.isAuthenticationRequired());

        prefs.setFailReads(false);
        prefs.setFailWrites(true);
        assertFalse(manager.setEnabled(true));
    }
}
