package com.velvet.couplesgame.core.reward;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.velvet.couplesgame.core.engine.ConsentFilter;
import com.velvet.couplesgame.data.local.entity.BoundaryRuleEntity;
import com.velvet.couplesgame.testing.FakeContext;
import com.velvet.couplesgame.testing.FakeSharedPreferences;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class RewardVoucherManagerTest {

    @Test
    public void savedVoucherIsLocalAvailableAndRedeemableOnlyOnce() {
        FakeSharedPreferences prefs = new FakeSharedPreferences();
        FakeContext context = new FakeContext(prefs);
        RewardVoucherManager manager = new RewardVoucherManager(context);

        assertTrue(manager.saveVoucher("auction-1", "A promise", "A shared massage",
                "Alex", 2, Collections.singletonList("massage")));

        assertEquals("velvet_reward_vouchers", context.getLastPrefsName());
        List<RewardVoucher> available = manager.getAvailable(new ConsentFilter(), 4);
        assertEquals(1, available.size());
        assertEquals("A promise", available.get(0).title);
        assertEquals("Alex", available.get(0).winnerName);
        assertFalse(available.get(0).isRedeemed());

        assertTrue(manager.redeem(available.get(0).id));
        assertFalse(manager.redeem(available.get(0).id));
        assertTrue(manager.getAvailable(new ConsentFilter(), 4).isEmpty());
        assertEquals(1, manager.getAll().size());
        assertTrue(manager.getAll().get(0).isRedeemed());
    }

    @Test
    public void availableVouchersRespectCurrentHardBoundariesAndTierCeiling() {
        RewardVoucherManager manager = new RewardVoucherManager(
                new FakeContext(new FakeSharedPreferences()));
        manager.saveVoucher("auction-1", "Blocked", "Description", "Alex", 1,
                Collections.singletonList("blindfold"));
        manager.saveVoucher("auction-2", "Higher tier", "Description", "Maya", 3,
                Collections.emptyList());

        ConsentFilter filter = new ConsentFilter();
        filter.replaceRules(Collections.singletonList(
                new BoundaryRuleEntity("blindfold", "YES", "HARD_NO")));

        List<RewardVoucher> allowed = manager.getAvailable(filter, 4);
        assertEquals(1, allowed.size());
        assertEquals("Higher tier", allowed.get(0).title);
        assertTrue(manager.getAvailable(filter, 2).isEmpty());
    }

    @Test
    public void malformedLocalVoucherEntriesAreIgnoredAndCannotBeRedeemed() {
        FakeSharedPreferences preferences = new FakeSharedPreferences();
        preferences.edit()
                .putBoolean("voucher.legacy.title", true)
                .putString("voucher.legacy.redeemed_at", "invalid")
                .commit();
        RewardVoucherManager manager = new RewardVoucherManager(new FakeContext(preferences));

        assertTrue(manager.getAll().isEmpty());
        assertFalse(manager.redeem("legacy"));
    }
}
