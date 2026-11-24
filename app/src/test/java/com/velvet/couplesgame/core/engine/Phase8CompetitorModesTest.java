package com.velvet.couplesgame.core.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.velvet.couplesgame.core.i18n.AssetContentLoader.NhieCardDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.PartyDuelRootDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.QuizQuestionDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.RoleplayActDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.RoleplayScenarioDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.ScratchCardDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.WyrCardDto;
import com.velvet.couplesgame.data.local.entity.BoundaryRuleEntity;
import com.velvet.couplesgame.ui.duel.PartyDuelEngine;
import com.velvet.couplesgame.ui.quiz.CoupleQuizEngine;
import com.velvet.couplesgame.ui.roleplay.RoleplayEngine;
import com.velvet.couplesgame.ui.scratch.ScratchAdventureEngine;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import org.junit.Test;

public class Phase8CompetitorModesTest {

    @Test
    public void scratchAdventureFiltersByCategoryTierAndConsent() {
        ScratchCardDto c1 = makeScratch("s1", "DARE", 1, "physical_light");
        ScratchCardDto c2 = makeScratch("s2", "LOCATION", 2, "public_space");
        ScratchCardDto c3 = makeScratch("s3", "FANTASY", 4, "blindfold");

        ConsentFilter filter = new ConsentFilter();
        filter.replaceRules(Collections.singletonList(
                new BoundaryRuleEntity("public_space", "HARD_NO", "YES")
        ));

        ScratchAdventureEngine engine = new ScratchAdventureEngine(new Random(1));
        engine.loadCards(Arrays.asList(c1, c2, c3), filter, 2);

        // Only s1 passes (s2 blocked by ConsentFilter, s3 blocked by maxTier=2)
        ScratchCardDto drawn = engine.drawNextCard();
        assertNotNull(drawn);
        assertEquals("s1", drawn.id);
        engine.markCurrentCompleted();
        assertEquals(1, engine.getCompletedCount());
    }

    @Test
    public void roleplayEngineStepsThroughActsAndReplacesPartnerPlaceholders() {
        RoleplayActDto act1 = new RoleplayActDto();
        act1.actTitle = "Act I";
        act1.direction = "{lead} pulls {partner} closer.";
        act1.whisperLine = "Hi";

        RoleplayActDto act2 = new RoleplayActDto();
        act2.actTitle = "Act II";
        act2.direction = "{partner} kisses {lead}.";
        act2.whisperLine = "Closer";

        RoleplayScenarioDto s = new RoleplayScenarioDto();
        s.id = "rp1";
        s.tier = 2;
        s.tags = Collections.singletonList("roleplay");
        s.acts = Arrays.asList(act1, act2);

        RoleplayEngine engine = new RoleplayEngine();
        engine.loadScenarios(Collections.singletonList(s), new ConsentFilter(), 2);
        assertEquals(1, engine.getAvailableScenarios().size());

        engine.startScenario(s);
        assertEquals("Alex pulls Maya closer.", engine.renderDirection(engine.getCurrentAct().direction, "Alex", "Maya"));
        assertFalse(engine.isLastAct());
        assertFalse(engine.advanceAct());
        assertTrue(engine.isLastAct());
        assertTrue(engine.advanceAct());
        assertEquals(1, engine.getCompletedScenarios());
    }

    @Test
    public void coupleQuizComputesChemistryPercentageAndMissCount() {
        QuizQuestionDto q1 = makeQuiz("q1", 1, "Prompt 1 {p1}");
        QuizQuestionDto q2 = makeQuiz("q2", 2, "Prompt 2 {p1}");

        CoupleQuizEngine engine = new CoupleQuizEngine(new Random(5));
        engine.startRound(Arrays.asList(q1, q2), new ConsentFilter(), 2);
        assertEquals(CoupleQuizEngine.Phase.SUBJECT_ANSWERING, engine.getPhase());

        assertEquals(CoupleQuizEngine.Phase.SUBJECT_ANSWERING, engine.recordSelection(0));
        assertEquals(CoupleQuizEngine.Phase.HANDOFF_TO_GUESSER, engine.recordSelection(2));

        engine.beginGuesserTurn();
        assertEquals(CoupleQuizEngine.Phase.GUESSER_GUESSING, engine.getPhase());

        // Match q1 (0==0), miss q2 (1!=2) -> 50% chemistry, 1 miss
        assertEquals(CoupleQuizEngine.Phase.GUESSER_GUESSING, engine.recordSelection(0));
        assertEquals(CoupleQuizEngine.Phase.RESULTS, engine.recordSelection(1));

        assertEquals(1, engine.getCorrectCount());
        assertEquals(1, engine.getMissCount());
        assertEquals(50, engine.getChemistryPercent());
        assertEquals(2, engine.buildComparisons("Alex").size());
    }

    @Test
    public void partyDuelCyclesBothModesAndTracksConfessions() {
        NhieCardDto n1 = new NhieCardDto();
        n1.id = "n1"; n1.tier = 1; n1.tags = Collections.singletonList("deep_talk"); n1.text = "Never have I ever...";

        WyrCardDto w1 = new WyrCardDto();
        w1.id = "w1"; w1.tier = 2; w1.tags = Collections.singletonList("playful"); w1.optionA = "A"; w1.optionB = "B";

        PartyDuelRootDto root = new PartyDuelRootDto();
        root.nhie = Collections.singletonList(n1);
        root.wyr = Collections.singletonList(w1);

        PartyDuelEngine engine = new PartyDuelEngine(new Random(3));
        engine.loadBundle(root, new ConsentFilter(), 2);

        assertNotNull(engine.nextNhie());
        engine.recordConfession(true);
        assertEquals(1, engine.getConfessionsA());

        engine.setActiveMode(PartyDuelEngine.Mode.WYR);
        assertNotNull(engine.nextWyr());
        engine.recordWyrOutcome(true);
        engine.recordWyrOutcome(false);
        assertEquals(1, engine.getWyrMatches());
        assertEquals(1, engine.getWyrDisagrees());
    }

    private ScratchCardDto makeScratch(String id, String cat, int tier, String tag) {
        ScratchCardDto dto = new ScratchCardDto();
        dto.id = id; dto.category = cat; dto.tier = tier; dto.durationSec = 30;
        dto.tags = Collections.singletonList(tag); dto.title = id; dto.text = "Test";
        return dto;
    }

    private QuizQuestionDto makeQuiz(String id, int tier, String prompt) {
        QuizQuestionDto q = new QuizQuestionDto();
        q.id = id; q.tier = tier; q.prompt = prompt;
        q.tags = Collections.singletonList("deep_talk");
        q.options = Arrays.asList("Opt 0", "Opt 1", "Opt 2", "Opt 3");
        return q;
    }
}
