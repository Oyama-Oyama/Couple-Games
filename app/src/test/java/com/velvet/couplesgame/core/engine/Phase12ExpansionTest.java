package com.velvet.couplesgame.core.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.velvet.couplesgame.core.i18n.AssetContentLoader.GeneratorBundleDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.TemplateDto;
import com.velvet.couplesgame.core.i18n.LocaleManager;
import com.velvet.couplesgame.core.model.GameModeCatalog;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.ui.drawguess.ColorPickerView;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import org.junit.Test;

public class Phase12ExpansionTest {

    @Test
    public void localeManagerResolvesAllEightSupportedGlobalRegions() {
        assertEquals("en", LocaleManager.resolveAssetLangDirForLocale(Locale.US));
        assertEquals("zh", LocaleManager.resolveAssetLangDirForLocale(Locale.SIMPLIFIED_CHINESE));
        assertEquals("zh_tw", LocaleManager.resolveAssetLangDirForLocale(Locale.TRADITIONAL_CHINESE));
        assertEquals("zh_tw", LocaleManager.resolveAssetLangDirForLocale(new Locale("zh", "HK")));
        assertEquals("ja", LocaleManager.resolveAssetLangDirForLocale(Locale.JAPAN));
        assertEquals("ko", LocaleManager.resolveAssetLangDirForLocale(Locale.KOREA));
        assertEquals("es", LocaleManager.resolveAssetLangDirForLocale(new Locale("es", "MX")));
        assertEquals("fr", LocaleManager.resolveAssetLangDirForLocale(Locale.FRANCE));
        assertEquals("pt", LocaleManager.resolveAssetLangDirForLocale(new Locale("pt", "BR")));
        // Unsupported region gracefully falls back to English
        assertEquals("en", LocaleManager.resolveAssetLangDirForLocale(new Locale("sv", "SE")));
    }

    @Test
    public void colorPickerPureHsvConversionProducesExactPrimaryColorsAndHex() {
        assertEquals(0xFFFF0000, ColorPickerView.hsvToRgbPure(0f, 1f, 1f));
        assertEquals(0xFF00FF00, ColorPickerView.hsvToRgbPure(120f, 1f, 1f));
        assertEquals(0xFF0000FF, ColorPickerView.hsvToRgbPure(240f, 1f, 1f));
        assertEquals(0xFFFFFFFF, ColorPickerView.hsvToRgbPure(180f, 0f, 1f));
        assertEquals(0xFF000000, ColorPickerView.hsvToRgbPure(180f, 1f, 0f));
        assertEquals("#FF3B6B", ColorPickerView.formatHex(0xFFFF3B6B));
    }

    @Test
    public void proceduralGeneratorPrioritizesHighHeatTemplatesWhenTier4Active() {
        TemplateDto t1 = new TemplateDto();
        t1.id = "t1"; t1.tier = 1; t1.type = "TRUTH"; t1.tags = Collections.singletonList("deep_talk");
        t1.pattern = "Mild {x}";

        TemplateDto t4 = new TemplateDto();
        t4.id = "t4"; t4.tier = 4; t4.type = "DARE"; t4.tags = Collections.singletonList("physical_light");
        t4.durationSec = 45;
        t4.pattern = "Explore {x} for {duration}s";

        GeneratorBundleDto bundle = new GeneratorBundleDto();
        bundle.templates = Arrays.asList(t1, t4);
        Map<String, List<String>> slots = new HashMap<>();
        slots.put("x", Arrays.asList("collarbone", "waist", "inner thigh"));
        slots.put("duration", Collections.singletonList("45"));
        bundle.slots = slots;

        ProceduralTaskGenerator gen = new ProceduralTaskGenerator(null, new Random(42));
        int tier4Hits = 0;
        for (int i = 0; i < 40; i++) {
            TaskCard card = gen.generateFromBundle(bundle, 4, null, new ConsentFilter());
            assertNotNull(card);
            if (card.tier == 4) tier4Hits++;
        }
        // With 3x weight on maxTier=4 vs 1x on tier=1, expect clear majority Tier 4 (~75%)
        assertTrue("Expected heat-weighted Tier 4 dominance, got " + tier4Hits + "/40", tier4Hits >= 22);
    }

    @Test
    public void gameModeCatalogSliceForDisplaySupportsCollapseAndExpand() {
        List<GameModeCatalog> all = GameModeCatalog.queryModes(GameModeCatalog.SortFilter.HOTTEST_FIRST);
        assertEquals(36, all.size());

        List<GameModeCatalog> collapsed = GameModeCatalog.sliceForDisplay(all, false, 4);
        assertEquals(4, collapsed.size());
        assertEquals(all.get(0), collapsed.get(0));
        assertEquals(all.get(3), collapsed.get(3));

        List<GameModeCatalog> expanded = GameModeCatalog.sliceForDisplay(all, true, 4);
        assertEquals(36, expanded.size());

        assertTrue(GameModeCatalog.sliceForDisplay(null, false, 4).isEmpty());
    }
}
