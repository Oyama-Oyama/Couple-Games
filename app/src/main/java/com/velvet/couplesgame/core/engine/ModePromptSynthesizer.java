package com.velvet.couplesgame.core.engine;

import com.velvet.couplesgame.core.i18n.AssetContentLoader.AuctionContractDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.BlackjackStakeDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.Connect4ActionDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.ForeplayStageDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.GeneratorBundleDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.LadderStepDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.ModeTemplateDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.NhieCardDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.PositionDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.PropChallengeDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.RoleplayActDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.RoleplayScenarioDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.ScratchCardDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.SimplePromptDto;
import com.velvet.couplesgame.core.i18n.AssetContentLoader.WyrCardDto;
import com.velvet.couplesgame.core.model.TaskCard;
import com.velvet.couplesgame.core.model.TaskType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pure-Java localized combinatorial prompt synthesizer that scales mode-specific decks
 * into hundreds-to-thousands of unique, non-repeating prompts across all 8 languages,
 * while strictly honoring ConsentFilter boundaries and Tonight's Heat maxTier.
 */
public class ModePromptSynthesizer {
    private static final Pattern SLOT_PATTERN = Pattern.compile("\\{([a-zA-Z0-9_]+)\\}");

    private final Random random;

    public ModePromptSynthesizer() {
        this(new Random());
    }

    public ModePromptSynthesizer(Random random) {
        this.random = random != null ? random : new Random();
    }

    public List<ScratchCardDto> synthesizeScratchCards(GeneratorBundleDto bundle,
                                                       ConsentFilter filter,
                                                       int maxTier,
                                                       int targetCount) {
        List<ModeTemplateDto> tpls = getModeTemplates(bundle, "scratch", filter, maxTier);
        if (tpls.isEmpty()) return Collections.emptyList();

        List<ScratchCardDto> out = new ArrayList<>();
        Set<String> seenText = new HashSet<>();
        int maxAttempts = targetCount * 10;
        for (int i = 0; i < maxAttempts && out.size() < targetCount; i++) {
            ModeTemplateDto t = tpls.get(random.nextInt(tpls.size()));
            Rendered r = render(t.pattern, bundle.slots, t.durationSec, null);
            if (!seenText.add(r.text)) continue;
            Rendered titleR = render(t.title != null ? t.title : "", bundle.slots, 0, null);
            ScratchCardDto dto = new ScratchCardDto();
            dto.id = "syn_sc_" + t.id + "_" + Integer.toHexString(r.text.hashCode());
            dto.category = t.category != null ? t.category : "DARE";
            dto.tier = t.tier;
            dto.durationSec = r.durationSec;
            dto.tags = t.tags;
            dto.title = titleR.text;
            dto.text = r.text;
            out.add(dto);
        }
        return out;
    }

    public List<SimplePromptDto> synthesizeSimplePrompts(GeneratorBundleDto bundle,
                                                         String modeKey,
                                                         ConsentFilter filter,
                                                         int maxTier,
                                                         int targetCount) {
        List<ModeTemplateDto> tpls = getModeTemplates(bundle, modeKey, filter, maxTier);
        if (tpls.isEmpty()) return Collections.emptyList();

        List<SimplePromptDto> out = new ArrayList<>();
        Set<String> seenText = new HashSet<>();
        int maxAttempts = targetCount * 10;
        for (int i = 0; i < maxAttempts && out.size() < targetCount; i++) {
            ModeTemplateDto t = tpls.get(random.nextInt(tpls.size()));
            Rendered r = render(t.pattern, bundle.slots, t.durationSec, null);
            if (!seenText.add(r.text)) continue;
            SimplePromptDto dto = new SimplePromptDto();
            dto.id = "syn_" + modeKey + "_" + t.id + "_" + Integer.toHexString(r.text.hashCode());
            dto.tier = t.tier;
            dto.tags = t.tags;
            dto.text = r.text;
            out.add(dto);
        }
        return out;
    }

