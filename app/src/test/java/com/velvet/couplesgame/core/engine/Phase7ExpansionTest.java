package com.velvet.couplesgame.core.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.velvet.couplesgame.core.billing.FeatureGate;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.CubeFaceDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.CubeModifierDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.FateCubesBundleDto;
import com.velvet.couplesgame.core.theme.AppTheme;
import com.velvet.couplesgame.data.local.entity.BoundaryRuleEntity;
import com.velvet.couplesgame.ui.fatecubes.FateCubesEngine;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import org.junit.Test;

public class Phase7ExpansionTest {

    @Test
    public void newSensualThemesResolveFreeAndVipAccessProperly() {
        assertTrue(FeatureGate.canAccessTheme(AppTheme.PEACH_BLUSH, false));
        assertTrue(FeatureGate.canAccessTheme(AppTheme.SAKURA_SILK, false));
        assertFalse(FeatureGate.canAccessTheme(AppTheme.SCARLET_LACE, false));
        assertFalse(FeatureGate.canAccessTheme(AppTheme.VIOLET_MIST, false));
        assertTrue(FeatureGate.canAccessTheme(AppTheme.SCARLET_LACE, true));
        assertTrue(FeatureGate.canAccessTheme(AppTheme.VIOLET_MIST, true));
    }

    @Test
    public void fateCubesRespectsConsentFilterHeatCeilingAndSwapsRoles() {
        FateCubesBundleDto bundle = new FateCubesBundleDto();
        bundle.actions = Arrays.asList(
                makeFace("a1", "Soft Kiss", 1, "physical_light"),
                makeFace("a2", "Lick Softly", 3, "bite_lick"),
                makeFace("a3", "Playful Spank", 4, "restraint_light")
        );
        bundle.spots = Arrays.asList(
                makeFace("s1", "Earlobe", 2, "physical_light"),
                makeFace("s2", "Inner Thigh", 4, "physical_light")
        );
        CubeModifierDto mod = new CubeModifierDto();
        mod.id = "m1";
        mod.label = "Eyes closed";
        mod.durationSec = 25;
        mod.tier = 2;
        mod.tags = Collections.singletonList("playful");
        bundle.modifiers = Collections.singletonList(mod);

        ConsentFilter filter = new ConsentFilter();
        filter.replaceRules(Collections.singletonList(
                new BoundaryRuleEntity("bite_lick", "YES", "HARD_NO")
        ));

        FateCubesEngine engine = new FateCubesEngine(new Random(42));
        // Free heat tier = 2 -> only a1 allowed (a2 blocked by bite_lick & tier, a3 blocked by tier)
        engine.loadBundle(bundle, filter, 2);
        assertEquals(1, engine.getEligibleActions().size());
        assertEquals("a1", engine.getEligibleActions().get(0).id);
        assertEquals(1, engine.getEligibleSpots().size());
        assertEquals("s1", engine.getEligibleSpots().get(0).id);

        assertFalse(engine.isComboReady());
        assertNotNull(engine.rollAction());
        assertFalse(engine.isComboReady()); // still waiting for bottom spot cube
        assertNotNull(engine.rollSpot());
        assertTrue(engine.isComboReady());
        assertNotNull(engine.getSelectedModifier());
        assertEquals(25, engine.getSelectedModifier().durationSec);

        assertTrue(engine.isGiverPlayerA());
        engine.completeRoundAndPrepareNext();
        assertEquals(1, engine.getRoundsCompleted());
        assertFalse(engine.isGiverPlayerA());
        assertNull(engine.getSelectedAction());
        assertNull(engine.getSelectedSpot());
    }

    private CubeFaceDto makeFace(String id, String label, int tier, String tag) {
        CubeFaceDto dto = new CubeFaceDto();
        dto.id = id;
        dto.label = label;
        dto.icon = "♥";
        dto.tier = tier;
        dto.tags = Collections.singletonList(tag);
        return dto;
    }
}
