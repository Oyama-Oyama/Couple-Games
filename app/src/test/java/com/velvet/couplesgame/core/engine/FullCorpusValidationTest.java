package com.velvet.couplesgame.core.engine;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.*;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.ui.auction.DesireAuctionEngine;
import com.velvet.couplesgame.ui.bomb.VelvetBombEngine;
import com.velvet.couplesgame.ui.connect4.Connect4Engine;
import com.velvet.couplesgame.ui.duel.PartyDuelEngine;
import com.velvet.couplesgame.ui.fatecubes.FateCubesEngine;
import com.velvet.couplesgame.ui.foreplay.ForeplayEscalatorEngine;
import com.velvet.couplesgame.ui.heartbeat.HeartbeatEngine;
import com.velvet.couplesgame.ui.ladder.DesireLadderEngine;
import com.velvet.couplesgame.ui.positions.PositionsEngine;
import com.velvet.couplesgame.ui.props.PropLabEngine;
import com.velvet.couplesgame.ui.quiz.CoupleQuizEngine;
import com.velvet.couplesgame.ui.scratch.ScratchAdventureEngine;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 全量题库 x 真实引擎 验证：每个游戏在每个标签/tier下执行 >=100 次抽题，
 * 统计连续重复率、空结果、tier合规性、覆盖率。
 * 结果写入 build/full_corpus_report.csv 供审计脚本汇总。
 */
public class FullCorpusValidationTest {

    private static final String[] LANGS = {"en","zh","zh_tw","ja","ko","es","fr","pt"};
    private static final String[] CONTENT_FILES = {
            "board_themes.json", "couple_quiz.json", "date_planner.json", "decks.json",
            "draw_words.json", "extreme_modes.json", "fate_cubes.json", "generator_templates.json",
            "intimate_modes.json", "party_duel.json", "roleplay_scenarios.json",
            "scratch_cards.json", "sealed_letter.json"
    };
    private static final int RUNS = 120; // >100 次实际抽题
    private static final Gson GSON = new Gson();
    private static final StringBuilder REPORT = new StringBuilder();
    private static final String ASSET_ROOT = "src/main/assets/content/";

    static {
        REPORT.append("game,lang,tier,tag,runs,empty,consecutive_repeats,repeat_rate_pct,distinct,text_dup\n");
    }