    public List<BlackjackStakeDto> synthesizeBlackjackStakes(GeneratorBundleDto bundle,
                                                             ConsentFilter filter,
                                                             int maxTier,
                                                             int targetCount) {
        List<ModeTemplateDto> tpls = getModeTemplates(bundle, "blackjack", filter, maxTier);
        if (tpls.isEmpty()) return Collections.emptyList();

        List<BlackjackStakeDto> out = new ArrayList<>();
        Set<String> seenText = new HashSet<>();
        int maxAttempts = targetCount * 10;
        for (int i = 0; i < maxAttempts && out.size() < targetCount; i++) {
            ModeTemplateDto t = tpls.get(random.nextInt(tpls.size()));
            Rendered r = render(t.pattern, bundle.slots, t.durationSec, null);
            if (!seenText.add(r.text)) continue;
            BlackjackStakeDto dto = new BlackjackStakeDto();
            dto.id = "syn_bj_" + t.id + "_" + Integer.toHexString(r.text.hashCode());
            dto.tier = t.tier;
            dto.tags = t.tags;
            dto.label = r.text;
            out.add(dto);
        }
        return out;
    }

    public List<AuctionContractDto> synthesizeAuctionContracts(GeneratorBundleDto bundle,
                                                               ConsentFilter filter,
                                                               int maxTier,
                                                               int targetCount) {
        List<ModeTemplateDto> tpls = getModeTemplates(bundle, "auction", filter, maxTier);
        if (tpls.isEmpty()) return Collections.emptyList();

        List<AuctionContractDto> out = new ArrayList<>();
        Set<String> seenText = new HashSet<>();
        int maxAttempts = targetCount * 10;
        for (int i = 0; i < maxAttempts && out.size() < targetCount; i++) {
            ModeTemplateDto t = tpls.get(random.nextInt(tpls.size()));
            Rendered r = render(t.pattern, bundle.slots, t.durationSec, null);
            if (!seenText.add(r.text)) continue;
            Rendered titleR = render(t.title != null ? t.title : "", bundle.slots, 0, null);
            AuctionContractDto dto = new AuctionContractDto();
            dto.id = "syn_auc_" + t.id + "_" + Integer.toHexString(r.text.hashCode());
            dto.tier = t.tier;
            dto.durationSec = r.durationSec;
            dto.tags = t.tags;
            dto.title = titleR.text;
            dto.text = r.text;
            out.add(dto);
        }
        return out;
    }

    public List<TaskCard> synthesizeDesireMatchCards(GeneratorBundleDto bundle,
                                                     ConsentFilter filter,
                                                     int maxTier,
                                                     int targetCount) {
        List<ModeTemplateDto> tpls = getModeTemplates(bundle, "desire_match", filter, maxTier);
        if (tpls.isEmpty()) return Collections.emptyList();

        List<TaskCard> out = new ArrayList<>();
        Set<String> seenText = new HashSet<>();
        int maxAttempts = targetCount * 10;
        for (int i = 0; i < maxAttempts && out.size() < targetCount; i++) {
            ModeTemplateDto t = tpls.get(random.nextInt(tpls.size()));
            Rendered r = render(t.pattern, bundle.slots, t.durationSec, null);
            if (!seenText.add(r.text)) continue;
            String id = "syn_dm_" + t.id + "_" + Integer.toHexString(r.text.hashCode());
            out.add(new TaskCard(
                    id, "deck_desire_match", t.tier, TaskType.DESIRE_MATCH,
                    t.tags, r.durationSec, r.text, true, false
            ));
        }
        return out;
    }

