package com.velvet.couplesgame.core.theme;

import androidx.annotation.ColorRes;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import com.velvet.couplesgame.R;

/**
 * Curated intimate & atmospheric themes shared across UI styles and game canvases.
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
