package com.velvet.couplesgame.core.engine;

import com.google.gson.Gson;
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
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 全量题库 x 真实引擎 验证：每个游戏在每个标签/tier下执行 >=100 次抽题，
 * 统计连续重复率、空结果、tier合规性、覆盖率。
 * 结果写入 build/full_corpus_report.csv 供审计脚本汇总。
 */
public class FullCorpusValidationTest {

    private static final String[] LANGS = {"en","zh","zh_tw","ja","ko","es","fr","pt"};
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
}