    public List<PropChallengeDto> synthesizePropChallenges(GeneratorBundleDto bundle,
                                                           ConsentFilter filter,
                                                           int maxTier,
                                                           int perPropCount) {
        List<ModeTemplateDto> tpls = getModeTemplates(bundle, "props", filter, maxTier);
        if (tpls.isEmpty()) return Collections.emptyList();

        List<PropChallengeDto> out = new ArrayList<>();
        Set<String> seenText = new HashSet<>();
        for (ModeTemplateDto t : tpls) {
            int addedForTemplate = 0;
            int maxAttempts = perPropCount * 10;
            for (int i = 0; i < maxAttempts && addedForTemplate < perPropCount; i++) {
                Rendered r = render(t.pattern, bundle.slots, t.durationSec, null);
                if (!seenText.add(r.text)) continue;
                PropChallengeDto dto = new PropChallengeDto();
                dto.id = "syn_pr_" + t.id + "_" + Integer.toHexString(r.text.hashCode());
                dto.propKey = t.category != null ? t.category : "ice";
                dto.tier = t.tier;
                dto.durationSec = r.durationSec;
                dto.tags = t.tags;
                dto.text = r.text;
                out.add(dto);
                addedForTemplate++;
            }
        }
        return out;
    }

    public List<Connect4ActionDto> synthesizeConnect4Actions(GeneratorBundleDto bundle,
                                                             ConsentFilter filter,
                                                             int maxTier,
                                                             int targetCount) {
        List<ModeTemplateDto> tpls = getModeTemplates(bundle, "connect4", filter, maxTier);
        if (tpls.isEmpty()) return Collections.emptyList();

        List<Connect4ActionDto> out = new ArrayList<>();
        Set<String> seenText = new HashSet<>();
        int maxAttempts = targetCount * 10;
        for (int i = 0; i < maxAttempts && out.size() < targetCount; i++) {
            ModeTemplateDto t = tpls.get(random.nextInt(tpls.size()));
            Rendered r = render(t.pattern, bundle.slots, t.durationSec, null);
            if (!seenText.add(r.text)) continue;
            Rendered labelR = render(t.title != null ? t.title : "", bundle.slots, 0, null);
            Connect4ActionDto dto = new Connect4ActionDto();
            dto.id = "syn_c4_" + t.id + "_" + Integer.toHexString(r.text.hashCode());
            dto.shortLabel = trimLabel(labelR.text, 10);
            dto.tier = t.tier;
            dto.tags = t.tags;
            dto.fullDare = r.text;
            out.add(dto);
        }
        return out;
    }

    /** Synthesizes fresh escalating 10-rung Desire Ladder steps combining physical tease + sizzling truths. */
    public List<LadderStepDto> synthesizeLadderSteps(GeneratorBundleDto bundle,
                                                     ConsentFilter filter,
                                                     int maxTier,
                                                     int stepCount) {
        List<ModeTemplateDto> tpls = getModeTemplates(bundle, "ladder", filter, maxTier);
        if (tpls.isEmpty()) return Collections.emptyList();

        List<LadderStepDto> out = new ArrayList<>();
        Set<String> seenTease = new HashSet<>();
        Set<String> seenTruth = new HashSet<>();

        for (int s = 1; s <= stepCount; s++) {
            int desiredTier = Math.min(maxTier, 1 + ((s - 1) * 4) / Math.max(1, stepCount));
            List<ModeTemplateDto> matchingTier = new ArrayList<>();
            for (ModeTemplateDto t : tpls) {
                if (t.tier <= desiredTier) matchingTier.add(t);
            }
            if (matchingTier.isEmpty()) matchingTier = tpls;

            ModeTemplateDto picked = null;
            Rendered tease = null;
            Rendered truth = null;
            for (int attempt = 0; attempt < 20; attempt++) {
                ModeTemplateDto cand = matchingTier.get(random.nextInt(matchingTier.size()));
                Rendered rTease = render(cand.pattern, bundle.slots, cand.durationSec, null);
                Rendered rTruth = render(cand.secondaryPattern != null ? cand.secondaryPattern : "{secret_question}",
                        bundle.slots, 0, null);
                picked = cand;
                tease = rTease;
                truth = rTruth;
                if (!seenTease.contains(rTease.text) && !seenTruth.contains(rTruth.text)) {
                    break;
                }
            }
            if (picked != null && tease != null && truth != null) {
                seenTease.add(tease.text);
                seenTruth.add(truth.text);
                LadderStepDto dto = new LadderStepDto();
                dto.step = s;
                dto.tier = picked.tier;
                dto.durationSec = tease.durationSec > 0 ? tease.durationSec : 35;
                dto.tags = picked.tags;
                dto.foreplayAction = tease.text;
                dto.truthQuestion = truth.text;
                out.add(dto);
            }
        }
        return out;
    }

