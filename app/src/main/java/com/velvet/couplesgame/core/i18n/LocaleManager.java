package com.velvet.couplesgame.core.i18n;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import java.util.Locale;

public final class LocaleManager {
    public static final String LANG_EN = "en";
    public static final String LANG_ZH = "zh-CN";
    public static final String LANG_JA = "ja";

    private LocaleManager() {}

    public static void switchLanguage(String languageTag) {
        LocaleListCompat locales = LocaleListCompat.forLanguageTags(languageTag);
        AppCompatDelegate.setApplicationLocales(locales);
    }

    public static String resolveAssetLangDir() {
        LocaleListCompat current = AppCompatDelegate.getApplicationLocales();
        Locale locale = !current.isEmpty() ? current.get(0) : Locale.getDefault();
        if (locale == null) return "en";

        String lang = locale.getLanguage();
        if ("zh".equals(lang)) return "zh";
        if ("ja".equals(lang)) return "ja";
        return "en";
    }
}
