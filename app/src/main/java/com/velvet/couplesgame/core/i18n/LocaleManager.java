package com.velvet.couplesgame.core.i18n;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import java.util.Locale;

/**
 * Manages in-app per-locale switching across 8 global languages:
 * EN, zh-CN, zh-TW, JA, KO, ES, FR, PT-BR.
 */
public final class LocaleManager {
    public static final String LANG_EN = "en";
    public static final String LANG_ZH_CN = "zh-CN";
    public static final String LANG_ZH_TW = "zh-TW";
    public static final String LANG_JA = "ja";
    public static final String LANG_KO = "ko";
    public static final String LANG_ES = "es";
    public static final String LANG_FR = "fr";
    public static final String LANG_PT_BR = "pt-BR";

    /** Legacy alias kept for compatibility. */
    public static final String LANG_ZH = LANG_ZH_CN;

    private LocaleManager() {}

    public static void switchLanguage(String languageTag) {
        LocaleListCompat locales = LocaleListCompat.forLanguageTags(languageTag);
        AppCompatDelegate.setApplicationLocales(locales);
    }

    public static String resolveAssetLangDir() {
        LocaleListCompat current = AppCompatDelegate.getApplicationLocales();
        Locale locale = !current.isEmpty() ? current.get(0) : Locale.getDefault();
        return resolveAssetLangDirForLocale(locale);
    }

    /** Pure-Java resolver mapping Android Locale instances to assets/content/{dir}/ folders. */
    public static String resolveAssetLangDirForLocale(Locale locale) {
        if (locale == null) return "en";
        String lang = locale.getLanguage();
        if ("zh".equals(lang)) {
            String country = locale.getCountry();
            String script = locale.getScript();
            if ("TW".equalsIgnoreCase(country)
                    || "HK".equalsIgnoreCase(country)
                    || "MO".equalsIgnoreCase(country)
                    || "Hant".equalsIgnoreCase(script)) {
                return "zh_tw";
            }
            return "zh";
        }
        if ("ja".equals(lang)) return "ja";
        if ("ko".equals(lang)) return "ko";
        if ("es".equals(lang)) return "es";
        if ("fr".equals(lang)) return "fr";
        if ("pt".equals(lang)) return "pt";
        return "en";
    }
}
