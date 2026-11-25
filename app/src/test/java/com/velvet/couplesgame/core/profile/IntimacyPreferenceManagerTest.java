package com.velvet.couplesgame.core.profile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.velvet.couplesgame.core.model.GameModeCatalog;
import com.velvet.couplesgame.testing.FakeContext;
import com.velvet.couplesgame.testing.FakeSharedPreferences;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import org.junit.Test;

public class IntimacyPreferenceManagerTest {

    @Test
    public void preferencesPersistLocallyAndOnlyRecommendMatchingModes() {
        FakeContext context = new FakeContext(new FakeSharedPreferences());
        IntimacyPreferenceManager manager = new IntimacyPreferenceManager(context);

        assertTrue(manager.saveSelectedStyles(EnumSet.of(
                IntimacyPreferenceManager.Style.ROMANTIC,
                IntimacyPreferenceManager.Style.IMAGINATIVE)));

        IntimacyPreferenceManager restored = new IntimacyPreferenceManager(context);
        assertEquals(EnumSet.of(
                IntimacyPreferenceManager.Style.ROMANTIC,
                IntimacyPreferenceManager.Style.IMAGINATIVE), restored.getSelectedStyles());
        List<GameModeCatalog> recommendations = restored.recommendModes(Arrays.asList(
                GameModeCatalog.DATE_PLANNER,
                GameModeCatalog.LUCKY_WHEEL,
                GameModeCatalog.ROLEPLAY_THEATER));

        assertEquals(Arrays.asList(
                GameModeCatalog.DATE_PLANNER,
                GameModeCatalog.ROLEPLAY_THEATER), recommendations);
        assertEquals("velvet_intimacy_preferences", context.getLastPrefsName());
    }

    @Test
    public void emptySelectionProducesNoRecommendationsAndCanBeCleared() {
        IntimacyPreferenceManager manager = new IntimacyPreferenceManager(
                new FakeContext(new FakeSharedPreferences()));

        assertTrue(manager.recommendModes(GameModeCatalog.queryModes(
                GameModeCatalog.SortFilter.HOTTEST_FIRST)).isEmpty());
        assertTrue(manager.saveSelectedStyles(
                Collections.singleton(IntimacyPreferenceManager.Style.PLAYFUL)));
        assertTrue(manager.saveSelectedStyles(Collections.emptySet()));
        assertTrue(manager.getSelectedStyles().isEmpty());
    }
}