    /** Synthesizes fresh Kama Positions Roulette variations honoring ConsentFilter & maxTier. */
    public List<PositionDto> synthesizePositions(GeneratorBundleDto bundle,
                                                 ConsentFilter filter,
                                                 int maxTier,
                                                 int targetCount) {
        List<ModeTemplateDto> tpls = getModeTemplates(bundle, "positions", filter, maxTier);
        if (tpls.isEmpty()) return Collections.emptyList();

        List<PositionDto> out = new ArrayList<>();
        Set<String> seenSetup = new HashSet<>();
        int maxAttempts = targetCount * 10;
        for (int i = 0; i < maxAttempts && out.size() < targetCount; i++) {
            ModeTemplateDto t = tpls.get(random.nextInt(tpls.size()));
            Rendered setupR = render(t.pattern, bundle.slots, t.durationSec, null);
            if (!seenSetup.add(setupR.text)) continue;
            Rendered titleR = render(t.title != null ? t.title : "", bundle.slots, 0, null);
            Rendered tipR = render(t.secondaryPattern != null ? t.secondaryPattern : "", bundle.slots, 0, null);

            int difficulty = 1;
            if (t.category != null) {
                try {
                    difficulty = Math.max(1, Math.min(3, Integer.parseInt(t.category.trim())));
                } catch (NumberFormatException ignored) {
                    difficulty = Math.max(1, Math.min(3, t.tier));
                }
            }

            PositionDto dto = new PositionDto();
            dto.id = "syn_pos_" + t.id + "_" + Integer.toHexString(setupR.text.hashCode());
            dto.name = titleR.text;
            dto.difficulty = difficulty;
            dto.heat = Math.min(5, Math.max(2, t.tier + 1));
            dto.durationSec = setupR.durationSec > 0 ? setupR.durationSec : 55;
            dto.tags = t.tags;
            dto.setup = setupR.text;
            dto.tip = tipR.text;
            out.add(dto);
        }
        return out;
    }

    /** Synthesizes fresh 5-stage Foreplay Escalator arcs from modeTemplates.foreplay. */
    public List<ForeplayStageDto> synthesizeForeplayStages(GeneratorBundleDto bundle,
                                                           ConsentFilter filter,
                                                           int maxTier,
                                                           int arcCount) {
        List<ModeTemplateDto> tpls = getModeTemplates(bundle, "foreplay", filter, maxTier);
        if (tpls.isEmpty()) return Collections.emptyList();

        List<ForeplayStageDto> out = new ArrayList<>();
        Set<String> seenInstr = new HashSet<>();
        for (int arc = 0; arc < arcCount; arc++) {
            for (int step = 1; step <= 5; step++) {
                int desiredTier = Math.min(maxTier, Math.max(1, ((step - 1) * 4) / 5 + 1));
                List<ModeTemplateDto> eligibleStep = new ArrayList<>();
                for (ModeTemplateDto t : tpls) {
                    if (t.tier <= desiredTier) eligibleStep.add(t);
                }
                if (eligibleStep.isEmpty()) eligibleStep = tpls;

                ModeTemplateDto picked = null;
                Rendered instrR = null;
                for (int attempt = 0; attempt < 20; attempt++) {
                    ModeTemplateDto cand = eligibleStep.get(random.nextInt(eligibleStep.size()));
                    Rendered r = render(cand.pattern, bundle.slots, cand.durationSec, null);
                    picked = cand;
                    instrR = r;
                    if (!seenInstr.contains(r.text)) break;
                }
                if (picked != null && instrR != null && seenInstr.add(instrR.text)) {
                    Rendered titleR = render(picked.title != null ? picked.title : "", bundle.slots, 0, null);
                    ForeplayStageDto dto = new ForeplayStageDto();
                    dto.step = step;
                    dto.stageTitle = titleR.text;
                    dto.durationSec = instrR.durationSec > 0 ? instrR.durationSec : 60;
                    dto.tags = picked.tags;
                    dto.instruction = instrR.text;
                    out.add(dto);
                }
            }
        }
        return out;
    }

