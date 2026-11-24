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
    private final Map<String, FateCubesBundleDto> fateCubesCache = new ConcurrentHashMap<>();
    private final Map<String, ScratchCardsRootDto> scratchCache = new ConcurrentHashMap<>();
    private final Map<String, RoleplayRootDto> roleplayCache = new ConcurrentHashMap<>();
    private final Map<String, QuizRootDto> quizCache = new ConcurrentHashMap<>();
    private final Map<String, PartyDuelRootDto> partyDuelCache = new ConcurrentHashMap<>();
    private final Map<String, IntimateModesBundleDto> intimateModesCache = new ConcurrentHashMap<>();
    private final Map<String, ExtremeModesBundleDto> extremeModesCache = new ConcurrentHashMap<>();
    private final Map<String, DatePlannerRootDto> datePlannerCache = new ConcurrentHashMap<>();
    private final Map<String, SealedLetterBundleDto> sealedLetterCache = new ConcurrentHashMap<>();

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

    public FateCubesBundleDto loadFateCubesBundle() {
        return loadFateCubesBundle(LocaleManager.resolveAssetLangDir());
    }

    public FateCubesBundleDto loadFateCubesBundle(String langDir) {
        return fateCubesCache.computeIfAbsent(langDir, lang -> {
            FateCubesBundleDto dto = readJsonWithFallback(lang, "fate_cubes.json", FateCubesBundleDto.class);
            return dto != null ? dto : new FateCubesBundleDto();
        });
    }

    public ScratchCardsRootDto loadScratchCards() {
        return loadScratchCards(LocaleManager.resolveAssetLangDir());
    }

    public ScratchCardsRootDto loadScratchCards(String langDir) {
        return scratchCache.computeIfAbsent(langDir, lang -> {
            ScratchCardsRootDto dto = readJsonWithFallback(lang, "scratch_cards.json", ScratchCardsRootDto.class);
            return dto != null ? dto : new ScratchCardsRootDto();
        });
    }

    public RoleplayRootDto loadRoleplayScenarios() {
        return loadRoleplayScenarios(LocaleManager.resolveAssetLangDir());
    }

    public RoleplayRootDto loadRoleplayScenarios(String langDir) {
        return roleplayCache.computeIfAbsent(langDir, lang -> {
            RoleplayRootDto dto = readJsonWithFallback(lang, "roleplay_scenarios.json", RoleplayRootDto.class);
            return dto != null ? dto : new RoleplayRootDto();
        });
    }

    public QuizRootDto loadCoupleQuiz() {
        return loadCoupleQuiz(LocaleManager.resolveAssetLangDir());
    }

    public QuizRootDto loadCoupleQuiz(String langDir) {
        return quizCache.computeIfAbsent(langDir, lang -> {
            QuizRootDto dto = readJsonWithFallback(lang, "couple_quiz.json", QuizRootDto.class);
            return dto != null ? dto : new QuizRootDto();
        });
    }

    public PartyDuelRootDto loadPartyDuel() {
        return loadPartyDuel(LocaleManager.resolveAssetLangDir());
    }

    public PartyDuelRootDto loadPartyDuel(String langDir) {
        return partyDuelCache.computeIfAbsent(langDir, lang -> {
            PartyDuelRootDto dto = readJsonWithFallback(lang, "party_duel.json", PartyDuelRootDto.class);
            return dto != null ? dto : new PartyDuelRootDto();
        });
    }

    public IntimateModesBundleDto loadIntimateModes() {
        return loadIntimateModes(LocaleManager.resolveAssetLangDir());
    }

    public IntimateModesBundleDto loadIntimateModes(String langDir) {
        return intimateModesCache.computeIfAbsent(langDir, lang -> {
            IntimateModesBundleDto dto = readJsonWithFallback(lang, "intimate_modes.json", IntimateModesBundleDto.class);
            return dto != null ? dto : new IntimateModesBundleDto();
        });
    }

    public ExtremeModesBundleDto loadExtremeModes() {
        return loadExtremeModes(LocaleManager.resolveAssetLangDir());
    }

    public ExtremeModesBundleDto loadExtremeModes(String langDir) {
        return extremeModesCache.computeIfAbsent(langDir, lang -> {
            ExtremeModesBundleDto dto = readJsonWithFallback(lang, "extreme_modes.json", ExtremeModesBundleDto.class);
            return dto != null ? dto : new ExtremeModesBundleDto();
        });
    }

    public DatePlannerRootDto loadDatePlanner() {
        return loadDatePlanner(LocaleManager.resolveAssetLangDir());
    }

    public DatePlannerRootDto loadDatePlanner(String langDir) {
        return datePlannerCache.computeIfAbsent(langDir, lang -> {
            DatePlannerRootDto dto = readJsonWithFallback(lang, "date_planner.json", DatePlannerRootDto.class);
            return dto != null ? dto : new DatePlannerRootDto();
        });
    }

    public SealedLetterBundleDto loadSealedLetter() {
        return loadSealedLetter(LocaleManager.resolveAssetLangDir());
    }

    public SealedLetterBundleDto loadSealedLetter(String langDir) {
        return sealedLetterCache.computeIfAbsent(langDir, lang -> {
            SealedLetterBundleDto dto = readJsonWithFallback(lang, "sealed_letter.json", SealedLetterBundleDto.class);
            return dto != null ? dto : new SealedLetterBundleDto();
        });
    }

    public void clearCache() {
        decksCache.clear();
        themesCache.clear();
        drawWordsCache.clear();
        generatorCache.clear();
        fateCubesCache.clear();
        scratchCache.clear();
        roleplayCache.clear();
        quizCache.clear();
        partyDuelCache.clear();
        intimateModesCache.clear();
        extremeModesCache.clear();
        datePlannerCache.clear();
        sealedLetterCache.clear();
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
        public Map<String, List<ModeTemplateDto>> modeTemplates = Collections.emptyMap();
    }

    public static class ModeTemplateDto {
        public String id;
        public String category;   // Scratch category or Prop key or BodyMap normal/hot
        public int tier;
        public int durationSec;
        public List<String> tags;
        public String title;      // Optional title pattern (Scratch, Auction, Connect4 shortLabel)
        public String pattern;    // Primary body pattern
        public String secondaryPattern; // Optional secondary pattern (e.g. Ladder truthQuestion)
    }

    public static class FateCubesBundleDto {
        public List<CubeFaceDto> actions = Collections.emptyList();
        public List<CubeFaceDto> spots = Collections.emptyList();
        public List<CubeModifierDto> modifiers = Collections.emptyList();
    }

    public static class CubeFaceDto {
        public String id;
        public String label;
        public String icon;
        public int tier;
        public List<String> tags;
    }

    public static class CubeModifierDto {
        public String id;
        public String label;
        public int durationSec;
        public int tier;
        public List<String> tags;
    }

    public static class ScratchCardsRootDto {
        public List<ScratchCardDto> cards = Collections.emptyList();
    }

    public static class ScratchCardDto {
        public String id;
        public String category; // DARE / LOCATION / FANTASY
        public int tier;
        public int durationSec;
        public List<String> tags;
        public String title;
        public String text;
    }

    public static class RoleplayRootDto {
        public List<RoleplayScenarioDto> scenarios = Collections.emptyList();
    }

    public static class RoleplayScenarioDto {
        public String id;
        public int tier;
        public List<String> tags;
        public String title;
        public String subtitle;
        public String leadRole;
        public String partnerRole;
        public List<RoleplayActDto> acts = Collections.emptyList();
    }

    public static class RoleplayActDto {
        public String actTitle;
        public int durationSec;
        public String direction;
        public String whisperLine;
    }

    public static class QuizRootDto {
        public List<QuizQuestionDto> questions = Collections.emptyList();
    }

    public static class QuizQuestionDto {
        public String id;
        public int tier;
        public List<String> tags;
        public String prompt;
        public List<String> options = Collections.emptyList();
    }

    public static class PartyDuelRootDto {
        public List<NhieCardDto> nhie = Collections.emptyList();
        public List<WyrCardDto> wyr = Collections.emptyList();
    }

    public static class NhieCardDto {
        public String id;
        public int tier;
        public List<String> tags;
        public String text;
    }

    public static class WyrCardDto {
        public String id;
        public int tier;
        public List<String> tags;
        public String optionA;
        public String optionB;
    }

    public static class IntimateModesBundleDto {
        public List<SimplePromptDto> heartbeatPrompts = Collections.emptyList();
        public List<SimplePromptDto> bombRelays = Collections.emptyList();
        public List<BlackjackStakeDto> blackjackStakes = Collections.emptyList();
        public List<BodyMapZoneDto> bodyMapZones = Collections.emptyList();
        public List<AuctionContractDto> auctionContracts = Collections.emptyList();
    }

    public static class SimplePromptDto {
        public String id;
        public int tier;
        public List<String> tags;
        public String text;
    }

    public static class BlackjackStakeDto {
        public String id;
        public int tier;
        public List<String> tags;
        public String label;
    }

    public static class BodyMapZoneDto {
        public String id;
        public String label;
        public float nx;
        public float ny;
        public int tier;
        public List<String> tags;
        public String normalAction;
        public String hotAction;
        public int durationSec;
    }

    public static class AuctionContractDto {
        public String id;
        public int tier;
        public int durationSec;
        public List<String> tags;
        public String title;
        public String text;
    }

    public static class ExtremeModesBundleDto {
        public List<PositionDto> positions = Collections.emptyList();
        public List<ForeplayStageDto> foreplayStages = Collections.emptyList();
        public List<PropChallengeDto> propChallenges = Collections.emptyList();
        public List<Connect4ActionDto> connect4Actions = Collections.emptyList();
        public List<LadderStepDto> ladderSteps = Collections.emptyList();
    }

    public static class DatePlannerRootDto {
        public int version = 1;
        public List<String> categories = Collections.emptyList();
        public List<DateActivityDto> activities = Collections.emptyList();
    }

    public static class DateActivityDto {
        public String id;
        public String category;
        public int tier;
        public int durationMin;
        public List<String> tags = Collections.emptyList();
        public String title;
        public String detail;
    }

    /** Content bundle for the Sealed Letter mode. */
    public static class SealedLetterBundleDto {
        public int version = 1;
        public List<SealDto> seals = Collections.emptyList();
        public List<LetterPromptDto> prompts = Collections.emptyList();
        public List<LetterDelayDto> delays = Collections.emptyList();
    }

    /** One wax-seal style. */
    public static class SealDto {
        public String id;
        public String symbol;
        public String label;
    }

    /** One writing prompt offered to the writer. */
    public static class LetterPromptDto {
        public String id;
        public String text;
    }

    /** One reveal-delay preset. */
    public static class LetterDelayDto {
        public String id;
        public int days;
        public String label;
    }

    public static class PositionDto {
        public String id;
        public String name;
        public int difficulty;
        public int heat;
        public int durationSec;
        public List<String> tags;
        public String setup;
        public String tip;
    }

    public static class ForeplayStageDto {
        public int step;
        public String stageTitle;
        public int durationSec;
        public List<String> tags;
        public String instruction;
    }

    public static class PropChallengeDto {
        public String id;
        public String propKey;
        public int tier;
        public int durationSec;
        public List<String> tags;
        public String text;
    }

    public static class Connect4ActionDto {
        public String id;
        public String shortLabel;
        public int tier;
        public List<String> tags;
        public String fullDare;
    }

    public static class LadderStepDto {
        public int step;
        public int tier;
        public int durationSec;
        public List<String> tags;
        public String foreplayAction;
        public String truthQuestion;
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
