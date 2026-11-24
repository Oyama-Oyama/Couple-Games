package com.velvet.couplesgame.core.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.velvet.couplesgame.core.model.GameModeCatalog;
import com.velvet.couplesgame.testing.FakeContext;
import com.velvet.couplesgame.testing.FakeSharedPreferences;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * B 项 · 18+ 年龄确认单测（架架规格 §2 / [API-009]，AC-104 / AC-701）。
 *
 * <p>软门禁语义：decline 只隐藏 heatRating &ge; 3 的模式，App 基础玩法仍可用，
 * GameModeCatalog 始终 36 项（不因测试或门禁改动）。
 */
public class AgeGateManagerTest {

    private static AgeGateManager newManager(FakeSharedPreferences prefs) {
        return new AgeGateManager(new FakeContext(prefs));
    }

    @Test
    public void firstLaunchIsUnconfirmedAndBlocksAdultContent() {
        AgeGateManager manager = newManager(new FakeSharedPreferences());

        assertFalse("首启未完成确认", manager.isConfirmed());
        assertFalse("首启不应默认标记为拒绝", manager.isDeclined());
        assertFalse("未确认时成人向内容必须不可见（fail-closed）",
                manager.isAdultContentAllowed());
    }

    @Test
    public void confirmAdultPersistsAcrossManagerInstances() {
        FakeSharedPreferences prefs = new FakeSharedPreferences();
        newManager(prefs).confirm(false);

        AgeGateManager restarted = newManager(prefs);
        assertTrue("确认结果必须持久化到 SharedPreferences", restarted.isConfirmed());
        assertFalse("确认成年不应置 declined", restarted.isDeclined());
        assertTrue("确认成年后成人向内容可见", restarted.isAdultContentAllowed());
    }

    @Test
    public void declineSetsRestrictedFlagAndPersists() {
        FakeSharedPreferences prefs = new FakeSharedPreferences();
        newManager(prefs).confirm(true);

        AgeGateManager restarted = newManager(prefs);
        assertTrue("拒绝同样算已处理，避免重复弹窗", restarted.isConfirmed());
        assertTrue("拒绝标志必须持久化", restarted.isDeclined());
        assertFalse("拒绝后成人向内容不可见", restarted.isAdultContentAllowed());
    }

    @Test
    public void softGateKeepsGentleModesReachableAndCatalogIntact() {
        FakeSharedPreferences prefs = new FakeSharedPreferences();
        AgeGateManager manager = newManager(prefs);
        manager.confirm(true);

        assertFalse("软门禁只隐藏成人向内容", manager.isAdultContentAllowed());

        List<GameModeCatalog> gentleModes = new ArrayList<>();
        for (GameModeCatalog mode : GameModeCatalog.values()) {
            if (mode.heatRating <= 2) gentleModes.add(mode);
        }
        assertFalse("decline 后仍应可达 heatRating≤2 的温和模式（不硬阻断整个 App）",
                gentleModes.isEmpty());
        assertEquals("门禁不得改变模式总数", 36, GameModeCatalog.values().length);
    }

    @Test
    public void repeatedConfirmAndDeclineAreIdempotent() {
        FakeSharedPreferences prefs = new FakeSharedPreferences();
        AgeGateManager manager = newManager(prefs);

        manager.confirm(false);
        manager.confirm(false);
        assertTrue(manager.isConfirmed());
        assertFalse(manager.isDeclined());
        assertTrue(manager.isAdultContentAllowed());

        manager.confirm(true);
        manager.confirm(true);
        assertTrue(manager.isConfirmed());
        assertTrue(manager.isDeclined());
        assertFalse(manager.isAdultContentAllowed());
    }

    @Test
    public void usesDocumentedSingleBooleanStore() {
        FakeSharedPreferences prefs = new FakeSharedPreferences();
        FakeContext context = new FakeContext(prefs);
        AgeGateManager manager = new AgeGateManager(context);

        assertEquals("存储文件名契约 [API-009]", "velvet_age_gate", context.getLastPrefsName());
        int keysBefore = prefs.size();
        manager.confirm(true);
        assertEquals("只存 confirmed / declined 两个布尔键",
                keysBefore + 2, prefs.size());
    }

    @Test
    public void storageFailuresStayFailClosed() {
        FakeSharedPreferences prefs = new FakeSharedPreferences();
        AgeGateManager manager = newManager(prefs);

        prefs.setFailReads(true);
        assertFalse("读取异常必须视为未确认", manager.isConfirmed());
        assertFalse("读取异常必须视为未拒绝", manager.isDeclined());
        assertFalse("读取异常不得默认放行成人内容", manager.isAdultContentAllowed());

        prefs.setFailReads(false);
        prefs.setFailWrites(true);
        manager.confirm(false);
        assertFalse("写盘失败不得误置为已确认", manager.isConfirmed());
    }
}