    /** Synthesizes fresh 3-act Roleplay Theater scenarios from modeTemplates.roleplay. */
    public List<RoleplayScenarioDto> synthesizeRoleplayScenarios(GeneratorBundleDto bundle,
                                                                 ConsentFilter filter,
                                                                 int maxTier,
                                                                 int targetCount) {
        List<ModeTemplateDto> tpls = getModeTemplates(bundle, "roleplay", filter, maxTier);
        if (tpls.isEmpty()) return Collections.emptyList();

        List<RoleplayScenarioDto> out = new ArrayList<>();
        Set<String> seenKeys = new HashSet<>();
        int maxAttempts = targetCount * 10;
        for (int i = 0; i < maxAttempts && out.size() < targetCount; i++) {
            ModeTemplateDto t = tpls.get(random.nextInt(tpls.size()));
            Rendered titleR = render(t.title != null ? t.title : "", bundle.slots, 0, null);
            Rendered act1R = render(t.pattern, bundle.slots, t.durationSec, null);
            if (!seenKeys.add(act1R.text)) continue;

            ModeTemplateDto t2 = tpls.get(random.nextInt(tpls.size()));
            Rendered act2R = render(t2.pattern, bundle.slots, t2.durationSec, null);
            ModeTemplateDto t3 = tpls.get(random.nextInt(tpls.size()));
            Rendered act3R = render(t3.pattern, bundle.slots, t3.durationSec, null);
            Rendered whisper1 = render("{whisper_topic}", bundle.slots, 0, null);
            Rendered whisper2 = render("{whisper_topic}", bundle.slots, 0, null);
            Rendered whisper3 = render("{secret_question}", bundle.slots, 0, null);

            RoleplayScenarioDto dto = new RoleplayScenarioDto();
            dto.id = "syn_rp_" + t.id + "_" + Integer.toHexString(act1R.text.hashCode());
            dto.tier = t.tier;
            dto.tags = t.tags;
            dto.title = titleR.text;
            dto.subtitle = trimLabel(act1R.text, 48);
            dto.leadRole = "Lead";
            dto.partnerRole = "Partner";

            List<RoleplayActDto> acts = new ArrayList<>(3);
            RoleplayActDto a1 = new RoleplayActDto();
            a1.actTitle = "Act 1";
            a1.durationSec = act1R.durationSec > 0 ? act1R.durationSec : 45;
            a1.direction = act1R.text;
            a1.whisperLine = whisper1.text;
            acts.add(a1);

            RoleplayActDto a2 = new RoleplayActDto();
            a2.actTitle = "Act 2";
            a2.durationSec = act2R.durationSec > 0 ? act2R.durationSec : 50;
            a2.direction = act2R.text;
            a2.whisperLine = whisper2.text;
            acts.add(a2);

            RoleplayActDto a3 = new RoleplayActDto();
            a3.actTitle = "Act 3";
            a3.durationSec = act3R.durationSec > 0 ? act3R.durationSec : 60;
            a3.direction = act3R.text;
            a3.whisperLine = whisper3.text;
            acts.add(a3);

            dto.acts = acts;
            out.add(dto);
        }
        return out;
    }