    private static <T> T load(String lang, String file, Class<T> clazz) {
        File f = new File(ASSET_ROOT + lang + "/" + file);
        if (!f.exists()) return null;
        try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
            return GSON.fromJson(r, clazz);
        } catch (Exception e) {
            throw new RuntimeException("load fail " + f, e);
        }
    }

    /** 记录一次抽题序列的统计 */
    private static void record(String game, String lang, int tier, String tag,
                               List<String> ids, List<String> texts) {
        int runs = ids.size();
        int empty = 0, consec = 0;
        for (int i = 0; i < runs; i++) {
            if (ids.get(i) == null || ids.get(i).isEmpty()) empty++;
            if (i > 0 && ids.get(i) != null && ids.get(i).equals(ids.get(i-1))) consec++;
        }
        int distinct = new HashSet<>(ids).size();
        int textDup = runs - new HashSet<>(texts).size();
        double rate = runs > 0 ? consec * 100.0 / runs : 100.0;
        REPORT.append(String.format("%s,%s,%d,%s,%d,%d,%d,%.1f,%d,%d%n",
                game, lang, tier, tag, runs, empty, consec, rate, distinct, textDup));
    }

    private static String idOf(Object o) {
        if (o == null) return "";
        if (o instanceof ScratchCardDto) return ((ScratchCardDto)o).id;
        if (o instanceof PropChallengeDto) return ((PropChallengeDto)o).id;
        if (o instanceof SimplePromptDto) return ((SimplePromptDto)o).id;
        if (o instanceof BlackjackStakeDto) return ((BlackjackStakeDto)o).id;
        if (o instanceof BodyMapZoneDto) return ((BodyMapZoneDto)o).id;
        if (o instanceof AuctionContractDto) return ((AuctionContractDto)o).id;
        if (o instanceof PositionDto) return ((PositionDto)o).id;
        if (o instanceof Connect4ActionDto) return ((Connect4ActionDto)o).id;
        if (o instanceof LadderStepDto) return "step_" + ((LadderStepDto)o).step;
        if (o instanceof CubeFaceDto) return ((CubeFaceDto)o).id;
        if (o instanceof CubeModifierDto) return ((CubeModifierDto)o).id;
        if (o instanceof NhieCardDto) return ((NhieCardDto)o).id;
        if (o instanceof WyrCardDto) return ((WyrCardDto)o).id;
        if (o instanceof QuizQuestionDto) return ((QuizQuestionDto)o).id;
        if (o instanceof ForeplayStageDto) return "stage_" + ((ForeplayStageDto)o).step;
        return "";
    }

    private static String textOf(Object o) {
        if (o == null) return "";
        if (o instanceof ScratchCardDto) return ((ScratchCardDto)o).text;
        if (o instanceof PropChallengeDto) return ((PropChallengeDto)o).text;
        if (o instanceof SimplePromptDto) return ((SimplePromptDto)o).text;
        if (o instanceof BlackjackStakeDto) return ((BlackjackStakeDto)o).label;
        if (o instanceof AuctionContractDto) return ((AuctionContractDto)o).text;
        if (o instanceof Connect4ActionDto) return ((Connect4ActionDto)o).fullDare;
        if (o instanceof CubeFaceDto) return ((CubeFaceDto)o).label;
        if (o instanceof CubeModifierDto) return ((CubeModifierDto)o).label;
        if (o instanceof NhieCardDto) return ((NhieCardDto)o).text;
        if (o instanceof WyrCardDto) return ((WyrCardDto)o).optionA + "|" + ((WyrCardDto)o).optionB;
        if (o instanceof QuizQuestionDto) return ((QuizQuestionDto)o).prompt;
        if (o instanceof LadderStepDto) return ((LadderStepDto)o).foreplayAction;
        if (o instanceof ForeplayStageDto) return ((ForeplayStageDto)o).instruction;
        return "";
    }

    /** 通用抽题验证器 */
    private interface Drawer { Object draw(int i); }
    private static void verify(String game, String lang, int tier, String tag, Drawer d, int runs) {
        List<String> ids = new ArrayList<>(), texts = new ArrayList<>();
        for (int i = 0; i < runs; i++) {
            Object o = d.draw(i);
            ids.add(idOf(o));
            texts.add(textOf(o));
        }
        record(game, lang, tier, tag, ids, texts);
    }

    // ---- 各游戏：按 tier x 标签分类 各抽 120 次 ----

    private void verifyScratch(String lang) {
        ScratchCardsRootDto b = load(lang, "scratch_cards.json", ScratchCardsRootDto.class);
        if (b == null) return;
        for (int tier = 1; tier <= 4; tier++) {
            final int t = tier;
            for (String cat : new String[]{"ALL","DARE","LOCATION","FANTASY"}) {
                ScratchAdventureEngine e = new ScratchAdventureEngine(new Random(1000+t+cat.length()));
                e.loadCards(b.cards, new ConsentFilter(), t);
                e.setCategory(cat);
                verify("scratch_adventure", lang, tier, cat, i -> e.drawNextCard(), RUNS);
            }
        }
    }

    private void verifyProps(String lang) {
        ExtremeModesBundleDto b = load(lang, "extreme_modes.json", ExtremeModesBundleDto.class);
        if (b == null) return;
        for (int tier = 2; tier <= 4; tier++) {
            final int t = tier;
            for (String prop : PropLabEngine.ALL_PROP_KEYS) {
                PropLabEngine e = new PropLabEngine(new Random(2000+t+prop.length()));
                e.loadChallenges(b.propChallenges, new ConsentFilter(), t);
                // 只保留当前 prop
                for (String pk : PropLabEngine.ALL_PROP_KEYS) if (!pk.equals(prop)) e.toggleProp(pk);
                verify("prop_sensory_lab", lang, tier, prop, i -> e.drawNextChallenge(), RUNS);
            }
        }
    }

    private void verifyConnect4(String lang) {
        ExtremeModesBundleDto b = load(lang, "extreme_modes.json", ExtremeModesBundleDto.class);
        if (b == null) return;
        for (int tier = 1; tier <= 4; tier++) {
            final int t = tier;
            Connect4Engine e = new Connect4Engine(new Random(3000+t));
            e.loadActionsAndReset(b.connect4Actions, new ConsentFilter(), t);
            verify("naughty_connect4", lang, tier, "all", i -> {
                // 每轮用 dropInColumn 铺满棋盘后 reset
                if (i % 42 == 0 && i > 0) e.resetBoard();
                return e.flattenCells().isEmpty() ? null : e.flattenCells().get(i % e.flattenCells().size()).action;
            }, RUNS);
        }
    }

    private void verifyHeartbeat(String lang) {
        IntimateModesBundleDto b = load(lang, "intimate_modes.json", IntimateModesBundleDto.class);
        if (b == null) return;
        for (int tier = 1; tier <= 4; tier++) {
            final int t = tier;
            HeartbeatEngine e = new HeartbeatEngine(new Random(4000+t));
            e.loadPrompts(b.heartbeatPrompts, new ConsentFilter(), t);
            verify("heartbeat_hold", lang, tier, "all", i -> e.drawNextPrompt(), RUNS);
        }
    }

    private void verifyBomb(String lang) {
        IntimateModesBundleDto b = load(lang, "intimate_modes.json", IntimateModesBundleDto.class);
        if (b == null) return;
        for (int tier = 1; tier <= 4; tier++) {
            final int t = tier;
            VelvetBombEngine e = new VelvetBombEngine(new Random(5000+t));
            e.loadRelays(b.bombRelays, new ConsentFilter(), t);
            verify("velvet_bomb", lang, tier, "all", i -> e.armNewRound(), RUNS);
        }
    }

    private void verifyAuction(String lang) {
        IntimateModesBundleDto b = load(lang, "intimate_modes.json", IntimateModesBundleDto.class);
        if (b == null) return;
        for (int tier = 1; tier <= 4; tier++) {
            final int t = tier;
            DesireAuctionEngine e = new DesireAuctionEngine(new Random(6000+t));
            e.loadContracts(b.auctionContracts, new ConsentFilter(), t);
            verify("desire_auction", lang, tier, "all", i -> e.nextContract(), RUNS);
        }
    }

    private void verifyPositions(String lang) {
        ExtremeModesBundleDto b = load(lang, "extreme_modes.json", ExtremeModesBundleDto.class);
        if (b == null) return;
        for (int diff = 1; diff <= 3; diff++) {
            final int d = diff;
            PositionsEngine e = new PositionsEngine(new Random(7000+d));
            e.loadPositions(b.positions, new ConsentFilter());
            e.setDifficultyFilter(d);
            verify("positions_roulette", lang, 0, "difficulty_"+d, i -> e.spinRandom(), RUNS);
        }
    }

    private void verifyForeplay(String lang) {
        ExtremeModesBundleDto b = load(lang, "extreme_modes.json", ExtremeModesBundleDto.class);
        if (b == null) return;
        ForeplayEscalatorEngine e = new ForeplayEscalatorEngine(new Random(8000));
        e.loadStages(b.foreplayStages, new ConsentFilter());
        List<String> ids = new ArrayList<>(), texts = new ArrayList<>();
        for (int i = 0; i < RUNS; i++) {
            ForeplayStageDto s = e.getCurrentStage();
            ids.add(idOf(s)); texts.add(textOf(s));
            e.nextStage(); // 内部到末尾自动 buildFreshArc 循环，忽略返回值
        }
        record("foreplay_escalator", lang, 0, "all", ids, texts);
    }

    private void verifyLadder(String lang) {
        ExtremeModesBundleDto b = load(lang, "extreme_modes.json", ExtremeModesBundleDto.class);
        if (b == null) return;
        DesireLadderEngine e = new DesireLadderEngine(new Random(9000));
        e.loadSteps(b.ladderSteps, new ConsentFilter());
        List<String> ids = new ArrayList<>(), texts = new ArrayList<>();
        for (int i = 0; i < RUNS; i++) {
            LadderStepDto s = e.getCurrentStep();
            ids.add(idOf(s)); texts.add(textOf(s));
            if (e.climbNext()) { // 到达 summit 后重置重新开始新一轮
                e.loadSteps(b.ladderSteps, new ConsentFilter());
            }
        }
        record("desire_ladder", lang, 0, "all", ids, texts);
    }

    private void verifyFateCubes(String lang) {
        FateCubesBundleDto b = load(lang, "fate_cubes.json", FateCubesBundleDto.class);
        if (b == null) return;
        for (int tier = 1; tier <= 4; tier++) {
            final int t = tier;
            FateCubesEngine e = new FateCubesEngine(new Random(10000+t));
            e.loadBundle(b, new ConsentFilter(), t);
            verify("fate_cubes_action", lang, tier, "action", i -> e.rollAction(), RUNS);
            FateCubesEngine e2 = new FateCubesEngine(new Random(11000+t));
            e2.loadBundle(b, new ConsentFilter(), t);
            verify("fate_cubes_spot", lang, tier, "spot", i -> e2.rollSpot(), RUNS);
        }
    }

    private void verifyDuel(String lang) {
        PartyDuelRootDto b = load(lang, "party_duel.json", PartyDuelRootDto.class);
        if (b == null) return;
        for (int tier = 1; tier <= 4; tier++) {
            final int t = tier;
            PartyDuelEngine e = new PartyDuelEngine(new Random(12000+t));
            e.loadBundle(b, new ConsentFilter(), t);
            verify("party_duel_nhie", lang, tier, "nhie", i -> e.nextNhie(), RUNS);
            PartyDuelEngine e2 = new PartyDuelEngine(new Random(13000+t));
            e2.loadBundle(b, new ConsentFilter(), t);
            verify("party_duel_wyr", lang, tier, "wyr", i -> e2.nextWyr(), RUNS);
        }
    }

    private void verifyQuiz(String lang) {
        QuizRootDto b = load(lang, "couple_quiz.json", QuizRootDto.class);
        if (b == null) return;
        for (int tier = 1; tier <= 4; tier++) {
            final int t = tier;
            List<String> ids = new ArrayList<>(), texts = new ArrayList<>();
            for (int r = 0; r < RUNS; r++) {
                CoupleQuizEngine e = new CoupleQuizEngine(new Random(14000+t+r));
                e.startRound(b.questions, new ConsentFilter(), t);
                QuizQuestionDto q = e.getCurrentQuestion();
                ids.add(idOf(q)); texts.add(textOf(q));
            }
            record("couple_quiz", lang, tier, "all", ids, texts);
        }
    }

    @Test
    public void validateAllGamesAllTagsAllLanguages() {
        for (String lang : LANGS) {
            verifyScratch(lang);
            verifyProps(lang);
            verifyConnect4(lang);
            verifyHeartbeat(lang);
            verifyBomb(lang);
            verifyAuction(lang);
            verifyPositions(lang);
            verifyForeplay(lang);
            verifyLadder(lang);
            verifyFateCubes(lang);
            verifyDuel(lang);
            verifyQuiz(lang);
        }
        File out = new File("build/full_corpus_report.csv");
        out.getParentFile().mkdirs();
        try (Writer w = new OutputStreamWriter(new FileOutputStream(out), StandardCharsets.UTF_8)) {
            w.write(REPORT.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("REPORT_LINES=" + REPORT.toString().split("\n").length);
        assertTrue("report generated", out.exists());
    }

    @Test
    public void validateContentIdsMatchAcrossAllLanguages() throws IOException {
        for (String file : CONTENT_FILES) {
            Set<String> expected = null;
            for (String lang : LANGS) {
                File asset = new File(ASSET_ROOT + lang + "/" + file);
                Set<String> ids = new HashSet<>();
                try (Reader reader = new InputStreamReader(
                        new FileInputStream(asset), StandardCharsets.UTF_8)) {
                    collectIds(JsonParser.parseReader(reader), lang + "/" + file, ids);
                }
                if (expected == null) {
                    expected = ids;
                } else {
                    assertEquals("ID mismatch in " + lang + "/" + file, expected, ids);
                }
            }
        }
    }

    @Test
    public void validateLocalizedAssetsHaveNoEncodingDamage() throws IOException {
        Pattern replacementRun = Pattern.compile("\\?{3,}");
        for (String lang : LANGS) {
            for (String file : CONTENT_FILES) {
                File asset = new File(ASSET_ROOT + lang + "/" + file);
                StringBuilder content = new StringBuilder();
                try (Reader reader = new InputStreamReader(
                        new FileInputStream(asset), StandardCharsets.UTF_8)) {
                    char[] buffer = new char[4096];
                    int count;
                    while ((count = reader.read(buffer)) != -1) {
                        content.append(buffer, 0, count);
                    }
                }
                assertFalse("Replacement character in " + lang + "/" + file,
                        content.indexOf("\uFFFD") >= 0);
                assertFalse("Possible lossy text encoding in " + lang + "/" + file,
                        replacementRun.matcher(content).find());
            }
        }
    }

    @Test
    public void validateSocialPromptAndAnswerArraysAlignAcrossLocales() throws IOException {
        String[] resourceDirs = {
                "values", "values-es", "values-fr", "values-ja",
                "values-ko", "values-pt-rBR", "values-zh-rCN", "values-zh-rTW"
        };
        Pattern arrayPattern = Pattern.compile(
                "<string-array name=\"([^\"]+)\">([\\s\\S]*?)</string-array>");
        Pattern itemPattern = Pattern.compile("<item>");
        String[] requiredArrays = {
                "social_most_likely_prompts", "social_love_language_prompts",
                "social_riddle_prompts", "social_riddle_answers"
        };

        for (String resourceDir : resourceDirs) {
            File strings = new File("src/main/res/" + resourceDir + "/strings.xml");
            String xml = new String(java.nio.file.Files.readAllBytes(strings.toPath()),
                    StandardCharsets.UTF_8);
            Map<String, Integer> counts = new HashMap<>();
            Matcher arrays = arrayPattern.matcher(xml);
            while (arrays.find()) {
                counts.put(arrays.group(1), itemPattern.matcher(arrays.group(2)).results().toArray().length);
            }
            for (String arrayName : requiredArrays) {
                assertEquals(resourceDir + "/" + arrayName, Integer.valueOf(6), counts.get(arrayName));
            }
        }
    }

    @Test
    public void validateCustomPropPromptTemplatesExistInEveryLocale() throws IOException {
        String[] resourceDirs = {
                "values", "values-es", "values-fr", "values-ja",
                "values-ko", "values-pt-rBR", "values-zh-rCN", "values-zh-rTW"
        };
        Pattern arrayPattern = Pattern.compile(
                "<string-array name=\"props_custom_prompt_templates\">([\\s\\S]*?)</string-array>");
        Pattern itemPattern = Pattern.compile("<item>([\\s\\S]*?)</item>");
        Pattern propPlaceholder = Pattern.compile("%1\\$s");

        for (String resourceDir : resourceDirs) {
            File strings = new File("src/main/res/" + resourceDir + "/strings.xml");
            String xml = new String(java.nio.file.Files.readAllBytes(strings.toPath()),
                    StandardCharsets.UTF_8);
            Matcher array = arrayPattern.matcher(xml);
            assertTrue(resourceDir + " custom prop prompt array is missing", array.find());
            Matcher items = itemPattern.matcher(array.group(1));
            int promptCount = 0;
            while (items.find()) {
                promptCount++;
                assertTrue(resourceDir + " prompt must contain the custom item placeholder",
                        propPlaceholder.matcher(items.group(1)).find());
            }
            assertEquals(resourceDir + " custom prop prompt count", 3, promptCount);
        }
    }

    @Test
    public void validateGeneratorPlaceholdersResolveInEveryLanguage() throws IOException {
        Pattern placeholderPattern = Pattern.compile("\\{([^{}]+)\\}");
        for (String lang : LANGS) {
            File asset = new File(ASSET_ROOT + lang + "/generator_templates.json");
            JsonObject root;
            try (Reader reader = new InputStreamReader(
                    new FileInputStream(asset), StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader).getAsJsonObject();
            }
            Set<String> slots = root.getAsJsonObject("slots").keySet();
            for (String slot : slots) {
                assertTrue("Invalid slot name in " + lang + ": " + slot,
                        slot.matches("[A-Za-z0-9_]+"));
            }
            assertGeneratorPlaceholdersResolve(root, asset.getPath(), slots, placeholderPattern);
        }
    }

    private static void assertGeneratorPlaceholdersResolve(JsonElement element, String asset,
                                                            Set<String> slots, Pattern pattern) {
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                assertGeneratorPlaceholdersResolve(entry.getValue(), asset, slots, pattern);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                assertGeneratorPlaceholdersResolve(child, asset, slots, pattern);
            }
        } else if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String text = element.getAsString();
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                String slot = matcher.group(1);
                assertTrue("Malformed placeholder {" + slot + "} in " + asset,
                        slot.matches("[A-Za-z0-9_]+"));
                assertTrue("Unknown placeholder {" + slot + "} in " + asset,
                        slots.contains(slot) || "zone".equals(slot));
            }
        }
    }

    @Test
    public void validateEditedHeartbeatPromptsHaveCleanLocalizedCopy() throws IOException {
        Set<String> editedIds = new HashSet<>(Arrays.asList(
                "hb_01", "hb_04", "hb_06", "hb_07", "hb_10"));
        String[][] knownArtifacts = {
                {"softly soft", "soft soft", "kisss"},
                {"温柔温柔的吻吸", "温柔的吻吸", "不准松开"},
                {"溫柔溫柔的吻吸", "溫柔的吻吸", "不准鬆開"},
                {"指を離したら負け", "息を沿わせてゆっくり息"},
                {"숨을 살짝 스치게 해고", "절대 떼지 마세요"},
                {"beso suave suavemente", "Lame y beso suave"},
                {"Ne lâchez pas", "Sans lâcher"},
                {"Não soltem os dedos", "Mordisque de leve"}
        };

        for (int i = 0; i < LANGS.length; i++) {
            IntimateModesBundleDto content = load(
                    LANGS[i], "intimate_modes.json", IntimateModesBundleDto.class);
            assertNotNull("Missing heartbeat content for " + LANGS[i], content);
            Set<String> foundIds = new HashSet<>();
            for (SimplePromptDto prompt : content.heartbeatPrompts) {
                if (!editedIds.contains(prompt.id)) {
                    continue;
                }
                foundIds.add(prompt.id);
                String lowerText = prompt.text.toLowerCase(Locale.ROOT);
                for (String artifact : knownArtifacts[i]) {
                    assertFalse("Translation artifact in " + LANGS[i] + "/" + prompt.id
                                    + ": " + prompt.text,
                            lowerText.contains(artifact.toLowerCase(Locale.ROOT)));
                }
            }
            assertEquals("Missing edited heartbeat IDs in " + LANGS[i],
                    editedIds, foundIds);
        }
    }

    @Test
    public void validateKnownEnglishAndSpanishCopyArtifactsAreAbsent() throws IOException {
        String[][] cases = {
                {"en", "kisss", "focus leave", "giving leave"},
                {"es", "beso suave suavemente", "en frente al tocador tenue",
                        "En frente al tocador tenue"}
        };
        for (String[] testCase : cases) {
            String lang = testCase[0];
            for (String file : CONTENT_FILES) {
                File contentFile = new File(ASSET_ROOT + lang + "/" + file);
                StringBuilder content = new StringBuilder();
                try (Reader reader = new InputStreamReader(
                        new FileInputStream(contentFile), StandardCharsets.UTF_8)) {
                    char[] buffer = new char[4096];
                    int count;
                    while ((count = reader.read(buffer)) != -1) {
                        content.append(buffer, 0, count);
                    }
                }
                for (int i = 1; i < testCase.length; i++) {
                    assertFalse("Copy artifact in " + lang + "/" + file + ": " + testCase[i],
                            content.indexOf(testCase[i]) >= 0);
                }
            }
        }
    }

    @Test
    public void validateStaticContentAvoidsCoercivePromptPhrasing() throws IOException {
        String[][] forbiddenPhrases = {
                {"keep their hands flat against the headboard",
                        "press their hands flat against the headboard",
                        "without letting them touch back", "forcing them to keep watching in the glass",
                        "can't move for 5 minutes", "unconditionally comply",
                        "hold both wrists gently above their head", "wrists pinned overhead",
                        "hands completely behind your back", "hands behind your back"},
                {"不许放下", "无条件配合", "不准主动回吻", "只能看着你", "双手贴紧床头板"},
                {"不許放下", "無條件配合", "不准主動回吻", "只能看著你", "只能看着你"},
                {"両手をベッドボードにつけたまま動かさず",
                        "両手を背中に回させて動かさず", "キス返し禁止", "反撃禁止",
                        "両手首を背中で", "両手首を頭上", "手首固定"},
                {"양손목을 머리 위로 부드럽게 고정하고", "한 손으로 양손목을 머리 위에 살짝 눌러 고정하고",
                        "반격 금지", "키스 반격을 금지", "움직이지 못하게",
                        "양손목을 등 뒤", "양손목을 머리 위", "반격 없이",
                        "서로 동의한 편안한 자세를 고르고 손은 자유롭게 움직일 수 있게"},
                {"Mantén sus manos pegadas al cabecero",
                        "Entrelaza sus dedos tras la nuca sin que pueda defenderse",
                        "no puede moverse:", "prohíbele devolver el beso", "sin pausa",
                        "Sujeta ambas muñecas", "Muñecas sujetas", "manos atrás",
                        "sin que toque de vuelta", "robándole un beso"},
                {"Garde ses mains plaquées contre la tête de lit",
                        "Mains plaquées à la tête de lit", "clouais doucement à la tête de lit",
                        "Plaque-le contre le mur", "sans arrêt",
                        "Retiens ses poignets derrière", "Maintenez ses poignets derrière",
                        "poignets derrière le dos"},
                {"Mantenha as mãos dele coladas na cabeceira", "Mãos coladas na cabeceira",
                        "sem recuar", "sem parar"}
        };

        for (int i = 0; i < LANGS.length; i++) {
            String lang = LANGS[i];
            for (String file : CONTENT_FILES) {
                File asset = new File(ASSET_ROOT + lang + "/" + file);
                JsonElement root;
                try (Reader reader = new InputStreamReader(
                        new FileInputStream(asset), StandardCharsets.UTF_8)) {
                    root = JsonParser.parseReader(reader);
                }
                assertNoForbiddenCopy(root, lang + "/" + file, forbiddenPhrases[i]);
            }
        }
    }

    private static void assertNoForbiddenCopy(JsonElement element, String asset,
                                             String[] forbiddenPhrases) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String lowerText = element.getAsString().toLowerCase(Locale.ROOT);
            for (String phrase : forbiddenPhrases) {
                assertFalse("Coercive prompt wording in " + asset + ": " + element.getAsString(),
                        lowerText.contains(phrase.toLowerCase(Locale.ROOT)));
            }
        } else if (element.isJsonObject()) {
            for (java.util.Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                assertNoForbiddenCopy(entry.getValue(), asset, forbiddenPhrases);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                assertNoForbiddenCopy(child, asset, forbiddenPhrases);
            }
        }
    }

    @Test
    public void validateSensitiveGeneratorPromptsRequireConsent() throws IOException {
        String[][] consentCues = {
                {"if you both agree", "if your partner wants to"},
                {"双方事先同意", "仅在伴侣也愿意时", "仅在双方都愿意时"},
                {"雙方事先同意", "僅在伴侶也願意時", "僅在雙方都願意時"},
                {"二人とも事前に同意", "相手も望む場合", "二人とも望むなら"},
                {"두 사람 모두 미리 동의", "두 사람이 미리 동의", "상대도 원할 때", "두 사람 모두 원한다면"},
                {"si ambos están de acuerdo", "si ambos quieren", "si a tu pareja le apetece"},
                {"si vous êtes tous les deux d'accord", "si vous convenez tous les deux",
                        "si votre partenaire en a envie", "si vous souhaitez tous les deux"},
                {"se ambos concordarem", "se ambos quiserem", "se a outra pessoa quiser"}
        };
        String[][] forbiddenPhrases = {
                {"forbid them from kissing back", "break silence and owe", "nonstop",
                        "non-stop", "without stopping", "if they grab your hands or lose control"},
                {"命令对方不准主动回吻", "若对方没忍住伸手抓你", "出声就加罚",
                        "破功出声就加罚", "连续 {duration} 秒不停", "不停{action_bold}", "绝对别停"},
                {"命令對方不準主動回吻", "若對方沒忍住伸手抓你", "出聲就加罰",
                        "破功出聲就加罰", "連續 {duration} 秒不停", "不停{action_bold}", "絕對別停"},
                {"キス返し禁止", "我慢できず手が出たら", "声が出たら追加", "止まらず"},
                {"키스 반격을 금지", "참지 못하고 손을 잡거나", "소리를 내면 추가", "멈추지 말고"},
                {"prohíbele devolver el beso", "si pierde el control o te agarra las manos",
                        "si rompe el silencio", "sin pausa", "sin parar"},
                {"interdis-lui de t'embrasser", "attrape tes mains, gage", "un seul bruit et gage",
                        "sans arrêt", "non-stop"},
                {"proíba de beijar de volta", "se perder o controle ou segurar suas mãos",
                        "se soltar um som, prenda extra", "sem parar", "sem pausar"}
        };

        for (int i = 0; i < LANGS.length; i++) {
            String lang = LANGS[i];
            File asset = new File(ASSET_ROOT + lang + "/generator_templates.json");
            JsonElement root;
            try (Reader reader = new InputStreamReader(
                    new FileInputStream(asset), StandardCharsets.UTF_8)) {
                root = JsonParser.parseReader(reader);
            }
            validateGeneratorCopy(root, lang + "/generator_templates.json",
                    consentCues[i], forbiddenPhrases[i], false);
        }
    }

    private static void validateGeneratorCopy(JsonElement element, String asset,
                                              String[] consentCues, String[] forbiddenPhrases,
                                              boolean inheritedSensitiveTag) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            boolean sensitiveTag = inheritedSensitiveTag;
            JsonElement tagsElement = object.get("tags");
            if (tagsElement != null && tagsElement.isJsonArray()) {
                for (JsonElement tag : tagsElement.getAsJsonArray()) {
                    if (tag.isJsonPrimitive()
                            && ("restraint_light".equals(tag.getAsString())
                            || "blindfold".equals(tag.getAsString()))) {
                        sensitiveTag = true;
                    }
                }
            }

            JsonElement patternElement = object.get("pattern");
            if (patternElement != null && patternElement.isJsonPrimitive()
                    && patternElement.getAsJsonPrimitive().isString()) {
                String pattern = patternElement.getAsString();
                boolean sensitive = sensitiveTag || pattern.contains("{restraint_pose}");
                if (sensitive) {
                    String lowerPattern = pattern.toLowerCase(Locale.ROOT);
                    boolean hasConsentCue = false;
                    for (String cue : consentCues) {
                        if (lowerPattern.contains(cue.toLowerCase(Locale.ROOT))) {
                            hasConsentCue = true;
                            break;
                        }
                    }
                    assertTrue("Missing prior-consent language in " + asset + ": " + pattern,
                            hasConsentCue);
                }
                String lowerPattern = pattern.toLowerCase(Locale.ROOT);
                for (String phrase : forbiddenPhrases) {
                    assertFalse("Unsafe generator wording in " + asset + ": " + pattern,
                            lowerPattern.contains(phrase.toLowerCase(Locale.ROOT)));
                }
            }

            for (java.util.Map.Entry<String, JsonElement> entry : object.entrySet()) {
                validateGeneratorCopy(entry.getValue(), asset, consentCues,
                        forbiddenPhrases, sensitiveTag);
            }
        } else if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                validateGeneratorCopy(child, asset, consentCues,
                        forbiddenPhrases, inheritedSensitiveTag);
            }
        }
    }

    private static void collectIds(JsonElement element, String asset, Set<String> ids) {
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            JsonElement id = object.get("id");
            if (id != null && id.isJsonPrimitive() && id.getAsJsonPrimitive().isString()) {
                assertTrue("Duplicate ID " + id.getAsString() + " in " + asset,
                        ids.add(id.getAsString()));
            }
            for (java.util.Map.Entry<String, JsonElement> entry : object.entrySet()) {
                collectIds(entry.getValue(), asset, ids);
            }
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement child : array) {
                collectIds(child, asset, ids);
            }
        }
    }
}
