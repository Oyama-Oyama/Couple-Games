package com.velvet.couplesgame.core.i18n;

import android.content.Context;
import com.google.gson.Gson;
import com.velvet.couplesgame.core.model.BoardTheme;
import com.velvet.couplesgame.core.model.DeckMeta;
import com.velvet.couplesgame.core.model.DrawWord;
import com.velvet.couplesgame.core.model.TaskCard;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads localized offline JSON assets from assets/content/{en|zh|ja}/ with automatic
 * fallback to English and thread-safe in-memory caching per language.
 */
public class AssetContentLoader {
    private static final String DEFAULT_LANG = "en";

    private final Context appContext;
    private final Gson gson = new Gson();

    private final Map<String, List<DeckMeta>> decksCache = new ConcurrentHashMap<>();
    private final Map<String, List<BoardTheme>> themesCache = new ConcurrentHashMap<>();
    private final Map<String, List<DrawWord>> drawWordsCache = new ConcurrentHashMap<>();
    private final Map<String, GeneratorBundleDto> generatorCache = new ConcurrentHashMap<>();

    public AssetContentLoader(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public List<DeckMeta> loadDecks() {
        return loadDecks(LocaleManager.resolveAssetLangDir());
    }

    public List<DeckMeta> loadDecks(String langDir) {
        return decksCache.computeIfAbsent(langDir, lang -> {
            DecksRootDto dto = readJsonWithFallback(lang, "decks.json", DecksRootDto.class);
            if (dto == null || dto.decks == null) return Collections.emptyList();

            List<DeckMeta> result = new ArrayList<>();
            for (DeckDto deckDto : dto.decks) {
                List<TaskCard> cards = new ArrayList<>();
                if (deckDto.cards != null) {
                    for (CardDto c : deckDto.cards) {
                        cards.add(new TaskCard(
                                c.id,
                                deckDto.id,
                                c.tier,
                                c.type,
                                c.tags,
                                c.durationSec,
                                c.text,
                                false,
                                false
                        ));
                    }
                }
                result.add(new DeckMeta(
                        deckDto.id,
                        deckDto.title,
                        deckDto.subtitle,
                        deckDto.tier,
                        deckDto.premium,
                        cards
                ));
            }
            return Collections.unmodifiableList(result);
        });
    }

    public List<TaskCard> loadAllOfficialCards() {
        List<TaskCard> all = new ArrayList<>();
        for (DeckMeta deck : loadDecks()) {
            all.addAll(deck.cards);
        }
        return all;
    }

    public List<BoardTheme> loadBoardThemes() {
        return loadBoardThemes(LocaleManager.resolveAssetLangDir());
    }

    public List<BoardTheme> loadBoardThemes(String langDir) {
        return themesCache.computeIfAbsent(langDir, lang -> {
            ThemesRootDto dto = readJsonWithFallback(lang, "board_themes.json", ThemesRootDto.class);
            if (dto == null || dto.themes == null) return Collections.emptyList();
            List<BoardTheme> result = new ArrayList<>();
            for (ThemeDto t : dto.themes) {
                result.add(new BoardTheme(t.id, t.name, t.bgHex, t.primaryHex, t.accentHex, t.totalTiles, t.premium));
            }
            return Collections.unmodifiableList(result);
        });
    }

    public List<DrawWord> loadDrawWords() {
        return loadDrawWords(LocaleManager.resolveAssetLangDir());
    }

    public List<DrawWord> loadDrawWords(String langDir) {
        return drawWordsCache.computeIfAbsent(langDir, lang -> {
            DrawWordsRootDto dto = readJsonWithFallback(lang, "draw_words.json", DrawWordsRootDto.class);
            if (dto == null || dto.words == null) return Collections.emptyList();
            List<DrawWord> result = new ArrayList<>();
            for (DrawWordDto w : dto.words) {
                result.add(new DrawWord(w.word, w.tier, w.tags));
            }
            return Collections.unmodifiableList(result);
        });
    }

    public GeneratorBundleDto loadGeneratorBundle() {
        return loadGeneratorBundle(LocaleManager.resolveAssetLangDir());
    }

    public GeneratorBundleDto loadGeneratorBundle(String langDir) {
        return generatorCache.computeIfAbsent(langDir, lang -> {
            GeneratorBundleDto dto = readJsonWithFallback(lang, "generator_templates.json", GeneratorBundleDto.class);
            return dto != null ? dto : new GeneratorBundleDto();
        });
    }

    public void clearCache() {
        decksCache.clear();
        themesCache.clear();
        drawWordsCache.clear();
        generatorCache.clear();
    }

    private <T> T readJsonWithFallback(String langDir, String fileName, Class<T> clazz) {
        T parsed = readJsonOrNull("content/" + langDir + "/" + fileName, clazz);
        if (parsed != null) return parsed;
        if (!DEFAULT_LANG.equals(langDir)) {
            return readJsonOrNull("content/" + DEFAULT_LANG + "/" + fileName, clazz);
        }
        return null;
    }

    private <T> T readJsonOrNull(String assetPath, Class<T> clazz) {
        try (InputStream is = appContext.getAssets().open(assetPath);
             InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, clazz);
        } catch (IOException ex) {
            return null;
        }
    }

    // ---- Internal Gson DTOs ----

    public static class DecksRootDto {
        public List<DeckDto> decks;
    }

    public static class DeckDto {
        public String id;
        public String title;
        public String subtitle;
        public int tier;
        public boolean premium;
        public List<CardDto> cards;
    }

    public static class CardDto {
        public String id;
        public int tier;
        public String type;
        public List<String> tags;
        public int durationSec;
        public String text;
    }

    public static class ThemesRootDto {
        public List<ThemeDto> themes;
    }

    public static class ThemeDto {
        public String id;
        public String name;
        public String bgHex;
        public String primaryHex;
        public String accentHex;
        public int totalTiles;
        public boolean premium;
    }

    public static class DrawWordsRootDto {
        public List<DrawWordDto> words;
    }

    public static class DrawWordDto {
        public String word;
        public int tier;
        public List<String> tags;
    }

    public static class GeneratorBundleDto {
        public List<TemplateDto> templates = Collections.emptyList();
        public Map<String, List<String>> slots = Collections.emptyMap();
    }

    public static class TemplateDto {
        public String id;
        public int tier;
        public String type;
        public List<String> tags;
        public int durationSec;
        public String pattern;
    }
}