    /** Generates fresh Never-Have-I-Ever confessions honoring ConsentFilter & maxTier. */
    public List<NhieCardDto> synthesizeNhieCards(GeneratorBundleDto bundle,
                                                 ConsentFilter filter,
                                                 int maxTier,
                                                 int targetCount) {
        List<ModeTemplateDto> tpls = getModeTemplates(bundle, "nhie", filter, maxTier);
        if (tpls.isEmpty()) return Collections.emptyList();

        List<NhieCardDto> out = new ArrayList<>();
        Set<String> seenText = new HashSet<>();
        int maxAttempts = targetCount * 10;
        for (int i = 0; i < maxAttempts && out.size() < targetCount; i++) {
            ModeTemplateDto t = tpls.get(random.nextInt(tpls.size()));
            Rendered r = render(t.pattern, bundle.slots, 0, null);
            if (!seenText.add(r.text)) continue;
            NhieCardDto dto = new NhieCardDto();
            dto.id = "syn_nhie_" + t.id + "_" + Integer.toHexString(r.text.hashCode());
            dto.tier = t.tier;
            dto.tags = t.tags;
            dto.text = r.text;
            out.add(dto);
        }
        return out;
    }

    /** Generates fresh Would-You-Rather matchups by cross-pairing distinct localized WYR options. */
    public List<WyrCardDto> synthesizeWyrMatchups(List<WyrCardDto> baseWyr,
                                                  ConsentFilter filter,
                                                  int maxTier,
                                                  int targetCount) {
        if (baseWyr == null || baseWyr.size() < 2) return Collections.emptyList();
        List<WyrCardDto> eligible = new ArrayList<>();
        for (WyrCardDto w : baseWyr) {
            if (w.tier <= maxTier && (filter == null || filter.isAllowed(w.tags))) {
                eligible.add(w);
            }
        }
        if (eligible.size() < 2) return Collections.emptyList();

        List<String> allChoices = new ArrayList<>();
        List<WyrCardDto> choiceSources = new ArrayList<>();
        for (WyrCardDto w : eligible) {
            if (w.optionA != null && !w.optionA.isEmpty()) {
                allChoices.add(w.optionA);
                choiceSources.add(w);
            }
            if (w.optionB != null && !w.optionB.isEmpty()) {
                allChoices.add(w.optionB);
                choiceSources.add(w);
            }
        }
        if (allChoices.size() < 4) return Collections.emptyList();

        List<WyrCardDto> out = new ArrayList<>();
        Set<String> usedPairs = new HashSet<>();
        for (WyrCardDto w : eligible) {
            usedPairs.add(canonicalPair(w.optionA, w.optionB));
        }

        int maxAttempts = targetCount * 10;
        for (int i = 0; i < maxAttempts && out.size() < targetCount; i++) {
            int a = random.nextInt(allChoices.size());
            int b = random.nextInt(allChoices.size());
            if (a == b) continue;
            String optA = allChoices.get(a);
            String optB = allChoices.get(b);
            if (optA.equals(optB)) continue;
            String pairKey = canonicalPair(optA, optB);
            if (!usedPairs.add(pairKey)) continue;

            WyrCardDto srcA = choiceSources.get(a);
            WyrCardDto srcB = choiceSources.get(b);
            WyrCardDto syn = new WyrCardDto();
            syn.id = "syn_wyr_" + Integer.toHexString(pairKey.hashCode());
            syn.tier = Math.max(srcA.tier, srcB.tier);
            List<String> mergedTags = new ArrayList<>();
            if (srcA.tags != null) mergedTags.addAll(srcA.tags);
            if (srcB.tags != null) {
                for (String t : srcB.tags) {
                    if (!mergedTags.contains(t)) mergedTags.add(t);
                }
            }
            syn.tags = mergedTags;
            syn.optionA = optA;
            syn.optionB = optB;
            out.add(syn);
        }
        return out;
    }

