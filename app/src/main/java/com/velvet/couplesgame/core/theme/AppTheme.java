package com.velvet.couplesgame.core.theme;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import com.velvet.couplesgame.R;

/**
 * 12 curated intimate & atmospheric themes shared across UI styles and all custom game canvases.
 */
public enum AppTheme {
    VELVET_NOIR(
            "velvet_noir",
            R.style.Theme_VelvetCouplesGame_VelvetNoir,
            R.string.theme_velvet_noir,
            R.string.theme_velvet_noir_desc,
            R.color.velvet_bg,
            R.color.velvet_surface,
            R.color.velvet_crimson,
            R.color.velvet_gold,
            R.color.velvet_ivory,
            R.color.velvet_muted,
            false
    ),
    PEACH_BLUSH(
            "peach_blush",
            R.style.Theme_VelvetCouplesGame_PeachBlush,
            R.string.theme_peach_blush,
            R.string.theme_peach_blush_desc,
            R.color.peach_bg,
            R.color.peach_surface,
            R.color.peach_primary,
            R.color.peach_accent,
            R.color.peach_text,
            R.color.peach_muted,
            false
    ),
    SAKURA_SILK(
            "sakura_silk",
            R.style.Theme_VelvetCouplesGame_SakuraSilk,
            R.string.theme_sakura_silk,
            R.string.theme_sakura_silk_desc,
            R.color.sakura_bg,
            R.color.sakura_surface,
            R.color.sakura_primary,
            R.color.sakura_accent,
            R.color.sakura_text,
            R.color.sakura_muted,
            false
    ),
    BLUSH_CHAMPAGNE(
            "blush_champagne",
            R.style.Theme_VelvetCouplesGame_BlushChampagne,
            R.string.theme_blush_champagne,
            R.string.theme_blush_champagne_desc,
            R.color.blush_bg,
            R.color.blush_surface,
            R.color.blush_primary,
            R.color.blush_accent,
            R.color.blush_text,
            R.color.blush_muted,
            false
    ),
    MIDNIGHT_PLUM(
            "midnight_plum",
            R.style.Theme_VelvetCouplesGame_MidnightPlum,
            R.string.theme_midnight_plum,
            R.string.theme_midnight_plum_desc,
            R.color.plum_bg,
            R.color.plum_surface,
            R.color.plum_primary,
            R.color.plum_accent,
            R.color.plum_text,
            R.color.plum_muted,
            false
    ),
    SCARLET_LACE(
            "scarlet_lace",
            R.style.Theme_VelvetCouplesGame_ScarletLace,
            R.string.theme_scarlet_lace,
            R.string.theme_scarlet_lace_desc,
            R.color.scarlet_bg,
            R.color.scarlet_surface,
            R.color.scarlet_primary,
            R.color.scarlet_accent,
            R.color.scarlet_text,
            R.color.scarlet_muted,
            true
    ),
    VIOLET_MIST(
            "violet_mist",
            R.style.Theme_VelvetCouplesGame_VioletMist,
            R.string.theme_violet_mist,
            R.string.theme_violet_mist_desc,
            R.color.violet_bg,
            R.color.violet_surface,
            R.color.violet_primary,
            R.color.violet_accent,
            R.color.violet_text,
            R.color.violet_muted,
            true
    ),
    CANDLELIGHT_AMBER(
            "candlelight_amber",
            R.style.Theme_VelvetCouplesGame_CandlelightAmber,
            R.string.theme_candlelight_amber,
            R.string.theme_candlelight_amber_desc,
            R.color.amber_bg,
            R.color.amber_surface,
            R.color.amber_primary,
            R.color.amber_accent,
            R.color.amber_text,
            R.color.amber_muted,
            true
    ),
    NEON_SILK(
            "neon_silk",
            R.style.Theme_VelvetCouplesGame_NeonSilk,
            R.string.theme_neon_silk,
            R.string.theme_neon_silk_desc,
            R.color.neon_bg,
            R.color.neon_surface,
            R.color.neon_primary,
            R.color.neon_accent,
            R.color.neon_text,
            R.color.neon_muted,
            true
    ),
    ROUGE_VELVET(
            "rouge_velvet",
            R.style.Theme_VelvetCouplesGame_RougeVelvet,
            R.string.theme_rouge_velvet,
            R.string.theme_rouge_velvet_desc,
            R.color.rouge_bg,
            R.color.rouge_surface,
            R.color.rouge_primary,
            R.color.rouge_accent,
            R.color.rouge_text,
            R.color.rouge_muted,
            false
    ),
    MOONLIT_LACE(
            "moonlit_lace",
            R.style.Theme_VelvetCouplesGame_MoonlitLace,
            R.string.theme_moonlit_lace,
            R.string.theme_moonlit_lace_desc,
            R.color.moonlit_bg,
            R.color.moonlit_surface,
            R.color.moonlit_primary,
            R.color.moonlit_accent,
            R.color.moonlit_text,
            R.color.moonlit_muted,
            false
    ),
    HONEY_DUSK(
            "honey_dusk",
            R.style.Theme_VelvetCouplesGame_HoneyDusk,
            R.string.theme_honey_dusk,
            R.string.theme_honey_dusk_desc,
            R.color.honey_bg,
            R.color.honey_surface,
            R.color.honey_primary,
            R.color.honey_accent,
            R.color.honey_text,
            R.color.honey_muted,
            true
    );

    public final String id;
    @StyleRes public final int styleRes;
    @StringRes public final int titleRes;
    @StringRes public final int moodRes;
    @ColorRes public final int bgColorRes;
    @ColorRes public final int surfaceColorRes;
    @ColorRes public final int primaryColorRes;
    @ColorRes public final int accentColorRes;
    @ColorRes public final int textPrimaryColorRes;
    @ColorRes public final int textMutedColorRes;
    public final boolean premium;

    AppTheme(String id,
             @StyleRes int styleRes,
             @StringRes int titleRes,
             @StringRes int moodRes,
             @ColorRes int bgColorRes,
             @ColorRes int surfaceColorRes,
             @ColorRes int primaryColorRes,
             @ColorRes int accentColorRes,
             @ColorRes int textPrimaryColorRes,
             @ColorRes int textMutedColorRes,
             boolean premium) {
        this.id = id;
        this.styleRes = styleRes;
        this.titleRes = titleRes;
        this.moodRes = moodRes;
        this.bgColorRes = bgColorRes;
        this.surfaceColorRes = surfaceColorRes;
        this.primaryColorRes = primaryColorRes;
        this.accentColorRes = accentColorRes;
        this.textPrimaryColorRes = textPrimaryColorRes;
        this.textMutedColorRes = textMutedColorRes;
        this.premium = premium;
    }

    public static AppTheme fromId(String id) {
        if (id == null) return VELVET_NOIR;
        for (AppTheme theme : values()) {
            if (theme.id.equals(id)) return theme;
        }
        return VELVET_NOIR;
    }
}