    /** Generates a fresh zone-specific normal or hot action for Body Map Hunt so every tap feels new. */
    public String synthesizeBodyMapAction(GeneratorBundleDto bundle,
                                          String zoneLabel,
                                          boolean isHotSpot,
                                          ConsentFilter filter,
                                          int maxTier,
                                          String fallbackAction) {
        return synthesizeBodyMapAction(bundle, zoneLabel, isHotSpot, filter, maxTier, fallbackAction, null);
    }

    /** Generates a fresh zone-specific action while avoiding any recently shown action strings. */
    public String synthesizeBodyMapAction(GeneratorBundleDto bundle,
                                          String zoneLabel,
                                          boolean isHotSpot,
                                          ConsentFilter filter,
                                          int maxTier,
                                          String fallbackAction,
                                          Set<String> recentActions) {
        String key = isHotSpot ? "bodymap_hot" : "bodymap_normal";
        List<ModeTemplateDto> tpls = getModeTemplates(bundle, key, filter, maxTier);
        if (tpls.isEmpty() || zoneLabel == null || zoneLabel.isEmpty()) {
            return fallbackAction;
        }
        String lastCandidate = fallbackAction;
        for (int attempt = 0; attempt < 24; attempt++) {
            ModeTemplateDto picked = tpls.get(random.nextInt(tpls.size()));
            Rendered r = render(picked.pattern, bundle.slots, picked.durationSec, zoneLabel);
            if (r.text != null && !r.text.isEmpty()) {
                lastCandidate = r.text;
                if (recentActions == null || !recentActions.contains(r.text)) {
                    return r.text;
                }
            }
        }
        return lastCandidate;
    }

    private List<ModeTemplateDto> getModeTemplates(GeneratorBundleDto bundle,
                                                   String key,
                                                   ConsentFilter filter,
                                                   int maxTier) {
        if (bundle == null || bundle.modeTemplates == null) return Collections.emptyList();
        List<ModeTemplateDto> raw = bundle.modeTemplates.get(key);
        if (raw == null || raw.isEmpty()) return Collections.emptyList();
        List<ModeTemplateDto> eligible = new ArrayList<>();
        for (ModeTemplateDto t : raw) {
            if (t.tier > maxTier) continue;
            if (filter != null && !filter.isAllowed(t.tags)) continue;
            eligible.add(t);
        }
        return eligible;
    }

    private Rendered render(String pattern, Map<String, List<String>> slots, int fallbackDuration, String zoneOverride) {
        if (pattern == null) return new Rendered("", fallbackDuration);
        Matcher matcher = SLOT_PATTERN.matcher(pattern);
        StringBuffer sb = new StringBuffer();
        int resolvedDuration = fallbackDuration;
        Set<String> usedInPrompt = new HashSet<>();

        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = key;
            if ("zone".equals(key) && zoneOverride != null) {
                replacement = zoneOverride;
            } else if (slots != null && slots.containsKey(key)) {
                List<String> options = slots.get(key);
                if (options != null && !options.isEmpty()) {
                    List<String> fresh = new ArrayList<>();
                    for (String opt : options) {
                        if (!usedInPrompt.contains(opt)) fresh.add(opt);
                    }
                    List<String> source = fresh.isEmpty() ? options : fresh;
                    replacement = source.get(random.nextInt(source.size()));
                    usedInPrompt.add(replacement);
                    if ("duration".equals(key)) {
                        try {
                            resolvedDuration = Integer.parseInt(replacement.trim());
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return new Rendered(sb.toString(), resolvedDuration);
    }

    private String canonicalPair(String a, String b) {
        if (a == null) a = "";
        if (b == null) b = "";
        return a.compareTo(b) <= 0 ? (a + "||" + b) : (b + "||" + a);
    }

    private String trimLabel(String text, int maxLen) {
        if (text == null) return "";
        String t = text.trim();
        return t.length() <= maxLen ? t : t.substring(0, maxLen);
    }

    private static class Rendered {
        final String text;
        final int durationSec;

        Rendered(String text, int durationSec) {
            this.text = text;
            this.durationSec = durationSec;
        }
    }
}
